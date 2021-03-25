package plugin.external.root.texteditor;

import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;

import plugin.external.arch.IPluginTextEditor;
import various.common.light.files.FileVarious;
import various.common.light.om.SelectionDtoFull;

public class CodeComparePlugin implements IPluginTextEditor {

	JMeldFork jMeld;

	public CodeComparePlugin() {
	}

	@Override
	public String getPluginZipName() {
		return "CODE_COMPARE.zip";
	}

	@Override
	public String getPluginName() {
		return "Code Compare Plugin";
	}

	@Override
	public void initialize() {
		jMeld = new JMeldFork(new String[0]);
	}

	@Override
	public boolean launchMain(final String[] args) {
		new Thread(()-> {
			JMeldFork.main(args);
		}).start();
		return true;
	}

	@Override
	public boolean openFrame(List<File> files) {

		jMeld = new JMeldFork(FileVarious.filesToPaths(files));
		jMeld.run();

		return true;
	}

	@Override
	public void kill() {
		// TODO Auto-generated method stub

	}

	@Override
	public ImageIcon getMenuIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getAvailableFunctions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SelectionDtoFull triggerFunction(String function, SelectionDtoFull currentTextArea) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean loadFiles(String function, List<File> files) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean loadFilesFromPath(String function, List<String> filePaths) {
		// TODO Auto-generated method stub
		return false;
	}

}
