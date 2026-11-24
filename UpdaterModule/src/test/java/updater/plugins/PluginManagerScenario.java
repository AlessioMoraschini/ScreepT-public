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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import updater.module.plugins.PluginDTO;
import updater.module.plugins.PluginManager;
import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;

/**
 * Run by {@link PluginManagerLifecycleTest} in a JVM of its own, whose working directory is an empty temporary folder:
 * the plugin manager takes the application folder, the plugins folder and the server address from the working directory
 * and from <i>resourcesFilePathsMappings.properties</i> when its class is loaded, so one JVM can run only one setup.
 * Plays the whole life of the plugins against a local web server: discovery from the manifest, integrity check,
 * installation, shared files, removal, update and the lists the launcher reads at the next start.
 * Exit code 0 = every check passed.
 */
public class PluginManagerScenario {

	static final Map<String, byte[]> web = new ConcurrentHashMap<String, byte[]>();
	static String base;

	public static void main(String[] args) {
		int code = 1;
		try {
			run();
			System.out.println("SCENARIO OK");
			code = 0;
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}
		System.exit(code);
	}

	static void check(boolean ok, String what) {
		if (!ok)
			throw new AssertionError(what);
	}

	static byte[] zip(String... namesAndContents) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(bos);
		for (int i = 0; i < namesAndContents.length; i += 2) {
			zos.putNextEntry(new ZipEntry(namesAndContents[i]));
			zos.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
		}
		zos.close();
		return bos.toByteArray();
	}

	static String sha512(File f) throws Exception {
		return ChecksumHasher.getFileHashBuffered(f, HashingFunction.SHA512, true);
	}

	static void publish(String alphaVersion, String alphaHash, String betaHash) {
		String m = "AvailablePluginsList=Alpha.zip,Beta.zip\n" + "Alpha.zip=" + alphaHash + "\n" + "Beta.zip=" + betaHash + "\n"
				+ "PLUGIN_VERSION_Alpha.zip=" + alphaVersion + "\n" + "PLUGIN_VERSION_Beta.zip=1.0.0\n"
				+ "DESCREEPTION_Alpha.zip=The first test plugin\n"
				+ "WARNING_MESSAGES_Alpha.zip=careful#_#really careful\n";
		web.put("/Plugins/manifest.properties", m.getBytes(StandardCharsets.UTF_8));
	}

	static File save(String name, byte[] bytes) throws IOException {
		File f = new File("PLUGINS", name);
		f.getParentFile().mkdirs();
		Files.write(f.toPath(), bytes);
		return f;
	}

	static String installedProps() throws IOException {
		return new String(Files.readAllBytes(new File("Installed_plugins.properties").toPath()), StandardCharsets.UTF_8);
	}

	static void run() throws Throwable {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", new HttpHandler() {
			@Override
			public void handle(HttpExchange ex) throws IOException {
				byte[] body = web.get(ex.getRequestURI().getPath());
				if (body == null) {
					ex.sendResponseHeaders(404, -1);
				} else if ("HEAD".equals(ex.getRequestMethod())) {
					ex.getResponseHeaders().set("Content-Length", String.valueOf(body.length));
					ex.sendResponseHeaders(200, -1);
				} else {
					ex.sendResponseHeaders(200, body.length);
					ex.getResponseBody().write(body);
				}
				ex.close();
			}
		});
		server.start();
		base = "http://127.0.0.1:" + server.getAddress().getPort() + "/Plugins/";
		// read by the plugin manager when its class is loaded
		Files.write(new File("resourcesFilePathsMappings.properties").toPath(),
				("PLUGINS_DIR=PLUGINS/\nWEB_PLUGINS_BASE_URL=" + base + "\nWEB_PLUGINS_MANIFEST_URL=" + base + "manifest.properties\n").getBytes(StandardCharsets.UTF_8));

		// the application always ships this file
		Files.write(new File("Installed_plugins.properties").toPath(), "#Installed plugins\nINSTALLED_LIST=\n".getBytes(StandardCharsets.UTF_8));

		byte[] alphaZip = zip("EXTERNAL_PLUGINS/alpha.txt", "alpha v1", "shared/common.txt", "used by both");
		byte[] betaZip = zip("EXTERNAL_PLUGINS/beta.txt", "beta", "shared/common.txt", "used by both");
		web.put("/Plugins/Alpha.zip", alphaZip);
		web.put("/Plugins/Beta.zip", betaZip);
		File alphaLocal = save("Alpha.zip", alphaZip);
		File betaLocal = save("Beta.zip", betaZip);
		String alphaHash = sha512(alphaLocal), betaHash = sha512(betaLocal);
		publish("1.0.0", alphaHash, betaHash);

		File root = new File(System.getProperty("user.dir"));
		check(PluginManager.APPLICATION_ROOT_DIR.equals(root), "the application folder is the working directory");
		check(PluginManager.WEB_PLUGINS_MANIFEST_URL.startsWith("http://127.0.0.1"), "the server address comes from the properties file: " + PluginManager.WEB_PLUGINS_MANIFEST_URL);

		PluginManager pm = new PluginManager();
		check(new File("PLUGINS").isDirectory(), "plugins folder created");
		check(PluginManager.getInstalledPluginsList().isEmpty(), "nothing installed at the start");

		// --- discovery
		ArrayList<PluginDTO> found = pm.discoverLatestPlugins(false);
		check(found.size() == 2, "two plugins in the manifest: " + found);
		PluginDTO alpha = found.get(0), beta = found.get(1);
		check("Alpha.zip".equals(alpha.getName()) && "1.0.0".equals(alpha.getLastVersion()), "alpha name/version " + alpha);
		check("The first test plugin".equals(alpha.getDescription()), "description");
		check(alpha.getWarnings().size() == 2 && alpha.getWarnings().get(1).equals("really careful"), "warnings split by the separator " + alpha.getWarnings());
		check("Description not available.".equals(beta.getDescription()), "default description");
		check((base + "Alpha.zip").equals(alpha.getCompleteURL()), "url " + alpha.getCompleteURL());
		check(!alpha.isInstallationCompleted(), "not installed yet");
		check(pm.retrieveFromCache("Alpha.zip") == alpha && pm.retrieveAllFromCache().size() == 2, "found plugins are cached");
		check(pm.retrieveFromCacheByNameNoExtension("Beta") == beta, "cache lookup by name without extension");
		check(pm.retrieveFromCache("Gamma.zip") == null, "unknown plugin");
		check(!pm.isInstalled(alpha) && !pm.isInstalled(null), "isInstalled");
		check(pm.findUpdatablePlugins(false).isEmpty(), "nothing updatable before installing");

		// --- integrity: wrong hash is refused, right one accepted
		alpha.setLocalFile(alphaLocal);
		alpha.setCheckSum("00");
		check(!pm.checkPluginHash(alpha), "a wrong hash is detected");
		boolean refused = false;
		try {
			pm.installPlugin(alpha, false);
		} catch (Throwable t) {
			refused = true;
		}
		check(refused && !new File("EXTERNAL_PLUGINS/alpha.txt").exists(), "nothing installed when the hash does not match");
		check(pm.installPlugin(null, false) == null, "null plugin");
		alpha.setCheckSum(alphaHash);
		check(pm.checkPluginHash(alpha) && !pm.checkPluginHash(null), "the right hash is accepted");

		// --- installation
		pm.installPlugin(alpha, false);
		check(alpha.isInstallationCompleted() && pm.isInstalled(alpha), "alpha installed");
		check(new File("EXTERNAL_PLUGINS/alpha.txt").isFile() && new File("shared/common.txt").isFile(), "files copied into the application folder");
		check(PluginManager.getInstalledPluginsList().contains("Alpha.zip"), "alpha in the installed list");
		check(installedProps().contains("alpha.txt"), "the installed files are written to the properties file");
		check(pm.getAllInstalledFiles().get("Alpha.zip").size() == 2, "installed files map " + pm.getAllInstalledFiles());

		beta.setLocalFile(betaLocal);
		beta.setCheckSum(betaHash);
		pm.installPlugin(beta, false);
		check(pm.getAllInstalledFiles().size() == 2, "two plugins installed");
		List<File> others = pm.getAllInstalledFilesExceptPlugin(alpha);
		check(others.size() == 2, "files of the others " + others);
		check(pm.getAllInstalledFilesMapExceptPlugin(alpha).keySet().equals(new java.util.HashSet<String>(Arrays.asList("Beta.zip"))), "map without alpha");
		ArrayList<String> filtered = pm.filterSharedFiles(new ArrayList<String>(Arrays.asList("EXTERNAL_PLUGINS/alpha.txt", "shared/common.txt")), alpha);
		check(filtered.size() == 1 && filtered.get(0).endsWith("alpha.txt"), "a file also used by beta is not removed with alpha " + filtered);

		// --- a newer alpha on the server -> updatable; the update removes the old files and asks the launcher to install the new one
		publish("2.0.0", alphaHash, betaHash);
		ArrayList<PluginDTO> updatable = pm.findUpdatablePlugins(false);
		check(updatable.size() == 1 && "Alpha.zip".equals(updatable.get(0).getName()), "alpha is updatable: " + updatable);
		check(pm.updateAll(updatable) == 1, "one update prepared");
		check(!new File("EXTERNAL_PLUGINS/alpha.txt").exists(), "old files removed by the update");
		check(new File("shared/common.txt").exists(), "the shared file survives");
		check(pm.getPendingPluginUpdates().contains("Alpha.zip"), "update pending for the next start " + pm.getPendingPluginUpdates());
		check(!pm.isInstalled(alpha), "alpha not installed any more");

		// --- pending lists
		pm.updatePendingDelete(new ArrayList<String>(Arrays.asList("old/a.txt", "old/b.txt")));
		pm.updatePendingDelete(new ArrayList<String>(Arrays.asList("old/a.txt")));
		pm.updatePendingDelete(new ArrayList<String>());
		String props = installedProps();
		check(props.contains("PLUGINS_PENDING_DELETE_AT_START") && props.contains("old/a.txt;old/b.txt"), "pending delete list without duplicates: " + props);
		pm.removeInstalledFilesFromPendingDelete(new ArrayList<String>(Arrays.asList("old/a.txt")));
		props = installedProps();
		check(!props.contains("old/a.txt") && props.contains("old/b.txt"), "what was installed again is no longer deleted at the next start: " + props);
		pm.removeInstalledFilesFromPendingDelete(new ArrayList<String>(Arrays.asList("old/b.txt")));
		pm.updatePendingPluginUpdate("Gamma.zip");
		pm.updatePendingPluginUpdate("Gamma.zip");
		check(pm.getPendingPluginUpdates().contains("Gamma.zip"), "pending update added");
		pm.removeFromPendingPluginUpdate(Arrays.asList("Gamma.zip"));
		pm.removeAllFromPendingPluginUpdate();
		check(pm.getPendingPluginUpdates().isEmpty(), "pending updates cleared");

		// --- install the new alpha again, then remove everything
		alpha.setLocalFile(alphaLocal);
		pm.installPlugin(alpha, false);
		check(pm.isInstalled(alpha), "alpha installed again");
		check(pm.removePlugin(alpha, false) && !new File("EXTERNAL_PLUGINS/alpha.txt").exists(), "alpha removed");
		check(pm.removePlugin(beta, false) && !new File("shared/common.txt").exists(), "the last user of the shared file removes it");
		check(PluginManager.getInstalledPluginsList().isEmpty(), "nothing installed at the end");
		check(!pm.removePlugin(null, false) && !pm.removePlugin(new PluginDTO("1", "u", null, "c"), false), "invalid plugin");

		// --- cache
		check(pm.getAllFromCache(false).size() == 2, "cache listing");
		check(pm.getValidFromCache(null) == null && pm.getValidFromCache(" ") == null, "no name, no plugin");
		check(pm.removeFromCache(beta) == beta && pm.removeFromCache((PluginDTO) null) == null, "remove from cache");
		check(pm.updateCache((PluginDTO) null) == null, "update null");
		ArrayList<PluginDTO> two = new ArrayList<PluginDTO>(Arrays.asList(alpha, beta));
		check(pm.updateCache(two).size() == 2 && pm.removeFromCache(two).size() == 2, "update/remove lists");
		pm.refreshCache("Alpha");
		check(pm.retrieveFromCache("Alpha.zip") != null, "refresh of one plugin");
		pm.refreshCache();
		check(pm.retrieveAllFromCache().size() == 2, "refresh of all");
		pm.clearCache();
		check(pm.retrieveAllFromCache().isEmpty(), "cache cleared");
		check(pm.clearPluginsLocalDir() && new File("PLUGINS").list().length == 0, "plugins folder emptied");

		// --- download from the server, then install what was downloaded
		publish("2.0.0", alphaHash, betaHash);
		ArrayList<PluginDTO> fresh = pm.discoverLatestPlugins(false);
		PluginDTO a2 = fresh.get(0), b2 = fresh.get(1);
		check(pm.downloadPlugin(null) == null && pm.downloadPlugins(new ArrayList<PluginDTO>()).isEmpty(), "nothing to download");
		check(pm.downloadPlugin(a2) == a2 && a2.getLocalFile().isFile(), "alpha downloaded to " + a2.getLocalFile());
		check(a2.getLocalFile().length() == alphaZip.length && pm.checkPluginHash(a2), "downloaded bytes match the manifest hash");
		check(pm.retrieveFromCache("Alpha.zip") == a2 && pm.getValidFromCache("Alpha.zip") == a2, "downloaded plugin is cached and valid");
		check(pm.downloadPlugins(new ArrayList<PluginDTO>(Arrays.asList(a2, b2))).size() == 2, "download of a list");
		pm.installPlugin(a2, false);
		check(pm.isInstalled(a2) && new File("EXTERNAL_PLUGINS/alpha.txt").isFile(), "downloaded plugin installs");
		check(pm.removePlugin(a2, true) && pm.getPendingPluginUpdates().contains("Alpha.zip"), "removal for an update is marked pending");
		pm.removeAllFromPendingPluginUpdate();
		web.remove("/Plugins/Beta.zip");
		boolean failed = false;
		try {
			pm.downloadPlugin(b2);
		} catch (Throwable t) {
			failed = true;
		}
		check(failed && pm.retrieveFromCache("Beta.zip") == null, "a missing file fails the download and leaves the cache");

		// --- the server stops publishing the manifest
		web.remove("/Plugins/manifest.properties");
		check(pm.discoverLatestPlugins(false).isEmpty(), "no manifest, no plugins");
		check(pm.updateAll(new ArrayList<PluginDTO>()) == 0, "nothing to update");
		server.stop(0);
	}
}
