package network;
import java.io.File;

import static testconfig.TestConstants.*;

import org.junit.Test;

import files.ZipWorker;

public class TestZip {

	@Test
	public void testZipFolder() throws Exception {
		File zipped = ZipWorker.zipFolder(sourceFolderPath + "FolderC", destFolder, true, true);
		System.out.println(zipped);
	}
}
