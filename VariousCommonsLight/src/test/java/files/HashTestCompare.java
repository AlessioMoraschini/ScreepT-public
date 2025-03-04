package files;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import test.util.utils.TestUtils;
import testconfig.TestConstants;
import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;

public class HashTestCompare {

	public static File sourceFile = new File(TestConstants.sourceFolderPath + "test.zip");
	public static boolean uppercase = false;
	
	@Test
	public void testCompareHashVersions() throws Throwable {
		for(HashingFunction algorithm : HashingFunction.values()) {
			
			TestUtils.printHeader("#", 50, algorithm.name());
			
			// V1: read All bytes in memory(older version used)
			@SuppressWarnings("deprecation")
			String hashV1 = ChecksumHasher.getFileHash(sourceFile, algorithm, uppercase);

			// V2: read buffered bytes in memory(new version)
			String hashV2 = ChecksumHasher.getFileHashBuffered(sourceFile, algorithm, uppercase);
			boolean testPassed = hashV1.equals(hashV2);
			TestUtils.printl("All Bytes in memory hashing : " + hashV1);
			TestUtils.printl("Buffered hashing : " + hashV2);
			TestUtils.printl("Same result test passed : " + testPassed);
			
			assertTrue(testPassed);
		}
	}
}
