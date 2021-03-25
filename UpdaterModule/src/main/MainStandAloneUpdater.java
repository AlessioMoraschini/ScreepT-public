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
package main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.ini4j.Wini;

import main.om.PropertiesDTO;
import various.common.light.files.CustomFileFilters;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.files.ZipWorker;
import various.common.light.gui.UpdateProgressPanel;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.network.ftp.FtpDumper;
import various.common.light.network.progress.UploadAction;
import various.common.light.network.utils.DownloadUtils;
import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.properties.PropertiesManager;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.string.StringWorker.EOL;

public class MainStandAloneUpdater {

	public static SafeLogger logger = new SafeLogger(MainStandAloneUpdater.class);

	public static final String CMD_FLAG_GENERATE_HASHES = "-generateHashes";
	public static final String CMD_FLAG_ZIP_FOLDERS = "-zipFolders";
	public static final String CMD_FLAG_ZIP_FOLDERS_EXCL_PARENT = "-excludeParent";
	public static final String CMD_FLAG_MAIN_RELEASE = "-mainRelease";
	public static final String CMD_FLAG_UPLOAD_FTP = "-uploadFtpUpdates";

	public static final String UPDATER_LOG_FILE_NAME = "updater_history";
	public static final String UPDATER_LOG_FILE = "./" + UPDATER_LOG_FILE_NAME + ".log";

	public static final File CURRENT_DIR = new File(System.getProperty("user.dir"));
	public static final String THIS_JAR_PATH = "updater.jar";

	// Main application update static named files
	public static final String MAIN_LAUNCHER_JAR = "ScreepT_Launcher.jar";
	public static final String MAIN_APP_PROPERTIES_PATH = "resourcesFilePathsMappings.properties";

	// Other functionalities
	public static final String FTP_PROPERTIES_FILE = "FTP.properties";
	public static String UPDATES_PROPERTIES_FILE = "manifest.properties";

	// Properties file keys
	public final static String KEY_DEFAULT_LAUNCHER_INI_PATH = "DEFAULT_LAUNCHER_INI_PATH";
	private static final String KEY_ROOT_FILES_UPDATES_DIR = "ROOT_DIR_APP_FILES";
	private static final String KEY_FOLDER_ZIP_TO_ELABORATE = "RELEASE_FOLDER-OR-ZIP_TO_ELABORATE";
	private static final String KEY_FOLDERS_TO_SKIP = "FOLDERS_TO_SKIP";


	private static final JOptionHelper dialogHelper = new JOptionHelper(null);
	public static final HashingFunction downloadsHashFunc = HashingFunction.SHA512;
	/**
	 * This is main application updater module, runnable as main class of updater.jar
	 * @param args -> args[0]= mainApplication jar ; args[1]=  updates extracted folder
	 */
	public static void main(String[] args) {

		try {
			logger.setLogFileEol(null);
			logger.setLogFile(new File(UPDATER_LOG_FILE));
			logger.setFlagWriteOnFile(true);
			FileWorker.logger = new SafeLogger(FileWorker.class, true, logger.getLogFile(), true);
			ZipWorker.logger = new SafeLogger(ZipWorker.class, true, logger.getLogFile(), true);
			DownloadUtils.logger = new SafeLogger(DownloadUtils.class, true, logger.getLogFile(), true);
			FtpDumper.logger = new SafeLogger(FtpDumper.class, true, logger.getLogFile(), true);
			ChecksumHasher.logger = new SafeLogger(ChecksumHasher.class, true, logger.getLogFile(), true);
			PropertiesManager.logger = new SafeLogger(PropertiesManager.class, true, logger.getLogFile(), true);

			logger.info("\n###############################################################" + EOL.defaultEol.eol +
					"# updater.jar - Starting - " + new Date().toString() + EOL.defaultEol.eol +
					"###############################################################" + EOL.defaultEol.eol);

			mainArgsOrhcestrator(args);

		} catch (Exception e) {
			signalErrorAndExit1(e);
		}
	}

