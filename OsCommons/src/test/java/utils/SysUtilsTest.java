package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import os.commons.om.JVMInfo;
import os.commons.om.OSinfo;
import os.commons.utils.SysUtils;

/**
 * System helpers: commands the application builds to start itself (they decide how a restart or a plugin launch behaves),
 * lookups in folder trees, process output reading, and what the JVM says about itself. Nothing here touches autostart,
 * the registry or the browser.
 */
public class SysUtilsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static boolean windows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}

	// ------------------------------------------------------------------ what the OS says

	@Test
	public void osDetectionAgreesWithTheJvm() {
		OSinfo os = SysUtils.retrieveOS();
		assertNotNull(os);
		assertEquals(windows(), os.isWindows());
		assertEquals(System.getProperty("os.name").toLowerCase().contains("linux"), os.isLinux());
		assertEquals(os.isWindows() ? "cmd" : "bash", SysUtils.getShellOsBased());
		assertEquals(System.getProperty("os.arch"), os.getOsArch());
		assertNotNull(SysUtils.getOsInfoLazy());
		assertTrue(SysUtils.OS_INFO_LAZY.isWindows() == os.isWindows());
	}

	@Test
	public void architectureAndMemoryAreConsistent() {
		assertTrue(SysUtils.is64arch() != SysUtils.is32arch());
		assertEquals(SysUtils.is64arch() ? 64 : 32, SysUtils.findIf32or64());
		assertTrue(SysUtils.getMaxMemoryAdmitted() > 0);
		assertEquals(System.getProperty("java.version"), SysUtils.getCurrentJVMversion());
		assertNotNull(SysUtils.getCurrentSystemEncoding());
		assertEquals(new File(System.getProperty("java.home")).getAbsoluteFile(), SysUtils.getCurrentJavaHome().getAbsoluteFile());
	}

	@Test
	public void jvmInfoDescribesTheRunningJvm() {
		JVMInfo info = SysUtils.getJVMinfo();
		assertNotNull("an empty holder, filled by the caller", info);
	}

	// ------------------------------------------------------------------ command lines the application builds

	@Test
	public void javaCommandShowsTheShellOrRunsWithoutOne() {
		if (windows()) {
			assertEquals("java ", SysUtils.getJavaCommand(true, "anything"));
			assertEquals("start \"My App\" \"javaw\" ", SysUtils.getJavaCommand(false, "My App"));
			assertEquals("a missing title is an empty one", "start \"\" \"javaw\" ", SysUtils.getJavaCommand(false, null));
		} else {
			assertEquals("java ", SysUtils.getJavaCommand(true, "x"));
			assertEquals("javaw ", SysUtils.getJavaCommand(false, "x"));
		}
	}

	@Test
	public void jarCommandCarriesJvmArgumentsJarAndQuotedFiles() throws IOException {
		File a = tmp.newFile("my file.txt");
		File b = tmp.newFile("other.txt");
		String cmd = SysUtils.startJarCommand("-Xmx1536m -Dx=y", "ScreepT.jar", new File[] { a, b }, true);
		assertTrue(cmd, cmd.startsWith("java -Xmx1536m -Dx=y -jar ScreepT.jar"));
		assertTrue("files with spaces are quoted: " + cmd, cmd.contains(" \"" + a.getAbsolutePath() + "\""));
		assertTrue(cmd.endsWith(" \"" + b.getAbsolutePath() + "\""));
		assertTrue("order of the files is kept", cmd.indexOf(a.getAbsolutePath()) < cmd.indexOf(b.getAbsolutePath()));

		String noFiles = SysUtils.startJarCommand("-Xmx512m", "ScreepT.jar", new File[0], true);
		assertTrue(noFiles, noFiles.contains("-Xmx512m"));
		assertTrue(noFiles, noFiles.contains("-jar"));
		assertTrue(noFiles, noFiles.contains("ScreepT.jar"));
		assertFalse(noFiles, noFiles.contains("\""));
		assertEquals(noFiles, SysUtils.startJarCommand("-Xmx512m", "ScreepT.jar", (File[]) null, true));
	}

	@Test
	public void classCommandPutsTheClassPathBeforeTheMainClass() throws IOException {
		File f = tmp.newFile("arg.txt");
		String cmd = SysUtils.startClassCommand("-Xms64m", "lib/*", "pkg.Main", new File[] { f }, true);
		assertTrue(cmd, cmd.startsWith("java -Xms64m -cp lib/* pkg.Main"));
		assertTrue(cmd, cmd.endsWith(" \"" + f.getAbsolutePath() + "\""));
		String plain = SysUtils.startClassCommand("-Xms64m", "lib/*", "pkg.Main", null, true);
		assertTrue(plain, plain.contains("-cp lib/*"));
		assertTrue(plain, plain.contains("pkg.Main"));
	}

	@Test
	public void shortcutNamesCarryTheVersion() {
		assertEquals("ScreepT_0.9.91.lnk", SysUtils.getVersionedFileName(new File("C:/apps/ScreepT.exe"), "0.9.91", ".lnk"));
		assertEquals("ScreepT.lnk", SysUtils.getVersionedFileName(new File("C:/apps/ScreepT.exe"), "", ".lnk"));
		assertEquals("my.tool_2.lnk", SysUtils.getVersionedFileName(new File("/x/my.tool.jar"), "2", ".lnk"));
	}

	// ------------------------------------------------------------------ searching a tree

	@Test
	public void findsAFileInTheRootOrInTheFirstFolderCaseInsensitively() throws IOException {
		File root = tmp.newFolder("root");
		new File(root, "docs/deep").mkdirs();
		File target = new File(root, "docs/deep/Needle.TXT");
		assertTrue(target.createNewFile());
		assertEquals(target, SysUtils.findFileInsideRoot(root, "needle.txt", false));
		assertNull("case sensitive search does not match a different case", SysUtils.findFileInsideRoot(root, "needle.txt", true));
		assertEquals(target, SysUtils.findFileInsideRoot(root, " Needle.TXT ", true));
		assertNull(SysUtils.findFileInsideRoot(root, null, false));
		assertNull(SysUtils.findFileInsideRoot(root, "absent.txt", false));
		assertEquals(new File(root, "docs/deep"), SysUtils.findDirectoryInsideRoot(root, "deep", true));
		assertEquals(new File(root, "docs"), SysUtils.findDirectoryInsideRoot(root, "DOCS", false));
		assertNull(SysUtils.findDirectoryInsideRoot(root, null, false));
	}

	@Test
	public void findsAFileThatIsNotInTheFirstFolder() throws IOException {
		File root = tmp.newFolder("root2");
		new File(root, "a-empty").mkdirs();
		new File(root, "b-has").mkdirs();
		File target = new File(root, "b-has/Needle.txt");
		assertTrue(target.createNewFile());
		File atRoot = new File(root, "z-rootfile.txt");
		assertTrue(atRoot.createNewFile());
		assertEquals(target, SysUtils.findFileInsideRoot(root, "Needle.txt", true));
		assertEquals(atRoot, SysUtils.findFileInsideRoot(root, "z-rootfile.txt", true));
		assertEquals(new File(root, "b-has"), SysUtils.findDirectoryInsideRoot(root, "b-has", true));
	}

	@Test
	public void classPathListsEveryJarOfAFolderTree() throws IOException {
		Assume.assumeTrue(windows());
		File lib = tmp.newFolder("lib");
		new File(lib, "sub").mkdirs();
		assertTrue(new File(lib, "one.jar").createNewFile());
		assertTrue(new File(lib, "sub/two.jar").createNewFile());
		assertTrue(new File(lib, "readme.txt").createNewFile());
		String cp = SysUtils.getClassPathFromAllJarsFoundInRoot(lib.getAbsolutePath(), false);
		assertTrue(cp, cp.startsWith("\"") && cp.endsWith("\""));
		assertTrue(cp, cp.contains("one.jar"));
		assertTrue(cp, cp.contains("two.jar"));
		assertTrue(cp, cp.contains(";"));
		assertFalse(cp, cp.contains("readme"));
		assertTrue(SysUtils.getClassPathFromAllJarsFoundInRoot(lib.getAbsolutePath(), true).startsWith("\"./"));
	}

	// ------------------------------------------------------------------ processes

	@Test
	public void processOutputIsReadLineByLine() {
		byte[] data = "first\nsecond\r\nthird".getBytes(StandardCharsets.UTF_8);
		List<String> lines = SysUtils.readProcessOutputLines(new ByteArrayInputStream(data));
		assertEquals(Arrays.asList("first", "second", "third"), lines);
		String text = SysUtils.readProcessOutput(new ByteArrayInputStream(data));
		String sep = System.getProperty("line.separator");
		assertEquals("first" + sep + "second" + sep + "third" + sep, text);
		assertEquals("", SysUtils.readProcessOutput(new ByteArrayInputStream(new byte[0])));
	}

	@Test
	public void aCommandIsLaunchedThroughTheShellAndItsOutputCanBeRead() throws Exception {
		Process p = SysUtils.launchCommand("echo hello-from-the-shell");
		String out = SysUtils.readProcessOutput(p.getInputStream());
		assertEquals(0, p.waitFor());
		assertEquals("hello-from-the-shell", out.trim());
		Process failing = SysUtils.launchCommand("exit 3");
		assertEquals(3, failing.waitFor());
	}

	@Test
	public void javaOfThisJvmReportsItsVersionFromTheInstalledFolder() throws IOException {
		File bin = new File(System.getProperty("java.home"), "bin");
		String version;
		try {
			version = SysUtils.getJavaVersionFromBin(bin.getAbsolutePath());
		} catch (IOException noReleaseFile) {
			Assume.assumeNoException("this JVM folder has no release file", noReleaseFile);
			return;
		}
		assertTrue("a version like 1.8.0_162 or 17.0.2: " + version, version.matches("[0-9]+([._][0-9]+)*.*"));
	}
}
