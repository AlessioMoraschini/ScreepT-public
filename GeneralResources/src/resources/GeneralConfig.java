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
package resources;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

import os.commons.om.OSinfo;
import os.commons.utils.SysUtils;
import resources.om.FilePojo;
import various.common.light.files.CustomFileFilters;
import various.common.light.files.FileWorker;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.properties.PropertiesManager;

public class GeneralConfig {

	// general properties file containing mappings (this way you can change them without rebuild)
	public static final String resourcesMappingDefPath = "resourcesFilePathsMappings.properties";
	public static PropertiesManager resourcesFilePathsMappings = new PropertiesManager(resourcesMappingDefPath);
	public static Properties linkedMappings = resourcesFilePathsMappings.getDynamicFileBindingsDef();

	public static SimpleDateFormat GENERIC_FORMATTER = new SimpleDateFormat("yyyy/MM/dd - hh:mm:ss");

	// TEST ERROR CATCH
	public static boolean FLAG_THROW_ERROR_TEST_ON;
	public static int MAX_N_OF_ERR_DIALOG_OPENED;
	public static int AWT_EVENT_CHECKER_DECREASE_LIMIT_PERIOD;

	// MAIN PROPERTIES
	public static String AUTHOR = "A.M. Design and Development";
	public static String APP_VERSION = resourcesFilePathsMappings.getProperty("APP_VERSION", "Unknown Version");
	public static String APPLICATION_SHORT_NAME = resourcesFilePathsMappings.getProperty("APPLICATION_SHORT_NAME", "ScreepT");
	public static String APPLICATION_NAME = APPLICATION_SHORT_NAME + " - V" + APP_VERSION;
	public static String BUILD_INCREMENTAL_NUMBER = resourcesFilePathsMappings.getProperty("BUILD_INCREMENTAL_NUMBER", "Unknown build number");
	public static String BUILD_COMMIT_HASH = resourcesFilePathsMappings.getProperty("BUILD_COMMIT_HASH", "Undefined commit hash");
	public static String CHANGE_LOG_FILE_PATH = resourcesFilePathsMappings.getProperty("CHANGE_LOG_FILE_PATH", "");
	public static File CHANGE_LOG_FILE = CHANGE_LOG_FILE_PATH.equals("") ? null : new File(CHANGE_LOG_FILE_PATH);

	public static String WEBSITE = resourcesFilePathsMappings.getProperty("WEBSITE", "www.am-design-development.com");
	public static String MAIL_INFO = resourcesFilePathsMappings.getProperty("MAIL_INFO", "info@am-design-development.com");
	public static String MAIL_BUGS = resourcesFilePathsMappings.getProperty("MAIL_BUGS", "bugs@am-design-development.com");
	public static String WEB_UPDATER_VERSION_PROP = resourcesFilePathsMappings.getProperty("WEB_UPDATER_VERSION_PROP", "version");
	public static String WEB_UPDATER_JAR_NAME = resourcesFilePathsMappings.getProperty("WEB_UPDATER_JAR_NAME", "updater.jar");
	public static String WEB_UPDATER_JAR_PROP = resourcesFilePathsMappings.getProperty("WEB_UPDATER_JAR_PROP", "updaterJarUrl");
	public static String WEB_UPDATES_PACK_NAME = resourcesFilePathsMappings.getProperty("WEB_UPDATES_PACK_NAME", "updates.zip");
	public static String WEB_UPDATES_PACK_PROP = resourcesFilePathsMappings.getProperty("WEB_UPDATES_PACK_PROP", "updatesZippedUrl");
	public static String WEB_UPDATES_MANIFEST_URL = resourcesFilePathsMappings.getProperty("WEB_UPDATES_MANIFEST_URL", "www.am-design-development.com/ScreepT/Updates/manifest.properties");
	public static String WEB_UPDATES_LICENSE_URL = resourcesFilePathsMappings.getProperty("WEB_UPDATES_LICENSE_URL", "www.am-design-development.com/ScreepT/Updates/License_ScreepT.html");
	public static String WEB_PLUGINS_MANIFEST_URL = resourcesFilePathsMappings.getProperty("WEB_PLUGINS_MANIFEST_URL", "www.am-design-development.com/ScreepT/Plugins/manifest.properties");
	public static String WEB_PLUGINS_BASE_URL = resourcesFilePathsMappings.getProperty("WEB_PLUGINS_BASE_URL", "www.am-design-development.com/ScreepT/Plugins/");
	public static String BUILD_TS_DATE = resourcesFilePathsMappings.getProperty("BUILD_TIMESTAMP")
			.replaceAll("T", " at ")
			.replaceAll("Z", "")
			.replaceAll("-", "/");

	// IMPORTANT FOLDERS
	public static String CURRENT_EXECUTION_DIR = System.getProperty("user.dir")+"/";
	public static String USER_PERSONAL_DIR = System.getProperty("user.home")+"/";
	public static String USER_DESKTOP = USER_PERSONAL_DIR+"Desktop/";
	public static File USER_DESKTOP_FILE = new File(USER_DESKTOP);

