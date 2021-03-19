package files;

import java.io.File;
import java.util.ArrayList;

import org.junit.Test;

import files.search.FileNameSearchLight;
import testconfig.TestConstants;

public class SearchTest {

	String fileNameMatch = "java";
	String folderMatch = "java";
	
	@Test
	public void fileSearchTest() {
		ArrayList<File> matchesFound = new ArrayList<File>();
		
		// search in subdirs
		matchesFound = FileNameSearchLight.searchFoldersInRootFolder(TestConstants.rootTestFolder, folderMatch, true);
		printFound(matchesFound);
		
		matchesFound = FileNameSearchLight.searchFilesInRootFolder(TestConstants.rootTestFolder, folderMatch, true);
		printFound(matchesFound);
	}
	
	private void printFound(ArrayList<File> fileList) {
		for (File iFile : fileList) {
			System.out.println(" => Found: " + iFile.getAbsolutePath());
		}
	}
}
