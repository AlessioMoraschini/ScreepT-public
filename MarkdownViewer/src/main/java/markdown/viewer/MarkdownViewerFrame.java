package markdown.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.tautua.markdownpapers.Markdown;

import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.texteditor.commons.om.SupportedFileTextEditor;
import gui.texteditor.commons.utils.RsyntaxUtils;
import io.github.furstenheim.CopyDown;
import net.miginfocom.swing.MigLayout;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.string.StringWorker.EOL;

public class MarkdownViewerFrame extends ParentFrame {

	public static String defaultTheme = "/org/fife/ui/rsyntaxtextarea/themes/dark.xml";

	private static final long serialVersionUID = -3827828968889648153L;
	private JScrollPane scrollPaneMarkDown;
	private JScrollPane scrollPaneHtml;
	private JSplitPane splitPaneCode;
	private JEditorPane textAreaPreview;
	private JScrollPane scrollPanePreview;
	private JSplitPane splitPaneMain;
	private JPanel panelHead;
	private JButton btnHtmlPreview;
	private JButton btnToMarkdown;
	private JButton btnToHtml;
	private JButton btnMarkdownPreview;
	private JLabel lblHtmlView;
	private JLabel lblMarkdownView;
	private RSyntaxTextArea textAreaHtml;
	private RSyntaxTextArea textAreaMarkDown;

