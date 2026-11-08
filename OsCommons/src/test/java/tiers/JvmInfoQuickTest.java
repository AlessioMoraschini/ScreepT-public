package tiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import os.commons.utils.JvmInfo;

/** QUICK tier: names of the installed Javas shown in the launcher */
public class JvmInfoQuickTest {

	private static String fakeHome(String release) throws Exception {
		File home = Files.createTempDirectory("fakejava").toFile();
		new File(home, "bin").mkdirs();
		new File(home, "bin/java.exe").createNewFile();
		Files.write(new File(home, "release").toPath(), release.getBytes("UTF-8"));
		return home.getAbsolutePath();
	}

	@Test
	public void readsVersionVendorAndKindFromTheReleaseFile() throws Exception {
		JvmInfo info = JvmInfo.of(fakeHome("JAVA_VERSION=\"21.0.2\"\nIMPLEMENTOR=\"Eclipse Adoptium\"\nOS_ARCH=\"x86_64\"\n"));
		assertEquals(21, info.major);
		assertEquals("Adoptium", info.vendor);
		assertEquals("OpenJDK", info.flavor);
		assertFalse("no javac: it is a JRE", info.jdk);
		assertTrue(info.hasJavaExecutable);
		assertEquals("jre Adoptium 21 OpenJDK", info.displayName());
		assertTrue(info.documentationUrl().endsWith("/21/"));
	}

	@Test
	public void oldVersionNumbersGiveTheFeatureVersion() throws Exception {
		assertEquals(8, JvmInfo.of(fakeHome("JAVA_VERSION=\"1.8.0_362\"\n")).major);
		assertEquals(17, JvmInfo.of(fakeHome("JAVA_VERSION=\"17.0.12\"\n")).major);
	}

	@Test
	public void repeatedNamesGetAProgressiveNumberAndUniqueOnesStayPlain() throws Exception {
		String a = fakeHome("JAVA_VERSION=\"21.0.2\"\nIMPLEMENTOR=\"Oracle Corporation\"\n");
		String b = fakeHome("JAVA_VERSION=\"21.0.3\"\nIMPLEMENTOR=\"Oracle Corporation\"\n");
		String c = fakeHome("JAVA_VERSION=\"17.0.1\"\nIMPLEMENTOR=\"Oracle Corporation\"\n");
		List<String> labels = JvmInfo.uniqueLabels(Arrays.asList(a, b, c));
		assertTrue(labels.get(0).endsWith("(1)"));
		assertTrue(labels.get(1).endsWith("(2)"));
		assertFalse(labels.get(2).contains("("));
	}
}
