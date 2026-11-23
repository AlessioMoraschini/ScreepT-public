package utility;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;
import various.common.light.utility.manipulation.ArrayHelper;
import various.common.light.utility.properties.CommentedProperties;
import various.common.light.utility.properties.PropertiesManager;
import various.common.light.utility.string.WildcardPattern;

/** Wildcards, collection helpers, checksums and the comment-preserving properties files. */
public class HashWildcardPropertiesTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Before
	public void quiet() {
		PropertiesManager.setLoggerEnabled(false);
		ChecksumHasher.logger.setLoggerEnabled(false);
	}

	private File write(String relative, byte[] content) throws Exception {
		File f = new File(tmp.getRoot(), relative);
		f.getParentFile().mkdirs();
		Files.write(f.toPath(), content);
		return f;
	}

	private File write(String relative, String content) throws Exception {
		return write(relative, content.getBytes(StandardCharsets.UTF_8));
	}

	// ------------------------------------------------------------------ wildcards

	@Test
	public void wildcardsAreRecognised() {
		assertTrue(WildcardPattern.hasWildcard("*.java"));
		assertTrue(WildcardPattern.hasWildcard("a?c"));
		assertFalse(WildcardPattern.hasWildcard("plain.txt"));
		assertFalse(WildcardPattern.hasWildcard(null));
		assertFalse(WildcardPattern.hasWildcard(""));
	}

	@Test
	public void wholeNameMatchingFollowsGlobRulesAndIgnoresCase() {
		assertTrue(WildcardPattern.matchesWhole("Readme.MD", "readme.*"));
		assertTrue(WildcardPattern.matchesWhole("report_2026.pdf", "report_????.pdf"));
		assertFalse("? is exactly one character", WildcardPattern.matchesWhole("report_26.pdf", "report_????.pdf"));
		assertTrue("star matches the empty string", WildcardPattern.matchesWhole("", "*"));
		assertTrue(WildcardPattern.matchesWhole("abc", "*"));
		assertFalse("whole means whole", WildcardPattern.matchesWhole("abcd", "abc"));
		assertFalse(WildcardPattern.matchesWhole("abc", "ab"));
		assertTrue("backtracking over several stars", WildcardPattern.matchesWhole("xxabxxabxxc", "*ab*ab*c"));
		assertFalse(WildcardPattern.matchesWhole("xxabxxc", "*ab*ab*c"));
		assertTrue(WildcardPattern.matchesWhole("a.b.c", "*.c"));
		assertTrue(WildcardPattern.matchesWhole("", ""));
		assertFalse(WildcardPattern.matchesWhole("a", ""));
	}

	@Test
	public void partMatchingFindsThePatternAnywhere() {
		assertTrue(WildcardPattern.matchesAnyPart("server.LOG.1", "log"));
		assertTrue(WildcardPattern.matchesAnyPart("alpha-beta-gamma", "b?ta"));
		assertTrue(WildcardPattern.matchesAnyPart("alpha-beta-gamma", "a*g"));
		assertFalse(WildcardPattern.matchesAnyPart("alpha", "z"));
		assertTrue("empty pattern is contained in anything", WildcardPattern.matchesAnyPart("alpha", ""));
	}

	// ------------------------------------------------------------------ array helper

	@Test
	public void arrayHelperBasics() throws Exception {
		assertEquals("c", ArrayHelper.getLast(new String[] { "a", "b", "c" }));
		assertNull(ArrayHelper.getLast((String[]) null));
		assertEquals(Arrays.asList("a", "b"), ArrayHelper.removeNulls("a", null, "b", null));
		assertEquals(Collections.emptyList(), ArrayHelper.removeNulls((String[]) null));

		List<String> list = ArrayHelper.populateList(ArrayList.class, "x", "y");
		assertEquals(Arrays.asList("x", "y"), list);
		Set<Integer> sorted = ArrayHelper.listToSet(Arrays.asList(5, 1, 3, 1), true);
		assertEquals(Arrays.asList(1, 3, 5), new ArrayList<>(sorted));
		assertEquals(3, ArrayHelper.listToSet(Arrays.asList(5, 1, 3, 1), false).size());
		assertTrue(ArrayHelper.listToSet(null, true).isEmpty());
		assertTrue(ArrayHelper.setToList(null).isEmpty());
		Set<String> unsorted = new java.util.HashSet<>(Arrays.asList("pear", "apple", "fig"));
		assertEquals(Arrays.asList("apple", "fig", "pear"), new ArrayList<>(ArrayHelper.sortSet(unsorted)));
		assertEquals(Arrays.asList("a", "b", "c"), ArrayHelper.asList(Arrays.asList("c", "a", "b"), true));
		assertEquals(Arrays.asList("c", "a", "b"), ArrayHelper.asList(Arrays.asList("c", "a", "b"), false));
		assertArrayEquals(new String[] { "a", "b" }, ArrayHelper.removeEmpties(new String[] { "a", "", null, "  ", "b" }));
	}

	@Test
	public void randomElementRespectsTheProbabilityBounds() {
		List<String> one = Collections.singletonList("only");
		for (int i = 0; i < 50; i++) {
			assertEquals("100% with one element always gives it", "only", ArrayHelper.getRandomElement(one, 100f));
			assertNull("0% never gives anything", ArrayHelper.getRandomElement(one, 0f));
			assertNull("0% never gives anything (array)", ArrayHelper.getRandomElement(new String[] { "a", "b" }, -5f));
		}
		assertNull(ArrayHelper.getRandomElement(Collections.<String>emptyList(), 100f));
		assertNull(ArrayHelper.getRandomElement((String[]) null, 100f));
		Set<String> seen = new TreeSet<>();
		String[] four = { "a", "b", "c", "d" };
		for (int i = 0; i < 400; i++) {
			seen.add(ArrayHelper.getRandomElement(four, 250f));
		}
		assertEquals("a probability above 100 is clamped and every element can come out", 4, seen.size());
	}

	// ------------------------------------------------------------------ checksums

	@Test
	public void checksumsMatchTheStandardTestVectors() throws Throwable {
		File abc = write("abc.txt", "abc");
		assertEquals("900150983CD24FB0D6963F7D28E17F72", ChecksumHasher.getFileHashBuffered(abc, HashingFunction.MD5, true));
		assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", ChecksumHasher.getFileHashBuffered(abc, HashingFunction.SHA1, false));
		assertEquals("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
				ChecksumHasher.getFileHashBuffered(abc.getAbsolutePath(), HashingFunction.SHA256));
		assertEquals("same as the path based entry point", ChecksumHasher.getFileHashBuffered(abc, HashingFunction.SHA256, true),
				ChecksumHasher.getFileHash(abc.getAbsolutePath(), HashingFunction.SHA256));
		assertEquals("legacy full-read variant agrees", ChecksumHasher.getFileHashBuffered(abc, HashingFunction.SHA512, false),
				ChecksumHasher.getFileHash(abc, HashingFunction.SHA512, false));
		assertEquals("", ChecksumHasher.getFileHash((File) null, HashingFunction.MD5, true));
		assertEquals("", ChecksumHasher.getFileHash((String) null, HashingFunction.MD5));
	}

	@Test
	public void bigFileHashAgreesWithTheJdkDigestForEveryAlgorithm() throws Throwable {
		byte[] data = new byte[3 * 1024 * 1024 + 17];
		new Random(42).nextBytes(data);
		File big = write("big.bin", data);
		for (HashingFunction f : HashingFunction.values()) {
			byte[] digest = MessageDigest.getInstance(f.algorithmCode).digest(data);
			StringBuilder hex = new StringBuilder();
			for (byte b : digest) {
				hex.append(String.format("%02x", b));
			}
			assertEquals(f.name(), hex.toString(), ChecksumHasher.getFileHashBuffered(big, f, false));
		}
	}

	@Test
	public void unreadableFileGivesAnEmptyHashNotAnException() throws Throwable {
		assertEquals("", ChecksumHasher.getFileHashBuffered(new File(tmp.getRoot(), "missing.bin"), HashingFunction.MD5, true));
	}

	@Test
	public void checkFileHashAcceptsCaseDifferencesAndRejectsOtherContent() throws Throwable {
		File abc = write("abc.txt", "abc");
		String sha1 = "a9993e364706816aba3e25717850c26c9cd0d89d";
		assertTrue(ChecksumHasher.checkFileHash(abc, sha1, HashingFunction.SHA1));
		assertTrue(ChecksumHasher.checkFileHash(abc.getAbsolutePath(), sha1.toUpperCase(), HashingFunction.SHA1));
		assertTrue("a manifest line may carry more text around the hash", ChecksumHasher.checkFileHash(abc, "abc.txt " + sha1 + " ok", HashingFunction.SHA1));
		assertFalse(ChecksumHasher.checkFileHash(abc, sha1.replace('a', 'b'), HashingFunction.SHA1));
		assertFalse(ChecksumHasher.checkFileHash(abc, null, HashingFunction.SHA1));
		assertFalse(ChecksumHasher.checkFileHash((File) null, sha1, HashingFunction.SHA1));
		File same = write("copy/abc-copy.txt", "abc");
		File other = write("copy/other.txt", "abd");
		assertTrue(ChecksumHasher.equalsFilesChecksum(abc, same, HashingFunction.SHA256));
		assertFalse(ChecksumHasher.equalsFilesChecksum(abc, other, HashingFunction.SHA256));
	}

	@Test
	public void folderHashDependsOnContentNotOnCreationOrder() throws Throwable {
		write("one/a.txt", "alpha");
		write("one/sub/b.txt", "beta");
		write("one/sub/deeper/c.txt", "gamma");
		write("two/sub/deeper/c.txt", "gamma");
		write("two/sub/b.txt", "beta");
		write("two/a.txt", "alpha");
		String h1 = ChecksumHasher.getFolderHash(new File(tmp.getRoot(), "one"), HashingFunction.SHA256, true);
		String h2 = ChecksumHasher.getFolderHash(new File(tmp.getRoot(), "two"), HashingFunction.SHA256, true);
		assertEquals("same files, same tree, same hash", h1, h2);
		assertFalse(h1.isEmpty());

		// (a new tree: hashed files stay memory-mapped, and Windows does not let a mapped file be rewritten)
		write("three/a.txt", "alpha");
		write("three/sub/b.txt", "beta!");
		write("three/sub/deeper/c.txt", "gamma");
		assertNotEquals("one changed byte in a nested file changes the folder hash", h1,
				ChecksumHasher.getFolderHash(new File(tmp.getRoot(), "three"), HashingFunction.SHA256, true));

		File single = write("single.txt", "abc");
		assertEquals("a file given as folder is hashed as a file", ChecksumHasher.getFileHashBuffered(single, HashingFunction.MD5, true),
				ChecksumHasher.getFolderHash(single, HashingFunction.MD5, true));
		assertEquals("", ChecksumHasher.getFolderHash(null, HashingFunction.MD5, true));
	}

	@Test
	public void xorOfDifferentLengthsKeepsTheLongerTail() {
		assertArrayEquals(new byte[] { 3, 3, 9 }, ChecksumHasher.xorBytes(new byte[] { 1, 2, 9 }, new byte[] { 2, 1 }));
		assertArrayEquals(new byte[] { 3, 3, 9 }, ChecksumHasher.xorBytes(new byte[] { 2, 1 }, new byte[] { 1, 2, 9 }));
	}

	@Test
	public void saltsAre16BytesAndDifferEachTime() throws Exception {
		byte[] a = ChecksumHasher.getSalt();
		byte[] b = ChecksumHasher.getSalt();
		assertEquals(16, a.length);
		assertFalse(Arrays.equals(a, b));
	}

	@Test
	public void manifestChecksViaPropertiesFile() throws Throwable {
		File good = write("pkg/good.jar", "good content");
		File bad = write("pkg/bad.jar", "bad content");
		File unlisted = write("pkg/unlisted.jar", "whatever");
		String goodHash = ChecksumHasher.getFileHashBuffered(good, HashingFunction.SHA256, true);
		File manifest = write("manifest.properties", "good.jar=" + goodHash + "\nbad.jar=" + goodHash + "\nempty.jar=   \n");
		PropertiesManager pm = new PropertiesManager(manifest);

		assertTrue(ChecksumHasher.checkHashFromProperties(good, pm, HashingFunction.SHA256));
		assertFalse("the manifest lists another hash for it", ChecksumHasher.checkHashFromProperties(bad, pm, HashingFunction.SHA256));
		assertTrue("not in the manifest: nothing to check", ChecksumHasher.checkHashFromProperties(unlisted, pm, HashingFunction.SHA256));
		assertFalse("missing file", ChecksumHasher.checkHashFromProperties(new File(tmp.getRoot(), "nope.jar"), pm, HashingFunction.SHA256));
		assertFalse(ChecksumHasher.checkHashFromProperties(good, null, HashingFunction.SHA256));

		assertTrue(ChecksumHasher.checkHashesFromProperties(Arrays.asList(good, unlisted), pm, HashingFunction.SHA256));
		assertFalse("one wrong file spoils the whole list", ChecksumHasher.checkHashesFromProperties(Arrays.asList(good, bad, unlisted), pm, HashingFunction.SHA256));
		assertTrue(ChecksumHasher.checkHashesFromProperties(null, pm, HashingFunction.SHA256));
		assertTrue(ChecksumHasher.checkHashesFromProperties(new ArrayList<File>(), pm, HashingFunction.SHA256));
	}

	// ------------------------------------------------------------------ properties files

	private static final String SAMPLE = "# Application settings\n"
			+ "# second header line\n"
			+ "\n"
			+ "count = 42\n"
			+ "# the ratio of things\n"
			+ "ratio=0.75\n"
			+ "flag=true\n"
			+ "size=800x600\n"
			+ "color=10-20-30\n"
			+ "list=one;;two; ;three\n"
			+ "name=  padded value  \n";

	@Test
	public void typedGettersReadValuesAndFallBackToTheDefault() throws Exception {
		PropertiesManager pm = new PropertiesManager(write("app.properties", SAMPLE));
		assertEquals(42, pm.getIntVarFromProps("count", -1));
		assertEquals(-1, pm.getIntVarFromProps("name", -1));
		assertEquals(-1, pm.getIntVarFromProps("missing", -1));
		assertEquals(0.75f, pm.getFloatVarFromProps("ratio", 0f), 0.0001f);
		assertEquals(9.5f, pm.getFloatVarFromProps("missing", 9.5f), 0.0001f);
		assertTrue(pm.getBooleanVarFromProps("flag", false));
		assertEquals("padded value", pm.getProperty("name"));
		assertEquals("", pm.getProperty("missing"));
		assertEquals("dflt", pm.getProperty("missing", "dflt"));
		assertNull(pm.getPropertyNullable("missing"));
		assertEquals(new Color(10, 20, 30), pm.getColorVarFromProps("color", Color.WHITE));
		assertEquals(Color.WHITE, pm.getColorVarFromProps("missing", Color.WHITE));
		assertEquals(new Dimension(800, 600), pm.getDimensionFromString("x", "size", new Dimension(1, 1)));
		assertEquals(new Dimension(1, 1), pm.getDimensionFromString("x", "missing", new Dimension(1, 1)));
		assertEquals(new Dimension(1, 1), pm.getDimensionFromString(",", "size", new Dimension(1, 1)));
		assertEquals("empty and blank items are dropped", Arrays.asList("one", "two", "three"), pm.getStringList("list", ";"));
		assertTrue(pm.getStringList("missing", ";").isEmpty());
		assertEquals("a;b;c", PropertiesManager.listToString(Arrays.asList("a", "b", "c"), ";"));
		assertEquals("", PropertiesManager.listToString(new ArrayList<String>(), ";"));
	}

	@Test
	public void fileListSkipsNonExistingFilesOnRequest() throws Exception {
		File real = write("real.txt", "x");
		File props = write("paths.properties", "files=" + real.getAbsolutePath().replace("\\", "/") + ";" + new File(tmp.getRoot(), "ghost.txt").getAbsolutePath().replace("\\", "/") + "\n");
		PropertiesManager pm = new PropertiesManager(props);
		assertEquals(2, pm.getFileList("files", ";", false).size());
		List<File> onlyExisting = pm.getFileList("files", ";", true);
		assertEquals(1, onlyExisting.size());
		assertEquals(real.getName(), onlyExisting.get(0).getName());
	}

	@Test
	public void savingACommentedPropertyKeepsCommentsOrderAndOtherKeys() throws Exception {
		File f = write("app.properties", SAMPLE);
		PropertiesManager pm = new PropertiesManager(f);
		pm.saveCommentedProperty("count", "43");

		String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
		assertTrue("header comment survives", text.contains("# Application settings"));
		assertTrue("comment of a single key survives", text.contains("# the ratio of things"));
		assertTrue("new value is written", java.util.regex.Pattern.compile("^count\\s*=\\s*43$", java.util.regex.Pattern.MULTILINE).matcher(text).find());
		assertFalse("old value is replaced, not duplicated", java.util.regex.Pattern.compile("^count\\s*=\\s*42$", java.util.regex.Pattern.MULTILINE).matcher(text).find());
		assertTrue(text.indexOf("count") < text.indexOf("ratio") && text.indexOf("ratio") < text.indexOf("flag"));

		PropertiesManager reread = new PropertiesManager(f);
		assertEquals(43, reread.getIntVarFromProps("count", -1));
		assertEquals(0.75f, reread.getFloatVarFromProps("ratio", 0f), 0.0001f);
		assertEquals("padded value", reread.getProperty("name"));
	}

	@Test
	public void savingANewKeyAppendsItToTheFile() throws Exception {
		File f = write("app.properties", SAMPLE);
		new PropertiesManager(f).saveCommentedProperty("brand.new", "yes");
		assertEquals("yes", new PropertiesManager(f).getProperty("brand.new"));
	}

	@Test
	public void savingWithoutWritingTheFileChangesOnlyMemory() throws Exception {
		File f = write("app.properties", SAMPLE);
		PropertiesManager pm = new PropertiesManager(f);
		pm.saveCommentedProperty("count", "99", false);
		assertEquals(99, pm.getIntVarFromProps("count", -1));
		assertEquals(42, new PropertiesManager(f).getIntVarFromProps("count", -1));
	}

	@Test
	public void removeAndPlainSaveWriteTheFile() throws Exception {
		File f = write("plain.properties", "a=1\nb=2\n");
		PropertiesManager pm = new PropertiesManager(f);
		assertTrue(pm.savePropertyChecked("c", "3"));
		assertEquals("3", new PropertiesManager(f).getProperty("c"));
		assertTrue(pm.removeProperty("a"));
		PropertiesManager after = new PropertiesManager(f);
		assertEquals("", after.getProperty("a"));
		assertEquals("2", after.getProperty("b"));
		assertTrue(pm.removeProperty("b", false));
		assertEquals("memory only", "", pm.getProperty("b"));
		assertEquals("file untouched", "2", new PropertiesManager(f).getProperty("b"));
		assertTrue(pm.saveAllProperties());
		assertEquals("", new PropertiesManager(f).getProperty("b"));
	}

	@Test
	public void missingFileGivesAnUnboundManager() {
		PropertiesManager pm = new PropertiesManager(new File(tmp.getRoot(), "nope.properties"));
		assertNull(pm.getDynamicFileBindingsDefSource());
		assertEquals("", pm.getProperty("anything"));
		assertFalse(pm.readFromFile());
	}

	@Test
	public void inMemoryManagerWorksOnTheGivenProperties() {
		Properties p = new Properties();
		p.setProperty("k", " v ");
		PropertiesManager pm = new PropertiesManager(p);
		assertEquals("v", pm.getProperty("k"));
		assertEquals(p, pm.getDynamicFileBindingsDef());
	}

	@Test
	public void commentedPropertiesRoundTripKeepsLinesInOrder() throws Exception {
		CommentedProperties cp = new CommentedProperties();
		cp.load(new java.io.ByteArrayInputStream("# top\nz=1\n\n# mid\na=2\n".getBytes(StandardCharsets.ISO_8859_1)));
		assertEquals("1", cp.getProperty("z"));
		assertEquals("2", cp.getProperty("a"));
		cp.put("z", "10");
		cp.add("m", "5");
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		cp.store(out, null);
		String text = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
		assertTrue(text, text.indexOf("# top") < text.indexOf("z=10"));
		assertTrue(text, text.indexOf("z=10") < text.indexOf("# mid"));
		assertTrue(text, text.indexOf("# mid") < text.indexOf("a=2"));
		assertTrue(text, text.contains("m=5"));
	}
}
