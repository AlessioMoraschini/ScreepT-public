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
package resources.om;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class FilePojo{
	
	public final File mFile;

	public FilePojo(final File file) {
		mFile = file;
	}
	
	public FilePojo(String file) {
		this( new File(file) );
	}
	
	public boolean isDirectory() {
        return mFile.isDirectory();
    }
	
	public Set<FilePojo> listFiles() {
		if(!mFile.isDirectory()) return new HashSet<FilePojo>();
        final File[] files = mFile.listFiles();
        
        if (files == null) return new HashSet<FilePojo>();
        if (files.length < 1) return new HashSet<FilePojo>();

        final LinkedHashSet<FilePojo> ret = new LinkedHashSet<FilePojo>();
        
        for (int i = 0; i < ret.size(); i++) {
            ret.add(new FilePojo(files[i]));
        }
        
        return ret;
    }

	// nullable result
	public static List<FilePojo> listFilesFromDir(File dir) {
		if(!dir.isDirectory()) return new ArrayList<FilePojo>();
		
		final File[] files = dir.listFiles();
		
		if (files == null) return new ArrayList<FilePojo>();
		if (files.length < 1) return new ArrayList<FilePojo>();
		
		final ArrayList<FilePojo> ret = new ArrayList<FilePojo>();
		for (File curr : files) {
			ret.add(new FilePojo(curr));
		}
		
		return ret;
	}

	// nullable result
	public static TreeMap<String, File> mapFilesFromDir(File dir) {
		TreeMap<String, File> emptyMap = new TreeMap<>();
		
		if(!dir.isDirectory()) return emptyMap;
		
		final File[] files = dir.listFiles();
		if (files == null || files.length < 1) return emptyMap;
		
		for (File curr : files) {
			emptyMap.put(curr.getName(), curr);
		}
		
		return emptyMap;
	}
	

    public File getFile() {
        return mFile;
    }
    
    public String getFileName() {
        return mFile.getName();
    }
    
    public String getFileAbsPath() {
        return mFile.getAbsolutePath();
    }
    
    public String getCanonicalAbsPath() throws IOException {
        return mFile.getCanonicalPath();
    }

    @Override 
    public String toString() {
        return mFile.getName();
    }
}
