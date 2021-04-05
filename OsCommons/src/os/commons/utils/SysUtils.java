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
package os.commons.utils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import os.commons.om.JVMInfo;
import os.commons.om.OSinfo;
import os.commons.utils.linux.LinuxFindJvmVersions;
import various.common.light.files.search.FileNameSearchAdvanced;
import various.common.light.gui.GuiUtils;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.string.StringWorker;


public class SysUtils {

	// SysUtils logger
	static SafeLogger logger = new SafeLogger(SysUtils.class);

	// STATIC METHODS
	public static final OSinfo OS_INFO_LAZY = new OSinfo();

	public static final String WIN_AUTOSTART_PATH = "C:/ProgramData/Microsoft/Windows/Start Menu/Programs/StartUp";
	public static final String UNIX_AUTOSTART_PATH = "/etc/init.d";

	public static final String winPath32 = "C:/Program Files (x86)/";
	public static final String winPath64 = "C:/Program Files/";

	public static final String winJavaExecutable = "java.exe";

	public static OSinfo retrieveOS() {
		return new OSinfo();
	}

	public static JVMInfo getJVMinfo() {
		return new JVMInfo();
	}


///////////////////////////////// GENERIC STUFF ////////////////////////////////////////////////

	/**
	 * @param target
	 * @param shortcutDestination
	 * @return element 0: success response from OS :: element 1: error message (empty string if success)
	 * @throws IOException
	 */
	public static String[] addToAutostart(File executable) throws IOException {
		return addToAutostart(executable, "");
	}

	/**
	 * @param target
	 * @param shortcutDestination
	 * @return element 0: success response from OS :: element 1: error message (empty string if success)
	 * @throws IOException
	 */
	public static String[] addToAutostart(File executable, String version) throws IOException {
		OSinfo OS = getOsInfoLazy();

		String linkName = getVersionedFileName(executable, version, ".lnk");

		if(OS.isWindows()) {
			return createShortcut(executable, new File(WIN_AUTOSTART_PATH + "/" + linkName));
		} else {
			return createShortcut(executable, new File(UNIX_AUTOSTART_PATH + "/" + linkName));
		}
	}

	public static boolean removeFromAutostart(File executable) throws IOException {
		return removeFromAutostart(executable, "");
	}

	public static boolean removeFromAutostart(File executable, String version) throws IOException {
		OSinfo OS = getOsInfoLazy();
		File toRemove = null;
		String linkName = getVersionedFileName(executable, version, ".lnk");

		if(OS.isWindows()) {
			toRemove = new File(WIN_AUTOSTART_PATH + "/" + linkName);
		} else {
			toRemove = new File(UNIX_AUTOSTART_PATH + "/" + linkName);
		}

		return FileUtils.deleteQuietly(toRemove);
	}

	public static boolean isPresentInAutostart(File executable) {
		return isPresentInAutostart(executable, "");
	}

	public static boolean isPresentInAutostart(File executable, String version) {
		OSinfo OS = getOsInfoLazy();
		File toRemove = null;
		String linkName = getVersionedFileName(executable, version, ".lnk");

		if(OS.isWindows()) {
			toRemove = new File(WIN_AUTOSTART_PATH + "/" + linkName);
		} else {
			toRemove = new File(UNIX_AUTOSTART_PATH + "/" + linkName);
		}

		return toRemove.exists();
	}

	public static String getVersionedFileName(File executable, String version, String extension) {
		String versionStr = StringWorker.trimToEmpty(version);
		boolean emptyVersion = version.equals("");

		return FilenameUtils.removeExtension(executable.getName())
				.concat(emptyVersion ? "" : "_")
				.concat(versionStr)
				.concat(".lnk");
	}

	public static void viewAutostartFolder(boolean openInShell) {
		if (getOsInfoLazy().isWindows()) {
			if (openInShell) {
				cdIntoCommandLine(new File(WIN_AUTOSTART_PATH));
			} else {
				GuiUtils.openInFileSystem(new File(WIN_AUTOSTART_PATH));
			}
		} else {
			if (openInShell) {
				cdIntoCommandLine(new File(UNIX_AUTOSTART_PATH));
			} else {
				GuiUtils.openInFileSystem(new File(UNIX_AUTOSTART_PATH));
			}
		}
	}

