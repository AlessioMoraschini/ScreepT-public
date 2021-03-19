package files;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import files.search.FileNameSearchAdvanced;
import files.search.FileNameSearchAdvanced.SearchType;

public class TestSearchAdvanced {

//	private static final String MATCH = "MATCH_TEST";
//	private static final File rootFolder = new File("TEST_ROOT_FOLDER");

	private static final boolean IGNORE_CASE = false;
	private static final String MATCH = "sAmple";
	private static final String MATCH_wildcard = "*Ample*";
	private static final File rootFolder = new File("C:\\Users\\xx\\Desktop\\Crypter Test\\BACKUP_ECLIPSE");
//	private static final File rootFolderLight = new File("C:\\Users\\xx\\Desktop\\Crypter Test\\BACKUP_ECLIPSE\\RemoteSystemsTempFiles");
	
	@Test
	@Ignore
	public void testFolderSearchDeep() throws IOException {
		
		System.out.print("TEST_SEARCH FOLDER (DEEP SEARCH) INSIDE: " + rootFolder.getCanonicalPath());
		searchTest(MATCH, SearchType.FOLDERS_ONLY, true, true);
	}

	@Test
	@Ignore
	public void testFolderSearch() throws IOException {
		
		System.out.print("TEST_SEARCH FOLDER (NON DEEP) INSIDE: " + rootFolder.getCanonicalPath());
		searchTest(MATCH, SearchType.FOLDERS_ONLY, false, true);
	}
	
	@Test
	@Ignore
	public void testFileSearchDeep() throws IOException {
		
		System.out.print("TEST_SEARCH FILE (DEEP SEARCH) INSIDE: " + rootFolder.getCanonicalPath());
		searchTest(MATCH, SearchType.FILE_ONLY, true, true);
	}
	
	@Test
	@Ignore
	public void testFileSearch() throws IOException {
		
		System.out.print("TEST_SEARCH FILE (NON DEEP) INSIDE: " + rootFolder.getCanonicalPath());
		searchTest(MATCH, SearchType.FILE_ONLY, false, true);
	}
	
	@Test
	@Ignore
	public void testAllSearchDeep() throws IOException {
		
		System.out.print("TEST_SEARCH ALL (DEEP SEARCH) INSIDE: " + rootFolder.getCanonicalPath());
		searchTest(MATCH, SearchType.ALL, true, true);
	}

	@Test
//	@Ignore
	public void testAllSearchDeepWildcard() throws IOException {
		
		System.out.print("TEST_SEARCH ALL (DEEP SEARCH) INSIDE: " + rootFolder.getCanonicalPath());
		searchTest(MATCH_wildcard, SearchType.ALL, true, true);
	}
	
	@Test
	@Ignore
	public void testAllSearch() throws IOException {
		
		System.out.print("TEST_SEARCH ALL (NON DEEP) INSIDE: " + rootFolder.getCanonicalPath());
		searchTest(MATCH, SearchType.ALL, false, false);
	}
	
	
	// ######## UTILITY ###########
	private void searchTest(String match, SearchType type, boolean deepSearch, boolean wildcardMode) {
		long start = System.currentTimeMillis();
		
		List<File> result = FileNameSearchAdvanced.findByMatch(
				rootFolder, match, false, type, deepSearch, IGNORE_CASE, wildcardMode);
		
		long end = System.currentTimeMillis();
		printElapsed(result.size(), start, end);
		
		int i = 1;
		for(File res : result) {
			System.out.println("RESULT N� " + i + " -> " +  res);
			i++;
		}
		
		System.out.println("\n\n ############################# END ##################################\n\n");
	}
	
	private void printElapsed(int i, long start, long end) {
		System.out.println ("   => TEST COMPLETED with " 
				+ i + " results (" + "Elapsed milliseconds: " + (end-start) + ")\n\n");
	}
}
