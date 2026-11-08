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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import gui.commons.dialogutils.NativeFileChooser;
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

	public ImageStudioPanel(File imageFile) {
		super(new MigLayout("insets 6, gapx 10, gapy 6, hidemode 3", "[grow,fill][300!]", "[][grow,fill][]"));
		this.file = imageFile;
		if (imageFile != null) {
			this.title = imageFile.getName();
		}

		settingsPanel = new ImageSettingsPanel(settings, this::onSettingsChanged);
		toolsBar = buildToolsBar();
		buildExtraSections();

		add(toolsBar, "growx");
		JPanel actions = buildActions();
		add(actions, "growx, wrap");
		add(canvas, "grow");
		add(settingsPanel, "grow, wrap");

		JPanel status = new JPanel(new MigLayout("insets 0, gapx 16", "[grow,fill][][]", "[]"));
		status.setOpaque(false);
		status.add(lblStatus);
		status.add(lblPixel);
		status.add(lblZoom);
		add(status, "span 2, growx");

		canvas.setPixelListener(lblPixel::setText);
		canvas.setZoomListener(lblZoom::setText);

		previewTimer = new Timer(PREVIEW_DELAY_MS, e -> renderPreview());
		previewTimer.setRepeats(false);
		estimateTimer = new Timer(ESTIMATE_DELAY_MS, e -> renderEstimate());
		estimateTimer.setRepeats(false);

		modeListener = () -> SwingUtilities.invokeLater(this::applyMode);
		StudioMode.addListener(modeListener);
		applyMode();
		bindKeys();

		if (imageFile != null) {
			loadFile(imageFile);
		}
	}

	// ------------------------------------------------------------------ UI

	private JPanel buildToolsBar() {
		JPanel bar = new JPanel(new MigLayout("insets 0, gapx 6, hidemode 3", "", "[]"));
		bar.setOpaque(false);
		bar.add(tool("Fit", "Fit the image in the window (F)", () -> canvas.fit()));
		bar.add(tool("100%", "Actual size, one image pixel per screen pixel (1)", () -> canvas.actualSize()));
		bar.add(tool("-", "Zoom out (Ctrl+-)", () -> canvas.zoomBy(1 / 1.25, null)), "gapleft 6");
		bar.add(tool("+", "Zoom in (Ctrl++)", () -> canvas.zoomBy(1.25, null)));
		bar.add(tool("Copy", "Copy the image shown to the clipboard (Ctrl+C)", this::copyToClipboard), "gapleft 6");
		bar.add(tool("Paste", "Open the image in the clipboard (Ctrl+V)", this::pasteFromClipboard));
		bar.add(new JLabel(), "growx, pushx");
		bar.add(ToolStyle.mutedLabel("Background"));
		final JComboBox<String> background = new JComboBox<String>(new String[] {"Checker", "Dark", "Gray", "Light", "Black", "White"});
		ToolStyle.hand(background);
		background.addActionListener(e -> canvas.setBackgroundStyle((String) background.getSelectedItem()));
		bar.add(background);
		return bar;
	}

	private JButton tool(String text, String tooltip, final Runnable run) {
		JButton button = ToolStyle.button(text, null, tooltip);
		button.setMargin(new java.awt.Insets(3, 9, 3, 9));
		button.addActionListener(e -> run.run());
		return button;
	}

	private JPanel buildActions() {
		JPanel actions = new JPanel(new MigLayout("insets 0, gapx 6, gapy 4, wrap 2", "[grow,fill][grow,fill]", "[]"));
		actions.setOpaque(false);
		JButton apply = ToolStyle.button("Apply edits", null, "Bake the adjustments, filters, rotation and resize in the image (with undo)");
		apply.addActionListener(e -> applyEdits());
		btnUndo = ToolStyle.button("Undo", null, "Undo the last applied edit (Ctrl+Z)");
		btnUndo.addActionListener(e -> undoLast());
		JButton revert = ToolStyle.button("Revert", null, "Discard every edit and reload the original image");
		revert.addActionListener(e -> revertToOriginal());
		JButton export = ToolStyle.accent(ToolStyle.button("Export...", null, "Write the result in any format, with the quality set on the right (Ctrl+Shift+S)"), ToolStyle.GREEN);
		export.addActionListener(e -> exportAs());
		actions.add(apply);
		actions.add(btnUndo);
		actions.add(revert);
		actions.add(export);
		return actions;
	}

	private void buildExtraSections() {
		JPanel details = new JPanel(new MigLayout("insets 0, wrap 1, gapy 6", "[grow,fill]", ""));
		details.setOpaque(false);
		lblInfo.setFont(ToolStyle.mono(-1f));
		details.add(lblInfo);
		details.add(histogram, "h 90!");
		JButton metadata = ToolStyle.button("Image metadata...", null, "Format and metadata read by ImageIO");
		metadata.addActionListener(e -> showMetadata());
		details.add(metadata);
		settingsPanel.addExtraSection("Details & histogram", details, StudioMode.NERD, true);

		JPanel view = new JPanel(new MigLayout("insets 0, wrap 1, gapy 4", "[grow,fill]", ""));
		view.setOpaque(false);
		final JComboBox<String> interpolation = new JComboBox<String>(new String[] {"Smooth (bilinear)", "Best (bicubic)", "Pixels (nearest)"});
		ToolStyle.hand(interpolation);
		interpolation.addActionListener(e -> canvas.setInterpolation(interpolation.getSelectedIndex()));
		view.add(interpolation);
		final JCheckBox grid = new JCheckBox("Pixel grid (zoom 800% or more)");
		ToolStyle.hand(grid);
		grid.addActionListener(e -> canvas.setGrid(grid.isSelected()));
		view.add(grid);
		settingsPanel.addExtraSection("View", view, StudioMode.NERD, false);
	}

	private void applyMode() {
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
		key(java.awt.event.KeyEvent.VK_R, 0, "rotR", () -> settingsPanel.rotate(90));
		key(java.awt.event.KeyEvent.VK_L, 0, "rotL", () -> settingsPanel.rotate(270));
		key(java.awt.event.KeyEvent.VK_H, 0, "flipH", () -> settingsPanel.flip(true));
		key(java.awt.event.KeyEvent.VK_V, 0, "flipV", () -> settingsPanel.flip(false));
		key(java.awt.event.KeyEvent.VK_C, ShortcutKeys.MENU_DOWN_MASK, "copy", this::copyToClipboard);
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
				return ImageIO.read(source);
			}

			@Override
			protected void done() {
				try {
					BufferedImage read = get();
					if (read == null) {
						throw new IOException("Unsupported image format");
					}
					setBaseImage(read, true);
					original = read;
					String ext = fileExtension(source);
					if (ImageEdit.writableFormats().contains(ext)) {
						settings.format = ext;
						settingsPanel.syncFromSettings();
					}
					refreshInfo();
				} catch (Exception e) {
					logger.error("Cannot load image " + source, e);
					canvas.setImage(null, true);
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
					canvas.setImage(preview, false);
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

	private void showMetadata() {
		if (file == null) {
			return;
		}
		StringBuilder text = new StringBuilder();
		text.append("File: ").append(file.getAbsolutePath()).append('\n');
		try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (readers.hasNext()) {
				ImageReader reader = readers.next();
				text.append("Format: ").append(reader.getFormatName()).append('\n');
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
		JTextArea area = new JTextArea(text.toString(), 22, 70);
		area.setEditable(false);
		area.setFont(ToolStyle.mono(0f));
		area.setCaretPosition(0);
		JOptionPane.showMessageDialog(this, new JScrollPane(area), "Image metadata", JOptionPane.PLAIN_MESSAGE);
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
		return !undo.isEmpty() || !settings.isIdentity() || pasted;
	}

	/** Saves the result over the original file, in its own format (after asking); a pasted image goes to Export */
	@Override
	public boolean save() {
		if (base == null) {
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
		StudioMode.removeListener(modeListener);
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
	public void setChangeListener(Runnable listener) {
		this.changeListener = listener;
	}

	private void fireChanged() {
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
