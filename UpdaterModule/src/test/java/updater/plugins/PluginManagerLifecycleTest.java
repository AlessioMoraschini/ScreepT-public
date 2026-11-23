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
package updater.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** The plugin manager from discovery to removal, against a local web server (see {@link PluginManagerScenario}). */
public class PluginManagerLifecycleTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void discoverInstallUpdateAndRemovePlugins() throws Exception {
		// unzipping a plugin opens a progress window
		org.junit.Assume.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
		ChildJvm r = ChildJvm.run(PluginManagerScenario.class, tmp.getRoot(), 120);
		assertEquals(r.output, 0, r.exitCode);
		assertTrue(r.output, r.output.contains("SCENARIO OK"));
	}
}
