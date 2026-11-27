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
