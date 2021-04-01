package plugin.external.arch;

import java.awt.Window;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.ImageIcon;

/**
 * Implements this or a subclass, and put the implementation under the right package.
 *
 * Structure of the plugin must be:
 *
 * plugin.external.root.jar.<ClassName [extends PluginAbstractParent] implements IPlugin>
 *
 * Folder structure:
 *
 *-/EXTERNAL_PLUGINS
 * -- /[getPluginName()]
 * ----- NAME_IPluginImpl.jar
 * ----- /[content]
 *
*TO interact with main application use AbstractPluginApplicationApi.getAvailableProviders(),
*and use its methods to get files or other stuff from application.
**/
public interface IPlugin extends IPluginParentExecutor<IPlugin> {

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
