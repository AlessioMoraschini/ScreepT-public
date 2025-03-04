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
