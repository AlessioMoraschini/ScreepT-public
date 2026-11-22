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
package various.common.light.utility.string;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.utility.string.StringWorker.EOL;

/**
 * Pure logic of {@link StringWorker}: null/empty/unicode handling, boundaries, EOL conversions, version compare.
 */
public class StringWorkerLogicTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String EOLS = EOL.defaultEol.eol;

	// ---------------------------------------------------------------- EOL

	@Test
	public void eolValuesAndRegexesAreConsistent() {
		assertEquals("\r", EOL.CR.eol);
		assertEquals("\n", EOL.LF.eol);
		assertEquals("\r\n", EOL.CRLF.eol);
		for (EOL e : EOL.values()) {
			assertTrue("regex of " + e, e.eol.matches(e.eolRegex));
		}
	}

	@Test
	public void osBasedEolMatchesOsName() {
		boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
		assertEquals(win ? EOL.CRLF : EOL.LF, EOL.getOsBasedEOL());
	}

	@Test
	public void unicodeEolConstantsAreTheRealCharacters() {
		assertEquals('\r', EOL.UNICODE_CR);
		assertEquals('\n', EOL.UNICODE_LF);
	}

	@Test
	public void crExclusiveRegexSkipsCrlf() {
		assertEquals("aXb\r\nc", "a\rb\r\nc".replaceAll(EOL.CR_EXCLUSIVE_REGEX, "X"));
	}

	@Test
	public void lfExclusiveRegexSkipsCrlf() {
		assertEquals("a\r\nbXc", "a\r\nb\nc".replaceAll(EOL.LF_EXCLUSIVE_REGEX, "X"));
	}

	// ---------------------------------------------------------------- case

	@Test
	public void isStringUpperCase() {
		assertFalse(StringWorker.isStringUpperCase(null));
		assertFalse(StringWorker.isStringUpperCase(""));
		assertFalse(StringWorker.isStringUpperCase("   "));
		assertTrue(StringWorker.isStringUpperCase("ABC"));
		assertTrue(StringWorker.isStringUpperCase("ABC 123 !?"));
		assertFalse(StringWorker.isStringUpperCase("AbC"));
		assertTrue(StringWorker.isStringUpperCase("ÀÉ"));
		assertFalse(StringWorker.isStringUpperCase("Àé"));
	}

	@Test
	public void isStringUpperCaseWithoutLettersIsTrue_legacy() {
		assertTrue(StringWorker.isStringUpperCase("123"));
	}

	@Test
	public void upperAfterDotsCapitalizesSentences() {
		assertEquals("Hello. World! Foo", StringWorker.upperAfterDots("hello. world! foo", '\u0000'));
		assertEquals("Hello\nWorld", StringWorker.upperAfterDots("hello\nworld", '\n'));
		assertEquals("", StringWorker.upperAfterDots("", '.'));
	}

	@Test
	public void lowerAfterDotsLowersFirstLetterOfSentences() {
		assertEquals("hELLO. wORLD", StringWorker.lowerAfterDots("HELLO. WORLD", '\u0000'));
		assertEquals("", StringWorker.lowerAfterDots("", '.'));
	}

	@Test
	public void uppercaseEveryFirstLetterNoSpace() {
		assertEquals("Hello Big World", StringWorker.uppercaseEveryFirstLetterNoSpace("hello   big world"));
		assertEquals("", StringWorker.uppercaseEveryFirstLetterNoSpace(""));
		assertEquals("", StringWorker.uppercaseEveryFirstLetterNoSpace("   "));
		assertEquals("Élan Vital", StringWorker.uppercaseEveryFirstLetterNoSpace("élan vital"));
	}

	@Test
	public void lowerCaseEveryFirstLetterNoSpace() {
		assertEquals("hello bIG wORLD", StringWorker.lowerCaseEveryFirstLetterNoSpace("Hello  BIG   WORLD"));
		assertEquals("", StringWorker.lowerCaseEveryFirstLetterNoSpace(""));
	}

	@Test
	public void upperAndLowerArrays() {
		assertArrayEquals(new String[] { "AB", "É" }, StringWorker.getUpperCaseArray(new String[] { "aB", "é" }));
		assertArrayEquals(new String[] { "ab", "é" }, StringWorker.getLowerCaseArray(new String[] { "Ab", "É" }));
		assertEquals(0, StringWorker.getUpperCaseArray(new String[0]).length);
	}

	// ---------------------------------------------------------------- trimming / length

	@Test
	public void removeLastChar() {
		assertNull(StringWorker.removeLastChar(null));
		assertEquals("", StringWorker.removeLastChar(""));
		assertEquals("", StringWorker.removeLastChar("a"));
		assertEquals("ab", StringWorker.removeLastChar("abc"));
	}

	@Test
	public void fitToMaxLengthKeepsShortStringsUntouched() {
		assertEquals("short.txt", StringWorker.fitToMaxLentgh("short.txt", 20));
		assertEquals("12345678901234567890", StringWorker.fitToMaxLentgh("12345678901234567890", 20));
	}

	@Test
	public void fitToMaxLengthNeverExceedsTheLimitAndKeepsBothEnds() {
		String src = "a_very_long_file_name_with_extension.properties";
		for (int lim = 10; lim < src.length(); lim++) {
			String res = StringWorker.fitToMaxLentgh(src, lim);
			assertTrue("lim " + lim + " -> '" + res + "'", res.length() <= lim);
			assertTrue(res.contains(".."));
			assertTrue(res.startsWith(src.substring(0, 2)));
			assertTrue(res.endsWith("s"));
		}
	}

	@Test
	public void forceLengthDeterministic() {
		assertEquals("---abc", StringWorker.forceLengthDeterministic("abc", 6, "-"));
		assertEquals("abc", StringWorker.forceLengthDeterministic("abcdefgh", 3, "-"));
		assertEquals("abc", StringWorker.forceLengthDeterministic("abc", 3, "-"));
	}

	@Test
	public void extractSameLengthByteArray() throws Exception {
		assertNull(StringWorker.extractSameLengthByteArray(null));
		assertEquals(0, StringWorker.extractSameLengthByteArray("").length);
		assertArrayEquals(new byte[] { 97, 98, 99 }, StringWorker.extractSameLengthByteArray("abc"));
		byte[] uni = StringWorker.extractSameLengthByteArray("é中");
		assertEquals(2, uni.length);
		assertEquals((byte) 0xC3, uni[0]);
		assertEquals((byte) 0xA9, uni[1]);
	}

	@Test
	public void countOccurrencies() {
		assertEquals(0, StringWorker.countOccurrencies(null, "a"));
		assertEquals(0, StringWorker.countOccurrencies("aaa", null));
		assertEquals(0, StringWorker.countOccurrencies("aaa", ""));
		assertEquals(0, StringWorker.countOccurrencies("", "a"));
		assertEquals(3, StringWorker.countOccurrencies("a-b-c-d", "-"));
		assertEquals("non overlapping", 2, StringWorker.countOccurrencies("aaaa", "aa"));
		assertEquals(1, StringWorker.countOccurrencies("xéy", "é"));
	}

	@Test
	public void countOccurrenciesOfBlankMatchIsZero_legacy() {
		// a match made only of whitespace is considered "empty", so spaces can not be counted
		assertEquals(0, StringWorker.countOccurrencies("a b c", " "));
	}

	@Test
	public void retrieveFromNelements() {
		assertEquals("cd", StringWorker.retrieveFromNelements("a,b,c,d", ",", 2));
		assertEquals("abcd", StringWorker.retrieveFromNelements("a,b,c,d", ",", 0));
		assertEquals("", StringWorker.retrieveFromNelements("a,b", ",", 5));
		assertNull(StringWorker.retrieveFromNelements(null, ",", 1));
		assertEquals("bc", StringWorker.retrieveFromNelements("a,b,c,d", ",", 1, 2));
		assertEquals("a", StringWorker.retrieveFromNelements("a,b,c,d", ",", 0, 0));
		assertEquals("", StringWorker.retrieveFromNelements("a,b,c,d", ",", 3, 1));
		assertNull(StringWorker.retrieveFromNelements(null, ",", 1, 2));
	}

	// ---------------------------------------------------------------- line / space removal

	@Test
	public void removeSpacesAndLinesRemovesEveryLineBreak() {
		assertEquals("ab", StringWorker.removeSpacesAndLines("a b"));
		assertEquals("abc", StringWorker.removeSpacesAndLines("a b" + EOLS + "c"));
		assertEquals("abc", StringWorker.removeSpacesAndLines("a b\nc"));
		assertEquals("abc", StringWorker.removeSpacesAndLines("a b\r\nc"));
		assertEquals("", StringWorker.removeSpacesAndLines(""));
	}

	@Test
	public void removeLinesRemovesEveryLineBreak() {
		assertEquals("a bc", StringWorker.removeLines("a b" + EOLS + "c"));
		assertEquals("a bc", StringWorker.removeLines("a b\nc"));
		assertEquals("a bc", StringWorker.removeLines("a b\r\nc"));
		assertEquals("", StringWorker.removeLines(""));
	}

	@Test
	public void removeBlankLines() {
		assertNull(StringWorker.removeBlankLines(null, "\n"));
		assertEquals("a", StringWorker.removeBlankLines("a", null));
		assertEquals("a\nb", StringWorker.removeBlankLines("a\n\nb", "\n"));
		assertEquals("a\nb", StringWorker.removeBlankLines("a\n   \nb", "\n"));
		assertEquals("a\r\nb", StringWorker.removeBlankLines("a\r\n\r\n\r\nb", "\r\n"));
		assertEquals("", StringWorker.removeBlankLines("", "\n"));
	}

	@Test
	public void getNTimesString() {
		assertEquals("ababab", StringWorker.getNTimesString("ab", 3));
		assertEquals("", StringWorker.getNTimesString("ab", 0));
		assertEquals("ab", StringWorker.getNTimesString("ab", -1));
		assertEquals("", StringWorker.getNTimesString("", 5));
		assertNull(StringWorker.getNTimesString(null, 5));
		assertEquals("   ", StringWorker.getNTimesString(" ", 3));
	}

	@Test
	public void repeatStringNTimes() {
		assertEquals("", StringWorker.repeatStringNTimes("x", 0, false));
		assertEquals("xxx", StringWorker.repeatStringNTimes("x", 3, false));
		assertEquals("x" + EOLS + "x" + EOLS + "x", StringWorker.repeatStringNTimes("x", 3, true));
		assertEquals("", StringWorker.repeatStringNTimes("x", -2, true));
	}

	// ---------------------------------------------------------------- tabs

	@Test
	public void replaceTabsWithSpaces() {
		assertEquals("    a    b", StringWorker.replaceTabsWithSpaces("\ta\tb", 4));
		assertEquals("ab", StringWorker.replaceTabsWithSpaces("a\tb", 0));
		assertEquals("no tabs", StringWorker.replaceTabsWithSpaces("no tabs", 4));
		assertEquals("", StringWorker.replaceTabsWithSpaces("", 4));
	}

	@Test
	public void replaceSpacesWithTabsConvertsOnlyFullTabSizedRuns() {
		assertEquals("\ta" + EOLS, StringWorker.replaceSpacesWithTabs("    a", 4));
		assertEquals("\t\tx" + EOLS, StringWorker.replaceSpacesWithTabs("        x", 4));
		assertEquals("a\tb" + EOLS, StringWorker.replaceSpacesWithTabs("a    b", 4));
		assertEquals("   a" + EOLS, StringWorker.replaceSpacesWithTabs("   a", 4));
		assertEquals("a   " + EOLS, StringWorker.replaceSpacesWithTabs("a   ", 4));
		assertEquals("\ta" + EOLS + "\tb" + EOLS,
				StringWorker.replaceSpacesWithTabs("    a" + EOLS + "    b", 4));
	}

	@Test
	public void spacesTabsRoundTripAddsATrailingTerminator_legacy() {
		String text = "    one" + EOLS + "        two" + EOLS + "three";
		String tabbed = StringWorker.replaceSpacesWithTabs(text, 4);
		String back = StringWorker.replaceTabsWithSpaces(tabbed, 4);
		assertEquals(text + EOLS, back);
	}

	@Test
	public void replaceSpacesWithTabsConvertsLfOnlyTextAsReturnedByASwingTextArea() {
		// JTextArea.getText() always uses LF, whatever the platform default is
		String converted = StringWorker.replaceSpacesWithTabs("    a\n    b\n        c", 4);
		assertTrue(converted, converted.startsWith("\ta\n\tb\n\t\tc"));
	}

	@Test
	@Ignore("BUG: replaceSpacesWithTabs appends EOL.defaultEol after the last line even if the text did not end with one: every use adds a line")
	public void replaceSpacesWithTabsKeepsTheTextEnding() {
		assertEquals("\ta\n\tb", StringWorker.replaceSpacesWithTabs("    a\n    b", 4));
		assertEquals("\ta", StringWorker.replaceSpacesWithTabs("    a", 4));
	}

	// ---------------------------------------------------------------- inject selection

	@Test
	public void injectStringInSelection() {
		assertEquals("hXYlo", StringWorker.injectStringInSelection("hello", "XY", 1, 3));
		assertEquals("XYhello", StringWorker.injectStringInSelection("hello", "XY", 0, 0));
		assertEquals("helloXY", StringWorker.injectStringInSelection("hello", "XY", 5, 5));
		assertEquals("XY", StringWorker.injectStringInSelection("hello", "XY", 0, 5));
		assertEquals("hello", StringWorker.injectStringInSelection("hello", "", 2, 2));
		assertEquals("heo", StringWorker.injectStringInSelection("hello", "", 2, 4));
	}

	@Test
	public void injectStringInSelectionClampsInvalidBounds() {
		assertEquals("Xhello", StringWorker.injectStringInSelection("hello", "X", -5, 0));
		assertEquals("heXllo", StringWorker.injectStringInSelection("hello", "X", 4, 2));
		assertEquals("heX", StringWorker.injectStringInSelection("hello", "X", 2, 99));
		assertEquals("helloX", StringWorker.injectStringInSelection("hello", "X", 10, 20));
		assertEquals("X", StringWorker.injectStringInSelection("", "X", 3, 9));
	}

	@Test
	public void injectStringInSelectionWithBothBoundsNegative() {
		assertEquals("Xhello", StringWorker.injectStringInSelection("hello", "X", -1, -1));
	}

	// ---------------------------------------------------------------- collections to string

	@Test
	public void stringValOfArraysAndMaps() {
		assertEquals("Val[0] : a" + EOLS + "Val[1] : b" + EOLS, StringWorker.stringVal(new String[] { "a", "b" }));
		assertEquals("Val[0] : 1" + EOLS + "Val[1] : 2" + EOLS, StringWorker.stringVal(new int[] { 1, 2 }));
		assertEquals("", StringWorker.stringVal(new int[0]));
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("k1", 1);
		map.put("k2", null);
		String expected = "Key : k1 <=> Value : 1" + EOLS + "Key : k2 <=> Value : null" + EOLS;
		assertEquals(expected, StringWorker.stringVal(map));
		assertEquals(expected, StringWorker.getMapToString(map));
		assertEquals("", StringWorker.getMapToString(new LinkedHashMap<String, String>()));
	}

	@Test
	public void strArrayIsNullSafe() {
		assertEquals("", StringWorker.strArray((String[]) null));
		assertEquals("Val[0] : aVal[1] : b", StringWorker.strArray(new String[] { "a", "b" }));
	}

	@Test
	public void concatSkipsNullsAndUsesSeparator() {
		assertEquals("", StringWorker.concat(null, EOL.LF));
		assertEquals("", StringWorker.concat(Collections.emptyList(), EOL.LF));
		assertEquals("a\nb\nc", StringWorker.concat(Arrays.asList("a", "b", "c"), EOL.LF));
		assertEquals("1\r\n2", StringWorker.concat(Arrays.asList(1, 2), EOL.CRLF));
		assertEquals("a\nb", StringWorker.concat(Arrays.asList("a", null, "b"), EOL.LF));
		assertEquals("a", StringWorker.concat(Collections.singletonList("a"), EOL.CR));
	}

	@Test
	public void concatFilePathsUsesCanonicalPathsAndDefaultSeparator() throws IOException {
		File a = tmp.newFile("with space.txt");
		File b = tmp.newFile("b.txt");
		List<File> files = new ArrayList<>(Arrays.asList(a, null, b));
		String res = StringWorker.concatFilePaths(files, EOL.LF);
		assertEquals(a.getCanonicalPath() + "\n" + b.getCanonicalPath(), res);
		assertEquals(a.getCanonicalPath() + EOLS + b.getCanonicalPath(),
				StringWorker.concatFilePaths(Arrays.asList(a, b), null));
		assertEquals("", StringWorker.concatFilePaths(null, EOL.LF));
	}

	@Test
	public void getStringWhileNotNull() {
		Character[] buf = { 'a', 'b', null, 'c' };
		assertEquals(Arrays.asList("a", "b"), StringWorker.getStringWhileNotNull(buf));
		assertTrue(StringWorker.getStringWhileNotNull(new Character[] { null, 'x' }).isEmpty());
		assertEquals(Arrays.asList("x", "y"), StringWorker.getStringWhileNotNull(new Character[] { 'x', 'y' }));
	}

	// ---------------------------------------------------------------- emptiness / contains

	@Test
	public void emptinessHelpers() {
		assertTrue(StringWorker.isEmpty(null));
		assertTrue(StringWorker.isEmpty(""));
		assertTrue(StringWorker.isEmpty(" \t\r\n"));
		assertFalse(StringWorker.isEmpty(" a "));
		assertTrue(StringWorker.isEmptyNoTrim(null));
		assertTrue(StringWorker.isEmptyNoTrim(""));
		assertFalse(StringWorker.isEmptyNoTrim(" "));
		assertTrue(StringWorker.notEmpty("a"));
		assertFalse(StringWorker.notEmpty("  "));
		assertEquals("", StringWorker.nullToEmpty(null));
		assertEquals(" x ", StringWorker.nullToEmpty(" x "));
		assertEquals("x", StringWorker.trimToEmpty("  x\n"));
		assertEquals("", StringWorker.trimToEmpty(null));
		assertEquals("def", StringWorker.replaceEmpyWithDefault("  ", "def"));
		assertEquals("def", StringWorker.replaceEmpyWithDefault(null, "def"));
		assertEquals("val", StringWorker.replaceEmpyWithDefault("val", "def"));
	}

	@Test
	public void containsHelpers() {
		assertFalse(StringWorker.contains(null, "a"));
		assertFalse(StringWorker.contains("a", null));
		assertTrue(StringWorker.contains("abc", ""));
		assertTrue(StringWorker.contains("abc", "bc"));
		assertFalse(StringWorker.contains("abc", "BC"));
		assertTrue(StringWorker.containsIgnoreCase("abc", "BC"));
		assertFalse(StringWorker.containsIgnoreCase(null, "BC"));
		assertFalse(StringWorker.containsIgnoreCase("abc", null));
		assertTrue(StringWorker.containsIgnoreCase("ÉCOLE", "école"));
	}

	@Test
	public void passwordCharHelpers() {
		assertTrue(StringWorker.containsOneOfMatchesChar("pass!".toCharArray(), "!?#".toCharArray()));
		assertFalse(StringWorker.containsOneOfMatchesChar("pass".toCharArray(), "!?#".toCharArray()));
		assertFalse(StringWorker.containsOneOfMatchesChar(new char[0], "!".toCharArray()));
		assertFalse(StringWorker.containsOneOfMatchesChar("abc".toCharArray(), new char[0]));
		assertTrue(StringWorker.containsMatch("abc".toCharArray(), 'c'));
		assertFalse(StringWorker.containsMatch("abc".toCharArray(), 'd'));
		assertFalse(StringWorker.containsMatch(new char[0], 'd'));
	}

	// ---------------------------------------------------------------- wildcards

	@Test
	public void wildcardNullAndEmptyNeverMatch() {
		assertFalse(StringWorker.checkWildcardMatch(null, "*"));
		assertFalse(StringWorker.checkWildcardMatch("abc", null));
		assertFalse(StringWorker.checkWildcardMatch("", "*"));
		assertFalse(StringWorker.checkWildcardMatch("abc", "   "));
	}

	@Test
	public void wildcardQuestionMarkIsOneOptionalCharacter() {
		assertTrue(StringWorker.checkWildcardMatch("abc", "a?c"));
		assertTrue("optional", StringWorker.checkWildcardMatch("ac", "a?c"));
		assertFalse(StringWorker.checkWildcardMatch("abbc", "a?c"));
	}

	@Test
	public void wildcardStarMatchesAnyTextAndTrimsInputs() {
		assertTrue(StringWorker.checkWildcardMatch("  report_2020.txt ", "report*.txt"));
		assertTrue(StringWorker.checkWildcardMatch("café crème", "café*"));
		assertTrue(StringWorker.checkWildcardMatch("dir with space/file.txt", "*with space*"));
		assertFalse(StringWorker.checkWildcardMatch("abc", "*d"));
	}

	@Test
	public void wildcardRegexGeneration() {
		assertEquals(".*?x.?", StringWorker.getWildcardRegex("*x?", false));
		assertEquals("(?i).*?abc", StringWorker.getWildcardRegex("*ABC", true));
		assertEquals("", StringWorker.getWildcardRegex(null, false));
	}

	@Test
	public void wildcardDotIsTreatedAsRegexAnyCharacter_legacy() {
		// '.' is not escaped: "*.java" also matches "fooXjava"
		assertTrue(StringWorker.checkWildcardMatch("fooXjava", "*.java"));
	}

	@Ignore("BUG: wildcard expression is used as raw regex: 'file(1)*' does not match 'file(1).txt', unbalanced '(' throws PatternSyntaxException")
	@Test
	public void wildcardWithRegexMetacharactersInFileNamesMustMatchLiterally() {
		// legitimate file names contain parentheses, plus, brackets...
		assertTrue(StringWorker.checkWildcardMatch("file(1).txt", "file(1)*"));
		assertTrue(StringWorker.checkWildcardMatch("a+b[1].txt", "a+b[1]*"));
		assertFalse(StringWorker.checkWildcardMatch("aab.txt", "a+b*"));
	}

	// ---------------------------------------------------------------- EOL normalization

	@Test
	public void normalizeToLf() {
		assertEquals("a\nb\nc\nd", StringWorker.normalizeStringToLF("a\r\nb\rc\nd"));
		assertEquals("", StringWorker.normalizeStringToLF(""));
		assertEquals("\n\n", StringWorker.normalizeStringToLF("\r\n\r"));
		assertEquals("no eol", StringWorker.normalizeStringToLF("no eol"));
	}

	@Test
	public void normalizeToLfOfNullThrowsNpe_legacy() {
		try {
			StringWorker.normalizeStringToLF(null);
			fail("legacy behavior: NPE expected");
		} catch (NullPointerException expected) {
			// documented as is
		}
	}

	@Test
	public void normalizeToEol() {
		String mixed = "a\nb\r\nc\rd";
		assertEquals("a\r\nb\r\nc\r\nd", StringWorker.normalizeStringToEol(mixed, EOL.CRLF));
		assertEquals("a\rb\rc\rd", StringWorker.normalizeStringToEol(mixed, EOL.CR));
		assertEquals("a\nb\nc\nd", StringWorker.normalizeStringToEol(mixed, EOL.LF));
		assertEquals("a|b|c|d", StringWorker.normalizeStringToEol(mixed, "|"));
	}

	@Test
	public void makeCrLfVisibleString() {
		String ls = System.getProperty("line.separator");
		assertEquals("", StringWorker.makeCRLFvisible((String) null));
		assertEquals("", StringWorker.makeCRLFvisible(""));
		assertEquals("a[\\r\\n]" + ls + "b[\\n]" + ls + "c[\\r]" + ls + "d",
				StringWorker.makeCRLFvisible("a\r\nb\nc\rd"));
		assertEquals("plain", StringWorker.makeCRLFvisible("plain"));
	}

	@Test
	public void makeCrLfVisibleFile() throws IOException {
		File f = tmp.newFile("eol sample.txt");
		Files.write(f.toPath(), "a\r\nb\nc\rdé".getBytes(StandardCharsets.UTF_8));
		assertEquals("a[\\r][\\n]\r\nb[\\n]\nc[\\r]\rdé", StringWorker.makeCRLFvisible(f));
	}

	@Test
	public void makeCrLfVisibleFileKeepsTrailingAndRepeatedCr() throws IOException {
		File f = tmp.newFile("cr.txt");
		Files.write(f.toPath(), "a\r\rb\r".getBytes(StandardCharsets.UTF_8));
		assertEquals("a[\\r]\r[\\r]\rb[\\r]\r",StringWorker.makeCRLFvisible(f));
	}

	@Test
	public void makeCrLfVisibleFileRejectsNullMissingFile() throws IOException {
		try {
			StringWorker.makeCRLFvisible((File) null);
			fail();
		} catch (IOException expected) {
			// ok
		}
		try {
			StringWorker.makeCRLFvisible(new File(tmp.getRoot(), "missing.txt"));
			fail();
		} catch (IOException expected) {
			// ok
		}
	}

	// ---------------------------------------------------------------- charsets

	@Test
	public void charsetSupport() throws Exception {
		assertTrue(StringWorker.isStringSupportedByCharset("abc", "US-ASCII"));
		assertFalse(StringWorker.isStringSupportedByCharset("café", "US-ASCII"));
		assertTrue(StringWorker.isStringSupportedByCharset("café 中", "UTF-8"));
		assertFalse(StringWorker.isStringSupportedByCharset("", "UTF-8"));
		assertFalse(StringWorker.isStringSupportedByCharset((String) null, "UTF-8"));
		assertFalse(StringWorker.isStringSupportedByCharset("abc", "no-such-charset"));
		assertFalse(StringWorker.isStringSupportedByCharset("abc", "bad charset name!"));
		assertFalse(StringWorker.isStringSupportedByCharset("abc", (String) null));
		assertTrue(StringWorker.isStringSupportedByCharset("café", StandardCharsets.ISO_8859_1));
		assertFalse(StringWorker.isStringSupportedByCharset("中", StandardCharsets.ISO_8859_1));
		assertFalse(StringWorker.isStringSupportedByCharset("abc", (Charset) null));
		assertTrue(StringWorker.validateCharset(StandardCharsets.UTF_8));
		assertFalse(StringWorker.validateCharset((Charset) null));
		assertFalse(StringWorker.validateCharset((String) null));
	}

	@Test
	public void stringToInputStreamUsesPlatformEncoding() throws IOException {
		try (InputStream in = StringWorker.stringToInputStream("abc")) {
			byte[] read = new byte[3];
			assertEquals(3, in.read(read));
			assertArrayEquals(new byte[] { 97, 98, 99 }, read);
			assertEquals(-1, in.read());
		}
	}

	// ---------------------------------------------------------------- versions

	@Test
	public void compareVersions() {
		assertEquals(0, StringWorker.compareVersions("1.2.3", "1.2.3"));
		assertEquals(0, StringWorker.compareVersions("1.0", "1"));
		assertEquals(0, StringWorker.compareVersions("1.0.0.0", "1"));
		assertEquals(1, StringWorker.compareVersions("1.2.10", "1.2.9"));
		assertEquals(-1, StringWorker.compareVersions("1.9", "1.10"));
		assertEquals(-1, StringWorker.compareVersions("1", "1.0.1"));
		assertEquals(1, StringWorker.compareVersions("2", "1.99.99"));
		assertEquals(-1, StringWorker.compareVersions("jdk-17.0.1", "17.0.2"));
		assertEquals(0, StringWorker.compareVersions("0.9.84", "0.9.84"));
		assertEquals(-1, StringWorker.compareVersions("0.9.84", "0.9.85"));
	}

	@Test
	public void compareVersionsRejectsInvalidInput() {
		String[][] invalid = { { null, "1" }, { "1", null }, { "abc", "1" }, { "1", "" }, { "", "" } };
		for (String[] pair : invalid) {
			try {
				StringWorker.compareVersions(pair[0], pair[1]);
				fail("expected IllegalArgumentException for " + Arrays.toString(pair));
			} catch (IllegalArgumentException expected) {
				// ok
			}
		}
	}

	@Test
	public void compareVersionsWithHugeNumberFailsAsIllegalArgument() {
		try {
			StringWorker.compareVersions("99999999999999999999", "1");
			fail();
		} catch (IllegalArgumentException expected) {
			// NumberFormatException is an IllegalArgumentException
		}
	}

	// ---------------------------------------------------------------- numbers

	@Test
	public void decimalFormatting() {
		char sep = DecimalFormatSymbols.getInstance().getDecimalSeparator();
		assertEquals("3" + sep + "14", StringWorker.getStringDouble(3.14159, 2));
		assertEquals("3" + sep + "142", StringWorker.getStringDouble(3.14159, 3));
		assertEquals("0" + sep + "12", StringWorker.getStringDouble(0.125, 2));
		assertEquals("-1" + sep + "50", StringWorker.getStringDouble(-1.5, 2));
		assertEquals("1" + sep + "0", StringWorker.getStringDouble(1d, 1));
	}

	@Test
	public void instanceConstructorsCopyTheInput() {
		assertEquals("", new StringWorker().currentString);
		assertEquals("abc", new StringWorker("abc").currentString);
	}
}
