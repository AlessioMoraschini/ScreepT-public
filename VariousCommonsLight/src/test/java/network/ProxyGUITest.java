package network;

import java.awt.Dialog.ModalityType;
import java.net.URL;

import various.common.light.network.http.HttpHelper;
import various.common.light.network.http.HttpHelper.HttpRequestMetod;
import various.common.light.network.proxy.ProxySettingsGUI;
import various.common.light.utility.properties.PropertiesManager;

public class ProxyGUITest {

	public static void main(String[] args) {
		
		try {
			
			ProxySettingsGUI.resourcesFilePathsMappings = new PropertiesManager("TEST_FILES/resourcesFilePathsMappings.properties");
					
			new ProxySettingsGUI(ModalityType.TOOLKIT_MODAL, false, "itabrepxyc01p.corp.altengroup.dir;@;3128;@;;@;;@;;@;true;@;");
			
			System.out.println(HttpHelper.is404("https://www.am-design-development.com", HttpRequestMetod.GET));
			
			int fileSize = HttpHelper.getFileSize(new URL("https://www.am-design-development.com/ScreepT/Updates/manifest.properties"));
		
			System.out.println(fileSize);
		} catch (Exception e) {
			e.printStackTrace();
		
		} finally {
			System.exit(0);
		}
	}

}
