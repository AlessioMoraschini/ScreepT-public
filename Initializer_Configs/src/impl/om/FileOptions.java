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
package impl.om;

import java.io.File;

import arch.INItializerParent;
import files.om.FileContentSearchDTO;
import files.om.FileNamed;
import om.LimitedConcurrentList;
import resources.GeneralConfig;

public class FileOptions {
	
	// FILE SEARCH KEYS
	public static final String KEY_MATCH_CASE = "FSEARCH_MATCH_CASE";
	public static final String KEY_WHOLE_WORD = "FSEARCH_WHOLE_WORD";
	public static final String KEY_REGEX = "FSEARCH_REGEX";
	public static final String KEY_BINARIES_FLAG = "FSEARCH_BINARIES";
	public static final String KEY_LAST_FILE_FILTERS = "FSEARCH_LAST_FILE_FILTERS";
	public static final String KEY_LAST_LOADED_FILES = "FSEARCH_LAST_LOADED_FILES";
	public static final String KEY_LAST_SEARCH_STRINGS = "FSEARCH_LAST_SEARCH_STRINGS";
	
	// DEFAULT VALUES (usfeul to keep valid object during iniLoad, in case of null read or exception)
	public final static boolean DEFAULT_FLAG_AUTOSAVE = true;
	public final static boolean DEFAULT_FLAG_OVERWRITE = false;
	public final static int DEFAULT_AUTOSAVE_INTERVAL = 5;
	public final static String DEFAULT_LAST_SRC_FOLDER_PATH = INItializerParent.USER_PERSONAL_DIR;
	public final static String DEFAULT_LAST_DEST_FOLDER_PATH = INItializerParent.USER_PERSONAL_DIR;
	public final static int DEFAULT_MIN_AUTOSAVE_INTERVAL = 60000;
	public final static boolean DEF_fSearchMatchCase = true;
	public final static boolean DEF_fSearchWholeWord = false;
	public final static boolean DEF_fSearchRegex = false;
	public final static boolean DEF_fSearchBinaries = false;
	public final static boolean DEF_fNameSearchUseManual = false;
	public final static File DEF_fNameSearchManualDir = GeneralConfig.MAIN_APP_PARENT_DIR;
	
	// FIELDS
	private boolean flagAutosave;
	private boolean flagOverwrite;
	public int autosaveInterval;
	private String lastSrcFolderPath;
	private String lastDstFolderPath;
	private File fNameSearchManualDir;
	private boolean fNameSearchUseManual;
	private boolean fSearchMatchCase;
	private boolean fSearchWholeWord;
	private boolean fSearchRegex;
	private boolean fSearchBinaries;
	private LimitedConcurrentList<String[]> fSearchLastFileFilters;
	private LimitedConcurrentList<FileNamed> fSearchLastLoadedFiles;
	private LimitedConcurrentList<String> fSearchLastSearchStrings;
	
	public FileOptions() {
		flagAutosave = DEFAULT_FLAG_AUTOSAVE;
		flagOverwrite = DEFAULT_FLAG_OVERWRITE;
		autosaveInterval = DEFAULT_AUTOSAVE_INTERVAL;
		lastSrcFolderPath = DEFAULT_LAST_SRC_FOLDER_PATH;
		lastDstFolderPath = DEFAULT_LAST_DEST_FOLDER_PATH;
		
		fNameSearchUseManual = DEF_fNameSearchUseManual;
		fNameSearchManualDir = DEF_fNameSearchManualDir;
		
		fSearchRegex = DEF_fSearchRegex;
		fSearchWholeWord = DEF_fSearchWholeWord;
		fSearchRegex = DEF_fSearchRegex;
		fSearchBinaries = DEF_fSearchBinaries;
		fSearchLastFileFilters = new LimitedConcurrentList<>(15);
		fSearchLastLoadedFiles = new LimitedConcurrentList<>(20);
		fSearchLastSearchStrings = new LimitedConcurrentList<>(15);
	}
	
	public FileOptions(boolean autosave, boolean overwrite, String lastSrcPath, String lastDstPath, int autosaveInt) {
		this();
		flagAutosave = autosave;
		flagOverwrite = overwrite;
		lastSrcFolderPath = lastDstPath;
		lastDstFolderPath = lastDstPath;
		autosaveInterval = autosaveInt;
	}
	
