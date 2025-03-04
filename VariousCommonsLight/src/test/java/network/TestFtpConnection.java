package network;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import test.util.utils.TestUtils;
import testconfig.TestConstants;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.network.ftp.FtpDumper;
import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;

public class TestFtpConnection {

	private static final String HOST = "ftp.am-design-development.com";
	private static final String USER = "";
	@SuppressWarnings("unused")
	private static String psw = "";
	private static FtpDumper dumper;
	private static final int PORT = 22;
	private static final String FOLDER_ON_SERVER_LONG_DOWNLOAD = "/www.am-design-development.com/ScreepT/Release";
	private static final String FOLDER_ON_SERVER = "/www.am-design-development.com/ScreepT/Updates";
	private static final String TEST_FILE_UPLOAD = "E.java";
	private static final File FILE_TO_UPLOAD = new File(TestConstants.sourceFolderPath + TEST_FILE_UPLOAD);
//	private static final String TEST_FILE_UPLOAD = "A.txt";
	private static final String TEST_FILE_DOWNLOAD_LONG = "ScreepT_V0.9.14.zip";
	private static final String TEST_FILE_DOWNLOAD = TEST_FILE_UPLOAD;
//	private static final String TEST_FILE_DOWNLOAD = "TEST_FTP_UPLOAD.txt";
	private static final String DOWNLOAD_DESTINATION_LOCAL = "C:/Users/xx/Desktop/Downloads/" + TEST_FILE_DOWNLOAD;
	
	@BeforeClass
	public static void init() {
		String psw = new JOptionHelper(null).askForString("Insert password", "Insert password");
		TestUtils.printl(psw);
		
		dumper = new FtpDumper(HOST, USER, psw, PORT);
	}
	
	@Test
	@Ignore
	public void testLongDownloadOnly() throws IOException {
		TestUtils.printHeader("#", 60, "TEST FTP - DOWNLOAD ONLY (big file)");
		TestUtils.printl("HOST: " + HOST);
		TestUtils.printl("USER: " + USER);
		TestUtils.printl("PORT: " + PORT);
		
		assertTrue(dumper.checkDirectoryExists(FOLDER_ON_SERVER_LONG_DOWNLOAD));
		assertTrue(dumper.checkFileExists(FOLDER_ON_SERVER_LONG_DOWNLOAD, TEST_FILE_DOWNLOAD_LONG));
		
//		progPanel.updateText("Downloading " + FOLDER_ON_SERVER + "/" + TEST_FILE_DOWNLOAD);
		dumper.downloadFile(FOLDER_ON_SERVER_LONG_DOWNLOAD, TEST_FILE_DOWNLOAD_LONG, "C:/Users/xx/Desktop/Downloads/Long_Test", true);
	}
	
	@Test
	@Ignore
	public void testCompleteCycleFtp() throws Throwable {
		
		checkDirExistance();
		
		TestUtils.printHeader("#", 60, "TEST FTP CONNECTION");
		TestUtils.printl("HOST: " + HOST);
		TestUtils.printl("USER: " + USER);
		TestUtils.printl("PORT: " + PORT);
		
		TestUtils.printArrayAsLines(1,dumper.getRemoteFilesInFolder(FOLDER_ON_SERVER));
		
//		UpdateProgressPanelMin progPanel = new UpdateProgressPanelMin(false);
//		progPanel.setUndefinedDuration();
//		progPanel.updateText("Uploading " + TestConstants.sourceFolderPath + TEST_FILE_UPLOAD);
		dumper.uploadFile(FILE_TO_UPLOAD.getAbsolutePath(), TEST_FILE_UPLOAD, FOLDER_ON_SERVER);
//		new JOptionHelper(null).info(FILE_TO_UPLOAD + ": Uploaded!", "Uploaded");
		
		assertTrue(dumper.checkFileExists(FOLDER_ON_SERVER, TEST_FILE_UPLOAD));
		
//		progPanel.updateText("Downloading " + FOLDER_ON_SERVER + "/" + TEST_FILE_DOWNLOAD);
		File downloaded = dumper.downloadFile(FOLDER_ON_SERVER, TEST_FILE_DOWNLOAD, DOWNLOAD_DESTINATION_LOCAL, false);

		assertTrue(ChecksumHasher.equalsFilesChecksum(downloaded, FILE_TO_UPLOAD, HashingFunction.SHA512));
		assertTrue(deleteFile(FILE_TO_UPLOAD.getName()));
		
//		progPanel.forceClose();
		new JOptionHelper(null).info("Finished! Result file: " + downloaded, "Downloaded");
		GuiUtils.openInFileSystem(new File(DOWNLOAD_DESTINATION_LOCAL));
	}
	
	public void checkDirExistance() throws IOException {
		TestUtils.print("Checking remote files existance...");

		assertTrue(dumper.checkDirectoryExists(FOLDER_ON_SERVER));
		
		TestUtils.print("Check finished. Ready to start upload!");
	}
	
	public boolean deleteFile(String toDelete) throws IOException {
		return dumper.deleteFile(FOLDER_ON_SERVER, toDelete);
	}
	
	@AfterClass
	public static void finalizeTest() {
		dumper.disconnect();
	}
}
