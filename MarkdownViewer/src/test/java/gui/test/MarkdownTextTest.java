package markdown.viewer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * The text logic of the Markdown editor: headings and table of contents, list renumbering, table formatting, math
 * extraction. Everything works on plain strings, so every case is a document in, a document out.
 */
public class MarkdownTextTest {

	// ------------------------------------------------------------------ headings and anchors

	@Test
	public void headingsKeepLevelTextAndLineAndSkipCodeBlocks() {
		String doc = "# Title\n\nintro\n\n## Section A\n```\n# not a heading\n```\n### Deep one ###\n#not a heading either\n####### seven\n## \n";
		List<MarkdownText.HeadingRef> h = MarkdownText.headings(doc);
		assertEquals(4, h.size());
		assertEquals(1, h.get(0).level);
		assertEquals("Title", h.get(0).text);
		assertEquals(0, h.get(0).line);
		assertEquals("Section A", h.get(1).text);
		assertEquals(4, h.get(1).line);
		assertEquals("closing hashes are not part of the text", "Deep one", h.get(2).text);
		assertEquals(3, h.get(2).level);
		assertEquals("an empty heading is still a heading", "", h.get(3).text);
	}

	@Test
	public void headingsInsideFrontMatterAreIgnored() {
		String doc = "---\ntitle: x\n# comment in yaml\n---\n# Real\n";
		List<MarkdownText.HeadingRef> h = MarkdownText.headings(doc);
		assertEquals(1, h.size());
		assertEquals("Real", h.get(0).text);
	}

	@Test
	public void repeatedHeadingsGetUniqueAnchorsLikeGitHub() {
		List<MarkdownText.HeadingRef> h = MarkdownText.headings("# Intro\n## Intro\n## Intro\n# Intro-1\n");
		assertEquals("intro", h.get(0).slug);
		assertEquals("intro-1", h.get(1).slug);
		assertEquals("intro-2", h.get(2).slug);
		assertEquals("a heading that already looks like a numbered one does not collide", "intro-1-1", h.get(3).slug);
	}

	@Test
	public void slugKeepsLettersDigitsUnderscoreAndTurnsSpacesIntoDashes() {
		assertEquals("hello-world", MarkdownText.slug("Hello World"));
		assertEquals("c-and-java-8_beta", MarkdownText.slug("C++ and Java 8_Beta!"));
		assertEquals("già-è-così", MarkdownText.slug("Già è così"));
		assertEquals("a--b", MarkdownText.slug("a - b").replace("---", "--"));
		assertEquals("", MarkdownText.slug("!?"));
	}

	@Test
	public void plainTextStripsInlineMarkup() {
		assertEquals("bold and italic and code", MarkdownText.plainText("**bold** and *italic* and `code`"));
		assertEquals("a link and an image", MarkdownText.plainText("[a link](http://x.y) and ![an image](i.png)"));
		assertEquals("ref link", MarkdownText.plainText("[ref link][1]"));
		assertEquals("html tag", MarkdownText.plainText("<b>html</b> tag"));
		assertEquals("struck", MarkdownText.plainText("~~struck~~"));
		assertEquals("snake_case_name stays", MarkdownText.plainText("snake_case_name stays"));
	}

	@Test
	public void tocIsAnIndentedListOfLinksStartingAtTheShallowestLevel() {
		String doc = "## Alpha\ntext\n### Beta\n## Gamma <!-- omit in toc -->\n### [Link](http://x) title\n";
		String toc = MarkdownText.buildToc(doc, "\n");
		assertEquals("- [Alpha](#alpha)\n  - [Beta](#beta)\n  - [Link title](#link-title)\n", toc);
		assertEquals("same with Windows line ends", toc.replace("\n", "\r\n"), MarkdownText.buildToc(doc, "\r\n"));
		assertEquals("", MarkdownText.buildToc("no headings here", "\n"));
	}

	@Test
	public void tocRangeFindsTheGeneratedBlockBetweenItsMarkers() {
		String doc = "# T\n<!-- TOC -->\n- [T](#t)\n<!-- /TOC -->\nbody";
		int[] range = MarkdownText.tocRange(doc);
		assertNotNull(range);
		assertEquals("- [T](#t)\n", doc.substring(range[0], range[1]));
		assertNull(MarkdownText.tocRange("# T\n<!-- TOC -->\nno end"));
		assertNull(MarkdownText.tocRange("# T only"));
	}

	@Test
	public void headingLevelsMoveWithinOneToSix() {
		assertEquals("## Title", MarkdownText.changeHeadingLevel("# Title", 1));
		assertEquals("# Title", MarkdownText.changeHeadingLevel("## Title", -1));
		assertEquals("Title", MarkdownText.changeHeadingLevel("# Title", -1));
		assertEquals("###### Title", MarkdownText.changeHeadingLevel("###### Title", 3));
		assertEquals("# plain text becomes a heading", MarkdownText.changeHeadingLevel("plain text becomes a heading", 1));
		assertEquals("plain", MarkdownText.changeHeadingLevel("plain", -1));
		assertEquals("   ## Indented", MarkdownText.changeHeadingLevel("   # Indented", 1));
	}