	/**
	 * @param target
	 * @param shortcutDestination
	 * @return element 0: success response from OS :: element 1: error message (empty string if success)
	 * @throws IOException
	 */
	public static String[] createShortcut(File target, File shortcutDestination) throws IOException {

		OSinfo OS = getOsInfoLazy();
		logger.info("Launching commands on currentOS: " + OS.getOsName() + OS.getOsArch());
		logger.debug("Creating a shortcut file for " + target + " : " + shortcutDestination);

		Runtime runtime = Runtime.getRuntime();
		String[] processOutput = new String[] {"", ""};

		if(OS.isWindows()) {
			processOutput = execNlogProcessOutput(runtime.exec("cmd /c mklink \"" + shortcutDestination.getCanonicalPath() + "\" \"" + target.getCanonicalPath() + "\""));
		} else {
			processOutput = execNlogProcessOutput(runtime.exec("ln -s \"" + target.getCanonicalPath() + "\" \"" + shortcutDestination.getCanonicalPath() + "\""));
		}

		return processOutput;
	}

	/**
  	 * @param main class must be a full package and class name (Ex => [Java.Lang.String])
	 *
	 * other params must not be null
	 */
	private static String startJarCommand(String jvmArgs, String jarPath, boolean viewShell) {
		return getJavaCommand(viewShell, jarPath) + jvmArgs + " -jar " + jarPath;
	}

	/**
	 * SIGNATURE2 : START .JAR with ARGS FILES
	 * @param main class must be a full package and class name (Ex => [Java.Lang.String])
	 *
	 * filteredArgsFiles can be null
	 *
	 * params must not be null
	 */
	public static String startJarCommand(String jvmArgs, String jarPath, File[] filteredArgsFiles, boolean viewShell) {

		if(filteredArgsFiles == null || filteredArgsFiles.length == 0) {
			return startJarCommand(jvmArgs, jarPath, viewShell);
		}

		String filesParams = "";
		StringBuilder builder = new StringBuilder();

		for(File file : filteredArgsFiles) {
			builder.append(" \"").append(file.getAbsolutePath()).append("\"");
		}
		filesParams = builder.toString();

		builder = new StringBuilder();
		builder.append(getJavaCommand(viewShell, jarPath)).append(jvmArgs).append(" -jar ").append(jarPath).append(filesParams);
		String command = builder.toString();
		return command;
	}


	/**
  	 * @param main class must be a full package and class name (Ex => [Java.Lang.String])
	 *
	 * params must not be null
	 */
	private static String startClassCommand(String jvmArgs, String classPath, String mainClass, boolean viewShell) {
		return getJavaCommand(viewShell, mainClass) + jvmArgs + " -cp " + classPath + " " + mainClass;
	}

	/**
	 * SIGNATURE2 : START .CLASS with ARGS FILES
	 * @param main class must be a full package and class name (Ex => [Java.Lang.String])
	 *
	 * filteredArgsFiles can be null
	 *
	 * other params must not be null
	 */
	public static String startClassCommand(String jvmArgs, String classPath, String mainClass, File[] filteredArgsFiles, boolean viewShell) {

		if(filteredArgsFiles == null || filteredArgsFiles.length == 0) {
			return startClassCommand(jvmArgs, classPath, mainClass, viewShell);
		}

		String filesParams = "";
		StringBuilder builder = new StringBuilder();

		for(File file : filteredArgsFiles) {
			builder.append(" \"").append(file.getAbsolutePath()).append("\"");
		}
		filesParams = builder.toString();

		builder = new StringBuilder();
		builder.append(getJavaCommand(viewShell, mainClass)).append(jvmArgs)
			   .append(" -cp ").append(classPath)
			   .append(" ").append(mainClass)
			   .append(filesParams);
		String command = builder.toString();
		return command;
	}

	public static String getJavaCommand(boolean viewShell, String shellTitle) {
		String javaLaunchKeyWord = "java ";
		shellTitle = shellTitle == null ? "" : shellTitle;

		if(OS_INFO_LAZY.isWindows()) {
			javaLaunchKeyWord = viewShell ? "java " : "start \"" + shellTitle + "\" \"javaw\" ";
		} else {
			javaLaunchKeyWord = viewShell ? "java " : "javaw ";
		}

		return javaLaunchKeyWord;
	}

	public static File getCurrentJavaHome() {
		return new File(System.getProperty("java.home"));
	}

