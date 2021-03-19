package plugin.external;

import java.io.File;
import java.util.List;

public interface IPlugin {

	public default boolean launchMain(String args[]) {return true;};

	public boolean openFrame(List<File> files);

	public void kill();
}
