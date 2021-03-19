package cache.arch;

@FunctionalInterface
public interface IResultCacheRefresher<T> {
	public T getRefreshedObject(IResultCacheInput input) throws Exception;
}