	private static void mainArgsOrhcestrator(String[] args) {

		if (args[0].trim().contains(CMD_FLAG_ZIP_FOLDERS)) {
			logger.info("\n\n\n******************************************************"
					+ "\n* ZipFolders detected:   -zipFolders  *"
					+ "\n*  Use -zipFolders" + CMD_FLAG_ZIP_FOLDERS_EXCL_PARENT + " to skip parent folder inside zip *"
					+ "\n******************************************************/\n\n");

			@SuppressWarnings("unused")
			List<File> zippedOutput = zipFolders(args);
		}

		if (args[0].trim().contains(CMD_FLAG_GENERATE_HASHES)) {

			List<File> toUpload = new ArrayList<>();

			if (args[0].trim().contains(CMD_FLAG_MAIN_RELEASE)) {
				logger.info("\n\n\n******************************************************"
						+ "\n* MainRelease detected: -generateHashes-mainRelease  *"
						+ "\n******************************************************/\n\n");
				/** Usage -> generateHashes(mainRelease) function: java -jar updater.jar -generateHashes-mainRelease true manifest.properties**/
				toUpload = mainGenerateReleaseHashed(args);

			} else {
				logger.info("\n\n\n*******************************************"
						+ "\n* Hash updates detected: -generateHashes  *"
						+ "\n*******************************************/\n\n");
				/********************************************************************************************************
				 * 	Usage -> generateHashes function: java -jar updater.jar -generateHashes true manifest.properties
				 ********************************************************************************************************/
				toUpload = mainGenerateUpdatesHashed(args);
			}

			if(args[0].trim().contains(CMD_FLAG_UPLOAD_FTP)) {

				logger.info("\n\n\n*******************************************"
						+ "\n* Upload flag detected: -uploadFtpUpdates *"
						+ "\n*******************************************/\n\n");
				/********************************************************************************************************
				 * Function A.1: "-generateHashes-uploadFtpUpdates" (reads ftp properties from FTP_PROPERTIES file)
				 * 	Usage -> generateHashes function:
				 *
				 * java -jar updater.jar -generateHashes-uploadFtpUpdates true manifest.properties
				 * java -jar updater.jar -generateHashes-uploadFtpUpdates-mainRelease true manifest.properties
				 ********************************************************************************************************/
				mainUploadUpdatesViaFTP(toUpload);
			}

			signalOKAndExit0();

		} else {

			logger.info("\n\n\n****************************"
					+ "\n* App embedded Update detected   *"
					+ "\n****************************/\n\n");
			/**********************************************************************************************************
			 * Function B: update ScreepT
			 * Usage -> update ScreepT function: java -jar updater.jar updater.jar UPDATES/updates ScreepT_0.9.5.jar
			 **********************************************************************************************************/
			mainUpdateScreepT(args);
		}
	}

