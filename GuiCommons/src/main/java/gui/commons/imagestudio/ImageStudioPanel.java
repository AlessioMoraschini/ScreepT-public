/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package gui.commons.imagestudio;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import gui.commons.dialogutils.NativeFileChooser;
import gui.commons.frameutils.frame.ImageViewerFrame;
import gui.commons.frameutils.frame.arch.ToolIcons;
import gui.commons.frameutils.frame.arch.ToolStyle;
import gui.commons.tabs.TabContent;
import net.miginfocom.swing.MigLayout;
import various.common.light.gui.ShortcutKeys;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;

/**
 * One image of the Image Studio: canvas with zoom and pan, tools bar, and on the right the accordion with conversion,
 * resize, adjustments, filters and transform. The preview is rendered in the background while the controls move;
 * "Apply" bakes the edits in the image (with undo), "Export" writes the result in any format.
 * Used both in the studio window (one panel per tab) and as an integrated tab of the editor.
 */
public class ImageStudioPanel extends JPanel implements TabContent {
	private static final long serialVersionUID = 1L;
	private static final SafeLogger logger = new SafeLogger(ImageStudioPanel.class);
	private static final int MAX_UNDO = 8;
	private static final int PREVIEW_DELAY_MS = 120;
	private static final int ESTIMATE_DELAY_MS = 500;

	private final ImageSettings settings = new ImageSettings();
	private final ImageSettingsPanel settingsPanel;
	private final ImageCanvas canvas = new ImageCanvas();
	private final HistogramView histogram = new HistogramView();
	private final JLabel lblStatus = ToolStyle.mutedLabel(" ");
	private final JLabel lblPixel = ToolStyle.mutedLabel(" ");
	private final JLabel lblZoom = ToolStyle.mutedLabel(" ");
	private final JLabel lblInfo = new JLabel("-");
	private final JPanel toolsBar;
	private final Runnable modeListener;
	private final boolean readOnly;
	private final AccordionPanel infoAccordion = new AccordionPanel();
	private JButton btnApply;
	private JButton btnPsdSettings;
	private PsdReader.Psd psdInfo;
	private JLabel lblMetaSummary;
	private Runnable closeHandler;
	private boolean comparing;
	private javax.swing.JToggleButton btnCompare;
	private JButton btnRevert;

	private File file;
	private String title = "Image";
	private BufferedImage original;
	private BufferedImage base;
	private BufferedImage preview;
	private final LinkedList<BufferedImage> undo = new LinkedList<BufferedImage>();
	private boolean pasted;
	private Runnable changeListener;

	private final Timer previewTimer;
	private final Timer estimateTimer;
	private int previewSequence;
	private int estimateSequence;
	private JButton btnUndo;

	/** Full studio, with the edit panel on the right: the Image Studio window */
	public ImageStudioPanel(File imageFile) {
		this(imageFile, false);
	}

	/**
	 * @param readOnly true: viewer for the editor tab (zoom, pan, details, histogram, copy), without edit panel and
	 *                 with a button that opens the image in the studio window
	 */
	public ImageStudioPanel(File imageFile, boolean readOnly) {
		super(new MigLayout("insets 6, gapx 8, gapy 6, hidemode 3", "[grow,fill,shrink]" + (readOnly ? "[240:260:290]" : "[270:300:330]"), "[][grow,fill,shrink][]"));
		this.readOnly = readOnly;
		this.file = imageFile;
		if (imageFile != null) {
			this.title = imageFile.getName();
		}

		settingsPanel = new ImageSettingsPanel(settings, this::onSettingsChanged);
		infoAccordion.setExclusive(false);
		toolsBar = buildToolsBar();
		buildExtraSections();

		add(toolsBar, "cell 0 0, growx, wmin 0");
		add(canvas, "cell 0 1, grow, wmin 0, hmin 0");
		add(buildSidebar(), "cell 1 0, spany 2, grow, wmin 0");

		JPanel status = new JPanel(new MigLayout("insets 0, gapx 16", "[grow,fill,shrink][][]", "[]"));
		status.setOpaque(false);
		status.add(lblStatus, "wmin 0");
		status.add(lblPixel);
		status.add(lblZoom);
		add(status, "cell 0 2, span 2, growx, wmin 0");

		canvas.setPixelListener(lblPixel::setText);
		canvas.setZoomListener(lblZoom::setText);

		previewTimer = new Timer(PREVIEW_DELAY_MS, e -> renderPreview());
		previewTimer.setRepeats(false);
		estimateTimer = new Timer(ESTIMATE_DELAY_MS, e -> renderEstimate());
		estimateTimer.setRepeats(false);

		if (readOnly) {
			modeListener = null;
		} else {
			modeListener = () -> SwingUtilities.invokeLater(this::applyMode);
			StudioMode.addListener(modeListener);
		}
		applyMode();
		bindKeys();

		if (imageFile != null) {
			loadFile(imageFile);
		}
	}

