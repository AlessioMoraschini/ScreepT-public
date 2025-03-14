package search;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import os.commons.utils.SysUtils;
import os.commons.utils.SysUtils.Browser;

@Ignore
public class UrlOpenerTest {

	private static String sourceFileUrl;
	private static String sourceUrlRemote = "www.google.com";
	
	@BeforeClass 
	public static void init() throws IOException {
		sourceFileUrl = new File("TEST_FILES/Readme/ReadMe.html").getCanonicalPath();
	}
	
	@Test
	public void testOpenUrl() throws IOException, InterruptedException {
		
		SysUtils.openUrlWithSpecificBrowser(sourceFileUrl, Browser.CHROME);
		SysUtils.openUrlWithSpecificBrowser(sourceFileUrl, Browser.FIREFOX);
		SysUtils.openUrlWithSpecificBrowser(sourceFileUrl, Browser.IE);
	}
	
	@Test
	public void testPreferredBrowserRemote() {
		SysUtils.openUrlInBrowserOrdered(sourceUrlRemote);
	}
}
