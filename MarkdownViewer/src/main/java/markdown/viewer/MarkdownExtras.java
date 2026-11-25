package markdown.viewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * Pure functions behind the "extra" features of the viewer (no windows here, every method is testable):
 * GitHub alerts, code highlighting, :emoji:, front matter, clickable task lists, pasted tables and table sorting.
 */
public final class MarkdownExtras {

	private MarkdownExtras() {
	}

	// ------------------------------------------------------------------ alerts  > [!NOTE]

	private static final String[] ALERTS = {"NOTE", "TIP", "IMPORTANT", "WARNING", "CAUTION"};
	private static final Pattern ALERT_START = Pattern.compile("<blockquote>\\s*<p>\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\\]\\s*", Pattern.CASE_INSENSITIVE);

	/**
	 * GitHub alerts: a quote that starts with [!NOTE] / [!TIP] / [!IMPORTANT] / [!WARNING] / [!CAUTION] becomes
	 * {@code <div class="alert-note"><p class="alert-title">Note</p>...</div>}. Other quotes are left alone.
	 */
	public static String applyAlerts(String html) {
		if(html == null || html.indexOf("<blockquote>") < 0)
			return html;
		StringBuilder out = new StringBuilder(html.length() + 128);
		int pos = 0;
		Matcher m = ALERT_START.matcher(html);
		while(m.find(pos)) {
			int end = matchingClose(html, m.start());
			if(end < 0)
				break;
			String type = m.group(1).toUpperCase(Locale.ROOT);
			String inner = html.substring(m.end(), end);
			// what is left of the first paragraph after the marker
			if(inner.startsWith("</p>"))
				inner = inner.substring(4).replaceFirst("^\\s+", "");
			else
				inner = "<p>" + inner;
			out.append(html, pos, m.start());
			out.append("<div class=\"alert-").append(type.toLowerCase(Locale.ROOT)).append("\"><p class=\"alert-title\">")
				.append(title(type)).append("</p>").append(inner).append("</div>");
			pos = end + "</blockquote>".length();
		}
		out.append(html, pos, html.length());
		return out.toString();
	}

	/** Index of the </blockquote> that closes the one opened at {@code start} (nested quotes counted), -1 when unbalanced. */
	private static int matchingClose(String html, int start) {
		int depth = 0;
		int i = start;
		while(i < html.length()) {
			int open = html.indexOf("<blockquote", i);
			int close = html.indexOf("</blockquote>", i);
			if(close < 0)
				return -1;
			if(open >= 0 && open < close) {
				depth++;
				i = open + "<blockquote".length();
			} else {
				depth--;
				if(depth == 0)
					return close;
				i = close + "</blockquote>".length();
			}
		}
		return -1;
	}

	private static String title(String type) {
		return type.charAt(0) + type.substring(1).toLowerCase(Locale.ROOT);
	}

	/** Swing preview rules for the alerts (Swing ignores most of CSS3, so colors are explicit). */
	public static List<String> swingAlertRules(boolean dark) {
		List<String> rules = new ArrayList<String>();
		String[] colors = dark ? new String[] {"#4493f8", "#3fb950", "#ab7df8", "#d29922", "#f85149"} : new String[] {"#0969da", "#1a7f37", "#8250df", "#9a6700", "#d1242f"};
		String[] backgrounds = dark ? new String[] {"#1b2a3d", "#1a2e22", "#2a2340", "#33290f", "#3a1d1f"} : new String[] {"#eaf3ff", "#e8f6ec", "#f3ecff", "#fff6dd", "#ffebe9"};
		for(int i = 0; i < ALERTS.length; i++) {
			String name = ALERTS[i].toLowerCase(Locale.ROOT);
			rules.add("div.alert-" + name + " {background-color:" + backgrounds[i] + "; border-left-width:4px; border-left-style:solid; border-left-color:" + colors[i]
					+ "; padding:4px 12px; margin-top:8px; margin-bottom:8px;}");
			rules.add("div.alert-" + name + " p.alert-title {color:" + colors[i] + "; font-weight:bold; margin-top:2px; margin-bottom:2px;}");
		}
		return rules;
	}

