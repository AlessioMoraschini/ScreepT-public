package utility;
import org.junit.Before;
import org.junit.Test;

import testconfig.TestConstants;
import various.common.light.utility.properties.PropertiesManager;

public class TestProps {

	public static final String testProps = TestConstants.rootTestFolderPath + "resourcesFilePathsMappings.properties";
	public PropertiesManager manager;
	
	@Before
	public void init() {
		manager = new PropertiesManager(testProps);
	}
	
	@Test
	public void testCommentedPropertySave() {
		manager.saveCommentedProperty("MAX_N_OF_ERR_DIALOG_OPENED", "5");
	}
	
//	@Test
//	public void testPropertiesApacheSave() {
//		manager.savePropertiesLayoutApache("MAX_N_OF_ERR_DIALOG_OPENED", "5");
//	}
	
	
}
