/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com/
 */
package plugin.external.root;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import antilocker.AntilockerGui;
import plugin.external.arch.IPlugin;
import plugin.external.arch.PluginAbstractParent;
import various.common.light.files.FileVarious;

public class AntilockerPlugin extends PluginAbstractParent implements IPlugin {

	public static final String[] availableFunctionsStandAlone = {"Launch Antilocker"};

	public static AntilockerGui gui;

	public AntilockerPlugin() {
		super();
	}

	@Override
	public boolean isTestPlugin() {
		return false;
	}

	@Override
	public String getPluginZipName() {
		return "Tricks_Project.zip";
	}

	@Override
	public String getPluginName() {
		return "Anti screen-locker";
	}

	@Override
	public String getTooltip() {
		return "A tool to move the mouse and click ctrl key each N seconds. Useful to avoid get kicked out from softwares if idle ;)";
	}

	@Override
	public boolean openFrame(List<File> files) {
		AntilockerGui.main(FileVarious.filesToPaths(files));
		SwingUtilities.invokeLater(()->{
			gui = AntilockerGui.gui;
			gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		});

		return true;
	}

	@Override
	public boolean launchMain(String[] args) {
		AntilockerGui.main(args);
		SwingUtilities.invokeLater(()->{
			AntilockerGui.gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		});
		return true;
	}

	@Override
	public void kill() {
		if(gui != null) {
			if(gui.antilocker != null)
				gui.antilocker.abort();

			gui.dispose();
		}
	}

	@Override
	public Map<String, FunctionExecutor> getMainStandalonExecutorMap(){
		Map<String, FunctionExecutor> mainStandalonExecutorMap = new HashMap<>();
		mainStandalonExecutorMap.put(availableFunctionsStandAlone[0], new FunctionExecutor(this));

		return mainStandalonExecutorMap;
	}

}
