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
package various.common.light.files.search;

import java.io.File;
import java.util.ArrayList;

/**
 * 
 * @author Alessio Moraschini
 * 
 * this class give utils method to search for a file, folder or list of all occurrencies,
 * knowin the source root directory, and the String name to search in the name for a match.
 *
 */
public class FileNameSearchLight {

	/**
	 * This method search for a file (only the first found) that is not a directory,
	 * and that contains in the name the given string. It searches also in subdirectories if specified.
	 * @param rootDir
	 * @param namePart
	 * @return the first found that matches, or null elsewhere.
	 */
	public static File searchFileInInRootDir(File rootDir, String namePart, boolean subdirectories) {
		
		File[] foundList = rootDir.listFiles();
		
		for (File foundLevelN : foundList) {
			if (foundLevelN.isFile() && foundLevelN.getName().indexOf(namePart) > -1) {
				return foundLevelN;
			}else if(foundLevelN.isDirectory() && subdirectories){
				return searchFileInInRootDir(foundLevelN, namePart, subdirectories);
			}
		}
		
		return null;
	}
	
	/**
	 * This method search for a directory (only the first found) that that contains in the name the given string.
	 * It searches also in subdirectories if specified.
	 * @param rootDir
	 * @param namePart
	 * @param subdirectories if this is true then search also in subdirectories
	 * @return the first found that matches, or null elsewhere.
	 */
	public static File searchFolderInRootDir(File rootDir, String namePart, boolean subdirectories) {
		
		File[] foundList = rootDir.listFiles();
		
		for (File foundLevelN : foundList) {
			if (foundLevelN.isDirectory() && foundLevelN.getName().indexOf(namePart) > -1) {
				return foundLevelN;
			}else if(foundLevelN.isDirectory() && subdirectories){
				return searchFolderInRootDir(foundLevelN, namePart, subdirectories);
			}
		}
		
		return null;
	}
	
	/**
	 * This method search for files in the given root directory,
	 * that contains in the name the given string. It searches also in subdirectories.
	 * @param rootDir
	 * @param namePart
	 * @param subdirectories if this is true then search also in subdirectories
	 * @return the list of found that matches, or empty List elsewhere.
	 */
	public static ArrayList<File> searchFilesInRootFolder(File rootDir, String namePart, boolean subdirectories) {
		
		ArrayList<File> returnList = new ArrayList<File>();
		
		File[] foundList = rootDir.listFiles();
		
		for (File foundLevelN : foundList) {
			if (foundLevelN.isFile() && foundLevelN.getName().indexOf(namePart) > -1) {
				returnList.add(foundLevelN);
			}else if(foundLevelN.isDirectory() && subdirectories){
				for (File subFile : searchFilesInRootFolder(foundLevelN, namePart, subdirectories)) {
					returnList.add(subFile);
				}
			}
		}
		
		return returnList;
	}
	
	/**
	 * This method search for directories in the given rootDirectory,
	 * that contains in their name the given string. If specified It searches also in subdirectories.
	 * @param rootDir
	 * @param namePart
	 * @param subdirectories if this is true then search also in subdirectories
	 * @return the list of found directories that matches, or empty list elsewhere.
	 */
	public static ArrayList<File> searchFoldersInRootFolder(File rootDir, String namePart, boolean subdirectories) {
		
		ArrayList<File> returnList = new ArrayList<File>();
		
		File[] foundList = rootDir.listFiles();
		
		for (File foundLevelN : foundList) {
			if (foundLevelN.isDirectory()) {
				if (foundLevelN.getName().indexOf(namePart) > -1) {
					returnList.add(foundLevelN);
				}
				if (subdirectories) {
					for (File subFile : searchFoldersInRootFolder(foundLevelN, namePart, subdirectories)) {
						returnList.add(subFile);
					}
				}
			}
		}
		
		return returnList;
	}
}
