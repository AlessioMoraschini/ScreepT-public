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
package various.common.light.files;

import java.io.File;
import java.util.Comparator;

/**
 * 
 * @author Alessio Moraschini
 * 
 * This class is created to compare files by name using the ordinal number, from 0 to n
 *
 * takes the number from filenames of type "number+separator+asodauuaibibb.extension"
 *
 * if first letter is not a numeric value then it calculates it as 0
 *
 */
public class CustomFileComparator implements Comparator<File> {

	String nameA,nameB;
	String separator;
	int position;
	
	public CustomFileComparator(String separ, int pos) {
		position = pos;
		separator = separ;
	}
	
	@Override
	public int compare(File fileA, File fileB) {
		nameA = fileA.getName();
		nameB = fileB.getName();
		
		String[] a = nameA.split(separator);
		String[] b = nameB.split(separator);
		
		Integer A;
		Integer B;
		
		try {
			A = Integer.valueOf(a[position]);
		}catch(NumberFormatException e) {
			A=0;
		}catch(IndexOutOfBoundsException e) {
			throw new RuntimeException("OutOfBounds, there is not position: "+position+" in the name! Can't compare");
		}
		try {
			B = Integer.valueOf(b[position]);
		}catch(NumberFormatException e) {
			B=0;
		}catch(IndexOutOfBoundsException e) {
			throw new RuntimeException("OutOfBounds, there is not position: "+position+" in the name! Can't compare");
		}
		
		return A.compareTo(B);
	}

}
