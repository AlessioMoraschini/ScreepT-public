/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package gui.commons.imagestudio;

import various.common.light.i18n.I18n;
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
	private final JLabel lblCount = ToolStyle.mutedLabel(I18n.t("batch.0_images"));
	private final JTextField fieldTarget = new JTextField();
	private final JTextField fieldPattern = new JTextField("{name}_{n}");
	private final JSpinner spinStart = new JSpinner(new SpinnerNumberModel(1, 0, 999999, 1));
	private final JComboBox<String> comboDigits = new JComboBox<String>(new String[] {"1 (1, 2, 3)", "2 (01, 02)", "3 (001, 002)", "4 (0001)", "5", "6"});
	private final JComboBox<String> comboCollision = new JComboBox<String>(new String[] {I18n.t("batch.add_a_number_if_the_name"), I18n.t("batch.overwrite_the_existing_file"), I18n.t("batch.skip_the_existing_file")});
	private final JLabel lblNames = ToolStyle.mutedLabel(" ");
	private final JProgressBar progress = new JProgressBar();
	private final JLabel lblProgress = ToolStyle.mutedLabel(" ");
	private final JButton btnRun;
	private final JCheckBox chkOriginal = new JCheckBox(I18n.t("batch.show_original"));
	private final JRadioButton rbSample = new JRadioButton(I18n.t("batch.built_in_sample"));
	private final JRadioButton rbCustom = new JRadioButton(I18n.t("batch.my_photo"));
	private final JRadioButton rbFirst = new JRadioButton(I18n.t("batch.image_selected_in_the_list"), true);

	private BufferedImage sampleImage;
	private BufferedImage processed;
	private File customSample;
	private final Timer previewTimer;
	private int previewSequence;
	private BatchWorker worker;

	public BatchDialog(Window owner, File suggestedFile) {
		super(owner, I18n.t("batch.batch_processing"), ModalityType.MODELESS);
		setLayout(new MigLayout("fill, insets 12, gapy 8", "[grow,fill][320!]", "[grow,fill][]"));

		settingsPanel = new ImageSettingsPanel(settings, this::onSettingsChanged, true);
		btnRun = ToolStyle.accent(ToolStyle.button(I18n.t("batch.process_all_images"), null, I18n.t("batch.applies_the_settings_to_every_image")), ToolStyle.GREEN);

		// the file list and the preview never overlap: the border between them can be dragged, and each side scrolls when it is too small
		JScrollPane sourcesScroll = new JScrollPane(buildSources(), javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sourcesScroll.setBorder(null);
		sourcesScroll.getVerticalScrollBar().setUnitIncrement(16);
		sourcesScroll.setMinimumSize(new Dimension(160, 120));
		JPanel previewPanel = buildPreview();
		previewPanel.setMinimumSize(new Dimension(220, 120));
		javax.swing.JSplitPane split = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT, sourcesScroll, previewPanel);
		split.setOneTouchExpandable(true);
		split.setContinuousLayout(true);
		split.setResizeWeight(0.0);
		split.setDividerLocation(Math.max(340, sourcesScroll.getViewport().getView().getPreferredSize().width + 24));
		split.setDividerSize(8);
		split.setBorder(null);
		add(split, "grow, wmin 0");
		add(settingsPanel, "grow, wrap");
		add(buildOutput(), "span 2, growx");

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
		DialogFit.apply(this, owner, 1100, 700);
	}

	// ------------------------------------------------------------------ UI

	private JPanel buildSources() {
		JPanel card = ToolStyle.card(new MigLayout("insets 12, wrap 1, gapy 6", "[grow,fill]", "[][grow,fill,60::][][][][][]"));
		card.add(ToolStyle.caption(I18n.t("batch.1_images_to_process_click_one")));
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
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && rbFirst.isSelected()) {
				loadSample();
			}
		});
		JScrollPane listScroll = new JScrollPane(list, javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		card.add(listScroll, "grow, wmin 0, hmin 60");
		JPanel buttons = new JPanel(new MigLayout("insets 0, gapx 4", "[grow,fill,sg b][grow,fill,sg b]", "[][]"));
		buttons.setOpaque(false);
		JButton addFiles = ToolStyle.button(I18n.t("batch.add_images"), null, I18n.t("batch.choose_image_files"));
		addFiles.addActionListener(e -> addFilesDialog());
		JButton addFolder = ToolStyle.button(I18n.t("batch.add_folder"), null, I18n.t("batch.add_every_image_of_a_folder"));
		addFolder.addActionListener(e -> addFolderDialog());
		JButton remove = ToolStyle.button("Remove", null, I18n.t("batch.remove_the_selected_images_from_the"));
		remove.addActionListener(e -> {
			for (File f : new ArrayList<File>(list.getSelectedValuesList())) {
				listModel.removeElement(f);
			}
			updateCount();
			if (rbFirst.isSelected()) {
				loadSample();
			}
		});
		JButton clear = ToolStyle.button("Clear", null, I18n.t("batch.empty_the_list"));
		clear.addActionListener(e -> {
			listModel.clear();
			updateCount();
			if (rbFirst.isSelected()) {
				loadSample();
			}
		});
		buttons.add(addFiles);
		buttons.add(addFolder, "wrap");
		buttons.add(remove);
		buttons.add(clear);
		card.add(buttons);
		card.add(lblCount);
		new FileDrop(list, BorderFactory.createLineBorder(ToolStyle.BLUE, 3), files -> SwingUtilities.invokeLater(() -> addPaths(files)));

		card.add(ToolStyle.caption(I18n.t("batch.2_sample_used_to_choose_the")), "gaptop 8");
		ButtonGroup group = new ButtonGroup();
		group.add(rbSample);
		group.add(rbCustom);
		group.add(rbFirst);
		JPanel radios = new JPanel(new MigLayout("insets 0, wrap 1, gapy 0", "[grow,fill]", ""));
		radios.setOpaque(false);
		radios.add(rbFirst);
		radios.add(rbSample);
		radios.add(rbCustom);
		card.add(radios);
		rbSample.addActionListener(e -> loadSample());
		rbFirst.addActionListener(e -> loadSample());
		rbCustom.addActionListener(e -> {
			NativeFileChooser chooser = new NativeFileChooser();
			chooser.setDialogTitle(I18n.t("batch.sample_photo"));
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
		p.add(ToolStyle.caption(I18n.t("batch.preview_on_the_sample_the_settings")));
		p.add(canvas, "grow");
		ToolStyle.hand(chkOriginal);
		chkOriginal.addActionListener(e -> showCurrentPreview());
		JPanel under = new JPanel(new MigLayout("insets 0, gapx 8", "[][grow,fill][]", "[]"));
		under.setOpaque(false);
		under.add(chkOriginal);
		under.add(new JLabel());
		JButton importSettings = ToolStyle.button(I18n.t("batch.import_settings"), null, PhotoshopPresets.SUPPORTED_HINT);
		importSettings.addActionListener(e -> settingsPanel.importPresetFromUser());
		under.add(importSettings);
		p.add(under, "growx");
		return p;
	}

	private JPanel buildOutput() {
		JPanel card = ToolStyle.card(new MigLayout("insets 12 14 12 14, gapx 10, gapy 6", "[][grow,fill][][][][grow,fill]", ""));
		card.add(ToolStyle.caption(I18n.t("batch.3_output")), "span 6, wrap");
		card.add(ToolStyle.mutedLabel(I18n.t("batch.target_folder")));
		fieldTarget.putClientProperty("JTextField.placeholderText", I18n.t("gui.where_the_new_images_are_written"));
		card.add(fieldTarget, "span 4");
		JButton browse = ToolStyle.button(I18n.t("gui.browse"), null, I18n.t("batch.choose_the_target_folder"));
		browse.addActionListener(e -> {
			NativeFileChooser chooser = new NativeFileChooser();
			chooser.setFileSelectionMode(NativeFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle(I18n.t("batch.target_folder"));
			if (chooser.showDialog(this, "Select") == NativeFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
				fieldTarget.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});
		card.add(browse, "wrap");

		card.add(ToolStyle.mutedLabel(I18n.t("batch.name_pattern")));
		fieldPattern.setToolTipText(I18n.t("batch.tokens_name_original_name_n_progressive"));
		card.add(fieldPattern);
		card.add(ToolStyle.mutedLabel(I18n.t("batch.start_at")));
		card.add(spinStart, "w 80!");
		card.add(ToolStyle.mutedLabel("Digits"));
		card.add(comboDigits, "wrap");

		card.add(ToolStyle.mutedLabel(I18n.t("batch.if_the_name_exists")));
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
		JButton close = ToolStyle.button(I18n.t("common.close"), null, I18n.t("batch.close_this_window_esc"));
		close.addActionListener(e -> requestClose());
		run.add(close);
		card.add(run, "span 6, growx");
		return card;
	}

	// ------------------------------------------------------------------ sources

	private void addFilesDialog() {
		NativeFileChooser chooser = new NativeFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.setDialogTitle(I18n.t("batch.add_images_2"));
		if (chooser.showDialog(this, "Add") == NativeFileChooser.APPROVE_OPTION) {
			addPaths(chooser.getSelectedFiles());
		}
	}

	private void addFolderDialog() {
		NativeFileChooser chooser = new NativeFileChooser();
		chooser.setFileSelectionMode(NativeFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle(I18n.t("batch.add_a_folder_of_images"));
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
		int firstNew = -1;
		for (File f : found) {
			if (present.add(f)) {
				if (firstNew < 0) {
					firstNew = listModel.size();
				}
				listModel.addElement(f);
			}
		}
		updateCount();
		if (rbFirst.isSelected()) {
			if (firstNew >= 0) {
				// the first image just added is the one shown: click another one to preview it instead
				list.setSelectedIndex(firstNew);
				list.ensureIndexIsVisible(firstNew);
			} else {
				loadSample();
			}
		}
	}

	private void updateCount() {
		lblCount.setText(listModel.size() + (listModel.size() == 1 ? I18n.t("batch.image") : I18n.t("batch.images")));
		updateNamesPreview();
	}

	// ------------------------------------------------------------------ sample and preview

	private void loadSample() {
		final File selected = list.getSelectedValue();
		final File source = rbCustom.isSelected() ? customSample
				: rbFirst.isSelected() ? (selected != null ? selected : (!listModel.isEmpty() ? listModel.get(0) : null)) : null;
		new SwingWorker<BufferedImage, Void>() {
			@Override
			protected BufferedImage doInBackground() throws Exception {
				BufferedImage read;
				if (source != null) {
					read = ImageEdit.read(source);
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
					settingsPanel.setThumbnailSource(sampleImage);
					canvas.setImage(sampleImage, true);
					renderPreview();
				} catch (Exception e) {
					logger.error("Cannot load the sample", e);
					lblProgress.setText(I18n.t("batch.cannot_load_the_sample_image") + e.getMessage());
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
					estimate = I18n.t("batch.sample_output") + result.getWidth() + " x " + result.getHeight() + I18n.t("batch.px_about") + ConversionUtils.coolFileSize((long) out.size());
				} catch (IOException e) {
					estimate = I18n.t("batch.this_format_cannot_be_written") + snapshot.format;
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
			lblNames.setText(I18n.t("batch.names_will_look_like") + baseName(new File("photo.jpg"), 0) + ", " + baseName(new File("photo.jpg"), 1));
			return;
		}
		StringBuilder text = new StringBuilder(I18n.t("batch.names"));
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
			lblProgress.setText(I18n.t("batch.stopping_after_the_current_image"));
			return;
		}
		if (listModel.isEmpty()) {
			JOptionPane.showMessageDialog(this, I18n.t("batch.add_at_least_one_image_to"), I18n.t("batch.nothing_to_do"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		String target = fieldTarget.getText().trim();
		if (target.isEmpty()) {
			JOptionPane.showMessageDialog(this, I18n.t("batch.choose_the_target_folder_first"), I18n.t("batch.target_folder"), JOptionPane.INFORMATION_MESSAGE);
			fieldTarget.requestFocusInWindow();
			return;
		}
		File folder = new File(target);
		if (folder.exists() && !folder.isDirectory()) {
			JOptionPane.showMessageDialog(this, I18n.t("batch.the_target_is_a_file_not"), I18n.t("batch.target_folder"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		List<File> sources = Collections.list(listModel.elements());
		for (File source : sources) {
			if (source.getAbsoluteFile().getParentFile() != null && source.getAbsoluteFile().getParentFile().equals(folder.getAbsoluteFile())
					&& comboCollision.getSelectedIndex() == 1) {
				if (JOptionPane.showConfirmDialog(this, I18n.t("batch.the_target_folder_is_also_a"),
						I18n.t("batch.overwrite_originals"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
					return;
				}
				break;
			}
		}
		worker = new BatchWorker(sources, folder, settings.copy(), comboCollision.getSelectedIndex());
		progress.setValue(0);
		progress.setMaximum(sources.size());
		btnRun.setText(I18n.t("old.stop"));
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
				publish(I18n.t("batch.processing") + (i + 1) + " / " + sources.size() + ": " + source.getName());
				try {
					BufferedImage image = ImageEdit.read(source);
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
			btnRun.setText(I18n.t("batch.process_all_images"));
			btnRun.setEnabled(true);
			try {
				int[] r = get();
				String summary = r[0] + I18n.t("batch.written") + r[1] + I18n.t("batch.skipped") + r[2] + I18n.t("batch.failed") + (isCancelled() ? I18n.t("batch.stopped") : "");
				lblProgress.setText(summary + "  ->  " + folder.getAbsolutePath());
				JOptionPane.showMessageDialog(BatchDialog.this, summary + I18n.t("batch.folder") + folder.getAbsolutePath()
						+ (r[2] > 0 ? I18n.t("batch.some_images_failed_details_are_in") : ""), I18n.t("batch.batch_finished"),
						r[2] > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception e) {
				logger.error("Batch failed", e);
				lblProgress.setText(I18n.t("batch.batch_failed") + e.getMessage());
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
