/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
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
package resources;

public class ImagesPathConfigurator {

	public static String IMAGE_LOGO_AM_MAIN;
	public static String IMAGE_BCKG_ABOUT;
	public static String IMAGE_BCKG_DRAGNDROP;
	public static String MAIN_APP_ICON = "Resources_ScreepT/Images/icon.png";
	public static String LAUNCHER_BACK_IMAGE_PATH = "LAUNCHER/launcher_icon.jpg";
	
	public static void init() {
		IMAGE_LOGO_AM_MAIN = GeneralConfig.DEFAULT_IMAGE_FOLDER + GeneralConfig.resourcesFilePathsMappings.getProperty("AM_IMAGE_MAIN_FILE");   
		IMAGE_BCKG_ABOUT = GeneralConfig.DEFAULT_IMAGE_FOLDER + GeneralConfig.resourcesFilePathsMappings.getProperty("BCKG_IMAGE_ABOUT_FILE");  
		IMAGE_BCKG_DRAGNDROP = GeneralConfig.DEFAULT_IMAGE_FOLDER + GeneralConfig.resourcesFilePathsMappings.getProperty("DRAGNDROP_BCKG_FILE");
		MAIN_APP_ICON = GeneralConfig.DEFAULT_IMAGE_FOLDER + GeneralConfig.resourcesFilePathsMappings.getProperty("MAIN_ICON_FILE");            
		LAUNCHER_BACK_IMAGE_PATH = GeneralConfig.resourcesFilePathsMappings.getProperty("LAUNCHER_BACK_IMAGE_PATH");                            
		                                                                                                                                        
	}
}
