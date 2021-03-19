package plugin.api;

import java.io.File;
import java.util.List;
import java.util.Vector;

import plugin.external.IPlugin;

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

	private static Vector<AbstractApplicationApi> availableProviders = new Vector<>(4);

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
}
