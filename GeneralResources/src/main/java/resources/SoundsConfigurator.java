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
