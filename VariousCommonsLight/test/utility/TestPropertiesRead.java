package utility;

import org.junit.BeforeClass;
import org.junit.Test;

import gui.dialogs.msg.JOptionHelper;
import testconfig.TestConstants;
import utility.properties.PropertiesManager;

public class TestPropertiesRead {
	
	private static final String sourceFilePropertiesPath = TestConstants.sourceFolderPath + "manifest.properties";
	private static PropertiesManager propertiesManager;
	
	@BeforeClass
	public static void init() {
		propertiesManager = new PropertiesManager(sourceFilePropertiesPath);
	}

	@Test
	public void testPropertiesRead() {
		String key = "updatesZippedUrl";
		while(!key.isEmpty()) {
			
			String value = propertiesManager.getProperty(key, "NULL");
			new JOptionHelper(null).info("KEY read value is: [" + value + "]", "READ from " + key);
			key = new JOptionHelper(null).askForString("Key for value read", "Insert property key");
		}
	}
}
