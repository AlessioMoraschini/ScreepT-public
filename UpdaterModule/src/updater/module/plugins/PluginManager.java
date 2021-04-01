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
package updater.module.plugins;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import main.MainStandAloneUpdater;
import updater.module.gui.PluginManagerGUI;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.files.ZipWorker;
import various.common.light.gui.UpdateProgressPanel;
import various.common.light.network.http.HttpHelper;
import various.common.light.network.http.HttpHelper.HttpRequestMetod;
import various.common.light.network.utils.DownloadUtils;
import various.common.light.om.LimitedConcurrentList;
import various.common.light.om.exception.ProgressBarInterruptedException;
import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ArrayHelper;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.properties.PropertiesManager;

/**
 * This class contains the methods to manage plugins download, verification, and installation.
 * It also provides a local caching functionality to store temporaneus valid downloaded plugins.
 * @author Alessio Moraschini
 */
public class PluginManager {

	private static SafeLogger logger = new SafeLogger(PluginManager.class);

	public static final String DESCRIPTION_PROP_PREFIX = "DESCREEPTION_";

	public static final String installedPluginPropsFileName = "Installed_plugins.properties";
	public static final String installedPluginPropsListKey = "INSTALLED_LIST";
	public static final String installedPluginPropsListSeparator = ",";
	public static final String installedPluginPendingDeleteListKey = "PLUGINS_PENDING_DELETE_AT_START";

	public static final HashingFunction DEFAULT_HASHING_F_PLUGINS = HashingFunction.SHA512;

	public static PropertiesManager resourcesFilePathsMappings = new PropertiesManager(MainStandAloneUpdater.MAIN_APP_PROPERTIES_PATH);
	public static PropertiesManager installedPluginProperties = new PropertiesManager(installedPluginPropsFileName);

	public static final String LOCAL_PLUGINS_DIR = resourcesFilePathsMappings.getProperty("PLUGINS_DIR", "PLUGINS/");
	public static final String WEB_PLUGINS_BASE_URL = resourcesFilePathsMappings.getProperty("WEB_PLUGINS_BASE_URL", "https://www.am-design-development.com/ScreepT/Plugins/");
	public static final String WEB_PLUGINS_MANIFEST_URL = resourcesFilePathsMappings.getProperty("WEB_PLUGINS_MANIFEST_URL", "https://www.am-design-development.com/ScreepT/Plugins/manifest.properties");
	public static final String LOCAL_PLUGINS_MANIFEST = LOCAL_PLUGINS_DIR + "manifest.properties";
	public static final String PLUGIN_LIST_MANIFEST_KEY = "AvailablePluginsList";

	public static final File APPLICATION_ROOT_DIR = new File(System.getProperty("user.dir"));

	// This is the plugin informations cache, updated at each download. NB there is not data here, only informations about it
	private HashMap<String, PluginDTO> pluginLocalMap;

	public static LimitedConcurrentList<String> pluginsFreshlyInstalled = new LimitedConcurrentList<>(200);

	public ArrayList<String> installedPluginNames;

	public PluginManager() {
		pluginLocalMap = new HashMap<>();
		installedPluginNames = getInstalledPluginsList();

		try {
			File pluginsDir = new File(LOCAL_PLUGINS_DIR);
			if(!pluginsDir.exists()) {
				pluginsDir.mkdirs();
				pluginsDir.mkdir();
			}
		} catch (Exception e) {
			logger.warn("Cannot create updates dir (Resources_ScreepT/UPDATES)", e);
		}
	}

	///// PLUGINS /////////////

