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
package files.watcher;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ChangeDTO {

	public ArrayList<File> changedFiles;
	public ArrayList<File> addedFiles;
	public ArrayList<File> removedFiles;
	
	public ChangeDTO(){
		changedFiles = new ArrayList<File>();
		addedFiles = new ArrayList<File>();
		removedFiles = new ArrayList<File>();
	}

	@Override
	public String toString() {
		return "ChangeDTO [changedFiles=" + str(changedFiles) + ", addedFiles=" + str(addedFiles) + ", removedFiles=" + str(removedFiles) + "]";
	}
	
	private String str(ArrayList<File> list) {
		StringBuilder sb = new StringBuilder();
		for(File curr : list) {
			sb.append(curr.getAbsolutePath());
		}
		
		return sb.toString();
	}
	
	/**
	 * Updates current obj with given change DTO (existing changes will be skipped to avoid duplicates)
	 * 
	 * @param changeDto
	 */
	public void update(ChangeDTO changeDto){
		for(File changed : changeDto.changedFiles) {
			if(!changedFiles.contains(changed)) {
				changedFiles.add(changed);
			}
		}
		for(File removed : changeDto.removedFiles) {
			if(!removedFiles.contains(removed)) {
				removedFiles.add(removed);
			}
		}
		for(File created : changeDto.addedFiles) {
			if(!addedFiles.contains(created)) {
				addedFiles.add(created);
			}
		}
	}
	
	public File findChangedFile(File target) throws IOException {
		for(File iterator : changedFiles) {
			if(iterator.getCanonicalFile().equals(target)) {
				return iterator;
			}
		}
		
		return null;
	}
	
	public File findAddeddFile(File target) throws IOException {
		for(File iterator : addedFiles) {
			if(iterator.getCanonicalFile().equals(target)) {
				return iterator;
			}
		}
		
		return null;
	}

	public File findRemovedFile(File target) throws IOException {
		for(File iterator : removedFiles) {
			if(iterator.getCanonicalFile().equals(target)) {
				return iterator;
			}
		}
		
		return null;
	}
	
	public boolean isChanged(File target) throws IOException {
		for(File iterator : changedFiles) {
			if(iterator.getCanonicalFile().equals(target)) {
				return true;
			}
		}
		return false;
	}
	public boolean isAdded(File target) throws IOException {
		for(File iterator : addedFiles) {
			if(iterator.getCanonicalFile().equals(target)) {
				return true;
			}
		}
		return false;
	}
	public boolean isRemoved(File target) throws IOException {
		for(File iterator : removedFiles) {
			if(iterator.getCanonicalFile().equals(target)) {
				return true;
			}
		}
		return false;
	}
	public boolean isAnyChangeType(File target) throws IOException {
		
		return (isAdded(target) || isRemoved(target) || isChanged(target));
	}
	
}
