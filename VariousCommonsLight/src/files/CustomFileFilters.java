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
package files;

import java.io.File;
import java.io.FileFilter;

public class CustomFileFilters {

	public static final String ALL_FOUND = "ALL";
	public static final String ALL_FOUND_HIDDEN = "ALLH";
	public static final String ALL_FOUND_VISIBLE = "ALLV";
	public static final String FILE_ONLY = "FILE_ONLY";
	public static final String FILE_ONLY_HIDDEN = "FILE_ONLYH";
	public static final String FILE_ONLY_VISIBLE = "FILE_ONLYV";
	public static final String DIR_ONLY = "DIR_ONLY";
	public static final String DIR_ONLY_HIDDEN = "DIR_ONLYH";
	public static final String DIR_ONLY_VISIBLE = "DIR_ONLYV";
	
	public static boolean fileAccepted(String filter) {
		return filter != null &&
				(filter.contains("ALL") || filter.contains("FILE"));
	}
	
	/**
	 * Return a custom file filter with a filter typte to select type of files to accept, without check on extension
	 * @param filterType use values defined in this class
	 * @return the custom file filter
	 */
	public static FileFilter customTypeFilter(String filterType) {
		return new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				
				// first check if file exists
				if(!pathname.exists()) {
					return false;
				}
				
				// now filter for type choosen
				return checkSelectedFileType(filterType, pathname);
			}
		};
	}
	
	/**
	 * Return a custom file filter with a filter typte to select type of files to accept, and extension of files to accept
	 * @param filterType use values defined in this class
	 * @param caseSensitive if true it consider different asD.tXt than asd.txt; otherwise they'll be recognized as equals
	 * @param extension specify the extension, in case of null this work as the only filterType signature
	 * @return the custom file filter
	 */
	public static FileFilter customTypeFilter(String filterType, String extension, boolean caseSensitive) {
		
		if(extension == null) {
			return customTypeFilter(filterType);
			
		}else return new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				
				boolean extensionCheck = false;
				
				// first check if file exists
				if(!pathname.exists()) {
					return false;
				}
				
				// now filter for type chosen
				extensionCheck = checkSelectedFileType(filterType, pathname);
				
				// if at least one match found for this file proceed with extension check
				if(extensionCheck) {
					boolean condition = (caseSensitive)? pathname.getName().endsWith(extension) : pathname.getName().toLowerCase().endsWith(extension.toLowerCase());
					if(condition) {
						return true;
					}
				}
				return false;
			}
		};
	}
	
	private static boolean checkSelectedFileType(String filterType, File pathname) {
		if(filterType.equals(ALL_FOUND)) {
			return true;
		}else if(filterType.equals(ALL_FOUND_VISIBLE) && !pathname.isHidden()){
			return true;
		}else if(filterType.equals(ALL_FOUND_HIDDEN) && pathname.isHidden()){
			return true;
		}else if(filterType.equals(FILE_ONLY) && pathname.isFile()) {
			return true;
		}else if(filterType.equals(FILE_ONLY_HIDDEN) && pathname.isFile() && pathname.isHidden()) {
			return true;
		}else if(filterType.equals(FILE_ONLY_VISIBLE) && pathname.isFile() && !pathname.isHidden()) {
			return true;
		}else if(filterType.equals(DIR_ONLY) && pathname.isDirectory()) {
			return true;
		}else if(filterType.equals(DIR_ONLY_HIDDEN) && pathname.isDirectory() && pathname.isHidden()) {
			return true;
		}else if(filterType.equals(DIR_ONLY_VISIBLE) && pathname.isDirectory() && !pathname.isHidden()) {
			return true;
		}else {
			// IF NO MATCH FOUND
			return false;
		}
	}
}