	// The first level folder where Application/ directory is contained
	public static File MAIN_APP_PARENT_DIR = new File(CURRENT_EXECUTION_DIR).getParentFile();
	public static String MAIN_APP_OS_DEPENDENT_LAUNCHER_NAME = SysUtils.getOsInfoLazy().isWindows()
			? resourcesFilePathsMappings.getProperty("MAIN_LAUNCHER_WINDOWS", "ScreepT.exe")
					: resourcesFilePathsMappings.getProperty("MAIN_LAUNCHER_UNIX", "ScreepT.sh");
			public static File MAIN_APP_OS_DEPENDENT_LAUNCHER = new File(MAIN_APP_PARENT_DIR.getAbsolutePath() + "/" + MAIN_APP_OS_DEPENDENT_LAUNCHER_NAME);

			// General Folders
			public static String MAIN_APP_ROOT_DIR = resourcesFilePathsMappings.getProperty("MAIN_APP_ROOT_DIR", "Application/");

			public static String USER_PERSONAL_DIR_SCREEPT_ROOT_FOLDER = USER_PERSONAL_DIR + "ScreepT_temp_files/";
			public static String USER_PERSONAL_DIR_SCREEPT_FOLDER_VERSION = USER_PERSONAL_DIR_SCREEPT_ROOT_FOLDER + "/" + APP_VERSION + "/";
			public static File F_USER_PERSONAL_DIR_SCREEPT_FOLDER_VERSION = new File(USER_PERSONAL_DIR_SCREEPT_FOLDER_VERSION);
			public static String USER_PERSONAL_DIR_SCREEPT_FILE_COMMON_DATA_FOLDER = USER_PERSONAL_DIR_SCREEPT_ROOT_FOLDER + "COMMON_DATA/";
			public static String TEMP_DECRIPTED_ROOT_PATH = USER_DESKTOP + "ScreepT_decrypted_tempFiles";
			public static File TEMP_DECRIPTED_ROOT = new File(TEMP_DECRIPTED_ROOT_PATH);

			public static String RESOURCES_DIR = resourcesFilePathsMappings.getProperty("RESOURCES_DIR", "Resources_ScreepT/");
			public static String PLUGINS_DIR = resourcesFilePathsMappings.getProperty("PLUGINS_DIR");
			public static String EXTERNAL_PLUGINS = resourcesFilePathsMappings.getProperty("EXTERNAL_PLUGINS", "EXTERNAL_PLUGINS/");
			public static String UPDATES_DIR = resourcesFilePathsMappings.getProperty("UPDATES_DIR");

			public static String CREEPTER_FOLDER = resourcesFilePathsMappings.getProperty("CREEPTER_FOLDER", "Resources_ScreepT/CreepTer/");
			public static String CREEPTER_INSTRUCTIONS_FILE = CREEPTER_FOLDER + "Instructions.txt";
			public static String DEFAULT_IMAGE_FOLDER = resourcesFilePathsMappings.getProperty("DEFAULT_IMAGE_FOLDER");
			public static String SOUNDS_FOLDER = resourcesFilePathsMappings.getProperty("SOUNDS_FOLDER");
			public static String WORDLIST_FOLDER = resourcesFilePathsMappings.getProperty("WORDLIST_FOLDER");
			public static String DEFAULT_INI_FOLDER = resourcesFilePathsMappings.getProperty("DEFAULT_INI_FOLDER");
			public static String THEMES_FOLDER_DEFAULT = resourcesFilePathsMappings.getProperty("THEMES_FOLDER_DEFAULT");
			public static String THEMES_FOLDER_CUSTOM = resourcesFilePathsMappings.getProperty("THEMES_FOLDER_CUSTOM");
			public static String PANEL_FILES_FOLDER = resourcesFilePathsMappings.getProperty("PANEL_TEMP_FOLDER");
			public static String TEMPLATES_FOLDER = resourcesFilePathsMappings.getProperty("TEMPLATES_FOLDER");
			public static String CLIBPOARDS_PANEL_SRC_FOLDER = resourcesFilePathsMappings.getProperty("CLIBPOARDS_PANEL_SRC_FOLDER");
			public static String JS_LIB_FOLDER = resourcesFilePathsMappings.getProperty("JS_LIB_FOLDER") != null? resourcesFilePathsMappings.getProperty("JS_LIB_FOLDER") : "/Resources_ScreepT/lib/js/";
			public static String SPELLCHECKER_DEFAULT_DICT = resourcesFilePathsMappings.getProperty("SPELLCHECKER_DEFAULT_DICT") != null? resourcesFilePathsMappings.getProperty("SPELLCHECKER_DEFAULT_DICT") : "english_dic.zip";
			public static String SPELLCHECKER_DICT_FOLDER = resourcesFilePathsMappings.getProperty("SPELLCHECKER_DICT_FOLDER") != null? resourcesFilePathsMappings.getProperty("SPELLCHECKER_DICT_FOLDER") : "Resources_ScreepT/SpellChecker/";
			public static File SPELLCHECKER_DICT_FOLER_FILE = new File(SPELLCHECKER_DICT_FOLDER);
			public static Color SPELL_CHECKER_UNDERLINE_COL = resourcesFilePathsMappings.getColorVarFromProps(resourcesFilePathsMappings.getProperty("SPELL_CHECKER_UNDERLINE_COL"), Color.orange);
			// Temp Folders and Files
			public static String TEMP_DIRECTORY = resourcesFilePathsMappings.getProperty("TEMP_DIR", RESOURCES_DIR + "temp/");
			public static File TEMP_DIRECTORY_FILE = new File(TEMP_DIRECTORY);
			static{
				FileWorker.mkDirs(
						EXTERNAL_PLUGINS,
						UPDATES_DIR,
						EXTERNAL_PLUGINS,
						USER_PERSONAL_DIR_SCREEPT_FOLDER_VERSION,
						USER_PERSONAL_DIR_SCREEPT_FILE_COMMON_DATA_FOLDER,
						TEMP_DIRECTORY);
			}

