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
package various.common.light.utility.hash;

public enum HashingFunction {MD5("MD5"),
							 SHA1("SHA-1"),
							 SHA256("SHA-256"),
							 SHA384("SHA-384"),
							 SHA512("SHA-512");

	public String algorithmCode;
	
	HashingFunction(String algorithm) {
		this.algorithmCode = algorithm;
	}
}
