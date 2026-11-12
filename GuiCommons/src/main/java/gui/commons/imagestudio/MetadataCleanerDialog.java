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

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import gui.commons.dialogutils.NativeFileChooser;
import gui.commons.frameutils.frame.arch.ToolStyle;
import net.miginfocom.swing.MigLayout;
import various.common.light.gui.dnd.FileDrop;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;

/**
 * Removes the metadata (EXIF, GPS, XMP, IPTC, comments...) from many images at once, without re-encoding them (see
 * {@link MetadataStripper}). Files and folders come from the buttons or by drag and drop, with filters; the clean copies
 * go to a folder (default, with or without the sub-folder structure) or replace the originals (never the default, with
 * confirmation and an optional backup of the originals).
 */
public class MetadataCleanerDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final SafeLogger logger = new SafeLogger(MetadataCleanerDialog.class);

	private static final Set<String> JPEG_EXT = new HashSet<String>(Arrays.asList("jpg", "jpeg", "jpe", "jfif"));
	private static final Set<String> KNOWN_EXT = new HashSet<String>(Arrays.asList("jpg", "jpeg", "jpe", "jfif", "png", "gif", "webp", "bmp", "tif", "tiff"));

	/** One file of the list; {@code root} is the folder it was added from (to keep the structure), or null */
	private static final class Entry {
		final File file;
		final File root;
		final long size;
		String metadata = "...";
		String result = "";

		Entry(File file, File root) {
			this.file = file;
			this.root = root;
			this.size = file.length();
		}
	}

	private final List<Entry> entries = new ArrayList<Entry>();
	private final AbstractTableModel model = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;
		private final String[] names = {"File", "Folder", "Size", "Metadata found", "Result"};

		@Override
		public int getRowCount() {
			return entries.size();
		}

		@Override
		public int getColumnCount() {
			return names.length;
		}

		@Override
		public String getColumnName(int column) {
			return names[column];
		}

		@Override
		public Object getValueAt(int row, int column) {
			Entry e = entries.get(row);
			switch (column) {
			case 0:
				return e.file.getName();
			case 1:
				return e.file.getParent();
			case 2:
				return ConversionUtils.coolFileSize(e.size);
			case 3:
				return e.metadata;
			default:
				return e.result;
			}
		}
	};
	private final JTable table = new JTable(model);
	private final JLabel lblCount = ToolStyle.mutedLabel("0 files");

	// filters
	private final JCheckBox chkSub = new JCheckBox("Include sub-folders", true);
	private final JCheckBox chkJpeg = new JCheckBox("JPEG", true);
	private final JCheckBox chkPng = new JCheckBox("PNG", true);
	private final JCheckBox chkWebp = new JCheckBox("WebP", true);
	private final JCheckBox chkGif = new JCheckBox("GIF", true);
	private final JCheckBox chkOther = new JCheckBox("Others (TIFF, BMP...)", false);
	private final JTextField fieldFilter = new JTextField();

	// what to remove
	private final JRadioButton rbAll = new JRadioButton("Everything (EXIF, GPS, XMP, IPTC, comments, thumbnails)", true);
	private final JRadioButton rbLocation = new JRadioButton("Only the location (GPS)");
	private final JCheckBox chkIcc = new JCheckBox("Keep the color profile (ICC)", true);
	private final JCheckBox chkOrientation = new JCheckBox("Keep the orientation (photos stay upright)", true);
	private final JCheckBox chkTrailing = new JCheckBox("Remove data after the image end (motion photo video, extra images)", true);
	private final JCheckBox chkReencode = new JCheckBox("Also clean TIFF and others by re-saving them", false);

	// output
	private final JRadioButton rbCopies = new JRadioButton("Save clean copies in a folder", true);
	private final JRadioButton rbReplace = new JRadioButton("Replace the originals (cannot be undone)");
	private final JTextField fieldTarget = new JTextField();
	private final JButton btnBrowse = ToolStyle.button("Browse...", null, "Choose the target folder");
	private final JTextField fieldPattern = new JTextField("{name}");
	private final JComboBox<String> comboCollision = new JComboBox<String>(new String[] {"Add a number if the name is taken", "Overwrite the existing file", "Skip the existing file"});
	private final JCheckBox chkStructure = new JCheckBox("Keep the sub-folder structure", true);
	private final JCheckBox chkCopyClean = new JCheckBox("Copy also the files without metadata", true);
	private final JCheckBox chkBackup = new JCheckBox("Move the originals to a backup folder first");
	private final JTextField fieldBackup = new JTextField();
	private final JButton btnBackupBrowse = ToolStyle.button("Browse...", null, "Choose the backup folder");
	private final JCheckBox chkDate = new JCheckBox("Keep the modified date of the files", true);

	private final JProgressBar progress = new JProgressBar();
	private final JLabel lblProgress = ToolStyle.mutedLabel(" ");
	private final JButton btnRun;
	private final JButton btnOpen = ToolStyle.button("Open target folder", null, "Show the folder with the clean copies");
	private CleanWorker worker;
	private File lastTarget;

	public MetadataCleanerDialog(Window owner, File suggestedFile) {
		super(owner, "Clean metadata", ModalityType.MODELESS);
		setLayout(new MigLayout("fill, insets 12, gapy 8, gapx 10", "[grow,fill][400!]", "[grow,fill][]"));
		btnRun = ToolStyle.accent(ToolStyle.button("Clean the files", null, "Remove the metadata from every file of the list"), ToolStyle.GREEN);

		add(buildFiles(), "grow");
		add(buildOptions(), "grow, wrap");
		add(buildOutput(), "span 2, growx");

		if (suggestedFile != null && suggestedFile.isFile()) {
			addPaths(new File[] {suggestedFile});
		}
		updateOutputEnabled();

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				requestClose();
			}
		});
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "escapeClose");
		getRootPane().getActionMap().put("escapeClose", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				requestClose();
			}
		});
		DialogFit.apply(this, owner, 900, 560);
	}

	// ------------------------------------------------------------------ UI

	private JPanel buildFiles() {
		JPanel card = ToolStyle.card(new MigLayout("insets 12, wrap 1, gapy 6", "[grow,fill]", "[][][][][grow,fill][]"));
		card.add(ToolStyle.caption("1. Files to clean (drop files or folders here)"));

		JPanel buttons = new JPanel(new MigLayout("insets 0, gapx 4", "[][][][][grow,fill][]", "[]"));
		buttons.setOpaque(false);
		JButton addFiles = ToolStyle.button("Add files...", null, "Choose image files");
		addFiles.addActionListener(e -> addFilesDialog());
		JButton addFolder = ToolStyle.button("Add folder...", null, "Add the images of a folder (sub-folders if ticked), with the filters below");
		addFolder.addActionListener(e -> addFolderDialog());
		JButton remove = ToolStyle.button("Remove", null, "Remove the selected files from the list");
		remove.addActionListener(e -> removeSelected());
		JButton clear = ToolStyle.button("Clear", null, "Empty the list");
		clear.addActionListener(e -> {
			if (worker == null) {
				entries.clear();
				model.fireTableDataChanged();
				updateCount();
			}
		});
		JButton analyze = ToolStyle.button("Check metadata", null, "Look inside the files to see what they carry (nothing is changed)");
		analyze.addActionListener(e -> analyzeAll());
		buttons.add(addFiles);
		buttons.add(addFolder);
		buttons.add(remove);
		buttons.add(clear);
		buttons.add(new JLabel());
		buttons.add(analyze);
		card.add(buttons);

		JPanel filters = new JPanel(new MigLayout("insets 0, gapx 8", "[][][][][][][grow,fill]", "[]"));
		filters.setOpaque(false);
		filters.add(ToolStyle.mutedLabel("Filters"));
		for (JCheckBox box : new JCheckBox[] {chkJpeg, chkPng, chkWebp, chkGif, chkOther}) {
			ToolStyle.hand(box);
			filters.add(box);
		}
		fieldFilter.putClientProperty("JTextField.placeholderText", "Name contains... (comma separated)");
		fieldFilter.setToolTipText("Only the files whose name contains one of these words are added");
		filters.add(fieldFilter);
		card.add(filters);

		JPanel row = new JPanel(new MigLayout("insets 0, gapx 8", "[][grow,fill]", "[]"));
		row.setOpaque(false);
		ToolStyle.hand(chkSub);
		row.add(chkSub);
		row.add(ToolStyle.mutedLabel("The filters apply to what you add from now on"));
		card.add(row);

		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setFillsViewportHeight(true);
		table.setRowHeight(22);
		table.getColumnModel().getColumn(0).setPreferredWidth(190);
		table.getColumnModel().getColumn(1).setPreferredWidth(230);
		table.getColumnModel().getColumn(2).setPreferredWidth(70);
		table.getColumnModel().getColumn(3).setPreferredWidth(150);
		table.getColumnModel().getColumn(4).setPreferredWidth(190);
		JScrollPane scroll = new JScrollPane(table);
		card.add(scroll, "grow, h 260::");
		table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "removeRows");
		table.getActionMap().put("removeRows", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				removeSelected();
			}
		});
		card.add(lblCount);
		new FileDrop(scroll, BorderFactory.createLineBorder(ToolStyle.BLUE, 3), files -> SwingUtilities.invokeLater(() -> addPaths(files)));
		return card;
	}

	private JPanel buildOptions() {
		JPanel card = ToolStyle.card(new MigLayout("insets 12, wrap 1, gapy 4", "[grow,fill]", ""));
		card.add(ToolStyle.caption("2. What to remove"));
		ButtonGroup group = new ButtonGroup();
		group.add(rbAll);
		group.add(rbLocation);
		for (JRadioButton radio : new JRadioButton[] {rbAll, rbLocation}) {
			ToolStyle.hand(radio);
			card.add(radio, "wmin 0");
		}
		rbAll.setToolTipText("Camera, lens, date, GPS, software, thumbnails, comments, copyright, IPTC, XMP...");
		rbLocation.setToolTipText("Zeroes the GPS data of the EXIF (and removes an XMP that carries a location); everything else stays");
		card.add(new javax.swing.JSeparator(), "growx, gaptop 4, gapbottom 2");
		for (JCheckBox box : new JCheckBox[] {chkIcc, chkOrientation, chkTrailing, chkReencode}) {
			ToolStyle.hand(box);
			card.add(box, "wmin 0");
		}
		chkIcc.setToolTipText("Without the profile the colors can look different on some screens");
		chkOrientation.setToolTipText("Writes a tiny EXIF with only the orientation: photos taken sideways keep showing upright");
		chkTrailing.setToolTipText("Some files carry a video or other images after the end of the picture: this removes them too");
		chkReencode.setToolTipText("TIFF and the formats that cannot be cleaned directly are decoded and saved again without metadata");
		rbLocation.addActionListener(e -> updateOptionsEnabled());
		rbAll.addActionListener(e -> updateOptionsEnabled());
		card.add(ToolStyle.mutedLabel("<html>JPEG, PNG, WebP and GIF are cleaned without touching the pixels:<br>no re-encoding, no quality loss.</html>"), "gaptop 6, wmin 0");
		return card;
	}

	private JPanel buildOutput() {
		JPanel card = ToolStyle.card(new MigLayout("insets 12 14 12 14, gapx 10, gapy 6", "[][grow,fill][][grow,fill]", ""));
		card.add(ToolStyle.caption("3. Output"), "span 4, wrap");
		ButtonGroup group = new ButtonGroup();
		group.add(rbCopies);
		group.add(rbReplace);
		ToolStyle.hand(rbCopies);
		ToolStyle.hand(rbReplace);
		rbCopies.addActionListener(e -> updateOutputEnabled());
		rbReplace.addActionListener(e -> updateOutputEnabled());
		card.add(rbCopies, "span 2");
		card.add(rbReplace, "span 2, wrap");

		card.add(ToolStyle.mutedLabel("Target folder"));
		fieldTarget.putClientProperty("JTextField.placeholderText", "Where the clean copies are written (required)");
		JPanel target = new JPanel(new MigLayout("insets 0, gapx 4", "[grow,fill][]", "[]"));
		target.setOpaque(false);
		target.add(fieldTarget);
		btnBrowse.addActionListener(e -> chooseFolder(fieldTarget, "Target folder"));
		target.add(btnBrowse);
		card.add(target);
		chkBackup.setToolTipText("The original of each file is moved there before the clean one takes its place");
		ToolStyle.hand(chkBackup);
		chkBackup.addActionListener(e -> updateOutputEnabled());
		card.add(chkBackup, "span 2, wrap");

		card.add(ToolStyle.mutedLabel("Name pattern"));
		fieldPattern.setToolTipText("<html>Tokens: <b>{name}</b> original name, <b>{n}</b> number in the list, <b>{date}</b> today (yyyyMMdd).<br>Example: <b>{name}_clean</b>. The extension stays the same.</html>");
		card.add(fieldPattern);
		card.add(ToolStyle.mutedLabel("Backup folder"));
		JPanel backup = new JPanel(new MigLayout("insets 0, gapx 4", "[grow,fill][]", "[]"));
		backup.setOpaque(false);
		backup.add(fieldBackup);
		btnBackupBrowse.addActionListener(e -> chooseFolder(fieldBackup, "Backup folder"));
		backup.add(btnBackupBrowse);
		card.add(backup, "wrap");

		card.add(ToolStyle.mutedLabel("If the name exists"));
		card.add(comboCollision);
		card.add(chkStructure, "span 2, wrap");
		ToolStyle.hand(chkStructure);
		chkStructure.setToolTipText("For the files added from a folder: the clean copies keep their sub-folders inside the target");
		card.add(chkDate, "span 2");
		ToolStyle.hand(chkDate);
		card.add(chkCopyClean, "span 2, wrap");
		ToolStyle.hand(chkCopyClean);
		chkCopyClean.setToolTipText("So the target folder holds every file, also those that had nothing to remove");

		JPanel run = new JPanel(new MigLayout("insets 0, gapx 8", "[grow,fill][][][]", "[]"));
		run.setOpaque(false);
		JPanel progressBox = new JPanel(new MigLayout("insets 0, wrap 1, gapy 2", "[grow,fill]", ""));
		progressBox.setOpaque(false);
		progressBox.add(progress);
		progressBox.add(lblProgress);
		run.add(progressBox);
		btnOpen.setEnabled(false);
		btnOpen.addActionListener(e -> openTarget());
		run.add(btnOpen);
		btnRun.setPreferredSize(new Dimension(190, 36));
		btnRun.addActionListener(e -> startOrCancel());
		run.add(btnRun);
		JButton close = ToolStyle.button("Close", null, "Close this window (Esc)");
		close.addActionListener(e -> requestClose());
		run.add(close);
		card.add(run, "span 4, growx, gaptop 6");
		return card;
	}

	private void updateOptionsEnabled() {
		boolean all = rbAll.isSelected();
		chkIcc.setEnabled(all);
		chkOrientation.setEnabled(all);
		chkTrailing.setEnabled(all);
		chkReencode.setEnabled(all);
	}

	private void updateOutputEnabled() {
		boolean copies = rbCopies.isSelected();
		fieldTarget.setEnabled(copies);
		btnBrowse.setEnabled(copies);
		fieldPattern.setEnabled(copies);
		comboCollision.setEnabled(copies);
		chkStructure.setEnabled(copies);
		chkCopyClean.setEnabled(copies);
		chkBackup.setEnabled(!copies);
		fieldBackup.setEnabled(!copies && chkBackup.isSelected());
		btnBackupBrowse.setEnabled(!copies && chkBackup.isSelected());
		btnRun.setText(copies ? "Clean the files" : "Replace with clean files");
	}

	private void chooseFolder(JTextField field, String title) {
		NativeFileChooser chooser = new NativeFileChooser();
		chooser.setFileSelectionMode(NativeFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle(title);
		if (chooser.showDialog(this, "Select") == NativeFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
			field.setText(chooser.getSelectedFile().getAbsolutePath());
		}
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

	private boolean accepts(File f) {
		String name = f.getName().toLowerCase(Locale.ROOT);
		int dot = name.lastIndexOf('.');
		String ext = dot < 0 ? "" : name.substring(dot + 1);
		if (!KNOWN_EXT.contains(ext) && !ImageEdit.isImageFile(f)) {
			return false;
		}
		boolean allowed;
		if (JPEG_EXT.contains(ext)) {
			allowed = chkJpeg.isSelected();
		} else if (ext.equals("png")) {
			allowed = chkPng.isSelected();
		} else if (ext.equals("webp")) {
			allowed = chkWebp.isSelected();
		} else if (ext.equals("gif")) {
			allowed = chkGif.isSelected();
		} else {
			allowed = chkOther.isSelected();
		}
		if (!allowed) {
			return false;
		}
		String text = fieldFilter.getText().trim().toLowerCase(Locale.ROOT);
		if (text.isEmpty()) {
			return true;
		}
		for (String token : text.split("[,;]")) {
			if (!token.trim().isEmpty() && name.contains(token.trim())) {
				return true;
			}
		}
		return false;
	}

	private void collect(File folder, File root, boolean recursive, List<Entry> found) {
		File[] children = folder.listFiles();
		if (children == null) {
			return;
		}
		Arrays.sort(children, new Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});
		for (File child : children) {
			if (child.isDirectory()) {
				if (recursive) {
					collect(child, root, true, found);
				}
			} else if (accepts(child)) {
				found.add(new Entry(child, root));
			}
		}
	}

	/** Adds files, and the images found in folders (sub-folders if ticked), without duplicates */
	private void addPaths(File[] paths) {
		if (paths == null || worker != null) {
			return;
		}
		Set<File> present = new HashSet<File>();
		for (Entry e : entries) {
			present.add(e.file);
		}
		List<Entry> found = new ArrayList<Entry>();
		for (File path : paths) {
			if (path.isDirectory()) {
				collect(path, path, chkSub.isSelected(), found);
			} else if (path.isFile() && accepts(path)) {
				found.add(new Entry(path, null));
			}
		}
		final List<Entry> added = new ArrayList<Entry>();
		for (Entry e : found) {
			if (present.add(e.file)) {
				entries.add(e);
				added.add(e);
			}
		}
		model.fireTableDataChanged();
		updateCount();
		scan(added);
	}

	private void removeSelected() {
		if (worker != null) {
			return;
		}
		int[] rows = table.getSelectedRows();
		for (int k = rows.length - 1; k >= 0; k--) {
			entries.remove(table.convertRowIndexToModel(rows[k]));
		}
		model.fireTableDataChanged();
		updateCount();
	}

	private void updateCount() {
		long total = 0;
		for (Entry e : entries) {
			total += e.size;
		}
		lblCount.setText(entries.size() + (entries.size() == 1 ? " file" : " files") + (entries.isEmpty() ? "" : " (" + ConversionUtils.coolFileSize(total) + ")"));
	}

	private void analyzeAll() {
		for (Entry e : entries) {
			e.metadata = "...";
		}
		model.fireTableDataChanged();
		scan(new ArrayList<Entry>(entries));
	}

	/** Looks inside the files in the background and fills the "Metadata found" column */
	private void scan(final List<Entry> toScan) {
		if (toScan.isEmpty()) {
			return;
		}
		new SwingWorker<Void, Entry>() {
			@Override
			protected Void doInBackground() {
				for (Entry e : toScan) {
					try {
						MetadataStripper.Result r = MetadataStripper.analyze(e.file);
						e.metadata = r.summary();
					} catch (IOException ex) {
						e.metadata = "unreadable";
					} catch (RuntimeException ex) {
						e.metadata = "unreadable";
					}
					publish(e);
				}
				return null;
			}

			@Override
			protected void process(List<Entry> chunks) {
				model.fireTableDataChanged();
			}

			@Override
			protected void done() {
				model.fireTableDataChanged();
			}
		}.execute();
	}

	// ------------------------------------------------------------------ run

	private MetadataStripper.Options readOptions() {
		MetadataStripper.Options o = new MetadataStripper.Options();
		o.mode = rbAll.isSelected() ? MetadataStripper.Mode.ALL : MetadataStripper.Mode.LOCATION;
		o.keepIcc = chkIcc.isSelected();
		o.keepOrientation = chkOrientation.isSelected();
		o.removeTrailing = chkTrailing.isSelected();
		o.reencodeOthers = chkReencode.isSelected();
		return o;
	}

	private void startOrCancel() {
		if (worker != null) {
			worker.cancel(true);
			return;
		}
		if (entries.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Add some files first.", "Clean metadata", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		boolean replace = rbReplace.isSelected();
		File target = null;
		File backup = null;
		if (replace) {
			String text = "<html>The metadata will be removed <b>from the original files</b>, which are overwritten:<br>"
					+ entries.size() + " files. This cannot be undone" + (chkBackup.isSelected() ? " (a backup of the originals is made)." : ", and you chose no backup.")
					+ "<br><br>Continue?</html>";
			Object[] options = {"Cancel", "Replace the files"};
			int choice = JOptionPane.showOptionDialog(this, text, "Replace the originals", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			if (choice != 1) {
				return;
			}
			if (chkBackup.isSelected()) {
				if (fieldBackup.getText().trim().isEmpty()) {
					JOptionPane.showMessageDialog(this, "Choose the backup folder.", "Clean metadata", JOptionPane.WARNING_MESSAGE);
					return;
				}
				backup = new File(fieldBackup.getText().trim());
			}
		} else {
			if (fieldTarget.getText().trim().isEmpty()) {
				JOptionPane.showMessageDialog(this, "Choose the target folder.", "Clean metadata", JOptionPane.WARNING_MESSAGE);
				fieldTarget.requestFocusInWindow();
				return;
			}
			target = new File(fieldTarget.getText().trim());
		}
		for (Entry e : entries) {
			e.result = "";
		}
		model.fireTableDataChanged();
		lastTarget = target;
		btnOpen.setEnabled(false);
		worker = new CleanWorker(new ArrayList<Entry>(entries), readOptions(), replace, target, backup, fieldPattern.getText().trim().isEmpty() ? "{name}" : fieldPattern.getText().trim(),
				comboCollision.getSelectedIndex(), chkStructure.isSelected(), chkCopyClean.isSelected(), chkDate.isSelected());
		btnRun.setText("Cancel");
		progress.setValue(0);
		progress.setMaximum(entries.size());
		worker.execute();
	}

	private void finished(String summary) {
		worker = null;
		updateOutputEnabled();
		lblProgress.setText(summary);
		btnOpen.setEnabled(lastTarget != null && lastTarget.isDirectory() && Desktop.isDesktopSupported());
		model.fireTableDataChanged();
	}

	private void openTarget() {
		try {
			Desktop.getDesktop().open(lastTarget);
		} catch (IOException | RuntimeException e) {
			logger.error("Cannot open the folder " + lastTarget, e);
		}
	}

	private void requestClose() {
		if (worker != null) {
			if (JOptionPane.showConfirmDialog(this, "A cleaning is running. Stop it and close?", "Clean metadata", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
				return;
			}
			worker.cancel(true);
		}
		dispose();
	}

	/** The cleaning, on a background thread: one file at a time, so a cancel always leaves whole files */
	private final class CleanWorker extends SwingWorker<Void, Integer> {
		private final List<Entry> list;
		private final MetadataStripper.Options options;
		private final boolean replace;
		private final File target;
		private final File backup;
		private final String pattern;
		private final int collision;
		private final boolean structure;
		private final boolean copyClean;
		private final boolean keepDate;
		private int cleaned;
		private int unchanged;
		private int failed;
		private int skipped;
		private long saved;

		CleanWorker(List<Entry> list, MetadataStripper.Options options, boolean replace, File target, File backup, String pattern, int collision, boolean structure, boolean copyClean,
				boolean keepDate) {
			this.list = list;
			this.options = options;
			this.replace = replace;
			this.target = target;
			this.backup = backup;
			this.pattern = pattern;
			this.collision = collision;
			this.structure = structure;
			this.copyClean = copyClean;
			this.keepDate = keepDate;
		}

		@Override
		protected Void doInBackground() {
			String today = new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date());
			for (int i = 0; i < list.size() && !isCancelled(); i++) {
				Entry e = list.get(i);
				try {
					e.result = handle(e, i + 1, today);
				} catch (IOException | RuntimeException ex) {
					failed++;
					e.result = "Error: " + ex.getMessage();
					logger.error("Cannot clean " + e.file, ex);
				}
				publish(i + 1);
			}
			return null;
		}

		private String handle(Entry e, int number, String today) throws IOException {
			long modified = e.file.lastModified();
			MetadataStripper.Result r = MetadataStripper.processFile(e.file, options);
			if (!r.supported) {
				skipped++;
				return "Not supported";
			}
			if (replace) {
				if (!r.changed()) {
					unchanged++;
					return "Nothing to remove";
				}
				if (backup != null) {
					File saved = unique(new File(backup, e.file.getName()));
					if (!backup.isDirectory() && !backup.mkdirs()) {
						throw new IOException("Cannot create the backup folder");
					}
					Files.copy(e.file.toPath(), saved.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
				}
				MetadataStripper.writeSafely(r.output, e.file);
				if (keepDate) {
					e.file.setLastModified(modified);
				}
				cleaned++;
				saved += e.size - r.output.length;
				return "Cleaned (-" + ConversionUtils.coolFileSize(Math.max(0, e.size - r.output.length)) + ")";
			}
			if (!r.changed() && !copyClean) {
				unchanged++;
				return "Nothing to remove (not copied)";
			}
			File destination = destinationOf(e, number, today);
			if (destination.exists()) {
				if (collision == 2) {
					skipped++;
					return "Skipped: " + destination.getName() + " exists";
				} else if (collision == 0) {
					destination = unique(destination);
				}
			}
			if (destination.getAbsoluteFile().equals(e.file.getAbsoluteFile())) {
				skipped++;
				return "Skipped: same file as the original";
			}
			if (r.changed()) {
				MetadataStripper.writeSafely(r.output, destination);
				cleaned++;
				saved += e.size - r.output.length;
			} else {
				File parent = destination.getAbsoluteFile().getParentFile();
				if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Cannot create the folder " + parent);
				}
				Files.copy(e.file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
				unchanged++;
			}
			if (keepDate) {
				destination.setLastModified(modified);
			}
			return r.changed() ? "Cleaned (-" + ConversionUtils.coolFileSize(Math.max(0, e.size - r.output.length)) + ")" : "Copied (no metadata)";
		}

		private File destinationOf(Entry e, int number, String today) {
			String name = e.file.getName();
			int dot = name.lastIndexOf('.');
			String base = dot < 0 ? name : name.substring(0, dot);
			String ext = dot < 0 ? "" : name.substring(dot);
			String newName = pattern.replace("{name}", base).replace("{n}", String.valueOf(number)).replace("{date}", today).replaceAll("[\\\\/:*?\"<>|]", "_") + ext;
			File folder = target;
			if (structure && e.root != null) {
				File parent = e.file.getParentFile();
				String rootPath = e.root.getAbsolutePath();
				String parentPath = parent == null ? "" : parent.getAbsolutePath();
				if (parentPath.length() > rootPath.length() && parentPath.startsWith(rootPath)) {
					folder = new File(target, parentPath.substring(rootPath.length() + 1));
				}
			}
			return new File(folder, newName);
		}

		private File unique(File wanted) {
			if (!wanted.exists()) {
				return wanted;
			}
			String name = wanted.getName();
			int dot = name.lastIndexOf('.');
			String base = dot < 0 ? name : name.substring(0, dot);
			String ext = dot < 0 ? "" : name.substring(dot);
			for (int n = 1; n < 100000; n++) {
				File candidate = new File(wanted.getParentFile(), base + "_" + n + ext);
				if (!candidate.exists()) {
					return candidate;
				}
			}
			return wanted;
		}

		@Override
		protected void process(List<Integer> chunks) {
			int done = chunks.get(chunks.size() - 1);
			progress.setValue(done);
			lblProgress.setText(done + " of " + list.size() + "...");
			model.fireTableDataChanged();
		}

		@Override
		protected void done() {
			String text = (isCancelled() ? "Stopped. " : "Done. ") + cleaned + " cleaned, " + unchanged + " without metadata" + (skipped > 0 ? ", " + skipped + " skipped" : "")
					+ (failed > 0 ? ", " + failed + " failed" : "") + (saved > 0 ? ", " + ConversionUtils.coolFileSize(saved) + " removed" : "");
			finished(text);
		}
	}
}