			// LICENSE MAPPING
			public static final String licensePropertiesPath = RESOURCES_DIR + "license.properties";
			public static PropertiesManager licenseProperties = new PropertiesManager(licensePropertiesPath);
			public static boolean LICENSE_ACCEPTED = licenseProperties.getBooleanVarFromProps("LICENSE_ACCEPTED", false);
			public static File LICENSE_FILE = new File(MAIN_APP_PARENT_DIR.getAbsolutePath() + File.separator + "License_ScreepT.html");

			/**
			 * This file is created at SW start, and deleted on close to signal other external programs
			 */
			public static String SCREEPT_ACTIVE_SIGNAL_PATH = USER_PERSONAL_DIR_SCREEPT_FOLDER_VERSION + "mainApplicationSignalFile.ScreepT";
			public static File SCREEPT_ACTIVE_SIGNAL_FILE = new File(SCREEPT_ACTIVE_SIGNAL_PATH);
			public static String SIGNAL_FILE_TO_LAUNCH_PROP_PREFIX = "FILE_TO_LAUNCH_";
			public static int SIGNAL_LISTENER_PERIOD = resourcesFilePathsMappings.getIntVarFromProps("SIGNAL_LISTENER_PERIOD", 300);

			// Ini files configuration
			public static String DEFAULT_INI;
			public static String BACKUP_INI;

			// Readme File
			public static String SCREEPT_TUTORIAL_HTML_PATH;
			public static String SCREEPT_TUTORIAL_HTML_URL;

			// Logs configuration
			public static String LOG_PATH = resourcesFilePathsMappings.getProperty("LOG_PATH");
			public static String LOG_PROPERTIES_PATH = resourcesFilePathsMappings.getProperty("LOG_PROPERTIES_PATH");
			public static String LOG_LAUNCHER_PATH = resourcesFilePathsMappings.getProperty("LOG_LAUNCHER_PATH");
			public static String LAUNCHER_CONTAINER_PATH = resourcesFilePathsMappings.getProperty("LAUNCHER_CONTAINER_PATH");
			public static String DEFAULT_LAUNCHER_INI_PATH = resourcesFilePathsMappings.getProperty("DEFAULT_LAUNCHER_INI_PATH");
			public static String LAUNCHER_LOG_PROPS_PATH = resourcesFilePathsMappings.getProperty("LAUNCHER_LOG_PROPS_PATH");

			// JVM and LAUNCHER
			public static String MAX_JVM_COMPLETE_COMPATIBILITY;
			public static String JVM_CUSTOM_ADDED_SETTINGS;
			public static ArrayList<String> BATCH_LAUNCHER_ADDED_INSTRUCTIONS;

			/**
			 * Used in launcher to choose right type of launch command for system
			 */
			public static boolean FLAG_LAUNCH_JAR_COMMAND;
			public static String MAIN_GENERAL_CLASSPATH_WIN = "\"" + getExternalPluginsClasspath() + "./*;./Resources_ScreepT/lib/*\" ";
			public static String MAIN_GENERAL_CLASSPATH_UNIX = "\"" + getExternalPluginsClasspath() + "./*:./Resources_ScreepT/lib/*\" ";
			public static String MAIN_GENERAL_STARTER_PACKAGE;

			// LAUNCHER DEFAULTS
			public static int DEFAULT_JVM_XMX;
			public static int DEFAULT_JVM_XMS;
			public static int DEFAULT_JVM_XSS;
			public static float DEFAULT_JVM_GUI;
			public static String DEFAULT_JVM_VERS_MAX;
			public static String DEFAULT_JVM_VERS_MIN;
			public static String DEFAULT_LOG_LEVEL;
			public static boolean DEFAULT_JVM_JAR_MODE;
			public static boolean DEFAULT_JVM_SHELL_ON;

			// UPDATES
			public static int UPDATES_AUTOCHECK_DELAY;

			// NETWORK OPTIONS
			public static int NETWORK_UTILS_TIMEOUT;
			public static String testAddress;
			public static String FTP_HOST = resourcesFilePathsMappings.getProperty("FTP_HOST", "speedtest.tele2.net");
			public static String FTP_PSW = resourcesFilePathsMappings.getProperty("FTP_PSW", "anonymous");
			public static String FTP_USER = resourcesFilePathsMappings.getProperty("FTP_USER", "anonymous");
			public static String FTP_UPLOAD_FOLDER = resourcesFilePathsMappings.getProperty("FTP_UPLOAD_FOLDER", "upload");

