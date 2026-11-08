package various.common.light.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.miginfocom.swing.MigLayout;
import various.common.light.files.CustomFileFilters;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.string.StringWorker;

/**
 * License reader / acceptance dialog, shared by the launcher and the app.
 * Left: the available documents (ScreepT license, libraries, detailed reports) with a filter; right: the text with
 * find (Ctrl+F, F3 / Shift+F3) and zoom (Ctrl +/-/0); bottom: Accept / Decline when the choice is mandatory.
 * Colors come from the current look and feel, so it works with the dark and the light themes.
 */
public class LightLicenseViewer extends JDialog {
	private static final long serialVersionUID = -4557971895329584958L;

	private static File MAIN_PARENT_FOLDER = new File(System.getProperty("user.dir")).getParentFile();
	public static File MAIN_LIBRARIES_FOLDER = new File(MAIN_PARENT_FOLDER.getAbsolutePath() + File.separator + "Libraries_licenses");
	public static File MAIN_LIBRARIES_FOLDER_DETAIL = new File(MAIN_LIBRARIES_FOLDER.getAbsolutePath() + File.separator + "detailedReport");

	public enum UserLicenseChoice{
		ACCEPTED,
		REJECTED;
	}

	private static final Dimension DEFAULT_PREF_DIMENSION = GuiUtils.getScreenSizePerc(75);
	private static final Color GREEN = new Color(61, 155, 92);

	public UserLicenseChoice userChoice = UserLicenseChoice.REJECTED;
	File sourceLicense;

	private JScrollPane scrollPane;
	private JEditorPane jEditorPane;
	private JLabel lblNewLabel;
	private JLabel lblSubtitle;
	private JButton btnDecline;
	private JButton btnAccept;
	private JButton btnBrowser;
	private JTextField findField;
	private JLabel findCount;
	private JList<Object> docList;
	private DefaultListModel<Object> docModel;
	private JTextField docFilter;

	private JOptionHelper dialogHelper;

	public boolean licenseLoadedCorrectly;

	public static LightLicenseViewer instance = null;

	public static boolean mandatoryChoice = true;

	private File loadedLicense;
	private float fontSize;

	/** Visibility flags: 0 ScreepT license, 1 libraries, 2 detailed reports, 3 open with browser */
	private boolean[] visible = {true, true, true, true};

	private final List<int[]> matches = new ArrayList<int[]>();
	private int currentMatch = -1;
	private final List<Object> allDocs = new ArrayList<Object>();

	/** A document of the sidebar: header rows have no file */
	private static final class Doc {
		final String label;
		final File file;

