package testconfig;

import java.io.File;

public class TestConstants {
	
	public static String rootTestFolderPath = "TEST_FILES/";
	public static File rootTestFolder = new File(rootTestFolderPath);
	
	public static String sourceFolderPath = TestConstants.rootTestFolderPath + "sourceFolder/";
	public static File sourceFolder = new File(sourceFolderPath);

	public static String destFolderPath = TestConstants.rootTestFolderPath + "destFolder/";
	public static File destFolder = new File(destFolderPath);
	
	static {
		rootTestFolder.mkdirs();
		
		sourceFolder.mkdirs();
		destFolder.mkdirs();
	}
}
