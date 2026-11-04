package markdown.viewer;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.options.MutableDataSet;

import gui.commons.dialogutils.ExtensionFileFilter;
import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.texteditor.commons.om.SupportedFileTextEditor;
import gui.texteditor.commons.utils.RsyntaxUtils;
import io.github.furstenheim.CopyDown;
import net.miginfocom.swing.MigLayout;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.GuiUtils;
import various.common.light.utility.string.StringWorker;

/**
 * Markdown viewer / editor: Markdown editor with live preview (GitHub style tables, task lists, strikethrough,
 * autolinks, table of contents), HTML source, headings outline, formatting toolbar, text zoom, HTML export.
 *
 * Esc closes the window (asking what to do with unsaved changes), every control has a tooltip with its shortcut.
 */
public class MarkdownViewerFrame extends ParentFrame {

	public static String defaultTheme = "/org/fife/ui/rsyntaxtextarea/themes/dark.xml";
	private static final String LIGHT_THEME = "/org/fife/ui/rsyntaxtextarea/themes/default.xml";

	private static final long serialVersionUID = -3827828968889648153L;

	private static final boolean MAC = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");
	private static final String KEY = MAC ? "Cmd" : "Ctrl";
	private static final int MENU = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private static final String PREF_NODE = "am-design-development/screept/markdownviewer";
	private static final int ZOOM_MIN = 60, ZOOM_MAX = 300, ZOOM_STEP = 10;
	private static final int EDITOR_BASE_SIZE = 14, PREVIEW_BASE_SIZE = 15;

	private enum Source { MARKDOWN, HTML }
	private enum ViewMode { EDITOR, SPLIT, PREVIEW }

	/** One heading of the outline. */
	private static final class OutlineItem {
		final int level;
		final String text;
		final int offset;

		OutlineItem(int level, String text, int offset) {
			this.level = level;
			this.text = text;
			this.offset = offset;
		}

		@Override
		public String toString() {
			StringBuilder indent = new StringBuilder();
			for (int i = 1; i < level; i++) {
				indent.append("    ");
			}
			return indent + (text.isEmpty() ? "(empty)" : text);
		}
	}

	// markdown engine (thread safe once built)
	private static Parser parser;
	private static HtmlRenderer renderer;

	// ui
	private RSyntaxTextArea textAreaMarkDown;
	private RSyntaxTextArea textAreaHtml;
	private RTextScrollPane scrollPaneMarkDown;
	private JEditorPane textAreaPreview;
	private JScrollPane scrollPanePreview;
	private JTabbedPane rightTabs;
	private JSplitPane splitPaneMain;
	private JSplitPane splitPaneOutline;
	private JPanel outlinePanel;
	private JList<OutlineItem> outlineList;
	private DefaultListModel<OutlineItem> outlineModel = new DefaultListModel<OutlineItem>();
	private JLabel lblStatus;
	private JLabel lblStats;
	private JLabel lblZoom;
	private JToggleButton btnModeEditor, btnModeSplit, btnModePreview;
	private JToggleButton btnOutline, btnWrap, btnSync, btnPreviewTheme;

	// state
	private File htmlFile, markdownFile;
	private String savedMarkdown = "";
	private String savedHtml = "";
	private Source source = Source.MARKDOWN;
	private ViewMode viewMode = ViewMode.SPLIT;
	private boolean dirty = false;
	private boolean updatingEditor = false, updatingHtml = false, updatingOutline = false, updatingScroll = false;
	private int zoomPercent = 100;
	private boolean previewDark;
	private boolean syncScroll = true;
	private Timer renderTimer;
	private AWTEventListener wheelListener;
	private KeyEventDispatcher escapeDispatcher;
	private Set<String> fontFamilies;

	public MarkdownViewerFrame(File html, File markdown) {
		super();

		htmlFile = html;
		markdownFile = markdown;

		String htmlStr, markdownStr;
		if(html == null)
			htmlStr = "";
		else
			try {
				htmlStr = FileWorker.readFileAsString(html);
			} catch (IOException e) {
				htmlStr = "";
			}

		if(markdown == null)
			markdownStr = "";
		else
			try {
				markdownStr = FileWorker.readFileAsString(markdown);
			} catch (IOException e) {
				markdownStr = "";
			}

		init(htmlStr, markdownStr);
	}

	/**
	 * @wbp.parser.constructor
	 */
	public MarkdownViewerFrame(String html, String markdown) {
		super();
		init(html, markdown);
	}

	// ------------------------------------------------------------------ public API (kept for the plugin)

	public boolean[] save() {
		boolean mk = saveMarkdown(false);
		boolean ht = htmlFile == null || saveHtml(true);

		return new boolean[] {ht, mk};
	}

	/** Saves the markdown text: to its file, or asks where when it has none. */
	public boolean saveMarkdown(boolean updateTitle) {
		try {
			if(markdownFile == null || !markdownFile.isFile()) {
				File chosen = chooseWriteFile("Notes.md", null, "md", "markdown");
				if(chosen == null) {
					return false;
				}
				markdownFile = chosen;
			}
			String text = textAreaMarkDown.getText();
			FileWorker.writeStringToFile(text, markdownFile, true);
			savedMarkdown = text;
			dirty = false;
			setStatus("Saved " + markdownFile.getName());
			return true;
		} catch (IOException e) {
			logger.error("", e);
			dialogHelper.error("Cannot save the markdown file :(");
			return false;
		} finally {
			updateTitle();
		}
	}

	/** Exports the rendered document as a standalone html page. */
	public boolean saveHtml(boolean updateTitle) {
		try {
			if(htmlFile == null) {
				String base = markdownFile != null ? baseName(markdownFile) : "Notes";
				File chosen = chooseWriteFile(base + ".html", markdownFile != null ? markdownFile.getParentFile() : null, "html", "htm");
				if(chosen == null) {
					return false;
				}
				htmlFile = chosen;
			}
			FileWorker.writeStringToFile(buildStandaloneHtml(), htmlFile, true);
			setStatus("Exported " + htmlFile.getName());
			return true;
		} catch (IOException e) {
			logger.error("", e);
			dialogHelper.error("Cannot export the html file :(");
			return false;
		} finally {
			if(updateTitle)
				updateTitle();
		}
	}

	public void updateTitle() {
		String name = markdownFile != null ? markdownFile.getName() : "untitled";
		setTitle("Markdown Viewer - " + name + (dirty ? " *" : ""));
		if(textAreaMarkDown != null) {
			textAreaMarkDown.setToolTipText(markdownFile != null ? FileVarious.getCanonicalPathSafe(markdownFile) : "Markdown source (not saved yet)");
		}
	}

	public void loadHtml(String html, boolean loadPreview) {
		if(html == null)
			return;

		setHtmlSourceText(html);
		source = Source.HTML;
		savedHtml = html;
		if(loadPreview)
			renderNow();
	}

	public void loadMarkdown(String markdown, boolean loadPreview) {
		if(markdown == null)
			return;

		setEditorText(markdown);
		savedMarkdown = markdown;
		dirty = false;
		source = Source.MARKDOWN;
		if(loadPreview)
			renderNow();
	}

