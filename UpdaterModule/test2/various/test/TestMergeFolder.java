package various.test;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import files.FileWorker;

public class TestMergeFolder {

	private File sourceFolderA = new File("./Test/updates_A");
	private File sourceFolderB = new File("./Test/updates_B_temp");
	
	@Test
	public void testMerge() throws IOException {
		FileWorker.copyDirectoryContents(sourceFolderA, sourceFolderB);
		
	}
}