		Doc(String label, File file) {
			this.label = label;
			this.file = file;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	public LightLicenseViewer(File sourceLicense, String header, String title) {
		this(sourceLicense, header, title, null);
	}

	public LightLicenseViewer(File sourceLicense, String header, String title, boolean... submenuVisible) {

		instance = this;

		loadedLicense = sourceLicense;

		String correctHeader = !StringWorker.isEmpty(header) ? header : "Accept licence to continue using this software";
		String correctTitle = !StringWorker.isEmpty(title) ? title : "EULA - acceptance";

		this.licenseLoadedCorrectly = false;
		this.sourceLicense = sourceLicense;
		this.dialogHelper = new JOptionHelper(this);
		if (submenuVisible != null) {
			for (int i = 0; i < visible.length && i < submenuVisible.length; i++) {
				visible[i] = submenuVisible[i];
			}
		}

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setPreferredSize(DEFAULT_PREF_DIMENSION);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setAlwaysOnTop(true);
		toFront();
		setTitle(correctTitle);

		Font base = UIManager.getFont("Label.font");
		fontSize = base != null ? base.getSize2D() + 1f : 14f;

		boolean showSidebar = (visible[1] && hasFiles(MAIN_LIBRARIES_FOLDER)) || (visible[2] && hasFiles(MAIN_LIBRARIES_FOLDER_DETAIL));

		getContentPane().setLayout(new MigLayout("fill, insets 16, gapy 10", "[grow,fill]", "[][grow,fill][]"));

		// header
		JPanel headerPanel = new JPanel(new MigLayout("insets 0, wrap 1, gapy 2", "[grow,fill]"));
		headerPanel.setOpaque(false);
		lblNewLabel = new JLabel(correctHeader);
		lblNewLabel.setFont(deriveFont(base, 6f, Font.BOLD));
		headerPanel.add(lblNewLabel);
		lblSubtitle = new JLabel(" ");
		lblSubtitle.setForeground(muted());
		headerPanel.add(lblSubtitle);
		getContentPane().add(headerPanel, "wrap");

		// reader
		jEditorPane = new JEditorPane();
		jEditorPane.setEditable(false);
		jEditorPane.setCursor(GuiUtils.CURSOR_TEXT);
		scrollPane = new JScrollPane(jEditorPane);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		JPanel reader = new JPanel(new BorderLayout(0, 6));
		reader.setOpaque(false);
		reader.add(buildFindBar(), BorderLayout.NORTH);
		reader.add(scrollPane, BorderLayout.CENTER);

		if (showSidebar) {
			JPanel body = new JPanel(new MigLayout("insets 0, gapx 12", "[220!][grow,fill]", "[grow,fill]"));
			body.setOpaque(false);
			body.add(buildSidebar());
			body.add(reader);
			getContentPane().add(body, "grow, wrap");
		} else {
			getContentPane().add(reader, "grow, wrap");
		}

		// footer
		JPanel footer = new JPanel(new MigLayout("insets 0, gapx 8", "[][grow][][]", "[]"));
		footer.setOpaque(false);
		btnBrowser = new JButton("Open in browser");
		btnBrowser.setCursor(GuiUtils.CURSOR_HAND);
		btnBrowser.setToolTipText("Open the document shown here with the default browser");
		btnBrowser.setVisible(visible[3]);
		footer.add(btnBrowser);
		footer.add(new JLabel(), "growx");

		btnDecline = new JButton("Decline");
		btnDecline.setCursor(GuiUtils.CURSOR_HAND);
		btnDecline.setPreferredSize(new Dimension(130, 34));

		btnAccept = new JButton("Accept");
		btnAccept.setCursor(GuiUtils.CURSOR_HAND);
		btnAccept.setBackground(GREEN);
		btnAccept.setForeground(Color.WHITE);
		btnAccept.setOpaque(true);
		btnAccept.putClientProperty("JButton.buttonType", "roundRect");
		btnAccept.setFont(deriveFont(btnAccept.getFont(), 0f, Font.BOLD));
		btnAccept.setPreferredSize(new Dimension(150, 34));

		if (mandatoryChoice) {
			footer.add(btnDecline);
			footer.add(btnAccept);
		} else {
			JButton close = new JButton("Close");
			close.setCursor(GuiUtils.CURSOR_HAND);
			close.setPreferredSize(new Dimension(130, 34));
			close.addActionListener((e) -> dispose());
			footer.add(close, "skip 1");
		}
		getContentPane().add(footer, "growx");

		applyReaderStyle();
		loadFile(sourceLicense);

		GuiUtils.centerComponent(this, DEFAULT_PREF_DIMENSION);

		addHandlers();

		setVisible(true);
	}

	private static boolean hasFiles(File folder) {
		File[] files = folder == null ? null : folder.listFiles();
		return files != null && files.length > 0;
	}

	private static Font deriveFont(Font font, float delta, int style) {
		Font origin = font != null ? font : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
		return origin.deriveFont(style, origin.getSize2D() + delta);
	}

	private static Color muted() {
		Color c = UIManager.getColor("Label.disabledForeground");
		return c != null ? c : Color.GRAY;
	}

	private static String hex(Color c) {
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	private JPanel buildFindBar() {
		JPanel bar = new JPanel(new MigLayout("insets 0, gapx 6", "[grow,fill][][][]", "[]"));
		bar.setOpaque(false);
		findField = new JTextField();
		findField.putClientProperty("JTextField.placeholderText", "Find in the text (Ctrl+F)");
		findField.setToolTipText("Find in the text: Enter or F3 next, Shift+F3 previous");
		bar.add(findField);
		findCount = new JLabel(" ");
		findCount.setForeground(muted());
		findCount.setPreferredSize(new Dimension(70, 10));
		bar.add(findCount);
		JButton prev = new JButton("Prev");
		prev.setCursor(GuiUtils.CURSOR_HAND);
		prev.addActionListener((e) -> stepMatch(-1));
		bar.add(prev);
		JButton next = new JButton("Next");
		next.setCursor(GuiUtils.CURSOR_HAND);
		next.addActionListener((e) -> stepMatch(1));
		bar.add(next);
		return bar;
	}

	private JComponent buildSidebar() {
		JPanel side = new JPanel(new BorderLayout(0, 6));
		side.setOpaque(false);
		docFilter = new JTextField();
		docFilter.putClientProperty("JTextField.placeholderText", "Filter documents");
		side.add(docFilter, BorderLayout.NORTH);

		docModel = new DefaultListModel<Object>();
		docList = new JList<Object>(docModel);
		docList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		docList.setCursor(GuiUtils.CURSOR_HAND);
		docList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				boolean header = value instanceof String;
				super.getListCellRendererComponent(list, value, index, isSelected && !header, false);
				setBorder(BorderFactory.createEmptyBorder(header ? 8 : 3, header ? 6 : 14, 3, 6));
				if (header) {
					setFont(deriveFont(list.getFont(), -1f, Font.BOLD));
					setForeground(muted());
					setText(((String) value).toUpperCase());
				}
				return this;
			}
		});
		docList.addListSelectionListener((e) -> {
			Object selected = docList.getSelectedValue();
			if (!e.getValueIsAdjusting() && selected instanceof Doc) {
				loadFile(((Doc) selected).file);
			}
		});
		side.add(new JScrollPane(docList), BorderLayout.CENTER);

