package various.test;

import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.Test;

import updater.module.gui.PluginManagerGUI;
import updater.module.plugins.PluginDTO;
import updater.module.plugins.PluginManager;

public class TestPlugins {

	static PluginManager manager;
	static PluginManagerGUI managerGUI;
	
	@Before
	public void init() {
		manager = new PluginManager();
	}
	
	@Test
	public void testDownloadPLuginsList() throws Throwable {
		
		manager.discoverLatestPlugins();
		
		SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	        	managerGUI = new PluginManagerGUI(null, manager, new Runnable() {@Override	public void run() {}});
	        	managerGUI.setVisible(true);
	        }
	    });
		
		for(PluginDTO i : manager.getAllFromCache(false)) {
			System.out.println(i + "\n\n");
		}
		
	}
	
	public static void main(String[] args) throws Throwable {
		manager = new PluginManager();
		manager.discoverLatestPlugins();
		
		SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	        	managerGUI = new PluginManagerGUI(null, manager, new Runnable() {@Override	public void run() {}});
	        	managerGUI.setVisible(true);
	        }
	    });
		
		for(PluginDTO i : manager.getAllFromCache(false)) {
			System.out.println(i + "\n\n");
		}
	}
}
