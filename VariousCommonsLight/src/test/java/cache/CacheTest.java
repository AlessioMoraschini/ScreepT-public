package cache;

import java.io.File;
import java.util.List;

import various.common.light.cache.impl.FileSearchResultCache;
import various.common.light.files.om.FileSearchDTO;
import various.common.light.files.search.FileNameSearchAdvanced;
import various.common.light.files.search.FileNameSearchAdvanced.SearchType;

public class CacheTest {
	
	private final static String ROOT_FOLDER = "C:\\Users\\xx\\Desktop\\Crypter Test\\Themes";
	private final static String MATCH = "Li";
	private final static long EXPIRY_TIME = 5000;
	
	private static FileSearchResultCache cache =
			new FileSearchResultCache(
				EXPIRY_TIME,
				(input) -> {
					return FileNameSearchAdvanced.findByMatch((FileSearchDTO)input);
				});
	
	public static void main(String[] args) throws Exception {
		
		cache.setMaxCacheSize(15);
		cache.setDeallocationFactor(0.5f);
		
		new Thread(() ->  {
			for(int i = 0; i < 60; i++) {
				try {
					FileSearchDTO inputDto = new FileSearchDTO(
							new File(ROOT_FOLDER),
							MATCH + i,
							true,
							SearchType.ALL,
							true,
							false,
							false);
					
					printList(cache.get(inputDto));

					printCacheSize();
					Thread.sleep(400);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		
//		new Thread(() ->  {
//			for(int i = 0; i < 60; i++) {
//				try {
//					printList(cache.get(new FileSearchDTOCache(new File(ROOT_FOLDER), MATCH.toLowerCase(), SearchType.ALL, true)));
//					Thread.sleep(1000);
//					printCacheSize();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}).start();
		
	}
	
	private static void printCacheSize() {
		System.out.println("Cache size is: " + cache.getCacheSize());
	}
	
	private static void printList(List<File> list) {
		if (list != null) {
			for (File file : list) {
				System.out.println(file);
			} 
		}
	}
	
}