			public static String DOWN_SHORT_FILE = resourcesFilePathsMappings.getProperty("DOWN_SHORT_FILE", "1MB.zip");
			public static int DOWN_SHORT_FILE_SIZE_MB = resourcesFilePathsMappings.getIntVarFromProps("DOWN_SHORT_FILE_SIZE_MB", 1);
			public static String DOWN_MED_FILE = resourcesFilePathsMappings.getProperty("DOWN_MED_FILE", "5MB.zip");
			public static int DOWN_MED_FILE_SIZE_MB = resourcesFilePathsMappings.getIntVarFromProps("DOWN_MED_FILE_SIZE_MB", 5);
			public static String DOWN_BIG_FILE = resourcesFilePathsMappings.getProperty("DOWN_BIG_FILE", "20MB.zip");
			public static int DOWN_BIG_FILE_SIZE_MB =  resourcesFilePathsMappings.getIntVarFromProps("DOWN_BIG_FILE_SIZE_MB", 20);

			public static String UP_SHORT_FILE = resourcesFilePathsMappings.getProperty("UP_SHORT_FILE", "250KB upload");
			public static int UP_SHORT_FILE_SIZE_KB =  resourcesFilePathsMappings.getIntVarFromProps("UP_SHORT_FILE_SIZE_KB", 250);
			public static String UP_MED_FILE = resourcesFilePathsMappings.getProperty("UP_MED_FILE", "1000KB upload");
			public static int UP_MED_FILE_SIZE_KB =  resourcesFilePathsMappings.getIntVarFromProps("UP_MED_FILE_SIZE_KB", 1000);
			public static String UP_BIG_FILE = resourcesFilePathsMappings.getProperty("UP_BIG_FILE", "4000KB upload");
			public static int UP_BIG_FILE_SIZE_KB =  resourcesFilePathsMappings.getIntVarFromProps("UP_BIG_FILE_SIZE_KB", 4000);

			/**	GUI PROPERTIES **/

			// MENU CUSTOM FONTS
			public static Font MENU_LASTFILES_DEFAULT_FONT;
			public static Font MENU_LASTFILES_FONT;


			// DEBUG GUI FLAG
			public static String debugPreRead;
			private static boolean FLAG_DEBUG_GRAPHICS;
			public static String DEBUG_GRAPHICS;
			public static boolean ASYNC_MODULES_LOAD;

			// TITLE REFRESHER
			public static int TITLE_REFRESH_TIME = resourcesFilePathsMappings.getIntVarFromProps("TITLE_REFRESH_TIME", 2000);

			// Tooltips
			public static int TOOLTIP_DELAY;
			public static int TOOLTIP_TIMEOUT;
			public static int TOOLTIP_RESHOW_DELAY;
			public static Font TOOLTIP_FONT;
			public static Font DEFAULT_POP_UP_MENUS_FONT;
			public static Font DIALOG_BTN_FONT;

			public static int LOADER_PANEL_HEIGHT = resourcesFilePathsMappings.getIntVarFromProps("LOADER_PANEL_HEIGHT", 280);

			// FILE CHOOSER DIALOG
			public static Dimension DEFAULT_FILE_CHOOSER_SIZE;
			public static Dimension FILE_CHOOSER_SIZE;

			// TEXT EDITOR DEFAULTS
			public static int UNDO_MAX_LIM;
			public static int ADVICE_FOOTER_TIMEOUT;
			public static Color ADVICE_FOOTER_OK_COLOR;
			public static Color ADVICE_FOOTER_KO_COLOR;
			public static Color ADVICE_FOOTER_INFO_COLOR;
			public static ArrayList<String> TXT_EDITOR_UPPERCASE_SEPARATORS = ConversionUtils.getStringListFromRawString(resourcesFilePathsMappings.getProperty("TXT_EDITOR_UPPERCASE_SEPARATORS"), ",");;
			public static int DEFAULT_TREE_ROW_HEIGHT;
			public static int MAX_LAST_OPENED_LIST_SIZE = resourcesFilePathsMappings.getIntVarFromProps("MAX_LAST_OPENED_LIST_SIZE", 12);
			public static int MAX_WORKSPACE_LIST_SIZE = resourcesFilePathsMappings.getIntVarFromProps("MAX_WORKSPACE_LIST_SIZE", 15);
			public static int MAX_REPLACE_DIAL_LIST_SIZE = resourcesFilePathsMappings.getIntVarFromProps("MAX_REPLACE_DIAL_LIST_SIZE", 50);
			public static int MAX_SEARCH_DIAL_LIST_SIZE = resourcesFilePathsMappings.getIntVarFromProps("MAX_SEARCH_DIAL_LIST_SIZE", 50);
			public static int MAX_HISTORY_SIZE = resourcesFilePathsMappings.getIntVarFromProps("MAX_HISTORY_SIZE", 40);
			public static int HISTORY_MOUSECLICK_LINES_DIFF_TO_ADD = resourcesFilePathsMappings.getIntVarFromProps("HISTORY_MOUSECLICK_LINES_DIFF_TO_ADD", 20);
			public static float SHELL_PANEL_FONT_SIZE = resourcesFilePathsMappings.getFloatVarFromProps("SHELL_PANEL_FONT_SIZE", 19f);
			public static boolean RECTANGULAR_TXT_SELECTION_ALLOWED;
			public static boolean PRELOAD_TXT_EDITOR_FLAG;
			public static Font TXT_EDITOR_MINIMAP_FONT_DEFAULT;
			public static Font TXT_EDITOR_MINIMAP_FONT;

			public static Font TXT_EDITOR_CLIPBOARD_FONT_DEFAULT;
			public static Font TXT_EDITOR_CLIPBOARD_FONT;

