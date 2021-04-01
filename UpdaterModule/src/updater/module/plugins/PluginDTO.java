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
package updater.module.plugins;

import java.io.File;

import various.common.light.utility.string.StringWorker;

public class PluginDTO implements Comparable<PluginDTO> {

	public File localExtractedDir;
	public File localFile;
	public String completeURL;
	public String name;
	public String description;
	public String checkSum;
	public boolean installationCompleted;

	public PluginDTO(String completeURL, String name, String checkSum) {
		this.completeURL = completeURL;
		this.name = name;
		this.checkSum = checkSum;
		this.localFile = null;
		this.localExtractedDir = null;
		this.installationCompleted = false;
		this.description = "";
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

	@Override
	public String toString() {
		return "PluginDTO [localExtractedDir=" + localExtractedDir + ", localFile=" + localFile + ", completeURL=" + completeURL + ", name=" + name
				+ ", checkSum=" + checkSum + ", installationCompleted=" + installationCompleted + "]";
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
