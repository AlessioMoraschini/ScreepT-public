package puppynoid.main;
import java.io.IOException;

import puppynoid.gui.StartFrame;
import various.common.light.gui.GUIStyelSettings;

public class MainRunner {

	public static void main(String[] args) throws IOException {
		
		// standalone only: inside ScreepT the game keeps the global look and feel
		GUIStyelSettings.setLightNimbus(3);
		new StartFrame();
	}
}
