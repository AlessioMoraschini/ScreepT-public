package cache.impl;

import java.io.File;
import java.util.List;

import cache.arch.AbstractCachedObject;
import cache.arch.AbstractResultCache;
import cache.arch.IResultCacheRefresher;

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
