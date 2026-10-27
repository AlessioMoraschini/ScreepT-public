package puppynoid.main;
import java.io.IOException;

import puppynoid.gui.StartFrame;
import various.common.light.gui.GUIStyelSettings;
import various.common.light.utility.properties.PropertiesManager;

public class MainPuppynoidRunner {

	public static void main(String[] args) throws IOException {
		
		PropertiesManager.setLoggerEnabled(true);
		// standalone only: inside ScreepT the game keeps the global look and feel
		GUIStyelSettings.setLightNimbus(3);
		new StartFrame();
	}
}
