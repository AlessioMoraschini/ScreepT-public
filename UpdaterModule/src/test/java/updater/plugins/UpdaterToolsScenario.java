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
package updater.plugins;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Permission;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;

import main.MainStandAloneUpdater;
import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;

/**
 * Run by {@link UpdaterToolsTest} in a JVM of its own with an empty temporary working directory: the release tools of
 * <i>updater.jar</i> (zip the folders, generate the hashes of the manifest) as Alessio launches them from the build folder.
 * The tool ends with a pause and System.exit: the exit is blocked and the log is polled instead.
 * Exit code 0 = every check passed.
 */
public class UpdaterToolsScenario {

	static int expectedSuccesses = 0;

	public static void main(String[] args) {
		int code = 1;
		try {
			System.setSecurityManager(new SecurityManager() {
				@Override
				public void checkExit(int status) {
					throw new SecurityException("exit " + status + " blocked");
				}

				@Override
				public void checkPermission(Permission perm) {
				}
			});
			run();
			System.out.println("SCENARIO OK");
			code = 0;
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}
		System.setSecurityManager(null);
		System.exit(code); // not halt(): the coverage agent writes its data in a shutdown hook
	}

	static void check(boolean ok, String what) {
		if (!ok)
			throw new AssertionError(what);
	}

	static void write(String path, String content) throws Exception {
		File f = new File(path);
		if (f.getParentFile() != null)
			f.getParentFile().mkdirs();
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}

	/** Starts the tool as the command line does and waits until it reports success. */
	static void runTool(final String... args) throws Exception {
		expectedSuccesses++;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					MainStandAloneUpdater.main(args);
				} catch (Throwable ignored) {
				}
			}
		});
		t.setDaemon(true);
		t.start();
		long end = System.currentTimeMillis() + 60000;
		while (System.currentTimeMillis() < end) {
			File log = new File("updater_history.log");
			if (log.exists()) {
				String s = new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8);
				int n = 0, i = -1;
				while ((i = s.indexOf("COMPLETE SUCCESS", i + 1)) >= 0)
					n++;
				if (n >= expectedSuccesses)
					return;
				// the blocked System.exit of a finished tool is logged as a failure five seconds later: not a real one
				if (count(s, "PROCESS FAILED") > count(s, "exit 0 blocked"))
					throw new AssertionError("the tool failed:\n" + s);
			}
			Thread.sleep(50);
		}
		throw new AssertionError("the tool did not finish");
	}

	static int count(String text, String what) {
		int n = 0, i = -1;
		while ((i = text.indexOf(what, i + 1)) >= 0)
			n++;
		return n;
	}

	static Properties load(String path) throws Exception {
		Properties p = new Properties();
		try (InputStream in = new FileInputStream(path)) {
			p.load(in);
		}
		return p;
	}

	static String sha512(String path) throws Exception {
		return ChecksumHasher.getFileHashBuffered(new File(path), HashingFunction.SHA512, true);
	}

	static void run() throws Throwable {
		// 1) -zipFolders-generateHashes: every folder becomes a zip, but the ones listed in FOLDERS_TO_SKIP
		write("manifest.properties", "FOLDERS_TO_SKIP=Skipped\n");
		write("Alpha/a.txt", "alpha");
		write("Alpha/sub/deep.txt", "deep");
		write("Beta/b.txt", "beta");
		write("Skipped/s.txt", "skipped");
		runTool("-zipFolders-generateHashes", "false", "manifest.properties");
		check(new File("Alpha.zip").isFile() && new File("Beta.zip").isFile(), "the folders are zipped");
		check(!new File("Skipped.zip").exists(), "a skipped folder is not zipped");
		Properties m = load("manifest.properties");
		check(sha512("Alpha.zip").equalsIgnoreCase(m.getProperty("Alpha.zip")), "hash of Alpha.zip in the manifest: " + m);
		check(sha512("Beta.zip").equalsIgnoreCase(m.getProperty("Beta.zip")), "hash of Beta.zip in the manifest");
		check(m.getProperty("manifest.properties") == null && m.getProperty("updater_history.log") == null, "manifest and log are not hashed");
		check(new File("Alpha.zip").length() > 0, "zip has content");

		// 2) -generateHashes true: the "updates" folder is zipped into updates.zip and every file gets its hash
		write("updates/ScreepT.jar", "new jar bytes");
		write("updates/Resources/conf.txt", "conf");
		write("notes.txt", "a file next to the zips");
		write("run.bat", "ignored");
		runTool("-generateHashes", "true", "manifest.properties");
		check(new File("updates.zip").isFile(), "updates.zip created");
		m = load("manifest.properties");
		check(sha512("updates.zip").equalsIgnoreCase(m.getProperty("updates.zip")), "hash of updates.zip");
		check(sha512("notes.txt").equalsIgnoreCase(m.getProperty("notes.txt")), "loose files are hashed too");
		check(m.getProperty("run.bat") == null, "batch files are not hashed");

		// 3) -generateHashes-mainRelease: the release folder named in FTP.properties is zipped and hashed
		write("ReleaseFolder/ScreepT.exe", "exe");
		write("ReleaseFolder/Application/app.jar", "jar");
		write("FTP.properties", "RELEASE_FOLDER-OR-ZIP_TO_ELABORATE=ReleaseFolder\n");
		write("updater.jar", "pretend updater jar");
		write("release_manifest.properties", "");
		runTool("-generateHashes-mainRelease", "true", "release_manifest.properties");
		check(new File("ReleaseFolder.zip").isFile(), "release folder zipped");
		Properties r = load("release_manifest.properties");
		check(sha512("ReleaseFolder.zip").equalsIgnoreCase(r.getProperty("ReleaseFolder.zip")), "hash of the release zip: " + r);
		check(sha512("updater.jar").equalsIgnoreCase(r.getProperty("updater.jar")), "hash of the updater jar: " + r);

		// 4) the release is already a zip
		write("FTP.properties", "RELEASE_FOLDER-OR-ZIP_TO_ELABORATE=ReleaseFolder.zip\n");
		write("release_manifest2.properties", "");
		runTool("-generateHashes-mainRelease", "false", "release_manifest2.properties");
		r = load("release_manifest2.properties");
		check(sha512("ReleaseFolder.zip").equalsIgnoreCase(r.getProperty("ReleaseFolder.zip")), "hash of a ready zip: " + r);

		// 5) only the folders named in FOLDERS_TO_KEEP are zipped, without the parent folder inside the zip
		write("manifest3.properties", "FOLDERS_TO_KEEP=Alpha\n");
		new File("Alpha.zip").delete();
		new File("Beta.zip").delete();
		runTool("-zipFolders-excludeParent-generateHashes", "false", "manifest3.properties");
		check(new File("Alpha.zip").isFile() && !new File("Beta.zip").exists(), "only the kept folder is zipped");
		try (java.util.zip.ZipFile z = new java.util.zip.ZipFile("Alpha.zip")) {
			check(z.getEntry("a.txt") != null && z.getEntry("Alpha/a.txt") == null, "no parent folder inside the zip");
		}
	}
}
