package utility;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import various.common.light.utility.string.StringWorker;

public class VersionCompareTest {

	private static String version_A = "0.9.9";
	private static String version_B = "1.1.1";
	private static String version_C = "1.1.1.2";

	@Test
	public void testCompare() {
		assertTrue(StringWorker.compareVersions(version_A, version_B) == -1);
		assertTrue(StringWorker.compareVersions(version_B, version_C) == -1);
		assertTrue(StringWorker.compareVersions(version_A, version_C) == -1);
		assertTrue(StringWorker.compareVersions(version_A, "0.10.9") == -1);
		assertTrue(StringWorker.compareVersions(version_A, "1.0.100") == -1);
		assertTrue(StringWorker.compareVersions(version_A, "1a.0.100") == -1);
		assertTrue(StringWorker.compareVersions("0.9.37", "1") == -1);
		assertTrue(StringWorker.compareVersions("1.0", "1") == 0);
		assertTrue(StringWorker.compareVersions("1.0", "1.0.0") == 0);
		assertTrue(StringWorker.compareVersions("1.0", "1.0.1") == -1);
	}
}
