/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package resources;

import sounds.SoundsManager;

public class SoundsManagerExtended extends SoundsManager {
	
	public static void playSound(String pathToResource, Float volum){
		if(volum != null) {
			SoundsManager.playSound(pathToResource, volum);
		}else {
			SoundsManager.playSound(pathToResource, SoundsConfigurator.DEFAULT_EFFECTS_VOLUME);
		}
	}
}
