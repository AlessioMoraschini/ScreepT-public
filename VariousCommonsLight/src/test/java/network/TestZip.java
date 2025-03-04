package network;
import java.io.File;

import static testconfig.TestConstants.*;

import org.junit.Test;

import various.common.light.files.ZipWorker;

public class TestZip {

	@Test
	public void testZipFolder() throws Exception {
		File zipped = ZipWorker.zipFolder(sourceFolderPath + "FolderC", destFolder, true, true);
		System.out.println(zipped);
	}
}
