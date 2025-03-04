package logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;

import various.common.light.files.FileWorker;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.string.StringWorker.EOL;

public class TestSafeLogger {
	
	private static final EOL eol = EOL.LF;
	
	private static File logFile = new File("./TEST_FILES/SAFE_LOGGER_TEST/sample.log");

	@Test
	public void testLoggerFileSave() throws IOException {
		SafeLogger logger = new SafeLogger(TestSafeLogger.class, true, logFile, false);
		logger.setLogFileEol(eol);
		
		assertTrue(logger.canWriteOnFile(false));
		assertTrue(!logger.canWriteOnFile(true));
		logger.setFlagWriteOnFile(true);
		assertTrue(logger.canWriteOnFile(true));
		
		logger.info("");
//		assertLogContentEqual(eol.eol + "INFO: ");
		clear();
		
		logger.info("This is a test log info");
		logger.warn("This is a test log warn");
		logger.error("This is a test log error");
		
//		assertLogContentEqual(eol.eol + "INFO: This is a test log info" + eol.eol
//				+ "WARNING: This is a test log warn" + eol.eol
//				+ "ERROR: This is a test log error");
	}
	
	@After
	public void clear() throws IOException {
		clearFile();
	}
	
	
	private void clearFile() throws IOException {
		FileWorker.writeStringToFile("", logFile, true);
	}
	
	void assertLogContentEqual(String toCheck) throws IOException {
		assertEquals(toCheck, FileWorker.readFileAsString(logFile, null, eol));
	}
}
