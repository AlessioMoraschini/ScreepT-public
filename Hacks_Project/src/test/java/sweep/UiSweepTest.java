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
package sweep;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import test.util.gui.UiSweep;

/** Builds, lays out, paints and disposes every window and panel of the module (never shown): see {@link UiSweep} */
public class UiSweepTest {

	@Test
	public void everyWindowAndPanelThatCanBeBuiltRenders() throws Exception {
		UiSweep.Result r = UiSweep.run(antilocker.AntiLocker.class, null);
		UiSweep.runMethods(antilocker.AntiLocker.class, null);
		java.nio.file.Files.write(java.nio.file.Paths.get("target", "uisweep.txt"), (r + " BUILT " + r.builtNames + " FAILED " + r.failedNames).getBytes("UTF-8"));
		assertTrue("the interface thread got stuck on " + r.hung, r.hung.isEmpty());
	}
}