	public String htmlToMarkdown(String html) {
		CopyDown converter = new CopyDown();
		return converter.convert(html);
	}

	public void htmlToMarkdown() {
		String html = textAreaHtml.getText();
		if(StringWorker.isEmpty(html))
			return;

		source = Source.MARKDOWN;
		textAreaMarkDown.setText(htmlToMarkdown(html));
		textAreaMarkDown.setCaretPosition(0);
		setStatus("HTML converted to markdown");
	}

	public String markdownToHtml(String markdown) {
		try {
			return getRenderer().render(getParser().parse(markdown == null ? "" : markdown));
		} catch (Exception e) {
			logger.error("Can't convert from markdown to html", e);
			dialogHelper.error("Invalid markdown data, cannot convert to html!");
			return "";
		}
	}

	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(1280, 780);
	}

	// ------------------------------------------------------------------ engine

	private static synchronized Parser getParser() {
		if(parser == null)
			buildEngine();
		return parser;
	}

	private static synchronized HtmlRenderer getRenderer() {
		if(renderer == null)
			buildEngine();
		return renderer;
	}

	private static void buildEngine() {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(
				TablesExtension.create(),
				StrikethroughExtension.create(),
				TaskListExtension.create(),
				AutolinkExtension.create(),
				AnchorLinkExtension.create(),
				TocExtension.create()));
		// headings get a named anchor (used by "#section" links and [TOC]) without turning the title into a link
		options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false);
		options.set(AnchorLinkExtension.ANCHORLINKS_SET_NAME, true);
		options.set(AnchorLinkExtension.ANCHORLINKS_SET_ID, false);
		options.set(AnchorLinkExtension.ANCHORLINKS_TEXT_PREFIX, "");
		options.set(AnchorLinkExtension.ANCHORLINKS_TEXT_SUFFIX, "");
		parser = Parser.builder(options).build();
		renderer = HtmlRenderer.builder(options).build();
	}

	// ------------------------------------------------------------------ init

	public void init(String html, String markdown) {
		loadPreferences();
		previewDark = isDarkLookAndFeel() != previewThemeOverride;

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(buildToolbars(), BorderLayout.NORTH);
		getContentPane().add(buildCenter(), BorderLayout.CENTER);
		getContentPane().add(buildStatusBar(), BorderLayout.SOUTH);

		setIconImages(AppIcon.images());
		resizeToDefault(false, true, false);
		GuiUtils.centerComponent(this, getDefaultDimension());

		installBehaviors();
		btnWrap.setSelected(true);
		btnSync.setSelected(syncScroll);
		btnPreviewTheme.setSelected(previewThemeOverride);
		btnOutline.setSelected(true);
		applyPreviewStyle();
		applyZoom(false);

		boolean markdownGiven = !StringWorker.isEmpty(markdown);
		updatingEditor = true;
		textAreaMarkDown.setText(markdownGiven ? markdown : "");
		textAreaMarkDown.setCaretPosition(0);
		updatingEditor = false;
		savedMarkdown = markdownGiven ? markdown : "";

		if(!markdownGiven && !StringWorker.isEmpty(html)) {
			// only html given: shown as it is, and converted into markdown to edit it
			setHtmlSourceText(html);
			savedHtml = html;
			source = Source.HTML;
			updatingEditor = true;
			textAreaMarkDown.setText(htmlToMarkdown(html));
			textAreaMarkDown.setCaretPosition(0);
			updatingEditor = false;
			savedMarkdown = textAreaMarkDown.getText();
		} else {
			source = Source.MARKDOWN;
		}

		renderNow();
		applyViewMode(viewMode);
		updateTitle();

		SwingUtilities.invokeLater(() -> {
			splitPaneOutline.setDividerLocation(210);
			applyViewMode(viewMode);
			textAreaMarkDown.requestFocusInWindow();
		});
	}

	// ------------------------------------------------------------------ ui construction

	private boolean previewThemeOverride = false;

	private JComponent buildToolbars() {
		JPanel bars = new JPanel(new GridLayout(0, 1));

		JToolBar main = newToolBar();
		main.add(iconButton(ToolIcons.Kind.OPEN, "Open a markdown (.md) or html file (" + KEY + "+O)", () -> openFile()));
		main.add(iconButton(ToolIcons.Kind.SAVE, "Save the markdown file (" + KEY + "+S)", () -> saveMarkdown(true)));
		main.add(iconButton(ToolIcons.Kind.SAVE_AS, "Save the markdown to another file (" + KEY + "+Shift+S)", () -> saveMarkdownAs()));
		main.add(iconButton(ToolIcons.Kind.EXPORT, "Export the rendered document as a standalone html page (" + KEY + "+E)", () -> exportHtml()));
		addGap(main);

		ButtonGroup modes = new ButtonGroup();
		btnModeEditor = iconToggle(ToolIcons.Kind.EDITOR, "Editor only: show just the markdown source (" + KEY + "+1)", modes);
		btnModeSplit = iconToggle(ToolIcons.Kind.SPLIT, "Split view: editor and live preview side by side (" + KEY + "+2)", modes);
		btnModePreview = iconToggle(ToolIcons.Kind.PREVIEW, "Preview only: show just the rendered document (" + KEY + "+3)", modes);
		btnModeEditor.addActionListener(e -> applyViewMode(ViewMode.EDITOR));
		btnModeSplit.addActionListener(e -> applyViewMode(ViewMode.SPLIT));
		btnModePreview.addActionListener(e -> applyViewMode(ViewMode.PREVIEW));
		main.add(btnModeEditor);
		main.add(btnModeSplit);
		main.add(btnModePreview);
		addGap(main);

		main.add(iconButton(ToolIcons.Kind.REFRESH, "Render the document again now (" + KEY + "+Enter)", () -> renderNow()));
		main.add(button("HTML → MD", "Convert the HTML source tab into markdown (replaces the editor text, undo is available)", () -> htmlToMarkdown()));
		addGap(main);
		main.add(iconButton(ToolIcons.Kind.COPY, "Copy the generated html to the clipboard", () -> copyHtml()));
		main.add(iconButton(ToolIcons.Kind.BROWSER, "Open the rendered page in the default web browser", () -> openInBrowser()));

		JToolBar tools = newToolBar();
		JButton btnBold = button("B", "Bold (" + KEY + "+B)", () -> wrapSelection("**", "**", "bold"));
		btnBold.setFont(btnBold.getFont().deriveFont(Font.BOLD));
		JButton btnItalic = button("I", "Italic (" + KEY + "+I)", () -> wrapSelection("*", "*", "italic"));
		btnItalic.setFont(btnItalic.getFont().deriveFont(Font.ITALIC));
		JButton btnStrike = button("S", "Strikethrough", () -> wrapSelection("~~", "~~", "text"));
		java.util.Map<java.awt.font.TextAttribute, Object> strikeAttributes = new java.util.HashMap<java.awt.font.TextAttribute, Object>(btnStrike.getFont().getAttributes());
		strikeAttributes.put(java.awt.font.TextAttribute.STRIKETHROUGH, java.awt.font.TextAttribute.STRIKETHROUGH_ON);
		btnStrike.setFont(btnStrike.getFont().deriveFont(strikeAttributes));
		JButton btnHeading = button("H", "Heading: cycles # / ## / ### / none on the current line (" + KEY + "+H)", () -> cycleHeading());
		btnHeading.setFont(btnHeading.getFont().deriveFont(Font.BOLD));
		tools.add(btnHeading);
		tools.add(btnBold);
		tools.add(btnItalic);
		tools.add(btnStrike);
		tools.add(button("</>", "Inline code (" + KEY + "+`)", () -> wrapSelection("`", "`", "code")));
		addGap(tools);
		tools.add(iconButton(ToolIcons.Kind.CODE_BLOCK, "Fenced code block", () -> codeBlock()));
		tools.add(iconButton(ToolIcons.Kind.LINK, "Insert a link (" + KEY + "+K)", () -> insertLink()));
		tools.add(iconButton(ToolIcons.Kind.IMAGE, "Insert an image", () -> insertImage()));
		tools.add(iconButton(ToolIcons.Kind.TABLE, "Insert a table", () -> insertTable()));
		tools.add(iconButton(ToolIcons.Kind.RULE, "Insert a horizontal rule", () -> insertText("\n---\n")));
		addGap(tools);
		tools.add(iconButton(ToolIcons.Kind.LIST, "Bulleted list on the selected lines", () -> toggleLinePrefix("- ")));
		tools.add(iconButton(ToolIcons.Kind.NUMBERED, "Numbered list on the selected lines", () -> numberedList()));
		tools.add(iconButton(ToolIcons.Kind.TASK, "Task list (checkboxes) on the selected lines", () -> toggleLinePrefix("- [ ] ")));
		tools.add(iconButton(ToolIcons.Kind.QUOTE, "Quote on the selected lines", () -> toggleLinePrefix("> ")));
		addGap(tools);

		btnOutline = iconToggle(ToolIcons.Kind.OUTLINE, "Show or hide the list of headings (" + KEY + "+Shift+O)", null);
		btnOutline.addActionListener(e -> setOutlineVisible(btnOutline.isSelected()));
		btnWrap = iconToggle(ToolIcons.Kind.WRAP, "Wrap long lines in the editor", null);
		btnWrap.addActionListener(e -> {
			textAreaMarkDown.setLineWrap(btnWrap.isSelected());
			textAreaHtml.setLineWrap(btnWrap.isSelected());
			savePreferences();
		});
		btnSync = iconToggle(ToolIcons.Kind.SYNC, "Scroll the preview together with the editor", null);
		btnSync.addActionListener(e -> {
			syncScroll = btnSync.isSelected();
			savePreferences();
		});
		btnPreviewTheme = iconToggle(ToolIcons.Kind.INVERT, "Invert the preview colors: dark preview on a light theme and the other way round", null);
		btnPreviewTheme.addActionListener(e -> {
			previewThemeOverride = btnPreviewTheme.isSelected();
			previewDark = isDarkLookAndFeel() != previewThemeOverride;
			applyPreviewStyle();
			renderNow();
			savePreferences();
		});
		tools.add(btnOutline);
		tools.add(btnWrap);
		tools.add(btnSync);
		tools.add(btnPreviewTheme);
		addGap(tools);

		tools.add(iconButton(ToolIcons.Kind.ZOOM_OUT, "Smaller text (" + KEY + " + minus, or " + KEY + " + mouse wheel)", () -> setZoom(zoomPercent - ZOOM_STEP)));
		lblZoom = new JLabel(zoomPercent + "%", SwingConstants.CENTER);
		lblZoom.setToolTipText("Text size: click to reset to 100% (" + KEY + "+0)");
		lblZoom.setPreferredSize(new Dimension(46, 22));
		lblZoom.setCursor(GuiUtils.CURSOR_HAND);
		lblZoom.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				setZoom(100);
			}
		});
		tools.add(lblZoom);
		tools.add(iconButton(ToolIcons.Kind.ZOOM_IN, "Larger text (" + KEY + " + plus, or " + KEY + " + mouse wheel)", () -> setZoom(zoomPercent + ZOOM_STEP)));

		bars.add(main);
		bars.add(tools);
		return bars;
	}

	/** Visible space between groups of buttons (a thin separator in the middle of it). */
	private void addGap(JToolBar bar) {
		bar.addSeparator(new Dimension(6, 0));
		JSeparator line = new JSeparator(SwingConstants.VERTICAL);
		line.setPreferredSize(new Dimension(2, 20));
		line.setMaximumSize(new Dimension(2, 20));
		bar.add(line);
		bar.addSeparator(new Dimension(6, 0));
	}

	private JButton iconButton(ToolIcons.Kind kind, String tooltip, final Runnable action) {
		JButton b = button("", tooltip, action);
		b.setIcon(ToolIcons.of(kind));
		b.setMargin(new Insets(5, 8, 5, 8));
		b.getAccessibleContext().setAccessibleName(tooltip);
		return b;
	}

	private JToggleButton iconToggle(ToolIcons.Kind kind, String tooltip, ButtonGroup group) {
		JToggleButton b = toggle("", tooltip, group);
		b.setIcon(ToolIcons.of(kind));
		b.setMargin(new Insets(5, 8, 5, 8));
		b.getAccessibleContext().setAccessibleName(tooltip);
		return b;
	}

	private JToolBar newToolBar() {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		bar.setRollover(true);
		bar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		return bar;
	}

	private JButton button(String text, String tooltip, final Runnable action) {
		JButton b = new JButton(text);
		b.setToolTipText(tooltip);
		b.setFocusable(false);
		b.setCursor(GuiUtils.CURSOR_HAND);
		b.putClientProperty("JButton.buttonType", "toolBarButton");
		b.addActionListener(e -> action.run());
		return b;
	}

	private JToggleButton toggle(String text, String tooltip, ButtonGroup group) {
		JToggleButton b = new JToggleButton(text);
		b.setToolTipText(tooltip);
		b.setFocusable(false);
		b.setCursor(GuiUtils.CURSOR_HAND);
		b.putClientProperty("JButton.buttonType", "toolBarButton");
		if(group != null)
			group.add(b);
		return b;
	}

	private JComponent buildCenter() {
		textAreaMarkDown = new RSyntaxTextArea();
		textAreaMarkDown.setSyntaxEditingStyle(SupportedFileTextEditor.MARKDOWN.syntax);
		textAreaMarkDown.setLineWrap(true);
		textAreaMarkDown.setWrapStyleWord(true);
		textAreaMarkDown.setTabSize(4);
		textAreaMarkDown.setCodeFoldingEnabled(false);
		textAreaMarkDown.setToolTipText("Markdown source");
		scrollPaneMarkDown = new RTextScrollPane(textAreaMarkDown);
		scrollPaneMarkDown.setLineNumbersEnabled(true);
		scrollPaneMarkDown.setFoldIndicatorEnabled(false);
		scrollPaneMarkDown.setBorder(BorderFactory.createEmptyBorder());
		scrollPaneMarkDown.setMinimumSize(new Dimension(0, 0));

		textAreaHtml = new RSyntaxTextArea();
		textAreaHtml.setSyntaxEditingStyle(SupportedFileTextEditor.HTML.syntax);
		textAreaHtml.setLineWrap(true);
		textAreaHtml.setCodeFoldingEnabled(false);
		textAreaHtml.setToolTipText("Generated html (editable: the preview follows what you type here)");
		RTextScrollPane htmlScroll = new RTextScrollPane(textAreaHtml);
		htmlScroll.setLineNumbersEnabled(true);
		htmlScroll.setBorder(BorderFactory.createEmptyBorder());
		applyEditorTheme();

		textAreaPreview = new JEditorPane();
		textAreaPreview.setEditable(false);
		scrollPanePreview = new JScrollPane(textAreaPreview);
		scrollPanePreview.setBorder(BorderFactory.createEmptyBorder());
		scrollPanePreview.getVerticalScrollBar().setUnitIncrement(24);

		rightTabs = new JTabbedPane(SwingConstants.TOP);
		rightTabs.addTab("Preview", null, scrollPanePreview, "Live rendering of the markdown");
		rightTabs.addTab("HTML source", null, htmlScroll, "The html that is generated from the markdown");
		rightTabs.setMinimumSize(new Dimension(0, 0));

		splitPaneMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPaneMarkDown, rightTabs);
		splitPaneMain.setResizeWeight(0.5);
		splitPaneMain.setContinuousLayout(true);
		splitPaneMain.setDividerSize(6);
		splitPaneMain.setBorder(BorderFactory.createEmptyBorder());

		outlineList = new JList<OutlineItem>(outlineModel);
		outlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		outlineList.setToolTipText("Headings of the document: click to jump");
		outlineList.setCursor(GuiUtils.CURSOR_HAND);
		outlineList.addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting() && !updatingOutline) {
				OutlineItem item = outlineList.getSelectedValue();
				if(item != null)
					jumpTo(item.offset);
			}
		});
		JLabel outlineTitle = new JLabel(" Outline");
		outlineTitle.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
		outlineTitle.setFont(outlineTitle.getFont().deriveFont(Font.BOLD));
		outlinePanel = new JPanel(new BorderLayout());
		outlinePanel.add(outlineTitle, BorderLayout.NORTH);
		JScrollPane outlineScroll = new JScrollPane(outlineList);
		outlineScroll.setBorder(BorderFactory.createEmptyBorder());
		outlinePanel.add(outlineScroll, BorderLayout.CENTER);
		outlinePanel.setMinimumSize(new Dimension(0, 0));

		splitPaneOutline = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, outlinePanel, splitPaneMain);
		splitPaneOutline.setContinuousLayout(true);
		splitPaneOutline.setDividerSize(6);
		splitPaneOutline.setBorder(BorderFactory.createEmptyBorder());
		return splitPaneOutline;
	}

	private JComponent buildStatusBar() {
		JPanel bar = new JPanel(new MigLayout("insets 3 10 3 10", "[grow][]", "[]"));
		lblStatus = new JLabel("Ready");
		lblStatus.setForeground(UIManager.getColor("Label.disabledForeground"));
		lblStats = new JLabel(" ");
		lblStats.setForeground(UIManager.getColor("Label.disabledForeground"));
		lblStats.setToolTipText("Words, characters, lines and the estimated reading time");
		bar.add(lblStatus, "growx");
		bar.add(lblStats);
		return bar;
	}

	private void applyEditorTheme() {
		String theme = isDarkLookAndFeel() ? defaultTheme : LIGHT_THEME;
		RsyntaxUtils.changeStyleViaThemeXml(textAreaMarkDown, theme);
		RsyntaxUtils.changeStyleViaThemeXml(textAreaHtml, theme);
	}

	// ------------------------------------------------------------------ behaviors

	private void installBehaviors() {
		renderTimer = new Timer(260, e -> renderNow());
		renderTimer.setRepeats(false);

		textAreaMarkDown.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) { editorChanged(); }
			@Override public void removeUpdate(DocumentEvent e) { editorChanged(); }
			@Override public void changedUpdate(DocumentEvent e) { }
		});
		textAreaHtml.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) { htmlChanged(); }
			@Override public void removeUpdate(DocumentEvent e) { htmlChanged(); }
			@Override public void changedUpdate(DocumentEvent e) { }
		});
		textAreaMarkDown.addCaretListener(e -> updateCaretStatus());

		scrollPaneMarkDown.getVerticalScrollBar().addAdjustmentListener(e -> {
			if(syncScroll && !updatingScroll && viewMode == ViewMode.SPLIT && rightTabs.getSelectedComponent() == scrollPanePreview) {
				syncPreviewToEditor();
			}
		});

		textAreaPreview.addHyperlinkListener(e -> {
			if(e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				setStatus(e.getDescription());
			} else if(e.getEventType() == HyperlinkEvent.EventType.EXITED) {
				setStatus("");
			} else if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				followLink(e);
			}
		});

		// close action: unsaved changes -> Save / Don't save / Cancel
		setCloseAction(() -> confirmClose());

		wheelListener = new AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				if(!(event instanceof MouseWheelEvent))
					return;
				MouseWheelEvent wheel = (MouseWheelEvent) event;
				if(wheel.isConsumed() || !(wheel.isControlDown() || (MAC && wheel.isMetaDown())))
					return;
				Component src = wheel.getComponent();
				Window window = src instanceof Window ? (Window) src : SwingUtilities.getWindowAncestor(src);
				if(window != MarkdownViewerFrame.this)
					return;

				setZoom(zoomPercent - (int) Math.signum(wheel.getPreciseWheelRotation()) * ZOOM_STEP);

				final JScrollPane sp = src instanceof JScrollPane ? (JScrollPane) src : (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, src);
				if(sp != null && sp.isWheelScrollingEnabled()) {
					sp.setWheelScrollingEnabled(false);
					SwingUtilities.invokeLater(() -> sp.setWheelScrollingEnabled(true));
				}
				wheel.consume();
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(wheelListener, AWTEvent.MOUSE_WHEEL_EVENT_MASK);

		// Esc closes the window (but not when typed in a text field / combo box)
		escapeDispatcher = new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if(e.getID() != KeyEvent.KEY_PRESSED || e.getKeyCode() != KeyEvent.VK_ESCAPE || e.getModifiersEx() != 0 || e.isConsumed())
					return false;
				Component src = e.getComponent();
				if(src == null)
					return false;
				Window window = src instanceof Window ? (Window) src : SwingUtilities.getWindowAncestor(src);
				if(window != MarkdownViewerFrame.this)
					return false;
				if((src instanceof JTextComponent && !(src instanceof JTextArea) && !(src instanceof JEditorPane)) || src instanceof JComboBox)
					return false;
				e.consume();
				SwingUtilities.invokeLater(() -> dispatchEvent(new WindowEvent(MarkdownViewerFrame.this, WindowEvent.WINDOW_CLOSING)));
				return true;
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(escapeDispatcher);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				Toolkit.getDefaultToolkit().removeAWTEventListener(wheelListener);
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(escapeDispatcher);
				if(renderTimer != null)
					renderTimer.stop();
			}
		});

		// shortcuts: on the editors too, because their own key bindings win over the window ones
		bind(KeyEvent.VK_S, 0, "mdv-save", () -> saveMarkdown(true));
		bind(KeyEvent.VK_S, InputEvent.SHIFT_MASK, "mdv-saveas", () -> saveMarkdownAs());
		bind(KeyEvent.VK_O, 0, "mdv-open", () -> openFile());
		bind(KeyEvent.VK_E, 0, "mdv-export", () -> exportHtml());
		bind(KeyEvent.VK_ENTER, 0, "mdv-refresh", () -> renderNow());
		bind(KeyEvent.VK_B, 0, "mdv-bold", () -> wrapSelection("**", "**", "bold"));
		bind(KeyEvent.VK_I, 0, "mdv-italic", () -> wrapSelection("*", "*", "italic"));
		bind(KeyEvent.VK_K, 0, "mdv-link", () -> insertLink());
		bind(KeyEvent.VK_H, 0, "mdv-heading", () -> cycleHeading());
		bind(KeyEvent.VK_BACK_QUOTE, 0, "mdv-code", () -> wrapSelection("`", "`", "code"));
		bind(KeyEvent.VK_1, 0, "mdv-view1", () -> setViewMode(ViewMode.EDITOR));
		bind(KeyEvent.VK_2, 0, "mdv-view2", () -> setViewMode(ViewMode.SPLIT));
		bind(KeyEvent.VK_3, 0, "mdv-view3", () -> setViewMode(ViewMode.PREVIEW));
		bind(KeyEvent.VK_O, InputEvent.SHIFT_MASK, "mdv-outline", () -> setOutlineVisible(!btnOutline.isSelected()));
		bind(KeyEvent.VK_PLUS, 0, "mdv-zoomin", () -> setZoom(zoomPercent + ZOOM_STEP));
		bind(KeyEvent.VK_EQUALS, 0, "mdv-zoomin2", () -> setZoom(zoomPercent + ZOOM_STEP));
		bind(KeyEvent.VK_ADD, 0, "mdv-zoomin3", () -> setZoom(zoomPercent + ZOOM_STEP));
		bind(KeyEvent.VK_MINUS, 0, "mdv-zoomout", () -> setZoom(zoomPercent - ZOOM_STEP));
		bind(KeyEvent.VK_SUBTRACT, 0, "mdv-zoomout2", () -> setZoom(zoomPercent - ZOOM_STEP));
		bind(KeyEvent.VK_0, 0, "mdv-zoomreset", () -> setZoom(100));
		bind(KeyEvent.VK_NUMPAD0, 0, "mdv-zoomreset2", () -> setZoom(100));
	}

	private void bind(int keyCode, int extraMask, String name, final Runnable action) {
		KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, MENU | extraMask);
		AbstractAction a = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		};
		for(JComponent c : new JComponent[] {getRootPane(), textAreaMarkDown, textAreaHtml}) {
			int condition = c == getRootPane() ? JComponent.WHEN_IN_FOCUSED_WINDOW : JComponent.WHEN_FOCUSED;
			c.getInputMap(condition).put(stroke, name);
			c.getActionMap().put(name, a);
		}
	}

	private void editorChanged() {
		if(updatingEditor)
			return;
		source = Source.MARKDOWN;
		dirty = true;
		updateTitle();
		renderTimer.restart();
	}

	private void htmlChanged() {
		if(updatingHtml)
			return;
		source = Source.HTML;
		renderTimer.restart();
	}

	// ------------------------------------------------------------------ rendering

	private void renderNow() {
		if(renderTimer != null)
			renderTimer.stop();
		long start = System.nanoTime();
		String markdown = textAreaMarkDown.getText();

		if(source == Source.MARKDOWN) {
			Node document = getParser().parse(markdown);
			String body = getRenderer().render(document);
			setHtmlSourceText(body);
			showPreview(body);
			updateOutline(document);
		} else {
			showPreview(textAreaHtml.getText());
			updateOutline(getParser().parse(markdown));
		}

		dirty = !markdown.equals(savedMarkdown);
		updateTitle();
		updateStats(markdown, (System.nanoTime() - start) / 1000000L);
		updateCaretStatus();
	}

	private void showPreview(String bodyHtml) {
		JScrollBar bar = scrollPanePreview.getVerticalScrollBar();
		int range = bar.getMaximum() - bar.getVisibleAmount();
		final double fraction = range > 0 ? bar.getValue() / (double) range : 0;

		String page = "<html><head></head><body>" + decorate(bodyHtml) + "</body></html>";
		textAreaPreview.setText(page);
		textAreaPreview.setCaretPosition(0);

		SwingUtilities.invokeLater(() -> {
			if(syncScroll && source == Source.MARKDOWN && viewMode == ViewMode.SPLIT) {
				syncPreviewToEditor();
			} else {
				setScrollFraction(scrollPanePreview, fraction);
			}
		});
	}

	/** Swing renders html 3.2: tables need their border and spacing as attributes. */
	private String decorate(String html) {
		return html == null ? "" : html.replace("<table>", "<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
	}

	private void setHtmlSourceText(String html) {
		updatingHtml = true;
		try {
			int caret = Math.min(textAreaHtml.getCaretPosition(), html.length());
			textAreaHtml.setText(html);
			textAreaHtml.setCaretPosition(caret);
		} finally {
			updatingHtml = false;
		}
	}

	private void setEditorText(String text) {
		updatingEditor = true;
		try {
			textAreaMarkDown.setText(text);
			textAreaMarkDown.setCaretPosition(0);
		} finally {
			updatingEditor = false;
		}
	}

	private void applyPreviewStyle() {
		HTMLEditorKit kit = new HTMLEditorKit();
		// own style sheet: the default one is shared with every html panel of the application
		StyleSheet sheet = new StyleSheet();
		sheet.addStyleSheet(kit.getStyleSheet());
		kit.setStyleSheet(sheet);

		boolean dark = previewDark;
		String bg = dark ? "#1e1f22" : "#ffffff";
		String fg = dark ? "#d4d7dc" : "#24292f";
		String head = dark ? "#e6edf3" : "#1f2328";
		String muted = dark ? "#8b949e" : "#57606a";
		String link = dark ? "#58a6ff" : "#0969da";
		String codeBg = dark ? "#2b2d30" : "#f3f4f6";
		String codeFg = dark ? "#e0a96d" : "#b3470b";
		int px = Math.round(PREVIEW_BASE_SIZE * zoomPercent / 100f);
		String sans = pickFont("Segoe UI", "SF Pro Text", "Helvetica Neue", "Ubuntu", "DejaVu Sans", "SansSerif");
		String mono = pickFont("Consolas", "Menlo", "DejaVu Sans Mono", "Monospaced");

		sheet.addRule("body {background-color:" + bg + "; color:" + fg + "; font-family:" + sans + "; font-size:" + px + "px; margin:14px 26px;}");
		sheet.addRule("p, li, td, th, dd, dt, div, blockquote {font-size:" + px + "px;}");
		sheet.addRule("p {margin-top:6px; margin-bottom:6px;}");
		sheet.addRule("h1 {color:" + head + "; font-size:" + Math.round(px * 1.9f) + "px; margin-top:18px; margin-bottom:8px;}");
		sheet.addRule("h2 {color:" + head + "; font-size:" + Math.round(px * 1.5f) + "px; margin-top:16px; margin-bottom:6px;}");
		sheet.addRule("h3 {color:" + head + "; font-size:" + Math.round(px * 1.25f) + "px; margin-top:14px; margin-bottom:4px;}");
		sheet.addRule("h4 {color:" + head + "; font-size:" + px + "px;}");
		sheet.addRule("h5 {color:" + muted + "; font-size:" + px + "px;}");
		sheet.addRule("h6 {color:" + muted + "; font-size:" + px + "px;}");
		sheet.addRule("a {color:" + link + ";}");
		sheet.addRule("code {font-family:" + mono + "; color:" + codeFg + "; background-color:" + codeBg + ";}");
		sheet.addRule("pre {font-family:" + mono + "; font-size:" + Math.round(px * 0.92f) + "px; color:" + fg + "; background-color:" + codeBg + "; margin-top:8px; margin-bottom:8px;}");
		sheet.addRule("blockquote {color:" + muted + "; margin-left:14px; margin-top:6px; margin-bottom:6px;}");
		sheet.addRule("li {margin-top:2px; margin-bottom:2px;}");
		sheet.addRule("th {background-color:" + codeBg + "; color:" + head + ";}");
		sheet.addRule("td {color:" + fg + ";}");
		sheet.addRule("hr {color:" + muted + ";}");

		textAreaPreview.setEditorKit(kit);
		textAreaPreview.setBackground(Color.decode(bg));
		textAreaPreview.setCaretColor(Color.decode(fg));
		textAreaPreview.setSelectionColor(dark ? new Color(38, 79, 120) : new Color(180, 213, 255));
		textAreaPreview.setSelectedTextColor(dark ? Color.WHITE : Color.BLACK);
		applyBase();
	}

	/** Relative images and links are resolved from the folder of the markdown file. */
	private void applyBase() {
		try {
			if(markdownFile != null && markdownFile.getParentFile() != null && textAreaPreview.getDocument() instanceof HTMLDocument) {
				URL base = markdownFile.getParentFile().toURI().toURL();
				((HTMLDocument) textAreaPreview.getDocument()).setBase(base);
			}
		} catch (Exception e) {
			logger.debug("Cannot set the base url of the preview");
		}
	}

	private String pickFont(String... candidates) {
		if(fontFamilies == null) {
			fontFamilies = new HashSet<String>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
		}
		for(String c : candidates) {
			if(fontFamilies.contains(c) || c.equals("SansSerif") || c.equals("Monospaced"))
				return "'" + c + "'";
		}
		return "SansSerif";
	}

	private boolean isDarkLookAndFeel() {
		Color bg = UIManager.getColor("TextArea.background");
		if(bg == null)
			bg = UIManager.getColor("Panel.background");
		if(bg == null)
			return false;
		return (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255d < 0.5;
	}

	// ------------------------------------------------------------------ outline, scroll, stats

	private void updateOutline(Node document) {
		final List<OutlineItem> items = new ArrayList<OutlineItem>();
		for(Node node : document.getDescendants()) {
			if(node instanceof Heading) {
				Heading heading = (Heading) node;
				items.add(new OutlineItem(heading.getLevel(), heading.getText().toString().trim(), heading.getStartOffset()));
			}
		}

		boolean same = items.size() == outlineModel.size();
		for(int i = 0; same && i < items.size(); i++) {
			same = items.get(i).toString().equals(outlineModel.get(i).toString());
		}
		updatingOutline = true;
		try {
			if(!same) {
				outlineModel.clear();
				for(OutlineItem item : items) {
					outlineModel.addElement(item);
				}
			} else {
				for(int i = 0; i < items.size(); i++) {
					outlineModel.set(i, items.get(i));
				}
			}
		} finally {
			updatingOutline = false;
		}
	}

	private void jumpTo(int offset) {
		try {
			int length = textAreaMarkDown.getDocument().getLength();
			int safe = Math.max(0, Math.min(offset, length));
			textAreaMarkDown.setCaretPosition(safe);
			Rectangle r = textAreaMarkDown.modelToView(safe);
			if(r != null) {
				scrollPaneMarkDown.getViewport().setViewPosition(new java.awt.Point(0, Math.max(0, r.y - 8)));
			}
			textAreaMarkDown.requestFocusInWindow();
			if(viewMode != ViewMode.EDITOR) {
				setScrollFraction(scrollPanePreview, length > 0 ? safe / (double) length : 0);
			}
		} catch (BadLocationException e) {
			logger.debug("Cannot jump to the selected heading");
		}
	}

	private void syncPreviewToEditor() {
		JScrollBar bar = scrollPaneMarkDown.getVerticalScrollBar();
		int range = bar.getMaximum() - bar.getVisibleAmount();
		setScrollFraction(scrollPanePreview, range > 0 ? bar.getValue() / (double) range : 0);
	}

	private void setScrollFraction(JScrollPane pane, double fraction) {
		JScrollBar bar = pane.getVerticalScrollBar();
		int range = bar.getMaximum() - bar.getVisibleAmount();
		if(range <= 0)
			return;
		updatingScroll = true;
		try {
			bar.setValue((int) Math.round(Math.max(0, Math.min(1, fraction)) * range));
		} finally {
			updatingScroll = false;
		}
	}

	private void updateStats(String markdown, long millis) {
		String trimmed = markdown.trim();
		int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
		int lines = textAreaMarkDown.getLineCount();
		int minutes = Math.max(1, Math.round(words / 200f));
		lblStats.setText(words + " words   " + markdown.length() + " chars   " + lines + " lines   ~" + minutes + " min read   " + millis + " ms   "
				+ (source == Source.MARKDOWN ? "source: markdown" : "source: html (edited)"));
	}

	private void updateCaretStatus() {
		if(lblStatus != null && lblStatus.getText() != null && (lblStatus.getText().startsWith("Ln ") || lblStatus.getText().trim().isEmpty() || lblStatus.getText().equals("Ready"))) {
			lblStatus.setText("Ln " + (textAreaMarkDown.getCaretLineNumber() + 1) + ", Col " + (textAreaMarkDown.getCaretOffsetFromLineStart() + 1));
		}
	}

	private void setStatus(String text) {
		lblStatus.setText(text == null || text.isEmpty() ? " " : text);
		if(text == null || text.isEmpty())
			updateCaretStatus();
	}

	// ------------------------------------------------------------------ view

	private void setViewMode(ViewMode mode) {
		applyViewMode(mode);
	}

	private void applyViewMode(ViewMode mode) {
		viewMode = mode;
		btnModeEditor.setSelected(mode == ViewMode.EDITOR);
		btnModeSplit.setSelected(mode == ViewMode.SPLIT);
		btnModePreview.setSelected(mode == ViewMode.PREVIEW);

		SwingUtilities.invokeLater(() -> {
			switch(mode) {
			case EDITOR:
				splitPaneMain.setDividerLocation(1.0);
				break;
			case PREVIEW:
				splitPaneMain.setDividerLocation(0.0);
				break;
			default:
				splitPaneMain.setDividerLocation(0.5);
			}
		});
		savePreferences();
	}

	private void setOutlineVisible(boolean visible) {
		btnOutline.setSelected(visible);
		splitPaneOutline.setDividerLocation(visible ? 210 : 0);
		savePreferences();
	}

	private void setZoom(int percent) {
		int clamped = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, percent));
		if(clamped == zoomPercent)
			return;
		zoomPercent = clamped;
		applyZoom(true);
		savePreferences();
	}

	private void applyZoom(boolean render) {
		float size = EDITOR_BASE_SIZE * zoomPercent / 100f;
		if(textAreaMarkDown != null) {
			textAreaMarkDown.setFont(textAreaMarkDown.getFont().deriveFont(size));
			textAreaHtml.setFont(textAreaHtml.getFont().deriveFont(size));
			scrollPaneMarkDown.getGutter().setLineNumberFont(scrollPaneMarkDown.getGutter().getLineNumberFont().deriveFont(Math.max(9f, size - 2f)));
		}
		if(lblZoom != null)
			lblZoom.setText(zoomPercent + "%");
		if(render) {
			applyPreviewStyle();
			renderNow();
		}
	}

	// ------------------------------------------------------------------ files

	private void openFile() {
		if(!confirmDiscard())
			return;
		File file = fileChooser.fileRead(this);
		if(file == null || !file.isFile())
			return;
		loadFile(file);
	}

	/** Loads a markdown file in the editor, or an html file into the html tab (converted to markdown). */
	public void loadFile(File file) {
		try {
			String text = FileWorker.readFileAsString(file);
			String name = file.getName().toLowerCase(Locale.ROOT);
			if(name.endsWith(".html") || name.endsWith(".htm")) {
				htmlFile = file;
				markdownFile = null;
				setHtmlSourceText(text);
				savedHtml = text;
				source = Source.HTML;
				setEditorText(htmlToMarkdown(text));
				savedMarkdown = "";
				dirty = true;
			} else {
				markdownFile = file;
				setEditorText(text);
				savedMarkdown = text;
				dirty = false;
				source = Source.MARKDOWN;
			}
			applyPreviewStyle();
			renderNow();
			setStatus("Opened " + file.getName());
		} catch (IOException e) {
			logger.error("", e);
			dialogHelper.error("Cannot read " + file.getName());
		}
	}

	private void saveMarkdownAs() {
		File chosen = chooseWriteFile(markdownFile != null ? markdownFile.getName() : "Notes.md", markdownFile != null ? markdownFile.getParentFile() : null, "md", "markdown");
		if(chosen == null)
			return;
		markdownFile = chosen;
		saveMarkdown(true);
		applyBase();
	}

	private void exportHtml() {
		File chosen = chooseWriteFile((markdownFile != null ? baseName(markdownFile) : "Notes") + ".html",
				markdownFile != null ? markdownFile.getParentFile() : null, "html", "htm");
		if(chosen == null)
			return;
		htmlFile = chosen;
		saveHtml(true);
	}

	private File chooseWriteFile(String presetName, File dir, String... extensions) {
		File chosen = fileChooser.fileWrite(this, presetName, dir, new ExtensionFileFilter[] {new ExtensionFileFilter(extensions, extensions[0])});
		if(chosen == null)
			return null;
		if(chosen.exists() && chosen.isFile() && !chosen.equals(markdownFile) && !chosen.equals(htmlFile)) {
			if(!dialogHelper.yesOrNo("File " + chosen.getName() + " exists, overwrite it?", "Overwrite?"))
				return null;
		}
		return chosen;
	}

	private boolean confirmDiscard() {
		if(!isDirty())
			return true;
		return confirmClose();
	}

	private boolean isDirty() {
		return dirty || !textAreaMarkDown.getText().equals(savedMarkdown);
	}

	/** Save / Don't save / Cancel, only when there are unsaved changes. */
	private boolean confirmClose() {
		if(!isDirty())
			return true;

		Object[] options = new Object[] {"Save", "Don't save", "Cancel"};
		int choice = JOptionPane.showOptionDialog(this, "The markdown has unsaved changes.", "Close Markdown Viewer?",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		if(choice == 1) {
			return true;
		}
		if(choice == 0) {
			boolean saved = saveMarkdown(true);
			return saved || dialogHelper.yesOrNo("An error occurred while saving, do you want to exit anyway?", "Save failed!");
		}
		return false;
	}

	// ------------------------------------------------------------------ clipboard, browser, links

	private void copyHtml() {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textAreaHtml.getText()), null);
		setStatus("HTML copied to the clipboard");
	}

	private void openInBrowser() {
		try {
			File temp = File.createTempFile("MarkdownViewer_", ".html");
			temp.deleteOnExit();
			FileWorker.writeStringToFile(buildStandaloneHtml(), temp, true);
			Desktop.getDesktop().browse(temp.toURI());
		} catch (Exception e) {
			logger.error("", e);
			dialogHelper.error("Cannot open the web browser :(");
		}
	}

	private void followLink(HyperlinkEvent e) {
		try {
			String description = e.getDescription();
			if(description != null && description.startsWith("#")) {
				textAreaPreview.scrollToReference(description.substring(1));
				return;
			}
			URL url = e.getURL();
			if(url != null && ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol()) || "mailto".equals(url.getProtocol()))) {
				Desktop.getDesktop().browse(url.toURI());
			} else if(url != null && "file".equals(url.getProtocol())) {
				File target = new File(url.toURI());
				String name = target.getName().toLowerCase(Locale.ROOT);
				if(target.isFile() && (name.endsWith(".md") || name.endsWith(".markdown")) && confirmDiscard()) {
					loadFile(target);
				}
			}
		} catch (Exception ex) {
			logger.debug("Cannot follow the link");
		}
	}

	/** Full html page with its own style (readable on its own in a browser, light or dark). */
	private String buildStandaloneHtml() {
		String body = source == Source.MARKDOWN ? markdownToHtml(textAreaMarkDown.getText()) : textAreaHtml.getText();
		String title = markdownFile != null ? baseName(markdownFile) : "Markdown";
		return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n<title>" + escape(title) + "</title>\n<style>\n"
				+ ":root{color-scheme:light dark;--bg:#fff;--fg:#24292f;--muted:#57606a;--link:#0969da;--code:#f3f4f6;--border:#d0d7de}\n"
				+ "@media (prefers-color-scheme:dark){:root{--bg:#1e1f22;--fg:#d4d7dc;--muted:#8b949e;--link:#58a6ff;--code:#2b2d30;--border:#3c3f44}}\n"
				+ "body{background:var(--bg);color:var(--fg);font-family:'Segoe UI',system-ui,-apple-system,Helvetica,Arial,sans-serif;line-height:1.6;max-width:880px;margin:2rem auto;padding:0 1.2rem}\n"
				+ "h1,h2{border-bottom:1px solid var(--border);padding-bottom:.3em}a{color:var(--link)}\n"
				+ "code{background:var(--code);padding:.15em .35em;border-radius:4px;font-family:Consolas,Menlo,monospace;font-size:.92em}\n"
				+ "pre{background:var(--code);padding:1em;border-radius:6px;overflow:auto}pre code{background:none;padding:0}\n"
				+ "blockquote{margin:1em 0;padding:0 1em;color:var(--muted);border-left:.25em solid var(--border)}\n"
				+ "table{border-collapse:collapse}th,td{border:1px solid var(--border);padding:.4em .8em}th{background:var(--code)}\n"
				+ "img{max-width:100%}hr{border:0;border-top:1px solid var(--border)}\n"
				+ "</style>\n</head>\n<body>\n" + body + "\n</body>\n</html>\n";
	}

	private String baseName(File file) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}

	private String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	// ------------------------------------------------------------------ editing helpers

	private void wrapSelection(String prefix, String suffix, String placeholder) {
		JTextArea area = textAreaMarkDown;
		String selected = area.getSelectedText();
		int start = area.getSelectionStart();
		if(selected == null || selected.isEmpty()) {
			area.replaceSelection(prefix + placeholder + suffix);
			area.select(start + prefix.length(), start + prefix.length() + placeholder.length());
		} else if(selected.startsWith(prefix) && selected.endsWith(suffix) && selected.length() >= prefix.length() + suffix.length()) {
			String inner = selected.substring(prefix.length(), selected.length() - suffix.length());
			area.replaceSelection(inner);
			area.select(start, start + inner.length());
		} else {
			area.replaceSelection(prefix + selected + suffix);
			area.select(start + prefix.length(), start + prefix.length() + selected.length());
		}
		area.requestFocusInWindow();
	}

	private void insertLink() {
		JTextArea area = textAreaMarkDown;
		String selected = area.getSelectedText();
		String text = selected == null || selected.isEmpty() ? "link text" : selected;
		int start = area.getSelectionStart();
		area.replaceSelection("[" + text + "](https://)");
		int urlStart = start + text.length() + 3;
		area.select(urlStart, urlStart + "https://".length());
		area.requestFocusInWindow();
	}

	private void insertImage() {
		JTextArea area = textAreaMarkDown;
		String selected = area.getSelectedText();
		String text = selected == null || selected.isEmpty() ? "description" : selected;
		int start = area.getSelectionStart();
		area.replaceSelection("![" + text + "](image.png)");
		int urlStart = start + text.length() + 4;
		area.select(urlStart, urlStart + "image.png".length());
		area.requestFocusInWindow();
	}

	private void codeBlock() {
		JTextArea area = textAreaMarkDown;
		String selected = area.getSelectedText();
		if(selected == null || selected.isEmpty()) {
			int start = area.getSelectionStart();
			area.replaceSelection("```\ncode\n```\n");
			area.select(start + 4, start + 8);
		} else {
			area.replaceSelection("```\n" + selected + "\n```");
		}
		area.requestFocusInWindow();
	}

	private void insertText(String text) {
		textAreaMarkDown.replaceSelection(text);
		textAreaMarkDown.requestFocusInWindow();
	}

	private void insertTable() {
		insertText("\n| Column 1 | Column 2 | Column 3 |\n|----------|----------|----------|\n|          |          |          |\n");
	}

	/** Adds the prefix to every selected line, or removes it when all of them already have it. */
	private void toggleLinePrefix(String prefix) {
		try {
			JTextArea area = textAreaMarkDown;
			int firstLine = area.getLineOfOffset(area.getSelectionStart());
			int end = area.getSelectionEnd();
			int lastLine = area.getLineOfOffset(end > area.getSelectionStart() && end > 0 ? end - 1 : end);

			boolean allHave = true;
			for(int line = firstLine; line <= lastLine; line++) {
				String text = lineText(line);
				if(!text.startsWith(prefix)) {
					allHave = false;
					break;
				}
			}
			for(int line = lastLine; line >= firstLine; line--) {
				int lineStart = area.getLineStartOffset(line);
				if(allHave) {
					area.replaceRange("", lineStart, lineStart + prefix.length());
				} else if(!lineText(line).startsWith(prefix)) {
					area.insert(prefix, lineStart);
				}
			}
			area.requestFocusInWindow();
		} catch (BadLocationException e) {
			logger.debug("Cannot change the selected lines");
		}
	}

	private void numberedList() {
		try {
			JTextArea area = textAreaMarkDown;
			int firstLine = area.getLineOfOffset(area.getSelectionStart());
			int end = area.getSelectionEnd();
			int lastLine = area.getLineOfOffset(end > area.getSelectionStart() && end > 0 ? end - 1 : end);
			for(int line = lastLine; line >= firstLine; line--) {
				area.insert((line - firstLine + 1) + ". ", area.getLineStartOffset(line));
			}
			area.requestFocusInWindow();
		} catch (BadLocationException e) {
			logger.debug("Cannot change the selected lines");
		}
	}

	private void cycleHeading() {
		try {
			JTextArea area = textAreaMarkDown;
			int line = area.getLineOfOffset(area.getCaretPosition());
			int lineStart = area.getLineStartOffset(line);
			String text = lineText(line);
			int hashes = 0;
			while(hashes < text.length() && text.charAt(hashes) == '#')
				hashes++;
			int remove = hashes > 0 && hashes < text.length() && text.charAt(hashes) == ' ' ? hashes + 1 : hashes;
			String next = hashes == 0 ? "# " : hashes >= 3 ? "" : repeat('#', hashes + 1) + " ";
			area.replaceRange(next, lineStart, lineStart + remove);
			area.requestFocusInWindow();
		} catch (BadLocationException e) {
			logger.debug("Cannot change the heading");
		}
	}

	private String lineText(int line) throws BadLocationException {
		JTextArea area = textAreaMarkDown;
		int start = area.getLineStartOffset(line);
		int end = area.getLineEndOffset(line);
		return area.getText(start, end - start).replace("\n", "").replace("\r", "");
	}

	private String repeat(char c, int times) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < times; i++)
			sb.append(c);
		return sb.toString();
	}

	// ------------------------------------------------------------------ preferences

	private void loadPreferences() {
		try {
			Preferences prefs = Preferences.userRoot().node(PREF_NODE);
			zoomPercent = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, prefs.getInt("zoom", 100)));
			previewThemeOverride = prefs.getBoolean("invertPreview", false);
			syncScroll = prefs.getBoolean("syncScroll", true);
			try {
				viewMode = ViewMode.valueOf(prefs.get("viewMode", "SPLIT"));
			} catch (IllegalArgumentException e) {
				viewMode = ViewMode.SPLIT;
			}
		} catch (Throwable t) {
			// preferences not available: defaults
		}
	}

	private void savePreferences() {
		try {
			Preferences prefs = Preferences.userRoot().node(PREF_NODE);
			prefs.putInt("zoom", zoomPercent);
			prefs.putBoolean("invertPreview", previewThemeOverride);
			prefs.putBoolean("syncScroll", syncScroll);
			prefs.put("viewMode", viewMode.name());
		} catch (Throwable t) {
			// not persisted
		}
		if(btnWrap != null && btnSync != null && btnPreviewTheme != null) {
			btnSync.setSelected(syncScroll);
			btnPreviewTheme.setSelected(previewThemeOverride);
		}
	}
}
