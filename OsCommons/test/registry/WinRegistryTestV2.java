package registry;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import utility.log.SafeLogger;
import utility.string.StringWorker;
import utils.windows.WinRegistryV2;

public class WinRegistryTestV2 {

	private static final String KEY_PATH_TO_WRITE_SHELL = "*\\shell\\Open-with-ScreepT";
	private static final String KEY_SHELL_ICON = "icon";
	// private static final String KEY_SHELL_COMMAND = "command";

	private static final String InstallPathIcon = "C:\\Users\\xx\\Desktop\\SCREEPT_LATEST_V1\\ScreepT.exe";
	// private static final String InstallPathCommand =
	// "\"C:\\Users\\xx\\Desktop\\SCREEPT_LATEST_V1\\ScreepT.exe\" \"%1\"";

	private final static SafeLogger logger = new SafeLogger(WinRegistryTestV2.class);
	static {
		logger.setLoggerAvailable(false);
	}

	@Test
	public void testWinRegistry() {
		try {
			logger.info(WinRegistryV2.valueForKey(WinRegistryV2.HKEY_CLASSES_ROOT, "*\\shell\\Open with ScreepT", "icon"));
			logger.info(WinRegistryV2.valueForKeyPath(WinRegistryV2.HKEY_CLASSES_ROOT, "*\\shell\\Open with ScreepT\\command", ""));

			logger.info(StringWorker.getMapToString(WinRegistryV2.valuesForPath(WinRegistryV2.HKEY_CLASSES_ROOT, "*\\shell\\Open with ScreepT")));
			logger.info(StringWorker.getMapToString(WinRegistryV2.valuesForPath(WinRegistryV2.HKEY_CLASSES_ROOT, "*\\shell\\Open with ScreepT\\command")));

			WinRegistryV2.createKey(WinRegistryV2.HKEY_CLASSES_ROOT, KEY_PATH_TO_WRITE_SHELL);
			WinRegistryV2.writeStringValue(WinRegistryV2.HKEY_CLASSES_ROOT, KEY_PATH_TO_WRITE_SHELL, KEY_SHELL_ICON, InstallPathIcon);
			//			WinRegistryV2.writeStringValue(WinRegistryV2.HKEY_CLASSES_ROOT, KEY_PATH_TO_WRITE_SHELL, KEY_SHELL_COMMAND, InstallPathCommand);

		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | IOException e) {
			logger.error("ERROR!", e);
		}
	}
}
