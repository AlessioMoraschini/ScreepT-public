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
package hw.commons.main;

import javax.swing.JFrame;

import org.apache.log4j.PropertyConfigurator;

import resources.GeneralConfig;

public class HardwareInfoFrameLauncher {

	public static void main(String[] args) {
		
		// Initialize logger properties
		PropertyConfigurator.configure(GeneralConfig.LOG_PROPERTIES_PATH);
		System.setProperty("java.library.path", GeneralConfig.RESOURCES_DIR + "lib");
					
		HwInfoFrame frame = new HwInfoFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Hardware info - "+GeneralConfig.APPLICATION_NAME);
		// fits its content (scroll bars if the screen is not enough), on the last monitor used by the application
		frame.showOnAppScreen(null);

		// START CHECKING FOR EXTERNAL SIGNAL to close
		frame.startExternalSignalCloseFlagCheck();
	}
}
