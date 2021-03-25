package shortcuts;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import utils.SysUtils;

public class TestShortcuts {

	@Test
	public void testCreation() throws IOException {
		File target = new File("TEST_FILES/Readme/ReadMe.html");
		File link = new File("TEST_FILES/ReadMe.lnk");
		
		System.out.println(SysUtils.createShortcut(target, link));
	}
}
