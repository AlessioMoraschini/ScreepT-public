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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;

import gui.commons.frameutils.utils.GuiUtilsExtended;
import gui.commons.general.MyFileView;
import gui.commons.treeutils.TreeUtils;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.GuiUtils;
import various.common.light.i18n.I18n;
import various.common.light.utility.manipulation.ConversionUtils;

/**
 * Properties of a file or a folder. What is quick to know (name, path, size, dates, permissions) is shown at once; the rest is
 * read in the background and fills in while the window is open, so it opens immediately also on huge files or folders:
 * type, owner, volume space, image size, checksums (MD5, SHA-1, SHA-256, CRC-32 in one single pass, with progress), lines,
 * encoding and line endings of text files, and the number of files and the total size of a folder. A hash can be pasted in the
 * compare field to see whether the file matches it. Every color comes from the look and feel.
 */
public class FilePropertiesFrame extends JFrame {
	private static final long serialVersionUID = -1506126616913194676L;

	public static final Color defaultRed = Color.ORANGE;
	public static final Color defaultBlue = Color.CYAN;
	public static final Color defaultGreen = Color.GREEN;

	public static final int DEF_width = 640;
	public static final int DEF_height = 640;

	/** Over this size the checksums are calculated only when asked: reading the whole file takes a while */
	private static final long AUTO_HASH_LIMIT = 256L * 1024 * 1024;
	/** Text statistics are skipped over this size */
	private static final int MAX_FOLDER_ELEMENTS = 300000;

	private static final Color OK = new Color(46, 160, 90);
	private static final Color OFF = new Color(214, 140, 30);
	private static final Color INFO = new Color(58, 124, 214);

	public File fileUnderAnalysis;

	private final JPanel content = new JPanel(new MigLayout("insets 14 18 14 18, fillx, gapy 4, hidemode 3", "[130!, right][grow, fill][]"));
	private final List<SwingWorker<?, ?>> workers = new ArrayList<SwingWorker<?, ?>>();
	private final List<JComponent> hashSection = new ArrayList<JComponent>();
	private final List<JComponent> folderSection = new ArrayList<JComponent>();
	private final List<JComponent> textSection = new ArrayList<JComponent>();
	private final List<JComponent> imageSection = new ArrayList<JComponent>();

	private final JLabel icon = new JLabel();
	private final JLabel title = new JLabel();
	private final JLabel subtitle = new JLabel();

	private JTextField name;
	private JTextField path;
	private JTextField realPath;
	private JTextField folder;
	private JTextField type;
	private JTextField mime;
	private JTextField extension;
	private JTextField size;
	private JTextField created;
	private JTextField modified;
	private JTextField accessed;
	private JTextField owner;
	private JTextField attributes;
	private JTextField linkTarget;
	private JTextField volume;
	private JTextField encoding;
	private JTextField lines;
	private JTextField endings;
	private JTextField dimensions;
	private JTextField folderItems;
	private JTextField folderSize;
	private JTextField md5;
	private JTextField sha1;
	private JTextField sha256;
	private JTextField crc32;
	private JTextField compare;
	private JLabel compareResult;
	private JPanel chips;
	private JProgressBar progress;
	private JButton stop;
	private JButton calculate;