	/**
	 * According to given OS, search for installed JVM paths
	 * @param OS
	 * @return the list of JVMs home directory (EX. "jdk1.8.0_201")
	 */
	public static ArrayList<String> findJvmVersionsInstalled(OSinfo OS){
		ArrayList<String> foundJVMS = new ArrayList<String>();

		if(OS.isLinux()) {

			try {
				foundJVMS.addAll(LinuxFindJvmVersions.CommandLineJavaAlternativesPaths());
			} catch (IOException e) {
				logger.error("Cannot retrieve linux java versions", e);
			}
		}else if(OS.isMac()) {

			// TODO MacOS

		}else if(OS.isWindows()) {

			// Add oracle's java list
			ArrayList<String> foundJavaRootFolders = findWindowsProgramPath("java");
			for(String currFolder : foundJavaRootFolders) {
				ArrayList<String> foundVersions = getAllJavaWinVersionFromRoot(currFolder);
				for (String currVersionPath : foundVersions) {
					foundJVMS.add(currVersionPath);
				}
			}

			// Add open JDK java list
			List<String> foundOpenJdkRootFolders = findWindowsProgramPathByNamePart("openjdk");
			for(String currFolder : foundOpenJdkRootFolders) {
				ArrayList<String> foundVersions = getAllDirsWithJavaExeInside(currFolder, winJavaExecutable);
				for (String currVersionPath : foundVersions) {
					foundJVMS.add(currVersionPath);
				}
			}
		} // END WINDOWS

		// MULTI-PLATFORM : Add current if not yet present into list
		String canonicalCurrentPath;
		canonicalCurrentPath = getCurrentJavaHome().getAbsolutePath();
		logger.info("Current jvm is : " + canonicalCurrentPath);
		if(!foundJVMS.contains(canonicalCurrentPath) && !foundJVMS.contains(new File(canonicalCurrentPath).getParentFile().getAbsolutePath())) {
			foundJVMS.add(canonicalCurrentPath);
			logger.info(canonicalCurrentPath + " not found in previously retrieved list -> added to JVM list");
		}

		return foundJVMS;
	}

	public static String getCurrentJVMversion() {
		return System.getProperty("java.version");
	}

	public static String getJavaVersionFromBin(String javaBinDirPath) throws IOException {
		File parentOfBin = new File(javaBinDirPath).getParentFile();
		File releaseFile = new File(parentOfBin.getAbsolutePath(), "release");
		BufferedReader bufferedReader = new BufferedReader(new FileReader(releaseFile));
		String version = bufferedReader.readLine();
		bufferedReader.close();
		return version.substring(version.indexOf("\"") + 1, version.lastIndexOf("\""));
	}

	public static String getCurrentSystemEncoding() {
		return System.getProperty("file.encoding");
	}

	/**
	 * @return the max memory usable by Java software in MB
	 */
	public static int getMaxMemoryAdmitted() {

		if (is64arch()) {
			return 4092;
		} else {
			return 2048;
		}
	}

	public static boolean isCurrentOpenJdk() {
		return System.getProperty("java.home").toLowerCase().contains("openjdk");
	}

	/**
	 * return true if current architecture is 64 bits, false is 32 bits.
	 *
	 * @return
	 */
	public static boolean is64arch() {
		String arch = System.getProperty("os.arch");
		if (arch != null && arch.contains("64")) {
			return true;
		} else
			return false;
	}

	/**
	 * return true if current architecture is 32 bits, false is 64 bits.
	 *
	 * @return
	 */
	public static boolean is32arch() {
		String arch = System.getProperty("os.arch");
		if (arch != null && arch.contains("86")) {
			return true;
		} else
			return false;
	}

	public static int findIf32or64() {
		if (is64arch()) {
			return 64;
		} else {
			return 32;
		}
	}

	public static String getShellOsBased() {
		if(OS_INFO_LAZY.isWindows()) {
			return "cmd";
		} else {
			return "bash";
		}
	}

	public static boolean openUrlWithSpecificBrowser(String URL, Browser targetBrowser) throws IOException, InterruptedException {
		try {
			Runtime runtime = Runtime.getRuntime();
			String shell = OS_INFO_LAZY.isWindows() ? getShellOsBased() : "";
			String argument1 = OS_INFO_LAZY.isWindows()? " /c " : OS_INFO_LAZY.isMac()? " " : " ";
			String prePost = OS_INFO_LAZY.isWindows() ? "\"" : "";
			runtime.exec(shell + argument1 + targetBrowser.getOsLaunchSwName() + prePost + URL + prePost);
			return true;

		} catch (Exception e) {
			return false;
		}
	}

