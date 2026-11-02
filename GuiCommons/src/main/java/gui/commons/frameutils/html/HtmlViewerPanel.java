/*
 * Copyright (c) A.M. Design And Development. All rights reserved.
 */
package gui.commons.frameutils.html;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.swing.LinkListener;

/**
 * HTML viewer made only of Swing (Flying Saucer renderer, no JavaFX, no native code): works on every Java from 8 on.
 * The page is cleaned and made XHTML by jsoup, scripts and old stylesheets are dropped and a modern theme (dark or light,
 * following the current look and feel) is applied. Features: table of contents built from the headings, search with
 * highlight (Enter / Shift+Enter, Ctrl+F), zoom (Ctrl+wheel, Ctrl +/-/0), selectable and copyable text, external links
 * opened in the browser and internal anchors.
 */
public class HtmlViewerPanel extends JPanel {

	private static final long serialVersionUID = 6025331894473350617L;
	private static final Logger logger = Logger.getLogger(HtmlViewerPanel.class);

	private static final int MAX_HITS = 2000;

	private final XHTMLPanel xhtml = new XHTMLPanel();
	private final FSScrollPane xhtmlScroll = new FSScrollPane(xhtml);
	private final JTextField searchField = new JTextField(22);
	private final JLabel countLabel = new JLabel(" ");
	private final DefaultListModel<Heading> tocModel = new DefaultListModel<>();
	private final JList<Heading> tocList = new JList<>(tocModel);
	private final JSplitPane split;

	/** Cleaned page, never modified: search results are shown on a copy */
	private Document source;
	private String baseUrl = "";
	private int hitCount = 0;
	private int currentHit = -1;
	private String lastQuery = "";
	private boolean tocUpdating = false;

	private static final class Heading {
		final int level;
		final String text;
		final String id;