	/**
	 * Copies a downloaded plugin from current zipped folder into the correct folder of screept
	 * (by design plugins zipped folders are supposed to be relative to main /application directory)
	 * @param askUserIfCannotVerifyChecksum if this is true and hash cannot be verified, then user will be asked what to do.
	 * 	if this is false installation will be automatically stopped if files are not (or cannot be) validated.
	 * @throws Throwable if a generic error occur
	 * @throws ProgressBarInterruptedException if user stopped the process
	 */
	public PluginDTO installPlugin(PluginDTO plugin, boolean askUserIfUnverifiedChecksum) throws Throwable, ProgressBarInterruptedException {

		// Input validation
		if(plugin == null || plugin.localFile == null || !plugin.localFile.exists()) {
			return plugin;
		}

		plugin.setInstallationCompleted(false);

		// Verify checksum asking user what to do in case of problems
		boolean hashOk = checkPluginHash(plugin);
		if(!hashOk) {
			boolean throwHashException = false;
			if(askUserIfUnverifiedChecksum) {
				String askMessage = plugin.getName() + " -> File integrity cannot be verified, install anyway?";
				if(PluginManagerGUI.dialogHelper.yesOrNo(askMessage, "File verification failed")) {
					String message = "Hash [" + plugin.getCheckSum() + "] not verified for file: " + plugin.localFile;
					logger.error(message);
					throw new Exception(message);
				}
			} else {
				throwHashException = true;
			}

			if (throwHashException) {
				String message = "User has chosen to exit plugin installation: Hash [" + plugin.getCheckSum() + "] not verified for file: " + plugin.localFile;
				logger.warn(message);
				throw new ProgressBarInterruptedException(message);
			}
		}

		// Clear destination plugin folder if extraction dir is already present
		String folderName = FilenameUtils.removeExtension(plugin.getName());
		File destinationNamedDir = new File(LOCAL_PLUGINS_DIR + File.separator + folderName);
		if(destinationNamedDir.exists() && destinationNamedDir.isDirectory() && destinationNamedDir.list().length > 0) {
			logger.info("Deleting destinationNamedDir: " + destinationNamedDir);
			FileWorker.deleteDirContentRecursive(destinationNamedDir);
		}

		// Unzip file
		logger.info("Unzipping plugin: " + plugin.getLocalFile().getAbsolutePath());
		File unzippedPlugin = ZipWorker.unzipFile(plugin.getLocalFile().getAbsolutePath(), destinationNamedDir.getAbsolutePath(), true);
		plugin.setLocalExtractedDir(unzippedPlugin);

		// Copy content into current application root, and update mainAppRoot-relative file installed into properties file
		logger.info("Copying content of " + plugin.getLocalExtractedDir().getAbsolutePath() + " into " + APPLICATION_ROOT_DIR);
		FileWorker.copyDirectoryContents(plugin.getLocalExtractedDir(), APPLICATION_ROOT_DIR);
		ArrayList<String> relativeNewCopied = FileVarious.getPathsRelativeToOtherFile(FileVarious.listFiles(unzippedPlugin, new ArrayList<>()), unzippedPlugin);
		plugin.setInstallationCompleted(true);
		addToInstalledList(plugin);

		String installedList = PropertiesManager.listToString(relativeNewCopied, installedPluginPropsListSeparator);
		installedPluginProperties.commentedProperties.setProperty(plugin.getName(), installedList);
		installedPluginProperties.savePropertyChecked(plugin.getName(), installedList);
		logger.info(installedPluginPropsFileName + " -> Installed files list saved for current plugin (" + plugin.getName() + ") :" + installedList);
		installedPluginProperties.readFromFile();

		updateCache(plugin);

		pluginsFreshlyInstalled.addUniqueToTop(plugin.getName());

		return plugin;
	}

	public boolean removePlugin(PluginDTO pluginToRemove) {

		logger.info("Starting plugin removal...");

		boolean uninstallOK = true;
		ArrayList<String> pendingUndeleted = new ArrayList<>();

		if(pluginToRemove == null || pluginToRemove.getName() == null) {
			logger.error("Invalid input data: pluginToRemove or name is null. Exiting removePlugin method");
			return false;
		}

		ArrayList<String> strPluginFilesList = installedPluginProperties.getStringList(pluginToRemove.getName(), installedPluginPropsListSeparator);
		strPluginFilesList = filterSharedFiles(strPluginFilesList, pluginToRemove);

		for(String toDelete : strPluginFilesList) {
			File filePending = new File(toDelete);
			try {
				// Delete only if contained in current application DIR (Avoid accidental deletes of other files unrelated to ScreepT)
				if (FileUtils.directoryContains(APPLICATION_ROOT_DIR, filePending)) {
					if (toDelete != null && !"".equals(toDelete.trim()) && filePending.exists() && filePending.isFile()) {
						FileUtils.forceDelete(filePending);
						logger.info(filePending + " uninstall result (deleted = true): " + uninstallOK);
					}
				} else {
					logger.warn("Directory " + APPLICATION_ROOT_DIR + " does not contain the given file path: " + filePending + " => it's not possible to delete files that reside outside ScreepT's folder!");
				}
			} catch (Throwable e) {
				logger.warn("An error occurred while tryin' to uninstall " + pluginToRemove + " => file will be deleted on exit.", e);
				pendingUndeleted.add(toDelete);
				uninstallOK = false;
				logger.warn("Delete will be tried when JVM will exit (or during next restart by launcher)");

				try {
					Files.newOutputStream(filePending.toPath(), StandardOpenOption.DELETE_ON_CLOSE);
				} catch (Throwable e1) {
					logger.warn("forceDeleteOnExit not possible, an error occurred :( -> files will be deleted at next restart by Launcher", e1);
				}
			}
		}

		removeFromInstalledList(pluginToRemove);
		pluginToRemove.setInstallationCompleted(!uninstallOK);
		updateCache(pluginToRemove);
		logger.info("Cache updated: " + pluginLocalMap);

		pluginsFreshlyInstalled.removeAllOccurrencesByEquals(pluginToRemove.getName());

		if(!uninstallOK) {
			// if something went wrong with delete, mark files that weren't deleted to be deleted by launcher at start
			updatePendingDelete(pendingUndeleted);
			logger.info("Pending delete list updated: " + pendingUndeleted);
		}

		logger.info("Remove plugin procedure completed, exiting.");

		return uninstallOK;
	}


