package markdown.viewer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

/** The pure functions of the extra features: alerts, code colors, emoji, front matter, task lists, tables. */
public class MarkdownExtrasTest {

	/** The html the viewer really builds for a markdown text (same pipeline as the preview and the export). */
	private static String render(String markdown) throws Exception {
		Method m = MarkdownViewerFrame.class.getDeclaredMethod("renderBody", String.class, com.vladsch.flexmark.util.ast.Node[].class);
		m.setAccessible(true);
		return (String) m.invoke(null, markdown, null);
	}

	// ------------------------------------------------------------------ alerts

	@Test
	public void alertBecomesAColoredBox() throws Exception {
		String html = render("> [!NOTE]\n> Remember **this**.\n\nafter");
		assertTrue(html, html.contains("<div class=\"alert-note\"><p class=\"alert-title\">Note</p>"));
		assertTrue(html, html.contains("Remember <strong>this</strong>."));
		assertFalse(html, html.contains("[!NOTE]"));
		assertFalse(html, html.contains("<blockquote"));
		assertTrue(html.contains("<p>after</p>"));
	}

	@Test
	public void everyAlertTypeAndMarkerOnItsOwnLine() throws Exception {
		for(String type : new String[] {"NOTE", "TIP", "IMPORTANT", "WARNING", "CAUTION"}) {
			String html = render("> [!" + type + "]\n>\n> first\n>\n> second\n");
			assertTrue(type + ": " + html, html.contains("alert-" + type.toLowerCase()));
			assertTrue(html, html.contains("<p>first</p>") && html.contains("<p>second</p>"));
			assertFalse(html, html.contains("<p></p>"));
		}
	}

	@Test
	public void plainQuotesAndNestedQuotesAreKept() throws Exception {
		assertTrue(render("> just a quote").contains("<blockquote>"));
		String nested = render("> [!TIP]\n> outer\n>\n> > inner quote\n\nafter");
		assertTrue(nested, nested.contains("alert-tip") && nested.contains("<blockquote>") && nested.contains("inner quote"));
		assertTrue(nested, nested.indexOf("</div>") > nested.indexOf("inner quote"));
		assertTrue(nested.contains("<p>after</p>"));
		// unknown type is a normal quote
		assertTrue(render("> [!FOO]\n> x").contains("<blockquote>"));
		// two alerts in a row
		String two = render("> [!NOTE]\n> a\n\n> [!WARNING]\n> b\n");
		assertTrue(two, two.contains("alert-note") && two.contains("alert-warning"));
	}

	// ------------------------------------------------------------------ code colors

	@Test
	public void fencedCodeGetsColorTokens() throws Exception {
		String html = render("```java\nint x = 42; // note\nString s = \"a<b\";\n```\n");
		assertTrue(html, html.contains("<span class=\"tk-k\">int</span>"));
		assertTrue(html, html.contains("<span class=\"tk-n\">42</span>"));
		assertTrue(html, html.contains("<span class=\"tk-c\">// note</span>"));
		assertTrue(html, html.contains("<span class=\"tk-s\">&quot;a&lt;b&quot;</span>"));
		assertFalse("text must stay escaped", html.contains("a<b"));
	}

	@Test
	public void codeColorsKeepTheTextIntact() throws Exception {
		String code = "function f(a) {\n  return a + 'x' /* c */;\n}\n";
		String html = MarkdownExtras.highlightCode("<pre><code class=\"language-js\">" + MarkdownText.htmlEscape(code) + "</code></pre>");
		assertTrue(html.contains("tk-k"));
		String inner = html.substring(html.indexOf("\">") + 2, html.lastIndexOf("</code>"));
		String text = inner.replaceAll("<[^>]+>", "").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
		assertEquals(code, text);
	}

	@Test
	public void unknownLanguageAndPlainFenceStayPlain() throws Exception {
		String html = render("```klingon\nqapla\n```\n```\nplain int x\n```\n");
		assertFalse(html, html.contains("tk-"));
		assertNull(MarkdownExtras.syntaxStyle("klingon"));
		assertNotNull(MarkdownExtras.syntaxStyle("JS"));
		assertNotNull(MarkdownExtras.syntaxStyle("sql"));
	}

	@Test
	public void htmlAndXmlAndJsonAndSqlAreColored() throws Exception {
		assertTrue(render("```html\n<div class=\"a\">x</div>\n```").contains("tk-t"));
		assertTrue(render("```json\n{\"a\": 1, \"b\": true}\n```").contains("tk-"));
		assertTrue(render("```sql\nSELECT a FROM t WHERE x = 1;\n```").contains("tk-k"));
		assertTrue(render("```python\ndef f():\n    return None\n```").contains("tk-k"));
	}

