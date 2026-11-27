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
package various.common.light.files.om;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileNamed{
	
	public final File mFile;

	public FileNamed(final String filePath) {
		mFile = new File(filePath);
	}

	public FileNamed(final File file) {
		mFile = file;
	}

	/** For callers that already know whether it is a folder (an index of the disk): no read from disk is needed to tell */
	public FileNamed(final File file, final boolean directory) {
		mFile = file;
		this.directory = directory;
	}
	
	/** Read from disk once: a tree asks it on every paint, and the node is recreated when the folder is read again */
	private Boolean directory;

	public boolean isDirectory() {
		Boolean cached = directory;
		if (cached == null) {
			cached = mFile != null && mFile.isDirectory();
			directory = cached;
		}
		return cached;
    }

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof FileNamed)) {
			return false;
		}
		File o = ((FileNamed) other).mFile;
		return mFile == null ? o == null : mFile.equals(o);
	}

	@Override
	public int hashCode() {
		return mFile == null ? 0 : mFile.hashCode();
	}
	
	public FileNamed[] listFiles() {
        if (mFile != null) {
			final File[] files = mFile.listFiles();
			if (files == null)
				return null;
			if (files.length < 1)
				return new FileNamed[0];
			final FileNamed[] ret = new FileNamed[files.length];
			for (int i = 0; i < ret.length; i++) {
				final File f = files[i];
				ret[i] = new FileNamed(f);
			}
			return ret;
		}else {
			return new FileNamed[0];
		}
    }
	
	public static List<FileNamed> fromFileList(List<File> files) {
		List<FileNamed> list = new ArrayList<FileNamed>();
		
		if(files != null) 
			for(File file : files) {
				if(file == null)
					continue;
				
				FileNamed fn = new FileNamed(file);
				list.add(fn);
			}
		
		return list;
	}

    public File getFile() {
        return mFile;
    }
    
    public String getFileName() {
        return (mFile != null)? mFile.getName() : "null_file";
    }
    
    public String getFileAbsPath() {
        return (mFile != null)? mFile.getAbsolutePath() : "null_file";
    }
    
    public String getCanonicalAbsPath() throws IOException {
        return (mFile != null)? mFile.getCanonicalPath() : "null_file";
    }

    @Override 
    public String toString() {
        return (mFile != null)? mFile.getName() : "null_file";
    }
}
