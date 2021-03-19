package utility;

import org.junit.Test;

import utility.log.SafeLogger;

public class SafeLoggerTest {
	
	SafeLogger logger = new SafeLogger(SafeLoggerTest.class);

	@Test
	public void testLoggerKo() {
		logger.error("ERROR_TEST");
		logger.error("ERROR_TEST", new Exception("Test exception"));
		
		logger.warn("WARN_TEST");
		logger.warn("WARN_TEST", new Exception("Test exception"));
		
		logger.info("INFO");
		logger.debug("DEBUG");
		
	}
}
