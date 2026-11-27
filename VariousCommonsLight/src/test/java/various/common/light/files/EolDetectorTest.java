/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.utility.string.StringWorker.EOL;

public class EolDetectorTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void theMostFrequentLineEndingWins() {
		assertEquals(EOL.CRLF, EolDetector.detect("a\r\nb\r\n"));
		assertEquals(EOL.LF, EolDetector.detect("a\nb\nc\r\n"));
		assertEquals(EOL.CR, EolDetector.detect("a\rb\r"));
		assertNull(EolDetector.detect("one line"));
		assertNull(EolDetector.detect(""));
	}

	@Test
	public void filesAreReadWithTheirEncoding() throws Exception {
		File file = tmp.newFile("u16.txt");
		Files.write(file.toPath(), "a\nb\n".getBytes(StandardCharsets.UTF_16));
		assertEquals(EOL.LF, EolDetector.detect(file, "UTF-16"));
		assertNull(EolDetector.detect(file, "no-such-charset"));
	}

	@Test
	public void aFirstLineBreakFarFromTheBeginningIsFound() throws Exception {
		File file = tmp.newFile("long.txt");
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < 700 * 1024; i++) {
			text.append('x');
		}
		Files.write(file.toPath(), text.append("\r\n").toString().getBytes(StandardCharsets.UTF_8));
		assertEquals(EOL.CRLF, EolDetector.detect(file, "UTF-8"));
	}
}
