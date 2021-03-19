package plugin.external.arch;

import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;

import om.SelectionDtoFull;

public interface IPluginTextEditor extends IPlugin {

	public ImageIcon getMenuIcon();

	public List<String> getAvailableFunctions();

	public SelectionDtoFull triggerFunction(String function, SelectionDtoFull currentTextArea);

	public boolean loadFiles(String function, List<File> files);

	public boolean loadFilesFromPath(String function, List<String> filePaths);

}
