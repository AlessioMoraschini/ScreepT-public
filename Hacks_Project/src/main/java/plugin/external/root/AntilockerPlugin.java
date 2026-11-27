/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
	public String getDescription() {
		return "A tool to move the mouse and click ctrl key each N seconds. Useful to avoid get kicked out from softwares if idle ;)";
	}

	@Override
	public boolean openFrame(List<File> files) {
		AntilockerGui.main(FileVarious.filesToStrings(files));
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
