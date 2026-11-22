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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JTextArea;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.files.om.DeleteFilesByExtensionDTO;
import various.common.light.utility.string.StringWorker.EOL;

/**
 * File read/write/copy/delete helpers of {@link FileWorker}. Everything happens inside a JUnit TemporaryFolder.
 */
public class FileWorkerTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File work;

	@Before
	public void createWorkFolder() throws IOException {
		work = tmp.newFolder("work dir");
	}

	private static File write(File f, String text) throws IOException {
		Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	private static String read(File f) throws IOException {
		return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
	}

	private static List<String> sortedNames(File dir) {
		List<String> names = new ArrayList<String>(Arrays.asList(dir.list()));
		Collections.sort(names);
		return names;
	}

	// ---------------------------------------------------------------- reading

	@Test
	public void readFileNormalizesLineEndingsAndAppendsOneTerminator() throws IOException {
		File f = write(new File(work, "mixed.txt"), "a\r\nb\rc\nd");
		assertEquals("a\nb\nc\nd\n", FileWorker.readFileAsString(f, "UTF-8", EOL.LF));
		assertEquals("a\r\nb\r\nc\r\nd\r\n", FileWorker.readFileAsString(f, "UTF-8", EOL.CRLF));
		assertEquals("a" + EOL.defaultEol.eol + "b" + EOL.defaultEol.eol + "c" + EOL.defaultEol.eol + "d" + EOL.defaultEol.eol,
				FileWorker.readFileAsString(f, "UTF-8"));
	}

	@Test
	public void readFileDoesNotPreserveTheMissingFinalNewLine_legacy() throws IOException {
		assertEquals("a\n", FileWorker.readFileAsString(write(new File(work, "1.txt"), "a"), "UTF-8", EOL.LF));
		assertEquals("a\n", FileWorker.readFileAsString(write(new File(work, "2.txt"), "a\n"), "UTF-8", EOL.LF));
		assertEquals("a\n\n", FileWorker.readFileAsString(write(new File(work, "3.txt"), "a\n\n"), "UTF-8", EOL.LF));
	}

	@Test
	public void readEmptyAndNewLineOnlyFiles() throws IOException {
		assertEquals("", FileWorker.readFileAsString(write(new File(work, "empty.txt"), ""), "UTF-8", EOL.LF));
		assertEquals("\n", FileWorker.readFileAsString(write(new File(work, "nl.txt"), "\n"), "UTF-8", EOL.LF));
	}

	@Test
	public void readFileWithEncodings() throws IOException {
		File f = write(new File(work, "unicode ü.txt"), "café 中文");
		assertEquals("café 中文\n", FileWorker.readFileAsString(f, "UTF-8", EOL.LF));
		// the same bytes read as ISO-8859-1 give mojibake, not an error
		assertNotNullAndDifferent("café 中文\n", FileWorker.readFileAsString(f, "ISO-8859-1", EOL.LF));
	}

	private static void assertNotNullAndDifferent(String notExpected, String actual) {
		assertNotNull(actual);
		assertFalse(notExpected.equals(actual));
	}

	@Test
	public void readFileWithUnknownEncodingFallsBackToTheDefaultOne() throws IOException {
		File f = write(new File(work, "ascii.txt"), "plain ascii");
		assertEquals("plain ascii\n", FileWorker.readFileAsString(f, "no-such-encoding", EOL.LF));
		assertEquals("plain ascii\n", FileWorker.readFileAsString(f, null, EOL.LF));
	}

	@Test
	public void readMissingOrNullFileIsAnIoError() {
		for (File f : new File[] { null, new File(work, "missing.txt") }) {
			try {
				FileWorker.readFileAsString(f);
				fail("IOException expected for " + f);
			} catch (IOException expected) {
				// ok
			}
		}
	}

	@Test
	public void moreThanNLines() throws IOException {
		File three = write(new File(work, "three.txt"), "1\n2\n3");
		assertTrue(FileWorker.checkIfHasMoreThanNLines(three, 2));
		assertFalse(FileWorker.checkIfHasMoreThanNLines(three, 3));
		assertFalse(FileWorker.checkIfHasMoreThanNLines(three, 4));
		assertTrue(FileWorker.checkIfHasMoreThanNLines(three, 0));
		File empty = write(new File(work, "empty.txt"), "");
		assertFalse(FileWorker.checkIfHasMoreThanNLines(empty, 0));
		assertFalse(FileWorker.checkIfHasMoreThanNLines(null, 0));
		assertFalse(FileWorker.checkIfHasMoreThanNLines(new File(work, "missing.txt"), 0));
		File crlf = write(new File(work, "crlf.txt"), "1\r\n2\r\n3\r\n4\r\n");
		assertTrue(FileWorker.checkIfHasMoreThanNLines(crlf, 3));
		assertFalse(FileWorker.checkIfHasMoreThanNLines(crlf, 4));
	}

	@Test
	public void textAreaComparedWithFileContent() throws IOException {
		File f = write(new File(work, "cmp.txt"), "hello");
		assertTrue(FileWorker.isTextAreaEqualToFileContent(new JTextArea("hello" + EOL.defaultEol.eol), f));
		assertFalse(FileWorker.isTextAreaEqualToFileContent(new JTextArea("hello"), f));
		assertFalse(FileWorker.isTextAreaEqualToFileContent(null, f));
		assertFalse(FileWorker.isTextAreaEqualToFileContent(new JTextArea("x"), null));
		assertFalse(FileWorker.isTextAreaEqualToFileContent(new JTextArea("x"), new File(work, "missing")));
	}

	// ---------------------------------------------------------------- writing

	@Test
	public void writeStringToNewFile() throws IOException {
		File f = new File(work, "out file.txt");
		assertTrue(FileWorker.writeStringToFile("hello é", f, false));
		assertEquals("hello é", FileWorker.readFileAsString(f, System.getProperty("file.encoding"), EOL.LF).replace("\n", ""));
	}

	@Test
	public void writeStringOverwritesWhenAskedTo() throws IOException {
		File f = write(new File(work, "o.txt"), "old content that is longer");
		assertTrue(FileWorker.writeStringToFile("new", f, true));
		assertEquals("new", read(f));
		assertEquals(Arrays.asList("o.txt"), sortedNames(work));
	}

	@Test
	public void writeStringWithoutOverwriteKeepsTheOriginalAndCreatesANumberedFile() throws IOException {
		File f = write(new File(work, "o.txt"), "keep me");
		assertTrue(FileWorker.writeStringToFile("second", f, false));
		assertEquals("keep me", read(f));
		assertEquals(Arrays.asList("o(1).txt", "o.txt"), sortedNames(work));
		assertEquals("second", read(new File(work, "o(1).txt")));
	}

	@Test
	public void writeStringConvertsLineEndings() throws IOException {
		File f = new File(work, "eol.txt");
		assertTrue(FileWorker.writeStringToFile("a\nb\r\nc", f, true, EOL.CRLF));
		assertEquals("a\r\nb\r\nc", read(f));
		assertTrue(FileWorker.writeStringToFile("a\r\nb", f, true, EOL.LF));
		assertEquals("a\nb", read(f));
		assertTrue(FileWorker.writeStringToFile("a\r\nb", f, true, (EOL) null));
		assertEquals("a\r\nb", read(f));
	}

	@Test
	public void writeEmptyStringCreatesEmptyFile() throws IOException {
		File f = new File(work, "empty.txt");
		assertTrue(FileWorker.writeStringToFile("", f, true));
		assertTrue(f.exists());
		assertEquals(0, f.length());
	}

	@Test
	public void writeIntoMissingFolderFails() {
		try {
			FileWorker.writeStringToFile("x", new File(work, "no/such/dir/x.txt"), true);
			fail("IOException expected");
		} catch (IOException expected) {
			// ok
		}
		assertFalse(new File(work, "no").exists());
	}

	@Test
	public void appendString() throws IOException {
		File f = write(new File(work, "app.txt"), "a");
		assertTrue(FileWorker.appendStringToFile("b", f, false, null));
		assertEquals("ab", read(f));
		assertTrue(FileWorker.appendStringToFile("\nc\r\nd", f, false, EOL.LF));
		assertEquals("ab\nc\nd", read(f));
	}

	@Test
	public void appendStringCreatesMissingFileOnlyWhenAllowed() throws IOException {
		File f = new File(work, "new.txt");
		assertFalse(FileWorker.appendStringToFile("x", f, false, null));
		assertFalse(f.exists());
		assertTrue(FileWorker.appendStringToFile("x", f, true, null));
		assertEquals("x", read(f));
		assertFalse(FileWorker.appendStringToFile("x", null, true, null));
		assertFalse(FileWorker.appendStringToFile("x", new File(work, "no dir/x.txt"), true, null));
	}

	// ---------------------------------------------------------------- copy

	@Test
	public void copyFileCreatesTheDestinationFolders() throws IOException {
		File src = write(new File(work, "src.txt"), "content");
		File dest = new File(work, "a/b/c/dest copy.txt");
		assertEquals(dest, FileWorker.copyFile(src, dest, false));
		assertEquals("content", read(dest));
		assertEquals("content", read(src));
	}

	@Test
	public void copyFileOverwritesOrRenames() throws IOException {
		File src = write(new File(work, "src.txt"), "new");
		File dest = write(new File(work, "dest.txt"), "old");
		File result = FileWorker.copyFile(src, dest, false);
		assertEquals(new File(work, "dest(1).txt"), result);
		assertEquals("old", read(dest));
		assertEquals("new", read(result));
		assertEquals(dest, FileWorker.copyFile(src, dest, true));
		assertEquals("new", read(dest));
	}

	@Test
	public void copyOfMissingSourceReturnsNull() {
		assertNull(FileWorker.copyFile(new File(work, "missing.txt"), new File(work, "d.txt"), false));
	}

	@Test
	public void renameJavaObjFile() throws IOException {
		File fresh = new File(work, "fresh.txt");
		assertEquals(fresh, FileWorker.renameJavaObjFile(fresh));
		write(fresh, "x");
		File first = FileWorker.renameJavaObjFile(fresh);
		assertEquals(new File(work, "fresh(1).txt"), first);
		write(first, "x");
		assertEquals(new File(work, "fresh(2).txt"), FileWorker.renameJavaObjFile(fresh));
		assertFalse(new File(work, "fresh(2).txt").exists());
	}

	@Test
	public void copyDirectoryContentsCopiesFilesAndFolders() throws IOException {
		File src = tmp.newFolder("src");
		write(new File(src, "a.txt"), "a");
		File sub = new File(src, "sub");
		assertTrue(sub.mkdir());
		write(new File(sub, "b.txt"), "b");
		File dest = tmp.newFolder("dest");
		write(new File(dest, "a.txt"), "old");
		FileWorker.copyDirectoryContents(src, dest);
		assertEquals("a", read(new File(dest, "a.txt")));
		assertEquals("b", read(new File(dest, "sub/b.txt")));
	}

	@Test
	public void copyDirectoryContentsFilteredByExtension() throws IOException {
		assumeTrue("uses a backslash separator", File.separatorChar == '\\');
		File src = tmp.newFolder("src2");
		write(new File(src, "a.txt"), "a");
		write(new File(src, "b.log"), "b");
		File dest = tmp.newFolder("dest2");
		FileWorker.copyDirectoryContents(src, dest, true, "txt");
		assertEquals(Arrays.asList("a.txt"), sortedNames(dest));
		FileWorker.copyDirectoryContents(src, dest, false, "");
		assertEquals(Arrays.asList("a(1).txt", "a.txt", "b.log"), sortedNames(dest));
	}

	@Test
	public void copyDirStructureCopiesOnlyFolders() throws IOException {
		File src = tmp.newFolder("tree");
		assertTrue(new File(src, "x/y").mkdirs());
		assertTrue(new File(src, "z").mkdirs());
		write(new File(src, "x/file.txt"), "f");
		File dest = tmp.newFolder("dest");
		assertTrue(FileWorker.copyDirStructure(src, dest, true));
		File copy = new File(dest, "tree");
		assertTrue(new File(copy, "x/y").isDirectory());
		assertTrue(new File(copy, "z").isDirectory());
		assertFalse(new File(copy, "x/file.txt").exists());
		assertTrue(FileWorker.copyDirStructure(src, dest, true));
		assertTrue("second copy gets a unique root", new File(dest, "tree(1)/x/y").isDirectory());
	}

	@Test
	public void copyDirStructureRejectsMissingSourceAndDestinationInsideSource() throws IOException {
		File src = tmp.newFolder("tree2");
		try {
			FileWorker.copyDirStructure(new File(work, "missing"), work, true);
			fail();
		} catch (IOException expected) {
			// ok
		}
		File inside = new File(src, "inner");
		assertTrue(inside.mkdir());
		try {
			FileWorker.copyDirStructure(src, inside, true);
			fail();
		} catch (IOException expected) {
			// ok: infinite recursion avoided
		}
	}

	@Test
	public void copyFileIntoDirOverwriting() throws IOException {
		File src = write(new File(work, "doc.txt"), "v2");
		File dest = tmp.newFolder("target");
		write(new File(dest, "doc.txt"), "v1");
		File result = FileWorker.copyFileIntoDir(src, dest, true, tmp.newFolder("temp").getPath() + File.separator);
		assertEquals(new File(dest.getAbsolutePath(), "doc.txt"), result);
		assertEquals("v2", read(new File(dest, "doc.txt")));
	}

	@Test
	public void copyFileIntoDirKeepingTheExistingFile() throws IOException {
		File src = write(new File(work, "doc.txt"), "v2");
		File dest = tmp.newFolder("target2");
		write(new File(dest, "doc.txt"), "v1");
		File result = FileWorker.copyFileIntoDir(src, dest, false, tmp.newFolder("temp2").getPath() + File.separator);
		assertNotNull(result);
		assertEquals("v1", read(new File(dest, "doc.txt")));
		assertEquals("v2", read(new File(dest, "doc(1).txt")));
		assertEquals(new File(dest.getAbsolutePath(), "doc(1).txt"), result);
	}

	@Test
	public void copyFileIntoDirCreatesTheDestinationAndRejectsInvalidInput() throws IOException {
		File src = write(new File(work, "doc.txt"), "v");
		File dest = new File(work, "new target");
		assertNotNull(FileWorker.copyFileIntoDir(src, dest, true, work.getPath() + File.separator));
		assertEquals("v", read(new File(dest, "doc.txt")));
		assertNull(FileWorker.copyFileIntoDir(null, dest, true, ""));
		assertNull(FileWorker.copyFileIntoDir(src, null, true, ""));
		assertNull(FileWorker.copyFileIntoDir(new File(work, "missing"), dest, true, ""));
	}

	@Test
	public void moveDirectory() throws IOException {
		File src = tmp.newFolder("to move");
		write(new File(src, "f.txt"), "f");
		File target = new File(tmp.getRoot(), "moved");
		FileWorker.moveDirectory(src, target, StandardCopyOption.ATOMIC_MOVE);
		assertFalse(src.exists());
		assertEquals("f", read(new File(target, "f.txt")));
	}

	// ---------------------------------------------------------------- delete

	private File treeWithFilesAndFolders(String name) throws IOException {
		File root = tmp.newFolder(name);
		write(new File(root, "a.txt"), "a");
		write(new File(root, "b.log"), "b");
		File sub = new File(root, "sub");
		assertTrue(sub.mkdir());
		write(new File(sub, "c.txt"), "c");
		assertTrue(new File(sub, "deeper").mkdir());
		write(new File(sub, "deeper/d.log"), "d");
		return root;
	}

	@Test
	public void deleteRecursiveKeepsTheRootFolder() throws IOException {
		File root = treeWithFilesAndFolders("del1");
		assertTrue(FileWorker.deleteDirContentRecursive(root));
		assertTrue(root.isDirectory());
		assertEquals(0, root.list().length);
	}

	@Test
	public void deleteIncludingRootRemovesEverything() throws IOException {
		File root = treeWithFilesAndFolders("del2");
		assertTrue(FileWorker.deleteDirContentInclRoot(root));
		assertFalse(root.exists());
	}

	@Test
	public void deleteOfMissingFolderIsHarmless() {
		assertTrue(FileWorker.deleteDirContentRecursive(new File(work, "missing")));
		assertTrue(FileWorker.deleteDirContentInclRoot(new File(work, "missing")));
		assertTrue(FileWorker.deleteDirContent(new File(work, "missing")));
	}

	@Test
	public void deleteDirContentRemovesFilesButNotFolders() throws IOException {
		File root = treeWithFilesAndFolders("del3");
		assertTrue(FileWorker.deleteDirContent(root));
		assertEquals(Arrays.asList("sub"), sortedNames(root));
		assertTrue(new File(root, "sub/c.txt").exists());
	}

	@Test
	public void emptyDirContentFolderOnly() throws IOException {
		File root = treeWithFilesAndFolders("del4");
		assertTrue(FileWorker.emptyDirContentFolderOnly(root));
		assertEquals(Arrays.asList("sub"), sortedNames(root));
		assertTrue(new File(root, "sub/deeper").isDirectory());
		assertEquals(0, new File(root, "sub/deeper").list().length);
		assertFalse(FileWorker.emptyDirContentFolderOnly(new File(work, "missing")));
	}

	@Test
	public void emptyDirContentByExtensionDeletesRecursively() throws IOException {
		File root = treeWithFilesAndFolders("del5");
		DeleteFilesByExtensionDTO result = FileWorker.emptyDirContentByExt(root, "log");
		assertTrue(result.isEsit());
		assertFalse(result.isNothingFound());
		assertFalse(new File(root, "b.log").exists());
		assertFalse(new File(root, "sub/deeper/d.log").exists());
		assertTrue(new File(root, "a.txt").exists());
		assertTrue(new File(root, "sub/c.txt").exists());
	}

	@Test
	public void emptyDirContentByExtensionReportsWhatWasNotFound() throws IOException {
		File root = treeWithFilesAndFolders("del6");
		DeleteFilesByExtensionDTO result = FileWorker.emptyDirContentByExt(root, "zzz");
		assertTrue(result.isNothingFound());
		assertTrue(result.getNotFoundExtensionResult().contains("zzz"));
		assertEquals(Arrays.asList("a.txt", "b.log", "sub"), sortedNames(root));
	}

	@Test
	public void emptyDirContentByExtensionSupportsListsWithSpaces() throws IOException {
		File root = treeWithFilesAndFolders("del7");
		DeleteFilesByExtensionDTO result = FileWorker.emptyDirContentByExt(root, " txt , log ");
		assertFalse(result.isNothingFound());
		assertFalse(new File(root, "a.txt").exists());
		assertFalse(new File(root, "b.log").exists());
		assertFalse(new File(root, "sub/c.txt").exists());
	}

	@Test
	public void emptyDirContentByExtensionRejectsMissingRoot() {
		assertFalse(FileWorker.emptyDirContentByExt(null, "txt").isEsit());
		assertFalse(FileWorker.emptyDirContentByExt(new File(work, "missing"), "txt").isEsit());
	}

	// ---------------------------------------------------------------- listing / misc

	@Test
	public void allContentListsFilesOfEverySubfolder() throws IOException {
		File root = treeWithFilesAndFolders("list1");
		List<String> names = new ArrayList<String>();
		for (File f : FileWorker.getAllContent(root, CustomFileFilters.ALL_FOUND)) {
			names.add(f.getName());
		}
		Collections.sort(names);
		assertEquals(Arrays.asList("a.txt", "b.log", "c.txt", "d.log"), names);
		assertEquals(4, FileWorker.getAllContent(root, CustomFileFilters.FILE_ONLY).size());
		assertEquals("a pure directory filter never accepts files", 0, FileWorker.getAllContent(root, CustomFileFilters.DIR_ONLY).size());
	}

	@Test
	public void mkDirsAndCleanup() throws IOException {
		String a = new File(work, "m1/inner").getPath();
		String b = new File(work, "m 2").getPath();
		FileWorker.mkDirs(a, b);
		FileWorker.mkDirs((String[]) null);
		assertTrue(new File(a).isDirectory());
		assertTrue(new File(b).isDirectory());
		write(new File(a, "x.txt"), "x");
		write(new File(b, "y.txt"), "y");
		FileWorker.cleanupDirs(b);
		FileWorker.cleanupDirs((String[]) null);
		assertEquals(0, new File(b).list().length);
		assertTrue("not cleaned", new File(a, "x.txt").exists());
		assertArrayEquals(new String[0], new File(b).list());
	}
}
