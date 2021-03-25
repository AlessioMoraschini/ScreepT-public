package files;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import test.util.utils.TestUtils;
import testconfig.TestConstants;
import various.common.light.files.FileWorker;

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
