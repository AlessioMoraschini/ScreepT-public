package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import os.commons.utils.JvmLocator;
import os.commons.utils.SysUtils;

/** How ScreepT recognises a Java installation: a folder with bin/java and, when present, the version of its release file. */
public class JvmLocatorTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File fakeHome(String name, String releaseLine) throws Exception {
		File home = tmp.newFolder(name);
		File bin = new File(home, "bin");
		assertTrue(bin.mkdirs());
		assertTrue(new File(bin, "java").createNewFile());
		if (releaseLine != null) {
			Files.write(new File(home, "release").toPath(), (releaseLine + "\n").getBytes(StandardCharsets.ISO_8859_1));
		}
		return home;
	}

	@Test
	public void aJavaHomeHasTheJavaLauncherInBin() throws Exception {
		assertTrue(JvmLocator.isJavaHome(fakeHome("jdk-17", null)));
		assertFalse("an empty folder", JvmLocator.isJavaHome(tmp.newFolder("empty")));
		assertFalse("a file", JvmLocator.isJavaHome(tmp.newFile("file.txt")));
		assertFalse(JvmLocator.isJavaHome(null));
		assertFalse(JvmLocator.isJavaHome(new File(tmp.getRoot(), "missing")));
	}

	@Test
	public void versionComesFromTheReleaseFileLine() throws Exception {
		assertEquals("17.0.2", JvmLocator.versionOfHome(fakeHome("a", "JAVA_VERSION=\"17.0.2\"")));
		assertEquals("1.8.0_162", JvmLocator.versionOfHome(fakeHome("b", "JAVA_VERSION=\"1.8.0_162\"")));
		assertEquals("a release without that line", "", JvmLocator.versionOfHome(fakeHome("c", "OS_NAME=\"Windows\"")));
		assertEquals("no release file", "", JvmLocator.versionOfHome(fakeHome("d", null)));
		assertEquals("", JvmLocator.versionOfHome(null));
	}

	@Test
	public void theJvmRunningTheTestsIsRecognisedAsAJavaHome() {
		File home = new File(System.getProperty("java.home"));
		assertTrue(JvmLocator.isJavaHome(home) || JvmLocator.isJavaHome(home.getParentFile()));
	}

	@Test
	public void searchReturnsOnlyRealJavaHomesWithoutDuplicates() {
		java.util.List<String> homes = JvmLocator.findExtraJvmHomes(SysUtils.retrieveOS());
		assertNotNull(homes);
		assertEquals("no duplicates", homes.size(), new java.util.HashSet<String>(homes).size());
		for (String h : homes) {
			assertTrue(h, JvmLocator.isJavaHome(new File(h)));
		}
	}
}
