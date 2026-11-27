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
package os.commons.om;

import java.util.Locale;

public class OSinfo {	
	
	// FIELDS
	
	private String osArch;
	private String osName;
	private String osNameShort;
	private String osVersion;	
	private OSenum OsEnum;
	
	// CONSTRUCTOR
	
	public OSinfo() {
		osArch = System.getProperty("os.arch");
		osNameShort = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		osName = System.getProperty("os.name");
		osVersion = System.getProperty("os.version");
		
		OsEnum = initOsEnum(osNameShort);
	}
	
	// public utils
	
	public boolean isWindows() {
		boolean win = (this.OsEnum.equals(OSenum.Windows)) ? true : false;
		return win;
	}
	
	public boolean isLinux() {
		boolean linux = (this.OsEnum.equals(OSenum.Linux)) ? true : false;
		return linux;
	}
	
	public boolean isMac() {
		boolean mac = (this.OsEnum.equals(OSenum.MacOS)) ? true : false;
		return mac;
	}
	
	public boolean isOther() {
		boolean other = (this.OsEnum.equals(OSenum.OTHER)) ? true : false;
		return other;
	}
	// LOCAL PRIVATE UTILS
	
	private OSenum initOsEnum(String OS) {
		
		if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
			return OSenum.MacOS;
		} else if (OS.indexOf("win") >= 0) {
			return OSenum.Windows;
		} else if (OS.indexOf("nux") >= 0) {
			return OSenum.Linux;
		} else {
			return OSenum.OTHER;
		}
	}

	// GETTERS AND SETTERS
	
	public String getOsArch() {
		return osArch;
	}

	public String getOsName() {
		return osName;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public String getOsNameShort() {
		return osNameShort;
	}

	public OSenum getOsEnum() {
		return OsEnum;
	}
	
}
