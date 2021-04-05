package plugin.external.root.texteditor;

import java.io.File;
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
		return getFileTreeExecutorMap();
	}
}
