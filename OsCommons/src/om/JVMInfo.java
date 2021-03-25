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
package om;

import java.util.ArrayList;

public class JVMInfo {	
	
	private String javaHome;
	private String javaCurrVersion;
	private ArrayList<String> javaVersionsFound;
	
	public JVMInfo() {
		
	}

	public String getJavaHome() {
		return javaHome;
	}

	public String getJavaCurrVersion() {
		return javaCurrVersion;
	}
	
	public ArrayList<String> getJavaVersionsFound() {
		return javaVersionsFound;
	}

}
