package plugin.external.root.embedded.test;

import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;

import om.SelectionDtoFull;
import plugin.external.arch.IPluginTextEditor;

public class HellowordTextEditorPlugin implements IPluginTextEditor {

	@Override
	public boolean isTestPlugin() {
		System.out.println("IS TEST PLUGIN!");
		return true;
	}



	@Override
	public boolean launchMain(String[] args) {
		System.out.println("LAUNCH MAIN!");
		return IPluginTextEditor.super.launchMain(args);
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
	public List<String> getAvailableFunctions() {
		System.out.println("getAvailableFunctions!");
		return null;
	}



	@Override
	public SelectionDtoFull triggerFunction(String function, SelectionDtoFull currentTextArea) {
		System.out.println("triggerFunction!");
		return null;
	}



	@Override
	public boolean loadFiles(String function, List<File> files) {
		System.out.println("loadFiles!");
		return false;
	}



	@Override
	public boolean loadFilesFromPath(String function, List<String> filePaths) {
		System.out.println("loadFilesFromPath!");
		return false;
	}

	@Override
	public String getPluginName() {
		return "Test Plugin - HellowordTextEditor";
	}

}
