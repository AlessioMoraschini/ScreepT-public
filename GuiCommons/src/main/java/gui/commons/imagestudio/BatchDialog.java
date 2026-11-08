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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import gui.commons.dialogutils.NativeFileChooser;
import gui.commons.frameutils.frame.arch.ToolStyle;
import net.miginfocom.swing.MigLayout;
import various.common.light.gui.dnd.FileDrop;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;

/**
 * Batch processing: the settings are chosen on one sample image (the built-in one, generated once and shipped with
 * the application, or one of your photos), then applied to every image of the list: convert, resize and every edit of
 * the panel on the right. Output goes to a folder of your choice, with a naming pattern and a counter.
 */
public class BatchDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final SafeLogger logger = new SafeLogger(BatchDialog.class);
	private static final String SAMPLE_RESOURCE = "/gui/commons/imagestudio/sample.jpg";
	private static final int PREVIEW_MAX_SIDE = 1800;

	private final ImageSettings settings = new ImageSettings();
	private final ImageSettingsPanel settingsPanel;
	private final ImageCanvas canvas = new ImageCanvas();
	private final DefaultListModel<File> listModel = new DefaultListModel<File>();
	private final JList<File> list = new JList<File>(listModel);
	private final JLabel lblCount = ToolStyle.mutedLabel("0 images");
	private final JTextField fieldTarget = new JTextField();
	private final JTextField fieldPattern = new JTextField("{name}_{n}");
	private final JSpinner spinStart = new JSpinner(new SpinnerNumberModel(1, 0, 999999, 1));
	private final JComboBox<String> comboDigits = new JComboBox<String>(new String[] {"1 (1, 2, 3)", "2 (01, 02)", "3 (001, 002)", "4 (0001)", "5", "6"});
	private final JComboBox<String> comboCollision = new JComboBox<String>(new String[] {"Add a number if the name is taken", "Overwrite the existing file", "Skip the existing file"});
	private final JLabel lblNames = ToolStyle.mutedLabel(" ");
	private final JProgressBar progress = new JProgressBar();
	private final JLabel lblProgress = ToolStyle.mutedLabel(" ");
	private final JButton btnRun;
	private final JCheckBox chkOriginal = new JCheckBox("Show original");
	private final JRadioButton rbSample = new JRadioButton("Built-in sample", true);
	private final JRadioButton rbCustom = new JRadioButton("My photo...");
	private final JRadioButton rbFirst = new JRadioButton("First of the list");

	private BufferedImage sampleImage;
	private BufferedImage processed;
	private File customSample;
	private final Timer previewTimer;
	private int previewSequence;
	private BatchWorker worker;

	public BatchDialog(Window owner, File suggestedFile) {
		super(owner, "Batch processing", ModalityType.MODELESS);
		setLayout(new MigLayout("fill, insets 12, gapy 8", "[300!][grow,fill][320!]", "[grow,fill][]"));

		settingsPanel = new ImageSettingsPanel(settings, this::onSettingsChanged, true);
		btnRun = ToolStyle.accent(ToolStyle.button("Process all images", null, "Applies the settings to every image of the list"), ToolStyle.GREEN);

		add(buildSources(), "grow");
		add(buildPreview(), "grow");
		add(settingsPanel, "grow, wrap");
		add(buildOutput(), "span 3, growx");

		previewTimer = new Timer(150, e -> renderPreview());
		previewTimer.setRepeats(false);

		if (suggestedFile != null && ImageEdit.isImageFile(suggestedFile)) {
			listModel.addElement(suggestedFile);
		}
		updateCount();
		loadSample();

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				requestClose();
			}
		});
		getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "escapeClose");
		getRootPane().getActionMap().put("escapeClose", new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				requestClose();
			}
		});
		setMinimumSize(new Dimension(1100, 700));
		setSize(1280, 780);
		setLocationRelativeTo(owner);
	}

	// ------------------------------------------------------------------ UI

	private JPanel buildSources() {
		JPanel card = ToolStyle.card(new MigLayout("insets 12, wrap 1, gapy 6", "[grow,fill]", "[][grow,fill][][][]"));
		card.add(ToolStyle.caption("1. Images to process"));
		list.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
				super.getListCellRendererComponent(l, value, index, selected, focus);
				File f = (File) value;
				setText(f.getName());
				setToolTipText(f.getAbsolutePath());
				return this;
			}
		});
		card.add(new JScrollPane(list), "grow, h 220::");
		JPanel buttons = new JPanel(new MigLayout("insets 0, gapx 4", "[grow,fill][grow,fill]", "[][]"));
		buttons.setOpaque(false);
		JButton addFiles = ToolStyle.button("Add images...", null, "Choose image files");
		addFiles.addActionListener(e -> addFilesDialog());
		JButton addFolder = ToolStyle.button("Add folder...", null, "Add every image of a folder");
		addFolder.addActionListener(e -> addFolderDialog());
		JButton remove = ToolStyle.button("Remove", null, "Remove the selected images from the list");
		remove.addActionListener(e -> {
			for (File f : new ArrayList<File>(list.getSelectedValuesList())) {
				listModel.removeElement(f);
			}
			updateCount();
		});
		JButton clear = ToolStyle.button("Clear", null, "Empty the list");
		clear.addActionListener(e -> {
			listModel.clear();
			updateCount();
		});
		buttons.add(addFiles);
		buttons.add(addFolder, "wrap");
		buttons.add(remove);
		buttons.add(clear);
		card.add(buttons);
		card.add(lblCount);
		new FileDrop(list, BorderFactory.createLineBorder(ToolStyle.BLUE, 3), files -> SwingUtilities.invokeLater(() -> addPaths(files)));

		card.add(ToolStyle.caption("2. Sample used to choose the settings"), "gaptop 8");
		ButtonGroup group = new ButtonGroup();
		group.add(rbSample);
		group.add(rbCustom);
		group.add(rbFirst);
		JPanel radios = new JPanel(new MigLayout("insets 0, wrap 1, gapy 0", "[grow,fill]", ""));
		radios.setOpaque(false);
		radios.add(rbSample);
		radios.add(rbCustom);
		radios.add(rbFirst);
		card.add(radios);
		rbSample.addActionListener(e -> loadSample());
		rbFirst.addActionListener(e -> loadSample());
		rbCustom.addActionListener(e -> {
			NativeFileChooser chooser = new NativeFileChooser();
			chooser.setDialogTitle("Sample photo");
			if (chooser.showDialog(this, "Use") == NativeFileChooser.APPROVE_OPTION && ImageEdit.isImageFile(chooser.getSelectedFile())) {
				customSample = chooser.getSelectedFile();
			} else if (customSample == null) {
				rbSample.setSelected(true);
			}
			loadSample();
		});
		return card;
	}

	private JPanel buildPreview() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 1, gapy 6", "[grow,fill]", "[][grow,fill][]"));
		p.setOpaque(false);
		p.add(ToolStyle.caption("Preview on the sample (the settings on the right apply to every image)"));
		p.add(canvas, "grow");
		ToolStyle.hand(chkOriginal);
		chkOriginal.addActionListener(e -> showCurrentPreview());
		p.add(chkOriginal);
		return p;
	}

	private JPanel buildOutput() {
		JPanel card = ToolStyle.card(new MigLayout("insets 12 14 12 14, gapx 10, gapy 6", "[][grow,fill][][][][grow,fill]", ""));
		card.add(ToolStyle.caption("3. Output"), "span 6, wrap");
		card.add(ToolStyle.mutedLabel("Target folder"));
		fieldTarget.putClientProperty("JTextField.placeholderText", "Where the new images are written (required)");
		card.add(fieldTarget, "span 4");
		JButton browse = ToolStyle.button("Browse...", null, "Choose the target folder");
		browse.addActionListener(e -> {
			NativeFileChooser chooser = new NativeFileChooser();
			chooser.setFileSelectionMode(NativeFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Target folder");
			if (chooser.showDialog(this, "Select") == NativeFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
				fieldTarget.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});
		card.add(browse, "wrap");

		card.add(ToolStyle.mutedLabel("Name pattern"));
		fieldPattern.setToolTipText("<html>Tokens: <b>{name}</b> original name, <b>{n}</b> progressive number, <b>{date}</b> today (yyyyMMdd).<br>The extension comes from the format.</html>");
		card.add(fieldPattern);
		card.add(ToolStyle.mutedLabel("Start at"));
		card.add(spinStart, "w 80!");
		card.add(ToolStyle.mutedLabel("Digits"));
		card.add(comboDigits, "wrap");

		card.add(ToolStyle.mutedLabel("If the name exists"));
		card.add(comboCollision);
		card.add(lblNames, "span 4, wrap");

		DocumentListener names = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { updateNamesPreview(); }
			@Override
			public void removeUpdate(DocumentEvent e) { updateNamesPreview(); }
			@Override
			public void changedUpdate(DocumentEvent e) { updateNamesPreview(); }
		};
		fieldPattern.getDocument().addDocumentListener(names);
		spinStart.addChangeListener(e -> updateNamesPreview());
		comboDigits.addActionListener(e -> updateNamesPreview());

		JPanel run = new JPanel(new MigLayout("insets 0, gapx 8", "[grow,fill][][]", "[]"));
		run.setOpaque(false);
		JPanel progressBox = new JPanel(new MigLayout("insets 0, wrap 1, gapy 2", "[grow,fill]", ""));
		progressBox.setOpaque(false);
		progressBox.add(progress);
		progressBox.add(lblProgress);
		run.add(progressBox);
		btnRun.setPreferredSize(new Dimension(190, 36));
		btnRun.addActionListener(e -> startOrCancel());
		run.add(btnRun);
		JButton close = ToolStyle.button("Close", null, "Close this window (Esc)");
		close.addActionListener(e -> requestClose());
		run.add(close);
		card.add(run, "span 6, growx");
		return card;
	}

	// ------------------------------------------------------------------ sources

	private void addFilesDialog() {
		NativeFileChooser chooser = new NativeFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.setDialogTitle("Add images");
		if (chooser.showDialog(this, "Add") == NativeFileChooser.APPROVE_OPTION) {
			addPaths(chooser.getSelectedFiles());
		}
	}

	private void addFolderDialog() {
		NativeFileChooser chooser = new NativeFileChooser();
		chooser.setFileSelectionMode(NativeFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Add a folder of images");
		if (chooser.showDialog(this, "Add") == NativeFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
			addPaths(new File[] {chooser.getSelectedFile()});
		}
	}

	/** Adds files, and the images found directly inside folders, in alphabetical order, without duplicates */
	private void addPaths(File[] paths) {
		if (paths == null) {
			return;
		}
		Set<File> present = new HashSet<File>(Collections.list(listModel.elements()));
		List<File> found = new ArrayList<File>();
		for (File path : paths) {
			if (path.isDirectory()) {
				File[] children = path.listFiles();
				if (children != null) {
					Arrays.sort(children, new Comparator<File>() {
						@Override
						public int compare(File a, File b) {
							return a.getName().compareToIgnoreCase(b.getName());
						}
					});
					for (File child : children) {
						if (ImageEdit.isImageFile(child)) {
							found.add(child);
						}
					}
				}
			} else if (ImageEdit.isImageFile(path)) {
				found.add(path);
			}
		}
		for (File f : found) {
			if (present.add(f)) {
				listModel.addElement(f);
			}
		}
		updateCount();
		if (rbFirst.isSelected()) {
			loadSample();
		}
	}

	private void updateCount() {
		lblCount.setText(listModel.size() + (listModel.size() == 1 ? " image" : " images"));
		updateNamesPreview();
	}

	// ------------------------------------------------------------------ sample and preview

	private void loadSample() {
		final File source = rbCustom.isSelected() ? customSample : rbFirst.isSelected() && !listModel.isEmpty() ? listModel.get(0) : null;
		new SwingWorker<BufferedImage, Void>() {
			@Override
			protected BufferedImage doInBackground() throws Exception {
				BufferedImage read;
				if (source != null) {
					read = ImageIO.read(source);
				} else {
					InputStream in = BatchDialog.class.getResourceAsStream(SAMPLE_RESOURCE);
					if (in == null) {
						throw new IOException("Built-in sample not found");
					}
					try {
						read = ImageIO.read(in);
					} finally {
						in.close();
					}
				}
				if (read == null) {
					throw new IOException("Unsupported image format");
				}
				int max = Math.max(read.getWidth(), read.getHeight());
				if (max > PREVIEW_MAX_SIDE) {
					double k = PREVIEW_MAX_SIDE / (double) max;
					read = ImageEdit.resize(read, (int) Math.round(read.getWidth() * k), (int) Math.round(read.getHeight() * k), ImageSettings.Resampling.AREA);
				}
				return read;
			}

			@Override
			protected void done() {
				try {
					sampleImage = get();
					settingsPanel.setBaseSize(sampleImage.getWidth(), sampleImage.getHeight());
					canvas.setImage(sampleImage, true);
					renderPreview();
				} catch (Exception e) {
					logger.error("Cannot load the sample", e);
					lblProgress.setText("Cannot load the sample image: " + e.getMessage());
				}
			}
		}.execute();
	}

	private void onSettingsChanged() {
		previewTimer.restart();
		updateNamesPreview();
	}

	private void renderPreview() {
		if (sampleImage == null) {
			return;
		}
		final BufferedImage source = sampleImage;
		final ImageSettings snapshot = settings.copy();
		final int sequence = ++previewSequence;
		new SwingWorker<BufferedImage, Void>() {
			private String estimate;

			@Override
			protected BufferedImage doInBackground() throws Exception {
				BufferedImage result = ImageEdit.apply(source, snapshot);
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ImageEdit.write(result, out, snapshot);
					estimate = "Sample output: " + result.getWidth() + " x " + result.getHeight() + " px, about " + ConversionUtils.coolFileSize((long) out.size());
				} catch (IOException e) {
					estimate = "This format cannot be written: " + snapshot.format;
				}
				return result;
			}

			@Override
			protected void done() {
				if (sequence != previewSequence) {
					return;
				}
				try {
					processed = get();
					settingsPanel.setEstimate(estimate);
					showCurrentPreview();
				} catch (Exception e) {
					logger.error("Preview failed", e);
				}
			}
		}.execute();
	}

	private void showCurrentPreview() {
		BufferedImage toShow = chkOriginal.isSelected() ? sampleImage : processed;
		if (toShow != null) {
			canvas.setImage(toShow, true);
		}
	}

	// ------------------------------------------------------------------ naming

	private int digits() {
		return comboDigits.getSelectedIndex() + 1;
	}

	/** Name (without folder) for the image number {@code index}, before any collision handling */
	String baseName(File source, int index) {
		String name = source.getName();
		int dot = name.lastIndexOf('.');
		String stem = dot > 0 ? name.substring(0, dot) : name;
		String number = String.format(Locale.ROOT, "%0" + digits() + "d", ((Number) spinStart.getValue()).intValue() + index);
		String pattern = fieldPattern.getText().trim().isEmpty() ? "{name}" : fieldPattern.getText();
		String result = pattern.replace("{name}", stem).replace("{n}", number).replace("{date}", new SimpleDateFormat("yyyyMMdd").format(new Date()));
		result = result.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
		return (result.isEmpty() ? stem : result) + "." + settings.format;
	}

	private void updateNamesPreview() {
		if (listModel.isEmpty()) {
			lblNames.setText("Names will look like: " + baseName(new File("photo.jpg"), 0) + ", " + baseName(new File("photo.jpg"), 1));
			return;
		}
		StringBuilder text = new StringBuilder("Names: ");
		for (int i = 0; i < Math.min(3, listModel.size()); i++) {
			text.append(i > 0 ? ", " : "").append(baseName(listModel.get(i), i));
		}
		if (listModel.size() > 3) {
			text.append(", ...");
		}
		lblNames.setText(text.toString());
	}

	// ------------------------------------------------------------------ run

	private void startOrCancel() {
		if (worker != null) {
			worker.cancel(false);
			btnRun.setEnabled(false);
			lblProgress.setText("Stopping after the current image...");
			return;
		}
		if (listModel.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Add at least one image to process.", "Nothing to do", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		String target = fieldTarget.getText().trim();
		if (target.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Choose the target folder first.", "Target folder", JOptionPane.INFORMATION_MESSAGE);
			fieldTarget.requestFocusInWindow();
			return;
		}
		File folder = new File(target);
		if (folder.exists() && !folder.isDirectory()) {
			JOptionPane.showMessageDialog(this, "The target is a file, not a folder.", "Target folder", JOptionPane.WARNING_MESSAGE);
			return;
		}
		List<File> sources = Collections.list(listModel.elements());
		for (File source : sources) {
			if (source.getAbsoluteFile().getParentFile() != null && source.getAbsoluteFile().getParentFile().equals(folder.getAbsoluteFile())
					&& comboCollision.getSelectedIndex() == 1) {
				if (JOptionPane.showConfirmDialog(this, "The target folder is also a source folder and \"Overwrite\" is selected:\nthe originals can be replaced. Continue?",
						"Overwrite originals?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
					return;
				}
				break;
			}
		}
		worker = new BatchWorker(sources, folder, settings.copy(), comboCollision.getSelectedIndex());
		progress.setValue(0);
		progress.setMaximum(sources.size());
		btnRun.setText("Stop");
		worker.execute();
	}

	/** Applies the settings to each image, one at a time (each step already uses every core) */
	private class BatchWorker extends SwingWorker<int[], String> {
		private final List<File> sources;
		private final File folder;
		private final ImageSettings snapshot;
		private final int collision;
		private final Set<String> usedInRun = new HashSet<String>();

		BatchWorker(List<File> sources, File folder, ImageSettings snapshot, int collision) {
			this.sources = sources;
			this.folder = folder;
			this.snapshot = snapshot;
			this.collision = collision;
		}

		@Override
		protected int[] doInBackground() {
			int done = 0;
			int skipped = 0;
			int failed = 0;
			for (int i = 0; i < sources.size() && !isCancelled(); i++) {
				File source = sources.get(i);
				publish("Processing " + (i + 1) + " / " + sources.size() + ": " + source.getName());
				try {
					BufferedImage image = ImageIO.read(source);
					if (image == null) {
						throw new IOException("unsupported format");
					}
					File target = resolveTarget(source, i);
					if (target == null) {
						skipped++;
					} else {
						ImageEdit.write(ImageEdit.apply(image, snapshot), target, snapshot);
						done++;
					}
				} catch (Exception e) {
					failed++;
					logger.error("Batch: cannot process " + source, e);
				}
				setProgress(0);
				final int step = i + 1;
				SwingUtilities.invokeLater(() -> progress.setValue(step));
			}
			return new int[] {done, skipped, failed};
		}

		private File resolveTarget(File source, int index) {
			String name = baseName(source, index);
			File target = new File(folder, name);
			boolean taken = target.exists() || usedInRun.contains(name.toLowerCase(Locale.ROOT));
			if (taken && collision == 2 && !usedInRun.contains(name.toLowerCase(Locale.ROOT))) {
				return null;
			}
			if (taken && (collision == 0 || usedInRun.contains(name.toLowerCase(Locale.ROOT)))) {
				int dot = name.lastIndexOf('.');
				String stem = name.substring(0, dot);
				String ext = name.substring(dot);
				int n = 1;
				do {
					name = stem + "_" + n++ + ext;
					target = new File(folder, name);
				} while (target.exists() || usedInRun.contains(name.toLowerCase(Locale.ROOT)));
			}
			usedInRun.add(name.toLowerCase(Locale.ROOT));
			return target;
		}

		@Override
		protected void process(List<String> chunks) {
			lblProgress.setText(chunks.get(chunks.size() - 1));
		}

		@Override
		protected void done() {
			worker = null;
			btnRun.setText("Process all images");
			btnRun.setEnabled(true);
			try {
				int[] r = get();
				String summary = r[0] + " written, " + r[1] + " skipped, " + r[2] + " failed" + (isCancelled() ? " (stopped)" : "");
				lblProgress.setText(summary + "  ->  " + folder.getAbsolutePath());
				JOptionPane.showMessageDialog(BatchDialog.this, summary + "\n\nFolder: " + folder.getAbsolutePath()
						+ (r[2] > 0 ? "\n\nSome images failed: details are in the log." : ""), "Batch finished",
						r[2] > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception e) {
				logger.error("Batch failed", e);
				lblProgress.setText("Batch failed: " + e.getMessage());
			}
		}
	}

	/** Closing the window: asks about a changed preset and remembers the settings as the last used */
	private void requestClose() {
		if (worker == null && settingsPanel.confirmPresetOnClose(this)) {
			dispose();
		}
	}

	@Override
	public void dispose() {
		previewTimer.stop();
		previewSequence++;
		if (worker != null) {
			worker.cancel(false);
		}
		super.dispose();
	}
}