	public static void openUrlInBrowserOrdered(String URL) {
		boolean opened = false;
		for(Browser browser : Browser.defaultBrowserOrder) {
			try {
				opened = openUrlWithSpecificBrowser(URL, browser);
			} catch (Exception e) {
				logger.warn("Cannot open " + URL + " with browser: " + browser, e);
			}

			if(opened) {
				logger.info(URL + " opened with " + browser);
				return;
			}
		}

		openUrlInBrowser(URL);
		logger.info(URL + " opened with system default bind application.");
	}

	public static boolean openUrlInBrowser(String URL) {

		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.browse(new URI(URL));
				return true;
			} catch (IOException | URISyntaxException e) {
				logger.error("Error opening multiplatform browser.\n\n", e);
			}
		} else {
			OSinfo OS = retrieveOS();
			Runtime runtime = Runtime.getRuntime();

			if (OS.isLinux()) {
				try {
					runtime.exec("xdg-open " + URL);
					return true;
				} catch (IOException e) {
					logger.error("Error opening URL with Linux Native command\n\n", e);
				}
			} else if (OS.isMac()) {
				try {
					runtime.exec("open " + URL);
					return true;
				} catch (IOException e) {
					logger.error("Error opening URL with Mac-OS Native command\n\n", e);
				}
			} else if (OS.isWindows()) {
				try {
					runtime.exec("rundll32 url.dll,FileProtocolHandler " + URL);
					return true;
				} catch (IOException e) {
					logger.error("Error opening URL with Windows Native command\n\n", e);
				}
			} else {
				return false;
			}
		}
		return false;
	}

	public static boolean openUrlInBrowserNative(String URL) {
		OSinfo OS = retrieveOS();
		Runtime runtime = Runtime.getRuntime();

		if (OS.isLinux()) {
			try {
				runtime.exec("xdg-open " + URL);
				return true;
			} catch (IOException e) {
				logger.error("Error opening URL with Linux Native command\n\n", e);
			}
		} else if (OS.isMac()) {
			try {
				runtime.exec("open " + URL);
				return true;
			} catch (IOException e) {
				logger.error("Error opening URL with Mac-OS Native command\n\n", e);
			}
		} else if (OS.isWindows()) {
			try {
				runtime.exec("rundll32 url.dll,FileProtocolHandler " + URL);
				return true;
			} catch (IOException e) {
				logger.error("Error opening URL with Windows Native command\n\n", e);
			}
		}

		return false;
	}

	/**
	 * Launch a system runnable file as .sh or .bat, reading it's output and waiting for a specific string in output
	 * @Param wait timeout is in milliseconds, after it's elapsed String will be returned even if signal was not retrieved from process output
	 * @return String output of process
	 * @throws IOException
	 */
	public static String launchBatchOrBashAndWaitForOutput(String execPath, String signal, int waitTimeout) throws IOException {

		if(execPath == null || execPath.equals("") || signal == null || signal.equals("")) {return "";}

		long startTimeMillisec = System.currentTimeMillis();
		long updatedTime = startTimeMillisec;
		Runtime runtime = Runtime.getRuntime();
		StringBuilder builder = new StringBuilder();

		Process proc = runtime.exec(execPath);
		InputStream os = proc.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(os));
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line+System.getProperty("line.separator"));
				updatedTime = System.currentTimeMillis();
				if(line.contains("HWINFOPANEL_STARTED_OK")) {
					logger.info("Signal found in output, returning string - Signal=" + signal);
					break;
				}

				if( (updatedTime-startTimeMillisec) > waitTimeout) {
					logger.info("Timeout occurred - " + waitTimeout + " elapsed without finding output: " + signal);
					break;
				}
			}
			reader.close();
		} catch (IOException e) {
			logger.error("There was an error reading process output from: " + execPath);
		}

		return builder.toString();
	}

	public static Process launchCommand(String command) throws IOException {
		List<String> commands = new ArrayList<>();
		if(new OSinfo().isWindows()) {
			commands.add("cmd.exe");
			commands.add("/C");
		}
		commands.add(command);

		ProcessBuilder builder = new ProcessBuilder(commands);
		return builder.start();
	}

	/**
	 * retrieve list of commands, write to file and run it in current OS
	 */
	public static Process launchBatchOrBash(String batchOrBashPath, boolean minimizedShell) {

		OSinfo OS = getOsInfoLazy();
		logger.info("Launching commands on currentOS: " + OS.getOsName() + OS.getOsArch());

		Runtime runtime = Runtime.getRuntime();

		try {
			try {
				new File(batchOrBashPath).setExecutable(true);

			} catch (Exception e1) {}

			// CASE WINDOWS
			if (OS.isWindows()) {
				String minimizedOpt = minimizedShell ? "/min " : "";
				logger.info("Commands launched without exceptions :)");
				return runtime.exec("cmd /c start " + minimizedOpt + "\"\" \"" + batchOrBashPath + "\"");

			} else {

				// TODO LINUX/MAC SHELL show on/off
				logger.info("Executing process [pwd]");
				execNlogProcessOutput(runtime.exec("pwd"));
				logger.info("Executing process [chmod u+x" + batchOrBashPath + "]");
				Process executableMaker = runtime.exec("chmod u+x " + batchOrBashPath);
				execNlogProcessOutput(executableMaker);
				executableMaker.waitFor();
				logger.info("Commands launched without exceptions :)");

				return runtime.exec(batchOrBashPath);
			}
		} catch (Exception e) {
			logger.error(" !!! Error launching generated commands file !!!\n\n", e);
			return null;
		}
	}

	public static Process launchExeAsAdmin(String exePath, String exeLaunchCommand, String... args) throws IOException, InterruptedException {
		try {
			new File(exePath).setExecutable(true);
		} catch (Exception e) {
			logger.error("Cannot set jar executable: " + exeLaunchCommand);
		}

		String params = " ";
		if(OS_INFO_LAZY.isWindows()) {
			if(args != null) {
				params = " -ArgumentList ";
				int i = 0;
				for(String arg : args) {
					params +=  "'"+ arg +"'" + ((i < args.length - 1) ? "," : "");
					i++;
				}
			}
			return Runtime.getRuntime().exec("powershell.exe Start-Process '" + exeLaunchCommand + "' "
					+ params +  " -verb RunAs");
		} else {
			if(args != null) {
				for(String arg : args) {
					params +=  arg + " ";
				}
			}

			logger.info("Executing process [pwd]");
			execNlogProcessOutput(Runtime.getRuntime().exec("pwd"));
			logger.info("Executing process [chmod u+x " + exeLaunchCommand + "]");
			Process executableMaker = Runtime.getRuntime().exec("chmod u+x " + exeLaunchCommand);
			execNlogProcessOutput(executableMaker);
			executableMaker.waitFor();
			return Runtime.getRuntime().exec("sudo -A " + exeLaunchCommand + params);
		}
	}

	/**
	 * retrieve list of commands, write to file and run it in current OS
	 */
	public static Process launchBatchOrBashSimple(String batchOrBashPath) {

		OSinfo OS = getOsInfoLazy();
		logger.info("Launching commands on currentOS: " + OS.getOsName() + OS.getOsArch());

		Runtime runtime = Runtime.getRuntime();

		try {
			try {
				new File(batchOrBashPath).setExecutable(true);

			} catch (Exception e1) {}

			// CASE WINDOWS
			if (OS.isWindows()) {
				logger.info("Commands launched without exceptions :)");
				return runtime.exec(batchOrBashPath);

			} else {

				// TODO LINUX/MAC SHELL show on/off
				logger.info("Executing process [pwd]");
				execNlogProcessOutput(runtime.exec("pwd"));
				logger.info("Executing process [chmod u+x" + batchOrBashPath + "]");
				Process executableMaker = runtime.exec("chmod u+x " + batchOrBashPath);
				execNlogProcessOutput(executableMaker);
				executableMaker.waitFor();
				logger.info("Commands launched without exceptions :)");

				return runtime.exec(batchOrBashPath);
			}
		} catch (Exception e) {
			logger.error(" !!! Error launching generated commands file !!!\n\n", e);
			return null;
		}
	}

	public static String readProcessOutput(InputStream inputStream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder builder = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			reader.close();
		} catch (IOException e) {
			logger.error("Error reading process output:\n\n", e);
		}

		return builder.toString();
	}

	public static ArrayList<String> readProcessOutputLines(InputStream inputStream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		ArrayList<String> lines = new ArrayList<>();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
		} catch (IOException e) {
			logger.error("Error reading process output:\n\n", e);
		}

		return lines;
	}

	public static String[] execNlogProcessOutput(Process process) {
		String output = SysUtils.readProcessOutput(process.getInputStream());
		logger.info(output);

		String errorString = SysUtils.readProcessOutput(process.getErrorStream());
		logger.error(errorString);

		return new String[] {"Process output: " + output, StringWorker.trimToEmpty(errorString)};
	}

	public static boolean cdIntoCommandLine(File directory) {
		OSinfo OS = retrieveOS();

		if(directory != null && directory.exists()) {
			directory = (directory.isDirectory())? directory : directory.getParentFile();
			if(OS.isWindows()) {
				try {
					Runtime rt = Runtime.getRuntime();
					rt.exec("cmd.exe /c start dir /p", null, directory);
					return true;
				}catch(Exception e) {
					logger.error("", e);
					return false;
				}
			}else{
				try {
					ProcessBuilder builder = new ProcessBuilder();
					builder.command("sh", "-c", "ls");
					builder.directory(directory);
					builder.start();

					return true;
				} catch (IOException e) {
					logger.error("", e);
					return false;
				}
			}
		}

		return false;
	}


	public static File findDirectoryInsideRoot(File parentDirectory, String directoryToFind, boolean caseSensitive) {

		if(directoryToFind == null) {
			return null;
		}

		File[] files = parentDirectory.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                continue;
            }
            if (caseSensitive && file.getName().equals(directoryToFind.trim())) {
                return file;
            } else if (!caseSensitive && file.getName().toLowerCase().equals(directoryToFind.trim().toLowerCase())) {
                return file;
            }

            if(file.isDirectory()) {
            	return findDirectoryInsideRoot(file, directoryToFind, caseSensitive);
            }
        }

        return null;
	}

	public static File findFileInsideRoot(File parentDirectory, String fileToFind, boolean caseSensitive) {

		if(fileToFind == null) {
			return null;
		}

		File[] files = parentDirectory.listFiles();
		for (File file : files) {
			if (caseSensitive && file.isFile() && file.getName().equals(fileToFind.trim())) {
				return file;

			} else if (!caseSensitive && file.isFile() && file.getName().toLowerCase().equals(fileToFind.trim().toLowerCase())) {
				return file;

			} else if(file.isDirectory()) {
				return findFileInsideRoot(file, fileToFind, caseSensitive);
			}
		}

		return null;
	}