			public static boolean TEXT_EDITOR_BACKUP_BEFORE_SWITCH_ENABLED;

			public static String CLIPBOARD_PANEL_DEFAULT = "Clipboard.txt";
			/**
			 * Lazy initialized map with clipboards names related to clipboard files
			 */
			public static TreeMap<String, File> CLIPBOARD_PANEL_MAP;
			/**
			 * Contains String themes name, and source theme file, lazy initialized
			 */
			public static TreeMap<String, File> XML_THEMES_MAP;
			/**
			 * Contains String themes name, and theme content lazy read
			 */
			public static TreeMap<String, String> XML_THEMES_CONTENT_MAP;

			public static Dimension MINIMUM_BUTTON_SIZE;
			public static Font BUTTON_DEFAULT_FONT;
			public static Font BUTTON_TAB_HEAD_FONT;
			public static int TAB_HEAD_HEIGHT;
			public static Font DEFAULT_FONT;
			public static Font DEFAULT_DIALOGS_FONT;
			public static Color BUTTON_BCKG_COLOR_TXT_EDITOR;
			public static Color BUTTON_BCKG_COLOR_TXT_EDITOR_DARKER;
			public static int MAX_FILE_SIZE_TXT_EDITOR_KB;
			public static int MAX_TEXT_EDITOR_SAFE_LINES;
			public static int TEXT_EDITOR_FOOTER_REFRESH_PERIOD_MS;
			public static int NORMAL_WHEEL_SPEED;
			public static int FAST_WHEEL_SPEED;
			public static int ULTRA_FAST_WHEEL_SPEED;
			public static int KEY_SCROLL_SPEED;
			public static int DEF_CARET_BLINK_RATE;

			public static int FOOTER_MAX_DIR_LABEL_LENGTH;
			public static int DEFAULT_FILE_TREE_ANCESTOR_LEVEL;

			// PROXY OPTIONS
			public static String DEF_PROXY_CONFIG_STRING = resourcesFilePathsMappings.getProperty("DEF_PROXY_CONFIG_STRING");

			public static GeneralConfig lazyInitializedGlobalReference;

			//// CONSTRUCTOR ////
			public GeneralConfig() {
				File propertiesSrc = new File(resourcesMappingDefPath);
				if(!propertiesSrc.exists()) {
					try {
						propertiesSrc.createNewFile();
					} catch (IOException e) {
					}
				}
				Thread configurator = new Thread(new Runnable() {
					@Override
					public void run() {
						initStaticConfig();
					}
				});
				configurator.start();
				try {
					configurator.join();
				}catch(InterruptedException e) {
				}
			}

			///////////////////////////////////////////////// METHODS ///////////////////////////////////////////////

			/**
			 * Properties reloader
			 */
			public static void reloadProperties () {
				resourcesFilePathsMappings = new PropertiesManager(resourcesMappingDefPath);
				linkedMappings = resourcesFilePathsMappings.getDynamicFileBindingsDef();
				reloadGeneralConfig();
			}

			public static String[] getPubIpProvList() {
				String list = resourcesFilePathsMappings.getProperty("PUB_IP_PROV_LIST");
				String[] providers = list.split(",");

				return providers;
			}

