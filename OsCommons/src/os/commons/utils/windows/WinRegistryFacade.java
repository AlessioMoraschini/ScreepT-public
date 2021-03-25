package os.commons.utils.windows;

import java.lang.reflect.InvocationTargetException;

public class WinRegistryFacade {

	public static String getWindowsDistribution() throws 
			IllegalArgumentException, 
			IllegalAccessException, 
			InvocationTargetException {
		
		return WinRegistry.readString (
			    WinRegistry.HKEY_LOCAL_MACHINE,                       
			   "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",     
			   "ProductName");                                        
	}
	
//	public static String addOpenwithFileMainMenu(String command, String description, String icon) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
//		String key = "*\\shell\\" + description;
//		
//		WinRegistryV2.createKey(WinRegistryV2.HKEY_CLASSES_ROOT, "*\\shell\\" + description);
//	}
}
