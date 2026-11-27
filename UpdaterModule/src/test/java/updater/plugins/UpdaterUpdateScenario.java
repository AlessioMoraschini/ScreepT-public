/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package updater.plugins;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import main.MainStandAloneUpdater;
import main.om.PropertiesDTO;

/**
 * Run by {@link UpdaterUpdateTest} in a JVM of its own: the update of the application as the launcher starts it
 * (<i>updater.jar updater.jar UPDATES/updates ScreepT_new.jar</i>), with the working directory = the Application folder.
 * The updater always ends with System.exit, so the checks are made by the test on the files it leaves.
 * Argument: ok | badhash | missing.
 */
public class UpdaterUpdateScenario {

	static void write(String path, String content) throws Exception {
		File f = new File(path);
		if (f.getParentFile() != null)
			f.getParentFile().mkdirs();
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}

	public static void main(String[] args) throws Exception {
		DialogAutoAnswer.configure(args.length > 1 ? args[1] : null, args.length > 2 ? args[2] : null);
		DialogAutoAnswer.start();
		String mode = args[0];
		if ("fail".equals(mode)) {
			// a tool command without its arguments: the updater reports the failure and ends with exit code 1
			MainStandAloneUpdater.main(new String[] { "-generateHashes" });
			return;
		}
		write("resourcesFilePathsMappings.properties", PropertiesDTO.DEF_PROXY_CONFIG_KEY + "=proxy.example:8080\nOTHER=old\n");
		if (!"noini".equals(mode))
			write("LAUNCHER/Launcher_Configuration.ini", "[APPLICATION_OPTIONS]\njarToLaunch=ScreepT_old.jar\n");
		write("ScreepT_old.jar", "old");
		write("keep_me.txt", "personal file");
		write("updater.jar", "updater");
		if (!"missing".equals(mode)) {
			write("UPDATES/updates.zip", "zipped updates");
			// the manifest carries the hashes of updater.jar and updates.zip: the first one wrong on purpose in "badhash"
			String jarHash = "badhash".equals(mode) ? "00" : PluginManagerScenario.sha512(new File("updater.jar"));
			write("UPDATES/updates/manifest.properties", "updater.jar=" + jarHash + "\nupdates.zip=" + PluginManagerScenario.sha512(new File("UPDATES/updates.zip")) + "\n");
			write("UPDATES/updates/ScreepT_new.jar", "new");
			write("UPDATES/updates/resourcesFilePathsMappings.properties", PropertiesDTO.DEF_PROXY_CONFIG_KEY + "=\nOTHER=new\n");
			write("UPDATES/updates/lib/dep.jar", "dep");
			write("UPDATES/updates/ROOT_DIR_APP_FILES/license.txt", "license");
		}
		MainStandAloneUpdater.main(new String[] { "updater.jar", "UPDATES/updates", "ScreepT_new.jar" });
		System.out.println("RETURNED"); // never: the updater always exits
	}
}