	/**
	 * Automatically download a single plugin into plugins folder.
	 * @param pluginToDownload
	 * @throws ProgressBarInterruptedException if user clicked on close during download process
	 */
	public PluginDTO downloadPlugin(PluginDTO targetPlugin) throws Throwable, ProgressBarInterruptedException {

		if(targetPlugin == null || targetPlugin.getCompleteURL() == null || "".equals(targetPlugin.getCompleteURL().trim())) {
			return null;
		}

		ArrayList<PluginDTO> fakeList = new ArrayList<>();
		fakeList.add(targetPlugin);

		ArrayList<PluginDTO> downloaded = downloadPlugins(fakeList);

		if(downloaded != null && !downloaded.isEmpty()) {
			return downloaded.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Automatically download given list into plugins folder.
	 * @param pluginsToDownload
	 * @throws ProgressBarInterruptedException if user clicked on close during download process
	 */
	public ArrayList<PluginDTO> downloadPlugins(ArrayList<PluginDTO> pluginsToDownload) throws Throwable, TimeoutException, ProgressBarInterruptedException{

		ArrayList<PluginDTO> downloadedList = new ArrayList<>();

		if(pluginsToDownload == null || pluginsToDownload.isEmpty()) {
			logger.warn("Invalid input data: null or empty list -> Exiting download procedure.");
			return downloadedList;
		}

		int downloadFilesNumber = pluginsToDownload.size();

		UpdateProgressPanel pluginManagerProgbar = null;

		try {
			pluginManagerProgbar = new UpdateProgressPanel(true);
			final UpdateProgressPanel finalProg = pluginManagerProgbar;
			pluginManagerProgbar.dialog.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					finalProg.toFront();
					finalProg.dialog.toFront();
				}
			});
			pluginManagerProgbar.getPb().setIndeterminate(true);
			pluginManagerProgbar.getGeneralBar().setIndeterminate(true);

			int i = 1;

			for(PluginDTO currentRemote : pluginsToDownload) {

				logger.info("Downloading plugin: " + currentRemote);

				try {
					int perc = (int)((float)(i*100)/(float)downloadFilesNumber);
					pluginManagerProgbar.update(perc, "Downloading "+currentRemote.completeURL, true);
					String localOut = LOCAL_PLUGINS_DIR + currentRemote.name;
					currentRemote.setLocalFile(DownloadUtils.downloadFile(false, new URL(currentRemote.completeURL), localOut, pluginManagerProgbar));

					downloadedList.add(currentRemote);

					// Use file name with intent to overwrite previously downloaded plugins with same name (each plugin has different name by design).
					// * permit an easy retrieve by key
					pluginLocalMap.put(currentRemote.name, currentRemote);

				} catch (Exception e) {
					removeFromCache(currentRemote);
					logger.error("Error downloading plugin, removed from cache.", e);
					throw e;
				}

				i++;
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		} finally {
			pluginManagerProgbar.forceClose();
		}

		logger.info("Plugin download completed!");
		return downloadedList;
	}

