package plugin.external.root.embedded.test;

import java.io.File;
import java.util.List;

import plugin.external.arch.IPlugin;

public class HellowordPlugin implements IPlugin {

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
		return IPlugin.super.launchMain(args);
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
	public String getPluginName() {
		return "Test Plugin - Helloword";
	}

}