	/** Css of the alerts for the exported page: a variable per type, light and dark. */
	public static String exportAlertCss() {
		StringBuilder css = new StringBuilder();
		// one class per box (Swing html does not understand two), so the base rule lists them all
		css.append(".alert-note,.alert-tip,.alert-important,.alert-warning,.alert-caution{margin:1em 0;padding:.4em 1em;border-left:.25em solid var(--c);background:var(--cbg);border-radius:0 6px 6px 0}"
				+ ".alert-title{color:var(--c);font-weight:600;margin:.2em 0}\n");
		String[] light = {"#0969da", "#1a7f37", "#8250df", "#9a6700", "#d1242f"};
		String[] lightBg = {"#eaf3ff", "#e8f6ec", "#f3ecff", "#fff6dd", "#ffebe9"};
		String[] dark = {"#4493f8", "#3fb950", "#ab7df8", "#d29922", "#f85149"};
		String[] darkBg = {"#1b2a3d", "#1a2e22", "#2a2340", "#33290f", "#3a1d1f"};
		StringBuilder darkRules = new StringBuilder();
		for(int i = 0; i < ALERTS.length; i++) {
			String name = ALERTS[i].toLowerCase(Locale.ROOT);
			css.append(".alert-").append(name).append("{--c:").append(light[i]).append(";--cbg:").append(lightBg[i]).append("}");
			darkRules.append(".alert-").append(name).append("{--c:").append(dark[i]).append(";--cbg:").append(darkBg[i]).append("}");
		}
		css.append("\n@media (prefers-color-scheme:dark){").append(darkRules).append("}\n");
		return css.toString();
	}

	// ------------------------------------------------------------------ code highlighting

	private static final Pattern CODE_BLOCK = Pattern.compile("<pre><code class=\"language-([A-Za-z0-9_+#.-]+)\">(.*?)</code></pre>", Pattern.DOTALL);
	private static final int MAX_HIGHLIGHT_CHARS = 30000;
	private static final Map<String, String> LANGUAGES = new HashMap<String, String>();

	static {
		put(SyntaxConstants.SYNTAX_STYLE_JAVA, "java");
		put(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, "javascript", "js", "jsx", "node");
		put(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT, "typescript", "ts", "tsx");
		put(SyntaxConstants.SYNTAX_STYLE_JSON, "json", "jsonc");
		put(SyntaxConstants.SYNTAX_STYLE_XML, "xml", "xsd", "svg", "pom");
		put(SyntaxConstants.SYNTAX_STYLE_HTML, "html", "htm");
		put(SyntaxConstants.SYNTAX_STYLE_CSS, "css");
		put(SyntaxConstants.SYNTAX_STYLE_SQL, "sql", "plsql", "pgsql");
		put(SyntaxConstants.SYNTAX_STYLE_PYTHON, "python", "py");
		put(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL, "bash", "sh", "shell", "zsh");
		put(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH, "bat", "batch", "cmd");
		put(SyntaxConstants.SYNTAX_STYLE_C, "c", "h");
		put(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "cpp", "c++", "cc", "hpp");
		put(SyntaxConstants.SYNTAX_STYLE_CSHARP, "csharp", "cs", "c#");
		put(SyntaxConstants.SYNTAX_STYLE_YAML, "yaml", "yml");
		put(SyntaxConstants.SYNTAX_STYLE_PHP, "php");
		put(SyntaxConstants.SYNTAX_STYLE_RUBY, "ruby", "rb");
		put(SyntaxConstants.SYNTAX_STYLE_GO, "go", "golang");
		put(SyntaxConstants.SYNTAX_STYLE_KOTLIN, "kotlin", "kt");
		put(SyntaxConstants.SYNTAX_STYLE_GROOVY, "groovy", "gradle");
		put(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE, "properties", "ini", "conf");
		put(SyntaxConstants.SYNTAX_STYLE_LUA, "lua");
		put(SyntaxConstants.SYNTAX_STYLE_PERL, "perl", "pl");
		put(SyntaxConstants.SYNTAX_STYLE_SCALA, "scala");
		put(SyntaxConstants.SYNTAX_STYLE_DELPHI, "pascal", "delphi");
		put(SyntaxConstants.SYNTAX_STYLE_MARKDOWN, "markdown", "md");
	}

	private static void put(String style, String... names) {
		for(String n : names)
			LANGUAGES.put(n, style);
	}

