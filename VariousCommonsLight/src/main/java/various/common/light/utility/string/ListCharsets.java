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
package various.common.light.utility.string;

import java.nio.charset.Charset;
import java.util.Vector;

/** this class is used to get charset names defined in current system
 * 
 * @author Alessio Moraschini
 *
 */
public class ListCharsets {
	
	/** @return a new list at every call, with each name once */
	public static Vector<String> getCharsets() {
		return new Vector<>(Charset.availableCharsets().keySet());
	}
}