			/**
			 * It's preferable to use lazy initialized CLIPBOARD_PANEL_MAP when possible
			 * @return
			 */
			public static File[] getClipboardsFiles(){
				File[] clipboards = new File(CLIBPOARDS_PANEL_SRC_FOLDER).listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {

						// Accept only files
						if(dir.isDirectory()) {
							return false;
						}else {
							return true;
						}
					}
				});

				return clipboards;
			}

			public static String getExternalPluginsClasspath() {
				File[] subDirs = new File(EXTERNAL_PLUGINS).listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.DIR_ONLY));
				String classPath = "";
				if(subDirs != null) {
					for(File dir : subDirs) {
						if(dir.isDirectory()) {
							classPath += "./" + EXTERNAL_PLUGINS + dir.getName() + "/*" + (new OSinfo().isWindows() ? ";" : ":");
						}
					}
				}

				return classPath;
			}

			public static TreeMap<String, File> getRefreshedClipBoardsMap(){
				return FilePojo.mapFilesFromDir(new File(CLIBPOARDS_PANEL_SRC_FOLDER));
			}

			/**
			 * It's preferable to use lazy initialized XML_THEME_MAP when possible
			 * @return
			 */
			public static TreeMap<String, File> getRefreshedThemesMap(){
				TreeMap<String, File> customThemes = FilePojo.mapFilesFromDir(new File(THEMES_FOLDER_CUSTOM));
				TreeMap<String, File> defaultThemes = FilePojo.mapFilesFromDir(new File(THEMES_FOLDER_DEFAULT));
				for(String currKey : customThemes.keySet()) {
					String currKeyCopy = new String(currKey);
					int i = 1;
					while(defaultThemes.containsKey(currKeyCopy)) {
						currKeyCopy = insertNumberBeforeExtension(currKey, i);
						i++;
					}
					defaultThemes.put(currKey, customThemes.get(currKey));
				}

				return defaultThemes;
			}

			/**
			 * Return a string with String keys(style scheme name) associated to String scheme file content (String file read)
			 *
			 * It's preferable to use lazy initialized XML_THEME_CONTENT_MAP when possible
			 */
			public static TreeMap<String, String> getRefreshedThemesContentMap(){
				TreeMap<String, String> customThemeContents = new TreeMap<String, String>();
				TreeMap<String, File> fileMap = new TreeMap<String, File>();
				File current;
				String currentRead = ""
						;
				if(XML_THEMES_MAP != null && XML_THEMES_MAP.size() > 0) {
					fileMap = XML_THEMES_MAP;
				}else {
					fileMap = getRefreshedThemesMap();
				}

				for(String themeId : fileMap.keySet()) {
					current = fileMap.get(themeId);
					if(current != null) {
						try {
							currentRead = FileWorker.readFileAsString(current);
							customThemeContents.put(themeId, currentRead);
						} catch (Exception e) {
						}
					}
				}

				return customThemeContents;
			}

			/**
			 *  * This is a fork from FileStuff - Copied to avoid dependency cycles
			 * @param fileName
			 * @param n
			 * @return
			 */
			private static String insertNumberBeforeExtension(String fileName, int n) {

				String extension = "";
				String name = "";

				int idxOfDot = fileName.lastIndexOf('.');   //Get the last index of . to separate extension
				if(idxOfDot>=0) {
					extension = fileName.substring(idxOfDot + 1);
					name = fileName.substring(0, idxOfDot);
				}else {
					name = fileName;
				}

				// if already with parentesis number
				if(name.substring(name.length()-1, name.length()).equals(")")) {
					idxOfDot = fileName.lastIndexOf('(');
					name = fileName.substring(0, idxOfDot);
				}
				fileName = name+"("+n+")."+extension;

				return fileName;
			}

			public Boolean initStaticConfig() {

				try {
					DEF_PROXY_CONFIG_STRING = resourcesFilePathsMappings.getProperty("DEF_PROXY_CONFIG_STRING");

					// TEST ERROR CATCH
					FLAG_THROW_ERROR_TEST_ON = Boolean.valueOf(resourcesFilePathsMappings.getProperty("FLAG_THROW_ERROR_TEST_ON"));
					MAX_N_OF_ERR_DIALOG_OPENED = resourcesFilePathsMappings.getIntVarFromProps("MAX_N_OF_ERR_DIALOG_OPENED", 2);
					AWT_EVENT_CHECKER_DECREASE_LIMIT_PERIOD = resourcesFilePathsMappings.getIntVarFromProps("AWT_EVENT_CHECKER_DECREASE_LIMIT_PERIOD", 5000);

					// Ini files configuration
					DEFAULT_INI= resourcesFilePathsMappings.getProperty("DEFAULT_INI");
					BACKUP_INI= resourcesFilePathsMappings.getProperty("BACKUP_INI");

					// Readme File
					SCREEPT_TUTORIAL_HTML_PATH = CURRENT_EXECUTION_DIR + "../Readme/ReadMe.html";
					SCREEPT_TUTORIAL_HTML_URL = "File:./" + SCREEPT_TUTORIAL_HTML_PATH;

					// JVM and LAUNCHER
					MAX_JVM_COMPLETE_COMPATIBILITY = resourcesFilePathsMappings.getProperty("MAX_JVM_COMPLETE_COMPATIBILITY");
					JVM_CUSTOM_ADDED_SETTINGS = resourcesFilePathsMappings.getProperty("JVM_CUSTOM_ADDED_SETTINGS");

					/**
					 * Batch windows launcher added instructions
					 */
					BATCH_LAUNCHER_ADDED_INSTRUCTIONS = resourcesFilePathsMappings.getStringList("BATCH_LAUNCHER_ADDED_INSTRUCTIONS", ";");

					/**
					 * Used in launcher to choose right type of launch command for system
					 */
					FLAG_LAUNCH_JAR_COMMAND = Boolean.valueOf(resourcesFilePathsMappings.getProperty("FLAG_LAUNCH_JAR_COMMAND"));
					MAIN_GENERAL_STARTER_PACKAGE = "maingUI.MainGuiGeneralAppStarter";

					// LAUNCHER DEFAULTS
					DEFAULT_JVM_XMX  = resourcesFilePathsMappings.getIntVarFromProps("DEFAULT_JVM_XMX", 768);
					DEFAULT_JVM_XMS  = resourcesFilePathsMappings.getIntVarFromProps("DEFAULT_JVM_XMS", 348);
					DEFAULT_JVM_XSS  = resourcesFilePathsMappings.getIntVarFromProps("DEFAULT_JVM_XSS", 100);
					DEFAULT_JVM_GUI  = resourcesFilePathsMappings.getFloatVarFromProps("DEFAULT_JVM_GUI", 1.0f);
					DEFAULT_JVM_VERS_MAX = resourcesFilePathsMappings.getProperty("DEFAULT_JVM_VERS_MAX");
					DEFAULT_JVM_VERS_MIN = resourcesFilePathsMappings.getProperty("DEFAULT_JVM_VERS_MIN");
					DEFAULT_LOG_LEVEL = resourcesFilePathsMappings.getProperty("DEFAULT_LOG_LEVEL");
					DEFAULT_JVM_JAR_MODE = Boolean.valueOf(resourcesFilePathsMappings.getProperty("DEFAULT_JVM_JAR_MODE"));
					DEFAULT_JVM_SHELL_ON = Boolean.valueOf(resourcesFilePathsMappings.getProperty("DEFAULT_JVM_SHELL_ON"));

					// UPDATES
					UPDATES_AUTOCHECK_DELAY  = resourcesFilePathsMappings.getIntVarFromProps("UPDATES_AUTOCHECK_DELAY", 10000);

					// NETWORK OPTIONS
					NETWORK_UTILS_TIMEOUT  = resourcesFilePathsMappings.getIntVarFromProps("NETWORK_UTILS_TIMEOUT", 5000);
					testAddress = resourcesFilePathsMappings.getProperty("testAddress");

					/**	GUI PROPERTIES **/

					// MENU CUSTOM FONTS
					MENU_LASTFILES_DEFAULT_FONT =  new Font("Segoe UI", Font.PLAIN, 15);
					MENU_LASTFILES_FONT =  resourcesFilePathsMappings.getFontFromString("MENU_LASTFILES_FONT", MENU_LASTFILES_DEFAULT_FONT);


					// DEBUG GUI FLAG
					debugPreRead = resourcesFilePathsMappings.getProperty("FLAG_DEBUG_GRAPHICS");
					FLAG_DEBUG_GRAPHICS = (debugPreRead!= null && debugPreRead.equalsIgnoreCase("true"))? true : false;
					DEBUG_GRAPHICS = (FLAG_DEBUG_GRAPHICS)? "debug," : "";
					ASYNC_MODULES_LOAD = resourcesFilePathsMappings.getBooleanVarFromProps("ASYNC_MODULES_LOAD", true);

					// Tooltips
					TOOLTIP_DELAY = resourcesFilePathsMappings.getIntVarFromProps("TOOLTIP_DELAY", 400);
					TOOLTIP_TIMEOUT = resourcesFilePathsMappings.getIntVarFromProps("TOOLTIP_TIMEOUT", 30000);
					TOOLTIP_RESHOW_DELAY = resourcesFilePathsMappings.getIntVarFromProps("TOOLTIP_RESHOW_DELAY", 2000);
					TOOLTIP_FONT = resourcesFilePathsMappings.getFontFromString("TOOLTIP_FONT", new Font("Segoe UI", Font.PLAIN, 18));
					DEFAULT_POP_UP_MENUS_FONT = resourcesFilePathsMappings.getFontFromString("DEFAULT_POP_UP_MENUS_FONT", new Font("Segoe UI", Font.PLAIN, 16));
					DIALOG_BTN_FONT = resourcesFilePathsMappings.getFontFromString("DIALOG_BTN_FONT", new Font("Segoe UI", Font.BOLD, 16));

					// FILE CHOOSER DIALOG
					DEFAULT_FILE_CHOOSER_SIZE = new Dimension(800, 600);
					FILE_CHOOSER_SIZE = resourcesFilePathsMappings.getDimensionFromString("-", "FILE_CHOOSER_SIZE_STR", DEFAULT_FILE_CHOOSER_SIZE);

					// TEXT EDITOR DEFAULTS
					UNDO_MAX_LIM = resourcesFilePathsMappings.getIntVarFromProps("UNDO_MAX_LIM", 1000);
					ADVICE_FOOTER_TIMEOUT = resourcesFilePathsMappings.getIntVarFromProps("ADVICE_FOOTER_TIMEOUT", 7000);
					ADVICE_FOOTER_OK_COLOR = resourcesFilePathsMappings.getColorVarFromProps("ADVICE_FOOTER_OK_COLOR", Color.GREEN);
					ADVICE_FOOTER_KO_COLOR = resourcesFilePathsMappings.getColorVarFromProps("ADVICE_FOOTER_KO_COLOR", Color.RED);
					ADVICE_FOOTER_INFO_COLOR = resourcesFilePathsMappings.getColorVarFromProps("ADVICE_FOOTER_INFO_COLOR", Color.BLUE);
					DEFAULT_TREE_ROW_HEIGHT = resourcesFilePathsMappings.getIntVarFromProps("DEFAULT_TREE_ROW_HEIGHT", 14);
					RECTANGULAR_TXT_SELECTION_ALLOWED = Boolean.valueOf(resourcesFilePathsMappings.getProperty("RECTANGULAR_TXT_SELECTION_ALLOWED"));
					PRELOAD_TXT_EDITOR_FLAG = Boolean.valueOf(resourcesFilePathsMappings.getProperty("PRELOAD_TXT_EDITOR_FLAG"));
					TXT_EDITOR_MINIMAP_FONT_DEFAULT =  new Font("Segoe UI", Font.PLAIN, 5);
					TXT_EDITOR_MINIMAP_FONT = resourcesFilePathsMappings.getFontFromString("TXT_EDITOR_MINIMAP_FONT", TXT_EDITOR_MINIMAP_FONT_DEFAULT);

					TXT_EDITOR_CLIPBOARD_FONT_DEFAULT =  new Font("Segoe UI", Font.PLAIN, 12);
					TXT_EDITOR_CLIPBOARD_FONT =  resourcesFilePathsMappings.getFontFromString("TXT_EDITOR_CLIPBOARD_FONT", TXT_EDITOR_CLIPBOARD_FONT_DEFAULT);

					TEXT_EDITOR_BACKUP_BEFORE_SWITCH_ENABLED = Boolean.valueOf(resourcesFilePathsMappings.getProperty("TEXT_EDITOR_BACKUP_BEFORE_SWITCH_ENABLED"));

					/**
					 * Lazy initialized map with clipboards names related to clipboard files
					 */
					CLIPBOARD_PANEL_MAP = getRefreshedClipBoardsMap();
					/**
					 * Contains String themes name, and source theme file, lazy initialized
					 */
					XML_THEMES_MAP = getRefreshedThemesMap();

					/**
					 * Contains String themes name, and theme content lazy read
					 */
					XML_THEMES_CONTENT_MAP = getRefreshedThemesContentMap();

					MINIMUM_BUTTON_SIZE = resourcesFilePathsMappings.getDimensionFromString("-", "MINIMUM_BUTTON_SIZE", new Dimension(18,24));
					BUTTON_DEFAULT_FONT =  resourcesFilePathsMappings.getFontFromString("BUTTON_DEFAULT_FONT", new Font("Segoe UI", Font.BOLD, 12));
					BUTTON_TAB_HEAD_FONT =  resourcesFilePathsMappings.getFontFromString("BUTTON_TAB_HEAD_FONT", new Font("Segoe UI", Font.PLAIN, 14));
					TAB_HEAD_HEIGHT =  resourcesFilePathsMappings.getIntVarFromProps("TAB_HEAD_HEIGHT", 24);
					DEFAULT_FONT = resourcesFilePathsMappings.getFontFromString("DEFAULT_FONT", new Font("Segoe UI", Font.PLAIN, 16));
					DEFAULT_DIALOGS_FONT = resourcesFilePathsMappings.getFontFromString("DEFAULT_DIALOGS_FONT", new Font("Segoe UI", Font.PLAIN, 16));
					BUTTON_BCKG_COLOR_TXT_EDITOR = resourcesFilePathsMappings.getColorVarFromProps("BUTTON_BCKG_COLOR_TXT_EDITOR", new Color(120, 125, 140));
					BUTTON_BCKG_COLOR_TXT_EDITOR_DARKER = resourcesFilePathsMappings.getColorVarFromProps("BUTTON_BCKG_COLOR_TXT_EDITOR_DARKER", new Color(71, 76, 84));
					MAX_FILE_SIZE_TXT_EDITOR_KB = resourcesFilePathsMappings.getIntVarFromProps("MAX_FILE_SIZE_TXT_EDITOR_KB", 1024*20); // 20 MB default
					MAX_TEXT_EDITOR_SAFE_LINES = resourcesFilePathsMappings.getIntVarFromProps("MAX_TEXT_EDITOR_SAFE_LINES", 1000000); // 1 Million lines default
					TEXT_EDITOR_FOOTER_REFRESH_PERIOD_MS = resourcesFilePathsMappings.getIntVarFromProps("TEXT_EDITOR_FOOTER_REFRESH_PERIOD_MS", 300);
					NORMAL_WHEEL_SPEED = resourcesFilePathsMappings.getIntVarFromProps("NORMAL_WHEEL_SPEED", 14);
					FAST_WHEEL_SPEED = resourcesFilePathsMappings.getIntVarFromProps("FAST_WHEEL_SPEED", 150);
					KEY_SCROLL_SPEED = resourcesFilePathsMappings.getIntVarFromProps("KEY_SCROLL_SPEED", 3);
					ULTRA_FAST_WHEEL_SPEED = resourcesFilePathsMappings.getIntVarFromProps("ULTRA_FAST_WHEEL_SPEED", 300);
					DEF_CARET_BLINK_RATE = resourcesFilePathsMappings.getIntVarFromProps("DEF_CARET_BLINK_RATE", 400);

					FOOTER_MAX_DIR_LABEL_LENGTH = resourcesFilePathsMappings.getIntVarFromProps("FOOTER_MAX_DIR_LABEL_LENGTH", 30);
					DEFAULT_FILE_TREE_ANCESTOR_LEVEL = resourcesFilePathsMappings.getIntVarFromProps("DEFAULT_FILE_TREE_ANCESTOR_LEVEL", 2);

					ImagesPathConfigurator.init();
					GeneralResourcesPaths.init();
					SoundsConfigurator.init();

					return new Boolean(true);
				} catch (Exception e) {
					return null;
				}
			}

			public static void refreshClipboardMap() {
				synchronized (CLIPBOARD_PANEL_MAP) {
					CLIPBOARD_PANEL_MAP = getRefreshedClipBoardsMap();
				}
			}
			public static void refreshThemeseMap() {
				synchronized (XML_THEMES_MAP) {
					XML_THEMES_MAP = getRefreshedThemesMap();
				}
			}

			public static void refreshThemeseContentMap() {
				synchronized (XML_THEMES_CONTENT_MAP) {
					XML_THEMES_CONTENT_MAP = getRefreshedThemesContentMap();
				}
			}

			/**
			 * @return true if GeneralConfig reference was not created this time, false if it was already initialized
			 */
			public static boolean initLazyGeneralConfig() {
				if(lazyInitializedGlobalReference == null){
					lazyInitializedGlobalReference = new GeneralConfig();
					return true;
				}else {
					return false;
				}
			}

			public static void reloadGeneralConfig() {
				lazyInitializedGlobalReference.initStaticConfig();
				ImagesPathConfigurator.init();
				GeneralResourcesPaths.init();
				SoundsConfigurator.init();
			}
}
