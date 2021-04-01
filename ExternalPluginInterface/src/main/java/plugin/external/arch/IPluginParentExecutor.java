package plugin.external.arch;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import plugin.external.root.embedded.test.HellowordPlugin;
import various.common.light.om.SelectionDtoFull;

public interface IPluginParentExecutor<T> extends Comparable<T> {

	final Map<String, Map<String, FunctionExecutor>> functionsMap = new HashMap<>();

	/**
	 * No need to override default implementation
	 */
	public default String getMapKey(String pluginId, FunctionType type) {
		return pluginId + type.name();
	}

	/**
	 * No need to override default implementation
	 */
	public default void registerFunctions(String pluginId, FunctionType type, Map<String, FunctionExecutor> functionMap) {
		functionsMap.put(getMapKey(pluginId, type), functionMap);
	};

	/**
	 * No need to override default implementation
	 */
	public default Set<String> getAvailableFunctions(String pluginId, FunctionType type) {
		return functionsMap.get(getMapKey(pluginId, type)).keySet();
	};

	/**
	 * No need to override default implementation
	 */
	public default Collection<FunctionExecutor> getAvailableExecutors(String pluginId, FunctionType type) {
		return functionsMap.get(getMapKey(pluginId, type)).values();
	};

	/**
	 * No need to override default implementation
	 */
	public default FunctionExecutor getExecutor(String pluginId, FunctionType type, String function) {
		return functionsMap.get(getMapKey(pluginId, type)).get(function);
	};

	/**
	 * OVERRIDE TO PROVIDE FUNCTIONS IMPLEMENTATION:
	 * #### STAND_ALONE ####
	 */
	public default Map<String, FunctionExecutor> getMainStandalonExecutorMap() {
		return new HashMap<String, FunctionExecutor>();
	}


	/**
	 * OVERRIDE TO PROVIDE FUNCTIONS IMPLEMENTATION:
	 * #### FILE_TREE ####
	 */
	public default Map<String, FunctionExecutor> getFileTreeExecutorMap() {
		return new HashMap<String, FunctionExecutor>();
	};


	/**
	 * OVERRIDE TO PROVIDE FUNCTIONS IMPLEMENTATION:
	 * #### STAND_ALONE ####
	 */
	public default Map<String, FunctionExecutor> getTextAreaExecutorMap() {
		return new HashMap<String, FunctionExecutor>();
	};

	public default void initExecutorsMap(String pluginId) {
		registerFunctions(pluginId, FunctionType.STANDALONE_STR_ARGS, getMainStandalonExecutorMap());
		registerFunctions(pluginId, FunctionType.FILE_TREE_RIGHT_CLICK_MENU, getFileTreeExecutorMap());
		registerFunctions(pluginId, FunctionType.TEXT_AREA_RIGHT_CLICK_MENU, getTextAreaExecutorMap());
	};

	///////////// TYPES OF MANAGED FUNCTIONS ////////////////

	public enum FunctionType {
		TEXT_AREA_RIGHT_CLICK_MENU("Right text area click"),
		FILE_TREE_RIGHT_CLICK_MENU("File Tree click"),
		STANDALONE_STR_ARGS("Standalone function");

		public String readableName;

		private FunctionType(String readableName) {
			this.readableName = readableName;
		}
	}

	/////////////// THE EXECUTOR TO ASSOCIATE TO EACH MAP FUNCTION (for each type)
	/////////////// /////////////
	public class FunctionExecutor {

		IPlugin plugin;

		/**
		 * Automatically added main standalone launcher to map using launchMain(args)
		 * method
		 *
		 * @param plugin
		 */
		public FunctionExecutor(IPlugin plugin) {
			this.plugin = plugin != null ? plugin : new HellowordPlugin();
		}

		public boolean executeMain(String[] args) {
			plugin.launchMain(args);
			return true;
		};

		public boolean executeFiles(List<File> files) {
			return true;
		};

		public SelectionDtoFull executeSelectionDto(SelectionDtoFull dto) {
			return null;
		};
	}
}
