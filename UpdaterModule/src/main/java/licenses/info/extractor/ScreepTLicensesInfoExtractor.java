package licenses.info.extractor;

import java.io.File;

import various.common.light.files.CustomFileFilters;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;

public class ScreepTLicensesInfoExtractor extends AbstractLicenseExtractor {

	public static String DEF_EXPORT_FOLDER = "/ROOT_DIR_APP_FILES/Libraries_licenses/detailedReport";
	public static String DEF_SOURCE_FOLDER = "/licenses_builds/";
	public static String DEF_OUTPUT_FOLDER_SUFFIX = "generated/";
	public static String DEF_PLUGIN_FOLDER_SUFFIX = "plugins/";

	public static String FILE_NAME_MAIN_APP_NO_EXT = "license_list_screept";

	public ScreepTLicensesInfoExtractor() {
		super(ScreepTLicensesInfoExtractor.class);
		baseExportFolder = "." + DEF_EXPORT_FOLDER;
		baseSourceFolder = "." + DEF_SOURCE_FOLDER;
		sourcePluginsFolder = baseSourceFolder + DEF_PLUGIN_FOLDER_SUFFIX;
		baseWorkOutputFolder = baseSourceFolder + DEF_OUTPUT_FOLDER_SUFFIX;
		outPluginsFolder = baseWorkOutputFolder + DEF_PLUGIN_FOLDER_SUFFIX;
	}

	public static void main(String[] args) throws Exception {
		ScreepTLicensesInfoExtractor instance = new ScreepTLicensesInfoExtractor();
		if(args != null && args.length > 2)
			instance.downloadLicenses = Boolean.valueOf(args[2]);
		else
			instance.downloadLicenses = false;

		instance.mainMethod(args);
	}

	private String sourcePluginsFolder;
	private String outPluginsFolder;
	private boolean downloadLicenses;

	@Override
	public void initPaths(File root) {
		baseExportFolder = root.getAbsolutePath() + DEF_EXPORT_FOLDER;
		baseSourceFolder = root.getAbsolutePath() + DEF_SOURCE_FOLDER;
		sourcePluginsFolder = baseSourceFolder + DEF_PLUGIN_FOLDER_SUFFIX;
		baseWorkOutputFolder = baseSourceFolder + DEF_OUTPUT_FOLDER_SUFFIX;
		outPluginsFolder = baseWorkOutputFolder + DEF_PLUGIN_FOLDER_SUFFIX;

		logger.info("sourcePluginsFolder: " + sourcePluginsFolder);
		logger.info("outPluginsFolder: " + outPluginsFolder);
	}

	@Override
	public void doWork() throws Exception {

		FileWorker.mkDirs(sourcePluginsFolder, outPluginsFolder);
		FileWorker.cleanupDirs(baseWorkOutputFolder);

		File screeptSource = new File(baseSourceFolder + FILE_NAME_MAIN_APP_NO_EXT + ".xml");
		exctractHtml(screeptSource.getAbsolutePath(), baseWorkOutputFolder + FILE_NAME_MAIN_APP_NO_EXT + ".html", downloadLicenses);

		File pluginsDefinitionsFolder = new File(sourcePluginsFolder);
		File[] definitions = pluginsDefinitionsFolder.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.FILE_ONLY));
		for(File definition : definitions) {
			File renamedOut = FileVarious.changeExtensionForceDot(definition, "html");
			String outputFile = outPluginsFolder + renamedOut.getName();
			exctractHtml(definition.getAbsolutePath(), outputFile, downloadLicenses);
		}
	}

	@Override
	public void exportResults() throws Exception {
		FileWorker.copyDirectoryContents(new File(baseWorkOutputFolder), new File(baseExportFolder));
		logger.info(baseWorkOutputFolder + " -> content copied into -> " + baseExportFolder);
	}

	private static void exctractHtml(String sourceFile, String outFile, boolean downloadLicenses) throws Exception {
		logger.info("Exctracting html from " + sourceFile + "\n\n" +
				LicensesInfoExtractor.writeLicensesInfo(
						new File(sourceFile), new File(outFile), downloadLicenses));
	}
}
