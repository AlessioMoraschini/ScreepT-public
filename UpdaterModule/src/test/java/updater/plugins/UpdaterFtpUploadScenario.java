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

import static updater.plugins.UpdaterToolsScenario.check;
import static updater.plugins.UpdaterToolsScenario.runTool;
import static updater.plugins.UpdaterToolsScenario.write;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import main.MainStandAloneUpdater;
import test.util.gui.AutoDialogCloser;
import updater.ftp.FakeFtpsServer;
import various.common.light.network.ftp.FtpDumper;

/**
 * Run by {@link UpdaterFtpUploadTest} in a JVM of its own with an empty temporary working directory: the upload of the release
 * files by updater.jar to a (fake, local) FTPS server, as {@code -generateHashes-uploadFtpUpdates} does. The dialogs of the tool
 * ("upload completed", "upload failed") are answered by {@link AutoDialogCloser}; the System.exit is blocked.
 * Exit code 0 = every check passed.
 */
public class UpdaterFtpUploadScenario {

	static final String REMOTE = "/ScreepT/Updates";

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
			AutoDialogCloser.start();
			run();
			System.out.println("SCENARIO OK");
			code = 0;
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}
		System.setSecurityManager(null);
		System.exit(code); // not halt(): the coverage agent writes its data in a shutdown hook
	}

	private static void ftpProperties(FakeFtpsServer server, String password) throws Exception {
		write("FTP.properties", "HOST=127.0.0.1\nPORT=" + server.getPort() + "\nUSER=" + FakeFtpsServer.USER + "\nPSW=" + password
				+ "\nFOLDER_ON_SERVER=" + REMOTE + "\nCONNECTION_TIMEOUT_MS=5000\n");
	}

	private static Object uploadDirectly(List<File> files) throws Throwable {
		Method m = MainStandAloneUpdater.class.getDeclaredMethod("mainUploadUpdatesViaFTP", List.class);
		m.setAccessible(true);
		try {
			return m.invoke(null, files);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	static void run() throws Throwable {
		new File("tls").mkdirs();
		FakeFtpsServer server = new FakeFtpsServer(new File("tls"));
		try {
			ftpProperties(server, FakeFtpsServer.PASSWORD);

			// 1) nothing to upload: the server is not even contacted
			uploadDirectly(new ArrayList<File>());
			check(server.commands.isEmpty(), "an empty list does not connect: " + server.commands);

			// 2) the server refuses the files: the tool tells it (dialog answered by the closer) and exits with a failure
			write("one.txt", "first");
			server.dirs.add("/ScreepT");
			server.dirs.add(REMOTE);
			server.rejectStorTimes = 99;
			FtpDumper.MAX_UPLOAD_ATTEMPTS = 1;
			List<File> failing = new ArrayList<File>();
			failing.add(new File("one.txt").getAbsoluteFile());
			try {
				uploadDirectly(failing);
				check(false, "a refused upload must end the tool with exit 1");
			} catch (SecurityException expected) {
				check(expected.getMessage().contains("exit 1"), "exit code of the failure: " + expected.getMessage());
			}
			check(server.files.get(REMOTE + "/one.txt") == null, "nothing stored");
			check(server.count("QUIT") == 1, "the tool disconnects even when the upload fails");
			server.rejectStorTimes = 0;
			FtpDumper.MAX_UPLOAD_ATTEMPTS = 3;
			server.commands.clear();

			// 3) full flow: hashes are generated, the old remote manifest is deleted first and the new one is uploaded last
			write("manifest.properties", "");
			write("updates/ScreepT.jar", "new jar bytes");
			write("updates/Resources/conf.txt", "conf");
			write("notes.txt", "a file next to the zips");
			server.files.put(REMOTE + "/manifest.properties", "old manifest".getBytes("UTF-8"));
			runTool("-generateHashes-uploadFtpUpdates", "true", "manifest.properties");

			check(Files.readAllBytes(new File("manifest.properties").toPath()).length > 0, "manifest generated");
			byte[] remoteManifest = server.files.get(REMOTE + "/manifest.properties");
			check(remoteManifest != null && java.util.Arrays.equals(remoteManifest, Files.readAllBytes(new File("manifest.properties").toPath())),
					"the remote manifest is the new one");
			check(java.util.Arrays.equals(server.files.get(REMOTE + "/updates.zip"), Files.readAllBytes(new File("updates.zip").toPath())), "updates.zip uploaded");
			check(java.util.Arrays.equals(server.files.get(REMOTE + "/notes.txt"), "a file next to the zips".getBytes("UTF-8")), "loose files uploaded");

			List<String> cmds = new ArrayList<String>(server.commands);
			int delete = cmds.indexOf("DELE manifest.properties");
			int firstStor = -1, lastStor = -1;
			for (int i = 0; i < cmds.size(); i++) {
				if (cmds.get(i).startsWith("STOR ")) {
					if (firstStor < 0) {
						firstStor = i;
					}
					lastStor = i;
				}
			}
			check(delete >= 0 && delete < firstStor, "the old manifest is deleted before the first upload: " + cmds);
			check(cmds.get(lastStor).equals("STOR manifest.properties"), "the manifest goes last: " + cmds);
		} finally {
			server.close();
		}
	}
}