	@Test
	public void fenceDetectionFollowsTheOpeningAndClosingMarkers() {
		String[] lines = { "text", "```java", "code", "```", "after", "~~~", "tilde", "~~~", "end" };
		assertFalse(MarkdownText.isInsideFence(lines, 0));
		assertTrue(MarkdownText.isInsideFence(lines, 1));
		assertTrue(MarkdownText.isInsideFence(lines, 2));
		assertTrue("the closing line belongs to the block", MarkdownText.isInsideFence(lines, 3));
		assertFalse(MarkdownText.isInsideFence(lines, 4));
		assertTrue(MarkdownText.isInsideFence(lines, 6));
		assertFalse(MarkdownText.isInsideFence(lines, 8));
	}

	@Test
	public void lineEndsAreDetectedAndStripped() {
		assertEquals("\r\n", MarkdownText.eol("a\r\nb"));
		assertEquals("\n", MarkdownText.eol("a\nb"));
		assertEquals("\n", MarkdownText.eol("no newline"));
		assertEquals("abc", MarkdownText.stripCr("abc\r"));
		assertEquals("abc", MarkdownText.stripCr("abc"));
		assertArrayEquals(new String[] { "a", "", "b" }, MarkdownText.lines("a\n\nb"));
		assertArrayEquals(new String[] { "" }, MarkdownText.lines(null));
		assertEquals(4, MarkdownText.leadingWidth("\tx"));
		assertEquals("a tab goes to the next multiple of 4", 4, MarkdownText.leadingWidth("  \tx"));
		assertEquals("a&amp;b &lt;c&gt; &quot;d&quot;", MarkdownText.htmlEscape("a&b <c> \"d\""));
		assertEquals("-----", MarkdownText.repeat('-', 5));
	}

	// ------------------------------------------------------------------ lists

	@Test
	public void listPrefixesTellMarkerNumberTaskAndWhatTheNextLineStartsWith() {
		MarkdownText.ListPrefix bullet = MarkdownText.listPrefix("  - item");
		assertEquals("  ", bullet.indent);
		assertEquals("-", bullet.marker);
		assertFalse(bullet.isOrdered());
		assertEquals("  - ", bullet.next());

		MarkdownText.ListPrefix numbered = MarkdownText.listPrefix("9. nine");
		assertTrue(numbered.isOrdered());
		assertEquals(9, numbered.number);
		assertEquals("10. ", numbered.next());
		assertEquals(3, numbered.contentOffset());

		MarkdownText.ListPrefix task = MarkdownText.listPrefix("- [x] done");
		assertEquals("[x] ", task.task);
		assertEquals("a new task starts unchecked", "- [ ] ", task.next());

		MarkdownText.ListPrefix quote = MarkdownText.listPrefix("> quoted");
		assertTrue(quote.isQuote());
		assertEquals("> ", quote.next());
		assertEquals("a bare marker still continues the list", "-", MarkdownText.listPrefix("-").marker);
		assertNull(MarkdownText.listPrefix("plain text"));
		assertNull("a dash glued to the word is not a list", MarkdownText.listPrefix("-word"));
	}

	@Test
	public void orderedListsAreRenumberedPerLevelAndBulletsLeftAlone() {
		String[] in = { "1. a", "1. b", "7. c", "   1. x", "   5. y", "2. d", "", "text", "3. new list", "- bullet", "5. after bullet" };
		String[] out = MarkdownText.renumber(in);
		assertArrayEquals(new String[] { "1. a", "2. b", "3. c", "   1. x", "   2. y", "4. d", "", "text", "3. new list", "- bullet", "5. after bullet" }, out);
		assertEquals("the input is not touched", "7. c", in[2]);
	}

	@Test
	public void renumberKeepsWindowsLineEnds() {
		String[] out = MarkdownText.renumber(new String[] { "1. a\r", "5. b\r" });
		assertArrayEquals(new String[] { "1. a\r", "2. b\r" }, out);
	}

	// ------------------------------------------------------------------ tables

	@Test
	public void tableLinesAreRecognised() {
		assertTrue(MarkdownText.isTableLine("| a | b |"));
		assertTrue(MarkdownText.isTableLine("a | b"));
		assertFalse(MarkdownText.isTableLine("no pipes"));
		assertFalse(MarkdownText.isTableLine("|"));
		assertTrue(MarkdownText.isDelimiterRow("|---|:--:|---:|"));
		assertTrue(MarkdownText.isDelimiterRow("--- | ---"));
		assertFalse(MarkdownText.isDelimiterRow("| a | b |"));
	}

	@Test
	public void indentedCodeBlockLinesAreNotTables() {
		assertFalse(MarkdownText.isTableLine("    | code block |"));
	}

