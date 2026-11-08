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
package gui.commons.frameutils.frame;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.commons.frameutils.frame.arch.ToolIcons;
import gui.commons.frameutils.frame.arch.ToolStyle;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import initializer.configs.impl.om.TextEditorOption;
import net.miginfocom.swing.MigLayout;
import resources.IconsPathConfigurator;
import various.common.light.gui.ShortcutKeys;
import various.common.light.utility.manipulation.ConversionUtils;

/**
 * File and folder hashes with several algorithms in one pass (MD5, SHA family, SHA-3 when the JVM has it, CRC32,
 * Adler-32), computed in background with progress and cancel, compared with a pasted expected hash.
 * Files can be dropped anywhere in the window. Folder hash: XOR of the hashes of all the files inside (as before).
 */
public class HashCheckerFrame extends ParentFrame {
	private static final long serialVersionUID = -5018329398511796700L;

	public static HashCheckerFrame singletonInstance = null;

	public static String HINT_VERIFY = "Paste the expected hash: it is compared with every calculated one";

	private static final int BUFFER_SIZE = 1 << 20;
	private static final long PROGRESS_INTERVAL_MS = 120;
	private static final Pattern HEX_TOKEN = Pattern.compile("[0-9a-fA-F]{8,}");
	private static final String NOT_FOR_FOLDERS = "n/a for folders";

	/** Algorithms selected in this session (shared by the windows opened later) */
	private static final Set<String> SELECTED = new LinkedHashSet<>();

	private final List<Row> rows = new ArrayList<>();
	private final AtomicInteger generation = new AtomicInteger();

	private File currentFile;
	public JFrame parentFrame;
	private final TextEditorOption txtOptions;

	private JTextField pathField;
	private JLabel fileInfoLabel;
	private JTextField expectedField;
	private ToolStyle.Badge compareBadge;
	private JProgressBar progressBar;
	private JButton cancelButton;
	private JButton browseButton;
	private JButton recalcButton;
	private JCheckBox uppercaseCheck;
	private JCheckBox prefixCheck;
	private JLabel statusLabel;
	private JPanel fileCard;
	private boolean running;
	private boolean currentIsFolder;

	public HashCheckerFrame(File initialFile, JFrame parentFrame, TextEditorOption txtOptions) {
		super(parentFrame, null);

		this.txtOptions = txtOptions == null ? new TextEditorOption() : txtOptions;
		this.parentFrame = parentFrame;
		singletonInstance = this;

		logger.info(this.getClass().getName() + " - Starting...");

		setTitle("File Hash Checker");
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_HASHING, this);

		synchronized (SELECTED) {
			if (SELECTED.isEmpty()) {
				for (Algo algo : Algo.available()) {
					if (algo.defaultOn) {
						SELECTED.add(algo.name);
					}
				}
			}
		}

		buildGui();
		addHandlers();
		installFileDrop(getContentPane());

		resizeToDefault(false, true, false);
		setMinimumSize(getMinimumDimension());
		pack();
		setSize(getDefaultDimension());
		setLocationRelativeTo(parentFrame);
		setVisible(true);
		setAlwaysOnTop(true);

		prefillExpectedFromClipboard();
		loadFile(initialFile, true);
		if (currentFile == null) {
			showEmptyState();
		}

