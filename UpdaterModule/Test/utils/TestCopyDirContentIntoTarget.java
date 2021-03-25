package utils;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class TestCopyDirContentIntoTarget {
	
	public static final File sourceDir = new File("DownloadTest/Application/UPDATES/updates");
	public static final File destDir = new File("DownloadTest/Application");
	
	@Test
	public void testCopyUpdates() throws IOException {
		Various.copyDirectoryContents(sourceDir.getAbsoluteFile(), destDir.getAbsoluteFile());
	}

}
