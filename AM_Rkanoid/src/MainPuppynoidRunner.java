import java.io.IOException;

import gui.StartFrame;
import utility.properties.PropertiesManager;

public class MainPuppynoidRunner {

	public static void main(String[] args) throws IOException {
		
		PropertiesManager.setLoggerEnabled(true);
		new StartFrame();
	}
}
