/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
