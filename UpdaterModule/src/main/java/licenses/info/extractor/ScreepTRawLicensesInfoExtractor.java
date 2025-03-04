package licenses.info.extractor;

import java.io.File;

import org.apache.commons.io.FileUtils;

import various.common.light.files.CustomFileFilters;
import various.common.light.files.FileWorker;

public class ScreepTRawLicensesInfoExtractor extends AbstractLicenseExtractor {

	public static String DEF_SOURCE_FOLDER = "raw_licenses_list/";
	public static String DEF_EXPORT_FOLDER = "/licenses_builds/";

	public static String DEF_OUTPUT_FOLDER_SUFFIX = "generated/";
	public static String DEF_PLUGIN_FOLDER_SUFFIX = "plugins";

	public static String DEF_COMPLETE_XML = "/UTILS_BUILD/complete_license_list.xml";

	public static String MAIN_APP_OUT_XML_NAME = "license_list_screept.xml";

	protected ScreepTRawLicensesInfoExtractor() {
		super(ScreepTRawLicensesInfoExtractor.class);
		completeXml = "." + DEF_COMPLETE_XML;
		baseExportFolder = "." + DEF_EXPORT_FOLDER;
		baseSourceFolder = baseExportFolder + DEF_SOURCE_FOLDER;
		baseWorkOutputFolder = baseSourceFolder + DEF_OUTPUT_FOLDER_SUFFIX;
		screeptLicenseFileName = MAIN_APP_OUT_XML_NAME;
	}

	public static void main(String[] args) throws Exception {
		new ScreepTRawLicensesInfoExtractor().mainMethod(args);
	}

	private String screeptLicenseFileName;
	private String completeXml;

	@Override
	public void initPaths(File root) {

		completeXml = root.getAbsolutePath() + DEF_COMPLETE_XML;
		baseExportFolder = root.getAbsolutePath() + DEF_EXPORT_FOLDER;
		baseSourceFolder = baseExportFolder + DEF_SOURCE_FOLDER;
		baseWorkOutputFolder = baseSourceFolder + DEF_OUTPUT_FOLDER_SUFFIX;

		logger.info("completeXml: " + completeXml);
	}

	@Override
	public void doWork()  throws Exception {

		FileWorker.mkDirs(baseWorkOutputFolder);
		FileWorker.cleanupDirs(baseWorkOutputFolder);

		File rawXmlFolder = new File(baseSourceFolder);
		File[] definitions = rawXmlFolder.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.FILE_ONLY));
		for(File definition : definitions) {
			String outputFile = baseWorkOutputFolder + definition.getName();
			extractFromCompleteList(definition.getAbsolutePath(), outputFile, completeXml);
		}
	}

	@Override
	public void exportResults() throws Exception {

		for (File generated : new File(baseWorkOutputFolder).listFiles()) {
			if (!screeptLicenseFileName.equals(generated.getName())) {
				FileUtils.copyFileToDirectory(generated, new File(baseExportFolder + DEF_PLUGIN_FOLDER_SUFFIX));
				logger.info(generated + " -> copied to -> " + baseExportFolder + DEF_PLUGIN_FOLDER_SUFFIX);
				FileUtils.deleteQuietly(generated);
				logger.info("Main App file deleted after export: " + generated);
			} else {
				FileUtils.copyFileToDirectory(generated, new File(baseExportFolder));
				logger.info(generated + " -> copied to -> " + baseExportFolder);
			}
		}
	}

	private void extractFromCompleteList(String sourceFile, String outFile, String completeListFile) throws Exception {
		logger.info(
				LicensesInfoExtractor.syncFromCompleteList(
						new File(sourceFile), new File(outFile), new File(completeListFile)));
	}
}
