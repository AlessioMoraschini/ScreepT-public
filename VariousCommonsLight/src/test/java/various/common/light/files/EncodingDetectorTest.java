package various.common.light.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import javax.swing.JTextArea;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.utility.string.StringWorker.EOL;

/** The guess of the encoding of a text file, and reading / writing a text area with a given encoding */
public class EncodingDetectorTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String TEXT = "caffè € “ok” più";

	private File file(byte[] bytes) throws Exception {
		File f = tmp.newFile();
		Files.write(f.toPath(), bytes);
		return f;
	}

	@Test
	public void byteOrderMarksWin() throws Exception {
		assertEquals("UTF-8", EncodingDetector.detect(file(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a'}), "ISO-8859-1"));
		assertEquals("UTF-16LE", EncodingDetector.detect(file(new byte[] {(byte) 0xFF, (byte) 0xFE, 'a', 0}), "UTF-8"));
		assertEquals("UTF-16BE", EncodingDetector.detect(file(new byte[] {(byte) 0xFE, (byte) 0xFF, 0, 'a'}), "UTF-8"));
		assertEquals("UTF-32LE", EncodingDetector.detect(file(new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 0, 'a', 0, 0, 0}), "UTF-8"));
		assertEquals("UTF-32BE", EncodingDetector.detect(file(new byte[] {0, 0, (byte) 0xFE, (byte) 0xFF, 0, 0, 0, 'a'}), "UTF-8"));
	}

	@Test
	public void validUtf8IsUtf8WhateverTheUserChose() throws Exception {
		File f = file(TEXT.getBytes(StandardCharsets.UTF_8));
		assertEquals("UTF-8", EncodingDetector.detect(f, "UTF-8"));
		assertEquals("UTF-8", EncodingDetector.detect(f, "ISO-8859-1"));
		assertEquals("UTF-8", EncodingDetector.detect(f, null));
	}

	@Test
	public void textThatIsNotUtf8IsTheLegacyEncodingOrTheOneTheUserChose() throws Exception {
		File f = file(TEXT.getBytes(Charset.forName("windows-1252")));
		assertEquals("windows-1252", EncodingDetector.detect(f, "UTF-8"));
		assertEquals("windows-1252", EncodingDetector.detect(f, ""));
		assertEquals("a single byte encoding chosen on purpose is kept", "ISO-8859-15", EncodingDetector.detect(f, "ISO-8859-15"));
		assertEquals("windows-1252", EncodingDetector.detect(f, "UTF-16LE"));
	}

	@Test
	public void plainAsciiKeepsTheChoiceOfTheUser() throws Exception {
		File f = file("just ascii\nand more".getBytes(StandardCharsets.US_ASCII));
		assertEquals("UTF-8", EncodingDetector.detect(f, "UTF-8"));
		assertEquals("ISO-8859-1", EncodingDetector.detect(f, "ISO-8859-1"));
		assertEquals("empty file", "UTF-8", EncodingDetector.detect(file(new byte[0]), null));
	}

	@Test
	public void missingOrUnreadableFilesGiveTheChoice() {
		assertEquals("UTF-8", EncodingDetector.detect(null, null));
		assertEquals("ISO-8859-1", EncodingDetector.detect(new File(tmp.getRoot(), "nope.txt"), "ISO-8859-1"));
		assertEquals("a folder is not a file", "UTF-8", EncodingDetector.detect(tmp.getRoot(), "UTF-8"));
	}

	@Test
	public void aCharacterCutAtTheEndOfTheSampleIsNotAnError() {
		byte[] utf8 = ("abcè").getBytes(StandardCharsets.UTF_8); // 61 62 63 C3 A8
		assertEquals("complete: valid", "UTF-8", EncodingDetector.detect(utf8, utf8.length, "UTF-8", true));
		byte[] cut = Arrays.copyOf(utf8, utf8.length - 1); // ends with C3
		assertEquals("only the beginning of a bigger file: still UTF-8", "UTF-8", EncodingDetector.detect(cut, cut.length, "ISO-8859-1", false));
		assertEquals("the whole file ends in the middle of a character: not UTF-8", "windows-1252", EncodingDetector.detect(cut, cut.length, "UTF-8", true));
	}

	@Test
	public void invalidSequencesAreNotUtf8() {
		assertTrue(!EncodingDetector.isValidUtf8(new byte[] {(byte) 0xC3, 0x28}, 2, true));
		assertTrue(!EncodingDetector.isValidUtf8(new byte[] {(byte) 0xE9, 'a'}, 2, true));
		assertTrue(EncodingDetector.isValidUtf8("é€🚀".getBytes(StandardCharsets.UTF_8), 9, true));
	}

	@Test
	public void bigFilesAreJudgedOnTheirBeginning() throws Exception {
		StringBuilder sb = new StringBuilder();
		while (sb.length() < 600000)
			sb.append("riga con accento è\n");
		assertEquals("UTF-8", EncodingDetector.detect(file(sb.toString().getBytes(StandardCharsets.UTF_8)), "ISO-8859-1"));
	}

	@Test
	public void textAreaIsReadAndWrittenWithTheGivenEncoding() throws Exception {
		File f = file(TEXT.getBytes(Charset.forName("windows-1252")));
		JTextArea area = new JTextArea();
		assertTrue(FileWorker.appendFileTxtArea(f, area, true, EOL.LF, "windows-1252"));
		assertEquals(TEXT + "\n", area.getText());

		File out = new File(tmp.getRoot(), "out.txt");
		assertTrue(FileWorker.writeStringToFileEncoded(TEXT, out, true, "windows-1252"));
		assertEquals(TEXT, new String(Files.readAllBytes(out.toPath()), Charset.forName("windows-1252")));
		assertEquals("the same bytes as the original", Arrays.toString(TEXT.getBytes(Charset.forName("windows-1252"))), Arrays.toString(Files.readAllBytes(out.toPath())));

		File utf16 = new File(tmp.getRoot(), "u16.txt");
		assertTrue(FileWorker.writeStringToFileEncoded("﻿" + TEXT, utf16, true, "UTF-16LE"));
		assertEquals("a byte order mark written as the first character comes back as it was", "UTF-16LE", EncodingDetector.detect(utf16, "UTF-8"));
	}
}
