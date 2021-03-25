package jarcall;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import os.commons.utils.SysUtils;

public class ExternalJarCallTest {

	Runtime runtime;
	String batchPath = "LauncherHwInfoTool.bat";
	
	@Before
	public void init() {
		runtime = Runtime.getRuntime();
	}
	
	@Test
	public void testJarBatchCall() throws IOException {
		System.out.print("Starting...");
		
		String res = SysUtils.launchBatchOrBashAndWaitForOutput(batchPath, "HWINFOPANEL_STARTED_OK", 10000);
		
		System.out.print(res+"\n\n => Launched :)");
	}
	
}