	// ------------------------------------------------------------------ emoji

	@Test
	public void shortcodesBecomeCharactersOutsideCode() throws Exception {
		String rocket = new String(Character.toChars(0x1F680));
		assertEquals("Go " + rocket + " now", MarkdownExtras.emojify("Go :rocket: now"));
		assertEquals(rocket + rocket, MarkdownExtras.emojify(":rocket::rocket:"));
		String html = render("Launch :rocket: and `:rocket:` and\n\n```\n:rocket:\n```\n\n[l](http://x/:rocket:) <span title=\":rocket:\">t</span>");
		assertEquals(html, 1, html.split(rocket, -1).length - 1);
		assertTrue(html.contains("<code>:rocket:</code>"));
		assertTrue(html.contains("http://x/:rocket:"));
		assertTrue(html.contains("title=\":rocket:\""));
	}

	@Test
	public void unknownShortcodesAndTimesAreUntouched() {
		assertEquals("at 10:30:45 or :nothing: or ::", MarkdownExtras.emojify("at 10:30:45 or :nothing: or ::"));
		assertEquals("key:value", MarkdownExtras.emojify("key:value"));
		assertTrue(MarkdownExtras.emojiCount() > 80);
		assertEquals(new String(Character.toChars(0x1F44D)), MarkdownExtras.emoji("+1"));
	}

	// ------------------------------------------------------------------ front matter

	@Test
	public void frontMatterIsATableAndKeepsTheOffsets() throws Exception {
		String md = "---\ntitle: \"My page\"\ntags: [a, b]\nlist:\n  - one\n  - two\ndraft: true\n---\n# Heading\n\ntext\n";
		List<String[]> entries = MarkdownExtras.frontMatterEntries(md);
		assertEquals(4, entries.size());
		assertArrayEquals(new String[] {"title", "My page"}, entries.get(0));
		assertArrayEquals(new String[] {"tags", "a, b"}, entries.get(1));
		assertArrayEquals(new String[] {"list", "one, two"}, entries.get(2));
		assertArrayEquals(new String[] {"draft", "true"}, entries.get(3));

		String blank = MarkdownExtras.blankFrontMatter(md);
		assertEquals(md.length(), blank.length());
		assertTrue(blank.endsWith("# Heading\n\ntext\n"));
		assertEquals(md.indexOf("# Heading"), blank.indexOf("# Heading"));
		assertEquals(md.split("\n", -1).length, blank.split("\n", -1).length);

		String html = render(md);
		assertTrue(html, html.startsWith("<table class=\"frontmatter\""));
		assertTrue(html, html.contains("<th align=\"left\">title</th><td>My page</td>"));
		assertTrue(html, html.contains("Heading</h1>"));
		assertFalse("the --- lines must not become a rule or a heading", html.contains("<hr") || html.contains("<h2"));
	}

	@Test
	public void ruleSeparatedTextIsNotFrontMatter() throws Exception {
		String slides = "---\n# Slide 1\ntext\n---\n# Slide 2\nmore\n";
		assertNull(MarkdownExtras.frontMatter(slides));
		assertEquals(slides, MarkdownExtras.blankFrontMatter(slides));
		String html = render(slides);
		assertTrue(html, html.contains("Slide 1") && html.contains("Slide 2") && html.contains("text") && html.contains("more"));
		// a header-less block, prose between the rules, a block of only comments
		assertNull(MarkdownExtras.frontMatter("---\nJust a sentence here.\n---\nafter"));
		assertNull(MarkdownExtras.frontMatter("---\n# only a comment\n---\nafter"));
		assertNull(MarkdownExtras.frontMatter("---\n---\nafter"));
		assertTrue(render("---\nJust a sentence here.\n---\nafter").contains("Just a sentence here."));
		// real ones: dashes in lists, url values, quotes, comments, the "..." end
		assertNotNull(MarkdownExtras.frontMatter("---\n# c\ntitle: A: B\nurl: http://x.y/z\nitems:\n- a\n- b\nnested:\n  k: v\n---\nbody"));
		assertNotNull(MarkdownExtras.frontMatter("---\ntitle: x\n...\nbody"));
	}

	@Test
	public void noFrontMatter() throws Exception {
		assertNull(MarkdownExtras.frontMatter("# T\n---\nx\n"));
		assertEquals("# T\n---\nx\n", MarkdownExtras.blankFrontMatter("# T\n---\nx\n"));
		assertEquals("", MarkdownExtras.frontMatterHtml("text"));
		assertNull(MarkdownExtras.frontMatter("---\nnever closed\n"));
	}