	public FileOptions enrichFromDTO(FileContentSearchDTO dto) {
		
		if(dto != null) {
			fSearchBinaries = dto.binariesIncluded;
			fSearchMatchCase = dto.matchCase;
			fSearchRegex = dto.regex;
			fSearchWholeWord = dto.wholeWord;
			fSearchLastSearchStrings.addUniqueToTop(dto.searchString);
			if(dto.fileFilters != null && dto.fileFilters.length > 0)
				fSearchLastFileFilters.addUniqueToTopByEquals(dto.fileFilters);
		}
		
		return this;
	}

	public FileContentSearchDTO initDTO() {
		FileContentSearchDTO dto = new FileContentSearchDTO(fSearchLastSearchStrings.getLast());
		dto.binariesIncluded = fSearchBinaries;
		dto.matchCase = fSearchMatchCase;
		dto.regex = fSearchRegex;
		dto.wholeWord = fSearchWholeWord;
		dto.fileFilters = fSearchLastFileFilters.getLast();
		
		return dto;
	}
	
	// GETTERS AND SETTERS

	public boolean isFlagAutosave() {
		return flagAutosave;
	}

	public int getAutosaveInterval() {
		return autosaveInterval;
	}

	public void setAutosaveInterval(int autosaveInterval) {
		this.autosaveInterval = autosaveInterval;
	}

	public void setFlagAutosave(boolean flagAutosave) {
		this.flagAutosave = flagAutosave;
	}

	public boolean isFlagOverwrite() {
		return flagOverwrite;
	}

	public void setFlagOverwrite(boolean flagOverwrite) {
		this.flagOverwrite = flagOverwrite;
	}

	public String getLastSrcFolderPath() {
		return lastSrcFolderPath;
	}

	public void setLastSrcFolderPath(String lastSrcFolderPath) {
		this.lastSrcFolderPath = lastSrcFolderPath;
	}

	public String getLastDstFolderPath() {
		return lastDstFolderPath;
	}

	public void setLastDstFolderPath(String lastDstFolderPath) {
		this.lastDstFolderPath = lastDstFolderPath;
	}

	public boolean isfSearchMatchCase() {
		return fSearchMatchCase;
	}

	public void setfSearchMatchCase(boolean fSearchMatchCase) {
		this.fSearchMatchCase = fSearchMatchCase;
	}

	public boolean isfSearchWholeWord() {
		return fSearchWholeWord;
	}

	public void setfSearchWholeWord(boolean fSearchWholeWord) {
		this.fSearchWholeWord = fSearchWholeWord;
	}

	public boolean isfSearchRegex() {
		return fSearchRegex;
	}

	public void setfSearchRegex(boolean fSearchRegex) {
		this.fSearchRegex = fSearchRegex;
	}

	public boolean isfSearchBinaries() {
		return fSearchBinaries;
	}

	public void setfSearchBinaries(boolean fSearchBinaries) {
		this.fSearchBinaries = fSearchBinaries;
	}

	public LimitedConcurrentList<String[]> getfSearchLastFileFilters() {
		return fSearchLastFileFilters;
	}

	public void setfSearchLastFileFilters(LimitedConcurrentList<String[]> fSearchLastFileFilters) {
		this.fSearchLastFileFilters = fSearchLastFileFilters;
	}

	public LimitedConcurrentList<FileNamed> getfSearchLastLoadedFiles() {
		return fSearchLastLoadedFiles;
	}

	public void setfSearchLastLoadedFiles(LimitedConcurrentList<FileNamed> fSearchLastLoadedFiles) {
		this.fSearchLastLoadedFiles = fSearchLastLoadedFiles;
	}

	public LimitedConcurrentList<String> getfSearchLastSearchStrings() {
		return fSearchLastSearchStrings;
	}

	public void setfSearchLastSearchStrings(LimitedConcurrentList<String> fSearchLastSearchStrings) {
		this.fSearchLastSearchStrings = fSearchLastSearchStrings;
	}

	public File getfNameSearchManualDir() {
		return fNameSearchManualDir;
	}

	public void setfNameSearchManualDir(File fNameSearchManualDir) {
		this.fNameSearchManualDir = fNameSearchManualDir;
	}

	public boolean isfNameSearchUseManual() {
		return fNameSearchUseManual;
	}

	public void setfNameSearchUseManual(boolean fNameSearchUseManual) {
		this.fNameSearchUseManual = fNameSearchUseManual;
	}
	
}
