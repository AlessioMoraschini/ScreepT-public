package os.commons.utils.linux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import various.common.light.utility.log.SafeLogger;

/**
 * @author amoraschini
 *
 */
public class LinuxFindJvmVersions {
	
	private static SafeLogger logger = new SafeLogger(LinuxFindJvmVersions.class);

	private static final String[] LIST_ALTERNATIVES = { "update-alternatives", "--list", "java" };
	private static final int TIMEOUT_SECONDS = 5;

	/**
	 * Homes of the Java alternatives of the system. Only the non interactive "--list" is used ("--config" waits for a choice on its
	 * input and never ends), with a time limit: looking for Java must never block the start. The usual folders are searched by
	 * {@code JvmLocator} anyway.
	 */
	public static ArrayList<String> CommandLineJavaAlternativesPaths() throws IOException{

		logger.info("Searching linux java alternatives with native command: " + String.join(" ", LIST_ALTERNATIVES));

		File output = File.createTempFile("screept-java-alternatives", ".txt");
		Process finder = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(LIST_ALTERNATIVES);
			builder.redirectErrorStream(true);
			builder.redirectOutput(output);
			finder = builder.start();
			finder.getOutputStream().close();
			if (!finder.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				logger.error("Java alternatives search timed out: skipped");
				return new ArrayList<>();
			}
			return homesFromList(Files.readAllLines(output.toPath(), Charset.defaultCharset()));

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new ArrayList<>();

		} finally {
			if (finder != null) {
				finder.destroy();
			}
			output.delete();
		}
	}

	/** Java homes of the lines ending with "/bin/java" (error messages and other lines are ignored), without duplicates */
	public static ArrayList<String> homesFromList(List<String> lines) {
		ArrayList<String> homes = new ArrayList<>();
		for (String line : lines) {
			String path = line.trim();
			if (!path.startsWith("/") || !path.endsWith("/bin/java")) {
				continue;
			}
			String home = path.substring(0, path.length() - "/bin/java".length());
			if (!home.isEmpty() && !homes.contains(home)) {
				homes.add(home);
			}
		}
		return homes;
	}

	public static String extractDirectory(String line) {
		if (line == null || "".equals(line.trim())) {
			return null;
		}

		// [/\\\\] matches all slashes and backslashes - \\s is equivalent to escaped spaces
		Matcher matcher = Pattern.compile("\\s.*[/\\\\](.*[/\\\\].*[a-z]?)\\s").matcher(line);
		StringBuilder builder = new StringBuilder();

		while (matcher.find()) {
			builder.append(matcher.group());
		}

		String[] parts = builder.toString().split("\\s");

		for (String part : parts) {
			if (part.contains("/")) {
				File thisPartFile = new File(part);
				// matches all characters followed by a number followed by another character
				while (thisPartFile != null && !thisPartFile.getName().matches(".*\\d.*")) {
					thisPartFile = thisPartFile.getParentFile();
				}

				return thisPartFile != null ? thisPartFile.getAbsolutePath() : null;
			}
		}

		return null;
	}
	
	public static String extractVersion(String folderName) {
		// Removes every slash, every character, : 
		// Then removes 64 or 32 (used for bit instead of version) and the separator character (-)
		return folderName.replaceAll("[a-zA-Z/\\\\:]", "").replaceAll("-|64|32", "");
	}
}
