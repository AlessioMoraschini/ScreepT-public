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
package files.search;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import files.om.FileSearchDTO;
import utility.string.StringWorker;

public class FileNameSearchAdvanced {

	public enum SearchType {
		FILE_ONLY,
		FOLDERS_ONLY,
		ALL;
	}
	
	public static List<File> findByMatch(FileSearchDTO dto){
		return findByMatch(dto, Long.MAX_VALUE);
	}
	public static List<File> findByMatch(FileSearchDTO dto, long stopAfterNResults){
		return findByMatch(
				dto.root,
				dto.matchNamePart,
				dto.exactMatch,
				dto.filter, 
				dto.recursiveDeep,
				dto.ignoreCase,
				dto.wildCardMode,
				stopAfterNResults);
	}
	
	/**
	 * Find first match only for every folder level, if not found proceed with son directories
	 * @param root
	 * @param matchNamePart
	 * @return
	 */
	public static List<File> findByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch, 
			SearchType filter, 
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode) {
		return findByMatch(root, matchNamePart, exactMatch, filter, recursiveDeep, ignoreCase, wildcardMode, Long.MAX_VALUE);
	}
	
	/**
	 * Find first match only for every folder level, if not found proceed with son directories
	 * @param root
	 * @param matchNamePart
	 * @return
	 */
	public static List<File> findByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch, 
			SearchType filter, 
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode,
			long stopAfterNResults) {
		
		ArrayList<File> foundFolderMatches = new ArrayList<File>();
		
		if(root == null || !root.exists() || !root.isDirectory()) {
			return foundFolderMatches;
		}
		
		if(SearchType.FILE_ONLY.equals(filter)) {
			return findOnlyFilesByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
		
		} else if (SearchType.FOLDERS_ONLY.equals(filter)) {
			return findOnlyFoldersByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
			
		} else {
			return findAllByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
			
		}
	}

	/**
	 * Search for all files and folders only in given root directory: matching files/folders with name containing matchNamePart.
	 * If recursiveDeep is true, then all subdirectories of root will be scanned too unless a result is already found. 
	 * If false then search will continue until a result is found
	 */
	public static List<File> findAllByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch,
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode,
			long stopAfterNResults) {
		
		ArrayList<File> foundMatches = new ArrayList<File>();
		
		if(root == null || !root.exists() || !root.isDirectory()) {
			return foundMatches;
		}
		
		File[] currentLevelFiles = root.listFiles();
		boolean somethingFound = false;
		
		for(File found : currentLevelFiles) {
			
			String foundName = ignoreCase ? found.getName().toUpperCase() : found.getName();
			String matchNameP = ignoreCase ? matchNamePart.toUpperCase() : matchNamePart;
			
			// Search for match in current level
			if ((wildcardMode && StringWorker.checkWildcardMatch(foundName, matchNameP, false)) ||
					(exactMatch && foundName.equals(matchNameP)) || 
					(!exactMatch && foundName.contains(matchNameP))) {
				
				foundMatches.add(found);
				somethingFound = true;
			}
		}
		
		if(!recursiveDeep) {
			if (!somethingFound) {
				for (File folder : currentLevelFiles) {
					if (folder.isDirectory()) {
						List<File> subFolderFounds = findAllByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
						foundMatches.addAll(subFolderFounds);
						if(!subFolderFounds.isEmpty()) {
							return foundMatches;
						}
					}
				} 
			} else {
				return foundMatches;
			}
			
		} else {
			for (File folder : currentLevelFiles) {
				if (folder.isDirectory()) {
					foundMatches.addAll(findAllByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults));
				
					if(sizeExceedLimit(foundMatches, stopAfterNResults))
						return foundMatches;
				}
			} 
		}
		
		return foundMatches;
	}
	
	private static boolean sizeExceedLimit(List<?> files, long maxSize) {
		return files != null && (maxSize >= 0 &&  files.size() > maxSize);
	}
	
	/**
	 * Search for all files only in given root directory: matching files with name containing matchNamePart.
	 * If recursiveDeep is true, then all subdirectories of root will be scanned too unless a result is already found. 
	 * If false then search will continue until a result is found
	 */
	public static List<File> findOnlyFilesByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch,
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode,
			long stopAfterNResults) {
		
		ArrayList<File> foundMatches = new ArrayList<File>();
		
		if(root == null || !root.exists() || !root.isDirectory()) {
			return foundMatches;
		}
		
		File[] currentLevelFiles = root.listFiles();
		boolean somethingFound = false;
		
		for(File found : currentLevelFiles) {
			
			String foundName = ignoreCase ? found.getName().toUpperCase() : found.getName();
			String matchNameP = ignoreCase ? matchNamePart.toUpperCase() : matchNamePart;
			
			// Search for match in current level
			if (found.isFile() && (
					(wildcardMode && StringWorker.checkWildcardMatch(foundName, matchNameP, false)) ||
					(exactMatch && foundName.equals(matchNameP)) ||
					(!exactMatch && foundName.contains(matchNameP)))) {
				
				foundMatches.add(found);
				somethingFound = true;
				
			}
		}
		
		if(!recursiveDeep) {
			if (!somethingFound) {
				for (File folder : currentLevelFiles) {
					if (folder.isDirectory()) {
						List<File> subFilesFounds = findOnlyFilesByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
						foundMatches.addAll(subFilesFounds);
						if(!subFilesFounds.isEmpty()) {
							return foundMatches;
						}
					}
				} 
			} else {
				return foundMatches;
			}
			
		} else {
			for (File folder : currentLevelFiles) {
				if (folder.isDirectory()) {
					foundMatches.addAll(findOnlyFilesByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults));
					
					if(sizeExceedLimit(foundMatches, stopAfterNResults))
						return foundMatches;
				}
			} 
		}
		
		return foundMatches;
	}
	
	/**
	 * Search for all folders only in given root directory: matching folders with name containing matchNamePart.
	 * If recursiveDeep is true, then all subdirectories of root will be scanned too unless a result is already found. 
	 * If false then search will continue until a result is found
	 */
	public static List<File> findOnlyFoldersByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch,
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode,
			long stopAfterNResults) {
		
		ArrayList<File> foundMatches = new ArrayList<File>();
		
		if(root == null || !root.exists() || !root.isDirectory()) {
			return foundMatches;
		}
		
		File[] currentLevelFolders = root.listFiles(foldersOnly());
		boolean somethingFound = false;
		
		for(File found : currentLevelFolders) {
			
			String foundName = ignoreCase ? found.getName().toUpperCase() : found.getName();
			String matchNameP = ignoreCase ? matchNamePart.toUpperCase() : matchNamePart;
			
			// Search for match in current level
			if ((wildcardMode && StringWorker.checkWildcardMatch(foundName, matchNameP, false)) ||
					(exactMatch && foundName.equals(matchNameP)) || 
					(!exactMatch && foundName.contains(matchNameP))) {
				
				foundMatches.add(found);
				somethingFound = true;
			}
		}
		
		if(!recursiveDeep) {
			if (!somethingFound) {
				for (File folder : currentLevelFolders) {
					List<File> subFolderFounds = findOnlyFoldersByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
					foundMatches.addAll(subFolderFounds);
					if(!subFolderFounds.isEmpty()) {
						return foundMatches;
					}
				} 
			} else {
				return foundMatches;
			}
			
		} else {
			for (File folder : currentLevelFolders) {
				foundMatches.addAll(findOnlyFoldersByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults));
				
				if(sizeExceedLimit(foundMatches, stopAfterNResults))
					return foundMatches;
			} 
		}
		
		return foundMatches;
	}
	
	public static FileFilter foldersOnly() {
		return (file) -> {return file.isDirectory();};
	}

	public static FileFilter filesOnly() {
		return (file) -> {return file.isFile();};
	}
}
