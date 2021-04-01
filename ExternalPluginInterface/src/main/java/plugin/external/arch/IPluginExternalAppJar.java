package plugin.external.arch;

/**
 * Structure of the plugin must be:
 *
 * plugin.external.root.jar.<ClassName [extends PluginAbstractParent] implements IPlugin>
 *
 * Folder structure:
 *
 *-/EXTERNAL_PLUGINS
 * -- /[getPluginName()]
 * ----- NAME_IPluginImpl.jar
 * ----- /[content]/[getJarName()].jar  -> here goes the full content jar export to run on a separate process
 *
 *
*TO interact with main application use AbstractPluginApplicationApi.getAvailableProviders(),
*and use its methods to get files or other stuff from application.
**/
public interface IPluginExternalAppJar extends IPlugin {

	public String getJarName();

	// NB: all methods that create the Executor maps must be implemented calling the provider's implementation of LaunchJar method
}
