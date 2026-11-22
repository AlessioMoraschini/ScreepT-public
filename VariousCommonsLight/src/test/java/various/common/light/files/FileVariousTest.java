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
package various.common.light.files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.utility.string.StringWorker.EOL;

/**
 * Pure and temp-folder based logic of {@link FileVarious}: path normalization, naming, counting, ancestors.
 * Everything happens inside a JUnit TemporaryFolder.
 */
public class FileVariousTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	/** tmp/d/a.txt, tmp/d/b.log, tmp/d/sub/c.txt, tmp/d/sub/deep/e.log, tmp/d/emptydir */
	private File d;

	@Before
	public void buildTree() throws IOException {
		d = tmp.newFolder("d dir");
		write(new File(d, "a.txt"), "a");
		write(new File(d, "b.log"), "b");
		File sub = new File(d, "sub");
		assertTrue(sub.mkdir());
		write(new File(sub, "c.txt"), "c");
		File deep = new File(sub, "deep");
		assertTrue(deep.mkdir());
		write(new File(deep, "e.log"), "e");
		assertTrue(new File(d, "emptydir").mkdir());
	}

	private static void write(File f, String text) throws IOException {
		Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
	}

	// ---------------------------------------------------------------- paths

	@Test
	public void parentHelpersReturnTheGivenFile_legacy() {
		File f = new File("x.txt");
		assertSame(f, FileVarious.getParent(f));
		assertNull(FileVarious.getParent(null));
		assertEquals(f.getAbsolutePath(), FileVarious.getParentPath(f));
		assertNull(FileVarious.getParentPath(null));
	}

	@Test
	public void canonicalPathSafe() throws IOException {
		File f = new File(d, "sub/../a.txt");
		assertEquals(new File(d, "a.txt").getCanonicalPath(), FileVarious.getCanonicalPathSafe(f));
		assertNull(FileVarious.getCanonicalPathSafe(null));
		assertEquals(new File(d, "a.txt").getCanonicalFile(), FileVarious.getCanonicalFileSafe(f));
		assertEquals(new File(d, "a.txt").getCanonicalFile(), FileVarious.getCanonicalFileSafe(f.getPath()));
		assertNull(FileVarious.getCanonicalFileSafe((File) null));
	}

	@Test
	public void normalizedPathUsesTheRequestedSeparator() throws IOException {
		String canonical = d.getCanonicalPath();
		String slash = FileVarious.getCanonicalPathSafeNormalized(d, "/");
		assertEquals(canonical.replace('\\', '/'), slash);
		String back = FileVarious.getCanonicalPathSafeNormalized(d, "\\");
		assertEquals(canonical.replace('/', '\\'), back);
		assertEquals(canonical.replace("\\", "|").replace("/", "|"), FileVarious.getCanonicalPathSafeNormalized(d, "|"));
		assertNull(FileVarious.getCanonicalPathSafeNormalized(null, "/"));
	}

	@Test
	public void sameFileIsDecidedByCanonicalPath() throws IOException {
		File a = new File(d, "a.txt");
		assertTrue(FileVarious.checkIfSameFile(a, new File(d, "sub/../a.txt")));
		assertFalse(FileVarious.checkIfSameFile(a, new File(d, "b.log")));
		assertFalse(FileVarious.checkIfSameFile(a, null));
		assertTrue("two nulls are both 'empty': legacy", FileVarious.checkIfSameFile(null, null));
	}

	@Test
	public void validation() {
		assertTrue(FileVarious.validateFileOrDir(d));
		assertTrue(FileVarious.validateDir(d));
		assertFalse(FileVarious.validateFile(d));
		assertTrue(FileVarious.validateFile(new File(d, "a.txt")));
		assertFalse(FileVarious.validateDir(new File(d, "a.txt")));
		assertFalse(FileVarious.validateFileOrDir(null));
		assertFalse(FileVarious.validateFileOrDir(new File(d, "nope")));
		assertFalse(FileVarious.validateDir(null));
		assertFalse(FileVarious.validateFile(null));
	}

	@Test
	public void relativePaths() throws Exception {
		File deepFile = new File(d, "sub/deep/e.log");
		assertEquals("sub/deep/e.log", FileVarious.getPathRelativeToOtherFile(deepFile, d));
		assertEquals("a.txt", FileVarious.getPathRelativeToOtherFile(new File(d, "a.txt"), d));
		assertNull(FileVarious.getPathRelativeToOtherFile(null, d));
		assertNull(FileVarious.getPathRelativeToOtherFile(d, null));
		File missingDir = new File(tmp.getRoot(), "missing dir");
		assertEquals("x/y.txt", FileVarious.getPathRelativeToOtherFile(new File(missingDir, "x/y.txt"), missingDir));
	}

	@Test
	public void relativePathsOfManyFilesSkipNullsAndMissing() throws Exception {
		ArrayList<File> files = new ArrayList<File>(Arrays.asList(new File(d, "a.txt"), null, new File(d, "missing.txt"),
				new File(d, "sub/c.txt")));
		assertEquals(Arrays.asList("a.txt", "sub/c.txt"), FileVarious.getPathsRelativeToOtherFile(files, d));
		assertTrue(FileVarious.getPathsRelativeToOtherFile(null, d).isEmpty());
		assertTrue(FileVarious.getPathsRelativeToOtherFile(files, new File(d, "missing")).isEmpty());
		assertTrue(FileVarious.getPathsRelativeToOtherFile(new ArrayList<File>(), d).isEmpty());
	}

	// ---------------------------------------------------------------- lists

	@Test
	public void addWithoutRepetitions() {
		File a = new File(d, "a.txt");
		File b = new File(d, "b.log");
		File aAgain = new File(d, "sub/../a.txt");
		List<File> original = new ArrayList<File>(Collections.singletonList(a));
		List<File> result = FileVarious.addWithoutRepetitions(Arrays.asList(aAgain, b, b), original);
		assertEquals(Arrays.asList(a, b), result);
	}

	@Test
	public void addWithoutRepetitionsHandlesNullsAndEmptyLists() {
		File a = new File(d, "a.txt");
		assertEquals(Collections.singletonList(a), FileVarious.addWithoutRepetitions(Collections.singletonList(a), null));
		List<File> original = new ArrayList<File>();
		assertSame(original, FileVarious.addWithoutRepetitions(null, original));
		assertTrue(original.isEmpty());
		assertEquals(Arrays.asList(a), FileVarious.addWithoutRepetitions(Arrays.asList(a, a), new ArrayList<File>()));
	}

	@Test
	public void listFilesReturnsOnlyFilesRecursively() {
		ArrayList<File> found = FileVarious.listFiles(d, null);
		List<String> names = new ArrayList<String>();
		for (File f : found) {
			names.add(f.getName());
		}
		Collections.sort(names);
		assertEquals(Arrays.asList("a.txt", "b.log", "c.txt", "e.log"), names);
		assertTrue(FileVarious.listFiles(new File(d, "missing"), null).isEmpty());
		ArrayList<File> seeded = new ArrayList<File>(Collections.singletonList(new File("seed")));
		assertSame(seeded, FileVarious.listFiles(new File(d, "emptydir"), seeded));
		assertEquals(1, seeded.size());
	}

	@Test
	public void fileListConversions() throws IOException {
		File a = new File(d, "a.txt");
		File missing = new File(d, "missing.txt");
		List<File> files = Arrays.asList(a, missing);
		assertArrayEquals(new String[] { a.getCanonicalPath(), missing.getCanonicalPath() }, FileVarious.filesToStrings(files));
		assertEquals(0, FileVarious.filesToStrings(null).length);
		assertEquals(Arrays.asList(a.getCanonicalPath(), missing.getCanonicalPath()), FileVarious.filesToStringList(files));
		assertTrue(FileVarious.filesToStringList(null).isEmpty());
		assertEquals(FileVarious.filesToStringList(files), FileVarious.filesToPathList(files));
		assertTrue(FileVarious.filesToPathList(null).isEmpty());
		assertEquals(Arrays.asList(a.getCanonicalFile()), FileVarious.removeNullsAndUnexisting(Arrays.asList(a, null, missing)));
		assertTrue(FileVarious.removeNullsAndUnexisting(null).isEmpty());
		assertEquals(a.getCanonicalPath() + ";" + missing.getCanonicalPath(), FileVarious.getStringFromFiles(files, ";"));
		assertEquals("", FileVarious.getStringFromFiles(new ArrayList<File>(), ";"));
		assertEquals(files.size(), FileVarious.getStringListFromFiles(files).size());
	}

	@Test
	public void pathsToFiles() throws IOException {
		String a = new File(d, "a.txt").getPath();
		String missing = new File(d, "missing.txt").getPath();
		assertEquals(2, FileVarious.pathsToFileList(Arrays.asList(a, missing), false).size());
		assertEquals(Collections.singletonList(new File(a).getCanonicalFile()),
				FileVarious.pathsToFileList(Arrays.asList(a, missing), true));
		assertTrue(FileVarious.pathsToFileList(null, true).isEmpty());
	}

	@Test
	public void fileArrayFromStringsStopsAtTheEndMarker() {
		File[] files = FileVarious.getFileArrayFromStrings(new String[] { "a.txt", "dir with space/b.txt", "###_END_SECTION", "ignored" });
		assertEquals(4, files.length);
		assertEquals(new File("a.txt"), files[0]);
		assertEquals(new File("dir with space/b.txt"), files[1]);
		assertNull(files[2]);
		assertNull(files[3]);
		assertEquals(0, FileVarious.getFileArrayFromStrings(new String[0]).length);
	}

	@Test
	public void containedInListAndRemoval() {
		File a = new File(d, "a.txt");
		List<File> files = new ArrayList<File>(Arrays.asList(new File(d, "b.log"), a));
		assertTrue(FileVarious.containedInList(files, new File(d, "sub/../a.txt")));
		assertFalse(FileVarious.containedInList(files, new File(d, "zzz")));
		assertFalse(FileVarious.containedInList(null, a));
		assertTrue(FileVarious.removeFileFromListByCanonicalEquals(files, new File(d, "sub/../a.txt")));
		assertEquals(1, files.size());
		assertFalse(FileVarious.removeFileFromListByCanonicalEquals(files, a));
		assertFalse(FileVarious.removeFileFromListByCanonicalEquals(null, a));
	}

	// ---------------------------------------------------------------- printing helpers

	@Test
	public void separatorAndSpaces() {
		assertEquals(EOL.defaultEol.eol + "  ---", FileVarious.getSeparator("-", 3));
		assertEquals(EOL.defaultEol.eol + "  ", FileVarious.getSeparator("-", 0));
		assertEquals("", FileVarious.getNspacesString(0));
		assertEquals("", FileVarious.getNspacesString(-3));
		assertEquals("     ", FileVarious.getNspacesString(5));
	}

	@Test
	public void headerLinesHaveTheSameWidthAndContainTheTitle() {
		String[][] cases = { { "*", "20", "Title" }, { "=", "21", "Title" }, { "#", "5", "A much longer title than width" },
				{ "-", "0", "" }, { "*", "40", "Even" } };
		for (String[] c : cases) {
			String header = FileVarious.getHeaderString(c[0], Integer.parseInt(c[1]), c[2]);
			assertTrue(header.contains(c[2]));
			String[] lines = header.split("\r?\n");
			List<Integer> widths = new ArrayList<Integer>();
			for (String line : lines) {
				if (!line.isEmpty()) {
					widths.add(line.length());
				}
			}
			assertEquals(3, widths.size());
			assertEquals(header, widths.get(0), widths.get(1));
			assertEquals(header, widths.get(1), widths.get(2));
		}
	}

	// ---------------------------------------------------------------- names

	@Test
	public void insertNumberBeforeExtension() {
		assertEquals("file(1).txt", FileVarious.insertNumberBeforeExtension("file.txt", 1));
		assertEquals("file(3).txt", FileVarious.insertNumberBeforeExtension("file(2).txt", 3));
		assertEquals("archive.tar(7).gz", FileVarious.insertNumberBeforeExtension("archive.tar.gz", 7));
		assertEquals("my file(12).md", FileVarious.insertNumberBeforeExtension("my file.md", 12));
	}

	@Test
	public void insertNumberBeforeMissingExtensionLeavesNoDanglingDot() {
		assertEquals("README(1)", FileVarious.insertNumberBeforeExtension("README", 1));
		assertEquals("README(2)", FileVarious.insertNumberBeforeExtension("README(1)", 2));
	}

	@Test
	public void insertNumberBeforeExtensionOfOddNamesDoesNotThrow() {
		assertEquals("(1).gitignore", FileVarious.insertNumberBeforeExtension(".gitignore", 1));
		assertEquals("file)(1).txt", FileVarious.insertNumberBeforeExtension("file).txt", 1));
	}

	@Test
	public void uniqueNamesSkipExistingFiles() throws IOException {
		File fresh = new File(d, "fresh.txt");
		assertEquals(fresh, FileVarious.uniqueJavaObjFile(fresh));
		assertEquals(fresh.getPath(), FileVarious.getUniqueNameFile(fresh));

		File a = new File(d, "a.txt");
		File first = FileVarious.uniqueJavaObjFile(a);
		assertEquals(new File(d, "a(1).txt"), first);
		write(first, "x");
		assertEquals(new File(d, "a(2).txt"), FileVarious.uniqueJavaObjFile(a));
		assertEquals(new File(d, "a(2).txt").getPath(), FileVarious.getUniqueNameFile(a));
		assertEquals("starting from a numbered name", new File(d, "a(2).txt"), FileVarious.uniqueJavaObjFile(first));
	}

	@Test
	public void uniqueNameOfAFileWithoutExtensionEndsWithADot_legacy() throws IOException {
		File readme = new File(d, "README");
		write(readme, "x");
		assertEquals(new File(d, "README(1).").getPath(), FileVarious.getUniqueNameFile(readme));
	}

	@Test
	public void renamedFileLivesInTheSameFolder() throws Exception {
		File a = new File(d, "a.txt");
		File renamed = FileVarious.getRenamed(a, "new name.txt", false);
		assertEquals(new File(d.getCanonicalPath(), "new name.txt"), renamed);
		assertFalse(renamed.exists());
		File uniqueRenamed = FileVarious.getRenamed(a, "b.log", true);
		assertEquals(new File(d.getCanonicalPath(), "b(1).log"), uniqueRenamed);
	}

	@Test
	public void ensureValidExtension() {
		assertEquals("", FileVarious.ensureValidExtension(null, "txt"));
		assertEquals("file.txt", FileVarious.ensureValidExtension("file", "txt"));
		assertEquals("file.md", FileVarious.ensureValidExtension("file.md", "txt"));
		assertEquals("file.txt", FileVarious.ensureValidExtension("file.", "txt"));
		assertEquals("file.txt", FileVarious.ensureValidExtension("file", null));
		assertEquals("file.txt", FileVarious.ensureValidExtension("file", "   "));
		assertEquals("NewFile.txt", FileVarious.ensureValidExtension("", "txt"));
		assertEquals("NewFile.txt", FileVarious.ensureValidExtension("...", "txt"));
		assertEquals("fallback is lowercased", "file.csv", FileVarious.ensureValidExtension("file", "CSV"));
	}

	@Test
	public void ensureValidExtensionAcceptsAFallbackWithTheDot() {
		assertEquals("file.md", FileVarious.ensureValidExtension("file", ".md"));
		assertEquals("file.txt", FileVarious.ensureValidExtension("file", "."));
	}

	@Test
	public void changeExtension() {
		File withSpaces = new File(d, "my file.txt");
		assertEquals(new File(d, "my file.md").getAbsolutePath(), FileVarious.changeExtension(withSpaces, ".md").getAbsolutePath());
		assertEquals(new File(d, "noext.md").getAbsolutePath(), FileVarious.changeExtension(new File(d, "noext"), ".md").getAbsolutePath());
		assertEquals(new File(d, "a.tar.zip").getAbsolutePath(), FileVarious.changeExtension(new File(d, "a.tar.gz"), ".zip").getAbsolutePath());
		assertEquals("only the last extension changes", new File(d, "a.b").getAbsolutePath(),
				FileVarious.changeExtension(new File(d, "a.c"), ".b").getAbsolutePath());
		assertEquals("removes the extension", new File(d, "a").getAbsolutePath(), FileVarious.changeExtension(new File(d, "a.c"), "").getAbsolutePath());
	}

	@Test
	public void changeExtensionForceDot() {
		File a = new File(d, "a.txt");
		assertEquals(new File(d, "a.md").getAbsolutePath(), FileVarious.changeExtensionForceDot(a, "md").getAbsolutePath());
		assertEquals(new File(d, "a.md").getAbsolutePath(), FileVarious.changeExtensionForceDot(a, ".md").getAbsolutePath());
		assertEquals(new File(d, "a.md").getAbsolutePath(), FileVarious.changeExtensionForceDot(a, "..md").getAbsolutePath());
		assertSame(a, FileVarious.changeExtensionForceDot(a, null));
		assertNull(FileVarious.changeExtensionForceDot(null, "md"));
	}

	@Test
	public void illegalCharsAreRemovedFromExtensions() {
		assertEquals(".txt", FileVarious.removeIllegalCharsFromExtension(".tx!t"));
		assertEquals("txt", FileVarious.removeIllegalCharsFromExtension("tx t"));
		assertEquals(".a.bc", FileVarious.removeIllegalCharsFromExtension(".a.b-c"));
		assertEquals("", FileVarious.removeIllegalCharsFromExtension(""));
		assertEquals("n", FileVarious.removeIllegalCharsFromExtension("ünï"));
		assertEquals("ABC123", FileVarious.removeIllegalCharsFromExtension("ABC123"));
		assertEquals(".", FileVarious.removeIllegalCharsFromExtension("."));
	}

	// ---------------------------------------------------------------- ancestors

	@Test
	public void ancestorsPathString() {
		File c = new File(d, "sub/c.txt");
		String sep = File.separator;
		assertEquals("c.txt", FileVarious.getNAncestorsPath(c, 0));
		assertEquals("sub" + sep + "c.txt", FileVarious.getNAncestorsPath(c, 1));
		assertEquals("d dir" + sep + "sub" + sep + "c.txt", FileVarious.getNAncestorsPath(c, 2));
		assertEquals("", FileVarious.getNAncestorsPath(null, 2));
		assertEquals("missing.txt", FileVarious.getNAncestorsPath(new File(d, "missing.txt"), 3));
	}

	@Test
	public void ancestorOfPathString() {
		File c = new File(d, "sub/c.txt");
		String sep = File.separator;
		assertEquals("", FileVarious.getNlevelsPathString(c, 0));
		assertEquals("c.txt", FileVarious.getNlevelsPathString(c, 1));
		assertEquals("sub" + sep + "c.txt", FileVarious.getNlevelsPathString(c, 2));
		assertEquals("", FileVarious.getNlevelsPathString(new File(d, "missing"), 2));
		assertEquals("", FileVarious.getNlevelsPathString(null, 2));
	}

	@Test
	public void ancestorFiles() {
		File c = new File(d, "sub/c.txt");
		assertEquals(new File(d, "sub").getAbsoluteFile(), FileVarious.getNlevelsAncestor(c, 0));
		assertEquals(new File(d, "sub").getAbsoluteFile(), FileVarious.getNlevelsAncestor(c, 1));
		assertEquals(d.getAbsoluteFile(), FileVarious.getNlevelsAncestor(c, 2));
		assertEquals(tmp.getRoot().getAbsoluteFile(), FileVarious.getNlevelsAncestor(c, 3));
		assertNull(FileVarious.getNlevelsAncestor(null, 1));
		File rootless = new File("single");
		assertEquals("a file with no parent is returned as it is", rootless, FileVarious.getNlevelsAncestor(rootless, 3));
		assertTrue(FileVarious.getNlevelsAncestor(c, 500).exists());
	}

	@Test
	public void ancestorExistence() {
		File c = new File(d, "sub/c.txt");
		assertTrue(FileVarious.doesAncestorNlevelsExist(c, 2));
		assertFalse(FileVarious.doesAncestorNlevelsExist(new File(d, "missing"), 2));
		assertFalse(FileVarious.doesAncestorNlevelsExist(null, 2));
	}

	@Test
	public void searchAncestorByName() {
		File e = new File(d, "sub/deep/e.log");
		assertEquals(new File(d, "sub"), FileVarious.searchUntilAncestorMatch(e, "sub", true));
		assertEquals(new File(d, "sub/deep"), FileVarious.searchUntilAncestorMatch(e, "ee", false));
		assertEquals("no match returns the son", e, FileVarious.searchUntilAncestorMatch(e, "zzz", true));
		assertEquals("exact does not match part of name", e, FileVarious.searchUntilAncestorMatch(e, "su", true));
		assertTrue(FileVarious.hasParent(e, "sub", true));
		assertTrue(FileVarious.hasParent(e, "d di", false));
		assertFalse(FileVarious.hasParent(e, "d di", true));
		assertFalse(FileVarious.hasParent(e, "e.log", true));
		assertFalse(FileVarious.hasParent(null, "x", true));
	}

	@Test
	public void containmentInDirectory() {
		File c = new File(d, "sub/c.txt");
		assertTrue(FileVarious.checkIfContainedInDir(c, d));
		assertTrue(FileVarious.checkIfContainedInDir(c, new File(d, "sub")));
		assertFalse("a folder is not contained in itself", FileVarious.checkIfContainedInDir(d, d));
		assertFalse(FileVarious.checkIfContainedInDir(new File(tmp.getRoot(), "d dir2/x.txt"), d));
	}

	@Test
	public void fileSonOfDir() {
		File e = new File(d, "sub/deep/e.log");
		assertTrue(FileVarious.isFileSonOfDir(e, d));
		assertTrue(FileVarious.isFileSonOfDir(e, new File(d, "sub/../sub")));
		assertFalse(FileVarious.isFileSonOfDir(d, d));
		assertFalse(FileVarious.isFileSonOfDir(new File(d, "a.txt"), new File(d, "sub")));
		assertFalse(FileVarious.isFileSonOfDir(null, d));
		assertFalse(FileVarious.isFileSonOfDir(e, null));
	}

	@Test
	public void directoryReplacementInPath() throws Exception {
		FileVarious various = new FileVarious();
		File sub = new File(d, "sub");
		File direct = new File(sub, "c.txt");
		assertEquals(new File(d.getCanonicalPath() + File.separator + "renamed" + File.separator + "c.txt"),
				various.replaceDirOccurrencesInPath(direct, sub, "renamed"));
		File nested = new File(sub, "deep/e.log");
		assertEquals(new File(d.getCanonicalPath() + File.separator + "renamed" + File.separator + "deep" + File.separator + "e.log"),
				various.replaceDirOccurrencesInPath(nested, sub, "renamed"));
		File outside = new File(tmp.getRoot(), "other.txt");
		assertSame(outside, various.replaceDirOccurrencesInPath(outside, sub, "renamed"));
		assertSame(direct, various.replaceDirOccurrencesInPath(direct, new File(sub, "c.txt"), "x"));
		assertNull(various.replaceDirOccurrencesInPath(null, sub, "x"));
	}

	@Test
	public void rootValidation() {
		String root = File.listRoots()[0].getAbsolutePath();
		assertTrue(FileVarious.isPathRootValid(root + "anything"));
		assertFalse(FileVarious.isPathRootValid("relative/path"));
		assertFalse(FileVarious.isPathRootValid(""));
	}

	// ---------------------------------------------------------------- counting

	@Test
	public void countFiles() {
		assertEquals(4, FileVarious.countFilesInDirectory(d, false));
		assertEquals(7, FileVarious.countFilesInDirectory(d, true));
		assertEquals(0, FileVarious.countFilesInDirectory(new File(d, "emptydir"), true));
	}

	@Test
	public void countFilesByExtensionAlsoFiltersSubfolders() {
		ArrayList<String> txt = new ArrayList<String>(Arrays.asList(".txt"));
		assertEquals(2, FileVarious.countFilesInDirectory(d, false, txt));
		assertEquals(2 + 3, FileVarious.countFilesInDirectory(d, true, txt));
		ArrayList<String> logUpper = new ArrayList<String>(Arrays.asList(".LOG"));
		assertEquals("case is ignored", 2, FileVarious.countFilesInDirectory(d, false, logUpper));
		assertEquals(0, FileVarious.countFilesInDirectory(d, false, new ArrayList<String>()));
	}

	@Test
	public void countUntilLimit() {
		assertEquals(4, FileVarious.countFilesUntilLimit(d, false, 100));
		assertEquals(7, FileVarious.countFilesUntilLimit(d, true, 100));
		assertTrue(FileVarious.checkIfContainsMoreFilesThanMax(d, false, 3));
		assertFalse(FileVarious.checkIfContainsMoreFilesThanMax(d, false, 4));
		assertTrue(FileVarious.checkIfContainsMoreFilesThanMax(d, true, 6));
		assertFalse(FileVarious.checkIfContainsMoreFilesThanMax(d, true, 7));
		assertFalse(FileVarious.checkIfContainsMoreFilesThanMax(new File(d, "emptydir"), true, 0));
	}

	// ---------------------------------------------------------------- real files

	@Test
	public void binaryDetectionTrustsTheContentType() throws IOException {
		File txt = new File(d, "a.txt");
		assertFalse(FileVarious.isBinaryFile(txt));
		File unknown = new File(d, "file.zzzunknownext");
		write(unknown, "x");
		assertFalse("unknown type is assumed text", FileVarious.isBinaryFile(unknown));
	}

	@Test
	public void clearContent() throws IOException {
		File a = new File(d, "a.txt");
		assertTrue(FileVarious.clearFileContent(a.getPath()));
		assertEquals(0, a.length());
		assertTrue(a.exists());
		assertFalse("missing folder: nothing can be created", FileVarious.clearFileContent(new File(d, "nodir/x.txt").getPath()));
	}

	@Test
	public void clearContentCreatesMissingFiles_legacy() {
		File fresh = new File(d, "created.txt");
		assertTrue(FileVarious.clearFileContent(fresh.getPath()));
		assertTrue(fresh.exists());
	}

	@Test
	public void renameFirstLetterToUppercase() {
		assertTrue(FileVarious.renameFileUpperFirst(new File(d, "b.log")));
		List<String> names = Arrays.asList(d.list());
		assertTrue(names.toString(), names.contains("B.log"));
		assertFalse(names.toString(), names.contains("b.log"));
		assertFalse(FileVarious.renameFileUpperFirst(new File(d, "does not exist.txt")));
	}
}
