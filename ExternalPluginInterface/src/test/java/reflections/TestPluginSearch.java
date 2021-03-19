package reflections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import plugin.api.AbstractApplicationApi;
import plugin.api.AbstractApplicationApi.PluginType;
import plugin.external.arch.IPlugin;
import plugin.external.root.embedded.test.HellowordPlugin;

public class TestPluginSearch {

	@Test
	public void testSearch() {
		Set<IPlugin> plugins = AbstractApplicationApi.getAvailablePlugins(PluginType.ROOT, true);
		assertFalse(plugins == null || plugins.isEmpty());
	}

	@Test
	public void testSearchTextEditor() {
		Set<IPlugin> plugins = AbstractApplicationApi.getAvailablePlugins(PluginType.EMBEDDED, true);
		assertFalse(plugins == null || plugins.isEmpty());
		assertTrue(plugins.size() == 2);
		assertFalse(plugins.iterator().next() instanceof HellowordPlugin);

		plugins = AbstractApplicationApi.filterPlugins(plugins, new Class[]{HellowordPlugin.class});
		assertTrue(plugins.size() == 1);
		assertTrue(plugins.iterator().next() instanceof HellowordPlugin);
	}
}
