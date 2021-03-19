package plugin.api;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import plugin.external.arch.IPlugin;
import plugin.external.arch.IPluginCrypter;
import plugin.external.arch.IPluginTextEditor;
import utility.log.SafeLogger;

/**
 *
 * <b>NB:</b> all static variables are shared to allow application to easily
 * integrate plugins by providing an available api that implements abstract
 * methods. For this reason, the jar and the application must share static
 * instances.
 *
 * This class is intented to be the application interface to call main app
 * exposed functions. It provides abstractMethods that have to be
 *
 * @author Alessio Moraschini
 */
public abstract class AbstractApplicationApi {

	private static final SafeLogger LOGGER = new SafeLogger(AbstractApplicationApi.class);

	public static final String PACKAGE_PLUGINS_ROOT = "plugin.external.root";
	public static final String PACKAGE_PLUGINS_ROOT_EMBEDDED = "plugin.external.root.embedded";
	public static final String PACKAGE_PLUGINS_TEXT_EDITOR = "plugin.external.root.texteditor";
	public static final String PACKAGE_PLUGINS_CRYPTER = "plugin.external.root.crypter";

	private static Vector<AbstractApplicationApi> availableProviders = new Vector<>(4);

	@SuppressWarnings("unchecked")
	public static Set<IPlugin> getAvailablePlugins(PluginType typeFilter, boolean initialize) {
		try {
			//			Reflections reflect = Reflections.collect(typeFilter.packagePath, (str)->{return false;}, new JavaCodeSerializer());
			Reflections reflect = initReflections(typeFilter.packagePath);
			Set<?> plugins = reflect.getSubTypesOf(typeFilter.pluginClasstype);

			return filterPlugins(
					instantiatePlugins((Set<Class<IPlugin>>) plugins, initialize),
					typeFilter.classFilters);
		} catch (Exception e) {
			LOGGER.error("Cannot load plugins", e);
			throw e;
		}
	};

	public static <T extends IPlugin> Set<IPlugin> filterPlugins(Set<IPlugin> plugins, Class<T>[] classFilters ){

		Set<IPlugin> pluginsFiltered = new HashSet<>();

		for(IPlugin plugin : plugins) {
			for (int i = 0; i < classFilters.length; i++) {
				if (classFilters[i] != null && classFilters[i].isAssignableFrom(plugin.getClass())) {
					pluginsFiltered.add(plugin);
					break;
				}
			}
		}

		return pluginsFiltered;
	}

	private static Reflections initReflections(String packagePath) {
		List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
		classLoadersList.add(ClasspathHelper.contextClassLoader());
		classLoadersList.add(ClasspathHelper.staticClassLoader());

		Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setScanners(new SubTypesScanner(false), new ResourcesScanner())
				.setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
				.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(PACKAGE_PLUGINS_ROOT))));

		return reflections;
	}

	private static Set<IPlugin> instantiatePlugins(Set<Class<IPlugin>> plugins, boolean initialize){

		Set<IPlugin> pluginSet = new HashSet<>();

		if(plugins != null) {
			for(Class<IPlugin> clazz : plugins) {
				try {
					IPlugin plugin = clazz.newInstance();
					if(initialize)
						plugin.initialize();
					pluginSet.add(plugin);
				} catch (InstantiationException | IllegalAccessException e) {
					LOGGER.error("Cannot load plugin of type: " + clazz, e);
				}
			}
		}

		return pluginSet;
	};

	public static final Vector<AbstractApplicationApi> getAvailableProviders() {
		synchronized (availableProviders) {
			return new Vector<AbstractApplicationApi>(availableProviders);
		}
	}

	public static final AbstractApplicationApi getProviderByClassName(String classPathComplete) throws ClassNotFoundException {
		return AbstractApplicationApi.class.cast(Class.forName(classPathComplete));
	}

	public static final void registerProvider(AbstractApplicationApi provider) {
		synchronized (availableProviders) {
			if (!availableProviders.contains(provider)) {
				availableProviders.add(provider);
			}
		}
	}

	public static final boolean removeProvider(AbstractApplicationApi provider) {
		synchronized (availableProviders) {
			if (availableProviders.contains(provider))
				return availableProviders.remove(provider);
			else
				return false;
		}
	}

	// ABSTRACT METHODS //

	/**
	 * @param text
	 *            the text to load
	 * @return Return message
	 */
	public abstract String loadText(String text);

	/**
	 * @param files
	 *            the files to load
	 * @return Return message
	 */
	public abstract String loadFiles(List<File> files);

	/**
	 * @param plugin
	 *            the plugin to search folder for
	 * @return Return plugin folder path
	 */
	public abstract String getPluginFolderPath(Class<? extends IPlugin> plugin);

	@SuppressWarnings("unchecked")
	public enum PluginType {
		ROOT(PACKAGE_PLUGINS_ROOT, IPlugin.class, new Class[]{IPlugin.class}),
		EMBEDDED(PACKAGE_PLUGINS_ROOT_EMBEDDED, IPlugin.class, new Class[]{IPlugin.class}),
		TEXT_EDITOR(PACKAGE_PLUGINS_TEXT_EDITOR, IPluginTextEditor.class, new Class[]{IPluginTextEditor.class}),
		CRYPTER(PACKAGE_PLUGINS_CRYPTER, IPluginCrypter.class, new Class[]{IPluginCrypter.class});

		public String packagePath;
		public Class<? extends IPlugin> pluginClasstype;
		public Class<? extends IPlugin>[] classFilters;

		private PluginType(String packagePath, Class<? extends IPlugin> pluginClasstype, Class<? extends IPlugin>... classFilters) {
			this.packagePath = packagePath;
			this.pluginClasstype = pluginClasstype;
			this.classFilters = classFilters == null ? new Class[]{pluginClasstype} : classFilters;
		}

	}
}
