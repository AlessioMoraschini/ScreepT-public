/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
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
