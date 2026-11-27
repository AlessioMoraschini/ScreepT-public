/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package io.github.furstenheim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** HTML in, markdown out: every rule of the converter with its options, on small documents. */
public class CopyDownTest {

	private static String md(String html) {
		return new CopyDown().convert(html);
	}

	private static String md(String html, OptionsBuilder options) {
		return new CopyDown(options.build()).convert(html);
	}

	private static OptionsBuilder opts() {
		return OptionsBuilder.anOptions();
	}

	@Test
	public void headingsAreSetextForLevelOneAndTwoAndAtxBeyondByDefault() {
		assertEquals("Title\n=====", md("<h1>Title</h1>"));
		assertEquals("Sub\n---", md("<h2>Sub</h2>"));
		assertEquals("### Third", md("<h3>Third</h3>"));
		assertEquals("###### Sixth", md("<h6>Sixth</h6>"));
	}

	@Test
	public void atxHeadingStyleUsesHashesAtEveryLevel() {
		OptionsBuilder o = opts().withHeadingStyle(HeadingStyle.ATX);
		assertEquals("# One", md("<h1>One</h1>", o));
		assertEquals("## Two", md("<h2>Two</h2>", o));
	}

	@Test
	public void paragraphsAreSeparatedByBlankLine() {
		assertEquals("first\n\nsecond", md("<p>first</p><p>second</p>"));
	}

	@Test
	public void lineBreakUsesTheConfiguredBreakString() {
		assertEquals("a  \nb", md("<p>a<br>b</p>"));
		assertEquals("a\\\nb", md("<p>a<br>b</p>", opts().withBr("\\")));
	}

	@Test
	public void emphasisAndStrongHonourDelimiters() {
		assertEquals("a _b_ **c**", md("<p>a <em>b</em> <strong>c</strong></p>"));
		assertEquals("a _b_ **c**", md("<p>a <i>b</i> <b>c</b></p>"));
		assertEquals("a *b* __c__", md("<p>a <em>b</em> <strong>c</strong></p>", opts().withEmDelimiter("*").withStrongDelimiter("__")));
	}

	@Test
	public void emptyEmphasisDisappears() {
		assertEquals("", md("<p><strong></strong></p>"));
		assertEquals("ab", md("<p>a<em></em>b</p>"));
	}

	@Test
	public void bulletListUsesTheMarkerAndNestedListsAreIndented() {
		assertEquals("*   one\n*   two", md("<ul><li>one</li><li>two</li></ul>"));
		assertEquals("-   one\n-   two", md("<ul><li>one</li><li>two</li></ul>", opts().withBulletListMaker("-")));
		assertEquals("*   a\n    *   b\n*   c", md("<ul><li>a<ul><li>b</li></ul></li><li>c</li></ul>"));
	}

	@Test
	public void orderedListNumbersFromStartAttribute() {
		assertEquals("1.  x\n2.  y", md("<ol><li>x</li><li>y</li></ol>"));
		assertEquals("5.  x\n6.  y", md("<ol start=\"5\"><li>x</li><li>y</li></ol>"));
	}

	@Test
	public void orderedListWithBrokenStartFallsBackToOne() {
		assertEquals("1.  x", md("<ol start=\"abc\"><li>x</li></ol>"));
	}

	@Test
	public void blockquotePrefixesEveryLine() {
		assertEquals("> quoted", md("<blockquote>quoted</blockquote>"));
		assertEquals("> a\n> \n> b", md("<blockquote><p>a</p><p>b</p></blockquote>"));
	}

	@Test
	public void indentedCodeBlockByDefault() {
		assertEquals("    int a;\n    int b;", md("<pre><code>int a;\nint b;</code></pre>"));
	}

	@Test
	public void fencedCodeBlockKeepsLanguageAndGrowsFenceWhenCodeContainsFence() {
		OptionsBuilder o = opts().withCodeBlockStyle(CodeBlockStyle.FENCED);
		assertEquals("```java\nint a;\n```", md("<pre><code class=\"language-java\">int a;\n</code></pre>", o));
		assertEquals("```\nplain\n```", md("<pre><code>plain</code></pre>", o));
		String grown = md("<pre><code>```\ninner\n```</code></pre>", o);
		assertTrue(grown, grown.startsWith("````\n") && grown.endsWith("\n````"));
		assertEquals("~~~\nx\n~~~", md("<pre><code>x</code></pre>", opts().withCodeBlockStyle(CodeBlockStyle.FENCED).withFence("~~~")));
	}

	@Test
	public void inlineCodeAndBackticksInsideIt() {
		assertEquals("use `foo()` now", md("<p>use <code>foo()</code> now</p>"));
		assertEquals("a ``x`y`` b", md("<p>a <code>x`y</code> b</p>"));
		assertEquals("a `` `x`` b", md("<p>a <code>`x</code> b</p>"));
		assertEquals("a ``x` `` b", md("<p>a <code>x`</code> b</p>"));
		assertEquals("", md("<p><code> </code></p>"));
	}

