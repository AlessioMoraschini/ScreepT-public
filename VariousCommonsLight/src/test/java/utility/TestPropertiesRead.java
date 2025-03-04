package utility;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import testconfig.TestConstants;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.properties.PropertiesManager;

public class TestPropertiesRead {
	
	private static final String sourceFilePropertiesPath = TestConstants.sourceFolderPath + "manifest.properties";
	private static PropertiesManager propertiesManager;
	
	@BeforeClass
	public static void init() {
		propertiesManager = new PropertiesManager(sourceFilePropertiesPath);
	}

	@Test
	@Ignore
	public void testPropertiesRead() {
		String key = "updatesZippedUrl";
		while(!key.isEmpty()) {
			
			String value = propertiesManager.getProperty(key, "NULL");
			new JOptionHelper(null).info("KEY read value is: [" + value + "]", "READ from " + key);
			key = new JOptionHelper(null).askForString("Key for value read", "Insert property key");
		}
	}
}
