package licenses.info.extractor;

import java.io.File;
import java.util.Arrays;

import various.common.light.utility.log.SafeLogger;

public abstract class AbstractLicenseExtractor {

	public static final SafeLogger logger = new SafeLogger(AbstractLicenseExtractor.class);

	private Class<? extends AbstractLicenseExtractor> implementation;

	protected String baseWorkOutputFolder = "";
	protected String baseExportFolder = "";
	protected String baseSourceFolder = "";

	protected AbstractLicenseExtractor(Class<? extends AbstractLicenseExtractor> clazz) {
		implementation = clazz != null ? clazz : AbstractLicenseExtractor.class;
		logger.info("Initialized AbstractLicenseExtractor instance: " + implementation.getCanonicalName());

		baseWorkOutputFolder = "";
		baseExportFolder = "";
		baseSourceFolder = "";
	}

	public static boolean validateRoot(File root) {
		return root != null && root.exists() && root.isDirectory();
	}

	public abstract void doWork() throws Exception;

	public abstract void initPaths(File root);

	public abstract void exportResults() throws Exception;


	/**
	 * Call by passing args:
	 *
	 * @param arg[0]=true -> export results to export folder
	 * @param arg[1]=rootDirectoryPath -> the path used as root of all folders
	 *
	 * @throws Exception
	 **/
	public final void mainMethod(String[] args) throws Exception {

		logger.info(implementation.getCanonicalName() + ".mainMethod(String[] args) -> ARGS: " + Arrays.toString(args));

		ExtractorDto dto = new ExtractorDto(args);

		if(validateRoot(dto.root))
			initPaths(dto.root);

		logCommonData(dto);

		doWork();

		if(dto.exportResults)
			exportResults();
	}

	protected void logCommonData(ExtractorDto dto) {
		if(validateRoot(dto.root))
			logger.info("Paths generated from root [" + dto.root.getAbsolutePath() + "]:");

		logger.info("baseExportFolder: " + baseExportFolder);
		logger.info("baseSourceFolder: " + baseSourceFolder);
		logger.info("baseWorkOutputFolder: " + baseWorkOutputFolder);
	}

	public class ExtractorDto {
		public boolean exportResults;
		public File root;

		public ExtractorDto(String[] args) {

			exportResults = args != null && args.length > 0 ? Boolean.parseBoolean(args[0]) : false;

			File rootPrj = args != null && args.length > 1 ? new File(args[1]) : null;
			if(validateRoot(rootPrj)) {
				root = rootPrj;
			} else {
				root = null;
			}
		}

	}
}
