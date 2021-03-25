package various.common.light.cache.arch;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import various.common.light.cache.exception.ResultCacheException;

public abstract class AbstractResultCache<T> {
	
	///////////////  DEFAULTS  ////////////////
	public static float DEFAULT_DEALLOCATION_FACTOR = 0.15f;
	
	///////////////  FIELDS  ////////////////
	private long expiryTime;
	private ConcurrentHashMap<String, AbstractCachedObject<T>> cache;
	private IResultCacheRefresher<T> cacheRefresher;
	private volatile int maxCacheSize;
	private volatile float deallocationFactor;
	
	///////////////  CONSTRUCTORS  ////////////////
	
	public AbstractResultCache() {
		this.expiryTime = Long.MAX_VALUE;
		this.cache = new ConcurrentHashMap<>();
		this.cacheRefresher = null;
		this.maxCacheSize = Integer.MAX_VALUE;
		this.deallocationFactor = DEFAULT_DEALLOCATION_FACTOR;
	}

	public AbstractResultCache(long expiryTime) {
		this();
		this.expiryTime = expiryTime;
	}
	
	public AbstractResultCache(long expiryTime, IResultCacheRefresher<T> cacheRefresher) {
		this(expiryTime);
		this.cacheRefresher = cacheRefresher;
	}
	
	public void resetCache() {
		synchronized (cache) {
			cache = new ConcurrentHashMap<>();
		}
	}
	
	///////////////  EXPOSED METHODS  ////////////////
	
	public final T getRefreshed(IResultCacheInput input) throws Exception {
		
		return getRefreshed(input, cacheRefresher);
	}

	public final T get(IResultCacheInput input) throws Exception {
		
		return get(input, cacheRefresher);
	}

	public final T getRefreshed(IResultCacheInput input, IResultCacheRefresher<T> cacheRefresher) throws Exception {
		
		validateCacheRefresher(cacheRefresher);
		
		T refreshedValue = cacheRefresher.getRefreshedObject(input);
		
		AbstractCachedObject<T> newCacheEntry = getCachedObjectInstance(refreshedValue);
		putInCache(input.getCacheKey(), newCacheEntry);
		
		return getFromCache(input);
	}

	public final T get(IResultCacheInput input, IResultCacheRefresher<T> cacheRefresher) throws Exception {
		
		validateCacheRefresher(cacheRefresher);
		
		String key = input.getCacheKey();
		AbstractCachedObject<T> cached = cache.get(key);
		
		if(AbstractCachedObject.isExpired(cached, expiryTime)){
			// Expired: must refresh the cached obj and return it
			T refreshedValue = cacheRefresher.getRefreshedObject(input);
			
			AbstractCachedObject<T> newCacheEntry = getCachedObjectInstance(refreshedValue);
			putInCache(key, newCacheEntry);
		} 
		
		return getFromCache(input);
	}
	
	public final T getFromCache(IResultCacheInput input) {
		String key = input.getCacheKey();
		return cache.get(key) != null ? cache.get(key).cloneCacheObjectValue() : null;
	}
	
	///////////////  INTERNAL UTILITY METHODS  ////////////////
	
	private void putInCache(String key, AbstractCachedObject<T> cachedObject) {
		cache.put(key, cachedObject);
		
		if(cache.size() > maxCacheSize) {
			synchronized(cache) {
				int toRemove = getCacheSizePercentage(deallocationFactor);
				Iterator<String> iterator = cache.keySet().iterator();
				
				while (toRemove > 0) {
					if (iterator.hasNext()) {
						cache.remove(iterator.next());
					} else {
						break;
					}
					
					toRemove--;
				}
			}
		}
	}
	
	private final int getCacheSizePercentage(float percentage) {
		if(cache != null) {
			float size = (float) cache.size();
			return (int) (size * percentage) + 1;
		}
		
		return 0;
	}
	
	private void validateCacheRefresher(IResultCacheRefresher<T> refresher) throws ResultCacheException {
		if(refresher == null) {
			throw new ResultCacheException("cacheRefresher is NULL! Please initialize it before to call getRefreshed(IResultCacheInput input), or call the method's signature with a valid cacheRefresher parameter");
		}
	}
	
	///////////////  GETTERS  ////////////////
	
	/**
	 * @return the actual number of cached objects
	 */
	public long getCacheSize() throws IllegalStateException {
		return cache.size();
	}
	
	public int getMaxCacheSize() {
		return maxCacheSize;
	}

	public IResultCacheRefresher<T> getCacheRefresher() {
		return cacheRefresher;
	}
	
	public long getExpiryTime() {
		return expiryTime;
	}
	
	public float getDeallocationFactor() {
		return deallocationFactor;
	}
	
	///////////////  SETTERS  ////////////////

	public void setCacheRefresher(IResultCacheRefresher<T> cacheRefresher) {
		synchronized (this.cacheRefresher) {
			this.cacheRefresher = cacheRefresher;
		}
	}
	
	public void setMaxCacheSize(int maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
	}

	public void setDeallocationFactor(float deallocationFactor) {
		this.deallocationFactor = deallocationFactor;
	}

	///////////////  ABSTRACT METHODS  ////////////////
	
	protected abstract AbstractCachedObject<T> getCachedObjectInstance(T value);
}
