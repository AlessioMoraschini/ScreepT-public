package plugin.external.arch;

import java.awt.Window;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.ImageIcon;

public interface IPlugin extends IPluginParentExecutor {

	@Override
	public default Map<String, FunctionExecutor> getMainStandalonExecutorMap() {
		Map<String, FunctionExecutor> mainStandalonExecutorMap = new HashMap<>();
		mainStandalonExecutorMap.put("Launch plugin", new FunctionExecutor(this));

		return mainStandalonExecutorMap;
	}

	//////////// INTERFACE METHODS ////////////

	public default ImageIcon getMenuIcon() {return new ImageIcon();};

	public default Window getWindow() {return null;};

	public default void kill() {};

	public default void initialize() {};

	public default String getTooltip() {return "";};

	public default boolean isTestPlugin() {return false;};

	public String getPluginZipName();

	public String getPluginName();

	public boolean launchMain(String args[]);

	public boolean openFrame(List<File> files);

	public String getID();


	public default String getRandomId() {return UUID.randomUUID().toString();};
}