	/**
	 * Retrieve from web Last available plugins and return them as list of objects of type PluginDTO
	 * @throws Throwable
	 */
	public ArrayList<PluginDTO> discoverLatestPlugins() throws Throwable, TimeoutException{

		ArrayList<PluginDTO> plugins = new ArrayList<>();
		UpdateProgressPanel pluginManagerProgbar = null;

		try {
			if(!HttpHelper.is404(new URL(WEB_PLUGINS_MANIFEST_URL), HttpRequestMetod.GET)) {
				pluginManagerProgbar = new UpdateProgressPanel(true);
				pluginManagerProgbar.getPb().setIndeterminate(true);

				logger.info(FileVarious.getHeaderString("#", 40, "DOWNLOAD AND PARSE MANIFEST .properties"));
				File downloadedManifest = DownloadUtils.downloadFile(false, new URL(WEB_PLUGINS_MANIFEST_URL), LOCAL_PLUGINS_MANIFEST, pluginManagerProgbar);
				logger.info("Manifest Downloaded!! Size of " + WEB_PLUGINS_MANIFEST_URL + " is: " + ConversionUtils.coolFileSize(downloadedManifest.length()));

				PropertiesManager propsManager = new PropertiesManager(downloadedManifest);

				ArrayList<String> pluginNames = propsManager.getStringList(PLUGIN_LIST_MANIFEST_KEY, installedPluginPropsListSeparator);
				for(String pluginName : pluginNames) {
					String checkSum = propsManager.getProperty(pluginName);
					String completeUrl = WEB_PLUGINS_BASE_URL + pluginName;
					String descrKey = DESCRIPTION_PROP_PREFIX + pluginName;
					String description = propsManager.getProperty(descrKey, "Description not available.");
					PluginDTO toAdd = new PluginDTO(completeUrl, pluginName, checkSum);
					toAdd.setDescription(description);

					plugins.add(toAdd);
					updateCache(toAdd);
				}
			} else {
				logger.error("Plugin information list unavailable");
			}
		} catch (Throwable e){
			logger.error("Download procedure interrupted by an exception", e);
			throw e;
		}

		updateAlreadyInstalledIntoList(plugins);

		return plugins;
	}

	public void updateAlreadyInstalledIntoList(ArrayList<PluginDTO> plugins) {

		if(plugins == null || plugins.isEmpty()) {
			return;
		}

		ArrayList<String> installedList = getInstalledPluginsList();

		for(PluginDTO current : plugins) {
			if(installedList.contains(current.getName())) {
				current.installationCompleted = true;
			}
		}
	}

	/**
	 * Deletes all downloaded files and folders into PLUGINS directory, deleting also the cache.
	 * @return
	 */
	public boolean clearPluginsLocalDir() {
		boolean result = false;

		logger.info("Deleting local plugin directory's content: " + LOCAL_PLUGINS_DIR);

		try {
			File pluginsFolder = new File(LOCAL_PLUGINS_DIR);
			if(pluginsFolder.exists()) {
				result = FileWorker.deleteDirContentRecursive(pluginsFolder);
				clearCache();
				logger.info("Folder deleted and cache cleared");
			}
		} catch (Exception e) {
			result = false;
			logger.error("Cannot clear plugin directory: ", e);
		}

		logger.info("Cleanup completed, exiting delete procedure");
		return result;
	}

	public static ArrayList<String> getInstalledPluginsList() {
		installedPluginProperties.readFromFile();
		return installedPluginProperties.getStringList(installedPluginPropsListKey, installedPluginPropsListSeparator);
	}

	private void addToInstalledList(PluginDTO plugin) {
		if(!installedPluginNames.contains(plugin.getName())) {
			installedPluginNames.add(plugin.getName());
		}

		String stringList = PropertiesManager.listToString(installedPluginNames, installedPluginPropsListSeparator);
		installedPluginProperties.saveCommentedProperty(installedPluginPropsListKey, stringList);

		plugin.installationCompleted = true;
	}

	private void removeFromInstalledList(PluginDTO plugin) {
		if(installedPluginNames.contains(plugin.getName())) {
			installedPluginNames.remove(plugin.getName());
		}

		String stringList = PropertiesManager.listToString(installedPluginNames, installedPluginPropsListSeparator);
		installedPluginProperties.saveCommentedProperty(installedPluginPropsListKey, stringList);
		installedPluginProperties.removeProperty(plugin.getName());

		plugin.installationCompleted = false;
	}

	public List<File> filterFilesSharedWithOthers(List<File> files, PluginDTO dto){
		List<File> filtered = getAllInstalledFilesExceptPlugin(dto);
		List<File> output = new ArrayList<>();

		for(File file : files) {
			if(!FileVarious.containedInList(filtered, file)) {
				output.add(file);
			}
		}

		return output;
	}

