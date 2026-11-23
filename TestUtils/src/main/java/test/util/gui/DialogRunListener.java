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
package test.util.gui;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/** Surefire listener (parent pom): starts the {@link AutoDialogCloser} so no test ever waits for a person */
public class DialogRunListener extends RunListener {

	@Override
	public void testRunStarted(Description description) throws Exception {
		System.setProperty("screept.nativeFileDialog", "false"); // NativeFileChooser: native dialogs cannot be answered by a test
		MemoryPreferences.install(); // the registry of the person running the tests is never touched
		AutoDialogCloser.start();
	}

	@Override
	public void testStarted(Description description) throws Exception {
		AutoDialogCloser.start();
	}
}
