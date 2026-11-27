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
