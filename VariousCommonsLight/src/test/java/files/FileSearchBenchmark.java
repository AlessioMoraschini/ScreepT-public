package files;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import various.common.light.files.FileWorker;
import various.common.light.utility.time.Benchmarker;

public class FileSearchBenchmark {

	static File rootSearchDir = new File("./../..");

	@Test
	public void compareFileUtilsAndFileWorker() {

		Benchmarker mark = new Benchmarker(false);
		int nFound = 0;

		mark.registerEvent("Starting FileWorker file search");
		nFound = FileWorker.getAllContent(rootSearchDir, "FILE").size();
		mark.registerEvent("FileWorker file search completed with " + nFound + " results!");

		mark.registerEvent("Starting FileUtils file search");
		nFound = FileUtils.listFiles(rootSearchDir, null, true).size();
		mark.registerEvent("FileUtils file search completed with " + nFound + " results!");

		System.out.println(mark.getSortedReportString());

	}
}
