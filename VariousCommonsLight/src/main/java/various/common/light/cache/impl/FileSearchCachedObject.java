package various.common.light.cache.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import various.common.light.cache.arch.AbstractCachedObject;

public class FileSearchCachedObject extends AbstractCachedObject<List<File>> {

	protected FileSearchCachedObject(List<File> value) {
		super(value);
	}

	@Override
	protected List<File> cloneCacheObjectValue() {
		List<File> cloned = null;
		if (value != null) {
			cloned = new ArrayList<File>();
			for (File file : value) {
				cloned.add(file == null ? null : new File(file.getAbsolutePath()));
			} 
		}
		
		return cloned;
	}
}
