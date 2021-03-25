/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package updater;

import static java.lang.System.out;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import exception.UpdatesUnavailableException;
import files.FileWorker;
import files.FileVarious;
import files.ZipWorker;
import gui.UpdateProgressPanel;
import main.MainStandAloneUpdater;
import network.http.HttpHelper;
import network.http.HttpHelper.HttpRequestMetod;
import network.utils.DownloadUtils;
import om.exception.ProgressBarInterruptedException;
import utility.hash.ChecksumHasher;
import utility.hash.HashingFunction;
import utility.log.SafeLogger;
import utility.manipulation.ConversionUtils;
import utility.properties.PropertiesManager;

public class UpdatesManager {

	public URL manifestRemoteUrl;
	
	public static SafeLogger logger = new SafeLogger(UpdatesManager.class);
	
	public static PropertiesManager resourcesFilePathsMappings = new PropertiesManager(MainStandAloneUpdater.MAIN_APP_PROPERTIES_PATH);
	public static String RESOURCES_DIR = resourcesFilePathsMappings.getProperty("RESOURCES_DIR", "Resources_ScreepT/");  
	public static String DEFAULT_LAUNCHER_INI_PATH = resourcesFilePathsMappings.getProperty("DEFAULT_LAUNCHER_INI_PATH", "LAUNCHER/Launcher_Configuration.ini");
	public static String UPDATES_DIR = resourcesFilePathsMappings.getProperty("UPDATES_DIR", "UPDATES/");
	public static String TEMP_DIR = resourcesFilePathsMappings.getProperty("TEMP_DIR", "UPDATES/");
	public static String WEB_UPDATER_VERSION_PROP = resourcesFilePathsMappings.getProperty("WEB_UPDATER_VERSION_PROP", "version");                                                                                                               
	public static String WEB_UPDATES_PACK_NAME = resourcesFilePathsMappings.getProperty("WEB_UPDATES_PACK_NAME", "updates.zip");                                                                                                               
	public static String WEB_UPDATES_PACK_PROP = resourcesFilePathsMappings.getProperty("WEB_UPDATES_PACK_PROP", "updatesZippedUrl");                                                                                                               
	public static String WEB_UPDATER_JAR_NAME = resourcesFilePathsMappings.getProperty("WEB_UPDATER_JAR_NAME", "updater.jar");                                                                                                               
	public static String WEB_UPDATER_JAR_PROP = resourcesFilePathsMappings.getProperty("WEB_UPDATER_JAR_PROP", "updaterJarUrl");                                                                                                               
	
	public static  final String localTempManifestName = "tempManifest.properties";
	public final String localTempManifestPath = UPDATES_DIR + localTempManifestName;
	public final String localTempZippedUpdates = UPDATES_DIR + WEB_UPDATES_PACK_NAME;

	public String currentVersion = "";
	public AtomicBoolean updatesReadyToInstall = new AtomicBoolean();
	
	public File[] lastUpdatesDownloaded;
	
	public UpdatesManager(URL manifestRemote, String currentVersion) {
		this.manifestRemoteUrl = manifestRemote;
		this.currentVersion = currentVersion;
		
		try {
			File updatesDir = new File(UPDATES_DIR);
			if(!updatesDir.exists()) {
				updatesDir.mkdirs();
				updatesDir.mkdir();
			}
		} catch (Exception e) {
			out.println("Cannot create updates dir (Resources_ScreepT/UPDATES) : " + Arrays.deepToString(e.getStackTrace()));
		}
	}
	
