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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import various.common.light.utility.string.StringWorker.EOL;

/** The line ending a text uses: the most frequent one. Null when the text has no line break at all. */
public final class EolDetector {

	/** Characters looked at before deciding, once a line break has been seen */
	private static final int SAMPLE = 512 * 1024;

	private EolDetector() {
	}

	public static EOL detect(CharSequence text) {
		Counter counter = new Counter();
		for (int i = 0; text != null && i < text.length(); i++) {
			counter.add(text.charAt(i));
		}
		return counter.result();
	}

	/** @return null also when the file cannot be read with that encoding */
	public static EOL detect(File file, String encoding) {
		if (file == null || !file.isFile()) {
			return null;
		}
		Counter counter = new Counter();
		try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding != null ? encoding : System.getProperty("file.encoding"))) {
			char[] buffer = new char[8192];
			long total = 0;
			int read;
			while ((total < SAMPLE || !counter.found()) && (read = reader.read(buffer)) > 0) {
				for (int i = 0; i < read; i++) {
					counter.add(buffer[i]);
				}
				total += read;
			}
		} catch (IOException e) {
			return null;
		}
		return counter.result();
	}

	private static final class Counter {
		private int crlf, lf, cr;
		private boolean pendingCr;

		void add(char c) {
			if (c == '\n') {
				if (pendingCr) {
					crlf++;
				} else {
					lf++;
				}
			} else if (pendingCr) {
				cr++;
			}
			pendingCr = c == '\r';
		}

		boolean found() {
			return crlf + lf + cr > 0;
		}

		EOL result() {
			int lastCr = cr + (pendingCr ? 1 : 0);
			if (crlf + lf + lastCr == 0) {
				return null;
			}
			if (crlf >= lf && crlf >= lastCr) {
				return EOL.CRLF;
			}
			return lf >= lastCr ? EOL.LF : EOL.CR;
		}
	}
}
