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
package various.common.light.files.watcher;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import various.common.light.utility.log.SafeLogger;

public class FileChangeMonitor {
	
	static SafeLogger logger = new SafeLogger(FileChangeMonitor.class);
	
	public boolean MOD_FLAG = true;
	public boolean DEL_FLAG = true;
	public boolean NEW_FLAG = true;

	// FIELDS // 
	private boolean monitorAllContents = false;
	public WatchService watchService;
	public WatchKey watchKeyAll;
	public ArrayList<File> monitoredFileList;
	public File rootMonitorDir;
	
	// CONSTRUCTORS //
	
	/** 
	 * - Root must be not blank, it's the scope of the monitor.
	 * 
	 * - initial file list to monitor can instead be null, put here file you want to monitor from just after object initialization
	 */
	public FileChangeMonitor(File rootMonitorFolder, ArrayList<File> monitoredInitialFiles){
		
		logger.info("FileChangeMonitor() -> Start - Creating a new monitor instance with MOD/CREATE/DELETE \n\n[rootMonitorFolder:"+rootMonitorFolder+"]");
		
		rootMonitorDir = rootMonitorFolder;
		
		preloadInitialFiles(monitoredInitialFiles);
		
		initialize();
		
		logger.info("FileChangeMonitor() -> Ready - Monitor instance created");
	}

	/** 
	 * - Root must be not blank, it's the scope of the monitor.
	 * 
	 * - initial file list to monitor can instead be null, put here file you want to monitor from just after object initialization
	 * - here you can specify with 3 flags which events you want to monitor (to change during execution use flag setters methods from this class)
	 */
	public FileChangeMonitor(File rootMonitorFolder, ArrayList<File> monitoredInitialFiles, boolean delete, boolean modded, boolean create){
		
		logger.info("FileChangeMonitor() -> Start - Creating a new monitor instance(MOD: "+modded+" CREATE:"+create+" DELETE:"+delete
				+") \n\n[rootMonitorFolder:"+rootMonitorFolder+"]");
		
		this.DEL_FLAG = delete;
		this.MOD_FLAG = modded;
		this.NEW_FLAG = create;
		
		rootMonitorDir = rootMonitorFolder;
		
		preloadInitialFiles(monitoredInitialFiles);
		
		initialize();
		
		logger.info("FileChangeMonitor() -> Ready - Monitor instance created");
	}
	
	private void preloadInitialFiles(ArrayList<File> monitoredInitialFiles) {
		if(monitoredInitialFiles != null){
			monitoredFileList = new ArrayList<File>();
			// test for folder nested and add files contained to monitor list
			monitoredFileList.addAll(monitoredInitialFiles);
			for(File subFile : monitoredInitialFiles) {
				if(subFile.isDirectory()) {
					addFolderContent(subFile);
				}
			}
		}else{
			monitoredFileList = new ArrayList<File>();
			if(monitorAllContents) {
				addFolderContent(rootMonitorDir);
			}
		}
	}
	
	// METHODS //
	
	/**
	 * Creates a monitor listening for the chosen event types
	 */
	public void initialize(){
		final Path rootPath = FileSystems.getDefault().getPath(rootMonitorDir.getAbsolutePath());
		try {
			watchService = FileSystems.getDefault().newWatchService();
			ArrayList<Kind<?>> path = new ArrayList<Kind<?>>(); 

			if(DEL_FLAG) {
				path.add(StandardWatchEventKinds.ENTRY_DELETE);
			}
			
			if(MOD_FLAG){
				path.add(StandardWatchEventKinds.ENTRY_MODIFY);
			}
			
			if(NEW_FLAG){
				path.add(StandardWatchEventKinds.ENTRY_CREATE);
			}
			
			watchKeyAll = rootPath.register(watchService, path.toArray(new Kind<?>[path.size()]));
	    }catch(Exception e){
	    	logger.error("\n\n !!! ERROR CREATING FILE-CHANGE-MONITOR OBJECT !!!\n\n");
	    }
	}
	
