import java.io.IOException;

import puppynoid.gui.StartFrame;
import various.common.light.utility.properties.PropertiesManager;

public class MainPuppynoidRunner {

	public static void main(String[] args) throws IOException {
		
		PropertiesManager.setLoggerEnabled(true);
		new StartFrame();
	}
}
