package updater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import updater.module.exception.UpdatesUnavailableException;
import various.common.light.network.utils.DownloadUtils;
import various.common.light.utility.properties.PropertiesManager;

/**
 * The update client against a real web server (local, serving what ScreepT's web space serves: a manifest, the updater
 * jar and the zip of the updates). What a user lives through: finding out that there is a newer version, downloading it,
 * and the protections: unreachable server, a manifest that changes while downloading, an empty updates folder.
 */
public class UpdatesManagerTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private HttpServer web;
	private final Map<String, byte[]> files = new ConcurrentHashMap<String, byte[]>();
	private final List<String> requests = new CopyOnWriteArrayList<String>();
	private volatile boolean manifestDrifts;
	private volatile boolean driftAfterZip;

	private String savedUpdatesDir;
	private String savedTempDir;
	private String savedJarName;

	private String base() {
		return "http://127.0.0.1:" + web.getAddress().getPort();
	}

	@Before
	public void startServerAndRedirectTheClientToTemporaryFolders() throws Exception {
		PropertiesManager.setLoggerEnabled(false);
		web = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		web.createContext("/", new HttpHandler() {
			@Override
			public void handle(HttpExchange ex) throws IOException {
				String path = ex.getRequestURI().getPath();
				requests.add(ex.getRequestMethod() + " " + path);
				byte[] body = files.get(path);
				if (body == null) {
					ex.sendResponseHeaders(404, -1);
				} else {
					if (path.endsWith("manifest.properties") && manifestDrifts) {
						// a new release was published meanwhile: the last line wins in a properties file
						body = (new String(body, StandardCharsets.UTF_8) + UpdatesManager.WEB_UPDATER_VERSION_PROP + "=9.9.9\n").getBytes(StandardCharsets.UTF_8);
					}
					if ("HEAD".equals(ex.getRequestMethod())) {
						ex.getResponseHeaders().set("Content-Length", String.valueOf(body.length));
						ex.sendResponseHeaders(200, -1);
					} else {
						ex.sendResponseHeaders(200, body.length);
						ex.getResponseBody().write(body);
						if (path.endsWith("updates.zip") && driftAfterZip) {
							manifestDrifts = true;
						}
					}
				}
				ex.close();
			}
		});
		web.start();

		savedUpdatesDir = UpdatesManager.UPDATES_DIR;
		savedTempDir = UpdatesManager.TEMP_DIR;
		savedJarName = UpdatesManager.WEB_UPDATER_JAR_NAME;
		File updates = new File(tmp.getRoot(), "UPDATES");
		UpdatesManager.UPDATES_DIR = updates.getAbsolutePath().replace('\\', '/') + "/";
		// as in the application: the folder for temporary files is not the one of the updates
		File temp = new File(tmp.getRoot(), "temp");
		assertTrue(temp.mkdirs());
		UpdatesManager.TEMP_DIR = temp.getAbsolutePath().replace('\\', '/') + "/";
		UpdatesManager.WEB_UPDATER_JAR_NAME = new File(tmp.getRoot(), "updater.jar").getAbsolutePath();
	}

	@After
	public void stopServerAndRestoreTheClient() {
		web.stop(0);
		UpdatesManager.UPDATES_DIR = savedUpdatesDir;
		UpdatesManager.TEMP_DIR = savedTempDir;
		UpdatesManager.WEB_UPDATER_JAR_NAME = savedJarName;
	}

	private void publish(String version) throws Exception {
		String manifest = UpdatesManager.WEB_UPDATER_VERSION_PROP + "=" + version + "\n"
				+ UpdatesManager.WEB_UPDATER_JAR_PROP + "=" + base() + "/Updates/updater.jar\n"
				+ UpdatesManager.WEB_UPDATES_PACK_PROP + "=" + base() + "/Updates/updates.zip\n";
		files.put("/Updates/manifest.properties", manifest.getBytes(StandardCharsets.UTF_8));
		files.put("/Updates/updater.jar", "pretend jar bytes".getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
			zip.putNextEntry(new ZipEntry("ScreepT_" + version + ".jar"));
			zip.write(("application " + version).getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
			zip.putNextEntry(new ZipEntry("Resources_ScreepT/readme.txt"));
			zip.write("hello".getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		files.put("/Updates/updates.zip", zipBytes.toByteArray());
	}

	private UpdatesManager manager(String currentVersion) throws Exception {
		return new UpdatesManager(new URL(base() + "/Updates/manifest.properties"), currentVersion);
	}

	/**
	 * The download shows progress windows: if one ever stops the run, the test fails after 30 s with the stacks of the
	 * threads involved instead of silently holding the whole suite.
	 */
	private static File[] download(final UpdatesManager m) throws Exception {
		final Object[] result = new Object[2];
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					result[0] = m.downloadAndExtractUpdates();
				} catch (Throwable e) {
					result[1] = e;
				}
			}
		}, "download-under-test");
		t.setDaemon(true);
		t.start();
		t.join(30_000);
		if (t.isAlive()) {
			StringBuilder dump = new StringBuilder();
			for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
				if (e.getKey() == t || e.getKey().getName().startsWith("AWT-EventQueue")) {
					dump.append("\n--- ").append(e.getKey().getName()).append('\n');
					for (StackTraceElement frame : e.getValue()) {
						dump.append("    at ").append(frame).append('\n');
					}
				}
			}
			t.interrupt();
			fail("the download did not finish in 30 s" + dump);
		}
		if (result[1] instanceof Exception) {
			throw (Exception) result[1];
		}
		if (result[1] instanceof Error) {
			throw (Error) result[1];
		}
		return (File[]) result[0];
	}

	private static File find(File dir, String name) throws IOException {
		java.util.Iterator<java.nio.file.Path> it = Files.walk(dir.toPath()).filter(p -> p.getFileName().toString().equals(name)).iterator();
		assertTrue(name + " is somewhere under " + dir, it.hasNext());
		return it.next().toFile();
	}

	// ------------------------------------------------------------------ is there something new

	@Test
	public void versionComparisonIsNumericPartByPart() {
		assertTrue(DownloadUtils.compareNewVersion("0.9.92", "0.9.91"));
		assertFalse(DownloadUtils.compareNewVersion("0.9.91", "0.9.91"));
		assertFalse(DownloadUtils.compareNewVersion("0.9.90", "0.9.91"));
		assertTrue("10 is more than 9, it is not text order", DownloadUtils.compareNewVersion("0.10.0", "0.9.99"));
		assertFalse(DownloadUtils.compareNewVersion("0.9.9", "0.9.10"));
		assertTrue(DownloadUtils.compareNewVersion("1.0.0", "0.9.99"));
		assertTrue("letters around the number are ignored", DownloadUtils.compareNewVersion("V0.9.92-beta", "0.9.91"));
		assertFalse("unreadable versions never trigger an update", DownloadUtils.compareNewVersion("", "0.9.91"));
	}

	@Test
	public void newerVersionOnTheServerIsReported() throws Exception {
		publish("0.9.95");
		assertEquals("0.9.95", manager("0.9.91").lookForUpdates(true));
	}

	@Test
	public void sameOrOlderVersionOnTheServerIsNotAnUpdate() throws Exception {
		publish("0.9.91");
		assertNull(manager("0.9.91").lookForUpdates(true));
		assertNull(manager("0.9.99").lookForUpdates(true));
	}

	@Test
	public void unreachableUpdatesAreReportedAsSuchNotAsNoNews() throws Exception {
		UpdatesManager m = manager("0.9.91");
		assertFalse(m.udpdatesReachable());
		try {
			m.lookForUpdates(true);
			fail("a missing manifest must not look like 'no updates'");
		} catch (UpdatesUnavailableException expected) {
			assertTrue(expected.getMessage().contains("Could not retrieve updates"));
		}
		publish("0.9.95");
		assertTrue(m.udpdatesReachable());
	}

	@Test
	public void deadServerIsAnErrorToo() throws Exception {
		UpdatesManager m = manager("0.9.91");
		web.stop(0);
		try {
			m.lookForUpdates(true);
			fail("connection refused must surface");
		} catch (Exception expected) {
			// any exception: the caller tells the user the check failed
		}
	}

	// ------------------------------------------------------------------ downloading

	@Test
	public void downloadBringsUpdaterJarAndUnpackedUpdates() throws Exception {
		publish("0.9.95");
		UpdatesManager m = manager("0.9.91");
		File[] got = download(m);
		assertNotNull(got);
		assertEquals("updater jar, unpacked updates, the new application jar", 3, got.length);
		assertEquals("pretend jar bytes", new String(Files.readAllBytes(got[0].toPath()), StandardCharsets.UTF_8));
		assertTrue("the zip was extracted: " + got[1], got[1].exists());
		File updatesDir = new File(UpdatesManager.UPDATES_DIR);
		assertEquals("application 0.9.95", new String(Files.readAllBytes(find(updatesDir, "ScreepT_0.9.95.jar").toPath()), StandardCharsets.UTF_8));
		assertEquals("hello", new String(Files.readAllBytes(find(updatesDir, "readme.txt").toPath()), StandardCharsets.UTF_8));
		assertEquals("folders of the zip are kept", "Resources_ScreepT", find(updatesDir, "readme.txt").getParentFile().getName());
		assertEquals("ScreepT_0.9.95.jar", got[2].getName());
		assertNotNull("what was downloaded is what the installer will ask for", m.getLatestUpdates());
		assertTrue(requests.contains("GET /Updates/updates.zip"));
	}

	@Test
	public void manifestThatChangesDuringTheDownloadAbortsAndCleansUp() throws Exception {
		publish("0.9.95");
		UpdatesManager m = manager("0.9.91");
		// the server publishes a new release right after the zip was served: what was downloaded may be a mix
		driftAfterZip = true;
		try {
			download(m);
			fail("a manifest that changed must stop the update");
		} catch (UpdatesUnavailableException expected) {
			// ok: nothing half downloaded may stay for the installer
		}
		assertNull(m.getLatestUpdates());
		assertFalse(m.updatesReadyToInstall.get());
		File[] left = new File(UpdatesManager.UPDATES_DIR).listFiles();
		assertTrue("the updates folder was emptied", left == null || left.length == 0);
	}

	@Test
	public void nothingDownloadedMeansNothingToInstall() throws Exception {
		UpdatesManager m = manager("0.9.91");
		assertNull(m.getLatestUpdates());
		try {
			download(m);
			fail("no manifest, no update");
		} catch (UpdatesUnavailableException expected) {
			// ok
		}
		assertNull(m.getLatestUpdates());
	}

	@Test
	public void emptyingTheUpdatesFolderRemovesOldDownloads() throws Exception {
		UpdatesManager m = manager("0.9.91");
		File dir = new File(UpdatesManager.UPDATES_DIR);
		assertTrue("the constructor prepares the folder", dir.isDirectory());
		Files.write(new File(dir, "old.jar").toPath(), new byte[] { 1, 2, 3 });
		new File(dir, "sub").mkdirs();
		Files.write(new File(dir, "sub/deep.txt").toPath(), new byte[] { 4 });
		assertTrue(m.emptyUpdatesDir());
		String[] left = dir.list();
		assertTrue(left == null || left.length == 0);
	}
}
