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
