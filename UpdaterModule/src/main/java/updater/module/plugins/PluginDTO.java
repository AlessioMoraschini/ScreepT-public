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
package updater.module.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import various.common.light.utility.properties.PropertiesManager;
import various.common.light.utility.string.StringWorker;

public class PluginDTO implements Comparable<PluginDTO> {

	public static final String DEFAULT_VERSION = "1.0.0";
	public static final String EMPTY_VERSION = "";

	public File localExtractedDir;
	public File localFile;
	public String completeURL;
	public String name;
	public String description;
	public String checkSum;
	public List<String> warnings;
	public String lastVersion;
	/** Oldest ScreepT this plugin works with (from the manifest), empty when it declares none */
	public String minAppVersion = "";
	public String installedVersion;
	public boolean installationCompleted;

	public PluginDTO(String version, String completeURL, String name, String checkSum) {
		this.completeURL = completeURL;
		this.name = name;
		this.checkSum = checkSum;
		this.localFile = null;
		this.localExtractedDir = null;
		this.installationCompleted = false;
		this.description = "";
		this.lastVersion = version == null ? DEFAULT_VERSION : version;
		this.warnings = new ArrayList<>();
		installedVersion = getInstalledVersion();
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}
	public String getMinAppVersion() {
		return minAppVersion == null ? "" : minAppVersion;
	}

	public void setMinAppVersion(String minAppVersion) {
		this.minAppVersion = minAppVersion == null ? "" : minAppVersion.trim();
	}

	/** Why this plugin does not suit the running ScreepT (it asks for a newer one), null when it does or nothing is known */
	public String getCompatibilityProblem() {
		return PluginManager.compatibilityProblem(this);
	}

	public String getLastVersion() {
		return lastVersion;
	}
	public void setLastVersion(String version) {
		this.lastVersion = version;
	}


	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCompleteURL() {
		return completeURL;
	}

	public void setCompleteURL(String completeURL) {
		this.completeURL = completeURL;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(String checkSum) {
		this.checkSum = checkSum;
	}

	public File getLocalFile() {
		return localFile;
	}

	public void setLocalFile(File localFile) {
		this.localFile = localFile;
	}

	public File getLocalExtractedDir() {
		return localExtractedDir;
	}

	public void setLocalExtractedDir(File localExtractedDir) {
		this.localExtractedDir = localExtractedDir;
	}

	public boolean isInstallationCompleted() {
		return installationCompleted;
	}

	public void setInstallationCompleted(boolean installationCompleted) {
		this.installationCompleted = installationCompleted;
	}

	public String enrichWithInstalledVersion(String filePropsPath) {
		setInstalledVersion(new PropertiesManager(filePropsPath).getProperty(PluginManager.PLUGIN_INSTALLED_VERSION_PROP, EMPTY_VERSION));
		return installedVersion;
	}

	public String getInstalledVersion() {
		return installationCompleted
				? EMPTY_VERSION.equals(installedVersion)
						? DEFAULT_VERSION
						: installedVersion
				: EMPTY_VERSION;
	}

	public void setInstalledVersion(String installedVersion) {
		this.installedVersion = installedVersion;
	}

	@Override
	public String toString() {
		return "PluginDTO [localExtractedDir=" + localExtractedDir + ", localFile=" + localFile + ", completeURL=" + completeURL + ", name=" + name
				+ ", checkSum=" + checkSum + ", installationCompleted=" + installationCompleted + "]";
	}

	public boolean isUpToDate() {
		return compareVersionWithLast(installedVersion) >= 0;
	}

	public int compareVersionWithInstalled(String version) {
		return StringWorker.compareVersions(version, getInstalledVersion());
	}

	public int compareVersionWithLast(String version) {
		try {
			return StringWorker.compareVersions(version, this.lastVersion);
		} catch (IllegalArgumentException e) {
			return - 1;
		}
	}

	@Override
	public int compareTo(PluginDTO o) {
		if(o == null)
			return -1;

		if(!StringWorker.isEmpty(getName()) && StringWorker.isEmpty(o.getName()))
			return -1;

		if(StringWorker.isEmpty(getName()) && StringWorker.isEmpty(o.getName()))
			return 0;

		if(StringWorker.isEmpty(getName()) && !StringWorker.isEmpty(o.getName()))
			return 1;

		return getName().compareTo(o.getName());
	}

}
