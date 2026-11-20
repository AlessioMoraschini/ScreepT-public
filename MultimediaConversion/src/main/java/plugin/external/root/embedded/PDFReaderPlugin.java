package plugin.external.root.embedded;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multimedia.conversion.gui.PdfSelector2TextFrame;
import plugin.external.arch.IPluginTextEditor;
import plugin.external.arch.PluginAbstractParent;
import resources.GeneralConfig;
import various.common.light.files.FileVarious;

/////////////////// PLUGIN INTERFACE ////////////////////////

public class PDFReaderPlugin extends PluginAbstractParent implements IPluginTextEditor, plugin.external.arch.IPluginPanelMode {

	public static final String[] availableFunctionsStandAlone = { "Launch PDF Reader" };
	public static final String[] availableFunctionsRightFileTreeClick = { "Open last file of selection" };

	public PDFReaderPlugin() {
		super();
	}

	@Override
	public boolean isTestPlugin() {
		return false;
	}

	@Override
	public String getPluginName() {
		return "Pdf Viewer";
	}

	@Override
	public String getPluginZipName() {
		return "Tesseract.zip";
	}

	@Override
	public String[] getPanelExtensions() {
		return new String[] {"pdf"};
	}

	@Override
	public String getPanelName() {
		return "PDF viewer";
	}

	@Override
	public boolean canOpenInPanel(File file) {
		return file != null && file.isFile() && file.getName().toLowerCase().endsWith(".pdf");
	}

	@Override
	public plugin.external.arch.PluginPanel createPanel(File file) throws Exception {
		return new multimedia.conversion.gui.PdfPanel(file);
	}

	@Override
	public boolean openFrame(List<File> files) {
		PdfSelector2TextFrame pdfParserBox = new PdfSelector2TextFrame(null, null, GeneralConfig.APPLICATION_NAME + " - Pdf2Text");
		pdfParserBox.setVisible(true);
		if (files != null && !files.isEmpty())
			pdfParserBox.loadFile(files.get(0));
		return true;
	}

	@Override
	public boolean launchMain(String[] args) {
		PdfSelector2TextFrame pdfParserBox = new PdfSelector2TextFrame(null, null, GeneralConfig.APPLICATION_NAME + " - Pdf2Text");
		pdfParserBox.setVisible(true);
		// the menu passes {null} when no file is selected (e.g. a new, unsaved tab): just open the empty reader
		if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
			File toLoad = new File(args[0]);
			if (toLoad.isFile())
				pdfParserBox.loadFile(toLoad);
		}

		return true;
	}

	@Override
	public String getDescription() {
		return "[Embedded] Parse images and PDF to extract contained text (A.K.A. OCR). Includes a basic PDF viewer";
	}

	@Override
	public Map<String, FunctionExecutor> getMainStandalonExecutorMap() {
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
				launchMain(FileVarious.filesToStrings(FileVarious.removeNullsAndUnexisting(files)));
				return true;
			}
		});

		return fileTreeExecutorMap;
	}

}
