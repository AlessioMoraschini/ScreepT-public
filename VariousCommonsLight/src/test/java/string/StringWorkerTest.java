package string;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import various.common.light.files.FileVarious;
import various.common.light.utility.string.StringWorker;

public class StringWorkerTest {

	@Test
	public void checkWildcardMatch() {

		String tocheck = "HELLO WORLD";
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "ELLO"));
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "HELLO"));
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "*HELLO"));
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "HELLO WORLD2"));
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "HELLO WORL"));
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "hello world"));

		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "hello world", true));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "*eLlO*", true));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "*ELLO*"));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "HELLO*"));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "*LLO*"));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "HELLO WORLD"));
	}

	@Test
	public void checkWildcardMatchPaths() {

		String tocheck = "TEST_FILES\\destFolder";
		String normalized = FileVarious.getCanonicalPathSafeNormalized(new File(tocheck), "/");
		Assert.assertTrue(StringWorker.checkWildcardMatch(normalized, "*/destFolder"));
		Assert.assertTrue(StringWorker.checkWildcardMatch(normalized, "*FILE*destFolder"));
		Assert.assertTrue(StringWorker.checkWildcardMatch(normalized, "*FILE*/destFolder"));
	}

	@Test
	public void checkDotsDigits() {
		Assert.assertEquals("1.8.0.202", StringWorker.keepOnlyDotsAndDigits("jdk1.8.0_202"));
		Assert.assertEquals("21.35", StringWorker.keepOnlyDotsAndDigits("openjdk_21-ea+35"));
		Assert.assertEquals("17.0.10.7", StringWorker.keepOnlyDotsAndDigits("temurin-17.0.10+7"));
		Assert.assertEquals("17.44.15.17.0.8", StringWorker.keepOnlyDotsAndDigits("zulu17.44.15-ca-jdk17.0.8"));
		Assert.assertEquals("17", StringWorker.keepOnlyDotsAndDigits("jdk-17"));
		Assert.assertEquals("21.35", StringWorker.keepOnlyDotsAndDigits("jdk-21+35"));
		Assert.assertEquals("11.0.2", StringWorker.keepOnlyDotsAndDigits(".11.0.2."));
		Assert.assertEquals("17.0.1", StringWorker.keepOnlyDotsAndDigits("jdk..17..0..1"));
		Assert.assertEquals("", StringWorker.keepOnlyDotsAndDigits("abc---xyz"));
		Assert.assertEquals("", StringWorker.keepOnlyDotsAndDigits(""));
		Assert.assertEquals("", StringWorker.keepOnlyDotsAndDigits(null));
	}

	@Test
	public void checkWildcardMatch2() {

		String tocheck = "HELlo.java";
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "*.javas", true));
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "*.javA", false));

		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "*.javA", true));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "*.java", true));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "HELLO*", true));
	}
}
