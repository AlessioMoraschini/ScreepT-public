package various.common.light.files.om;

import java.io.File;

import various.common.light.cache.arch.IResultCacheInput;
import various.common.light.files.search.FileNameSearchAdvanced.SearchType;


public class FileSearchDTO implements IResultCacheInput {

	public File root;
	public String matchNamePart;
	public boolean exactMatch;
	public SearchType filter;
	public boolean recursiveDeep;
	public boolean ignoreCase;
	public boolean wildCardMode;
	
	public FileSearchDTO(
			File root,
			String matchNamePart,
			boolean exactMatch,
			SearchType filter, 
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildCardMode) {
		
		super();
		this.root = root;
		this.matchNamePart = matchNamePart;
		this.exactMatch = exactMatch;
		this.filter = filter;
		this.recursiveDeep = recursiveDeep;
		this.ignoreCase = ignoreCase;
		this.wildCardMode = wildCardMode;
	}
	
	@Override
	public String getCacheKey() {
		return (root != null ? root.getAbsolutePath() : "") + matchNamePart + exactMatch + filter + recursiveDeep + ignoreCase + wildCardMode;
	}

	@Override
	public String toString() {
		return "FileSearchDTO [root=" + root + ", matchNamePart=" + matchNamePart + ", exactMatch=" + exactMatch + ", filter=" + filter + ", recursiveDeep="
				+ recursiveDeep + ", ignoreCase=" + ignoreCase + ", wildcardMode=" + wildCardMode + "]";
	}
}