	// ------------------------------------------------------------------ tasks

	@Test
	public void taskLinesSkipCodeAndCountProgress() {
		String md = "- [ ] one\n- [x] two\n  - [X] nested\n\n```\n- [ ] in code\n```\n\n1. [ ] numbered\n> - [ ] quoted\n- plain [ ] text\n";
		assertEquals(5, MarkdownExtras.taskLines(md).size());
		assertArrayEquals(new int[] {2, 5}, MarkdownExtras.taskProgress(md));
		assertArrayEquals(new int[] {0, 0}, MarkdownExtras.taskProgress("no tasks"));
	}

	@Test
	public void toggleChangesOnlyTheBox() {
		String md = "- [ ] one\r\n- [x] two\r\n```\r\n- [ ] code\r\n```\r\n* [ ] three";
		assertEquals("- [x] one\r\n- [x] two\r\n```\r\n- [ ] code\r\n```\r\n* [ ] three", MarkdownExtras.toggleTask(md, 0));
		assertEquals("- [ ] one\r\n- [ ] two\r\n```\r\n- [ ] code\r\n```\r\n* [ ] three", MarkdownExtras.toggleTask(md, 1));
		assertEquals("- [ ] one\r\n- [x] two\r\n```\r\n- [ ] code\r\n```\r\n* [x] three", MarkdownExtras.toggleTask(md, 2));
		assertNull(MarkdownExtras.toggleTask(md, 3));
		assertNull(MarkdownExtras.toggleTask(md, -1));
		assertEquals(md, MarkdownExtras.toggleTask(MarkdownExtras.toggleTask(md, 1), 1));
	}

	@Test
	public void taskLinksMatchTheTaskOrderOfTheSource() throws Exception {
		String md = "# T\n\n- [ ] one\n- [x] two\n\ntext\n\n- [ ] three\n";
		String preview = MarkdownExtras.taskLinks(render(md));
		assertFalse(preview, preview.contains("<input"));
		int a = preview.indexOf("mdv-task:0"), b = preview.indexOf("mdv-task:1"), c = preview.indexOf("mdv-task:2");
		assertTrue(preview, a > 0 && b > a && c > b);
		assertTrue(preview, preview.contains("mdv-task:0\">☐") && preview.contains("mdv-task:1\">☑") && preview.contains("mdv-task:2\">☐"));
		assertTrue("the exported html keeps real checkboxes", render(md).contains("<input"));
	}

	// ------------------------------------------------------------------ tables

	@Test
	public void spreadsheetCellsBecomeATable() {
		String table = MarkdownExtras.tabularToTable("Name\tQty\tNote\r\nApple\t3\ta|b\r\nPear\t10\t\r\n");
		assertNotNull(table);
		String[] lines = table.split("\n");
		assertEquals(4, lines.length);
		assertTrue(lines[0], lines[0].startsWith("| Name") && lines[0].contains("Qty"));
		assertTrue(lines[1], lines[1].matches("\\|( -+ \\|){3}"));
		assertTrue("pipe escaped: " + lines[2], lines[2].contains("a\\|b"));
		assertEquals(lines[0].length(), lines[2].length());
		assertTrue(MarkdownText.isDelimiterRow(lines[1]));
	}

	@Test
	public void otherTextIsNotATable() {
		assertNull(MarkdownExtras.tabularToTable("single\tline"));
		assertNull(MarkdownExtras.tabularToTable("a\tb\nc"));
		assertNull(MarkdownExtras.tabularToTable("a\tb\nc\td\te"));
		assertNull(MarkdownExtras.tabularToTable("plain text\nwithout tabs"));
		assertNull(MarkdownExtras.tabularToTable("only\none"));
		assertNull("tab indented lines are not cells", MarkdownExtras.tabularToTable("\tfoo\n\tbar"));
		assertNull(MarkdownExtras.tabularToTable("\tif(x)\n\t\treturn;\n\t}"));
		assertNotNull("an empty cell below the header is fine", MarkdownExtras.tabularToTable("a\tb\n\t2"));
		assertNull(MarkdownExtras.tabularToTable(null));
	}

	@Test
	public void columnAtCountsCells() {
		String row = "| a | `x|y` | c \\| d | e |";
		assertEquals(0, MarkdownExtras.columnAt(row, 3));
		assertEquals(1, MarkdownExtras.columnAt(row, 8));
		assertEquals(2, MarkdownExtras.columnAt(row, row.indexOf("c ")));
		assertEquals(3, MarkdownExtras.columnAt(row, row.length() - 3));
		assertEquals(3, MarkdownExtras.columnAt(row, 999));
		assertEquals(0, MarkdownExtras.columnAt("a | b", 0));
		assertEquals(1, MarkdownExtras.columnAt("a | b", 4));
	}