	/**
	 * @return null if current is updated, else it return the newest version downloadable
	 * @throws Exception 
	 */
	public String lookForUpdates(boolean silent) throws Exception, TimeoutException {
		String newVersionAvailable = null;
		
		UpdateProgressPanel updateManagerProgbar = null;
		try {
			
			if(HttpHelper.is404(manifestRemoteUrl, HttpRequestMetod.GET)) {
				throw new UpdatesUnavailableException(getUpdatesUnavailableMessage());
			}
			
			if (!silent) {
				updateManagerProgbar = new UpdateProgressPanel(true);
				updateManagerProgbar.setModalType(ModalityType.MODELESS);
				updateManagerProgbar.dialog.setTitle("Looking for updates...");
				updateManagerProgbar.getPb().setIndeterminate(true);
			}
			
			logger.info(FileVarious.getHeaderString("#", 40, "DOWNLOAD AND PARSE MANIFEST .properties"));
			File downloadedManifest = DownloadUtils.downloadFile(silent, manifestRemoteUrl, localTempManifestPath, updateManagerProgbar);
			logger.info("Manifest Downloaded!! Size of " + manifestRemoteUrl + " is: " + ConversionUtils.coolFileSize(downloadedManifest.length()));
			
			PropertiesManager propsManager = new PropertiesManager(downloadedManifest);
			
			String version = propsManager.getProperty(WEB_UPDATER_VERSION_PROP);
			logger.info("Version: " + version);
			
			newVersionAvailable = (DownloadUtils.compareNewVersion(version, currentVersion)) ? version : null;
		} catch (Exception e) {
			logger.error("An error occurred:", e);
			updatesReadyToInstall.set(false);
			if(e instanceof ProgressBarInterruptedException ) {
				logger.warn("User stopped process:", e);
				emptyUpdatesDir();
				throw e;
			} else {
				logger.error("", e);
				throw e;
			}
		} finally {
			if(updateManagerProgbar != null) {
				updateManagerProgbar.forceClose();
			}
		}
		
		return newVersionAvailable;
	}
	
	public boolean udpdatesReachable() throws MalformedURLException, IOException {
		return !HttpHelper.is404(manifestRemoteUrl, HttpRequestMetod.GET);
	}
	
	public boolean emptyUpdatesDir() {
		File updatesDir = new File(UPDATES_DIR);
		return FileWorker.deleteDirContentRecursive(updatesDir);
	}
	
	/**
	 * Returns an array of two elements: first is jar updater, second is updates extracted folder reference
	 */
	public File[] downloadAndExtractUpdates() throws Exception, TimeoutException {
		
		emptyUpdatesDir();
		
		lastUpdatesDownloaded = new File[3];
		
		if(HttpHelper.is404(manifestRemoteUrl, HttpRequestMetod.GET)) {
			throw new UpdatesUnavailableException(getUpdatesUnavailableMessage());
		}
		
		UpdateProgressPanel updateProgressBar = null;
		try {
			if(UPDATES_DIR == null || !new File(UPDATES_DIR).isDirectory()) {
				return null;
			}
			
			updateProgressBar = new UpdateProgressPanel(true);
			updateProgressBar.setModalType(ModalityType.MODELESS);
			updateProgressBar.dialog.setTitle("Downloading files...");
			updateProgressBar.update(20, "Downloading "+manifestRemoteUrl, true);
			logger.info(FileVarious.getHeaderString("#", 40, "DOWNLOAD AND PARSE MANIFEST .properties"));
			
			// Download manifest.properties into UPDATES -> /UPDATES/manifest.properties
			File downloadedManifest = DownloadUtils.downloadFile(false, manifestRemoteUrl, localTempManifestPath, updateProgressBar);
			logger.info("Manifest Downloaded!! Size of " + manifestRemoteUrl + " is: " + ConversionUtils.coolFileSize(downloadedManifest.length()));
			
			PropertiesManager propsManager = new PropertiesManager(downloadedManifest);
			
			String version = propsManager.getProperty(WEB_UPDATER_VERSION_PROP);
			logger.info("Version: " + version);
			
			// Calculate jar main application reference after extract -> /UPDATES/ScreepT_X.Y.Z.jar
			lastUpdatesDownloaded[2] = new File("ScreepT_"+version+".jar");
			
			// READ FROM DOWNLOADED PROPS
			URL updaterJarUrl = new URL(propsManager.getProperty(WEB_UPDATER_JAR_PROP));
			logger.info("updaterJarUrl: " + updaterJarUrl);
			
			URL updatesZippedUrl = new URL(propsManager.getProperty(WEB_UPDATES_PACK_PROP));
			logger.info("updatesZippedUrl: " + updatesZippedUrl);
			
			// Download jar updater into root -> /application/updater.jar
			updateProgressBar.update(40, "Downloading "+updaterJarUrl, true);
			logger.info(FileVarious.getHeaderString("#", 40, "PARSE AND DOWNLOAD PACKETS"));
			
			File downloadedUpdater = DownloadUtils.downloadFile(false, updaterJarUrl, WEB_UPDATER_JAR_NAME, updateProgressBar);
			lastUpdatesDownloaded[0] = downloadedUpdater;
			logger.info("Updater jar Downloaded!! Size of " + updaterJarUrl + " is: " + ConversionUtils.coolFileSize(downloadedUpdater.length()));

			// Download zipped updates -> /UPDATES/updates.zip
			updateProgressBar.update(70, "Downloading "+updaterJarUrl, true);
			File downloadedUpdatesZip = DownloadUtils.downloadFile(false, updatesZippedUrl, localTempZippedUpdates, updateProgressBar);
			logger.info("Updates ZIP Downloaded!! Size of " + updatesZippedUrl + " is: " + ConversionUtils.coolFileSize(downloadedUpdatesZip.length()));
			
			// Final check for remote changes
			finalReCheckManifest(downloadedManifest);
			
			updateProgressBar.forceClose();
			
			// Extract zipped updates -> /UPDATES/updates/
			File unzippedUpdates = ZipWorker.unzipFile(downloadedUpdatesZip.getAbsolutePath(), UPDATES_DIR, true);
			logger.info("Unzipped updates: " + unzippedUpdates);
			lastUpdatesDownloaded[1] = unzippedUpdates;
			
		} catch (Exception e) {
			logger.error("An error occurred!", e);
			
			if (e instanceof ProgressBarInterruptedException) {
				logger.warn("User stopped process:", e);
			}

			updatesReadyToInstall.set(false);
			emptyUpdatesDir();
			
			throw e;
			
		} finally {
			if(updateProgressBar != null) {
				updateProgressBar.forceClose();
			}
		}
		
		// Return the reference of updater application jar file
		return lastUpdatesDownloaded;
	}
	