	// ------------------------------------------------------------------ UI

	private JPanel buildToolsBar() {
		final JPanel bar = new JPanel(new MigLayout("insets 0, gapx 4, hidemode 3", "", "[]"));
		bar.setOpaque(false);
		bar.add(tool("Fit", null, "Fit the image in the window (F)", () -> canvas.fit()));
		bar.add(tool("100%", null, "Actual size, one image pixel per screen pixel (1)", () -> canvas.actualSize()));
		bar.add(tool("-", null, "Zoom out (Ctrl+-)", () -> canvas.zoomBy(1 / 1.25, null)));
		bar.add(tool("+", null, "Zoom in (Ctrl++)", () -> canvas.zoomBy(1.25, null)));
		bar.add(new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL), "growy, gapx 6 6, hmin 16");
		bar.add(tool("Copy", ToolIcons.Kind.COPY, "Copy the image shown to the clipboard (Ctrl+C)", this::copyToClipboard));
		if (!readOnly) {
			bar.add(tool("Paste", ToolIcons.Kind.PASTE, "Open the image in the clipboard (Ctrl+V)", this::pasteFromClipboard));
			btnCompare = new javax.swing.JToggleButton("Before");
			btnCompare.putClientProperty("JButton.buttonType", "toolBarButton");
			btnCompare.setMargin(new java.awt.Insets(3, 8, 3, 8));
			btnCompare.setToolTipText("Show the image without the pending edits (B)");
			ToolStyle.hand(btnCompare);
			btnCompare.addActionListener(e -> {
				comparing = btnCompare.isSelected();
				if (base != null) {
					canvas.setImage(comparing ? base : current(), false);
				}
			});
			bar.add(btnCompare, "gapleft 4");
		}
		bar.add(new JLabel(), "growx, pushx, wmin 0");
		final JLabel caption = ToolStyle.mutedLabel("Background");
		bar.add(caption);
		final JComboBox<String> background = new JComboBox<String>(new String[] {"Checker", "Dark", "Gray", "Light", "Black", "White"});
		ToolStyle.hand(background);
		background.addActionListener(e -> canvas.setBackgroundStyle((String) background.getSelectedItem()));
		bar.add(background, "wmin 0");
		// narrow window or tab: the background choice gives way, the bar never overflows
		bar.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				int width = bar.getWidth();
				caption.setVisible(width > 540);
				background.setVisible(width > 420);
			}
		});
		return bar;
	}

	private JButton tool(String text, ToolIcons.Kind icon, String tooltip, final Runnable run) {
		JButton button = ToolStyle.button(text, icon, tooltip);
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		button.setMargin(new java.awt.Insets(3, 8, 3, 8));
		button.addActionListener(e -> run.run());
		return button;
	}

	/** Right column: the edit panel and the actions (studio), or details and the way to the studio (read-only viewer) */
	private JPanel buildSidebar() {
		JPanel side = new JPanel(new MigLayout("insets 0, wrap 1, gapy 8, hidemode 3", "[grow,fill]", "[grow,fill][]"));
		side.setOpaque(false);
		if (readOnly) {
			JPanel holder = new ImageSettingsPanel.ScrollableHolder(new MigLayout("insets 0 0 0 6, aligny top", "[grow,fill]", ""));
			holder.setOpaque(false);
			holder.add(infoAccordion, "growx");
			JScrollPane scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.setBorder(null);
			scroll.setOpaque(false);
			scroll.getViewport().setOpaque(false);
			scroll.getVerticalScrollBar().setUnitIncrement(16);
			side.add(scroll, "grow");
			JButton edit = ToolStyle.accent(ToolStyle.button("Edit in Image Studio", null, "Open this image in the Image Studio window: edits, filters, resize and export"), ToolStyle.BLUE);
			edit.setEnabled(file != null);
			edit.addActionListener(e -> {
				ImageViewerFrame.loadIntoLastExistingInstance(file);
				if (closeHandler != null) {
					Object[] options = {"Keep tab", "Close tab"};
					int choice = JOptionPane.showOptionDialog(this, "The image is open in the Image Studio window.\nClose this tab?", "Edit in Image Studio",
							JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					if (choice == 1) {
						closeHandler.run();
					}
				}
			});
			side.add(edit, "growx, h 32!");
		} else {
			side.add(settingsPanel, "grow");
			side.add(buildActions(), "growx");
		}
		return side;
	}

	private JPanel buildActions() {
		JPanel actions = new JPanel(new MigLayout("insets 0, gapx 6, gapy 6", "[grow,fill,sg a][grow,fill,sg a][grow,fill,sg a]", ""));
		actions.setOpaque(false);
		actions.add(new javax.swing.JSeparator(), "span 3, growx, wrap");
		JButton export = ToolStyle.accent(ToolStyle.button("Export...", null, "Write the result in any format, with the quality set above (Ctrl+Shift+S)"), ToolStyle.GREEN);
		export.addActionListener(e -> exportAs());
		actions.add(export, "span 3, growx, h 32!, wrap");
		btnApply = ToolStyle.button("Apply", null, "Bake the adjustments, filters, rotation and resize in the image (with undo)");
		btnApply.addActionListener(e -> applyEdits());
		btnUndo = ToolStyle.button("Undo", null, "Undo the last applied edit (Ctrl+Z)");
		btnUndo.addActionListener(e -> undoLast());
		btnRevert = ToolStyle.button("Revert", null, "Discard every edit and reload the original image");
		btnRevert.addActionListener(e -> revertToOriginal());
		actions.add(btnApply, "wmin 0");
		actions.add(btnUndo, "wmin 0");
		actions.add(btnRevert, "wmin 0");
		updateActions();
		return actions;
	}

	/** Apply, Undo and Revert are on only when they have something to do */
	private void updateActions() {
		if (btnApply == null) {
			return;
		}
		btnApply.setEnabled(base != null && !settings.isIdentity());
		btnUndo.setEnabled(!undo.isEmpty());
		btnRevert.setEnabled(original != null && (!undo.isEmpty() || !settings.isIdentity()));
	}

	private void addInfoSection(String title, JComponent content, StudioMode minimumMode, boolean expanded) {
		if (readOnly) {
			infoAccordion.addSection(title, content, StudioMode.BASIC, expanded);
		} else {
			settingsPanel.addExtraSection(title, content, minimumMode, expanded);
		}
	}

	private void buildExtraSections() {
		JPanel details = new JPanel(new MigLayout("insets 0, wrap 1, gapy 6", "[grow,fill]", ""));
		details.setOpaque(false);
		lblInfo.setFont(ToolStyle.mono(-1f));
		details.add(lblInfo, "wmin 0");
		details.add(histogram, "h 90!");
		JButton metadata = ToolStyle.button("Image metadata...", null, "Format and metadata read by ImageIO");
		metadata.addActionListener(e -> showMetadata());
		details.add(metadata);
		btnPsdSettings = ToolStyle.button("Camera Raw settings of the PSD...", null, "The XMP of this Photoshop file holds Camera Raw settings: read them as Image Studio settings");
		btnPsdSettings.setVisible(false);
		btnPsdSettings.addActionListener(e -> usePsdSettings());
		if (!readOnly) {
			details.add(btnPsdSettings);
		}
		addInfoSection("Details & histogram", details, StudioMode.NERD, true);

		JPanel view = new JPanel(new MigLayout("insets 0, wrap 1, gapy 4", "[grow,fill]", ""));
		view.setOpaque(false);
		final JComboBox<String> interpolation = new JComboBox<String>(new String[] {"Smooth (bilinear)", "Best (bicubic)", "Pixels (nearest)"});
		ToolStyle.hand(interpolation);
		interpolation.addActionListener(e -> canvas.setInterpolation(interpolation.getSelectedIndex()));
		view.add(interpolation, "wmin 0");
		final JCheckBox grid = new JCheckBox("Pixel grid (800% zoom and up)");
		ToolStyle.hand(grid);
		grid.addActionListener(e -> canvas.setGrid(grid.isSelected()));
		view.add(grid, "wmin 0");
		addInfoSection("View", view, StudioMode.NERD, true);
		addMetadataSection();
	}

	private void applyMode() {
		if (readOnly) {
			toolsBar.setVisible(true);
			lblPixel.setVisible(true);
			canvas.setPixelReadout(true);
			return;
		}
		StudioMode mode = StudioMode.current();
		toolsBar.setVisible(mode != StudioMode.BASIC);
		lblPixel.setVisible(mode != StudioMode.BASIC);
		canvas.setPixelReadout(mode != StudioMode.BASIC);
		settingsPanel.applyMode(mode);
		revalidate();
	}

	private void bindKeys() {
		key(java.awt.event.KeyEvent.VK_F, 0, "fit", () -> canvas.fit());
		key(java.awt.event.KeyEvent.VK_1, 0, "actual", () -> canvas.actualSize());
		key(java.awt.event.KeyEvent.VK_EQUALS, ShortcutKeys.MENU_DOWN_MASK, "zin", () -> canvas.zoomBy(1.25, null));
		key(java.awt.event.KeyEvent.VK_ADD, ShortcutKeys.MENU_DOWN_MASK, "zin2", () -> canvas.zoomBy(1.25, null));
		key(java.awt.event.KeyEvent.VK_MINUS, ShortcutKeys.MENU_DOWN_MASK, "zout", () -> canvas.zoomBy(1 / 1.25, null));
		key(java.awt.event.KeyEvent.VK_SUBTRACT, ShortcutKeys.MENU_DOWN_MASK, "zout2", () -> canvas.zoomBy(1 / 1.25, null));
		key(java.awt.event.KeyEvent.VK_C, ShortcutKeys.MENU_DOWN_MASK, "copy", this::copyToClipboard);
		if (readOnly) {
			return;
		}
		key(java.awt.event.KeyEvent.VK_B, 0, "compare", () -> btnCompare.doClick());
		key(java.awt.event.KeyEvent.VK_R, 0, "rotR", () -> settingsPanel.rotate(90));
		key(java.awt.event.KeyEvent.VK_L, 0, "rotL", () -> settingsPanel.rotate(270));
		key(java.awt.event.KeyEvent.VK_H, 0, "flipH", () -> settingsPanel.flip(true));
		key(java.awt.event.KeyEvent.VK_V, 0, "flipV", () -> settingsPanel.flip(false));
		key(java.awt.event.KeyEvent.VK_V, ShortcutKeys.MENU_DOWN_MASK, "paste", this::pasteFromClipboard);
		key(java.awt.event.KeyEvent.VK_Z, ShortcutKeys.MENU_DOWN_MASK, "undo", this::undoLast);
		key(java.awt.event.KeyEvent.VK_S, ShortcutKeys.MENU_DOWN_MASK, "save", () -> save());
		key(java.awt.event.KeyEvent.VK_S, ShortcutKeys.MENU_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK, "export", this::exportAs);
	}

	private void key(int code, int modifiers, String name, final Runnable run) {
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(code, modifiers), name);
		getActionMap().put(name, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Component focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				if (modifiers == 0 && focus instanceof javax.swing.text.JTextComponent) {
					return;
				}
				run.run();
			}
		});
	}

	// ------------------------------------------------------------------ loading

	private void loadFile(final File source) {
		lblStatus.setText("Loading " + source.getName() + "...");
		new SwingWorker<BufferedImage, Void>() {
			@Override
			protected BufferedImage doInBackground() throws Exception {
				if (PsdReader.isPsd(source)) {
					psdInfo = PsdReader.read(source);
					return psdInfo.image;
				}
				return ImageIO.read(source);
			}

			@Override
			protected void done() {
				try {
					BufferedImage read = get();
					if (read == null) {
						throw new IOException("Format ." + fileExtension(source) + " is not supported by this Java");
					}
					canvas.setMessage(null);
					setBaseImage(read, true);
					original = read;
					String ext = fileExtension(source);
					if (ImageEdit.writableFormats().contains(ext)) {
						settings.format = ext;
						settingsPanel.syncFromSettings();
					}
					refreshInfo();
					refreshMetadataSummary();
					if (psdInfo != null) {
						if (!psdInfo.note.isEmpty()) {
							lblStatus.setText(lblStatus.getText() + "  -  " + psdInfo.note.split("\n")[0]);
						}
						if (btnPsdSettings != null) {
							btnPsdSettings.setVisible(psdInfo.hasCameraRaw());
						}
					}
				} catch (Exception e) {
					logger.error("Cannot load image " + source, e);
					canvas.setImage(null, true);
					String why = e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
					canvas.setMessage("This file cannot be shown\n" + (why == null ? "" : why));
					lblStatus.setText("Cannot load the image: " + source.getName());
				}
			}
		}.execute();
	}

	private static String fileExtension(File f) {
		String name = f.getName().toLowerCase(Locale.ROOT);
		int dot = name.lastIndexOf('.');
		String ext = dot < 0 ? "" : name.substring(dot + 1);
		return ext.equals("jpeg") ? "jpg" : ext.equals("tif") ? "tiff" : ext;
	}

	private void setBaseImage(BufferedImage image, boolean refit) {
		base = image;
		preview = null;
		settingsPanel.setBaseSize(image.getWidth(), image.getHeight());
		if (!readOnly) {
			settingsPanel.setThumbnailSource(image);
		}
		canvas.setImage(image, refit);
		histogram.setImage(image);
		scheduleEstimate();
		refreshInfo();
		fireChanged();
	}

	/** The image as it would be exported: base with the pending edits */
	private BufferedImage current() {
		return preview != null ? preview : base;
	}

	private void refreshInfo() {
		BufferedImage shown = current();
		if (shown == null) {
			lblInfo.setText("-");
			return;
		}
		int w = shown.getWidth();
		int h = shown.getHeight();
		String size = file != null ? ConversionUtils.coolFileSize(file) : "-";
		lblInfo.setText("<html>" + w + " x " + h + " px (" + String.format(Locale.ROOT, "%.2f", w * (double) h / 1e6) + " MP)<br>"
				+ "File: " + (file != null ? fileExtension(file).toUpperCase(Locale.ROOT) : "-") + ", " + size + "<br>"
				+ "Alpha: " + (shown.getColorModel().hasAlpha() ? "yes" : "no") + ", " + shown.getColorModel().getPixelSize() + " bit<br>"
				+ (isModified() ? "<font color='#d68c26'>edited, not saved</font>" : "unchanged") + "</html>");
		lblStatus.setText((file != null ? file.getName() : title) + "  -  " + w + " x " + h + " px  -  " + size);
	}

	// ------------------------------------------------------------------ preview

	private void onSettingsChanged() {
		previewTimer.restart();
		scheduleEstimate();
		fireChanged();
	}

	private void renderPreview() {
		if (base == null) {
			return;
		}
		final BufferedImage source = base;
		final ImageSettings snapshot = settings.copy();
		final int sequence = ++previewSequence;
		if (snapshot.isIdentity()) {
			preview = null;
			canvas.setImage(base, false);
			refreshInfo();
			return;
		}
		new SwingWorker<BufferedImage, Void>() {
			@Override
			protected BufferedImage doInBackground() {
				return ImageEdit.apply(source, snapshot);
			}

			@Override
			protected void done() {
				if (sequence != previewSequence) {
					return; // a newer render is on its way
				}
				try {
					preview = get();
					canvas.setImage(comparing ? base : preview, false);
					histogram.setImage(preview);
					refreshInfo();
				} catch (Exception e) {
					logger.error("Preview failed", e);
					lblStatus.setText("Preview failed: " + e.getMessage());
				}
			}
		}.execute();
	}

	private void scheduleEstimate() {
		if (readOnly) {
			return;
		}
		estimateTimer.restart();
	}

	/** Estimated size of the exported file, encoded in the background */
	private void renderEstimate() {
		final BufferedImage shown = current();
		if (shown == null) {
			return;
		}
		final ImageSettings snapshot = settings.copy();
		final int sequence = ++estimateSequence;
		settingsPanel.setEstimate("Estimating file size...");
		new SwingWorker<Integer, Void>() {
			@Override
			protected Integer doInBackground() throws Exception {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageEdit.write(shown, out, snapshot);
				return out.size();
			}

			@Override
			protected void done() {
				if (sequence != estimateSequence) {
					return;
				}
				try {
					settingsPanel.setEstimate("Output: " + shown.getWidth() + " x " + shown.getHeight() + " px, about " + ConversionUtils.coolFileSize((long) get()));
				} catch (Exception e) {
					settingsPanel.setEstimate("This format cannot be written: " + snapshot.format);
				}
			}
		}.execute();
	}

	// ------------------------------------------------------------------ edits

	/** Bakes the pending edits in the image; the output choices (format, quality) stay */
	public void applyEdits() {
		if (base == null || settings.isIdentity()) {
			return;
		}
		BufferedImage result = preview != null ? preview : ImageEdit.apply(base, settings);
		undo.addLast(base);
		while (undo.size() > MAX_UNDO) {
			undo.removeFirst();
		}
		settings.resetEdits();
		settingsPanel.syncFromSettings();
		setBaseImage(result, false);
	}

	public void undoLast() {
		if (undo.isEmpty()) {
			return;
		}
		settings.resetEdits();
		settingsPanel.syncFromSettings();
		setBaseImage(undo.removeLast(), false);
	}

	public void revertToOriginal() {
		if (original == null) {
			return;
		}
		undo.clear();
		settings.resetEdits();
		settingsPanel.syncFromSettings();
		setBaseImage(original, false);
	}

	// ------------------------------------------------------------------ clipboard, files

	private void copyToClipboard() {
		final BufferedImage shown = current();
		if (shown == null) {
			return;
		}
		try {
			java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
				@Override
				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[] {DataFlavor.imageFlavor};
				}

				@Override
				public boolean isDataFlavorSupported(DataFlavor flavor) {
					return DataFlavor.imageFlavor.equals(flavor);
				}

				@Override
				public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
					if (!isDataFlavorSupported(flavor)) {
						throw new UnsupportedFlavorException(flavor);
					}
					return shown;
				}
			}, null);
			lblStatus.setText("Image copied to the clipboard");
		} catch (IllegalStateException e) {
			lblStatus.setText("The clipboard is busy, try again");
		}
	}

	private void pasteFromClipboard() {
		try {
			Transferable data = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if (data != null && data.isDataFlavorSupported(DataFlavor.imageFlavor)) {
				java.awt.Image image = (java.awt.Image) data.getTransferData(DataFlavor.imageFlavor);
				BufferedImage buffered = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = buffered.createGraphics();
				g.drawImage(image, 0, 0, null);
				g.dispose();
				file = null;
				pasted = true;
				title = "Clipboard image";
				original = buffered;
				undo.clear();
				settings.resetEdits();
				settingsPanel.syncFromSettings();
				setBaseImage(buffered, true);
			} else {
				lblStatus.setText("No image in the clipboard");
			}
		} catch (Exception e) {
			logger.error("Cannot paste the clipboard image", e);
			lblStatus.setText("Cannot read the clipboard image");
		}
	}

	/** Writes the shown result to a file chosen by the user, in the format of the export settings */
	public void exportAs() {
		BufferedImage result = currentResult();
		if (result == null) {
			return;
		}
		NativeFileChooser chooser = new NativeFileChooser();
		String baseName = file != null ? file.getName().replaceAll("\\.[^.]+$", "") : "image";
		File dir = file != null ? file.getParentFile() : new File(".");
		chooser.setSelectedFile(new File(dir, baseName + "_edit." + settings.format));
		chooser.setDialogTitle("Export image");
		if (chooser.showDialog(this, "Export") != NativeFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
			return;
		}
		File target = chooser.getSelectedFile();
		String chosenExt = fileExtension(target);
		ImageSettings out = settings.copy();
		if (ImageEdit.writableFormats().contains(chosenExt)) {
			out.format = chosenExt; // the extension typed by the user wins
		} else {
			target = new File(target.getParentFile(), target.getName() + "." + settings.format);
		}
		if (target.exists() && JOptionPane.showConfirmDialog(this, "Overwrite " + target.getName() + "?", "File exists", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
			return;
		}
		try {
			ImageEdit.write(result, target, out);
			lblStatus.setText("Exported: " + target.getAbsolutePath() + " (" + ConversionUtils.coolFileSize(target) + ")");
		} catch (IOException e) {
			logger.error("Cannot export the image", e);
			lblStatus.setText("Cannot export the image: " + e.getMessage());
		}
	}

	/** Base image with every pending edit, rendered now (the preview may still be on its way) */
	private BufferedImage currentResult() {
		if (base == null) {
			return null;
		}
		return settings.isIdentity() ? base : ImageEdit.apply(base, settings);
	}

	/** Accordion section "Metadata": what the file carries that can be removed, and the same removal tools as the window header */
	private void addMetadataSection() {
		JPanel meta = new JPanel(new MigLayout("insets 0, wrap 1, gapy 4", "[grow,fill]", ""));
		meta.setOpaque(false);
		lblMetaSummary = new JLabel("-");
		lblMetaSummary.setFont(ToolStyle.font(-1f, java.awt.Font.PLAIN));
		meta.add(lblMetaSummary, "wmin 0");
		JButton show = ToolStyle.button("Show all metadata...", null, "Every tag the file carries (you can also clean from there)");
		show.addActionListener(e -> showMetadata());
		JButton copy = ToolStyle.button("Save a clean copy...", null, "A copy of the file without metadata; the original stays");
		copy.addActionListener(e -> saveCleanCopy());
		JButton remove = ToolStyle.button("Remove from this file...", null, "Removes the metadata from the file itself (asks first)");
		remove.addActionListener(e -> cleanThisFile());
		JButton tool = ToolStyle.button("Cleaning tool...", null, "Remove the metadata from many files: folders, filters, drag and drop");
		tool.addActionListener(e -> new MetadataCleanerDialog(SwingUtilities.getWindowAncestor(this), file).setVisible(true));
		for (JButton b : new JButton[] {show, copy, remove, tool}) {
			meta.add(b, "wmin 0");
		}
		addInfoSection("Metadata", meta, StudioMode.BASIC, false);
	}

	/** Fills the summary of the Metadata section in the background */
	private void refreshMetadataSummary() {
		if (lblMetaSummary == null || file == null) {
			return;
		}
		final File target = file;
		new SwingWorker<MetadataStripper.Result, Void>() {
			@Override
			protected MetadataStripper.Result doInBackground() throws Exception {
				return MetadataStripper.analyze(target);
			}

			@Override
			protected void done() {
				String text;
				try {
					MetadataStripper.Result r = get();
					if (!r.supported) {
						text = "This format cannot be cleaned directly.";
					} else if (r.removed.isEmpty()) {
						text = "No metadata to remove.";
					} else {
						StringBuilder t = new StringBuilder("<html><body style='width:190px'>");
						for (String item : r.removed) {
							t.append("&bull; ").append(item.replace("&", "&amp;").replace("<", "&lt;")).append("<br>");
						}
						text = t.append("</body></html>").toString();
					}
				} catch (Exception e) {
					text = "Metadata not readable.";
				}
				lblMetaSummary.setText(text);
			}
		}.execute();
	}

	/** Settings stored in the XMP of a PSD: read them (after a warning, the picture already contains these edits) */
	private void usePsdSettings() {
		if (psdInfo == null || !psdInfo.hasCameraRaw()) {
			return;
		}
		Object[] options = {"Cancel", "Read the settings"};
		int choice = JOptionPane.showOptionDialog(this, "<html>The merged picture of a Photoshop file already shows these Camera Raw edits:<br>applying them again would double the effect.<br>"
				+ "Use them to see what was done, or to copy the look to another image.</html>", "Camera Raw settings of the PSD", JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (choice != 1) {
			return;
		}
		try {
			settingsPanel.applyAdobeSettings(PhotoshopPresets.parseXmp(psdInfo.xmp), "Photoshop file " + (file != null ? file.getName() : ""));
		} catch (IOException e) {
			lblStatus.setText("Cannot read the Camera Raw settings: " + e.getMessage());
		}
	}

	private void showMetadata() {
		if (file == null) {
			return;
		}
		StringBuilder text = new StringBuilder();
		text.append("File: ").append(file.getAbsolutePath()).append('\n');
		MetadataStripper.Result removable = null;
		try {
			removable = MetadataStripper.analyze(file);
		} catch (IOException | RuntimeException e) {
			// shown below as not readable
		}
		if (removable != null && removable.supported) {
			text.append("\nRemovable metadata: ").append(removable.removed.isEmpty() ? "none" : "").append('\n');
			for (String item : removable.removed) {
				text.append("  - ").append(item).append('\n');
			}
		}
		if (PsdReader.isPsd(file)) {
			try {
				text.append('\n').append(PsdReader.readInfo(file).describe());
			} catch (IOException e) {
				text.append("\nPhotoshop information not readable: ").append(e.getMessage());
			}
		} else {
			try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
				Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
				if (readers.hasNext()) {
					ImageReader reader = readers.next();
					text.append("\nFormat: ").append(reader.getFormatName()).append('\n');
					reader.setInput(in);
					javax.imageio.metadata.IIOMetadata meta = reader.getImageMetadata(0);
					if (meta != null) {
						for (String format : meta.getMetadataFormatNames()) {
							text.append("\n[").append(format).append("]\n");
							dump(meta.getAsTree(format), 0, text);
						}
					}
					reader.dispose();
				}
			} catch (Exception e) {
				text.append("\nMetadata not readable: ").append(e.getMessage());
			}
		}
		JTextArea area = new JTextArea(text.toString(), 22, 70);
		area.setEditable(false);
		area.setFont(ToolStyle.mono(0f));
		area.setCaretPosition(0);
		Object[] options = {"Save a clean copy...", "Remove from this file...", "Cleaning tool...", "Close"};
		int choice = JOptionPane.showOptionDialog(this, new JScrollPane(area), "Image metadata", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[3]);
		if (choice == 0) {
			saveCleanCopy();
		} else if (choice == 1) {
			cleanThisFile();
		} else if (choice == 2) {
			new MetadataCleanerDialog(SwingUtilities.getWindowAncestor(this), file).setVisible(true);
		}
	}

	/** A copy of the file without metadata (everything, keeping the color profile and the orientation), the original stays */
	private void saveCleanCopy() {
		try {
			MetadataStripper.Result r = MetadataStripper.processFile(file, new MetadataStripper.Options());
			if (!r.supported) {
				JOptionPane.showMessageDialog(this, "The metadata of this format cannot be removed without re-saving the image.\nUse the cleaning tool with \"re-save\" ticked.", "Clean metadata", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			NativeFileChooser chooser = new NativeFileChooser();
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			chooser.setSelectedFile(new File(file.getParentFile(), (dot < 0 ? name : name.substring(0, dot)) + "_clean" + (dot < 0 ? "" : name.substring(dot))));
			chooser.setDialogTitle("Save a clean copy");
			if (chooser.showDialog(this, "Save") != NativeFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
				return;
			}
			File target = chooser.getSelectedFile();
			if (target.exists() && JOptionPane.showConfirmDialog(this, "Overwrite " + target.getName() + "?", "File exists", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
				return;
			}
			MetadataStripper.writeSafely(r.changed() ? r.output : java.nio.file.Files.readAllBytes(file.toPath()), target);
			lblStatus.setText((r.changed() ? "Clean copy saved: " : "No metadata to remove, copy saved: ") + target.getAbsolutePath());
		} catch (IOException | RuntimeException e) {
			logger.error("Cannot save the clean copy", e);
			lblStatus.setText("Cannot save the clean copy: " + e.getMessage());
		}
	}

	/** Removes the metadata from the file itself, after a confirmation (cancel is the default) */
	private void cleanThisFile() {
		try {
			MetadataStripper.Result r = MetadataStripper.processFile(file, new MetadataStripper.Options());
			if (!r.supported) {
				JOptionPane.showMessageDialog(this, "The metadata of this format cannot be removed without re-saving the image.\nUse the cleaning tool with \"re-save\" ticked.", "Clean metadata", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			if (!r.changed()) {
				JOptionPane.showMessageDialog(this, "There is nothing to remove from this file.", "Clean metadata", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			Object[] options = {"Cancel", "Remove the metadata"};
			int choice = JOptionPane.showOptionDialog(this, "<html>The metadata will be removed <b>from " + file.getName() + " itself</b> (the picture is not touched).<br>This cannot be undone.</html>",
					"Clean this file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			if (choice != 1) {
				return;
			}
			long modified = file.lastModified();
			MetadataStripper.writeSafely(r.output, file);
			file.setLastModified(modified);
			lblStatus.setText("Metadata removed from " + file.getName() + " (" + ConversionUtils.coolFileSize(file) + ")");
			refreshInfo();
			refreshMetadataSummary();
		} catch (IOException | RuntimeException e) {
			logger.error("Cannot clean the file", e);
			lblStatus.setText("Cannot clean the file: " + e.getMessage());
		}
	}

	private static void dump(org.w3c.dom.Node node, int depth, StringBuilder out) {
		for (int i = 0; i < depth; i++) {
			out.append("  ");
		}
		out.append(node.getNodeName());
		org.w3c.dom.NamedNodeMap attributes = node.getAttributes();
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				org.w3c.dom.Node attribute = attributes.item(i);
				String value = attribute.getNodeValue();
				out.append(' ').append(attribute.getNodeName()).append("=\"").append(value.length() > 120 ? value.substring(0, 120) + "..." : value).append('"');
			}
		}
		out.append('\n');
		for (org.w3c.dom.Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			dump(child, depth + 1, out);
		}
	}

	// ------------------------------------------------------------------ TabContent

	@Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public boolean isModified() {
		return !readOnly && (!undo.isEmpty() || !settings.isIdentity() || pasted);
	}

	/** Saves the result over the original file, in its own format (after asking); a pasted image goes to Export */
	@Override
	public boolean save() {
		if (readOnly || base == null) {
			return true;
		}
		if (file == null || !ImageEdit.writableFormats().contains(fileExtension(file))) {
			exportAs();
			return !isModified() || file != null;
		}
		if (JOptionPane.showConfirmDialog(this, "Overwrite " + file.getName() + " with the edited image?", "Save image", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
			return false;
		}
		try {
			ImageSettings out = settings.copy();
			out.format = fileExtension(file);
			ImageEdit.write(currentResult(), file, out);
			// the file now holds the edits: they become the new original
			BufferedImage saved = currentResult();
			undo.clear();
			settings.resetEdits();
			settingsPanel.syncFromSettings();
			original = saved;
			setBaseImage(saved, false);
			lblStatus.setText("Saved: " + file.getAbsolutePath());
			return true;
		} catch (IOException e) {
			logger.error("Cannot save the image", e);
			lblStatus.setText("Cannot save the image: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean confirmClose(Component parent) {
		if (readOnly) {
			return true;
		}
		if (!settingsPanel.confirmPresetOnClose(parent)) {
			return false;
		}
		if (!isModified()) {
			return true;
		}
		Object[] options = {"Save", "Discard", "Cancel"};
		int choice = JOptionPane.showOptionDialog(parent, "\"" + title + "\" has edits that were not saved.", "Unsaved image",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		if (choice == 0) {
			return save();
		}
		return choice == 1;
	}

	@Override
	public void onSelected() {
		canvas.requestFocusInWindow();
	}

	@Override
	public void dispose() {
		if (modeListener != null) {
			StudioMode.removeListener(modeListener);
		}
		previewTimer.stop();
		estimateTimer.stop();
		previewSequence++;
		estimateSequence++;
		undo.clear();
		base = null;
		preview = null;
		original = null;
	}

	@Override
	public void setCloseHandler(Runnable closeTab) {
		this.closeHandler = closeTab;
	}

	@Override
	public void setChangeListener(Runnable listener) {
		this.changeListener = listener;
	}

	private void fireChanged() {
		updateActions();
		if (changeListener != null) {
			changeListener.run();
		}
	}

	/** Used by the window host to show the image of a file already open in another place */
	public void reload() {
		if (file != null) {
			loadFile(file);
		}
	}
}