		allDocs.clear();
		if (visible[0]) {
			allDocs.add("License");
			allDocs.add(new Doc("ScreepT license", sourceLicense));
		}
		if (visible[1]) {
			addGroup("Libraries", sortedFiles(MAIN_LIBRARIES_FOLDER));
		}
		if (visible[2]) {
			List<File> screept = new ArrayList<File>();
			List<File> plugins = new ArrayList<File>();
			List<File> details = FileWorker.getAllContent(MAIN_LIBRARIES_FOLDER_DETAIL, CustomFileFilters.FILE_ONLY);
			if (details != null) {
				for (File detail : details) {
					(detail.getName().toLowerCase().contains("screept") ? screept : plugins).add(detail);
				}
			}
			addGroup("Detailed reports", screept);
			addGroup("Plugins reports", plugins);
		}
		fillDocList("");

		docFilter.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { fillDocList(docFilter.getText()); }
			@Override
			public void removeUpdate(DocumentEvent e) { fillDocList(docFilter.getText()); }
			@Override
			public void changedUpdate(DocumentEvent e) { fillDocList(docFilter.getText()); }
		});
		return side;
	}

	private static List<File> sortedFiles(File folder) {
		File[] files = folder == null ? null : folder.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.FILE_ONLY));
		List<File> list = files == null ? new ArrayList<File>() : new ArrayList<File>(Arrays.asList(files));
		Collections.sort(list, new Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});
		return list;
	}

	private void addGroup(String title, List<File> files) {
		if (files.isEmpty()) {
			return;
		}
		allDocs.add(title);
		for (File file : files) {
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			String label = (dot > 0 ? name.substring(0, dot) : name).replace("_License", "").replace('_', ' ');
			allDocs.add(new Doc(label, file));
		}
	}

	/** Shows the documents matching the filter; a group header stays only if one of its documents does */
	private void fillDocList(String filter) {
		String needle = filter == null ? "" : filter.trim().toLowerCase();
		docModel.clear();
		String pendingHeader = null;
		Object toSelect = null;
		for (Object entry : allDocs) {
			if (entry instanceof String) {
				pendingHeader = (String) entry;
			} else if (needle.isEmpty() || ((Doc) entry).label.toLowerCase().contains(needle)) {
				if (pendingHeader != null) {
					docModel.addElement(pendingHeader);
					pendingHeader = null;
				}
				docModel.addElement(entry);
				if (loadedLicense != null && ((Doc) entry).file.equals(loadedLicense)) {
					toSelect = entry;
				}
			}
		}
		if (toSelect != null) {
			docList.setSelectedValue(toSelect, true);
		}
	}

	public void addHandlers() {

		// default close operation(same as reset)
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (mandatoryChoice && !askExitRejecting()) {
					setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
					return;
				} else {
					setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					instance = null;
					dispose();
				}
			}
		});

		btnAccept.addActionListener((e) -> {
			exitAccepting();
		});

		btnDecline.addActionListener((e) -> {
			askExitRejecting();
		});

		btnBrowser.addActionListener((e) -> {
			try {
				Desktop.getDesktop().browse(FileVarious.getCanonicalFileSafe(loadedLicense).toURI());
			} catch (Exception e1) {
				dialogHelper.warn("Cannot open the browser: " + e1.getMessage());
			}
		});

		findField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { runFind(); }
			@Override
			public void removeUpdate(DocumentEvent e) { runFind(); }
			@Override
			public void changedUpdate(DocumentEvent e) { runFind(); }
		});
		findField.addActionListener((e) -> stepMatch(1));

		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F, ShortcutKeys.MENU_DOWN_MASK), "find", () -> {
			findField.requestFocusInWindow();
			findField.selectAll();
		});
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escapeClose", () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "findNext", () -> stepMatch(1));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), "findPrev", () -> stepMatch(-1));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ShortcutKeys.MENU_DOWN_MASK), "zoomIn", () -> zoom(1f));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, ShortcutKeys.MENU_DOWN_MASK), "zoomIn2", () -> zoom(1f));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ShortcutKeys.MENU_DOWN_MASK), "zoomOut", () -> zoom(-1f));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ShortcutKeys.MENU_DOWN_MASK), "zoomOut2", () -> zoom(-1f));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_0, ShortcutKeys.MENU_DOWN_MASK), "zoomReset", () -> {
			Font base = UIManager.getFont("Label.font");
			fontSize = base != null ? base.getSize2D() + 1f : 14f;
			applyReaderStyle();
			loadFile(loadedLicense);
		});
	}

	private void bindKey(KeyStroke key, String name, final Runnable action) {
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
		getRootPane().getActionMap().put(name, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	private void zoom(float delta) {
		fontSize = Math.max(9f, Math.min(40f, fontSize + delta));
		applyReaderStyle();
		loadFile(loadedLicense);
	}

	/** Reader colors and fonts follow the look and feel (dark or light) */
	private void applyReaderStyle() {
		Color bg = UIManager.getColor("EditorPane.background");
		if (bg == null) {
			bg = UIManager.getColor("TextField.background");
		}
		Color fg = UIManager.getColor("EditorPane.foreground");
		if (fg == null) {
			fg = UIManager.getColor("Label.foreground");
		}
		bg = bg != null ? bg : Color.WHITE;
		fg = fg != null ? fg : Color.BLACK;
		Color link = UIManager.getColor("Component.accentColor");
		link = link != null ? link : new Color(58, 124, 214);
		Font base = UIManager.getFont("Label.font");
		String family = base != null ? base.getFamily() : "SansSerif";

		HTMLEditorKit kit = new HTMLEditorKit();
		StyleSheet styles = new StyleSheet();
		styles.addStyleSheet(kit.getStyleSheet());
		int px = Math.round(fontSize);
		styles.addRule("body {color:" + hex(fg) + "; background-color:" + hex(bg) + "; font-family:'" + family + "'; font-size:" + px + "px; margin:14px 18px;}");
		styles.addRule("p, div, td, li, span, b, u, i, font {color:" + hex(fg) + ";}");
		styles.addRule("h1, h2, h3, h4 {color:" + hex(fg) + "; font-family:'" + family + "';}");
		styles.addRule("a {color:" + hex(link) + ";}");
		styles.addRule("pre, code {font-family:monospace; font-size:" + Math.max(9, px - 2) + "px; color:" + hex(fg) + ";}");
		styles.addRule(".app_name {color:" + hex(link) + "; font-weight:bold;}");
		kit.setStyleSheet(styles);
		jEditorPane.setEditorKit(kit);
		jEditorPane.setBackground(bg);
		jEditorPane.setForeground(fg);
		jEditorPane.setCaretColor(fg);
		jEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		jEditorPane.setFont(deriveFont(base, 1f, Font.PLAIN).deriveFont(fontSize));
	}

	private void loadFile(File licenseToLoad) {
		if (licenseToLoad == null) {
			return;
		}
		String fileContent = "License not found at: " + licenseToLoad;
		try {
			fileContent = FileWorker.readFileAsString(licenseToLoad, "UTF-8");
		} catch (Exception e) {
			dialogHelper.warn("License file not found!");
			licenseLoadedCorrectly = false;
		}

		fileContent = StringWorker.isEmpty(fileContent) ? "Not found" : fileContent;
		jEditorPane.setText(fileContent);
		licenseLoadedCorrectly = !StringWorker.isEmpty(fileContent);

		GuiUtils.scrollJScrollPane(scrollPane, false);
		jEditorPane.setCaretPosition(0);

		lblSubtitle.setText(licenseToLoad.getName());

		loadedLicense = licenseToLoad;
		runFind();
	}

	// FIND

	private void runFind() {
		Highlighter highlighter = jEditorPane.getHighlighter();
		highlighter.removeAllHighlights();
		matches.clear();
		currentMatch = -1;
		String needle = findField == null ? "" : findField.getText().toLowerCase();
		if (needle.isEmpty()) {
			findCount.setText(" ");
			return;
		}
		try {
			String text = jEditorPane.getDocument().getText(0, jEditorPane.getDocument().getLength()).toLowerCase();
			int from = 0;
			DefaultHighlighter.DefaultHighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
					new Color(214, 140, 38, 110));
			while ((from = text.indexOf(needle, from)) >= 0) {
				matches.add(new int[] {from, from + needle.length()});
				highlighter.addHighlight(from, from + needle.length(), painter);
				from += needle.length();
				if (matches.size() >= 5000) {
					break;
				}
			}
		} catch (BadLocationException e) {
			// text changed while searching: nothing to highlight
		}
		findCount.setText(matches.isEmpty() ? "0 found" : matches.size() + " found");
		if (!matches.isEmpty()) {
			stepMatch(1);
		}
	}

	private void stepMatch(int direction) {
		if (matches.isEmpty()) {
			return;
		}
		currentMatch = currentMatch < 0 ? (direction > 0 ? 0 : matches.size() - 1) : (currentMatch + direction + matches.size()) % matches.size();
		int[] match = matches.get(currentMatch);
		jEditorPane.select(match[0], match[1]);
		try {
			Rectangle rect = jEditorPane.modelToView(match[0]);
			if (rect != null) {
				rect.height = Math.max(rect.height, 20);
				jEditorPane.scrollRectToVisible(new Rectangle(rect.x, Math.max(0, rect.y - 60), 10, rect.height + 120));
			}
		} catch (BadLocationException e) {
			// ignore: the match is highlighted anyway
		}
		findCount.setText((currentMatch + 1) + " / " + matches.size());
	}

	// CHOICE

	private boolean askExitRejecting() {
		if (dialogHelper.yesOrNo("Are you sure to exit license acceptance? It's required to proceed.", "Exit license acceptance?")) {
			userChoice = UserLicenseChoice.REJECTED;
			instance = null;
			dispose();
			return true;
		} else {
			return false;
		}
	}

	private void exitAccepting() {
		userChoice = UserLicenseChoice.ACCEPTED;
		dispose();
	}

	public JButton getBtnDecline() {
		return btnDecline;
	}

	public JButton getBtnAccept() {
		return btnAccept;
	}

	/** Kept for compatibility: the visibility of the document groups is now decided in the constructor */
	public void setSubMenusVisible(boolean... indexes) {
		if (indexes == null) {
			return;
		}
		for (int i = 0; i < visible.length && i < indexes.length; i++) {
			visible[i] = indexes[i];
		}
		if (btnBrowser != null) {
			btnBrowser.setVisible(visible[3]);
		}
	}

	public static LightLicenseViewer getInstance(File sourceLicenseFile, String header, String title, boolean... visibleFlags) {
		return instance == null ? new LightLicenseViewer(sourceLicenseFile, header, title, visibleFlags) : instance;
	}

	public static File getMAIN_LIBRARIES_FOLDER() {
		return MAIN_LIBRARIES_FOLDER;
	}

	public static void setMAIN_LIBRARIES_FOLDER(File mAIN_LIBRARIES_FOLDER) {
		MAIN_LIBRARIES_FOLDER = mAIN_LIBRARIES_FOLDER;
	}

	public static File getMAIN_LIBRARIES_FOLDER_DETAIL() {
		return MAIN_LIBRARIES_FOLDER_DETAIL;
	}

	public static void setMAIN_LIBRARIES_FOLDER_DETAIL(File mAIN_LIBRARIES_FOLDER_DETAIL) {
		MAIN_LIBRARIES_FOLDER_DETAIL = mAIN_LIBRARIES_FOLDER_DETAIL;
	}

}
