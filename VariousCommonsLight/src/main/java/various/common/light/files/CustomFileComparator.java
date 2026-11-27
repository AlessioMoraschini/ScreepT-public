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