	/**
	 * This method is meant to test if the initial manifest is not changed during the upload process: 
	 * if so it means that something has changed during the download, possibily leading to inconsistent data.
	 * 
	 * To avoid this, in this case an exception of type UpdatesUnavailableException will be thrown.
	 * 
	 * @throws UpdatesUnavailableException 
	 * 
	 * @throws Exception 
	 */
	private void finalReCheckManifest(File originalManifest) throws UpdatesUnavailableException, Exception {
		if(HttpHelper.is404(manifestRemoteUrl, HttpRequestMetod.GET)) {
			throw new UpdatesUnavailableException(getUpdatesUnavailableMessage());
		}
		
		File newTempManifest = DownloadUtils.downloadFile(true, manifestRemoteUrl, TEMP_DIR + localTempManifestName, null);
		boolean fileIsTheSame = false;
		
		try {
			fileIsTheSame = ChecksumHasher.equalsFilesChecksum(newTempManifest, originalManifest, HashingFunction.SHA512);
		} catch (Throwable e) {
			logger.error("The was an error while comparing new temp manifest checksum", e);
		}
		
		if(!fileIsTheSame) {
			Exception exc = new UpdatesUnavailableException(getUpdatesUnavailableMessage());
			logger.error("New manifest does not match the original one: launching exception!", exc);
			throw exc;
		} else {
			logger.info("New manifest matched correctly :)");
		}
	}
	
	public File[] getLatestUpdates() {
		try {
			if (lastUpdatesDownloaded == null || lastUpdatesDownloaded.length < 2 || !lastUpdatesDownloaded[0].exists() || !lastUpdatesDownloaded[1].exists()) {
				return null;
			} else {
				return lastUpdatesDownloaded;
			} 
		} catch (Exception e) {
			updatesReadyToInstall.set(false);
			return null;
		}
	}
	
	private String getUpdatesUnavailableMessage() {
		return "<html><b> Could not retrieve updates! </b>"
				+ "<br><br>This could mean that there are updates to new version in progress, or that some files are no longer available."
				+ "<br>Retry later, if the problem persists contact administrator [" + manifestRemoteUrl + "]</html>";
	}
}
