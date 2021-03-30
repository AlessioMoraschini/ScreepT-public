package plugin.external.root.texteditor;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;

import plugin.external.arch.IPluginTextEditor;
import various.common.light.files.FileVarious;
import various.common.light.om.SelectionDtoFull;

public class CodeComparePlugin implements IPluginTextEditor {

	public static final String defaultTempFolder = System.getProperty("user.home") + "/ScreeptTemp_CodeCompare/";
	public static final String currentSessionTempFolder = defaultTempFolder + new Date().getTime() + "/";

	JMeldLauncher jMeld;

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
		jMeld = new JMeldLauncher(new String[0]);
	}

	@Override
	public boolean launchMain(final String[] args) {
		new Thread(()-> {
			JMeldLauncher.main(ensureAtLeastTwoFiles(args));
		}).start();
		return true;
	}

	private String[] ensureAtLeastTwoFiles(String args[]) {

		String[] argsValid = new String[args.length > 2 ? args.length : 2];

		if (args != null && args.length == 0) {
			argsValid[0] = createTempFile().getAbsolutePath();
			argsValid[1] = createTempFile().getAbsolutePath();

		} else if (args != null && args.length == 1) {
			argsValid[0] = args[0];
			argsValid[1] = createTempFile().getAbsolutePath();

		} else {
			argsValid[0] = args[0];
			argsValid[1] = args[1];
		}

		return argsValid;
	}

	private File createTempFile() {
		String userHome = currentSessionTempFolder + "CompareTest.txt";
		File home = FileVarious.uniqueJavaObjFile(new File(userHome));
		home.getParentFile().mkdirs();
		try {
			home.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return home;
	}

	@Override
	public boolean openFrame(List<File> files) {

		jMeld = new JMeldLauncher(
				ensureAtLeastTwoFiles(FileVarious.filesToPaths(files)));
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