	private File htmlFile, markdownFile;
	private JButton btnSaveHtml;
	private JButton btnSaveMarkdown;

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
		setCloseAction(()->{
			int choice = dialogHelper.yesNoOrCanc("Every unsaved file will be lost. Do you want to save before to close?", "close Markdown Viewer?");
			if(JOptionHelper.CANC == choice) {
				return false;

			} else if(JOptionHelper.NO == choice) {
				return true;
			}

			boolean[] saved = save();
			boolean askConfirm = !saved[0] || !saved[1];
			if(askConfirm && !dialogHelper.yesOrNo("An error occurred while saving, do you want to exit anyway?", "Save failed!")) {
				return false;
			} else {
				return true;
			}
		});
	}

	public boolean[] save() {
		boolean mk = saveMarkdown(false);
		boolean ht = saveHtml(true);

		return new boolean[] {ht, mk};
	}

	public boolean saveMarkdown(boolean updateTitle) {
		try {
			if(!StringWorker.isEmpty(textAreaMarkDown.getText()) && !FileWorker.isTextAreaEqualToFileContent(textAreaMarkDown, markdownFile)) {
				markdownFile = saveCurrentFileAs(markdownFile, textAreaMarkDown, null, SupportedFileTextEditor.MARKDOWN);
			}
		} catch (IOException e) {
			logger.error("", e);
			return false;
		} finally {
			if(updateTitle)
				updateTitle();
		}

		return true;
	}

	public boolean saveHtml(boolean updateTitle) {
		try {
			if(!StringWorker.isEmpty(textAreaHtml.getText()) && !FileWorker.isTextAreaEqualToFileContent(textAreaHtml, htmlFile)) {
				htmlFile = saveCurrentFileAs(htmlFile, textAreaHtml, null, SupportedFileTextEditor.HTML);
			}
		} catch (IOException e) {
			logger.error("", e);
			return false;
		} finally {
			if(updateTitle)
				updateTitle();
		}

		return true;
	}

	public void updateTitle() {
		String htmlFileStr =  htmlFile != null ? htmlFile.getName() : " - ";
		String htmlFileStrFull =  htmlFile != null ? FileVarious.getCanonicalPathSafe(htmlFile) : "Undefined file";
		String markdownFileStr = markdownFile != null ? markdownFile.getName() : " - ";
		String markdownFileStrFull = markdownFile != null ? FileVarious.getCanonicalPathSafe(markdownFile) : "Undefined file";
		String title = "Markdown Viewer (markdown: " + markdownFileStr + "  ##  html: " + htmlFileStr;
		setTitle(title);

		lblHtmlView.setText("Html view (" + htmlFileStr + ")");
		lblHtmlView.setToolTipText(htmlFileStrFull);
		lblMarkdownView.setText("Markdown view (" + markdownFileStr + ")");
		lblMarkdownView.setToolTipText(markdownFileStrFull);
	}

	public void init(String html, String markdown) {
		getContentPane().setBackground(Color.GRAY);
		getContentPane().setLayout(new MigLayout("", "[grow][grow][grow][grow]", "[30px:n:40px][grow][grow][grow][grow]"));

		panelHead = new JPanel();
		panelHead.setBorder(new LineBorder(Color.LIGHT_GRAY));
		panelHead.setBackground(Color.DARK_GRAY);
		getContentPane().add(panelHead, "cell 0 0 4 1,grow");
		panelHead.setLayout(new MigLayout("", "[][][grow][:25%:25%][:25%:25%][grow][][]", "[grow]"));

		btnHtmlPreview = new JButton("Preview");
		btnHtmlPreview.setBackground(Color.GRAY);
		btnHtmlPreview.setCursor(GuiUtils.CURSOR_HAND);
		panelHead.add(btnHtmlPreview, "cell 0 0,alignx left");

		btnSaveHtml = new JButton("Save Html");
		btnSaveHtml.setBackground(Color.GRAY);
		btnSaveHtml.setCursor(GuiUtils.CURSOR_HAND);
		panelHead.add(btnSaveHtml, "cell 1 0");

		btnToMarkdown = new JButton("To markdown");
		btnToMarkdown.setBackground(Color.GRAY);
		btnToMarkdown.setCursor(GuiUtils.CURSOR_HAND);
		panelHead.add(btnToMarkdown, "cell 2 0,alignx left");

		btnToHtml = new JButton("To html");
		btnToHtml.setBackground(Color.GRAY);
		btnToHtml.setCursor(GuiUtils.CURSOR_HAND);
		panelHead.add(btnToHtml, "cell 5 0,alignx right");

		btnSaveMarkdown = new JButton("Save Markdown");
		btnSaveMarkdown.setBackground(Color.GRAY);
		btnSaveMarkdown.setCursor(GuiUtils.CURSOR_HAND);
		panelHead.add(btnSaveMarkdown, "cell 6 0");

		btnMarkdownPreview = new JButton("Preview");
		btnMarkdownPreview.setBackground(Color.GRAY);
		btnMarkdownPreview.setCursor(GuiUtils.CURSOR_HAND);
		panelHead.add(btnMarkdownPreview, "cell 7 0,alignx right");

		splitPaneMain = new JSplitPane();
		splitPaneMain.setDividerSize(5);
		splitPaneMain.setContinuousLayout(true);
		splitPaneMain.setOrientation(JSplitPane.VERTICAL_SPLIT);
		getContentPane().add(splitPaneMain, "cell 0 1 4 4,grow");

		scrollPanePreview = new JScrollPane();
		scrollPanePreview.setBorder(new LineBorder(Color.LIGHT_GRAY));
		splitPaneMain.setTopComponent(scrollPanePreview);

		textAreaPreview = new JEditorPane();
		textAreaPreview.setEditable(false);
		textAreaPreview.setBackground(Color.WHITE);
		textAreaPreview.setCursor(GuiUtils.CURSOR_TEXT);
		scrollPanePreview.setViewportView(textAreaPreview);
		addHtmlStyle();

		splitPaneCode = new JSplitPane();
		splitPaneCode.setDividerSize(5);
		splitPaneCode.setContinuousLayout(true);
		splitPaneMain.setBottomComponent(splitPaneCode);

		scrollPaneHtml = new JScrollPane();
		scrollPaneHtml.setBorder(new LineBorder(Color.LIGHT_GRAY));
		scrollPaneHtml.setBackground(Color.DARK_GRAY);
		splitPaneCode.setLeftComponent(scrollPaneHtml);

		textAreaHtml = new RSyntaxTextArea();
		scrollPaneHtml.setViewportView(textAreaHtml);
		textAreaHtml.setSyntaxEditingStyle(SupportedFileTextEditor.HTML.syntax);
		RsyntaxUtils.changeStyleViaThemeXml(textAreaHtml, defaultTheme);

		lblHtmlView = new JLabel("Html view");
		scrollPaneHtml.setColumnHeaderView(lblHtmlView);
		lblHtmlView.setHorizontalAlignment(SwingConstants.CENTER);
		lblHtmlView.setBorder(new LineBorder(Color.GRAY));
		lblHtmlView.setForeground(Color.LIGHT_GRAY);
		lblHtmlView.setOpaque(true);
		lblHtmlView.setBackground(Color.DARK_GRAY);

		scrollPaneMarkDown = new JScrollPane();
		scrollPaneMarkDown.setBorder(new LineBorder(Color.LIGHT_GRAY));
		splitPaneCode.setRightComponent(scrollPaneMarkDown);

		textAreaMarkDown = new RSyntaxTextArea();
		textAreaMarkDown.setSyntaxEditingStyle(SupportedFileTextEditor.MARKDOWN.syntax);
		scrollPaneMarkDown.setViewportView(textAreaMarkDown);
		textAreaMarkDown.setBorder(null);
		RsyntaxUtils.changeStyleViaThemeXml(textAreaMarkDown, defaultTheme);

		lblMarkdownView = new JLabel("Markdown view");
		scrollPaneMarkDown.setColumnHeaderView(lblMarkdownView);
		lblMarkdownView.setOpaque(true);
		lblMarkdownView.setHorizontalAlignment(SwingConstants.CENTER);
		lblMarkdownView.setForeground(Color.LIGHT_GRAY);
		lblMarkdownView.setBorder(new LineBorder(Color.GRAY));
		lblMarkdownView.setBackground(Color.DARK_GRAY);

		addHandlers();
		resizeToDefault(false, true, false);
		GuiUtils.centerComponent(this, getDefaultDimension());

		loadHtml(html, StringWorker.isEmpty(markdown));
		loadMarkdown(markdown, !StringWorker.isEmpty(markdown));

		SwingUtilities.invokeLater(()->{
			splitPaneCode.setDividerLocation(0.5d);
			splitPaneMain.setDividerLocation(0.5d);
		});
	}

	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(1200, 700);
	}



	private void addHandlers() {
		btnHtmlPreview.addActionListener((e) -> {
			String html = textAreaHtml.getText();
			if(StringWorker.isEmpty(html))
				return;

			loadHtmlPreview(html);
		});

		btnMarkdownPreview.addActionListener((e) -> {
			String html = markdownToHtml(textAreaMarkDown.getText());
			if(StringWorker.isEmpty(html))
				return;

			loadHtmlPreview(html);
		});

		btnToHtml.addActionListener((e) -> {
			markdownToHtml();
		});

		btnToMarkdown.addActionListener((e) -> {
			htmlToMarkdown();
		});

		btnSaveHtml.addActionListener((e) -> {
			saveHtml(true);
		});

		btnSaveMarkdown.addActionListener((e) -> {
			saveMarkdown(true);
		});
	}

	private void addHtmlStyle() {

		HTMLEditorKit kit = new HTMLEditorKit();
		textAreaPreview.setEditorKit(kit);

		// add some styles to the html
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body {color:#000; font-family:times; margin: 4px; }");
        styleSheet.addRule("h1 {color: #222222;}");
        styleSheet.addRule("h2 {color: #ff0000;}");
        styleSheet.addRule("pre {font : 10px monaco; color : black; background-color : #fafafa; }");

        textAreaPreview.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        textAreaPreview.setFont(new Font("Segoe UI", Font.PLAIN, 18));

	}

	public void loadHtml(String html, boolean loadPreview) {
		if(html == null)
			return;

		textAreaHtml.setText(html);
		if(loadPreview)
			btnHtmlPreview.doClick();
	}

	public void loadMarkdown(String markdown, boolean loadPreview) {
		if(markdown == null)
			return;

		textAreaMarkDown.setText(markdown);
		if(loadPreview)
			btnMarkdownPreview.doClick();
	}

	private void loadHtmlPreview(String toLoad) {
		textAreaPreview.setText(toLoad);
		GuiUtils.scrollJScrollPane(scrollPanePreview, false);
	}


	public String htmlToMarkdown(String html) {
		CopyDown converter = new CopyDown();
		return converter.convert(html);
	}

	public void htmlToMarkdown() {

		String html = textAreaHtml.getText();

		if(StringWorker.isEmpty(html))
			return;

		String markdown = htmlToMarkdown(html);
		textAreaMarkDown.setText(markdown);
	}


	public String markdownToHtml(String markdown) {
		Markdown mk = new Markdown();
		StringReader reader = new StringReader(markdown);
		Writer writer = new StringWriter();

		String html = "";
		try {
			mk.transform(reader, writer);
			html = writer.toString();
		} catch (Exception e) {
			logger.error("Can't convert from markdown to hml", e);
			dialogHelper.error("Invalid markdown data, cannot convert to html!");
			html = "";
		}

		return html;
	}


	public void markdownToHtml() {
		String markdown = textAreaMarkDown.getText();
		String html = markdownToHtml(markdown);

		if(StringWorker.isEmpty(markdown) || StringWorker.isEmpty(html))
			return;

		textAreaHtml.setText(html);
	}


	public File saveCurrentFileAs(File current, JTextArea source, EOL lineSeparator, SupportedFileTextEditor type) {
		File saved = super.saveCurrentFileAs(current, source, lineSeparator, type.extension);
		return saved;
	}

}
