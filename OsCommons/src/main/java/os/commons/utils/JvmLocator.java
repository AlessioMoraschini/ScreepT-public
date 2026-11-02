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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import os.commons.om.OSinfo;

/**
 * Finds Java homes (directories with a bin/java inside) in the usual places of each OS, on top of the ones
 * already found by {@link SysUtils}: the "jre" folder next to the application, JAVA_HOME, the PATH, the
 * folders of the most common vendors (Oracle, Eclipse Adoptium/Temurin, Amazon Corretto, Microsoft, Azul Zulu,
 * BellSoft Liberica, IBM Semeru, GraalVM, Red Hat...) and the managers' folders (SDKMAN, IntelliJ, asdf, Homebrew).
 */
public final class JvmLocator {

	/** Folder, relative to the application root, where ScreepT keeps the Java it downloads */
	public static final String LOCAL_JRE_DIR = "jre";

	private static final List<String> VENDOR_WORDS = Arrays.asList("java", "jdk", "jre", "openjdk", "adoptium", "adoptopenjdk",
			"temurin", "corretto", "zulu", "microsoft", "bellsoft", "liberica", "semeru", "graal", "redhat", "sapmachine", "dragonwell");

	private JvmLocator() {
	}

	/** @return the Java homes found in the extra locations, without duplicates, the local "jre" folder first */
	public static List<String> findExtraJvmHomes(OSinfo os) {
		Set<File> homes = new LinkedHashSet<File>();

		// 1) the Java shipped with (or downloaded for) this ScreepT installation: highest priority
		addHome(homes, new File(LOCAL_JRE_DIR));
		addHome(homes, new File(LOCAL_JRE_DIR, "Contents/Home"));
		addHomesIn(homes, new File(LOCAL_JRE_DIR), false);

		// 2) JAVA_HOME and the PATH
		String javaHome = System.getenv("JAVA_HOME");
		if (javaHome != null && !javaHome.trim().isEmpty()) {
			addHome(homes, new File(javaHome.trim()));
		}
		addHomesFromPath(homes);

		// 3) well known folders of the current OS
		if (os.isWindows()) {
			for (String variable : new String[] { "ProgramFiles", "ProgramFiles(x86)", "ProgramW6432" }) {
				addVendorHomes(homes, envDir(variable, null));
			}
			addVendorHomes(homes, envDir("LOCALAPPDATA", "Programs"));
			addHomesIn(homes, envDir("USERPROFILE", ".jdks"), false);

		} else if (os.isMac()) {
			addHomesIn(homes, new File("/Library/Java/JavaVirtualMachines"), true);
			addHomesIn(homes, userDir("Library/Java/JavaVirtualMachines"), true);
			for (String brewRoot : new String[] { "/opt/homebrew/opt", "/usr/local/opt" }) {
				File[] formulas = new File(brewRoot).listFiles();
				for (File formula : formulas == null ? new File[0] : formulas) {
					if (formula.getName().toLowerCase(Locale.ROOT).startsWith("openjdk")) {
						addHome(homes, new File(formula, "libexec/openjdk.jdk/Contents/Home"));
						addHome(homes, formula);
					}
				}
			}
			addHomesIn(homes, userDir(".sdkman/candidates/java"), false);
			addHomesIn(homes, userDir(".jdks"), false);

		} else { // Linux and the others
			for (String root : new String[] { "/usr/lib/jvm", "/usr/java", "/opt/java", "/usr/lib64/jvm" }) {
				addHomesIn(homes, new File(root), false);
			}
			addVendorHomes(homes, new File("/opt"));
			addHomesIn(homes, userDir(".sdkman/candidates/java"), false);
			addHomesIn(homes, userDir(".jdks"), false);
			addHomesIn(homes, userDir(".asdf/installs/java"), false);
		}

		List<String> result = new ArrayList<String>();
		for (File home : homes) {
			result.add(home.getAbsolutePath());
		}
		return result;
	}

	/**
	 * @return the version declared by the "release" file of a Java home (JAVA_VERSION="..."), or an empty string if
	 *         the file is missing or has no version. It is far more reliable than the name of the folder.
	 */
	public static String versionOfHome(File home) {
		if (home == null) {
			return "";
		}
		File release = new File(home, "release");
		if (!release.isFile()) {
			return "";
		}
		try {
			for (String line : Files.readAllLines(release.toPath(), Charset.forName("ISO-8859-1"))) {
				if (line.startsWith("JAVA_VERSION=")) {
					return line.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
				}
			}
		} catch (IOException | RuntimeException e) {
			// unreadable: caller falls back to the folder name
		}
		return "";
	}

	/** A Java home is a directory containing bin/java (or bin/java.exe) */
	public static boolean isJavaHome(File dir) {
		return dir != null && dir.isDirectory()
				&& (new File(dir, "bin/java.exe").isFile() || new File(dir, "bin/java").isFile());
	}

	// ---------------------------------------------------------------------------------------------

	private static void addHome(Set<File> homes, File dir) {
		if (!isJavaHome(dir)) {
			return;
		}
		try {
			dir = dir.getCanonicalFile();
		} catch (IOException e) {
			dir = dir.getAbsoluteFile();
		}
		homes.add(dir);
	}

	/** Adds the direct subfolders of root that are Java homes, or have the home inside Contents/Home (macOS bundles) */
	private static void addHomesIn(Set<File> homes, File root, boolean macBundles) {
		File[] children = root == null ? null : root.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			addHome(homes, child);
			// archives of macOS builds keep the home one level deeper
			addHome(homes, new File(child, "Contents/Home"));
		}
	}

	/** root/<vendor folder>/<jdk folder>, where the vendor folder is recognized by its name (Program Files style) */
	private static void addVendorHomes(Set<File> homes, File root) {
		File[] vendors = root == null ? null : root.listFiles();
		if (vendors == null) {
			return;
		}
		for (File vendor : vendors) {
			String name = vendor.getName().toLowerCase(Locale.ROOT);
			boolean vendorFolder = false;
			for (String word : VENDOR_WORDS) {
				if (name.contains(word)) {
					vendorFolder = true;
					break;
				}
			}
			if (vendorFolder && vendor.isDirectory()) {
				addHome(homes, vendor);
				addHomesIn(homes, vendor, false);
			}
		}
	}

	private static void addHomesFromPath(Set<File> homes) {
		String path = System.getenv("PATH");
		if (path == null) {
			return;
		}
		for (String entry : path.split(File.pathSeparator)) {
			if (entry.trim().isEmpty()) {
				continue;
			}
			File bin = new File(entry.trim());
			File home = bin.getParentFile();
			// real homes have a release file: this skips launcher shims like ...\Oracle\Java\javapath
			if (home != null && "bin".equalsIgnoreCase(bin.getName()) && new File(home, "release").isFile()) {
				addHome(homes, home);
			}
		}
	}

	private static File envDir(String variable, String child) {
		String value = System.getenv(variable);
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return child == null ? new File(value) : new File(value, child);
	}

	private static File userDir(String child) {
		String home = System.getProperty("user.home");
		return home == null ? null : new File(home, child);
	}
}