	private static void mainUpdateScreepT(String[] args) {

		PropertiesManager resourcesFilePathsMappings = new PropertiesManager(MAIN_APP_PROPERTIES_PATH);

		final File updaterJar = new File(args[0]).getAbsoluteFile();
		final File updatesFolder = new File(args[1]).getAbsoluteFile();
		final File newJar = new File(args[2]).getAbsoluteFile();

		String manifestPath = updatesFolder.getAbsolutePath() + File.separator + UPDATES_PROPERTIES_FILE;
		PropertiesManager manifestProps = new PropertiesManager(manifestPath);

		// ScreepTVx.x/application/
		File generalApplicationFolder = new File("").getAbsoluteFile();
		// ScreepTVx.x/
		File mainRootDirectory = generalApplicationFolder.getParentFile();

		UpdateProgressPanel updateProgressMonitor = null;
		try {
			if(!validateUpdatesFolder(updaterJar, updatesFolder, manifestProps)) {
				launchJar(MAIN_LAUNCHER_JAR);
			}

			File oldJar = getOldJar(resourcesFilePathsMappings);

			if(updaterJar.exists() && updaterJar.isFile() && updatesFolder.exists() && updatesFolder.isDirectory()){

				// Current files backup - ask to user
				boolean proceed = false;
				do {
					Integer choice = dialogHelper.yesNoOrCanc(
							"ScreepT will be updated, do you want to backup current installation before to update?" +
						    " (update process will not touch your personal files, but if you prefer you can backup all ScreepT folder before to update)",
						    "Backup current Version files?");

					if (JOptionHelper.OK == choice) {
						boolean validDirectory = true;
						do {
							File destBackupDir = FileVarious.directoRead(null, "Select a backup destination, or proceed without backup. A unique \"ScreepT_updater_backup\" folder will be created in the specified destination.", new File(System.getProperty("user.home")));
							if (destBackupDir != null && destBackupDir.exists()) {
								destBackupDir = new File(destBackupDir.getCanonicalPath() + File.separator + "ScreepT_updater_backup");
								destBackupDir = FileWorker.renameJavaObjFile(destBackupDir);
								FileWorker.copyDirectoryContents(mainRootDirectory, destBackupDir);
							} else {
								validDirectory = !dialogHelper.yesOrNo("Destination (" + destBackupDir + ") is not valid: wanna choose another one?", "Invalid Backup destination");
							}
						} while (!validDirectory);
					}

					if (JOptionHelper.CANC == choice) {
						if(dialogHelper.yesOrNo("Are you sure to exit update Procedure?", "Exit procedure?")) {
							launchJar(MAIN_LAUNCHER_JAR);
						}
					} else {
						proceed = true;
					}

				} while(!proceed);

				// Proceed with update
				updateProgressMonitor = new UpdateProgressPanel(true, true);
				updateProgressMonitor.update(30, "Copying new files...");

				PropertiesDTO backupProperties = getPropertiesToKeep(resourcesFilePathsMappings);

				try {
					Thread.sleep(2000);
				} catch (Exception e1) {
					logger.error("", e1);;
				}

				// Check if there is Root files update to copy one level up
				File rootDirUpdates = new File(updatesFolder.getAbsolutePath() + File.separator + KEY_ROOT_FILES_UPDATES_DIR);
				if(rootDirUpdates.exists()) {
					try {
						updateProgressMonitor.update(50, "Updating licenses and launcher...", true);
						FileWorker.copyDirectoryContents(rootDirUpdates, mainRootDirectory);
						boolean deleted = FileWorker.deleteDirContentInclRoot(rootDirUpdates);
						if(deleted)
							logger.info(rootDirUpdates.getAbsolutePath() + " copied and deleted.");
						else
							logger.warn("Something may be not copied and/or deleted.");
					} catch (Exception e) {
						logger.error("", e);
					}
				}

				try {
					Thread.sleep(2000);
				} catch (Exception e1) {
					logger.error("", e1);
				}
				// replace existing files matching with updates (don't touch user personal files)
				String msg = "Copying content of " + updatesFolder + " into " + generalApplicationFolder;
				updateProgressMonitor.update(70, msg, true);
				logger.info(msg);
				FileWorker.copyDirectoryContents(updatesFolder, generalApplicationFolder);

				logger.info("Updating configuration...");
				updateProgressMonitor.update(85, 100, "Updating configuration...");
				restorePropertiesToKeep(backupProperties, MAIN_APP_PROPERTIES_PATH);
				saveNewJarRef(resourcesFilePathsMappings, newJar);

				logger.info("deleting old jar...");
				FileUtils.deleteQuietly(oldJar);

				logger.info("Completed!");
				updateProgressMonitor.update(100, "Completed! Restarting ScreepT...", true);

			} else {
				String fileData = "UpdaterJar => "+updaterJar+"; updates folder => "+updatesFolder;
				dialogHelper.error("Updates data or updater jar not found, exiting updated procedure." + fileData, " (Updates not found)");
			}

			launchJar(MAIN_LAUNCHER_JAR);

		} catch (Throwable e) {

			dialogHelper.errorScroll("Cannot update, an error occurred: " + e.getClass(), e.getClass().getCanonicalName(), e);
			logger.error(e.getClass().getCanonicalName(), e);

			try {
				launchJar(MAIN_LAUNCHER_JAR);
			} catch (IOException e1) {
				logger.error(e.getClass().getCanonicalName(), e);
			}

		} finally {
			if (updateProgressMonitor != null) {
				updateProgressMonitor.forceClose();
			}
			logger.info("Exiting updater. Bie bie ;)");
			System.exit(0);
		}
	}

