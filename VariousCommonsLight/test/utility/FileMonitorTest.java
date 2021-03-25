package utility;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.junit.*;

import test.util.utils.TestUtils;
import testconfig.TestConstants;
import various.common.light.files.watcher.ChangeDTO;
import various.common.light.files.watcher.FileChangeMonitor;


public class FileMonitorTest {
	
	public static final String rootDirPath = TestConstants.sourceFolderPath + "monitorTest";
	
	// Test 1
	public static final File file1MonitoredPath = new File(rootDirPath+"\\A.txt");
	public static final File folder1MonitoredPath = new File(rootDirPath+"\\folder");
	
	// Test 2
	public static final File fileOuter1MonitoredPath = new File("C:\\Users\\xx\\Desktop\\Crypter Test\\Prova_1Standard.ini");

	// Test 3 - single file
	public static final File fileSingleMonitoredPath = new File(rootDirPath+"\\A.txt");
	
	public static final int limit = 600; // 10 minutes
	
	@Before
	public void init() {
	}
	
	@Test
	@Ignore
	public void testFileChangeMonitor(){
		File rootDir = new File(rootDirPath);
		Assert.assertTrue(rootDir.exists());
		TestUtils.printl(rootDir.getAbsolutePath());
		ArrayList<File> monitored = new ArrayList<File>();
		monitored.add(file1MonitoredPath);
		monitored.add(folder1MonitoredPath);
		FileChangeMonitor fileMonitor = new FileChangeMonitor(rootDir, monitored);
		
		printResults(fileMonitor, null);
	}

	@Test
	@Ignore
	public void testFileChangeMonitor2(){
		File rootDir = new File(rootDirPath);
		Assert.assertTrue(rootDir.exists());
		TestUtils.printl(rootDir.getAbsolutePath());
		ArrayList<File> monitored = new ArrayList<File>();
		monitored.add(fileOuter1MonitoredPath);
		monitored.add(file1MonitoredPath);
		FileChangeMonitor fileMonitor = new FileChangeMonitor(rootDir, monitored, true, true, true);
		
		printResults(fileMonitor, null);
	}
	
	@Test
	public void testSingleFileChangeMonitor(){
		File rootDir = new File(rootDirPath);
		Assert.assertTrue(rootDir.exists());
		TestUtils.printl(rootDir.getAbsolutePath());
		ArrayList<File> monitored = new ArrayList<File>();
		monitored.add(fileSingleMonitoredPath);
		FileChangeMonitor fileMonitor = new FileChangeMonitor(rootDir, monitored, true, true, true);
		
		printResults(fileMonitor, fileSingleMonitoredPath);
	}
	
	/**
	 * If file is != null then use that, in other case use all monitored
	 * @param fileMonitor
	 * @param target
	 */
	private void printResults(FileChangeMonitor fileMonitor, File target) {
		int i = 0;
		boolean isChanged = false;
		boolean isDeleted = false;
		boolean isCreated = false;
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		String nowStr = "";
		
		try{
			while(i < limit){
				
				ChangeDTO changes = new ChangeDTO();
				
				if(target != null) {
					changes = fileMonitor.checkForFile(target);
				}else {
					changes = fileMonitor.checkForEverithing();
				}
				
				nowStr = formatter.format(new Date());
				isChanged = changes.changedFiles.size() != 0;
				isDeleted = changes.removedFiles.size() != 0;
				isCreated = changes.addedFiles.size() != 0;
				
				if (isCreated || isChanged || isDeleted) {
					TestUtils.print("\n\n ########################  EVENT REGISTERED - "+nowStr+" ###############################\n\n");
					for(File removed : changes.removedFiles){
						TestUtils.printl("\n => FILE REMOVED: " + removed.getAbsolutePath());
					}
					for(File added : changes.addedFiles){
						TestUtils.printl("\n => FILE ADDED: " + added.getAbsolutePath());
					}
					for(File changed : changes.changedFiles){
						TestUtils.printl("\n => FILE CHANGED: " + changed.getAbsolutePath());
					}
				}else {
				}
				
				Thread.sleep(2000);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		TestUtils.printl("\n\n TIME ELAPSED : CLOSING TEST");
	}
}