/////////////////////////////  WINDOWS ONLY  //////////////////////////////////////////

	public static ArrayList<String> findWindowsProgramPath(String programName) {
		ArrayList<String> foundList = new ArrayList<String>();

		if(programName == null) {
			return foundList;
		}

		File sw32 = new File(winPath32, programName);
		File sw64 = new File(winPath64, programName);

		if(sw64.exists()) {
			foundList.add(sw64.getAbsolutePath());
		}
		if(sw32.exists()) {
			foundList.add(sw32.getAbsolutePath());
		}

		return foundList;
	}

	public static List<String> findWindowsProgramPathByNamePart(String programNamePart) {
		ArrayList<String> foundList = new ArrayList<String>();

		if(programNamePart == null) {
			return foundList;
		}

		File[] sw32 = new File(winPath32).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().contains(programNamePart);
			}
		});

		File[] sw64 = new File(winPath64).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().contains(programNamePart.toLowerCase());
			}
		});

		for(File dir : sw64) {
			foundList.add(dir.getAbsolutePath());
		}
		for(File dir : sw32) {
			foundList.add(dir.getAbsolutePath());
		}

		return foundList;
	}

	public static String getClassPathFromAllJarsFoundInRoot(String root, boolean isRootCurrentDir) {
		String classPath = "";
		String separator = getOsInfoLazy().isWindows() ? ";" : ":";

		for (File found : FileNameSearchAdvanced.findOnlyFilesByMatch(
				new File(root), ".jar", false, true, false, false, Long.MAX_VALUE)) {
			classPath += found + separator;
		}

		classPath = StringWorker.removeLastChar(classPath);

		String relativePrefix = isRootCurrentDir ? "./" : "";

		if(getOsInfoLazy().isWindows())
			return "\"" + relativePrefix + classPath + "\"";
		else
			return ("/") + relativePrefix + classPath + "/";
	}

	public static ArrayList<String> getAllDirsWithJavaExeInside(String root, String fileToFind) {
		ArrayList<String> stringRetrievedPaths = new ArrayList<String>();

		File currentRootDir = new File(root);
		currentRootDir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				File currentFile = new File(dir.getPath() + File.separator + name);
				if(findFileInsideRoot(currentFile, fileToFind, true) != null) {
					stringRetrievedPaths.add(currentFile.getAbsolutePath());
				}
				return false;
			}
		});

		return stringRetrievedPaths;
	}

	public static ArrayList<String> getAllJavaWinVersionFromRoot(String root) {
		ArrayList<String> stringRetrievedPaths = new ArrayList<String>();

		File currentRootDir = new File(root);
		currentRootDir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {

				if(!(name.toLowerCase().indexOf("jdk")<0) || !(name.toLowerCase().indexOf("jre")<0)) {
					if (dir.isDirectory()) {
						String currentPath = dir.getPath() + File.separator + name;
						File binFolder = new File(currentPath+"/bin");
						File exeJvm = new File(binFolder.getAbsolutePath() + File.separator + winJavaExecutable);
						// if name contains (case insensitive) jdk or jre then it's a java install directory
						if (exeJvm.exists()) {
							stringRetrievedPaths.add(currentPath);
							return true;
						}
					}
				}
				return false;
			}
		}); // END FILE FILTER

		return stringRetrievedPaths;
	}