	@Test
	public void sortByNumbersAndText() {
		String[] rows = {"| Item | Price |", "| --- | ---: |", "| b | 10 |", "| a | 9,5 |", "| c |  |", "| d | 1.234,5 |"};
		String[] asc = MarkdownExtras.sortTable(rows, 1, true);
		assertTrue(asc[2], asc[2].contains("9,5"));
		assertTrue(asc[3], asc[3].contains("10 |"));
		assertTrue(asc[4], asc[4].contains("1.234,5"));
		assertTrue("empty last: " + asc[5], asc[5].trim().startsWith("| c"));
		String[] desc = MarkdownExtras.sortTable(rows, 1, false);
		assertTrue(desc[2], desc[2].contains("1.234,5"));
		assertTrue("empty stays last when descending too is not required, but the header stays", desc[0].contains("Item"));
		String[] byName = MarkdownExtras.sortTable(rows, 0, true);
		assertTrue(byName[2].contains("| a") && byName[3].contains("| b") && byName[4].contains("| c") && byName[5].contains("| d"));
		assertEquals(rows.length, byName.length);
		assertNull(MarkdownExtras.sortTable(new String[] {"| a |", "text"}, 0, true));
	}

	@Test
	public void sortIgnoresCaseAndKeepsAlignment() {
		String[] rows = {"| A | B |", "| :--- | ---: |", "| beta | 1 |", "| Alpha | 2 |", "| gamma | 3 |"};
		String[] sorted = MarkdownExtras.sortTable(rows, 0, true);
		assertTrue(sorted[2].contains("Alpha") && sorted[3].contains("beta") && sorted[4].contains("gamma"));
		assertTrue(sorted[1], sorted[1].contains(":--") && sorted[1].contains("-:"));
	}

	@Test
	public void richHtmlHasNoCssClassesLeft() throws Exception {
		String html = render("> [!WARNING]\n> be **careful**\n\n```java\nint x = 1;\n```\n\nplain");
		String rich = MarkdownExtras.richHtml(html);
		assertFalse(rich, rich.contains("class=\"tk-") || rich.contains("alert-warning") || rich.contains("<div"));
		assertTrue(rich, rich.contains("<font color=\"#cf222e\">int</font>"));
		assertTrue(rich, rich.contains("<table") && rich.contains("#fff6dd") && rich.contains("#9a6700") && rich.contains("Warning"));
		assertTrue(rich, rich.contains("be <strong>careful</strong>") && rich.contains("<p>plain</p>"));
		// nested div inside an alert (display math) keeps the box balanced
		String math = MarkdownExtras.richHtml("<div class=\"alert-note\"><p class=\"alert-title\">Note</p><div class=\"math\">x</div><p>t</p></div><p>after</p>");
		assertTrue(math, math.indexOf("</table>") > math.indexOf("<div class=\"math\">x</div><p>t</p>") && math.endsWith("<p>after</p>"));
		assertEquals("", MarkdownExtras.richHtml(null));
	}

	// ------------------------------------------------------------------ css

	@Test
	public void stylesExistForEveryClassTheHtmlUses() {
		String swing = String.join("\n", MarkdownExtras.swingAlertRules(true)) + String.join("\n", MarkdownExtras.swingTokenRules(false));
		for(String c : new String[] {"alert-note", "alert-tip", "alert-important", "alert-warning", "alert-caution", "tk-c", "tk-k", "tk-s", "tk-n", "tk-f", "tk-a", "tk-t"})
			assertTrue(c, swing.contains(c));
		String export = MarkdownExtras.exportAlertCss() + MarkdownExtras.exportTokenCss();
		assertTrue(export.contains("prefers-color-scheme:dark") && export.contains(".alert-caution") && export.contains(".tk-k"));
	}

	@Test
	public void parserSanityForTheTaskHtml() {
		// guards the assumption of taskLinks: flexmark writes the checkbox with this class
		com.vladsch.flexmark.util.options.MutableDataSet o = new com.vladsch.flexmark.util.options.MutableDataSet();
		o.set(Parser.EXTENSIONS, java.util.Arrays.asList(com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension.create()));
		String html = HtmlRenderer.builder(o).build().render(Parser.builder(o).build().parse("- [x] a\n- [ ] b\n"));
		assertTrue(html, html.contains("task-list-item-checkbox"));
	}
}