	/** RSyntaxTextArea style of a fence language ("js", "java"...), null when unknown. */
	public static String syntaxStyle(String language) {
		return language == null ? null : LANGUAGES.get(language.toLowerCase(Locale.ROOT));
	}

	/**
	 * Fenced code blocks with a known language get {@code <span class="tk-k">} tokens (c comment, k keyword, s string, n number,
	 * f function / attribute, a annotation, t tag): the colors come from the style sheet, so the preview and the exported page
	 * (light / dark) both follow their theme.
	 */
	public static String highlightCode(String html) {
		if(html == null || html.indexOf("<pre><code class=\"language-") < 0)
			return html;
		Matcher m = CODE_BLOCK.matcher(html);
		StringBuffer sb = new StringBuffer();
		while(m.find()) {
			String style = syntaxStyle(m.group(1));
			String text = unescape(m.group(2));
			String replacement = m.group();
			if(style != null && text.length() <= MAX_HIGHLIGHT_CHARS) {
				try {
					replacement = "<pre><code class=\"language-" + m.group(1) + "\">" + highlight(text, style) + "</code></pre>";
				} catch (RuntimeException | BadLocationException e) {
					// unknown / broken lexer: the block stays plain
				}
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	static String highlight(String text, String style) throws BadLocationException {
		String normalized = text.replace("\r\n", "\n");
		RSyntaxDocument doc = new RSyntaxDocument(style);
		doc.insertString(0, normalized, null);
		StringBuilder out = new StringBuilder(normalized.length() * 2);
		int lines = doc.getDefaultRootElement().getElementCount();
		for(int i = 0; i < lines; i++) {
			if(i > 0)
				out.append('\n');
			for(Token t = doc.getTokenListForLine(i); t != null && t.isPaintable(); t = t.getNextToken()) {
				String lexeme = t.getLexeme();
				String css = tokenClass(t.getType());
				String escaped = MarkdownText.htmlEscape(lexeme);
				if(css == null || lexeme.trim().isEmpty())
					out.append(escaped);
				else
					out.append("<span class=\"tk-").append(css).append("\">").append(escaped).append("</span>");
			}
		}
		return out.toString();
	}

	private static String tokenClass(int type) {
		switch(type) {
		case TokenTypes.COMMENT_EOL:
		case TokenTypes.COMMENT_MULTILINE:
		case TokenTypes.COMMENT_DOCUMENTATION:
		case TokenTypes.COMMENT_KEYWORD:
		case TokenTypes.COMMENT_MARKUP:
		case TokenTypes.MARKUP_COMMENT:
			return "c";
		case TokenTypes.RESERVED_WORD:
		case TokenTypes.RESERVED_WORD_2:
		case TokenTypes.DATA_TYPE:
		case TokenTypes.LITERAL_BOOLEAN:
			return "k";
		case TokenTypes.FUNCTION:
		case TokenTypes.MARKUP_TAG_ATTRIBUTE:
			return "f";
		case TokenTypes.LITERAL_NUMBER_DECIMAL_INT:
		case TokenTypes.LITERAL_NUMBER_FLOAT:
		case TokenTypes.LITERAL_NUMBER_HEXADECIMAL:
			return "n";
		case TokenTypes.LITERAL_CHAR:
		case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
		case TokenTypes.LITERAL_BACKQUOTE:
		case TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE:
		case TokenTypes.REGEX:
			return "s";
		case TokenTypes.ANNOTATION:
		case TokenTypes.PREPROCESSOR:
			return "a";
		case TokenTypes.MARKUP_TAG_NAME:
		case TokenTypes.MARKUP_TAG_DELIMITER:
			return "t";
		default:
			return null;
		}
	}

	private static String unescape(String s) {
		return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
	}

	private static final Pattern TOKEN_SPAN = Pattern.compile("<span class=\"tk-([a-z])\">([^<]*)</span>");
	private static final String TOKEN_KINDS = "cksnfat";

	/**
	 * Swing preview only: the token spans become {@code <font color>} with the colors of the theme (Swing does not apply a css class
	 * to a span inside code). The exported page keeps the classes and has its own css.
	 */
	public static String swingTokens(String html, boolean dark) {
		if(html == null || html.indexOf("class=\"tk-") < 0)
			return html;
		String[] colors = tokenColors(dark);
		Matcher m = TOKEN_SPAN.matcher(html);
		StringBuffer sb = new StringBuffer();
		while(m.find()) {
			int kind = TOKEN_KINDS.indexOf(m.group(1).charAt(0));
			m.appendReplacement(sb, Matcher.quoteReplacement(kind < 0 ? m.group() : "<font color=\"" + colors[kind] + "\">" + m.group(2) + "</font>"));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String[] tokenColors(boolean dark) {
		return dark ? new String[] {"#7f8c98", "#ff7b72", "#a5d6a7", "#79c0ff", "#d2a8ff", "#e3b341", "#7ee787"}
				: new String[] {"#6a737d", "#cf222e", "#0a7d3c", "#0550ae", "#8250df", "#9a6700", "#116329"};
	}

	private static final Pattern ALERT_BOX = Pattern.compile("<div class=\"alert-(note|tip|important|warning|caution)\"><p class=\"alert-title\">([^<]*)</p>");

	/**
	 * Html for pasting in rich text editors (Word, Outlook, Gmail), which ignore css classes: code colors become font colors
	 * and the alerts one-cell tables with the border and background of the preview (light colors).
	 */
	public static String richHtml(String html) {
		if(html == null)
			return "";
		String out = swingTokens(html, false);
		Matcher m = ALERT_BOX.matcher(out);
		StringBuilder sb = new StringBuilder(out.length() + 256);
		int pos = 0;
		while(m.find(pos)) {
			int end = matchingDivClose(out, m.start());
			if(end < 0)
				break;
			int type = Arrays.asList("note", "tip", "important", "warning", "caution").indexOf(m.group(1));
			String color = new String[] {"#0969da", "#1a7f37", "#8250df", "#9a6700", "#d1242f"}[type];
			String background = new String[] {"#eaf3ff", "#e8f6ec", "#f3ecff", "#fff6dd", "#ffebe9"}[type];
			sb.append(out, pos, m.start());
			sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"8\" style=\"border-collapse:collapse\"><tr><td style=\"background-color:").append(background)
				.append(";border-left:4px solid ").append(color).append("\" bgcolor=\"").append(background).append("\"><p><b><font color=\"").append(color).append("\">")
				.append(m.group(2)).append("</font></b></p>").append(out, m.end(), end).append("</td></tr></table>");
			pos = end + "</div>".length();
		}
		sb.append(out, pos, out.length());
		return sb.toString();
	}

	/** Index of the </div> that closes the div opened at {@code start}, -1 when unbalanced. */
	private static int matchingDivClose(String html, int start) {
		int depth = 0, i = start;
		while(i < html.length()) {
			int open = html.indexOf("<div", i);
			int close = html.indexOf("</div>", i);
			if(close < 0)
				return -1;
			if(open >= 0 && open < close) {
				depth++;
				i = open + 4;
			} else {
				depth--;
				if(depth == 0)
					return close;
				i = close + 6;
			}
		}
		return -1;
	}

	/** Token colors as css rules. */
	public static List<String> swingTokenRules(boolean dark) {
		String[] c = dark ? new String[] {"#7f8c98", "#ff7b72", "#a5d6a7", "#79c0ff", "#d2a8ff", "#e3b341", "#7ee787"}
				: new String[] {"#6a737d", "#cf222e", "#0a7d3c", "#0550ae", "#8250df", "#9a6700", "#116329"};
		return Arrays.asList(
				".tk-c {color:" + c[0] + ";}", ".tk-k {color:" + c[1] + ";}", ".tk-s {color:" + c[2] + ";}", ".tk-n {color:" + c[3] + ";}",
				".tk-f {color:" + c[4] + ";}", ".tk-a {color:" + c[5] + ";}", ".tk-t {color:" + c[6] + ";}");
	}

	public static String exportTokenCss() {
		return ".tk-c{color:#6a737d}.tk-k{color:#cf222e}.tk-s{color:#0a7d3c}.tk-n{color:#0550ae}.tk-f{color:#8250df}.tk-a{color:#9a6700}.tk-t{color:#116329}\n"
				+ "@media (prefers-color-scheme:dark){.tk-c{color:#7f8c98}.tk-k{color:#ff7b72}.tk-s{color:#a5d6a7}.tk-n{color:#79c0ff}.tk-f{color:#d2a8ff}.tk-a{color:#e3b341}.tk-t{color:#7ee787}}\n";
	}

	// ------------------------------------------------------------------ emoji

	private static final Pattern EMOJI_SKIP = Pattern.compile("<pre[\\s\\S]*?</pre>|<code[\\s\\S]*?</code>|<[^>]+>");

	/** The unicode character of a shortcode ("tada" -> party popper), null when unknown. */
	public static String emoji(String name) {
		EmojiCatalog.Entry e = EmojiCatalog.byName(name);
		return e == null ? null : e.text;
	}

	public static int emojiCount() {
		return EmojiCatalog.size();
	}

	/** :shortcode: to the character, in the text of the page only (code, tags and attributes are left alone). */
	public static String emojify(String html) {
		if(html == null || html.indexOf(':') < 0)
			return html;
		StringBuilder out = new StringBuilder(html.length());
		Matcher skip = EMOJI_SKIP.matcher(html);
		int pos = 0;
		while(skip.find()) {
			out.append(replaceCodes(html.substring(pos, skip.start())));
			out.append(skip.group());
			pos = skip.end();
		}
		out.append(replaceCodes(html.substring(pos)));
		return out.toString();
	}

	private static String replaceCodes(String text) {
		if(text.indexOf(':') < 0)
			return text;
		StringBuilder out = new StringBuilder(text.length());
		int i = 0;
		while(i < text.length()) {
			char c = text.charAt(i);
			if(c == ':') {
				int end = text.indexOf(':', i + 1);
				String e = end > i + 1 ? emoji(text.substring(i + 1, end)) : null;
				if(e != null) {
					out.append(e);
					i = end + 1;
					continue;
				}
			}
			out.append(c);
			i++;
		}
		return out.toString();
	}

	// ------------------------------------------------------------------ front matter

	/** The "---" block at the top of the file (delimiters included), null when there is none. */
	public static String frontMatter(String markdown) {
		if(markdown == null || !markdown.startsWith("---"))
			return null;
		String[] lines = MarkdownText.lines(markdown);
		int end = MarkdownText.frontMatterEnd(lines);
		if(end == 0)
			return null;
		int chars = 0;
		for(int i = 0; i < end; i++)
			chars += lines[i].length() + 1;
		return markdown.substring(0, Math.min(markdown.length(), chars));
	}

	/** The same text with the front matter replaced by spaces (line breaks and offsets stay as they are). */
	public static String blankFrontMatter(String markdown) {
		String block = frontMatter(markdown);
		if(block == null)
			return markdown;
		StringBuilder sb = new StringBuilder(markdown.length());
		for(int i = 0; i < block.length(); i++) {
			char c = block.charAt(i);
			sb.append(c == '\n' || c == '\r' ? c : ' ');
		}
		return sb.append(markdown, block.length(), markdown.length()).toString();
	}

	/** key / value pairs of the top level of the front matter (a list becomes "a, b, c"). */
	public static List<String[]> frontMatterEntries(String markdown) {
		List<String[]> entries = new ArrayList<String[]>();
		String block = frontMatter(markdown);
		if(block == null)
			return entries;
		String[] lines = MarkdownText.lines(block);
		String key = null;
		StringBuilder value = new StringBuilder();
		for(int i = 1; i < lines.length; i++) {
			String line = MarkdownText.stripCr(lines[i]);
			if(line.equals("---") || line.equals("..."))
				break;
			Matcher kv = Pattern.compile("^([A-Za-z0-9_][\\w .-]*):(?:\\s+(.*))?$").matcher(line);
			if(kv.matches()) {
				if(key != null)
					entries.add(new String[] {key, value.toString().trim()});
				key = kv.group(1).trim();
				value = new StringBuilder(unquote(kv.group(2) == null ? "" : kv.group(2).trim()));
			} else if(key != null && line.trim().startsWith("- ")) {
				if(value.length() > 0)
					value.append(", ");
				value.append(unquote(line.trim().substring(2).trim()));
			}
		}
		if(key != null)
			entries.add(new String[] {key, value.toString().trim()});
		return entries;
	}

	private static String unquote(String v) {
		if(v.length() >= 2 && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))))
			return v.substring(1, v.length() - 1);
		if(v.startsWith("[") && v.endsWith("]"))
			return v.substring(1, v.length() - 1);
		return v;
	}

	/** Small two column table of the front matter, empty when there is none. */
	public static String frontMatterHtml(String markdown) {
		List<String[]> entries = frontMatterEntries(markdown);
		if(entries.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder("<table class=\"frontmatter\" border=\"0\" cellpadding=\"4\" cellspacing=\"0\">");
		for(String[] e : entries)
			sb.append("<tr><th align=\"left\">").append(MarkdownText.htmlEscape(e[0])).append("</th><td>").append(MarkdownText.htmlEscape(e[1])).append("</td></tr>");
		return sb.append("</table>\n").toString();
	}

	// ------------------------------------------------------------------ task lists

	private static final Pattern TASK_LINE = Pattern.compile("^(\\s*(?:>\\s*)*(?:[-*+]|\\d{1,9}[.)])\\s+\\[)([ xX])(\\](?:\\s.*|$))");
	private static final Pattern TASK_INPUT = Pattern.compile("<input[^>]*task-list-item-checkbox[^>]*/?>", Pattern.CASE_INSENSITIVE);
	public static final String TASK_SCHEME = "mdv-task:";

	/** Line indexes of the task items (outside code fences and front matter), in document order. */
	public static List<Integer> taskLines(String markdown) {
		List<Integer> result = new ArrayList<Integer>();
		String[] lines = MarkdownText.lines(markdown);
		String fence = null;
		for(int i = MarkdownText.frontMatterEnd(lines); i < lines.length; i++) {
			String line = MarkdownText.stripCr(lines[i]);
			String t = line.trim();
			if(fence != null) {
				if(t.startsWith(fence) && t.replace(fence.substring(0, 1), "").isEmpty())
					fence = null;
				continue;
			}
			if(t.startsWith("```") || t.startsWith("~~~")) {
				char c = t.charAt(0);
				int n = 0;
				while(n < t.length() && t.charAt(n) == c)
					n++;
				fence = MarkdownText.repeat(c, n);
				continue;
			}
			if(TASK_LINE.matcher(line).matches())
				result.add(i);
		}
		return result;
	}

	/** {done, total} of the task items. */
	public static int[] taskProgress(String markdown) {
		int done = 0;
		List<Integer> tasks = taskLines(markdown);
		String[] lines = MarkdownText.lines(markdown);
		for(int i : tasks) {
			Matcher m = TASK_LINE.matcher(MarkdownText.stripCr(lines[i]));
			if(m.matches() && !m.group(2).equals(" "))
				done++;
		}
		return new int[] {done, tasks.size()};
	}

	/** The text with the {@code index}-th task item checked / unchecked, null when there is no such item. */
	public static String toggleTask(String markdown, int index) {
		int at = taskMarkOffset(markdown, index);
		if(at < 0)
			return null;
		return markdown.substring(0, at) + (markdown.charAt(at) == ' ' ? "x" : " ") + markdown.substring(at + 1);
	}

	/** Offset (in the text) of the space / x inside the box of the {@code index}-th task item, -1 when there is no such item. */
	public static int taskMarkOffset(String markdown, int index) {
		List<Integer> tasks = taskLines(markdown);
		if(index < 0 || index >= tasks.size())
			return -1;
		String[] lines = MarkdownText.lines(markdown);
		int offset = 0;
		for(int i = 0; i < tasks.get(index); i++)
			offset += lines[i].length() + 1;
		Matcher m = TASK_LINE.matcher(MarkdownText.stripCr(lines[tasks.get(index)]));
		return m.matches() ? offset + m.group(1).length() : -1;
	}

	/** Swing preview only: every task checkbox becomes a link "mdv-task:N" (a box character) that the viewer turns into a click on the source. */
	public static String taskLinks(String html) {
		if(html == null || html.indexOf("task-list-item-checkbox") < 0)
			return html;
		Matcher m = TASK_INPUT.matcher(html);
		StringBuffer sb = new StringBuffer();
		int n = 0;
		while(m.find()) {
			boolean checked = m.group().toLowerCase(Locale.ROOT).contains("checked");
			m.appendReplacement(sb, Matcher.quoteReplacement("<a class=\"task\" href=\"" + TASK_SCHEME + n + "\">" + (checked ? "☑" : "☐") + "</a>"));
			n++;
		}
		m.appendTail(sb);
		return sb.toString();
	}

	// ------------------------------------------------------------------ tables: paste and sort

	/** A pipe table made of spreadsheet cells (tab separated lines, same columns on every line); null when the text is not such a block. */
	public static String tabularToTable(String text) {
		if(text == null || text.indexOf('\t') < 0)
			return null;
		String[] raw = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
		List<String> rows = new ArrayList<String>();
		for(String r : raw) {
			if(!r.isEmpty() || !rows.isEmpty())
				rows.add(r);
		}
		while(!rows.isEmpty() && rows.get(rows.size() - 1).trim().isEmpty())
			rows.remove(rows.size() - 1);
		if(rows.size() < 2)
			return null;
		int columns = -1;
		List<List<String>> cells = new ArrayList<List<String>>();
		for(String r : rows) {
			List<String> parts = new ArrayList<String>(Arrays.asList(r.split("\t", -1)));
			if(columns < 0)
				columns = parts.size();
			if(parts.size() != columns || columns < 2)
				return null;
			for(int i = 0; i < parts.size(); i++)
				parts.set(i, parts.get(i).trim().replace("|", "\\|"));
			if(cells.isEmpty() && parts.contains(""))
				return null; // indented text, not a header row
			cells.add(parts);
		}
		String[] table = new String[rows.size() + 1];
		table[0] = join(cells.get(0));
		table[1] = join(Collections.nCopies(columns, "---"));
		for(int i = 1; i < cells.size(); i++)
			table[i + 1] = join(cells.get(i));
		String[] formatted = MarkdownText.formatTable(table);
		return formatted == null ? null : String.join("\n", formatted);
	}

	private static String join(List<String> cells) {
		StringBuilder sb = new StringBuilder("|");
		for(String c : cells)
			sb.append(' ').append(c).append(" |");
		return sb.toString();
	}

	/** Index of the table column the caret is in (0-based), from its column in the row. */
	public static int columnAt(String row, int caretColumn) {
		String line = MarkdownText.stripCr(row);
		int pipes = 0;
		boolean code = false;
		for(int i = 0; i < Math.min(caretColumn, line.length()); i++) {
			char c = line.charAt(i);
			if(c == '`')
				code = !code;
			else if(c == '|' && !code && (i == 0 || line.charAt(i - 1) != '\\'))
				pipes++;
		}
		if(line.trim().startsWith("|"))
			pipes--;
		int cells = MarkdownText.splitRow(line).size();
		return Math.max(0, Math.min(pipes, cells - 1));
	}

	/**
	 * Table lines (header, delimiter row, body) with the body sorted by a column: numbers (also "1,5", "12%", "$3") by value,
	 * the rest alphabetically ignoring case, empty cells last. The result is formatted. Null when it is not a table.
	 */
	public static String[] sortTable(String[] rows, int column, boolean ascending) {
		if(rows.length < 2 || !MarkdownText.isDelimiterRow(rows[1]))
			return null;
		List<String> body = new ArrayList<String>(Arrays.asList(rows).subList(2, rows.length));
		final int col = column;
		final int sign = ascending ? 1 : -1;
		Collections.sort(body, new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				String x = cell(a, col), y = cell(b, col);
				if(x.isEmpty() || y.isEmpty())
					return x.isEmpty() && y.isEmpty() ? 0 : x.isEmpty() ? 1 : -1;
				Double nx = number(x), ny = number(y);
				int r = nx != null && ny != null ? Double.compare(nx, ny) : x.compareToIgnoreCase(y);
				return sign * r;
			}
		});
		String[] sorted = new String[rows.length];
		sorted[0] = rows[0];
		sorted[1] = rows[1];
		for(int i = 0; i < body.size(); i++)
			sorted[i + 2] = body.get(i);
		String[] formatted = MarkdownText.formatTable(sorted);
		return formatted == null ? sorted : formatted;
	}

	private static String cell(String row, int column) {
		List<String> cells = MarkdownText.splitRow(row);
		return column < cells.size() ? cells.get(column).trim() : "";
	}

	private static Double number(String s) {
		String t = s.replaceAll("[\\s%$€£]", "");
		if(t.matches("[-+]?\\d{1,3}(\\.\\d{3})+(,\\d+)?"))
			t = t.replace(".", "").replace(',', '.');
		else
			t = t.replace(',', '.');
		try {
			return t.isEmpty() ? null : Double.valueOf(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
