package plugin.external.root.embedded.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import plugin.external.arch.IPlugin;
import plugin.external.arch.IPluginTextEditor;
import various.common.light.utility.string.StringWorker;

public class HellowordTextEditorPlugin implements IPluginTextEditor {

	@Override
	public boolean isTestPlugin() {
		System.out.println("IS TEST PLUGIN!");
		return true;
	}

	@Override
	public String getPluginZipName() {
		return null;
	}

	@Override
	public boolean launchMain(String[] args) {
		System.out.println("LAUNCH MAIN!");
		return false;
	}

	@Override
	public boolean openFrame(List<File> files) {
		System.out.println("HELLO WORLD!");
		return true;
	}

	@Override
	public void kill() {
		System.out.println("KILLED!");
	}

	@Override
	public void initialize() {
		System.out.println("INITIALIZED!");
	}

	@Override
	public ImageIcon getMenuIcon() {
		System.out.println("MENU ICON!");
		return null;
	}

	@Override
	public String getPluginName() {
		return "Test Plugin - HellowordTextEditor";
	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public List<String> getWarnings() {
		return new ArrayList<String>();
	}

	@Override
	public int compareTo(IPlugin o) {

		if(o == null || StringWorker.isEmpty(o.getPluginName()) || StringWorker.isEmpty(getPluginName()))
			return -1;

		if(o.getPluginName().equals(getPluginName()))
			return 0;

		return getPluginName().compareTo(o.getPluginName());
	}

}
