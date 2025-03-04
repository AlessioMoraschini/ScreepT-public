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

public class SoundsConfigurator {

	public static float DEFAULT_EFFECTS_VOLUME = 0.45f;
	
	public static String CLICK_1;
	public static String CLICK_2;
	public static String CLICK_DOUBLE;
	public static String ERROR;
	public static String SMASHING;
	public static String SOUND_CLOCK;
	public static String SOUND_PLING;
	public static String SOUND_POOL_SHOT;
	
	public static void init() {
		CLICK_1 = GeneralConfig.SOUNDS_FOLDER + "Click1.wav";          
		CLICK_2 = GeneralConfig.SOUNDS_FOLDER + "Click2.wav";          
		CLICK_DOUBLE = GeneralConfig.SOUNDS_FOLDER + "clickDouble.wav";
		ERROR = GeneralConfig.SOUNDS_FOLDER + "Error.wav";             
		SMASHING = GeneralConfig.SOUNDS_FOLDER + "Smashing.wav";       
		SOUND_CLOCK = GeneralConfig.SOUNDS_FOLDER + "clock.wav";       
		SOUND_PLING = GeneralConfig.SOUNDS_FOLDER + "Pling.wav";       
		SOUND_POOL_SHOT = GeneralConfig.SOUNDS_FOLDER + "PoolShot.wav";
	}
	
}