	/**
	 * Add recursively files and sub-folders contained in the given folder
	 * @param folder
	 */
	public void addFolderContent(File folder){
		if (folder != null && folder.isDirectory()) {
			String curr = "";
			for (File current : folder.listFiles()) {
				
				if (current.isDirectory()) {
					addFolderContent(current);
					logger.debug("FileChangeMonitor() -> Directory: adding recursively...");
				}
				
				try {
					curr = current.getCanonicalPath();
				} catch (Exception e) {
					curr = current.getAbsolutePath();
				}
				
				synchronized (monitoredFileList) {
					monitoredFileList.add(current);
					logger.debug("FileChangeMonitor() -> "+curr+" added to monitored List.");
				}
			}
		}
	}
	
	/**
	 * CLEAN ChangeDTO parameter from given file occurrences
	 */
	public static ChangeDTO cleanDtoFromFile(ChangeDTO changes, File target) {
		changes.removedFiles.removeAll(Collections.singletonList(target));
		changes.changedFiles.removeAll(Collections.singletonList(target));
		changes.addedFiles.removeAll(Collections.singletonList(target));
		
		return changes;
	}
	
	/**
	 * @return all changes in monitored files (and new adds in root folder), according to specified Flags mod/create/remove
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public ChangeDTO checkForEverithing() throws Exception{
		
		logger.debug("checkForEverithing() -> starting check...");
		
		ChangeDTO changedsResponse = new ChangeDTO();
		boolean fileAlreadyAdded = false;
		boolean justAdded = false;
        for (WatchEvent<?> event : watchKeyAll.pollEvents()) {
            
        	WatchEvent<Path> eventPath = (WatchEvent<Path>) event;
        	Path dir = (Path)watchKeyAll.watchable();
        	final Path changed = dir.resolve(eventPath.context());
        	
            synchronized (monitoredFileList) {
				Iterator<File> iteratorMonitored = monitoredFileList.iterator();
				List<File> thingsToBeAddToMonitored = new ArrayList<File>();
				
				while (iteratorMonitored.hasNext()) {
					File currentFileInMonitored = iteratorMonitored.next();
					File currentChange = new File(changed.toUri());

					if (MOD_FLAG 
							&& event.kind() == StandardWatchEventKinds.ENTRY_MODIFY 
							&& !justAdded
							&& currentFileInMonitored.toPath().endsWith(currentChange.getAbsolutePath())) {
						
						changedsResponse.changedFiles.add(currentChange);
						
					}else if (NEW_FLAG 
							&& event.kind() == StandardWatchEventKinds.ENTRY_CREATE 
							&& !fileAlreadyAdded) {
						
						changedsResponse.addedFiles.add(currentChange);
						justAdded = false;
						if (monitorAllContents) {
							thingsToBeAddToMonitored.add(currentChange);
						}
						fileAlreadyAdded = false;
						
					} else if (DEL_FLAG 
							&& event.kind() == StandardWatchEventKinds.ENTRY_DELETE
							&& currentFileInMonitored.toPath().endsWith(currentChange.getAbsolutePath())) {
						
						changedsResponse.removedFiles.add(currentChange);
						if (monitoredFileList.contains(currentChange)) {
							iteratorMonitored.remove();
						}
					}
				}
				
				fileAlreadyAdded = false;
				monitoredFileList.addAll(thingsToBeAddToMonitored);
	        
            }// End synchronized
            
            logger.debug("checkForEverithing() -> finished with result: " + changedsResponse);
            
	        // reset the key
	        boolean valid = watchKeyAll.reset();
	        if (!valid) {
	        	// notify error
	        }
        }
        
        return changedsResponse;
	}

	/**
	 * @return all changes in specified file, according to specified Flags mod/remove/create
	 * @throws InterruptedException
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public ChangeDTO checkForFile(File target) throws Exception{
		
		ChangeDTO changedsResponse = new ChangeDTO();
		if(target == null) {
			return changedsResponse;
		}
		
		for (WatchEvent<?> event : watchKeyAll.pollEvents()) {
			
			WatchEvent<Path> eventPath = (WatchEvent<Path>) event;
			Path dir = (Path)watchKeyAll.watchable();
			final Path changed = dir.resolve(eventPath.context());
			
			synchronized (target) {
				
				File currentChange = new File(changed.toUri());
				
				if (MOD_FLAG 
						&& event.kind() == StandardWatchEventKinds.ENTRY_MODIFY 
						&& target.toPath().endsWith(currentChange.getCanonicalPath())) {
					
					changedsResponse.changedFiles.add(currentChange);
					
				}else if (NEW_FLAG 
						&& event.kind() == StandardWatchEventKinds.ENTRY_CREATE 
						&& target.toPath().endsWith(currentChange.getCanonicalPath())) {
					
					changedsResponse.addedFiles.add(currentChange);
					
				} else if (DEL_FLAG 
						&& event.kind() == StandardWatchEventKinds.ENTRY_DELETE
						&& target.toPath().endsWith(currentChange.getCanonicalPath())) {
					
					changedsResponse.removedFiles.add(currentChange);
				}
			}// End synchronized
		}
		
//		logger.debug("checkForEverithing() -> finished with result: " + changedsResponse);
		
		return changedsResponse;
	}
	
	/**
	 * Thread safe method to add a file into the monitored list with current type of listening
	 * @param fileToAdd
	 * @return
	 */
	public boolean addFileToMonitorList(File fileToAdd){
		if(fileToAdd == null) {
			return false;
		}
		
		synchronized (monitoredFileList) {
			if (!monitoredFileList.contains(fileToAdd)) {
				monitoredFileList.add(fileToAdd);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Thread safe method to remove a file from the monitored list
	 * @param fileToAdd
	 * @return
	 */
	public boolean removeFromListener(File toRemove) {
		if(toRemove == null) {
			return false;
		}
		
		synchronized (monitoredFileList) {
			if (monitoredFileList.contains(toRemove)) {
				monitoredFileList.remove(toRemove);
			}
		}
		
		return true;
	}

	/**
	 * Thread safe method to remove all files from the monitored list
	 * @param fileToAdd
	 * @return
	 */
	public boolean removeAllFromListener() {
		
		synchronized (monitoredFileList) {
			monitoredFileList.clear();
		}
		
		return true;
	}

	/**
	 * Thread safe method to remove a collection of file files from the monitored list
	 * @param fileToAdd
	 * @return
	 */
	public boolean removeFromListener(Collection<File> toRemove) {
		if(toRemove == null || toRemove.size() == 0) {
			return false;
		}
		
		synchronized (monitoredFileList) {
			monitoredFileList.removeAll(toRemove);
		}
		
		return true;
	}
	
	// GETTERS & SETTERS //
	
	public boolean isMOD_FLAG() {
		return MOD_FLAG;
	}

	public void setMOD_FLAG(boolean mOD_FLAG) {
		MOD_FLAG = mOD_FLAG;
	}

	public boolean isDEL_FLAG() {
		return DEL_FLAG;
	}

	public void setDEL_FLAG(boolean dEL_FLAG) {
		DEL_FLAG = dEL_FLAG;
	}

	public boolean isNEW_FLAG() {
		return NEW_FLAG;
	}

	public void setNEW_FLAG(boolean nEW_FLAG) {
		NEW_FLAG = nEW_FLAG;
	}

	@Deprecated
	public void removeFileFromList(int index){
		monitoredFileList.remove(index);
	}
	@Deprecated
	public void removeFileFromList(File fileToRemove){
		monitoredFileList.remove(fileToRemove);
	}
}
