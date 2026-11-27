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
package markdown.viewer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** LaTeX in, html 3.2 out: the approximation used by the Swing preview of the formulas. */
public class TexLiteTest {

	private static String html(String tex) {
		return TexLite.toHtml(tex);
	}

	@Test
	public void lettersAreItalicAndDigitsAreKept() {
		assertEquals("<i>x</i>2", html("x2"));
		assertEquals("", html(null));
		assertEquals("", html("   "));
	}

	@Test
	public void operatorsGetSpacesAndMinusIsATypographicOne() {
		assertEquals("<i>a</i> + <i>b</i> = 3", html("a+b=3"));
		assertEquals("<i>a</i>−<i>b</i>", html("a-b"));
		assertEquals("<i>a</i>&lt;<i>b</i>&gt;<i>c</i>", html("a<b>c"));
		assertEquals("<i>x</i>′", html("x'"));
		assertEquals("<i>a</i>∗<i>b</i>", html("a*b"));
	}

	@Test
	public void superscriptsAndSubscriptsTakeAGroupOrASingleAtom() {
		assertEquals("<i>x</i><sup>2</sup>", html("x^2"));
		assertEquals("<i>a</i><sub><i>i</i><i>j</i></sub>", html("a_{ij}"));
		assertEquals("<i>x</i><sub>1</sub><sup>2</sup>", html("x_1^2"));
		assertEquals("<i>e</i><sup><i>x</i><sup>2</sup></sup>", html("e^{x^2}"));
		assertEquals("<i>x</i><sup><i>n</i></sup>", html("x^ n"));
	}

	@Test
	public void greekLettersAndSymbolsBecomeUnicode() {
		assertEquals("α + β", html("\\alpha + \\beta"));
		assertEquals("ΓΩ", html("\\Gamma\\Omega"));
		assertEquals("∞", html("\\infty"));
		assertEquals("<i>a</i>≤<i>b</i>", html("a \\leq b"));
		assertEquals("∀∃∈∉", html("\\forall\\exists\\in\\notin"));
		assertEquals("→⇒", html("\\to\\Rightarrow"));
		assertEquals("…", html("\\ldots"));
	}

	@Test
	public void sumWithLimitsKeepsBothScripts() {
		assertEquals("∑<sub><i>i</i> = 1</sub><sup><i>n</i></sup>", html("\\sum_{i=1}^{n}"));
	}

	@Test
	public void functionNamesStayUpright() {
		assertEquals("sin<i>x</i>", html("\\sin x"));
		assertEquals("lim<sub><i>n</i>→∞</sub>", html("\\lim_{n\\to\\infty}"));
	}

	@Test
	public void fractionIsWrittenWithSlash() {
		assertEquals("(<i>a</i>)/(<i>b</i>)", html("\\frac{a}{b}"));
		assertEquals("(1)/(2)", html("\\dfrac12"));
		assertEquals("(<i>x</i> + 1)/(<i>y</i>)", html("\\frac{x+1}{y}"));
	}

	@Test
	public void squareRootsWithAndWithoutIndex() {
		assertEquals("√(<i>x</i>)", html("\\sqrt{x}"));
		assertEquals("<sup>3</sup>√(<i>x</i>)", html("\\sqrt[3]{x}"));
	}

	@Test
	public void textKeepsItsSpacesAndEscapesHtml() {
		assertEquals("if <i>x</i>", html("\\text{if } x"));
		assertEquals("a&lt;b", html("\\mathrm{a<b}"));
	}

	@Test
	public void fontCommands() {
		assertEquals("<b><i>v</i></b>", html("\\mathbf{v}"));
		assertEquals("<i><i>v</i></i>", html("\\mathit{v}"));
		assertEquals("ℝℕℤ", html("\\mathbb{R}\\mathbb{N}\\mathbb{Z}"));
		assertEquals("<i>X</i>", html("\\mathcal{X}"));
		assertEquals("A", html("\\mathbb{A}"));
	}

	@Test
	public void accentsAreCombiningCharacters() {
		assertEquals("<i>x</i>̅", html("\\bar{x}"));
		assertEquals("<i>x</i>̂", html("\\hat{x}"));
		assertEquals("<i>v</i>⃗", html("\\vec{v}"));
		assertEquals("<i>x</i>̇", html("\\dot{x}"));
		assertEquals("<i>x</i>̃", html("\\tilde{x}"));
	}

	@Test
	public void leftAndRightDelimitersAndStylesAreInvisible() {
		assertEquals("(<i>x</i>)", html("\\left( x \\right)"));
		assertEquals("<i>x</i>", html("\\left. x \\right."));
		assertEquals("<i>x</i>", html("\\displaystyle x"));
		assertEquals("[<i>x</i>]", html("\\bigl[ x \\bigr]"));
	}

	@Test
	public void matrixLikeEnvironmentsAreFlattenedWithLineBreaks() {
		assertEquals("<i>a</i> <i>b</i><br><i>c</i> <i>d</i>", html("\\begin{matrix} a & b \\\\ c & d \\end{matrix}"));
	}

	@Test
	public void escapedCharactersAreLiteral() {
		assertEquals("{}%$#_", html("\\{\\}\\%\\$\\#\\_"));
		assertEquals("&amp;", html("\\&"));
	}

	@Test
	public void unknownCommandsAreShownAsWritten() {
		assertEquals("\\foo", html("\\foo"));
		assertEquals("\\", html("\\"));
	}

	@Test
	public void brokenInputFallsBackToTheEscapedSource() {
		assertEquals("\\sqrt[3 &lt; x", html("\\sqrt[3 < x"));
	}
}
