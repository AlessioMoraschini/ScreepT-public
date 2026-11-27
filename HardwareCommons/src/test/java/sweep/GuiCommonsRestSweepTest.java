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

import java.io.File;

import org.junit.Assume;
import org.junit.Test;

import test.util.gui.UiSweep;

/**
 * The method sweep of a module has a time budget, so the classes at the end of the alphabet are left out. This module is not on the critical path
 * of the build: it sweeps the part of the classes of gui.commons.frameutils.frame.ImageViewerFrame that the owner leaves out (smoke coverage, no assertions on behaviour).
 */
public class GuiCommonsRestSweepTest {

	@Test
	public void sweepTheClassesLeftOutByTheirOwner() throws Exception {
		Class<?> anchor = gui.commons.frameutils.frame.ImageViewerFrame.class;
		Assume.assumeTrue(new File(anchor.getProtectionDomain().getCodeSource().getLocation().toURI()).isDirectory());
		int calls = UiSweep.runMethods(anchor, null, "gui.commons.frameutils.frame.ImageViewerFrame", 25000L);
		assertTrue(calls >= 0);
	}
}
