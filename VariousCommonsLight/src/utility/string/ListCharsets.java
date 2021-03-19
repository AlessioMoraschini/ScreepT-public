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
package utility.string;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.Vector;

/** this class is used to get charset names defined in current system
 * 
 * @author Alessio Moraschini
 *
 */
public class ListCharsets {
	
	private static SortedMap<?, ?> charsets = Charset.availableCharsets();
    private static Set<?> names = charsets.keySet();
    private static Vector<String> nameList= new Vector<>();
    
    public static Vector<String>  getCharsets() {
    	for (Iterator<?> e = names.iterator(); e.hasNext();) {
	      String name = (String) e.next();
	      nameList.add(name);
    	}
    	return nameList;
    }
}
