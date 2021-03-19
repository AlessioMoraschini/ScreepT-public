package string;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import files.FileVarious;
import utility.string.StringWorker;

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
	public void checkWildcardMatch2() {
		
		String tocheck = "HELlo.java";
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "*.javas", true));
		Assert.assertFalse(StringWorker.checkWildcardMatch(tocheck, "*.javA", false));
		
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "*.javA", true));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "*.java", true));
		Assert.assertTrue(StringWorker.checkWildcardMatch(tocheck, "HELLO*", true));
	}
}