////////////////////////////////  MAC-OS ONLY  /////////////////////////////////////////////////

	public static ArrayList<String> getMacOsJavaVersions() {
		ArrayList<String> stringRetrievedPaths = new ArrayList<String>();

		// TODO implement MAcOs java version

		return stringRetrievedPaths;
	}

	public static OSinfo getOsInfoLazy() {
		return (OS_INFO_LAZY!=null)? OS_INFO_LAZY : retrieveOS();
	}

	public enum Browser {
		CHROME("Google Chrome", "start chrome ", "google-chrome ", "open -a google-chrome "),
		FIREFOX("Mozilla Firefox", "start firefox ", "firefox ", "open -a firefox "),
		SAFARI("Safari", "start safari ", "safari ", "open -a safari "),
		EDGE("Microsoft Edge", "start microsoft-edge ", "microsoft-edge ", "open -a microsoft-edge "),
		IE("Internet Explorer", "start iexplore ", "iexplore ", "open -a iexplore "),
		OPERA("Opera", "start opera ", "opera ", "open -a opera ");

		public static Browser[] defaultBrowserOrder = new Browser[] {Browser.CHROME, Browser.FIREFOX, Browser.IE};

		public String browserName;
		public String winProgamName;
		public String bashProgamName;
		public String macProgamName;

		private Browser(String browserName, String winOsSwName, String bashOsSwName, String macProgamName) {
			this.browserName = browserName;
			this.winProgamName = winOsSwName;
			this.bashProgamName = bashOsSwName;
			this.macProgamName = macProgamName;
		}

		public String getOsLaunchSwName() {

			if(OS_INFO_LAZY.isWindows()) {
				return this.winProgamName;

			} else if(OS_INFO_LAZY.isMac()) {
				return this.macProgamName;

			} else {
				return this.bashProgamName;
			}
		}
	}

	/**
	 * Get registry add command by path, key, and value
	 *
	 * [REG ADD "HKEY_CLASSES_ROOT\*\shell\Open_with_ScreepT\command" /t REG_SZ /d "\"C:\Users\xx\Desktop\SCREEPT_LATEST_V1\ScreepT.exe\" \"^%1\"" /f]
	 */
	public static String getRegistryAddString(String path, String key, String val) {
		return "REG ADD \""+path+"\" /v \""+key+"\" /t REG_SZ /d \""+val+"\" /f";
	}

	/**
	 * Get registry add command by path, val, and parameter
	 *
	 * [REG ADD "HKEY_CLASSES_ROOT\*\shell\Open_with_ScreepT\command" /t REG_SZ /d "\"C:\Users\xx\Desktop\SCREEPT_LATEST_V1\ScreepT.exe\" \"^%1\"" /f]
	 */
	public static String getRegistryAddStringVal(String path, String val, String param) {
		return "REG ADD \""+path+"\" /t REG_SZ /d \"\\\""+val+"\\\" \\\"" + param + "\\\"\" /f";
	}

	/**
	 * Get registry remove command by key
	 * @param path
	 * @return
	 */
	public static String getPathRemoveString(String path) {
		return "REG DELETE \""+path+"\" /f";
	}

}

