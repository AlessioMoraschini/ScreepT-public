/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Ultimate Calculator - Copyright (c) 2016 Minhas Kamal (original project)
 * Modifications Copyright (C) 2019-2026 AM-Design-Development - (Alessio Moraschini)
 *
 * Licensed under the MIT License: see the file license/LICENSE.txt of this module.
 */
package sweep;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import test.util.gui.UiSweep;

/** Builds, lays out, paints and disposes every window and panel of the module (never shown): see {@link UiSweep} */
public class UiSweepTest {

	@Test
	public void everyWindowAndPanelThatCanBeBuiltRenders() throws Exception {
		UiSweep.Result r = UiSweep.run(com.minhasKamal.ultimateCalculator.CalculatorMainApp.class, null);
		UiSweep.runMethods(com.minhasKamal.ultimateCalculator.CalculatorMainApp.class, null);
		java.nio.file.Files.write(java.nio.file.Paths.get("target", "uisweep.txt"), (r + " BUILT " + r.builtNames + " FAILED " + r.failedNames).getBytes("UTF-8"));
		assertTrue("the interface thread got stuck on " + r.hung, r.hung.isEmpty());
	}
}