	private static ArrayList<File> mainGenerateUpdatesHashed(String[] args) {

		Boolean createUpdatesZip = Boolean.valueOf(args[1]);
		UPDATES_PROPERTIES_FILE = args[2];
		PropertiesManager manifestProps = new PropertiesManager(UPDATES_PROPERTIES_FILE);

		File updatesDir = new File("updates");
		// Only for updates folder -> zip folder into current one
		if(createUpdatesZip && updatesDir.isDirectory() && updatesDir.exists()) {
			try {
				ZipWorker.zipFolder(updatesDir.getAbsolutePath(), CURRENT_DIR, true, true);
			} catch (Exception e) {
				signalErrorAndExit1(e);
			}
		}

		ArrayList<File> toUpload = new ArrayList<>();
		toUpload.add(new File(UPDATES_PROPERTIES_FILE));
		for(File curr : CURRENT_DIR.listFiles()) {

			if (curr.isFile() &&
				!curr.getName().endsWith(".bat") &&
				!curr.getName().endsWith(".log") &&
				!curr.getName().equals(UPDATES_PROPERTIES_FILE) &&
				!curr.getName().equals(FTP_PROPERTIES_FILE)) {

				try {
					String hash = ChecksumHasher.getFileHashBuffered(curr.getName(), downloadsHashFunc, true);
					manifestProps.saveCommentedProperty(curr.getName(), hash);
					manifestProps.readFromFile();
					if(!manifestProps.getProperty(curr.getName(), "").equals(hash)) {
						new JOptionHelper(null).warn("Hash control not passed: check properties for " + curr.getName(), "Hash not matching");
					} else {
						toUpload.add(curr);
					}
				} catch (Throwable e) {
					signalErrorAndExit1(e);
				}
			}

		}

		return toUpload;
	}

