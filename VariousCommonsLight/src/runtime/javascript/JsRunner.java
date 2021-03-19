/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package runtime.javascript;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import files.CustomFileFilters;
import files.FileWorker;

public class JsRunner {
	
	public ScriptEngineManager manager;
	public ScriptEngine engine;
	
	public JsRunner() {
		manager = new ScriptEngineManager();
		engine = manager.getEngineByName("JavaScript");
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
		
		ArrayList<File> librariesToLoad = FileWorker.getAllContent(file, CustomFileFilters.FILE_ONLY);
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
		
		ArrayList<File> librariesToLoad = FileWorker.getAllContent(file, CustomFileFilters.FILE_ONLY);
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
