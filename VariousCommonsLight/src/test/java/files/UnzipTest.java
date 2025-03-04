package files;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

import testconfig.TestConstants;
import various.common.light.files.ZipWorker;


public class UnzipTest {

	@Test
	public void unzipTest() throws Exception {
		File unzipped = ZipWorker.unzipFile(TestConstants.sourceFolderPath + "test.zip", TestConstants.destFolderPath + "unzippedTest", true);
		assertNotNull(unzipped);
	}
}
