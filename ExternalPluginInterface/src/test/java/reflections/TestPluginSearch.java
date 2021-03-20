package reflections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import plugin.api.AbstractPluginApplicationApi;
import plugin.api.AbstractPluginApplicationApi.PluginType;
import plugin.external.arch.IPlugin;
import plugin.external.root.embedded.test.HellowordPlugin;

public class TestPluginSearch {

	@Test
	public void testSearch() {
		Set<IPlugin> plugins = AbstractPluginApplicationApi.getAvailablePlugins(PluginType.ROOT, true);
		assertFalse(plugins == null || plugins.isEmpty());
	}

	@Test
	public void testSearchFiltered() {
		Set<IPlugin> plugins = AbstractPluginApplicationApi.getAvailablePlugins(PluginType.EMBEDDED, true);
		assertFalse(plugins == null || plugins.isEmpty());
		assertTrue(plugins.size() == 2);
		assertFalse(plugins.iterator().next() instanceof HellowordPlugin);

		plugins = AbstractPluginApplicationApi.filterPlugins(plugins, new Class[]{HellowordPlugin.class});
		assertTrue(plugins.size() == 1);
		assertTrue(plugins.iterator().next() instanceof HellowordPlugin);
	}
}
