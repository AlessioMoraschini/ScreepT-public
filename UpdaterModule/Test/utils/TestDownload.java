package utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import gui.UpdateProgressPanelMin;
import resources.GeneralConfig;
import resources.PropertiesManager;

import static testUtils.TestUtils.*;

public class TestDownload {

	public static final String downloadTestFolder = "DownloadTest/";
	public static final File downloadTestFolderFile = new File(downloadTestFolder);
	public static URL urlManifestRemote;
	public static String tempManifestFilePath = downloadTestFolder + "tempManifest.properties";
	public static File tempManifestFile = new File(tempManifestFilePath);
	public static String tempUpdaterFilePath = downloadTestFolder + GeneralConfig.WEB_UPDATER_JAR_NAME;
	public static File tempUpdaterFile = new File(tempUpdaterFilePath);
	public static String tempUpdatesZipFilePath = downloadTestFolder + GeneralConfig.WEB_UPDATES_PACK_NAME;
	public static File tempUpdatesZipFile = new File(tempUpdatesZipFilePath);
	
	public static UpdatesManager updatesManager;
	public static String testCurrVersion = "0.8.0";
	
	@Before
	public void initialize() throws IOException {
		urlManifestRemote = new URL("https://" + GeneralConfig.WEB_UPDATES_MANIFEST_URL);
		tempManifestFile.delete();
		tempManifestFile.createNewFile();
		tempUpdaterFile.delete();
		tempUpdaterFile.createNewFile();
		tempUpdatesZipFile.delete();
		tempUpdatesZipFile.createNewFile();
		
		updatesManager = new UpdatesManager(urlManifestRemote, testCurrVersion);
	}
	
	@Test 
	public void updateManager() throws Exception{
		printl(getHeaderString("#", 40, "TEST UPDATES MANAGER"));
		String newVersion = updatesManager.lookForUpdates(false);
		printl("Test version: "+testCurrVersion+" -> new version: " + newVersion);
		
		if (newVersion != null) {
			File[] output = updatesManager.downloadAndExtractUpdates();
			printl("Executable jar updater: " + output[0].toString());
			printl("Extracted updates dir: " + output[1].toString());
		}
	}
	
	@Test
	@Ignore
	public void testUrlFileSize() {
		int size = DownloadUtils.getFileSize(urlManifestRemote);
		printl(getHeaderString("#", 40, "TEST SIZE"));
		printl("Size of " + urlManifestRemote + " is: " + Various.coolFileSize((long)size));
		
	}

	@Test
	@Ignore
	public void testUrlFileDownload() throws Exception {
		printl(getHeaderString("#", 40, "TEST DOWNLOAD AND PARSE MANIFEST .properties"));
		File downloadedManifest = DownloadUtils.downloadFile(false, urlManifestRemote, tempManifestFilePath, new UpdateProgressPanelMin());
		printl("Manifest Downloaded!! Size of " + urlManifestRemote + " is: " + Various.coolFileSize(downloadedManifest.length()));
		
		PropertiesManager propsManager = new PropertiesManager(downloadedManifest);
		
		String version = propsManager.getProperty(GeneralConfig.WEB_UPDATER_VERSION_PROP);
		printl("Version: " + version);
		URL updaterJarUrl = new URL(propsManager.getProperty(GeneralConfig.WEB_UPDATER_JAR_PROP));
		printl("updaterJarUrl: " + updaterJarUrl);
		URL updatesZippedUrl = new URL(propsManager.getProperty(GeneralConfig.WEB_UPDATES_PACK_PROP));
		printl("updatesZippedUrl: " + updatesZippedUrl);
		
		printl(getHeaderString("#", 40, "TEST PARSE AND DOWNLOAD PACKETS"));
		File downloadedUpdater = DownloadUtils.downloadFile(false, updaterJarUrl, tempUpdaterFilePath, new UpdateProgressPanelMin());
		printl("Updater jar Downloaded!! Size of " + updaterJarUrl + " is: " + Various.coolFileSize(downloadedUpdater.length()));

		File downloadedUpdatesZip = DownloadUtils.downloadFile(false, updatesZippedUrl, tempUpdatesZipFilePath, new UpdateProgressPanelMin());
		printl("Updates ZIP Downloaded!! Size of " + updatesZippedUrl + " is: " + Various.coolFileSize(downloadedUpdatesZip.length()));
		
		File unzippedUpdates = DownloadUtils.unzipFile(downloadedUpdatesZip.getAbsolutePath(), downloadTestFolderFile.getAbsolutePath(), true);
		printl("Unzipped updates: " + unzippedUpdates);
		
	}
}
