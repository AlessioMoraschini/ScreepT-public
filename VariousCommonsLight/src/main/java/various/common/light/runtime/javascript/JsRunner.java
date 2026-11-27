/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
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
package various.common.light.runtime.javascript;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import various.common.light.files.CustomFileFilters;
import various.common.light.files.FileWorker;

public class JsRunner {

	public ScriptEngineManager manager;
	public ScriptEngine engine;

	public JsRunner() {
		manager = new ScriptEngineManager();
		engine = manager.getEngineByName("JavaScript");
		if (engine == null) {
			engine = manager.getEngineByName("rhino");
		}
	}

	public void initRunnerFromSingleJsLib(String librayPath) throws ScriptException, IOException {
		initRunnerFromSingleJsLib(new File(librayPath));
	}

	public void initRunnerFromSingleJsLib(File librayPath) throws ScriptException, IOException {
			engine.eval(Files.newBufferedReader(Paths.get(librayPath.getAbsolutePath()), StandardCharsets.UTF_8));
	}

	public void initRunnerFromFolderLib(File librayRootPath) {
		File file = librayRootPath;
		if(!file.isDirectory() || file == null || !file.exists()) {
			file = new File("./");
		}

		List<File> librariesToLoad = FileWorker.getAllContent(file, CustomFileFilters.FILE_ONLY);
		for (File lib : librariesToLoad) {
			try {
				engine.eval(Files.newBufferedReader(Paths.get(lib.getAbsolutePath()), StandardCharsets.UTF_8));
			} catch (Exception e) {
			}
		}
	}

	public void initRunnerFromFolderLib(String librayRootPath) {
		File file = new File(librayRootPath);
		if(!file.isDirectory() || file == null || !file.exists()) {
			file = new File("./");
		}

		List<File> librariesToLoad = FileWorker.getAllContent(file, CustomFileFilters.FILE_ONLY);
		for (File lib : librariesToLoad) {
			try {
				engine.eval(Files.newBufferedReader(Paths.get(lib.getAbsolutePath()), StandardCharsets.UTF_8));
			} catch (Exception e) {
			}
		}
	}

	public Object invokeFunction(String function, Object... params) throws NoSuchMethodException, ScriptException {
		Invocable inv = (Invocable) engine;
		// call function from script file
		Object result = inv.invokeFunction(function, params);

		return result;
	}

}