	public FilePropertiesFrame(File targetFile, boolean alwaysOnTop) {
		super();
		fileUnderAnalysis = targetFile;
		setAlwaysOnTop(alwaysOnTop);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		build();
		refreshValues(targetFile);
		JScrollPane scroll = new JScrollPane(content);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		getContentPane().add(buildHeader(), BorderLayout.NORTH);
		getContentPane().add(scroll, BorderLayout.CENTER);
		getContentPane().add(buildButtons(), BorderLayout.SOUTH);
		// fits the content, never more than the screen; scroll bars appear when it does not fit
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension wanted = getSize();
		setSize(Math.min(Math.max(wanted.width, 760), (int) (screen.width * 0.9)), Math.min(wanted.height, (int) (screen.height * 0.88)));
		setMinimumSize(new Dimension(460, 320));
		GuiUtilsExtended.centerComponent(this, getWidth(), getHeight());
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				cancelWorkers();
			}
		});
	}

	// ------------------------------------------------------------------ layout

	private Color muted() {
		Color back = UIManager.getColor("Panel.background") != null ? UIManager.getColor("Panel.background") : Color.GRAY;
		return TreeUtils.blend(TreeUtils.textOn(back), back, 0.42f);
	}

	private JPanel buildHeader() {
		JPanel header = new JPanel(new MigLayout("insets 14 18 10 18, fillx", "[48!][grow]", "[]"));
		title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
		subtitle.setForeground(muted());
		header.add(icon, "cell 0 0 1 2, aligny center");
		header.add(title, "cell 1 0, growx");
		header.add(subtitle, "cell 1 1, growx");
		return header;
	}

	private JPanel buildButtons() {
		JPanel bar = new JPanel(new MigLayout("insets 8 18 12 18, fillx", "[shrink 0][shrink 0][grow][shrink 0]"));
		JButton open = new JButton(I18n.t("gui.view_in_file_system_explorer"));
		open.setIcon(new ImageIcon(IconsPathConfigurator.ICON_COPY_DIR_STRUCTURE));
		open.setCursor(GuiUtils.CURSOR_HAND);
		open.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				GuiUtilsExtended.openInFileSystem(fileUnderAnalysis);
			}
		}).start());
		JButton copyPath = copyButton(I18n.t("props.copy_path"), new java.util.function.Supplier<String>() {
			@Override
			public String get() {
				return fileUnderAnalysis.getAbsolutePath();
			}
		});
		JButton refresh = new JButton(I18n.t("props.refresh"));
		refresh.setCursor(GuiUtils.CURSOR_HAND);
		refresh.addActionListener(e -> refreshValues(fileUnderAnalysis));
		bar.add(open);
		bar.add(copyPath);
		bar.add(new JLabel(), "growx");
		bar.add(refresh);
		return bar;
	}

	private JButton copyButton(final String label, final java.util.function.Supplier<String> text) {
		final JButton button = new JButton(label);
		button.setCursor(GuiUtils.CURSOR_HAND);
		button.setFocusable(false);
		button.addActionListener(e -> {
			String value = text.get();
			if (value != null && !value.isEmpty()) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
				button.setText(I18n.t("props.copied"));
				Timer back = new Timer(1200, ev -> button.setText(label));
				back.setRepeats(false);
				back.start();
			}
		});
		return button;
	}

	private void section(String text, List<JComponent> group) {
		JLabel label = new JLabel(text.toUpperCase());
		label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
		label.setForeground(muted());
		JPanel line = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(TreeUtils.blend(getBackground(), TreeUtils.textOn(getBackground()), 0.16f));
				g.fillRect(0, getHeight() / 2, getWidth(), 1);
			}
		};
		line.setPreferredSize(new Dimension(10, 8));
		content.add(label, "span 3, split 2, gaptop 12, left");
		content.add(line, "growx, gaptop 12, wrap");
		if (group != null) {
			group.add(label);
			group.add(line);
		}
	}

	/** A label and a value that can be selected and copied; the group lets whole parts be hidden */
	private JTextField row(String label, List<JComponent> group, boolean wide) {
		JLabel l = new JLabel(label);
		l.setForeground(muted());
		JTextField value = new JTextField();
		value.setEditable(false);
		value.setColumns(12);
		value.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		value.setOpaque(false);
		value.setForeground(UIManager.getColor("Label.foreground"));
		value.setCaretColor(value.getForeground());
		value.putClientProperty("rowLabel", l);
		content.add(l, "aligny center");
		content.add(value, "growx, wrap");
		if (group != null) {
			group.add(l);
			group.add(value);
		}
		return value;
	}

	private JTextField hashRow(String label) {
		JLabel l = new JLabel(label);
		l.setForeground(muted());
		final JTextField value = new JTextField();
		value.setEditable(false);
		value.setColumns(34);
		value.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		value.setOpaque(false);
		value.setFont(new Font(Font.MONOSPACED, Font.PLAIN, value.getFont().getSize()));
		value.setForeground(UIManager.getColor("Label.foreground"));
		JButton copy = copyButton(I18n.t("props.copy"), new java.util.function.Supplier<String>() {
			@Override
			public String get() {
				return value.getText();
			}
		});
		copy.setMargin(new java.awt.Insets(1, 8, 1, 8));
		content.add(l, "aligny center");
		content.add(value, "growx");
		content.add(copy, "wrap");
		hashSection.add(l);
		hashSection.add(value);
		hashSection.add(copy);
		return value;
	}

	private void build() {
		// ----- general
		section(I18n.t("props.section_general"), null);
		name = row(I18n.t("gui.file_name").replace(":", ""), null, false);
		path = row(I18n.t("gui.file_path").replace(":", ""), null, false);
		folder = row(I18n.t("props.parent"), null, false);
		realPath = row(I18n.t("props.real_path"), null, false);
		type = row(I18n.t("props.type"), null, false);
		extension = row(I18n.t("props.extension"), null, false);
		mime = row(I18n.t("props.mime"), null, false);
		size = row(I18n.t("gui.file_size").replace(":", ""), null, false);
		linkTarget = row(I18n.t("props.link_target"), null, false);

		// ----- dates
		section(I18n.t("props.section_dates"), null);
		created = row(I18n.t("gui.created_on").replace(":", ""), null, false);
		modified = row(I18n.t("gui.last_change").replace(":", ""), null, false);
		accessed = row(I18n.t("gui.last_access").replace(":", ""), null, false);

		// ----- access
		section(I18n.t("props.section_access"), null);
		chips = new JPanel(new MigLayout("insets 0, gap 6", "", ""));
		chips.setOpaque(false);
		content.add(chips, "span 3, growx, wrap");
		owner = row(I18n.t("props.owner"), null, false);
		attributes = row(I18n.t("props.attributes"), null, false);
		volume = row(I18n.t("props.volume"), null, false);

		// ----- image
		section(I18n.t("props.section_image"), imageSection);
		dimensions = row(I18n.t("props.dimensions"), imageSection, false);

		// ----- text
		section(I18n.t("props.section_content"), textSection);
		encoding = row(I18n.t("props.encoding"), textSection, false);
		lines = row(I18n.t("props.lines"), textSection, false);
		endings = row(I18n.t("props.line_endings"), textSection, false);

		// ----- folder
		section(I18n.t("props.section_folder"), folderSection);
		folderItems = row(I18n.t("props.items"), folderSection, false);
		folderSize = row(I18n.t("props.total_size"), folderSection, false);

		// ----- checksums
		section(I18n.t("props.section_hashes"), hashSection);
		md5 = hashRow("MD5");
		sha1 = hashRow("SHA-1");
		sha256 = hashRow("SHA-256");
		crc32 = hashRow("CRC-32");
		progress = new JProgressBar(0, 1000);
		progress.setPreferredSize(new Dimension(10, 6));
		progress.setBorderPainted(false);
		stop = new JButton(I18n.t("props.stop"));
		stop.setMargin(new java.awt.Insets(1, 8, 1, 8));
		calculate = new JButton(I18n.t("props.compute"));
		calculate.setMargin(new java.awt.Insets(1, 8, 1, 8));
		content.add(new JLabel(), "");
		content.add(progress, "growx, aligny center");
		content.add(stop, "split 2");
		content.add(calculate, "wrap");
		hashSection.add(progress);
		hashSection.add(stop);
		hashSection.add(calculate);
		compare = new JTextField(12);
		compare.putClientProperty("JTextField.placeholderText", I18n.t("props.compare_placeholder"));
		compareResult = new JLabel(" ");
		JLabel compareLabel = new JLabel(I18n.t("props.compare"));
		compareLabel.setForeground(muted());
		content.add(compareLabel, "aligny center");
		content.add(compare, "growx");
		content.add(compareResult, "wrap");
		hashSection.add(compareLabel);
		hashSection.add(compare);
		hashSection.add(compareResult);
		compare.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				checkCompare();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				checkCompare();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				checkCompare();
			}
		});
		stop.addActionListener(e -> cancelWorkers());
		calculate.addActionListener(e -> startContentPass(true));
	}

	private void checkCompare() {
		String typed = compare.getText().trim().replace(" ", "");
		if (typed.isEmpty()) {
			compareResult.setText(" ");
			return;
		}
		String found = null;
		for (Object[] pair : new Object[][] { { "MD5", md5 }, { "SHA-1", sha1 }, { "SHA-256", sha256 }, { "CRC-32", crc32 } }) {
			String value = ((JTextField) pair[1]).getText();
			if (!value.isEmpty() && value.equalsIgnoreCase(typed)) {
				found = (String) pair[0];
			}
		}
		compareResult.setText(found != null ? "✓ " + found : "✗");
		compareResult.setForeground(found != null ? OK : OFF);
	}

	/** Sets the text of a row and hides the row when there is nothing to show */
	private static void setRowText(JTextField field, String text) {
		field.setText(text);
		field.setCaretPosition(0);
		boolean visible = text != null && !text.isEmpty();
		field.setVisible(visible);
		Object label = field.getClientProperty("rowLabel");
		if (label instanceof JComponent) {
			((JComponent) label).setVisible(visible);
		}
	}

	private static void show(List<JComponent> group, boolean visible) {
		for (JComponent c : group) {
			c.setVisible(visible);
		}
	}

	// ------------------------------------------------------------------ values

	private void cancelWorkers() {
		for (SwingWorker<?, ?> w : workers) {
			w.cancel(true);
		}
		workers.clear();
		stop.setVisible(false);
		progress.setVisible(false);
	}

	private static String grouped(long value) {
		return new DecimalFormat("#,##0").format(value);
	}

	private static String when(long millis) {
		if (millis <= 0) {
			return "-";
		}
		String date = GeneralConfig.GENERIC_FORMATTER.format(new Date(millis));
		long seconds = Math.max(0, (System.currentTimeMillis() - millis) / 1000);
		String ago;
		if (seconds < 60) {
			ago = I18n.t("props.just_now");
		} else if (seconds < 3600) {
			ago = I18n.fmt("props.minutes_ago", seconds / 60);
		} else if (seconds < 86400) {
			ago = I18n.fmt("props.hours_ago", seconds / 3600);
		} else if (seconds < 86400L * 730) {
			ago = I18n.fmt("props.days_ago", seconds / 86400);
		} else {
			ago = I18n.fmt("props.years_ago", seconds / (86400L * 365));
		}
		return date + "   (" + ago + ")";
	}

	private void chip(String text, Color color) {
		chips.add(new Chip(text, color));
	}

	/**
	 * Updates every value for the given file: the quick ones at once, the rest in the background.
	 */
	public void refreshValues(File target) {
		if (target == null || !target.exists()) {
			return;
		}
		cancelWorkers();
		fileUnderAnalysis = target;
		final File file = target;
		final boolean directory = file.isDirectory();

		ImageIcon fileIcon = directory ? new ImageIcon(IconsPathConfigurator.iconFolderPath) : MyFileView.getExtIcon(file);
		icon.setIcon(fileIcon);
		GuiUtilsExtended.setFrameIcon(fileIcon, this);
		setTitle(file.getName().isEmpty() ? file.getPath() : file.getName());
		title.setText(file.getName().isEmpty() ? file.getPath() : file.getName());
		setRowText(name, file.getName());
		setRowText(path, file.getAbsolutePath());
		setRowText(folder, file.getAbsoluteFile().getParent() == null ? "" : file.getAbsoluteFile().getParent());
		String ext = "";
		int dot = file.getName().lastIndexOf('.');
		if (!directory && dot > 0 && dot < file.getName().length() - 1) {
			ext = file.getName().substring(dot + 1).toLowerCase(Locale.ROOT);
		}
		setRowText(extension, ext);
		setRowText(size, directory ? "" : ConversionUtils.coolFileSize(file.length()) + "   (" + grouped(file.length()) + " " + I18n.t("props.bytes") + ")");
		subtitle.setText(directory ? I18n.t("props.kind_folder") : (ext.isEmpty() ? I18n.t("props.kind_file") : ext.toUpperCase(Locale.ROOT) + " · " + I18n.t("props.kind_file"))
				+ "  ·  " + ConversionUtils.coolFileSize(file.length()));

		created.setText("...");
		modified.setText(when(file.lastModified()));
		accessed.setText("...");
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			created.setText(when(attr.creationTime().toMillis()));
			accessed.setText(when(attr.lastAccessTime().toMillis()));
			modified.setText(when(attr.lastModifiedTime().toMillis()));
		} catch (IOException e) {
			created.setText("-");
			accessed.setText("-");
		}

		chips.removeAll();
		chip(file.canRead() ? I18n.t("gui.can_read") : I18n.t("gui.cannot_read"), file.canRead() ? OK : OFF);
		chip(file.canWrite() ? I18n.t("gui.can_write") : I18n.t("gui.cannot_write"), file.canWrite() ? OK : OFF);
		chip(file.canExecute() ? I18n.t("gui.can_execute") : I18n.t("gui.cannot_execute"), file.canExecute() ? OK : OFF);
		chip(file.isHidden() ? I18n.t("gui.hidden_file") : I18n.t("gui.visible_file"), file.isHidden() ? INFO : OK);
		if (Files.isSymbolicLink(file.toPath())) {
			chip(I18n.t("props.symlink"), INFO);
		}
		chips.revalidate();
		chips.repaint();

		for (JTextField f : new JTextField[] { realPath, type, mime, owner, attributes, volume, linkTarget, dimensions }) {
			setRowText(f, "");
		}
		encoding.setText("");
		lines.setText("");
		endings.setText("");
		folderItems.setText("");
		folderSize.setText("");
		md5.setText("");
		sha1.setText("");
		sha256.setText("");
		crc32.setText("");
		compare.setText("");
		compareResult.setText(" ");

		show(hashSection, !directory);
		show(textSection, !directory);
		show(folderSection, directory);
		show(imageSection, false);
		stop.setVisible(false);
		calculate.setVisible(false);
		progress.setVisible(false);

		startMetaPass(file, directory, ext);
		if (directory) {
			startFolderPass(file);
		} else {
			startContentPass(file.length() <= AUTO_HASH_LIMIT);
		}
		revalidate();
	}

	private <T> void run(SwingWorker<T, ?> worker) {
		workers.add(worker);
		worker.execute();
	}

	// ------------------------------------------------------------------ background: type, owner, volume, image

	private static final class Meta {
		String realPath = "";
		String type = "";
		String mime = "";
		String owner = "";
		String attributes = "";
		String volume = "";
		String linkTarget = "";
		String dimensions = "";
	}

	private void startMetaPass(final File file, final boolean directory, final String ext) {
		run(new SwingWorker<Meta, Void>() {
			@Override
			protected Meta doInBackground() {
				Meta m = new Meta();
				Path p = file.toPath();
				try {
					m.realPath = file.getCanonicalPath();
				} catch (IOException e) {
					m.realPath = file.getAbsolutePath();
				}
				try {
					m.type = FileSystemView.getFileSystemView().getSystemTypeDescription(file);
				} catch (Throwable e) {
					m.type = "";
				}
				if (m.type == null) {
					m.type = "";
				}
				try {
					String probed = directory ? null : Files.probeContentType(p);
					m.mime = probed == null ? "" : probed;
				} catch (Throwable e) {
					m.mime = "";
				}
				try {
					m.owner = String.valueOf(Files.getOwner(p));
				} catch (Throwable e) {
					m.owner = "";
				}
				try {
					List<String> flags = new ArrayList<String>();
					try {
						DosFileAttributes dos = Files.readAttributes(p, DosFileAttributes.class);
						if (dos.isReadOnly()) {
							flags.add(I18n.t("props.read_only"));
						}
						if (dos.isHidden()) {
							flags.add(I18n.t("gui.hidden_file"));
						}
						if (dos.isSystem()) {
							flags.add(I18n.t("props.system"));
						}
						if (dos.isArchive()) {
							flags.add(I18n.t("props.archive"));
						}
					} catch (UnsupportedOperationException e) {
						m.attributes = PosixFilePermissions.toString(Files.getPosixFilePermissions(p));
					}
					if (!flags.isEmpty()) {
						StringBuilder sb = new StringBuilder();
						for (String f : flags) {
							sb.append(sb.length() == 0 ? "" : ", ").append(f);
						}
						m.attributes = sb.toString();
					}
				} catch (Throwable e) {
					// attributes not available on this system
				}
				try {
					if (Files.isSymbolicLink(p)) {
						m.linkTarget = String.valueOf(Files.readSymbolicLink(p));
					}
				} catch (Throwable e) {
					// not a readable link
				}
				try {
					FileStore store = Files.getFileStore(p);
					m.volume = store.toString() + "   ·   " + I18n.fmt("props.free_of", ConversionUtils.coolFileSize(store.getUsableSpace()),
							ConversionUtils.coolFileSize(store.getTotalSpace()));
				} catch (Throwable e) {
					m.volume = "";
				}
				if (!directory && !ext.isEmpty() && ImageIO.getImageReadersBySuffix(ext).hasNext()) {
					// the size is read from the header: the image is not decoded
					try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
						if (in != null) {
							java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
							if (readers.hasNext()) {
								ImageReader reader = readers.next();
								try {
									reader.setInput(in);
									int w = reader.getWidth(0);
									int h = reader.getHeight(0);
									m.dimensions = w + " × " + h + " px   ·   " + reader.getFormatName().toUpperCase(Locale.ROOT)
											+ "   ·   " + String.format(Locale.ROOT, "%.2f", w * h / 1000000.0) + " MP";
								} finally {
									reader.dispose();
								}
							}
						}
					} catch (Throwable e) {
						m.dimensions = "";
					}
				}
				return m;
			}

			@Override
			protected void done() {
				if (isCancelled()) {
					return;
				}
				try {
					Meta m = get();
					setRowText(realPath, m.realPath.equals(path.getText()) ? "" : m.realPath);
					setRowText(type, m.type);
					setRowText(mime, m.mime);
					setRowText(owner, m.owner);
					setRowText(attributes, m.attributes);
					setRowText(volume, m.volume);
					setRowText(linkTarget, m.linkTarget);
					setRowText(dimensions, m.dimensions);
					show(imageSection, !m.dimensions.isEmpty());
				} catch (Exception e) {
					// the quick values stay as they are
				}
			}
		});
	}

	// ------------------------------------------------------------------ background: folder

	private static final class FolderStats {
		long files;
		long folders;
		long bytes;
		boolean partial;
	}

	private void startFolderPass(final File dir) {
		folderItems.setText(I18n.t("props.calculating"));
		folderSize.setText(I18n.t("props.calculating"));
		run(new SwingWorker<FolderStats, FolderStats>() {
			@Override
			protected FolderStats doInBackground() throws Exception {
				final FolderStats stats = new FolderStats();
				final long[] lastPublish = { System.currentTimeMillis() };
				Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						stats.files++;
						stats.bytes += attrs.size();
						return step();
					}

					@Override
					public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) {
						if (!d.equals(dir.toPath())) {
							stats.folders++;
						}
						return step();
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) {
						return FileVisitResult.CONTINUE;
					}

					private FileVisitResult step() {
						if (isCancelled()) {
							return FileVisitResult.TERMINATE;
						}
						if (stats.files + stats.folders > MAX_FOLDER_ELEMENTS) {
							stats.partial = true;
							return FileVisitResult.TERMINATE;
						}
						long now = System.currentTimeMillis();
						if (now - lastPublish[0] > 150) {
							lastPublish[0] = now;
							FolderStats copy = new FolderStats();
							copy.files = stats.files;
							copy.folders = stats.folders;
							copy.bytes = stats.bytes;
							publish(copy);
						}
						return FileVisitResult.CONTINUE;
					}
				});
				return stats;
			}

			@Override
			protected void process(List<FolderStats> chunks) {
				if (!isCancelled() && !chunks.isEmpty()) {
					showFolder(chunks.get(chunks.size() - 1), true);
				}
			}

			@Override
			protected void done() {
				if (isCancelled()) {
					return;
				}
				try {
					showFolder(get(), false);
				} catch (Exception e) {
					folderItems.setText("-");
					folderSize.setText("-");
				}
			}
		});
	}

	private void showFolder(FolderStats s, boolean running) {
		String items = I18n.fmt("props.items_value", grouped(s.files), grouped(s.folders));
		if (s.partial) {
			items += "   ·   " + I18n.fmt("props.partial", grouped(MAX_FOLDER_ELEMENTS));
		}
		folderItems.setText(running ? items + " ..." : items);
		folderSize.setText(ConversionUtils.coolFileSize(s.bytes) + "   (" + grouped(s.bytes) + " " + I18n.t("props.bytes") + ")");
		subtitle.setText(I18n.t("props.kind_folder") + "  ·  " + items + "  ·  " + ConversionUtils.coolFileSize(s.bytes));
	}

	// ------------------------------------------------------------------ background: checksums and text, one pass

	private static final class Pass {
		String md5 = "";
		String sha1 = "";
		String sha256 = "";
		String crc32 = "";
		boolean binary;
		String encoding = "";
		long lines;
		String endings = "";
	}

	private void startContentPass(boolean automatic) {
		final File file = fileUnderAnalysis;
		if (!file.isFile()) {
			md5.setText(I18n.t("gui.not_a_file_cannot_hash"));
			return;
		}
		if (!automatic) {
			// big file: the user decides
			calculate.setVisible(true);
			md5.setText(I18n.t("props.hash_on_request"));
			return;
		}
		calculate.setVisible(false);
		stop.setVisible(true);
		progress.setVisible(true);
		progress.setValue(0);
		md5.setText(I18n.t("props.calculating"));
		sha1.setText(I18n.t("props.calculating"));
		sha256.setText(I18n.t("props.calculating"));
		crc32.setText(I18n.t("props.calculating"));
		encoding.setText(I18n.t("props.calculating"));
		final long total = Math.max(1, file.length());

		run(new SwingWorker<Pass, Integer>() {
			@Override
			protected Pass doInBackground() throws Exception {
				Pass result = new Pass();
				MessageDigest m5 = MessageDigest.getInstance("MD5");
				MessageDigest s1 = MessageDigest.getInstance("SHA-1");
				MessageDigest s256 = MessageDigest.getInstance("SHA-256");
				CRC32 crc = new CRC32();
				byte[] buffer = new byte[1 << 20];
				long done = 0;
				long lf = 0;
				long cr = 0;
				long crlf = 0;
				int previous = -1;
				boolean sniffed = false;
				long lastByteIsEol = 0;
				long lastPublish = 0;
				try (InputStream in = Files.newInputStream(file.toPath())) {
					int read;
					while ((read = in.read(buffer)) > 0) {
						if (isCancelled()) {
							return null;
						}
						m5.update(buffer, 0, read);
						s1.update(buffer, 0, read);
						s256.update(buffer, 0, read);
						crc.update(buffer, 0, read);
						if (!sniffed) {
							sniffed = true;
							int sniff = Math.min(read, 8192);
							for (int i = 0; i < sniff; i++) {
								if (buffer[i] == 0) {
									result.binary = true;
									break;
								}
							}
							result.encoding = guessEncoding(buffer, read);
						}
						if (!result.binary) {
							for (int i = 0; i < read; i++) {
								byte b = buffer[i];
								if (b == '\n') {
									if (previous == '\r') {
										crlf++;
										cr--;
									} else {
										lf++;
									}
								} else if (b == '\r') {
									cr++;
								}
								previous = b;
							}
						}
						done += read;
						long now = System.currentTimeMillis();
						if (now - lastPublish > 80) {
							lastPublish = now;
							publish((int) (done * 1000 / total));
						}
					}
				}
				lastByteIsEol = (previous == '\n' || previous == '\r') ? 1 : 0;
				result.md5 = hex(m5.digest());
				result.sha1 = hex(s1.digest());
				result.sha256 = hex(s256.digest());
				result.crc32 = String.format("%08x", crc.getValue());
				if (!result.binary) {
					long terminators = lf + cr + crlf;
					result.lines = done == 0 ? 0 : terminators + (lastByteIsEol == 1 ? 0 : 1);
					int kinds = (lf > 0 ? 1 : 0) + (cr > 0 ? 1 : 0) + (crlf > 0 ? 1 : 0);
					if (kinds == 0) {
						result.endings = "-";
					} else if (kinds > 1) {
						result.endings = I18n.t("props.eol_mixed") + " (CRLF " + grouped(crlf) + ", LF " + grouped(lf) + ", CR " + grouped(cr) + ")";
					} else {
						result.endings = crlf > 0 ? "CRLF (Windows)" : lf > 0 ? "LF (Unix)" : "CR (old Mac)";
					}
				}
				return result;
			}

			@Override
			protected void process(List<Integer> chunks) {
				if (!isCancelled() && !chunks.isEmpty()) {
					progress.setValue(chunks.get(chunks.size() - 1));
				}
			}

			@Override
			protected void done() {
				if (isCancelled()) {
					return;
				}
				stop.setVisible(false);
				progress.setVisible(false);
				try {
					Pass r = get();
					if (r == null) {
						return;
					}
					md5.setText(r.md5);
					sha1.setText(r.sha1);
					sha256.setText(r.sha256);
					crc32.setText(r.crc32);
					if (r.binary) {
						encoding.setText(I18n.t("props.binary"));
						setRowText(lines, "");
						setRowText(endings, "");
					} else {
						setRowText(encoding, r.encoding);
						setRowText(lines, grouped(r.lines));
						setRowText(endings, r.endings);
					}
					for (JTextField f : new JTextField[] { md5, sha1, sha256, crc32 }) {
						f.setCaretPosition(0);
					}
					checkCompare();
				} catch (Exception e) {
					md5.setText("-");
					sha1.setText("-");
					sha256.setText("-");
					crc32.setText("-");
					encoding.setText("-");
				}
			}
		});
	}

	private static String hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
		}
		return sb.toString();
	}

	/** BOM, then whether the beginning is valid UTF-8 (ASCII when it has no byte over 127) */
	private static String guessEncoding(byte[] data, int length) {
		if (length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
			return "UTF-8 (BOM)";
		}
		if (length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
			return "UTF-16 LE (BOM)";
		}
		if (length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
			return "UTF-16 BE (BOM)";
		}
		int sample = Math.min(length, 65536);
		boolean ascii = true;
		for (int i = 0; i < sample; i++) {
			if (data[i] < 0) {
				ascii = false;
				break;
			}
		}
		if (ascii) {
			return "ASCII";
		}
		try {
			// a multi-byte character cut by the end of the sample is not an error
			int end = sample;
			while (end > 0 && end < length && (data[end] & 0xC0) == 0x80) {
				end--;
			}
			java.nio.charset.Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data, 0, end));
			return "UTF-8";
		} catch (CharacterCodingException e) {
			return I18n.t("props.encoding_ansi");
		}
	}

	// ------------------------------------------------------------------ chip

	/** A small rounded tag colored by its state */
	private static final class Chip extends JComponent {
		private static final long serialVersionUID = 1L;
		private final String text;
		private final Color color;

		Chip(String text, Color color) {
			this.text = text;
			this.color = color;
			setFont(UIManager.getFont("Label.font") != null ? UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12f) : getFont());
		}

		@Override
		public Dimension getPreferredSize() {
			int w = getFontMetrics(getFont()).stringWidth(text) + 22;
			return new Dimension(w, getFontMetrics(getFont()).getHeight() + 8);
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 38));
			g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
			g.setColor(color);
			g.fillOval(8, getHeight() / 2 - 3, 6, 6);
			g.setColor(TreeUtils.blend(color, TreeUtils.textOn(getBackground() == null ? Color.GRAY : getBackground()), 0.35f));
			g.setFont(getFont());
			g.drawString(text, 19, (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
			g.dispose();
		}
	}
}
