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
package various.common.light.om;

public enum EscapeType {HTML_3,
						HTML_4,
						ECMASCRIPT,
						JSON,
						JAVA,
						XML,
						XML_10,
						XML_11;

	private EscapeType() {
	}
	
	public static String[] getTypesString() {
		String[] stringArray = new String[EscapeType.values().length];
		int i = 0;
		for(EscapeType  currType : EscapeType.values()) {
			stringArray[i] = currType.name();
			i++;
		}
		
		return stringArray;
	}
}
