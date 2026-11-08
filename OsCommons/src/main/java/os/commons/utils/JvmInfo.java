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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * What can be said about an installed Java without running it: read from the "release" file of its home and from
 * the folder itself. Gives the short readable name used in the launcher ("jdk Oracle 21 OpenJDK"), the vendor, the
 * kind (JDK or JRE), the architecture and the documentation links.
 */
public final class JvmInfo {

	public final String home;
	/** Full version as declared ("21.0.2", "1.8.0_362"), or what can be guessed from the folder name */
	public final String version;
	/** Feature version: 8, 11, 17, 21... (0 when unknown) */
	public final int major;
	public final String vendor;
	public final boolean jdk;
	public final String architecture;
	/** "OpenJDK" or "Java SE" */
	public final String flavor;
	public final boolean hasJavaExecutable;

	private JvmInfo(String home, String version, int major, String vendor, boolean jdk, String architecture, String flavor, boolean hasJavaExecutable) {
		this.home = home;
		this.version = version;
		this.major = major;
		this.vendor = vendor;
		this.jdk = jdk;
		this.architecture = architecture;
		this.flavor = flavor;
		this.hasJavaExecutable = hasJavaExecutable;
	}

	/** Reads the information of the Java home (never throws: unknown values are left readable) */
	public static JvmInfo of(String homePath) {
		File home = new File(homePath);
		Properties release = new Properties();
		File releaseFile = new File(home, "release");
		if (releaseFile.isFile()) {
			try (InputStream in = new FileInputStream(releaseFile)) {
				release.load(in);
			} catch (IOException | RuntimeException e) {
				// unreadable release file: the folder name will do
			}
		}
		String version = unquote(release.getProperty("JAVA_VERSION"));
		if (version.isEmpty()) {
			version = JvmLocator.versionOfHome(home);
		}
		if (version == null || version.isEmpty()) {
			version = home.getName();
		}
		String lowerPath = homePath.toLowerCase(Locale.ROOT);
		String implementor = unquote(release.getProperty("IMPLEMENTOR"));
		String vendor = vendorName(implementor, lowerPath);
		boolean jdk = new File(home, "bin" + File.separator + "javac").isFile() || new File(home, "bin" + File.separator + "javac.exe").isFile()
				|| new File(home, "lib" + File.separator + "tools.jar").isFile();
		String arch = unquote(release.getProperty("OS_ARCH"));
		if (arch.isEmpty()) {
			arch = lowerPath.contains("x86") && !lowerPath.contains("x86_64") ? "x86" : "";
		}
		String runtimeText = (unquote(release.getProperty("JAVA_RUNTIME_VERSION")) + " " + implementor + " " + unquote(release.getProperty("SOURCE")) + " " + lowerPath).toLowerCase(Locale.ROOT);
		boolean open = runtimeText.contains("openjdk") || runtimeText.contains("temurin") || runtimeText.contains("adoptium") || runtimeText.contains("corretto")
				|| runtimeText.contains("zulu") || runtimeText.contains("liberica") || runtimeText.contains("semeru") || runtimeText.contains("microsoft");
		boolean hasJava = new File(home, "bin" + File.separator + "java").isFile() || new File(home, "bin" + File.separator + "java.exe").isFile();
		return new JvmInfo(homePath, version, majorOf(version), vendor, jdk, arch, open ? "OpenJDK" : "Java SE", hasJava);
	}

	/** Short readable name: "jdk Oracle 21 OpenJDK", "jre Adoptium 17 OpenJDK" */
	public String displayName() {
		return (jdk ? "jdk" : "jre") + " " + vendor + (major > 0 ? " " + major : "") + " " + flavor;
	}

	/** Official documentation of this Java feature version */
	public String documentationUrl() {
		if (major >= 9) {
			return "https://docs.oracle.com/en/java/javase/" + major + "/";
		}
		return "https://docs.oracle.com/javase/8/docs/";
	}

	/** Site of the vendor of this Java, or null when unknown */
	public String vendorUrl() {
		String v = vendor.toLowerCase(Locale.ROOT);
		if (v.contains("adoptium") || v.contains("temurin")) {
			return "https://adoptium.net/documentation/";
		} else if (v.contains("corretto") || v.contains("amazon")) {
			return "https://docs.aws.amazon.com/corretto/";
		} else if (v.contains("zulu") || v.contains("azul")) {
			return "https://docs.azul.com/core/";
		} else if (v.contains("microsoft")) {
			return "https://learn.microsoft.com/java/openjdk/";
		} else if (v.contains("liberica") || v.contains("bellsoft")) {
			return "https://bell-sw.com/documentation/";
		} else if (v.contains("oracle")) {
			return "https://www.oracle.com/java/technologies/downloads/";
		}
		return null;
	}

	/**
	 * Readable labels for a list of homes, in the same order. A name shared by more than one Java gets a progressive
	 * number: "jre Oracle 21 OpenJDK (1)", "jre Oracle 21 OpenJDK (2)"; a unique name stays as it is.
	 */
	public static List<String> uniqueLabels(List<String> homes) {
		List<String> names = new ArrayList<String>();
		Map<String, Integer> total = new HashMap<String, Integer>();
		for (String home : homes) {
			String name = of(home).displayName();
			names.add(name);
			total.put(name, total.containsKey(name) ? total.get(name) + 1 : 1);
		}
		Map<String, Integer> seen = new HashMap<String, Integer>();
		List<String> labels = new ArrayList<String>();
		for (String name : names) {
			if (total.get(name) > 1) {
				int n = seen.containsKey(name) ? seen.get(name) + 1 : 1;
				seen.put(name, n);
				labels.add(name + " (" + n + ")");
			} else {
				labels.add(name);
			}
		}
		return labels;
	}

	static String vendorName(String implementor, String lowerPath) {
		String text = (implementor + " " + lowerPath).toLowerCase(Locale.ROOT);
		if (text.contains("adoptium") || text.contains("temurin") || text.contains("adoptopenjdk")) {
			return "Adoptium";
		} else if (text.contains("corretto") || text.contains("amazon")) {
			return "Corretto";
		} else if (text.contains("zulu") || text.contains("azul")) {
			return "Zulu";
		} else if (text.contains("microsoft")) {
			return "Microsoft";
		} else if (text.contains("liberica") || text.contains("bellsoft")) {
			return "Liberica";
		} else if (text.contains("semeru") || text.contains("ibm")) {
			return "IBM";
		} else if (text.contains("graal")) {
			return "GraalVM";
		} else if (text.contains("sapmachine")) {
			return "SapMachine";
		} else if (text.contains("redhat") || text.contains("red hat")) {
			return "Red Hat";
		} else if (text.contains("oracle") || text.contains("program files\\java") || text.contains("/library/java/javavirtualmachines/jdk")) {
			return "Oracle";
		}
		return implementor.isEmpty() ? "Java" : implementor.replace("Corporation", "").replace("Inc.", "").trim();
	}

	static int majorOf(String version) {
		try {
			String v = version.trim();
			if (v.startsWith("1.")) {
				return Integer.parseInt(v.substring(2, v.indexOf('.', 2) > 0 ? v.indexOf('.', 2) : v.length()).replaceAll("\\D.*", ""));
			}
			return Integer.parseInt(v.replaceAll("^(\\d+).*$", "$1"));
		} catch (RuntimeException e) {
			return 0;
		}
	}

	private static String unquote(String value) {
		return value == null ? "" : value.replace("\"", "").trim();
	}
}
