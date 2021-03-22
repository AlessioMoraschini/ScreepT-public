package plugin.external.arch;

import java.io.File;
import java.util.List;

public interface IPlugin {

	public String getPluginZipName();

	public String getPluginName();

	public default String getTooltip() {return "";};

	public default boolean launchMain(String args[]) {return true;};

	public default boolean isTestPlugin() {return false;};

	public void initialize();

	public boolean openFrame(List<File> files);

	public void kill();

}