	@Test
	public void rowsSplitOnPipesExceptEscapedOnesAndThoseInCode() {
		assertEquals(Arrays.asList("a", "b", "c"), MarkdownText.splitRow("| a | b | c |"));
		assertEquals(Arrays.asList("a", "b"), MarkdownText.splitRow("a | b"));
		assertEquals(Arrays.asList("x \\| y", "z"), MarkdownText.splitRow("| x \\| y | z |"));
		assertEquals(Arrays.asList("`a | b`", "c"), MarkdownText.splitRow("| `a | b` | c |"));
		assertEquals(Arrays.asList("a", "", "c"), MarkdownText.splitRow("| a | | c |"));
	}

	@Test
	public void tablesAreAlignedAccordingToTheirDelimiterRow() {
		String[] rows = { "|Name|Qty|Note|", "|:--|--:|:-:|", "|apple|3|ok|", "|kiwi fruit|12|x|" };
		String[] out = MarkdownText.formatTable(rows);
		assertNotNull(out);
		assertEquals("| Name       |  Qty | Note  |", out[0]);
		assertEquals("| :--------- | ---: | :---: |", out[1]);
		assertEquals("| apple      |    3 |  ok   |", out[2]);
		assertEquals("| kiwi fruit |   12 |   x   |", out[3]);
		for (String r : out) {
			assertEquals("every row has the same width", out[0].length(), r.length());
		}
	}

	@Test
	public void tableFormattingWorksWithWideCharactersAndMissingCells() {
		assertEquals(2, MarkdownText.displayWidth("ab"));
		assertEquals("a CJK character takes two columns", 4, MarkdownText.displayWidth("日本"));
		assertEquals("combining marks take none", 1, MarkdownText.displayWidth("é"));
		String[] out = MarkdownText.formatTable(new String[] { "|a|b|", "|-|-|", "|日本|" });
		assertEquals("aligned by display width, not by character count", MarkdownText.displayWidth(out[0]), MarkdownText.displayWidth(out[2]));
		assertTrue(out[2].contains("日本"));
		assertEquals("the missing cell exists as an empty one", 2, MarkdownText.splitRow(out[2]).size());
		assertEquals("", MarkdownText.splitRow(out[2]).get(1));
	}

	@Test
	public void notATableGivesNull() {
		assertNull(MarkdownText.formatTable(new String[] { "| a | b |" }));
		assertNull(MarkdownText.formatTable(new String[] { "| a | b |", "| c | d |" }));
	}

	@Test
	public void tableFormattingKeepsIndentAndLineEnds() {
		String[] out = MarkdownText.formatTable(new String[] { "  |a|b|\r", "  |-|-|\r", "  |1|2|\r" });
		for (String row : out) {
			assertTrue(row, row.startsWith("  |"));
			assertTrue(row, row.endsWith("|\r"));
		}
	}

	// ------------------------------------------------------------------ math

	@Test
	public void inlineAndDisplayFormulasAreLiftedOutAndPutBackRendered() {
		String doc = "Energy $E=mc^2$ is famous.\n\n$$\n\\int_0^1 x\\,dx\n$$\n\nPrice is $5 and $6.\n```\n$not math$\n```\n";
		MarkdownText.MathExtraction math = MarkdownText.extractMath(doc);
		assertFalse(math.isEmpty());
		assertEquals(Arrays.asList("E=mc^2", "\\int_0^1 x\\,dx"), math.formulas);
		assertEquals(Arrays.asList(Boolean.FALSE, Boolean.TRUE), math.display);
		assertFalse("formulas are gone from the markdown", math.markdown.contains("E=mc"));
		assertTrue("money amounts are not formulas", math.markdown.contains("Price is $5 and $6."));
		assertTrue("code stays code", math.markdown.contains("$not math$"));

		String html = "<p>" + math.markdown.split("\n")[0] + "</p>";
		String restored = MarkdownText.restoreMath(html, math, new MarkdownText.MathRenderer() {
			@Override
			public String render(String tex, boolean display) {
				return (display ? "[D:" : "[I:") + tex + "]";
			}
		});
		assertEquals("<p>Energy [I:E=mc^2] is famous.</p>", restored);
	}

	@Test
	public void singleLineDisplayFormulaAndEscapedDollars() {
		MarkdownText.MathExtraction math = MarkdownText.extractMath("$$ a+b $$\ncost \\$5 and `$x$` stay\n");
		assertEquals(Arrays.asList("a+b"), math.formulas);
		assertEquals(Arrays.asList(Boolean.TRUE), math.display);
		assertTrue(math.markdown.contains("cost \\$5 and `$x$` stay"));
		assertTrue(MarkdownText.extractMath("no dollars").isEmpty());
		assertEquals("", MarkdownText.extractMath(null).markdown);
		assertEquals("untouched html when there is no math", "<p>x</p>",
				MarkdownText.restoreMath("<p>x</p>", MarkdownText.extractMath("plain"), null));
	}

	@Test
	public void unclosedDisplayBlockIsKeptAsWrittenNotLost() {
		MarkdownText.MathExtraction math = MarkdownText.extractMath("before\n$$\nx + y\nnever closed");
		assertTrue(math.markdown.contains("never closed"));
		assertTrue(math.formulas.isEmpty());
	}
}
