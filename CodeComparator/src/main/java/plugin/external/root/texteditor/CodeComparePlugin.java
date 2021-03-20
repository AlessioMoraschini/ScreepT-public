package plugin.external.root.texteditor;

import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;

import files.FileVarious;
import om.SelectionDtoFull;
import plugin.external.arch.IPluginTextEditor;

public class CodeComparePlugin implements IPluginTextEditor {

	JMeldFork jMeld;

	public CodeComparePlugin() {
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
		String[] filePaths = new String[files != null ? files.size() : 0];
		if(files != null) {
			for(int i = 0; i < files.size(); i++) {
				filePaths[i] = FileVarious.getCanonicalPathSafe(files.get(i));
			}
		}

		jMeld = new JMeldFork(filePaths);
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