	@Test
	public void horizontalRuleUsesOption() {
		assertEquals("a\n\n* * *\n\nb", md("<p>a</p><hr><p>b</p>"));
		assertEquals("---", md("<hr>", opts().withHr("---")));
	}

	@Test
	public void inlineLinksWithAndWithoutTitle() {
		assertEquals("[site](http://x.org)", md("<a href=\"http://x.org\">site</a>"));
		assertEquals("[site](http://x.org \"The Title\")", md("<a href=\"http://x.org\" title=\"The Title\">site</a>"));
		assertEquals("anchor without href stays text", md("<a name=\"n\">anchor without href stays text</a>"));
	}

	@Test
	public void referencedLinksCollectDefinitionsAtTheEnd() {
		String doc = "<p><a href=\"http://a\">A</a> and <a href=\"http://b\" title=\"tb\">B</a></p>";
		assertEquals("[A][1] and [B][2]\n\n[1]: http://a\n[2]: http://b \"tb\"",
				md(doc, opts().withLinkStyle(LinkStyle.REFERENCED)));
		assertEquals("[A][] and [B][]\n\n[A]: http://a\n[B]: http://b \"tb\"",
				md(doc, opts().withLinkStyle(LinkStyle.REFERENCED).withLinkReferenceStyle(LinkReferenceStyle.COLLAPSED)));
		assertEquals("[A] and [B]\n\n[A]: http://a\n[B]: http://b \"tb\"",
				md(doc, opts().withLinkStyle(LinkStyle.REFERENCED).withLinkReferenceStyle(LinkReferenceStyle.SHORTCUT)));
	}

	@Test
	public void referencesAreResetBetweenConversions() {
		CopyDown cd = new CopyDown(opts().withLinkStyle(LinkStyle.REFERENCED).build());
		cd.convert("<a href=\"http://a\">A</a>");
		assertEquals("[B][1]\n\n[1]: http://b", cd.convert("<a href=\"http://b\">B</a>"));
	}

	@Test
	public void imagesWithAltAndTitleAndWithoutSource() {
		assertEquals("![logo](a.png)", md("<img src=\"a.png\" alt=\"logo\">"));
		assertEquals("![logo](a.png \"T\")", md("<img src=\"a.png\" alt=\"logo\" title=\"T\">"));
		assertEquals("", md("<img alt=\"nothing\">"));
	}

	@Test
	public void specialCharactersInTextAreEscaped() {
		assertEquals("2 \\* 3 \\[x\\] snake\\_case", md("<p>2 * 3 [x] snake_case</p>"));
		assertEquals("\\- not a list", md("<p>- not a list</p>"));
		assertEquals("\\# not a title".substring(0, 0) + "\\# not a title", md("<p># not a title</p>"));
		assertEquals("1\\. not a list", md("<p>1. not a list</p>"));
		assertEquals("\\> not a quote", md("<p>&gt; not a quote</p>"));
	}

	@Test
	public void textInsideCodeIsNotEscaped() {
		assertEquals("`a_b*c`", md("<code>a_b*c</code>"));
	}

	@Test
	public void whitespaceIsCollapsedOutsidePreAndPreIsKept() {
		assertEquals("a b c", md("<p>a    b\n\n  c</p>"));
		assertEquals("    keep   spaces\n    and lines", md("<pre><code>keep   spaces\nand lines</code></pre>"));
	}

	@Test
	public void flankingSpacesMoveOutsideTheDelimiters() {
		assertEquals("a **b** c", md("<p>a<strong> b </strong>c</p>"));
	}

	@Test
	public void unknownBlockAndInlineElementsKeepTheirContent() {
		assertEquals("one\n\ntwo", md("<div>one</div><div>two</div>"));
		assertEquals("a b c", md("<p>a <span>b</span> c</p>"));
	}

	@Test
	public void blankBlockProducesNothingAndEmptyInputIsEmpty() {
		assertEquals("", md(""));
		assertEquals("", md("<p>   </p>"));
		assertEquals("x", md("<p>x</p><p> </p>"));
	}

	@Test
	public void commentsAreDropped() {
		assertEquals("a b", md("<p>a<!-- hidden --> b</p>"));
	}

	@Test
	public void realisticPageRoundTripsToReadableMarkdown() {
		String html = "<h1>Guide</h1><p>Intro with <em>style</em> and <a href=\"u\">link</a>.</p>"
				+ "<h2>Steps</h2><ol><li>Open</li><li>Close</li></ol><pre><code class=\"language-sh\">run.sh\n</code></pre>";
		String out = md(html, opts().withCodeBlockStyle(CodeBlockStyle.FENCED));
		assertEquals("Guide\n=====\n\nIntro with _style_ and [link](u).\n\nSteps\n-----\n\n1.  Open\n2.  Close\n\n```sh\nrun.sh\n```", out);
	}
}
