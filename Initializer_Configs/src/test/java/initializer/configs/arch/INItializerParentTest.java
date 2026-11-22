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
package initializer.configs.arch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.ini4j.Wini;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.files.om.FileNamed;

/**
 * Parsing and serialization helpers of {@link INItializerParent} and reading/writing of single values on a temporary ini file.
 */
public class INItializerParentTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File ini;

	/** Minimal concrete owner of an ini file */
	private static final class TestInit extends INItializerParent {
		TestInit(String path) {
			super(path);
		}

		@Override
		public void readConfigIniFile() {
			// not needed
		}

		@Override
		public void writeCurrentConfigIniFile() {
			// not needed
		}
	}

	@Before
	public void createIni() throws IOException {
		ini = tmp.newFile("test config.ini");
	}

	private static final String S = INItializerParent.SEPARATOR_DEFAULT;
	private static final String IN = INItializerParent.SEPARATOR_INNER_DEFAULT;
	private static final String LONG = INItializerParent.SEPARATOR_INNER_DEFAULT_LONG;

	// ---------------------------------------------------------------- scalar parsing

	@Test
	public void intParsingFallsBackToTheDefault() {
		assertEquals(42, INItializerParent.getIntVarFromString("42", 7));
		assertEquals(-3, INItializerParent.getIntVarFromString("-3", 7));
		assertEquals(7, INItializerParent.getIntVarFromString("abc", 7));
		assertEquals(7, INItializerParent.getIntVarFromString("", 7));
		assertEquals(7, INItializerParent.getIntVarFromString(null, 7));
		assertEquals(7, INItializerParent.getIntVarFromString(" 42", 7));
		assertEquals(7, INItializerParent.getIntVarFromString("99999999999", 7));
		assertEquals(7, INItializerParent.getIntVarFromString("4.5", 7));
	}

	@Test
	public void floatAndDoubleParsing() {
		assertEquals(0.5f, INItializerParent.getFloatVarFromString("0.5", 1f), 0f);
		assertEquals(1f, INItializerParent.getFloatVarFromString("x", 1f), 0f);
		assertEquals(1f, INItializerParent.getFloatVarFromString(null, 1f), 0f);
		assertEquals(2.25, INItializerParent.getDoubleVarFromString("2.25", 1d), 0d);
		assertEquals(1d, INItializerParent.getDoubleVarFromString("2,25", 1d), 0d);
		assertEquals(1d, INItializerParent.getDoubleVarFromString("", 1d), 0d);
	}

	@Test
	public void fileParsing() {
		File def = new File("default");
		assertEquals(new File("a/b.txt"), INItializerParent.getFileFromString("a/b.txt", def));
		assertSame(def, INItializerParent.getFileFromString(null, def));
		assertEquals(new File(""), INItializerParent.getFileFromString("", def));
	}

	@Test
	public void booleanParsing() throws IOException {
		TestInit init = new TestInit(ini.getPath());
		assertTrue(init.getBooleanFromString("true", false));
		assertTrue(init.getBooleanFromString("TRUE", false));
		assertFalse(init.getBooleanFromString("false", true));
		assertTrue("blank uses the default", init.getBooleanFromString("", true));
		assertTrue("null uses the default", init.getBooleanFromString(null, true));
		assertFalse("garbage is false, not the default", init.getBooleanFromString("yes", true));
	}

	@Test
	public void fontAndColorWrappers() {
		Font f = new Font("Dialog", Font.BOLD, 14);
		assertEquals(f.getFontName() + "-1-14", INItializerParent.getFontString(f));
		assertEquals(14, INItializerParent.getFontFromString("Dialog-1-14").getSize());
		Font def = new Font("Dialog", Font.PLAIN, 9);
		assertSame(def, INItializerParent.getFontFromString("broken", def));
		assertEquals("1-2-3", INItializerParent.getRgbString(new Color(1, 2, 3)));
		assertEquals(new Color(1, 2, 3), INItializerParent.getRgbColorFromString("1-2-3"));
	}

	// ---------------------------------------------------------------- lists

	@Test
	public void arrayListFromStringSkipsEmptyElements() {
		ArrayList<String> def = new ArrayList<String>(Arrays.asList("def"));
		assertEquals(Arrays.asList("a", "b", "c"), INItializerParent.getStringListFromString("a;b;;c;", ";", def));
		assertEquals(Collections.emptyList(), INItializerParent.getStringListFromString("", ";", def));
		assertSame(def, INItializerParent.getStringListFromString(null, ";", def));
		assertEquals(Arrays.asList("with space", "x"), INItializerParent.getStringListFromString("with space;x", ";", def));
	}

	@Test
	public void vectorFromStringKeepsEmptyMiddleElements() {
		Vector<String> def = new Vector<String>(Arrays.asList("def"));
		assertEquals(Arrays.asList("a", "b", "", "c"), INItializerParent.getStringListFromString("a;b;;c", ";", def));
		assertEquals("trailing empty parts are dropped by split", Arrays.asList("a"), INItializerParent.getStringListFromString("a;;", ";", def));
		assertSame(def, INItializerParent.getStringListFromString((String) null, ";", def));
		assertEquals("an empty value is a list with one empty element: legacy", Arrays.asList(""),
				INItializerParent.getStringListFromString("", ";", def));
		assertEquals(Arrays.asList("x", "y"), INItializerParent.getStringListFromString("x" + LONG + "y", LONG, def));
	}

	@Test
	public void fileNamedListFromString() {
		Vector<FileNamed> def = new Vector<FileNamed>();
		Vector<FileNamed> list = INItializerParent.getFileNamedListFromString(" C:\\dir one\\a.txt" + S + "b.txt ", S, def);
		assertEquals(2, list.size());
		assertEquals(new FileNamed("C:\\dir one\\a.txt"), list.get(0));
		assertEquals(new FileNamed("b.txt"), list.get(1));
		assertEquals("blank separator falls back to ';'", 2,
				INItializerParent.getFileNamedListFromString("a;b", "", def).size());
		assertTrue("null value gives an empty path", INItializerParent.getFileNamedListFromString(null, S, def).size() <= 1);
	}

	@Test
	public void matrixOfStringsFromString() {
		Vector<String[]> def = new Vector<String[]>();
		// rows are separated by "::", the cells of a row by ":;:;" (as written by toCharSeparatedStringMatrix)
		String raw = "*.java" + LONG + "*.xml" + IN + "*.txt" + IN + "docs" + LONG + "readme";
		Vector<String[]> matrix = INItializerParent.getMatrixOfStringsFromString(raw, IN, LONG, def);
		assertEquals(3, matrix.size());
		assertEquals(Arrays.asList("*.java", "*.xml"), Arrays.asList(matrix.get(0)));
		assertEquals(Arrays.asList("*.txt"), Arrays.asList(matrix.get(1)));
		assertEquals(Arrays.asList("docs", "readme"), Arrays.asList(matrix.get(2)));
		Vector<String[]> blankRow = INItializerParent.getMatrixOfStringsFromString("a" + IN + "   " + IN + "b", IN, LONG, def);
		assertEquals("a blank row becomes a row with one empty cell: legacy", 3, blankRow.size());
		assertEquals(Arrays.asList(""), Arrays.asList(blankRow.get(1)));
		assertEquals(0, INItializerParent.getMatrixOfStringsFromString(null, IN, LONG, def).size());
	}

	@Test
	public void mapFromKeysAndValues() {
		Map<String, Integer> map = INItializerParent.getMapFromKeyList(Arrays.asList("a", "b", "c"), Arrays.asList(1, null), -1);
		assertEquals(Integer.valueOf(1), map.get("a"));
		assertEquals("null value", Integer.valueOf(-1), map.get("b"));
		assertEquals("missing value", Integer.valueOf(-1), map.get("c"));
		assertEquals(3, map.size());
		assertTrue(INItializerParent.getMapFromKeyList(new ArrayList<String>(), new ArrayList<Integer>(), 0).isEmpty());
	}

	@Test
	public void stringMapParsing() {
		Map<String, String> def = new HashMap<String, String>();
		String raw = " k1 " + IN + " v1 " + S + "only" + S + "bad" + IN + "a" + IN + "b" + S + S + "k2" + IN + "v2";
		Map<String, String> map = INItializerParent.getStringMapFromString(raw, S, IN, def);
		assertEquals("v1", map.get("k1"));
		assertEquals("a key without value is kept empty", "", map.get("only"));
		assertEquals("v2", map.get("k2"));
		assertFalse("three parts are ignored", map.containsKey("bad"));
		assertTrue(INItializerParent.getStringMapFromString(null, S, IN, def).isEmpty());
		assertTrue(INItializerParent.getStringMapFromString("", S, IN, def).isEmpty());
	}

	@Test
	public void mapToStringAndBack() {
		assertEquals("", INItializerParent.mapToString(null));
		assertEquals("", INItializerParent.mapToString(new HashMap<String, String>()));
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("one", "1");
		map.put("two", "2");
		String text = INItializerParent.mapToString(map);
		assertEquals("one" + IN + "1" + S + "two" + IN + "2", text);
		assertEquals(map, INItializerParent.getStringMapFromString(text, S, IN, new HashMap<String, String>()));
	}

	@Test
	public void listToStringUsesCommas() throws IOException {
		TestInit init = new TestInit(ini.getPath());
		assertEquals("", init.listToString(new ArrayList<String>()));
		assertEquals("a", init.listToString(Arrays.asList("a")));
		assertEquals("a,b,c", init.listToString(Arrays.asList("a", "b", "c")));
	}

	// ---------------------------------------------------------------- static file access

	@Test
	public void writeAndReadValueOnAFileWithSpaces() throws IOException {
		assertTrue(INItializerParent.writeToFile(ini.getPath(), "SECTION", "key", "value"));
		assertEquals("value", INItializerParent.readFromFile(ini.getPath(), "SECTION", "key"));
		assertTrue(INItializerParent.writeToFile(ini.getPath(), "SECTION", "key", "changed"));
		assertEquals("changed", INItializerParent.readFromFile(ini.getPath(), "SECTION", "key"));
	}

	@Test
	public void valuesSurviveTheIniRoundTrip() throws IOException {
		String[] values = { "C:\\Users\\Alessio Test\\file name.txt", "caf\u00E9 \u00FC \u00F1", "a=b;c,d", "x::y", "100%", "{0} [x]",
				"quote \" and ' end", "http://host:80/path?q=1&r=2", "multi word value" };
		for (String value : values) {
			assertTrue("written: " + value, INItializerParent.writeToFile(ini.getPath(), "SEC", "k", value));
			assertEquals(value, INItializerParent.readFromFile(ini.getPath(), "SEC", "k"));
		}
	}

	@Test
	public void valueWithSurroundingSpacesIsReportedAsNotWritten_legacy() throws IOException {
		// the value is trimmed when read back, so the comparison made by writeToFile fails
		assertFalse(INItializerParent.writeToFile(ini.getPath(), "SEC", "k", "  padded  "));
		assertEquals("padded", INItializerParent.readFromFile(ini.getPath(), "SEC", "k"));
	}

	@Test
	public void missingFileOrValueReadsAsEmpty() throws IOException {
		assertEquals("", INItializerParent.readFromFile(null, "S", "k"));
		assertEquals("", INItializerParent.readFromFile(new File(tmp.getRoot(), "missing.ini").getPath(), "S", "k"));
		assertEquals("", INItializerParent.readFromFile(ini.getPath(), "S", "absent"));
		assertFalse(INItializerParent.writeToFile(null, "S", "k", "v"));
		assertFalse(INItializerParent.writeToFile(new File(tmp.getRoot(), "missing.ini").getPath(), "S", "k", "v"));
		assertFalse("the file is not created", new File(tmp.getRoot(), "missing.ini").exists());
	}

	@Test
	public void sectionsAreIndependent() throws IOException {
		INItializerParent.writeToFile(ini.getPath(), "A", "k", "1");
		INItializerParent.writeToFile(ini.getPath(), "B", "k", "2");
		assertEquals("1", INItializerParent.readFromFile(ini.getPath(), "A", "k"));
		assertEquals("2", INItializerParent.readFromFile(ini.getPath(), "B", "k"));
		assertEquals("", INItializerParent.readFromFile(ini.getPath(), "C", "k"));
	}

	// ---------------------------------------------------------------- instance access

	@Test
	public void singleValuesAreStoredImmediately() throws IOException {
		TestInit init = new TestInit(ini.getPath());
		assertTrue(init.writeSingleValue("SEC", "text", "some text"));
		assertTrue(init.writeSingleValue("SEC", "int", 42));
		assertTrue(init.writeSingleValue("SEC", "float", 1.5f));
		assertTrue(init.writeSingleValue("SEC", "bool", true));
		Wini onDisk = new Wini(ini);
		assertEquals("some text", onDisk.get("SEC", "text"));
		assertEquals("42", onDisk.get("SEC", "int"));
		assertEquals("1.5", onDisk.get("SEC", "float"));
		assertEquals("true", onDisk.get("SEC", "bool"));
		assertEquals("some text", init.readFromFile("SEC", "text"));
	}

	@Test
	public void safeConfigurationValuesUseTheDefaultsWhenBlank() throws IOException {
		Files.write(ini.toPath(), ("[SEC]\r\nfilled=value\r\nblank=\r\nspaces=   \r\nfile=" + ini.getPath().replace("\\", "\\\\") + "\r\n")
				.getBytes(StandardCharsets.UTF_8));
		TestInit init = new TestInit(ini.getPath());
		assertEquals("value", init.getSafeStringConf("SEC", "filled", "def"));
		assertEquals("def", init.getSafeStringConf("SEC", "blank", "def"));
		assertEquals("def", init.getSafeStringConf("SEC", "spaces", "def"));
		assertEquals("def", init.getSafeStringConf("SEC", "missing", "def"));
		assertEquals("def", init.getSafeStringConf("NOSEC", "missing", "def"));
		assertEquals(ini, init.getSafeFileConf("SEC", "file", "default.txt"));
		assertEquals("not a file: default", new File("default.txt"), init.getSafeFileConf("SEC", "filled", "default.txt"));
		assertEquals(new File("default.txt"), init.getSafeFileConf("SEC", "blank", "default.txt"));
	}

	@Test
	public void readFileIsNotRefreshedByWrites_legacy() throws IOException {
		TestInit init = new TestInit(ini.getPath());
		init.writeSingleValue("SEC", "k", "new");
		assertEquals("", init.getSafeStringConf("SEC", "k", ""));
		init.setIniReadFile(new Wini(ini));
		assertEquals("new", init.getSafeStringConf("SEC", "k", ""));
	}

	@Test
	public void storeWritesPendingChanges() throws IOException {
		TestInit init = new TestInit(ini.getPath());
		init.getIniFile().put("SEC", "pending", "yes");
		assertEquals("", INItializerParent.readFromFile(ini.getPath(), "SEC", "pending"));
		init.store();
		assertEquals("yes", INItializerParent.readFromFile(ini.getPath(), "SEC", "pending"));
		assertNotNull(init.getIniReadFile());
		assertNull(new Wini().get("SEC", "pending"));
		List<String> names = new ArrayList<String>(Arrays.asList(tmp.getRoot().list()));
		assertEquals(Arrays.asList("test config.ini"), names);
	}

	@Test
	public void constructorWithAnExistingWiniSharesIt() throws IOException {
		Wini shared = new Wini(ini);
		TestWini init = new TestWini(shared, ini.getPath());
		assertSame(shared, init.getIniFile());
		assertSame(shared, init.getIniReadFile());
	}

	private static final class TestWini extends INItializerParent {
		TestWini(Wini configFile, String defaultIniPath) {
			super(configFile, defaultIniPath);
		}

		@Override
		public void readConfigIniFile() {
			// not needed
		}

		@Override
		public void writeCurrentConfigIniFile() {
			// not needed
		}
	}

	@Test
	public void constructorWithoutWiniOpensTheDefaultPath() throws IOException {
		TestWini init = new TestWini(null, ini.getPath());
		assertNotNull(init.getIniFile());
		assertEquals(ini.getCanonicalPath(), init.getIniFile().getFile().getCanonicalPath());
	}

	@Test
	public void constructorOnAMissingFileLeavesTheIniUnset() {
		TestInit init = new TestInit(new File(tmp.getRoot(), "does not exist.ini").getPath());
		assertNull(init.getIniFile());
	}
}
