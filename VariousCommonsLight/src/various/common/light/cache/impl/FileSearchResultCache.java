package various.common.light.cache.impl;

import java.io.File;
import java.util.List;

import various.common.light.cache.arch.AbstractCachedObject;
import various.common.light.cache.arch.AbstractResultCache;
import various.common.light.cache.arch.IResultCacheRefresher;

public class FileSearchResultCache extends AbstractResultCache<List<File>> {
	
	
	public FileSearchResultCache() {
		super();
	}

	public FileSearchResultCache(long expiryTime, IResultCacheRefresher<List<File>> cacheRefresher) {
		super(expiryTime, cacheRefresher);
	}

	public FileSearchResultCache(long expiryTime) {
		super(expiryTime);
	}

	@Override
	protected AbstractCachedObject<List<File>> getCachedObjectInstance(List<File> value) {
		return new FileSearchCachedObject(value);
	}

}
