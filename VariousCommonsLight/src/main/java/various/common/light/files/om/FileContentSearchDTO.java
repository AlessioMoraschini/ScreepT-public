package various.common.light.files.om;

import java.io.Serializable;
import java.util.Arrays;

import various.common.light.utility.string.StringWorker;

public class FileContentSearchDTO implements Serializable {
	private static final long serialVersionUID = 8534947210098858129L;
	
	public boolean matchCase;
	public boolean wholeWord;
	public boolean regex;
	public boolean binariesIncluded;
	public String[] fileFilters;
	
	public String searchString;
	
	
	public FileContentSearchDTO(String searchString) {
		this(searchString, false, false, false, false, null);
	}
	
	public FileContentSearchDTO(String searchString, boolean matchCase, boolean wholeWord, boolean regex, boolean binariesIncluded, String[] fileFilters) {
		this.searchString = StringWorker.trimToEmpty(searchString);
		this.matchCase = matchCase;
		this.wholeWord = wholeWord;
		this.regex = regex;
		this.binariesIncluded = binariesIncluded;
		this.fileFilters = fileFilters == null ? new String[0] : fileFilters;
	}

	@Override
	public String toString() {
		return "FileContentSearchDTO " + toStringLight();
	}

	public String toStringLight() {
		return "["
				+ "searchString=" + searchString 
				+ ", matchCase=" + matchCase 
				+ ", wholeWord=" + wholeWord 
				+ ", regex=" + regex 
				+ ", binariesIncluded=" + binariesIncluded
				+ ", fileFilters=" + Arrays.toString(fileFilters) 
				+ " ]";
	}
}
