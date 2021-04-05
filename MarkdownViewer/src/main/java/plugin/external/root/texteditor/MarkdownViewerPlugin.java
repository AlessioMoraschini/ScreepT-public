package plugin.external.root.texteditor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import plugin.api.AbstractPluginApplicationApi;
import plugin.external.arch.IPluginTextEditor;
import plugin.external.arch.PluginAbstractParent;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.om.SelectionDtoFull;
import various.common.light.utility.string.StringWorker.EOL;

public class MarkdownViewerPlugin extends PluginAbstractParent implements IPluginTextEditor {

	public static String TEMP_F_NAME = "MarkdownViewer.txt";

	public static final String[] availableFunctionsStandAlone = {"Launch markdown viewer plugin"};
	public static final String[] availableFunctionsRightTextAreaClick =
		{"View rendered markdown", "Convert html to markdown", "Convert markdown to html"};
	public static final String[] availableFunctionsRightFileTreeClick = availableFunctionsRightTextAreaClick;

	public static final String currentSessionTempFolder = AbstractPluginApplicationApi.defaultTempFolder + "MarkdownViewer_ScreepT_Plugin/" + new Date().getTime() + "/";

	public MarkdownViewerPlugin() {
		super();
	}

	@Override
	public String getVersion() {
		return super.getVersion();
	}

	@Override
	public boolean isTestPlugin() {
		return false;
	}

	@Override
	public String getPluginZipName() {
		return "MARKDOWN_VIEWER.zip";
	}

	@Override
	public String getPluginName() {
		return "Markdown viewer Plugin";
	}

	@Override
	public String getDescription() {
		return "A tool to view markdown files and convert from/to html";
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean launchMain(final String[] args) {
		new Thread(()-> {
		}).start();
		return true;
	}

	@Override
	public boolean openFrame(List<File> files) {

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
				launchMain(FileVarious.filesToStrings(files));
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
					files.add(readFile(null, createTempFile(TEMP_F_NAME, currentSessionTempFolder)));

				files.add(readFile(null, new File(currentSessionTempFolder)));

				launchMain(FileVarious.filesToStrings(files));

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

		// Compare current with empty file
		fileTreeExecutorMap.put(availableFunctionsRightTextAreaClick[2], new FunctionExecutor(this) {

			@Override
			public SelectionDtoFull executeSelectionDto(SelectionDtoFull dto) {
				String fileName = dto != null && dto.file != null ? dto.file.getName() : TEMP_F_NAME;
				File temp = createTempFile("TextArea_Content_" + fileName, currentSessionTempFolder);
				try {
					FileWorker.writeStringToFile(dto.getCompleteString(), temp, true, EOL.defaultEol);
				} catch (IOException e) {
					e.printStackTrace();
				}

				if(dto != null && dto.file != null)
					launchMain(new String[] {dto.file.getAbsolutePath(), temp.getAbsolutePath()});
				else
					launchMain(new String[] {temp.getAbsolutePath()});

				return dto;
			}
		});


		return fileTreeExecutorMap;
	}
}