		Heading(int level, String text, String id) {
			this.level = level;
			this.text = text;
			this.id = id;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	public HtmlViewerPanel() {
		super(new BorderLayout());

		xhtml.setOpaque(true);
		xhtml.setInteractive(true);
		xhtml.setMaxFontScale(3f);
		xhtml.setMinFontScale(0.6f);
		replaceLinkListener();
		xhtmlScroll.setBorder(BorderFactory.createEmptyBorder());
		xhtmlScroll.getVerticalScrollBar().setUnitIncrement(24);

		tocList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tocList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Heading h = (Heading) value;
				label.setBorder(BorderFactory.createEmptyBorder(3, 8 + (h.level - 1) * 14, 3, 6));
				label.setFont(label.getFont().deriveFont(h.level == 1 ? Font.BOLD : Font.PLAIN));
				return label;
			}
		});
		tocList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && !tocUpdating && tocList.getSelectedValue() != null) {
				scrollToId(tocList.getSelectedValue().id);
			}
		});
		JScrollPane tocScroll = new JScrollPane(tocList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		tocScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor()));
		tocScroll.setMinimumSize(new Dimension(0, 0));

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tocScroll, xhtmlScroll);
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setDividerLocation(230);
		split.setDividerSize(4);
		split.setContinuousLayout(true);

		add(buildToolbar(), BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);
		installKeys();
	}

	// ------------------------------------------------------------------ public API

	/** Loads a page: file path or file:/http: URI. Safe to call from any thread. */
	public void load(final String pathOrUri) {
		new Thread(() -> {
			try {
				URI uri = pathOrUri.startsWith("file:") || pathOrUri.startsWith("http") ? URI.create(pathOrUri) : new File(pathOrUri).toURI();
				String html;
				if ("file".equals(uri.getScheme())) {
					html = new String(Files.readAllBytes(new File(uri).toPath()), StandardCharsets.UTF_8);
				} else {
					html = Jsoup.connect(uri.toString()).get().outerHtml();
				}
				final Document prepared = prepare(html, uri.toString());
				SwingUtilities.invokeLater(() -> show(prepared, uri.toString()));
			} catch (Exception e) {
				logger.error("Cannot load " + pathOrUri, e);
				SwingUtilities.invokeLater(() -> showError(pathOrUri, e));
			}
		}, "html-viewer-loader").start();
	}

	public void focusSearch() {
		searchField.requestFocusInWindow();
		searchField.selectAll();
	}

	public void zoomIn() {
		xhtml.incrementFontSize();
	}

	public void zoomOut() {
		xhtml.decrementFontSize();
	}

	public void zoomReset() {
		xhtml.resetFontSize();
	}

	// ------------------------------------------------------------------ page preparation

	private Document prepare(String html, String base) {
		Document d = Jsoup.parse(html, base);
		d.select("script, style, link[rel=stylesheet], noscript").remove();
		for (Element img : d.select("img[src]")) {
			String abs = img.absUrl("src");
			if (!abs.isEmpty()) {
				img.attr("src", abs);
			}
		}
		// ids for the headings, and the table of contents
		final List<Heading> headings = new ArrayList<>();
		int n = 0;
		for (Element h : d.select("h1, h2, h3, h4")) {
			String text = h.text().trim();
			if (text.isEmpty()) {
				continue;
			}
			if (h.id().isEmpty()) {
				h.attr("id", "sec-" + (++n));
			}
			headings.add(new Heading(h.tagName().charAt(1) - '0', text, h.id()));
		}
		d.head().appendElement("style").attr("type", "text/css").appendChild(new DataNode(theme()));
		d.outputSettings().syntax(Document.OutputSettings.Syntax.xml).escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
				.charset("UTF-8").prettyPrint(false);
		for (Node child : new ArrayList<>(d.childNodes())) {
			if (child instanceof org.jsoup.nodes.DocumentType) {
				child.remove();
			}
		}
		SwingUtilities.invokeLater(() -> {
			tocModel.clear();
			for (Heading h : headings) {
				tocModel.addElement(h);
			}
		});
		return d;
	}

	private void show(Document prepared, String base) {
		source = prepared;
		baseUrl = base;
		hitCount = 0;
		currentHit = -1;
		lastQuery = "";
		countLabel.setText(" ");
		render(prepared);
	}

	private void render(Document doc) {
		try {
			String xml = doc.outerHtml();
			org.w3c.dom.Document w3c = XMLResource.load(new java.io.StringReader(xml)).getDocument();
			xhtml.setDocument(w3c, baseUrl);
		} catch (Exception e) {
			logger.error("Cannot render the page", e);
			showError(baseUrl, e);
		}
	}

	private void showError(String what, Exception e) {
		Document d = Jsoup.parse("<html><body><h1>Cannot open the page</h1><p>" + escape(what) + "</p><pre>" + escape(String.valueOf(e)) + "</pre></body></html>");
		d.head().appendElement("style").attr("type", "text/css").appendChild(new DataNode(theme()));
		d.outputSettings().syntax(Document.OutputSettings.Syntax.xml).escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
		render(d);
	}

	private static String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	// ------------------------------------------------------------------ links

	private void replaceLinkListener() {
		for (Object l : new ArrayList<Object>(xhtml.getMouseTrackingListeners())) {
			if (l instanceof LinkListener) {
				xhtml.removeMouseTrackingListener((org.xhtmlrenderer.swing.FSMouseListener) l);
			}
		}
		xhtml.addMouseTrackingListener(new LinkListener() {
			@Override
			public void linkClicked(BasicPanel panel, String uri) {
				openLink(uri);
			}
		});
	}

	private void openLink(String uri) {
		if (uri == null || uri.isEmpty()) {
			return;
		}
		int hash = uri.indexOf('#');
		if (uri.startsWith("#") || (hash > 0 && baseUrl.length() > 0 && uri.substring(0, hash).equals(baseUrl.replaceAll("#.*$", "")))) {
			scrollToId(uri.substring(hash + 1));
			return;
		}
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(URI.create(uri));
			}
		} catch (Exception e) {
			logger.error("Cannot open link " + uri, e);
		}
	}

	/** Scrolls to the element with given id; the layout may not be ready yet, so it retries for a short while */
	private void scrollToId(final String id) {
		final int[] tries = { 0 };
		final Timer timer = new Timer(60, null);
		timer.addActionListener(e -> {
			Box box = null;
			try {
				box = xhtml.getSharedContext().getBoxById(id);
			} catch (Exception ignored) {
				// layout not ready
			}
			if (box != null) {
				timer.stop();
				xhtml.scrollTo(new Point(0, Math.max(0, box.getAbsY() - 12)));
			} else if (++tries[0] > 25) {
				timer.stop();
			}
		});
		timer.setInitialDelay(0);
		timer.start();
	}

	// ------------------------------------------------------------------ search

	private void search(String query, boolean restart) {
		if (source == null) {
			return;
		}
		query = query.trim();
		if (query.isEmpty()) {
			hitCount = 0;
			currentHit = -1;
			lastQuery = "";
			countLabel.setText(" ");
			render(source);
			return;
		}
		if (restart || !query.equals(lastQuery)) {
			lastQuery = query;
			currentHit = 0;
		}
		Document copy = source.clone();
		hitCount = markHits(copy, query, currentHit);
		if (hitCount == 0) {
			currentHit = -1;
			countLabel.setText("No results");
			render(copy);
			return;
		}
		currentHit = Math.min(currentHit, hitCount - 1);
		countLabel.setText((currentHit + 1) + " / " + hitCount + (hitCount >= MAX_HITS ? "+" : ""));
		render(copy);
		scrollToId("hit-" + currentHit);
	}

	private void step(boolean forward) {
		String query = searchField.getText().trim();
		if (query.isEmpty() || source == null) {
			return;
		}
		if (!query.equals(lastQuery) || hitCount == 0) {
			search(query, true);
			return;
		}
		currentHit = (currentHit + (forward ? 1 : hitCount - 1)) % hitCount;
		search(query, false);
	}

	/** Wraps every match in a span; returns the number of matches */
	private static int markHits(Document doc, String query, int current) {
		final String needle = query.toLowerCase(Locale.ROOT);
		int count = 0;
		List<TextNode> texts = new ArrayList<>();
		collectText(doc.body(), texts);
		for (TextNode tn : texts) {
			String text = tn.getWholeText();
			String lower = text.toLowerCase(Locale.ROOT);
			if (lower.length() != text.length() || !lower.contains(needle)) {
				continue;
			}
			int from = 0;
			int idx;
			List<Node> parts = new ArrayList<>();
			while ((idx = lower.indexOf(needle, from)) >= 0 && count < MAX_HITS) {
				if (idx > from) {
					parts.add(new TextNode(text.substring(from, idx)));
				}
				Element span = new Element("span");
				span.attr("id", "hit-" + count).addClass(count == current ? "hit cur" : "hit");
				span.appendChild(new TextNode(text.substring(idx, idx + needle.length())));
				parts.add(span);
				count++;
				from = idx + needle.length();
			}
			if (from < text.length()) {
				parts.add(new TextNode(text.substring(from)));
			}
			for (Node part : parts) {
				tn.before(part);
			}
			tn.remove();
		}
		return count;
	}

	private static void collectText(Element root, List<TextNode> out) {
		if (root == null) {
			return;
		}
		for (Node child : root.childNodes()) {
			if (child instanceof TextNode) {
				if (!((TextNode) child).isBlank()) {
					out.add((TextNode) child);
				}
			} else if (child instanceof Element) {
				collectText((Element) child, out);
			}
		}
	}

	// ------------------------------------------------------------------ toolbar and keys

	private JComponent buildToolbar() {
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor()));

		JButton toc = button("Index", "Show or hide the table of contents");
		toc.addActionListener(e -> {
			boolean show = split.getDividerLocation() < 10;
			split.setDividerLocation(show ? 230 : 0);
		});
		searchField.putClientProperty("JTextField.placeholderText", "Search in the guide (Ctrl+F)");
		searchField.setToolTipText("Enter: next result, Shift+Enter: previous, Esc: clear");
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			private final Timer delay = new Timer(350, e -> search(searchField.getText(), true));

			{
				delay.setRepeats(false);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				delay.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				delay.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				delay.restart();
			}
		});
		searchField.addActionListener(e -> step((e.getModifiers() & ActionEvent.SHIFT_MASK) == 0));
		searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "prevHit");
		searchField.getActionMap().put("prevHit", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				step(false);
			}
		});
		searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
		searchField.getActionMap().put("clearSearch", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				searchField.setText("");
				search("", true);
			}
		});

		JButton prev = button("▲", "Previous result");
		prev.addActionListener(e -> step(false));
		JButton next = button("▼", "Next result");
		next.addActionListener(e -> step(true));
		JButton zoomOut = button("A-", "Zoom out (Ctrl -)");
		zoomOut.addActionListener(e -> zoomOut());
		JButton zoomReset = button("100%", "Reset zoom (Ctrl 0)");
		zoomReset.addActionListener(e -> zoomReset());
		JButton zoomIn = button("A+", "Zoom in (Ctrl +)");
		zoomIn.addActionListener(e -> zoomIn());

		bar.add(toc);
		bar.add(searchField);
		bar.add(prev);
		bar.add(next);
		bar.add(countLabel);
		bar.add(zoomOut);
		bar.add(zoomReset);
		bar.add(zoomIn);
		return bar;
	}

	private static JButton button(String text, String tip) {
		JButton b = new JButton(text);
		b.setToolTipText(tip);
		b.setFocusable(false);
		b.setMargin(new java.awt.Insets(2, 8, 2, 8));
		return b;
	}

	private void installKeys() {
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find", this::focusSearch);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "findNext", () -> step(true));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), "findPrev", () -> step(false));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "zoomIn", this::zoomIn);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn2", this::zoomIn);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut", this::zoomOut);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut2", this::zoomOut);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "zoomReset", this::zoomReset);
		xhtmlScroll.addMouseWheelListener(e -> {
			if (e.isControlDown()) {
				if (e.getWheelRotation() < 0) {
					zoomIn();
				} else {
					zoomOut();
				}
				e.consume();
			}
		});
	}

	private void bind(KeyStroke key, String name, final Runnable action) {
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
		getActionMap().put(name, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	// ------------------------------------------------------------------ theme

	private static Color borderColor() {
		Color c = UIManager.getColor("Component.borderColor");
		return c != null ? c : Color.GRAY;
	}

	private static boolean darkTheme() {
		Color bg = UIManager.getColor("Panel.background");
		return bg == null || (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000 < 128;
	}

	/** CSS 2.1 only (the renderer does not know variables, flex or grid) */
	private static String theme() {
		boolean dark = darkTheme();
		String bg = dark ? "#1e2025" : "#ffffff";
		String text = dark ? "#d7dae0" : "#24292f";
		String muted = dark ? "#8b93a1" : "#57606a";
		String accent = dark ? "#61afef" : "#0969da";
		String accent2 = dark ? "#e5c07b" : "#9a6700";
		String line = dark ? "#343944" : "#d0d7de";
		String codeBg = dark ? "#282c34" : "#f6f8fa";
		String hitBg = dark ? "#5b4a1a" : "#fff2a8";
		String curBg = dark ? "#d19a3a" : "#ffb03a";
		String curText = dark ? "#111111" : "#000000";
		return "html, body { background: " + bg + "; color: " + text + "; }\n"
			+ "body { font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif; font-size: 15px; line-height: 1.6; margin: 0 auto; padding: 24px 44px 60px 44px; max-width: 980px; }\n"
			+ "h1, h2, h3, h4 { font-weight: bold; line-height: 1.25; margin: 1.6em 0 0.6em 0; color: " + text + "; }\n"
			+ "h1 { font-size: 2.1em; text-align: center; color: " + accent + "; margin-top: 0.4em; }\n"
			+ "h2 { font-size: 1.55em; color: " + accent + "; border-bottom: 1px solid " + line + "; padding-bottom: 0.25em; }\n"
			+ "h3 { font-size: 1.25em; color: " + accent2 + "; }\n"
			+ "h4 { font-size: 1.05em; color: " + muted + "; }\n"
			+ "p { margin: 0.7em 0; }\n"
			+ "a { color: " + accent + "; text-decoration: none; }\n"
			+ "a:hover { text-decoration: underline; }\n"
			+ "ul, ol { margin: 0.6em 0; padding-left: 1.8em; }\n"
			+ "li { margin: 0.28em 0; }\n"
			+ "code, kbd, tt { font-family: Consolas, 'Courier New', monospace; font-size: 0.92em; background: " + codeBg + "; color: " + accent2 + "; padding: 1px 5px; border: 1px solid " + line + "; }\n"
			+ "pre { font-family: Consolas, 'Courier New', monospace; background: " + codeBg + "; border: 1px solid " + line + "; padding: 10px 14px; margin: 0.9em 0; }\n"
			+ "pre code { border: 0; padding: 0; background: transparent; }\n"
			+ "table { border-collapse: collapse; margin: 0.9em 0; }\n"
			+ "th, td { border: 1px solid " + line + "; padding: 6px 12px; text-align: left; }\n"
			+ "th { background: " + codeBg + "; color: " + accent + "; }\n"
			+ "blockquote { border-left: 4px solid " + accent + "; background: " + codeBg + "; margin: 0.9em 0; padding: 4px 16px; color: " + muted + "; }\n"
			+ "hr { border: 0; border-top: 1px solid " + line + "; margin: 1.6em 0; }\n"
			+ "img { max-width: 100%; }\n"
			+ ".center { text-align: center; }\n"
			+ ".highlightBolded { font-weight: bold; color: " + accent2 + "; }\n"
			+ ".highlightBoldedGrey { font-weight: bold; color: " + muted + "; }\n"
			+ ".SHCT_TEXT { font-weight: bold; color: " + accent2 + "; }\n"
			+ ".subMenuthirdLevel { font-weight: bold; color: " + accent + "; }\n"
			+ ".hit { background: " + hitBg + "; color: " + text + "; }\n"
			+ ".cur { background: " + curBg + "; color: " + curText + "; }\n";
	}

	/** Kept for the callers that still ask the page to be re-laid out after a resize */
	public void resizeComp(int width, int height) {
		xhtml.relayout();
	}
}