		logger.info(this.getClass().getName() + " - Started!");
	}

	// ---------------------------------------------------------------------------------------------------------------
	// GUI
	// ---------------------------------------------------------------------------------------------------------------

	private void buildGui() {

		JPanel content = new JPanel(new MigLayout("wrap 1, fill, insets 14 18 14 18, gapy 10, hidemode 3", "[grow,fill]", "[][][][grow,fill][]"));
		setContentPane(content);

		content.add(ToolStyle.header("File Hash Checker", "Drop a file or a folder anywhere in this window, or browse for it"));

		// FILE
		fileCard = ToolStyle.card(new MigLayout("insets 0, fillx, gap 8 6, hidemode 3", "[][grow,fill][][]", "[][]"));
		JLabel fileIcon = new JLabel(ToolIcons.of(ToolIcons.Kind.FOLDER, 22));
		pathField = new JTextField();
		pathField.setFont(ToolStyle.font(1f, Font.PLAIN));
		pathField.putClientProperty("JTextField.placeholderText", "Path of the file or folder to hash (Enter to load)");
		pathField.setToolTipText("Type or paste a path and press Enter, or drop a file on the window");
		browseButton = ToolStyle.button("Browse...", ToolIcons.Kind.FOLDER, "Choose a file or a folder (Ctrl+O)");
		recalcButton = ToolStyle.button("Recalculate", ToolIcons.Kind.REFRESH, "Calculate again the selected hashes (F5)");
		fileInfoLabel = ToolStyle.mutedLabel(" ");
		progressBar = new JProgressBar(0, 1000);
		progressBar.setStringPainted(true);
		progressBar.setVisible(false);
		cancelButton = ToolStyle.iconButton(ToolIcons.Kind.STOP, "Stop the calculation (Esc)");
		cancelButton.setVisible(false);

		fileCard.add(fileIcon, "spany 2, aligny top, gaptop 2");
		fileCard.add(pathField, "growx");
		fileCard.add(browseButton);
		fileCard.add(recalcButton, "wrap");
		fileCard.add(fileInfoLabel, "growx, wmin 0");
		fileCard.add(progressBar, "w 220!");
		fileCard.add(cancelButton, "wrap");
		content.add(fileCard);

		// EXPECTED HASH
		JPanel compareCard = ToolStyle.card(new MigLayout("insets 0, fillx, gap 8 6", "[][grow,fill][][]", "[][]"));
		expectedField = new JTextField();
		expectedField.setFont(ToolStyle.mono(1f));
		expectedField.putClientProperty("JTextField.placeholderText", HINT_VERIFY);
		expectedField.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
		expectedField.setToolTipText("Any format works: plain hex, \"SHA-256=...\", \"sha256sum\" lines... Case does not matter");
		JButton pasteButton = ToolStyle.iconButton(ToolIcons.Kind.PASTE, "Paste the hash from the clipboard");
		pasteButton.addActionListener(e -> {
			String clip = ToolStyle.clipboardText();
			if (clip != null) {
				expectedField.setText(clip.trim());
			}
		});
		compareBadge = ToolStyle.badge();
		compareCard.add(ToolStyle.caption("Expected"));
		compareCard.add(expectedField, "growx");
		compareCard.add(pasteButton, "wrap");
		compareCard.add(compareBadge, "skip 1, span 2, growx 0, wmin 0");
		content.add(compareCard);

		content.add(ToolStyle.caption("Hashes"), "gaptop 2");

		// RESULTS
		JPanel results = ToolStyle.card(new MigLayout("wrap 4, fillx, insets 0, gap 10 6", "[][grow,fill][][20!]"));
		for (Algo algo : Algo.available()) {
			Row row = new Row(algo);
			rows.add(row);
			results.add(row.check);
			results.add(row.value, "growx, wmin 120");
			results.add(row.copy);
			results.add(row.mark);
		}
		JScrollPane scroll = new JScrollPane(results);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		content.add(scroll, "grow, hmin 120");

		// FOOTER
		JPanel footer = new JPanel(new MigLayout("insets 0, fillx, gap 12", "[][][grow][]"));
		footer.setOpaque(false);
		uppercaseCheck = new JCheckBox("Uppercase");
		uppercaseCheck.setSelected(txtOptions.uppercase_hash);
		uppercaseCheck.setToolTipText("Show and copy the hashes in uppercase (the comparison ignores the case)");
		prefixCheck = new JCheckBox("Algorithm prefix");
		prefixCheck.setSelected(txtOptions.prefix_hash);
		prefixCheck.setToolTipText("Show and copy the hashes as ALGORITHM=hash (e.g. SHA-256=...)");
		statusLabel = ToolStyle.mutedLabel(" ");
		JButton copyAll = ToolStyle.button("Copy all", ToolIcons.Kind.COPY, "Copy file name and all the calculated hashes (Ctrl+Shift+C)");
		copyAll.addActionListener(e -> copyAll());
		footer.add(uppercaseCheck);
		footer.add(prefixCheck);
		footer.add(statusLabel, "growx, wmin 0");
		footer.add(copyAll);
		content.add(footer);
	}

	private void addHandlers() {

		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				generation.incrementAndGet();
				singletonInstance = null;
				isActive.set(false);
				logger.debug("Closing " + getClass().getName());
			}
		});

		pathField.addActionListener(e -> {
			String text = pathField.getText().trim();
			if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
				text = text.substring(1, text.length() - 1);
			}
			File typed = new File(text);
			if (typed.exists()) {
				loadFile(typed, true);
			} else {
				pathField.putClientProperty("JComponent.outline", "error");
				fileInfoLabel.setText("Not found: " + text);
			}
		});

		browseButton.addActionListener(e -> browse());
		recalcButton.addActionListener(e -> startHashing(false));
		cancelButton.addActionListener(e -> cancel());

		uppercaseCheck.addActionListener(e -> {
			txtOptions.setUppercase_hash(uppercaseCheck.isSelected());
			if (currentIsFolder) {
				// the folder hash combines the file hashes as text: their case changes the result
				startHashing(false);
			} else {
				refreshValues();
			}
		});
		prefixCheck.addActionListener(e -> {
			txtOptions.setPrefix_hash(prefixCheck.isSelected());
			refreshValues();
		});

		expectedField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				expectedChanged();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				expectedChanged();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				expectedChanged();
			}
		});

		ToolStyle.bindKey(this, KeyStroke.getKeyStroke(KeyEvent.VK_O, ShortcutKeys.MENU_DOWN_MASK), "hashBrowse", this::browse);
		ToolStyle.bindKey(this, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "hashRecalc", () -> startHashing(false));
		ToolStyle.bindKey(this, KeyStroke.getKeyStroke(KeyEvent.VK_C, ShortcutKeys.MENU_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "hashCopyAll", this::copyAll);
		ToolStyle.bindKey(this, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "hashEscape", () -> {
			if (running) {
				cancel();
			} else {
				dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
			}
		});
	}

	/** Files dropped on any part of the window (but the expected hash field, which accepts dropped text) */
	private void installFileDrop(Component component) {
		if (component == expectedField) {
			return;
		}
		new DropTarget(component, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetDragEvent event) {
				if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					event.acceptDrag(DnDConstants.ACTION_COPY);
					fileCard.setBorder(BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(ToolStyle.BLUE, 2, true), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
				} else {
					event.rejectDrag();
				}
			}

			@Override
			public void dragExit(DropTargetEvent event) {
				resetDropBorder();
			}

			@Override
			@SuppressWarnings("unchecked")
			public void drop(DropTargetDropEvent event) {
				resetDropBorder();
				try {
					if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
						event.rejectDrop();
						return;
					}
					event.acceptDrop(DnDConstants.ACTION_COPY);
					List<File> files = (List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					event.dropComplete(true);
					if (files != null && !files.isEmpty()) {
						loadFile(files.get(0), true);
					}
				} catch (Exception e) {
					logger.warn("Cannot read the dropped files", e);
					event.dropComplete(false);
				}
			}
		}, true);

		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				installFileDrop(child);
			}
		}
	}

	private void resetDropBorder() {
		fileCard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// ACTIONS
	// ---------------------------------------------------------------------------------------------------------------

	private void browse() {
		File readFile = fileChooser.fileAndDirRead(thisFrame);
		if (readFile != null && readFile.exists()) {
			loadFile(readFile, true);
		}
	}

	/** Calculates again the hashes of the loaded file */
	public void updateHash() {
		SwingUtilities.invokeLater(() -> startHashing(false));
	}

	public void loadFile(File toLoadFile, boolean calculateHash) {
		if (toLoadFile == null || !toLoadFile.exists()) {
			return;
		}
		currentFile = toLoadFile;
		currentIsFolder = toLoadFile.isDirectory();
		pathField.setText(toLoadFile.getAbsolutePath());
		pathField.putClientProperty("JComponent.outline", null);
		setTitle("File Hash Checker - " + toLoadFile.getName());
		fileInfoLabel.setText(describe(toLoadFile));
		for (Row row : rows) {
			row.raw = null;
		}
		refreshValues();

		if (calculateHash) {
			startHashing(false);
		}
	}

	private static String describe(File file) {
		String modified = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date(file.lastModified()));
		if (file.isDirectory()) {
			return "Folder, modified " + modified + ". Its hash combines (XOR) the hashes of all the files inside";
		}
		return ToolStyle.size(file.length()) + ", modified " + modified;
	}

	private void cancel() {
		if (!running) {
			return;
		}
		generation.incrementAndGet();
		finishRun();
		for (Row row : rows) {
			if (row.raw == null) {
				row.value.setText("");
			}
		}
		refreshValues();
		statusLabel.setText("Stopped");
	}

	/**
	 * Starts the calculation in background of the selected algorithms
	 * @param onlyMissing true to calculate only the selected rows without a value (an algorithm just ticked)
	 */
	private void startHashing(boolean onlyMissing) {

		if (currentFile == null || !currentFile.exists()) {
			showEmptyState();
			return;
		}

		final List<Algo> todo = new ArrayList<>();
		for (Row row : rows) {
			if (row.check.isSelected() && (!onlyMissing || row.raw == null)) {
				todo.add(row.algo);
				row.raw = null;
			}
		}
		if (todo.isEmpty()) {
			refreshValues();
			if (selectedRows() == 0) {
				compareBadge.setStatus("Tick at least one algorithm", null, null);
			}
			return;
		}

		final int run = generation.incrementAndGet();
		final File file = currentFile;
		final boolean folder = file.isDirectory();
		final boolean uppercase = uppercaseCheck.isSelected();

		running = true;
		progressBar.setVisible(true);
		progressBar.setIndeterminate(folder);
		progressBar.setValue(0);
		progressBar.setString(folder ? "Reading the folder..." : "0%");
		cancelButton.setVisible(true);
		recalcButton.setEnabled(false);
		statusLabel.setText(" ");
		for (Algo algo : todo) {
			Row row = rowOf(algo);
			row.value.setText("");
			row.value.putClientProperty("JTextField.placeholderText", "Calculating...");
			row.value.repaint();
		}
		compareBadge.setStatus(" ", null, null);

		Thread worker = new Thread(() -> {
			long start = System.nanoTime();
			try {
				final Map<Algo, String> result;
				final long[] stats = new long[2];
				if (folder) {
					result = hashFolder(file, todo, uppercase, run, stats);
				} else {
					result = hashFile(file, todo, run, true);
					stats[0] = file.length();
					stats[1] = 1;
				}
				final double seconds = (System.nanoTime() - start) / 1e9;
				SwingUtilities.invokeLater(() -> {
					if (run != generation.get()) {
						return;
					}
					finishRun();
					for (Map.Entry<Algo, String> entry : result.entrySet()) {
						rowOf(entry.getKey()).raw = entry.getValue();
					}
					refreshValues();
					statusLabel.setText(summary(folder, stats, seconds));
				});
			} catch (CancelledException e) {
				// a newer run or a stop: nothing to show
			} catch (Throwable e) {
				logger.error("An error occurred while calculating hash for file: " + file, e);
				SwingUtilities.invokeLater(() -> {
					if (run != generation.get()) {
						return;
					}
					finishRun();
					refreshValues();
					compareBadge.setStatus("Cannot read " + file.getName() + ": " + e.getMessage(), ToolIcons.of(ToolIcons.Kind.CROSS, 14, Color.WHITE), ToolStyle.RED);
				});
			}
		}, "HashCheckerWorker");
		worker.setDaemon(true);
		worker.start();
	}

	private void finishRun() {
		running = false;
		progressBar.setVisible(false);
		progressBar.setIndeterminate(false);
		cancelButton.setVisible(false);
		recalcButton.setEnabled(true);
	}

	private static String summary(boolean folder, long[] stats, double seconds) {
		String time = seconds < 1 ? Math.round(seconds * 1000) + " ms" : String.format(Locale.ROOT, "%.1f s", seconds);
		if (folder) {
			return stats[1] + " files, " + ToolStyle.size(stats[0]) + " in " + time;
		}
		if (seconds > 0.3) {
			return "Done in " + time + " (" + ToolStyle.size((long) (stats[0] / seconds)) + "/s)";
		}
		return "Done in " + time;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// HASHING (worker thread)
	// ---------------------------------------------------------------------------------------------------------------

	/** One read of the file updates all the algorithms. Plain stream: no memory mapping, the file is never locked */
	private Map<Algo, String> hashFile(File file, List<Algo> algos, int run, boolean showProgress) throws Exception {

		List<MessageDigest> digests = new ArrayList<>();
		List<Checksum> checksums = new ArrayList<>();
		for (Algo algo : algos) {
			if (algo.digest) {
				digests.add(MessageDigest.getInstance(algo.name));
			} else {
				checksums.add(algo.newChecksum());
			}
		}

		long total = Math.max(1, file.length());
		long done = 0;
		long lastUpdate = System.currentTimeMillis();
		long start = System.nanoTime();
		byte[] buffer = new byte[BUFFER_SIZE];

		try (InputStream in = new FileInputStream(file)) {
			int read;
			while ((read = in.read(buffer)) > 0) {
				if (run != generation.get()) {
					throw new CancelledException();
				}
				for (MessageDigest digest : digests) {
					digest.update(buffer, 0, read);
				}
				for (Checksum checksum : checksums) {
					checksum.update(buffer, 0, read);
				}
				done += read;
				long now = System.currentTimeMillis();
				if (showProgress && now - lastUpdate > PROGRESS_INTERVAL_MS) {
					lastUpdate = now;
					final int permille = (int) Math.min(1000, done * 1000 / total);
					final double seconds = (System.nanoTime() - start) / 1e9;
					final String speed = seconds > 0.2 ? " - " + ToolStyle.size((long) (done / seconds)) + "/s" : "";
					SwingUtilities.invokeLater(() -> {
						if (run == generation.get()) {
							progressBar.setValue(permille);
							progressBar.setString(permille / 10 + "%" + speed);
						}
					});
				}
			}
		}

		Map<Algo, String> result = new HashMap<>();
		int d = 0;
		int c = 0;
		for (Algo algo : algos) {
			if (algo.digest) {
				result.put(algo, ConversionUtils.printHexBinary(digests.get(d++).digest()));
			} else {
				result.put(algo, String.format("%08X", checksums.get(c++).getValue()));
			}
		}
		return result;
	}

	/**
	 * Same result of ChecksumHasher.getFolderHash: XOR of the text of the hashes of all the files (sub folders
	 * contribute with their own folder hash), so values calculated with the old window are still valid.
	 * Checksums (CRC32, Adler-32) are not defined for folders.
	 */
	private Map<Algo, String> hashFolder(File folder, List<Algo> algos, boolean uppercase, int run, long[] stats) throws Exception {
		Map<Algo, byte[]> accumulators = new HashMap<>();
		folderAccumulate(folder, algos, uppercase, run, stats, accumulators);
		Map<Algo, String> result = new HashMap<>();
		for (Algo algo : algos) {
			result.put(algo, algo.digest ? ConversionUtils.printHexBinary(accumulators.containsKey(algo) ? accumulators.get(algo) : new byte[0]) : NOT_FOR_FOLDERS);
		}
		return result;
	}

	private void folderAccumulate(File folder, List<Algo> algos, boolean uppercase, int run, long[] stats, Map<Algo, byte[]> accumulators) throws Exception {
		File[] contents = folder.listFiles();
		if (contents == null) {
			return;
		}
		List<Algo> digestAlgos = new ArrayList<>();
		for (Algo algo : algos) {
			if (algo.digest) {
				digestAlgos.add(algo);
			}
		}
		for (File current : contents) {
			if (run != generation.get()) {
				throw new CancelledException();
			}
			if (current.isDirectory()) {
				Map<Algo, byte[]> sub = new HashMap<>();
				folderAccumulate(current, digestAlgos, uppercase, run, stats, sub);
				for (Algo algo : digestAlgos) {
					String subHash = ConversionUtils.printHexBinary(sub.containsKey(algo) ? sub.get(algo) : new byte[0]);
					xorInto(accumulators, algo, subHash);
				}
			} else {
				Map<Algo, String> fileHashes;
				try {
					fileHashes = hashFile(current, digestAlgos, run, false);
				} catch (CancelledException e) {
					throw e;
				} catch (Exception e) {
					// unreadable file: it contributes nothing, as in the old folder hash
					logger.warn("Cannot hash " + current + ": " + e.getMessage());
					continue;
				}
				for (Algo algo : digestAlgos) {
					String hex = fileHashes.get(algo);
					xorInto(accumulators, algo, uppercase ? hex.toUpperCase(Locale.ROOT) : hex.toLowerCase(Locale.ROOT));
				}
				stats[0] += current.length();
				stats[1]++;
				final String progress = stats[1] + " files - " + current.getName();
				SwingUtilities.invokeLater(() -> {
					if (run == generation.get()) {
						progressBar.setString(progress);
					}
				});
			}
		}
	}

	private static void xorInto(Map<Algo, byte[]> accumulators, Algo algo, String hashText) throws Exception {
		byte[] current = accumulators.containsKey(algo) ? accumulators.get(algo) : new byte[0];
		accumulators.put(algo, xorBytes(hashText.getBytes("UTF-8"), current));
	}

	private static byte[] xorBytes(byte[] first, byte[] second) {
		byte[] longer = first.length >= second.length ? first : second;
		byte[] shorter = longer == first ? second : first;
		byte[] result = new byte[longer.length];
		for (int i = 0; i < longer.length; i++) {
			result[i] = i < shorter.length ? (byte) (longer[i] ^ shorter[i]) : longer[i];
		}
		return result;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// RESULTS AND COMPARISON
	// ---------------------------------------------------------------------------------------------------------------

	private String formatted(Row row) {
		if (row.raw == null) {
			return "";
		}
		if (NOT_FOR_FOLDERS.equals(row.raw)) {
			return row.raw;
		}
		String value = uppercaseCheck.isSelected() ? row.raw.toUpperCase(Locale.ROOT) : row.raw.toLowerCase(Locale.ROOT);
		return prefixCheck.isSelected() ? row.algo.name + "=" + value : value;
	}

	private void refreshValues() {
		for (Row row : rows) {
			boolean selected = row.check.isSelected();
			row.value.setText(formatted(row));
			row.value.setCaretPosition(0);
			row.value.setEnabled(selected);
			row.copy.setEnabled(row.raw != null && !NOT_FOR_FOLDERS.equals(row.raw));
			String placeholder;
			if (!selected) {
				placeholder = "Tick to calculate";
			} else if (currentFile == null) {
				placeholder = "Drop a file here or browse for it";
			} else {
				placeholder = running ? "Calculating..." : "Press F5 to calculate";
			}
			row.value.putClientProperty("JTextField.placeholderText", placeholder);
			row.value.repaint();
		}
		expectedChanged();
	}

	private void showEmptyState() {
		fileInfoLabel.setText("No file loaded: drop one here, paste its path above or press Browse (Ctrl+O)");
		refreshValues();
	}

	/** Compares the expected hash with every calculated value; ticks the algorithm its length suggests, if needed */
	private void expectedChanged() {

		for (Row row : rows) {
			row.mark.setIcon(null);
			row.value.putClientProperty("JComponent.outline", null);
		}

		List<String> tokens = new ArrayList<>();
		Matcher matcher = HEX_TOKEN.matcher(expectedField.getText());
		while (matcher.find()) {
			tokens.add(matcher.group().toUpperCase(Locale.ROOT));
		}

		if (expectedField.getText().trim().isEmpty()) {
			compareBadge.setStatus("Paste an expected hash to verify the file", null, null);
			return;
		}
		if (tokens.isEmpty()) {
			compareBadge.setStatus("This is not a hexadecimal hash", ToolIcons.of(ToolIcons.Kind.CROSS, 14, Color.WHITE), ToolStyle.ORANGE);
			return;
		}

		for (String token : tokens) {
			for (Row row : rows) {
				if (row.raw != null && row.raw.equalsIgnoreCase(token)) {
					row.mark.setIcon(ToolIcons.of(ToolIcons.Kind.CHECK, 16, ToolStyle.GREEN));
					row.value.putClientProperty("JComponent.outline", ToolStyle.GREEN);
					compareBadge.setStatus("Match: the file has the expected " + row.algo.name, ToolIcons.of(ToolIcons.Kind.CHECK, 14, Color.WHITE), ToolStyle.GREEN);
					return;
				}
			}
		}

		// no match: maybe the right algorithm is not ticked yet
		String token = tokens.get(tokens.size() - 1);
		Algo suggested = Algo.forHexLength(token.length());
		if (suggested != null && currentFile != null) {
			Row row = rowOf(suggested);
			if (row != null && !row.check.isSelected()) {
				row.check.setSelected(true);
				rememberSelection(row);
				startHashing(true);
				return;
			}
		}
		if (running) {
			compareBadge.setStatus("Comparing when the calculation ends...", null, null);
			return;
		}
		if (currentFile == null) {
			compareBadge.setStatus("Load a file to compare it with this hash", null, null);
			return;
		}
		String hint = suggested != null ? " (" + token.length() + " digits: " + Algo.namesForHexLength(token.length()) + ")" : " (" + token.length() + " digits)";
		compareBadge.setStatus("No match: different file or algorithm" + hint, ToolIcons.of(ToolIcons.Kind.CROSS, 14, Color.WHITE), ToolStyle.RED);
	}

	/** A hash in the clipboard when the window opens is proposed as expected value */
	private void prefillExpectedFromClipboard() {
		String clip = ToolStyle.clipboardText();
		if (clip == null) {
			return;
		}
		clip = clip.trim();
		if (clip.length() <= 160 && clip.matches("(?i)([a-z0-9/-]+[=:]\\s*)?[0-9a-f]{8,128}") && Algo.forHexLength(clip.replaceAll("(?i)^[a-z0-9/-]+[=:]\\s*", "").length()) != null) {
			expectedField.setText(clip);
		}
	}

	private void copyAll() {
		StringBuilder text = new StringBuilder();
		if (currentFile != null) {
			text.append(currentFile.getName()).append(System.lineSeparator());
		}
		for (Row row : rows) {
			if (row.raw != null && !NOT_FOR_FOLDERS.equals(row.raw)) {
				String value = uppercaseCheck.isSelected() ? row.raw.toUpperCase(Locale.ROOT) : row.raw.toLowerCase(Locale.ROOT);
				text.append(row.algo.name).append(": ").append(value).append(System.lineSeparator());
			}
		}
		ToolStyle.copyToClipboard(text.toString());
		statusLabel.setText("Copied " + selectedRowsWithValue() + " hashes");
	}

	private int selectedRows() {
		int count = 0;
		for (Row row : rows) {
			count += row.check.isSelected() ? 1 : 0;
		}
		return count;
	}

	private int selectedRowsWithValue() {
		int count = 0;
		for (Row row : rows) {
			count += row.raw != null && !NOT_FOR_FOLDERS.equals(row.raw) ? 1 : 0;
		}
		return count;
	}

	private Row rowOf(Algo algo) {
		for (Row row : rows) {
			if (row.algo == algo) {
				return row;
			}
		}
		return null;
	}

	private static void rememberSelection(Row row) {
		synchronized (SELECTED) {
			if (row.check.isSelected()) {
				SELECTED.add(row.algo.name);
			} else {
				SELECTED.remove(row.algo.name);
			}
		}
	}

	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(900, 640);
	}

	@Override
	public Dimension getMinimumDimension() {
		return new Dimension(640, 460);
	}

	public static HashCheckerFrame getInstance(File initialFile, JFrame parentFrame, TextEditorOption txtOptions) {
		return singletonInstance == null ? new HashCheckerFrame(initialFile, parentFrame, txtOptions) : singletonInstance;
	}

	// ---------------------------------------------------------------------------------------------------------------

	/** One algorithm row: tick, value, copy button, match mark */
	private final class Row {
		final Algo algo;
		final JCheckBox check;
		final JTextField value;
		final JButton copy;
		final JLabel mark = new JLabel();
		String raw;

		Row(Algo algo) {
			this.algo = algo;
			check = new JCheckBox(algo.name);
			check.setFont(ToolStyle.font(1f, Font.BOLD));
			synchronized (SELECTED) {
				check.setSelected(SELECTED.contains(algo.name));
			}
			check.setToolTipText(algo.description);
			value = new JTextField();
			value.setEditable(false);
			value.setFont(ToolStyle.mono(0f));
			value.setToolTipText("Click to select, double click to copy");
			copy = ToolStyle.iconButton(ToolIcons.Kind.COPY, "Copy the " + algo.name + " hash");

			check.addActionListener(e -> {
				rememberSelection(this);
				if (check.isSelected() && raw == null && currentFile != null) {
					startHashing(true);
				} else {
					refreshValues();
				}
			});
			copy.addActionListener(e -> copyValue());
			value.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					value.selectAll();
					if (e.getClickCount() == 2) {
						copyValue();
					}
				}
			});
		}

		private void copyValue() {
			if (raw != null && !NOT_FOR_FOLDERS.equals(raw)) {
				ToolStyle.copyToClipboard(formatted(this));
				statusLabel.setText(algo.name + " copied");
			}
		}
	}

	/** Supported algorithm: message digest (from the JVM providers) or checksum */
	private static final class Algo {

		private static List<Algo> available;

		final String name;
		final boolean digest;
		final boolean defaultOn;
		final int hexLength;
		final String description;

		private Algo(String name, boolean digest, boolean defaultOn, int hexLength, String description) {
			this.name = name;
			this.digest = digest;
			this.defaultOn = defaultOn;
			this.hexLength = hexLength;
			this.description = description;
		}

		Checksum newChecksum() {
			return "CRC32".equals(name) ? new CRC32() : new Adler32();
		}

		static synchronized List<Algo> available() {
			if (available == null) {
				List<Algo> list = new ArrayList<>();
				addDigest(list, "MD5", true, 32, "Fast, fine to spot corrupted downloads; not secure against tampering");
				addDigest(list, "SHA-1", true, 40, "Still common in old checksum lists; not secure against tampering");
				addDigest(list, "SHA-224", false, 56, "SHA-2 family, 224 bit");
				addDigest(list, "SHA-256", true, 64, "SHA-2, the most used today for downloads and releases");
				addDigest(list, "SHA-384", false, 96, "SHA-2 family, 384 bit");
				addDigest(list, "SHA-512", true, 128, "SHA-2 family, 512 bit (ScreepT uses it for its updates)");
				addDigest(list, "SHA-512/224", false, 56, "SHA-512 truncated to 224 bit");
				addDigest(list, "SHA-512/256", false, 64, "SHA-512 truncated to 256 bit");
				// SHA-3 is in the JVM from Java 9: shown only when available
				addDigest(list, "SHA3-224", false, 56, "SHA-3 family, 224 bit");
				addDigest(list, "SHA3-256", false, 64, "SHA-3 family, 256 bit");
				addDigest(list, "SHA3-384", false, 96, "SHA-3 family, 384 bit");
				addDigest(list, "SHA3-512", false, 128, "SHA-3 family, 512 bit");
				list.add(new Algo("CRC32", false, false, 8, "32 bit checksum (zip, png...): detects accidental errors only"));
				list.add(new Algo("Adler-32", false, false, 8, "32 bit checksum (zlib): detects accidental errors only"));
				available = Collections.unmodifiableList(list);
			}
			return available;
		}

		private static void addDigest(List<Algo> list, String name, boolean defaultOn, int hexLength, String description) {
			try {
				MessageDigest.getInstance(name);
				list.add(new Algo(name, true, defaultOn, hexLength, description));
			} catch (Exception e) {
				// not provided by this JVM
			}
		}

		/** The most common algorithm producing a hex hash of this length */
		static Algo forHexLength(int length) {
			String[] preferred = {"MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512", "CRC32"};
			for (String name : preferred) {
				for (Algo algo : available()) {
					if (algo.name.equals(name) && algo.hexLength == length) {
						return algo;
					}
				}
			}
			return null;
		}

		static String namesForHexLength(int length) {
			StringBuilder names = new StringBuilder();
			for (Algo algo : available()) {
				if (algo.hexLength == length) {
					names.append(names.length() > 0 ? ", " : "").append(algo.name);
				}
			}
			return names.toString();
		}
	}

	private static final class CancelledException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}