	public Map<String, List<File>> getAllInstalledFilesMapExceptPlugin(PluginDTO plugin){
		Map<String, List<File>> allFiles = getAllInstalledFiles();
		if(allFiles.containsKey(plugin.getName()))
			allFiles.remove(plugin.getName());

		return allFiles;
	}

	public List<File> getAllInstalledFilesExceptPlugin(PluginDTO plugin){
		Map<String, List<File>> allFiles = getAllInstalledFiles();
		Iterator<String> it = allFiles.keySet().iterator();
		Set<File> filesFiltered = new HashSet<>();

		while(it.hasNext()) {
			String pluginName = it.next();
			if(!pluginName.equals(plugin.getName()))
				filesFiltered.addAll(allFiles.get(pluginName));
		}

		return ArrayHelper.setToList(filesFiltered);
	}

	public Map<String, List<File>> getAllInstalledFiles(){

		List<String> installed = getInstalledPluginsList();
		Map<String, List<File>> installedMap = new HashMap<>();

		for(String pluginName : installed) {
			List<File> installedFiles = installedPluginProperties.getFileList(pluginName, ";", true);
			installedMap.put(pluginName, installedFiles);
		}

		return installedMap;
	}

	/**
	 * Updates and save string list of relative paths of files to delete at next start by launcher.
	 * Existing pending deletes will be kept, if already present pending delete won't be written to list in installed_plugins.properties
	 *
	 * @param pendingRelativePaths
	 */
	public ArrayList<String> filterSharedFiles(ArrayList<String> pendingRelativePaths, PluginDTO toRemove) {

		List<File> filteredToRemove = FileVarious.pathsToFileList(pendingRelativePaths, true);
		filteredToRemove = filterFilesSharedWithOthers(filteredToRemove, toRemove);

		ArrayList<String> filteredStr = new ArrayList<>();
		for(String s : pendingRelativePaths) {
			if(!FileVarious.containedInList(filteredToRemove, FileVarious.getCanonicalFileSafe(s))) {
				filteredStr.add(s);
			}
		}

		return filteredStr;
	}

	/**
	 * Updates and save string list of relative paths of files to delete at next start by launcher.
	 * Existing pending deletes will be kept, if already present pending delete won't be written to list in installed_plugins.properties
	 *
	 * @param pendingRelativePaths
	 */
	public void updatePendingDelete(ArrayList<String> pendingRelativePaths) {
		ArrayList<String> currentlyPending = installedPluginProperties.getStringList(installedPluginPendingDeleteListKey, ";");

		if (pendingRelativePaths != null && !pendingRelativePaths.isEmpty()) {
			for (String currentPending : pendingRelativePaths) {
				if (!currentlyPending.contains(currentPending)) {
					currentlyPending.add(currentPending);
				}
			}
		} else {
			return;
		}

		installedPluginProperties.savePropertyChecked(
				installedPluginPendingDeleteListKey,
				PropertiesManager.listToString(currentlyPending, ";"));
	}

	public boolean isInstalled(PluginDTO plugin) {
		if(plugin == null || plugin.getName() == null) {
			return false;
		}

		ArrayList<String> installed = getInstalledPluginsList();
		return installed.contains(plugin.getName());
	}

	public boolean checkPluginHash(PluginDTO pluginToCheck) throws Throwable {
		if(pluginToCheck != null) {
			String checkSum = pluginToCheck.getCheckSum();
			File localZipFile = pluginToCheck.getLocalFile();

			if(checkSum != null && !"".equals(checkSum.trim()) && localZipFile != null && localZipFile.exists() && localZipFile.isFile()) {
				Boolean hashOk = ChecksumHasher.checkFileHash(localZipFile, checkSum, DEFAULT_HASHING_F_PLUGINS);
				return hashOk;
			}
		}

		return false;
	}

	// PLUGINS - LOCAL STATIC CACHE IN MEMORY //

	public void clearCache() {
		pluginLocalMap.clear();
	}

	public ArrayList<PluginDTO> getAllFromCache(boolean getValidOnly) throws Throwable{
		ArrayList<PluginDTO> returnList = new ArrayList<>();
		PluginDTO currentCached;
		PluginDTO cachedValid;

		for(String currentKey : pluginLocalMap.keySet()) {
			currentCached = pluginLocalMap.get(currentKey);
			if(currentCached != null) {
				if(!getValidOnly) {
					returnList.add(currentCached);

				} else {

					cachedValid = getValidFromCache(currentKey);
					if(cachedValid != null) {
						returnList.add(cachedValid);
					}
				}
			}
		}

		return returnList;
	}

