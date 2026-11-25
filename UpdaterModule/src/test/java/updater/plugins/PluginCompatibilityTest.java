package updater.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import updater.module.plugins.PluginDTO;
import updater.module.plugins.PluginManager;

/** A plugin that asks for a newer ScreepT than the running one is recognised, and the sentence says what to do */
public class PluginCompatibilityTest {

	private final String before = PluginManager.currentAppVersion;

	@After
	public void restore() {
		PluginManager.currentAppVersion = before;
	}

	private static PluginDTO plugin(String min) {
		PluginDTO p = new PluginDTO("1.6.0", "http://x/MARKDOWN_VIEWER.zip", "MARKDOWN_VIEWER.zip", "00");
		p.setMinAppVersion(min);
		return p;
	}

	@Test
	public void olderScreeptGetsTheAdviceNewerOrEqualDoesNot() {
		PluginManager.currentAppVersion = "0.9.86";
		String problem = PluginManager.compatibilityProblem(plugin("0.9.87"));
		assertNotNull(problem);
		assertTrue(problem, problem.contains("MARKDOWN_VIEWER") && problem.contains("1.6.0") && problem.contains("0.9.87") && problem.contains("0.9.86"));
		assertTrue("it says where to update: " + problem, problem.contains(PluginManager.updateMenuName()) && !PluginManager.updateMenuName().endsWith("."));

		PluginManager.currentAppVersion = "0.9.87";
		assertNull(PluginManager.compatibilityProblem(plugin("0.9.87")));
		PluginManager.currentAppVersion = "0.9.100";
		assertNull("100 is more than 87, it is not text order", PluginManager.compatibilityProblem(plugin("0.9.87")));
		PluginManager.currentAppVersion = "1.0.0";
		assertNull(PluginManager.compatibilityProblem(plugin("0.9.87")));
	}

	@Test
	public void whatIsNotClearNeverBlocks() {
		PluginManager.currentAppVersion = "0.9.50";
		assertNull("no minimum declared", PluginManager.compatibilityProblem(plugin("")));
		assertNull(PluginManager.compatibilityProblem(plugin(null)));
		assertNull("unreadable minimum", PluginManager.compatibilityProblem(plugin("soon")));
		assertNull(PluginManager.compatibilityProblem(null));
		PluginManager.currentAppVersion = "";
		assertNull("unknown running version", PluginManager.compatibilityProblem(plugin("9.9.9")));
		PluginManager.currentAppVersion = "Unknown Version";
		assertNull(PluginManager.compatibilityProblem(plugin("9.9.9")));
		assertEquals("0.9.87", plugin(" 0.9.87 ").getMinAppVersion());
	}
}