	private static List<File> zipFolders(String[] args) {

		boolean excludeParent = args[0].contains(CMD_FLAG_ZIP_FOLDERS + CMD_FLAG_ZIP_FOLDERS_EXCL_PARENT);
		UPDATES_PROPERTIES_FILE = args[2];

		logger.info("Src: " + CURRENT_DIR);

		File[] subFolders = CURRENT_DIR.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.DIR_ONLY));
		List<File> zippedFolders = new ArrayList<>();
		List<File> zipErrorFolders = new ArrayList<>();

		List<File> foldersToSkip = new PropertiesManager(UPDATES_PROPERTIES_FILE).getFileList(KEY_FOLDERS_TO_SKIP, ",", true);

		if(subFolders != null) {
			for (File folder : subFolders) {

				if(FileVarious.containedInList(foldersToSkip, folder))
					continue;

				try {
					// zip the wanted folder and place the zipped file into current directory
					logger.info("Zipping Src folder: " + folder.getAbsolutePath());
					zippedFolders.add(ZipWorker.zipFolder(folder.getAbsolutePath(), CURRENT_DIR, true, excludeParent));
					logger.info("Src folder zipped! Result file is: " + folder.getAbsolutePath());

				} catch (Exception e) {
					logger.error("Cannot zip folder: " + folder, e);
					zipErrorFolders.add(folder);
				}
			}

			logger.info("Zip completed. Zipped folders are: \n" + FileVarious.getStringFromFiles(zippedFolders, EOL.getOsBasedEOL().eol));
			logger.info("Zip completed. Zip-failed folders are: \n" + FileVarious.getStringFromFiles(zipErrorFolders, EOL.getOsBasedEOL().eol));

		} else {
			logger.warn("No folder to zip found inside: " + CURRENT_DIR);
		}

		if(!zipErrorFolders.isEmpty() && !dialogHelper.yesOrNo("Some folders have not been zipped (see log). Proceed anyway?")) {
			signalErrorAndExit1(new Exception(
					FileVarious.getStringFromFiles(zipErrorFolders, EOL.getOsBasedEOL().eol)));
		}

		return zippedFolders;
	}

	private static List<File> mainGenerateReleaseHashed(String[] args) {

		Boolean createUpdatesZip = Boolean.valueOf(args[1]);
		UPDATES_PROPERTIES_FILE = args[2];

		File releaseFolderOrZip = new File(new PropertiesManager(FTP_PROPERTIES_FILE).getProperty(KEY_FOLDER_ZIP_TO_ELABORATE));
		logger.info("Src: " + releaseFolderOrZip.getAbsolutePath());

		// Only for folder to be zipped -> zip folder into current one
		if(createUpdatesZip && releaseFolderOrZip.isDirectory() && releaseFolderOrZip.exists()) {
			try {
				// zip the wanted folder and place the zipped file into current directory
				logger.info("Zipping Src folder: " + releaseFolderOrZip.getAbsolutePath());
				releaseFolderOrZip = ZipWorker.zipFolder(releaseFolderOrZip.getAbsolutePath(), CURRENT_DIR, true, true);
				logger.info("Src folder zipped! Result file is: " + releaseFolderOrZip.getAbsolutePath());
			} catch (Exception e) {
				signalErrorAndExit1(e);
			}
		}

		ArrayList<File> toUpload = new ArrayList<>();

		File updaterJarFile = new File(THIS_JAR_PATH);
		if (updaterJarFile.exists()) {
			toUpload.add(updaterJarFile);
		}

		if(releaseFolderOrZip != null && releaseFolderOrZip.isFile() && StringWorker.trimToEmpty(releaseFolderOrZip.getName()).endsWith(".zip")) {

			try {
				PropertiesManager manifestProps = new PropertiesManager(UPDATES_PROPERTIES_FILE);

				if (updaterJarFile.exists()) {
					String updaterHash = ChecksumHasher.getFileHashBuffered(THIS_JAR_PATH, downloadsHashFunc, true);
					manifestProps.saveCommentedProperty(THIS_JAR_PATH, updaterHash);
				}
				String hash = ChecksumHasher.getFileHashBuffered(releaseFolderOrZip, downloadsHashFunc, true);
				manifestProps.saveCommentedProperty(releaseFolderOrZip.getName(), hash);
				manifestProps.saveAllProperties();
				manifestProps.readFromFile();
				if (!manifestProps.getProperty(releaseFolderOrZip.getName(), "").equals(hash)) {
					new JOptionHelper(null).warn(
							"Hash control not passed: check properties for " + releaseFolderOrZip.getName(), "Hash not matching");
				} else {
					toUpload.add(releaseFolderOrZip);
				}
			} catch (Throwable e) {
				signalErrorAndExit1(e);
			}
		}

		toUpload.add(new File(UPDATES_PROPERTIES_FILE));

		return toUpload;
	}


	////////////////////// JAR METHODS //////////////////////////

	private static File getOldJar(PropertiesManager resourcesFilePathsMappings) {
		try {
			String DEFAULT_LAUNCHER_INI_PATH = resourcesFilePathsMappings.getProperty(MainStandAloneUpdater.KEY_DEFAULT_LAUNCHER_INI_PATH, "LAUNCHER/Launcher_Configuration.ini");
			Wini iniLauncherConfig = new Wini(new File(DEFAULT_LAUNCHER_INI_PATH));
			String oldJarName = iniLauncherConfig.get("APPLICATION_OPTIONS", "jarToLaunch");
			return (oldJarName != null) ? new File(oldJarName) : null;
		} catch (Exception e) {
			logger.error("Cannot retrieve old jar", e);
			return null;
		}
	}

	private static void saveNewJarRef(PropertiesManager resourcesFilePathsMappings, File newJar) {
		try {
			String DEFAULT_LAUNCHER_INI_PATH = resourcesFilePathsMappings.getProperty(MainStandAloneUpdater.KEY_DEFAULT_LAUNCHER_INI_PATH, "LAUNCHER/Launcher_Configuration.ini");
			Wini iniLauncherConfig = new Wini(new File(DEFAULT_LAUNCHER_INI_PATH));

			iniLauncherConfig.put("APPLICATION_OPTIONS", "jarToLaunch", newJar.getName());
			iniLauncherConfig.store();
		} catch (Exception e) {
			logger.error("Cannot save new jar reference", e);
		}
	}

	private static void launchJar(String launcherPath) throws IOException {

		logger.info("Launching main jar!");
		Runtime runtime = Runtime.getRuntime();
		runtime.exec("java -jar " + launcherPath);

		System.exit(0);
	}

	/**
	 * Updates Hashes vaildation
	 */
	private static boolean validateUpdatesFolder(File updaterJar, File updatesFolder, PropertiesManager manifestProps) {
		// .... /UPDATES/updates.zip  ::  (updatesFolder = .... /UPDATES/updates/)
		File zipUpdates = new File(updatesFolder.getParentFile() + File.separator + "updates.zip");
		boolean proceed = true;
		try {
			proceed = (!ChecksumHasher.checkHashFromProperties(updaterJar, manifestProps, downloadsHashFunc)) ? false : proceed;
			proceed = (!ChecksumHasher.checkHashFromProperties(zipUpdates, manifestProps, downloadsHashFunc)) ? false : proceed;
		} catch (Throwable e) {
			logger.error("Cannot proceed with update procedure, error while validating checksum!", e);
			proceed = false;
		}

		if(!proceed) {
			if(dialogHelper.yesOrNo("It was not possible to verify integrity of downloaded updates, install anyway?")) {
				proceed = true;
			} else {
				proceed = false;
			}
			logger.info("User has chosen to continue: " + proceed);
		}

		return proceed;
	}

	// BACKUP-RESTORE PROPERTIES
	public static PropertiesDTO getPropertiesToKeep(PropertiesManager resourcesFilePathsMappings) {
		PropertiesDTO propertiesDTO = new PropertiesDTO();
		propertiesDTO.proxyConfString = resourcesFilePathsMappings.getProperty(PropertiesDTO.DEF_PROXY_CONFIG_KEY);
		return propertiesDTO;
	}
	public static void restorePropertiesToKeep(PropertiesDTO backupProperties, String propertiesFilePath) {
		PropertiesManager resourcesFilePathsMappings = new PropertiesManager(propertiesFilePath);
		resourcesFilePathsMappings.saveCommentedProperty(PropertiesDTO.DEF_PROXY_CONFIG_KEY, backupProperties.proxyConfString);
	}

	/**
	 *  SIGNAL KO BEFORE EXIT
	 */
	private static void signalErrorAndExit1(Throwable e) {
		logger.error("\n\n\n*******************"
				+ "\n* PROCESS FAILED! *"
				+ "\n*******************/\n");
		logger.error("# error detected at: " + new Date().toString());
		logger.error("An error occurred: ",e);
		try {
			Thread.sleep(10000);
			System.exit(1);
		} catch (InterruptedException e1) {
		}
	}

	/**
	 *  SIGNAL OK BEFORE EXIT
	 */
	private static void signalOKAndExit0() {
		logger.info("\n\n\n*********************"
				+ "\n* COMPLETE SUCCESS! *"
				+ "\n*********************/\n");
		logger.info("# Completed at: " + new Date().toString());
		logger.info("Finished! Updater will close after 5 seconds from now...");
		try {
			Thread.sleep(5000);
			System.exit(0);
		} catch (InterruptedException e1) {
		}
	}

	/**
	 * FTP update mode - Developer usage only
	 */
	private static void mainUploadUpdatesViaFTP(List<File> toUploadList) {

		if(toUploadList == null || toUploadList.isEmpty()) {
			logger.info("Nothing to upload!");
			return;
		} else {
			logger.info("Files to upload:" + EOL.defaultEol.eol + StringWorker.concatFilePaths(toUploadList, EOL.defaultEol));
		}

		PropertiesManager ftpProperties;
		try {
			ftpProperties = new PropertiesManager(FTP_PROPERTIES_FILE);
		} catch (Exception e) {
			logger.error("Cannot read properties file: " + FTP_PROPERTIES_FILE, e);
			throw e;
		}

		int CONNECTION_TIMEOUT_MS = ftpProperties.getIntVarFromProps("CONNECTION_TIMEOUT_MS", 25000);
		String HOST = ftpProperties.getProperty("HOST", "ftp.am-design-development.com");
		String USER = ftpProperties.getProperty("USER", "");
		String PSW = ftpProperties.getProperty("PSW", "");
		int PORT = ftpProperties.getIntVarFromProps("PORT", 22);
		String FOLDER_ON_SERVER = ftpProperties.getProperty("FOLDER_ON_SERVER", "/www.am-design-development.com/ScreepT/Updates");

		FtpDumper dumper = null;
		FtpDumper.setCONNECTION_TIMEOUT(CONNECTION_TIMEOUT_MS);
		boolean validLogin = false;
		int tries = 0;
		while (!validLogin) {
			if ( tries > 0 || StringWorker.isEmpty(USER)) {
				USER = new JOptionHelper(null).askForString("Enter USERNAME", "Username", USER);
			}

			if (PSW.equals("")) {
				char[] psw = new JOptionHelper(null).askForPasswordNullable("Enter FTPS password for user:" + USER, "Insert FTPS password", 64, "Upload");
				if (psw != null) {
					PSW = new String(psw);
				}
			}
			UpdateProgressPanel panel = new UpdateProgressPanel();
			panel.setUndefinedDuration();
			panel.setClosable(false);
			panel.update(20, "Checking connection to FTPS server...");
			dumper = new FtpDumper(HOST, USER, PSW, PORT);
			validLogin = dumper.ftps != null && dumper.isConnected();
			UpdateProgressPanel.forceDisposeSafe(panel);
			if(!validLogin) {
				dialogHelper.error("Login failed");
				PSW = "";
			}

			tries++;
		}

		UpdateProgressPanel progPanel = new UpdateProgressPanel(true);
		progPanel.dialog.setTitle("Uploading files");
		// Remove remote manifest before to start upload, and upload separately after other files:
		// -> this will avoid users to start updates during the update process.
		File manifestFound = findManifestAndMoveAtTheEnd(toUploadList);
		if(manifestFound != null) {
			try {
				logger.info("Deleting remote manifest: " + UPDATES_PROPERTIES_FILE);
				boolean deleted = dumper.deleteFile(FOLDER_ON_SERVER, manifestFound.getName());
				logger.info("Remote manifest deleted? -> " + deleted);
			} catch (Exception e) {
				logger.warn("Cannot delete remote " + UPDATES_PROPERTIES_FILE + " before to upload!", e);
			}
		}

		try {
			int i = 1;
			for (File currentToUpload : toUploadList) {
				logger.info("Uploading: " + currentToUpload + " ...");

				dumper.setUploadAction(getUploadAction(progPanel, currentToUpload.length(), currentToUpload.getName()));
				int generalProgress = i*100 / toUploadList.size();
				generalProgress = generalProgress < 100 ? generalProgress : 99;
				progPanel.update(generalProgress, currentToUpload + " (" + String.valueOf(i) + "/" + toUploadList.size() + ")", true);
				progPanel.adaptWidthToCurrentText();

				dumper.uploadFile(currentToUpload.getAbsolutePath(), currentToUpload.getName(), FOLDER_ON_SERVER);
				logger.info(currentToUpload + " -> Uploaded");

				i++;
			}

		} catch (Throwable e) {
			e.printStackTrace();

		} finally {
			progPanel.forceClose();
			dumper.disconnect();

			new JOptionHelper(null).info("FTP upload completed!", "Upload completed");
		}
	}

	private static UploadAction getUploadAction(UpdateProgressPanel progPanel, long fileSize, String fileName) {
		return new UploadAction() {
			@Override
			public void doAfterUploadProgress(int percent, String currentFile) {
				progPanel.update(percent, null, false);
			}
		};
	}

	private static File findManifestAndMoveAtTheEnd(List<File> readyToDownload) {
		for(File current : readyToDownload) {
			if(UPDATES_PROPERTIES_FILE.equals(current.getName())) {
				readyToDownload.remove(current);
				readyToDownload.add(current); // Moves the file at the end of the list

				return current;
			}
		}

		return null;
	}

}
