package plugin.external.root.texteditor;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import plugin.external.arch.IPluginTextEditor;
import plugin.external.arch.PluginAbstractParent;
import various.common.light.files.FileVarious;
import various.common.light.om.SelectionDtoFull;

public class CodeComparePlugin extends PluginAbstractParent implements IPluginTextEditor {

	public static final String defaultTempFolder = System.getProperty("user.home") + "/ScreeptTemp_CodeCompare/";
	public static final String currentSessionTempFolder = defaultTempFolder + new Date().getTime() + "/";

	public static final String[] availableFunctionsStandAlone = {"Launch code compare plugin"};
	public static final String[] availableFunctionsRightTextAreaClick = {"Compare current file with another", "Compare current file with empty one"};
	public static final String[] availableFunctionsRightFileTreeClick = {"Compare selected files", "Empty source compare"};

	JMeldLauncher jMeld;

	public CodeComparePlugin() {
		super();
	}

	@Override
	public boolean isTestPlugin() {
		return false;
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
			JMeldLauncher.main(ensureAtLeastTwoFiles(args, currentSessionTempFolder));
		}).start();
		return true;
	}

	@Override
	public boolean openFrame(List<File> files) {

		jMeld = new JMeldLauncher(
				ensureAtLeastTwoFiles(FileVarious.filesToPaths(files), currentSessionTempFolder));
		jMeld.run();

		return true;
	}

	@Override
	public ImageIcon getMenuIcon() {
		return null;
	}

	@Override
	public Map<String, FunctionExecutor> getMainStandalonExecutorMap(){
		Map<String, FunctionExecutor> mainStandalonExecutorMap = new HashMap<>();
		mainStandalonExecutorMap.put(availableFunctionsStandAlone[0], new FunctionExecutor(this));

		return mainStandalonExecutorMap;
	}

	@Override
	public Map<String, FunctionExecutor> getFileTreeExecutorMap() {
		Map<String, FunctionExecutor> fileTreeExecutorMap = new HashMap<>();

		// Compare selected
		fileTreeExecutorMap.put(availableFunctionsRightFileTreeClick[0], new FunctionExecutor(this) {
			@Override
			public boolean executeFiles(List<File> files) {
				launchMain(FileVarious.filesToPaths(files));
				return true;
			}
		});

		// Compare empty sources
		fileTreeExecutorMap.put(availableFunctionsRightFileTreeClick[1], new FunctionExecutor(this) {
			@Override
			public boolean executeFiles(List<File> files) {
				launchMain(new String[0]);
				return true;
			}
		});

		return fileTreeExecutorMap;
	}

	@Override
	public Map<String, FunctionExecutor> getTextAreaExecutorMap() {
		Map<String, FunctionExecutor> fileTreeExecutorMap = new HashMap<>();

		// Compare current with another
		fileTreeExecutorMap.put(availableFunctionsRightTextAreaClick[0], new FunctionExecutor(this) {

			@Override
			public SelectionDtoFull executeSelectionDto(SelectionDtoFull dto) {

				List<File> files = new ArrayList<File>();

				if(dto != null && dto.file != null)
					files.add(dto.file);
				else
					files.add(readFile(null, new File("./")));

				files.add(readFile(null, new File("./")));

				launchMain(FileVarious.filesToPaths(files));

				return dto;
			}

		});

		// Compare current with empty file
		fileTreeExecutorMap.put(availableFunctionsRightTextAreaClick[1], new FunctionExecutor(this) {

			@Override
			public SelectionDtoFull executeSelectionDto(SelectionDtoFull dto) {
				if(dto != null && dto.file != null)
					launchMain(new String[] {dto.file.getAbsolutePath()});
				else
					launchMain(new String[0]);

				return dto;
			}

		});


		return fileTreeExecutorMap;
	}
}
