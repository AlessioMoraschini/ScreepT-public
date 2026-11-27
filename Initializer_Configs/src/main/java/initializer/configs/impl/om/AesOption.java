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
package initializer.configs.impl.om;

import various.common.light.om.SecurityLevel;

public class AesOption {

	// DEFAULT VALUES (usfeul to keep valid object during iniLoad, in case of null read or exception)
	public final static boolean DEFAULT_FLAG_AES = true;
	public final static boolean DEF_FLAG_HASHKEY = true;
	
	public final static SecurityLevel DEF_securityLevel = SecurityLevel.MID;
	
	
	// FIELDS	
	private boolean flagAes;  // flag che indica se � attiva la fase di codifica/decodifica AES
	private boolean hashKey;
	private SecurityLevel securityLevel;
	
	// constructors //
	
	public AesOption() {
		flagAes = DEFAULT_FLAG_AES;
		hashKey = DEF_FLAG_HASHKEY;
		securityLevel = DEF_securityLevel;
	}
	
	public AesOption(boolean AES) {
		this();
		flagAes = AES;
	}
	
	// getters and setters //
	
	public boolean isHashKey() {
		return hashKey;
	}

	public void setHashKey(boolean hashKey) {
		this.hashKey = hashKey;
	}

	public boolean isFlagAes() {
		return flagAes;
	}

	public void setFlagAes(boolean flagAes) {
		this.flagAes = flagAes;
	}

	public SecurityLevel getSecurityLevel() {
		return securityLevel;
	}

	public void setSecurityLevel(SecurityLevel securityLevel) {
		this.securityLevel = securityLevel;
	}

}
