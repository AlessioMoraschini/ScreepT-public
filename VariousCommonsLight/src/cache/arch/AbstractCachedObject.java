package cache.arch;

public abstract class AbstractCachedObject<T> {

	private long tsUpdate;
	protected T value;
	
	protected AbstractCachedObject(T value) {
		super();
		this.tsUpdate = System.currentTimeMillis();
		this.value = value;
	}

	public long getTsUpdate() {
		return tsUpdate;
	}
	
	protected abstract T cloneCacheObjectValue();

	public static boolean isExpired(AbstractCachedObject<?> cacheObj, long expiryTime){
		return cacheObj == null || expiryTime < (System.currentTimeMillis() - cacheObj.tsUpdate);
	}
}
