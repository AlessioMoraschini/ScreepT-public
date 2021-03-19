package files;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import testconfig.TestConstants;
import utils.TestUtils;

/**
 * 
 * @author xx
 *
 */
public class FilesGenericTest {

	String outPathMKDIRS = TestConstants.destFolderPath + "testFolder_MKDIRS/test.txt";
	@Test
	public void testMkDirs() throws IOException {
		File out = new File(outPathMKDIRS);
		out.getParentFile().mkdirs();
		
		FileWorker.writeStringToFile(System.lineSeparator().replaceAll("\n", "ciao").replaceAll("ciao", System.lineSeparator()), out, true);
		TestUtils.print(FileWorker.readFileAsString(out).replaceAll("ciao", System.lineSeparator()));
	}
}
