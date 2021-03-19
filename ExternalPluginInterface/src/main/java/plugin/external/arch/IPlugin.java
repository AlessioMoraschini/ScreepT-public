package plugin.external.arch;

import java.io.File;
import java.util.List;

public interface IPlugin {

	public String getPluginName();

	public default boolean launchMain(String args[]) {return true;};

	public void initialize();

	public default boolean isTestPlugin() {return false;};

	public boolean openFrame(List<File> files);

	public void kill();

}
