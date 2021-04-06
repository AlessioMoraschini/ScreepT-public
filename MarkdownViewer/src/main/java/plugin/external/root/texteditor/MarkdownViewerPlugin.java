package plugin.external.root.texteditor;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import markdown.viewer.MarkdownViewerFrame;
import plugin.api.AbstractPluginApplicationApi;
import plugin.external.arch.IPluginTextEditor;
import plugin.external.arch.PluginAbstractParent;
import various.common.light.files.FileVarious;
import various.common.light.om.SelectionDtoFull;

public class MarkdownViewerPlugin extends PluginAbstractParent implements IPluginTextEditor {

	public static String TEMP_F_NAME = "MarkdownViewer.txt";

	public static final String[] availableFunctionsStandAlone = {"Launch markdown viewer plugin"};
	public static final String[] availableFunctionsRightTextAreaClick = {"Load as markdown", "Load as html"};
	public static final String[] availableFunctionsRightFileTreeClick = availableFunctionsRightTextAreaClick;

	public static final String currentSessionTempFolder = AbstractPluginApplicationApi.defaultTempFolder + "MarkdownViewer_ScreepT_Plugin/" + new Date().getTime() + "/";

	public MarkdownViewerPlugin() {
		super();
	}

	@Override
	public String getVersion() {
		return "1.0.0";
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
	public List<String> getWarnings() {
		List<String> warnings = new ArrayList<>();
		warnings.add("This plugin is only compatible from ScreepT version 0.9.51");
		return warnings;
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean launchMain(final String[] args) {
		new Thread(()-> {
			MarkdownViewerFrame frame = new MarkdownViewerFrame(
					args.length > 0 ? new File(args[0]) : null,
					args.length > 1 ? new File(args[1]) : null);
			frame.setVisible(true);
		}).start();
		return true;
	}

	@Override
	public boolean openFrame(List<File> files) {
		launchMain(FileVarious.filesToStrings(files));
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
				String markdown = files!= null && files.size() > 0 ? FileVarious.getCanonicalPathSafe(files.get(0)) : null;
				launchMain(new String[] {null, markdown});
				return true;
			}
		});

		// Compare empty sources
		fileTreeExecutorMap.put(availableFunctionsRightFileTreeClick[1], new FunctionExecutor(this) {
			@Override
			public boolean executeFiles(List<File> files) {
				String html = files!= null && files.size() > 0 ? FileVarious.getCanonicalPathSafe(files.get(0)) : null;
				launchMain(new String[] {html, null});
				return true;
			}
		});

		return fileTreeExecutorMap;
	}

	@Override
	public Map<String, FunctionExecutor> getTextAreaExecutorMap() {
		Map<String, FunctionExecutor> fileTreeExecutorMap = new HashMap<>();

		// Compare selected
		fileTreeExecutorMap.put(availableFunctionsRightTextAreaClick[0], new FunctionExecutor(this) {
			@Override
			public SelectionDtoFull executeSelectionDto(SelectionDtoFull dto) {
				String markdown = dto.file != null ? FileVarious.getCanonicalPathSafe(dto.file) : null;
				launchMain(new String[] {null, markdown});
				return super.executeSelectionDto(dto);
			}
		});

		// Compare empty sources
		fileTreeExecutorMap.put(availableFunctionsRightTextAreaClick[1], new FunctionExecutor(this) {
			@Override
			public SelectionDtoFull executeSelectionDto(SelectionDtoFull dto) {
				String markdown = dto.file != null ? FileVarious.getCanonicalPathSafe(dto.file) : null;
				launchMain(new String[] {markdown, null});
				return super.executeSelectionDto(dto);
			}
		});

		return fileTreeExecutorMap;
	}
}