	/**
	 * Check if there is a valid plugin into local cache, checking if Url, local file, and correct hash are present into localCache
	 *
	 * If found one, it will be returned. if not found a valid one, then method will return null.
	 * @throws Throwable
	 */
	public PluginDTO getValidFromCache(String pluginName) throws Throwable {

		if(pluginName == null || "".equals(pluginName.trim())) {
			return null;
		}

		boolean alreadyInCache = pluginLocalMap.containsKey(pluginName);
		PluginDTO pluginCacheVal = pluginLocalMap.get(pluginName);
		boolean usablePlugin =
				alreadyInCache
				&& pluginCacheVal != null
				&& pluginCacheVal.completeURL != null
				&& !"".equals(pluginCacheVal.completeURL);

		Boolean hashOk = ChecksumHasher.checkFileHash(pluginCacheVal.localFile, pluginCacheVal.checkSum, DEFAULT_HASHING_F_PLUGINS);

		if(usablePlugin && hashOk) {
			return pluginCacheVal;
		} else {
			return null;
		}
	}

	public ArrayList<PluginDTO> retrieveAllFromCache(){
		ArrayList<PluginDTO> cachedPluginsList = new ArrayList<>();

		cachedPluginsList.addAll(pluginLocalMap.values());

		return cachedPluginsList;
	}

	public PluginDTO retrieveFromCache(String pluginFileName) {
		return pluginLocalMap.get(pluginFileName);
	}

	public PluginDTO retrieveFromCacheByNameNoExtension(String nameWithoutExtension) {
		for(String key : pluginLocalMap.keySet()) {
			if(key.contains(nameWithoutExtension)) {
				return pluginLocalMap.get(key);
			}
		}

		return null;
	}

	public void refreshCache() {
		refreshCache((String[])null);
	}

	public void refreshCache(String... pluginNamesNoExt) {
		try {
			List<PluginDTO> updatedFromWeb = discoverLatestPlugins();
			for(PluginDTO dto : updatedFromWeb) {
				boolean match = pluginNamesNoExt == null || pluginNamesNoExt.length == 0 || Arrays.asList(pluginNamesNoExt).contains(FilenameUtils.removeExtension(dto.getName()));
				if(match)
					updateCache(dto);
			}
		} catch (Throwable e) {
			logger.error("Can't refresh plugin cache!", e);
		}
	}

	public PluginDTO updateCache(PluginDTO pluginToUpdate) {
		if(pluginToUpdate == null) {
			return null;
		}

		return pluginLocalMap.put(pluginToUpdate.getName(), pluginToUpdate);
	}

	public ArrayList<PluginDTO> updateCache(ArrayList<PluginDTO> pluginsToUpdate) {
		if(pluginsToUpdate == null || pluginsToUpdate.isEmpty()) {
			return pluginsToUpdate;
		}

		ArrayList<PluginDTO> addedList = new ArrayList<>();

		for(PluginDTO currentToAdd : pluginsToUpdate) {
			addedList.add(pluginLocalMap.put(currentToAdd.getName(), currentToAdd));
		}

		return addedList;
	}

	public PluginDTO removeFromCache(PluginDTO pluginToUpdate) {
		if(pluginToUpdate == null) {
			return null;
		}

		return pluginLocalMap.remove(pluginToUpdate.getName());
	}

	public ArrayList<PluginDTO> removeFromCache(ArrayList<PluginDTO> pluginsToUpdate) {
		if(pluginsToUpdate == null || pluginsToUpdate.isEmpty()) {
			return pluginsToUpdate;
		}

		ArrayList<PluginDTO> removedList = new ArrayList<>();

		for(PluginDTO currentToRemove : pluginsToUpdate) {
			if(pluginLocalMap.remove(currentToRemove.getName(), currentToRemove)) {
				removedList.add(currentToRemove);
			}
		}

		return removedList;
	}

	public String getFileAsStringListFromFolderRelative(File containerFolder, File relativeToThis) throws IOException {
		File relativeReference = (relativeToThis != null && relativeToThis.exists()) ? relativeToThis : APPLICATION_ROOT_DIR;

		ArrayList<File> allFiles = FileVarious.listFiles(containerFolder, new ArrayList<File>());
		ArrayList<String> relativePahs = new ArrayList<>();
		for(File current : allFiles) {
			relativePahs.add(FileVarious.getPathRelativeToOtherFile(current, relativeReference));
		}

		return PropertiesManager.listToString(relativePahs, installedPluginPropsListSeparator);
	}
}
