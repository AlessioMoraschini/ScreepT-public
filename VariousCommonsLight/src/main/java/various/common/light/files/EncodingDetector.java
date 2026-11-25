package various.common.light.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Guesses the encoding of a text file without any library: byte order mark, then "is it valid UTF-8", else the usual legacy one.
 * Only text that is not plain ASCII needs a guess: an ASCII file reads the same in every encoding, so the one chosen by the user is kept.
 */
public final class EncodingDetector {

	/** What a file that is not UTF-8 most probably is (smart quotes, euro sign, accents) */
	public static final String LEGACY = "windows-1252";
	public static final String DEFAULT = "UTF-8";
	/** Bytes looked at: the beginning of a big file tells enough */
	private static final int SAMPLE = 512 * 1024;

	private EncodingDetector() {
	}

	/** @param fallback encoding chosen by the user: kept when the file does not say (ASCII only, unreadable) */
	public static String detect(File file, String fallback) {
		String chosen = fallback == null || fallback.trim().isEmpty() ? DEFAULT : fallback.trim();
		if (file == null || !file.isFile()) {
			return chosen;
		}
		try (InputStream in = new FileInputStream(file)) {
			byte[] buffer = new byte[SAMPLE];
			int total = 0, read;
			while (total < buffer.length && (read = in.read(buffer, total, buffer.length - total)) > 0) {
				total += read;
			}
			return detect(buffer, total, chosen, total < buffer.length);
		} catch (IOException e) {
			return chosen;
		}
	}

	/** @param complete false when the bytes are only the beginning of the file: a character cut at the end is not an error */
	public static String detect(byte[] bytes, int length, String fallback, boolean complete) {
		String chosen = fallback == null || fallback.trim().isEmpty() ? DEFAULT : fallback.trim();
		if (length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
			return "UTF-8";
		}
		if (length >= 4 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE && bytes[2] == 0 && bytes[3] == 0) {
			return "UTF-32LE";
		}
		if (length >= 4 && bytes[0] == 0 && bytes[1] == 0 && (bytes[2] & 0xFF) == 0xFE && (bytes[3] & 0xFF) == 0xFF) {
			return "UTF-32BE";
		}
		if (length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
			return "UTF-16LE";
		}
		if (length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
			return "UTF-16BE";
		}
		boolean ascii = true;
		for (int i = 0; i < length; i++) {
			if (bytes[i] < 0) {
				ascii = false;
				break;
			}
		}
		if (ascii) {
			return chosen;
		}
		if (isValidUtf8(bytes, length, complete)) {
			return "UTF-8";
		}
		// not UTF-8: the legacy encoding, unless the user chose another single byte one on purpose
		return chosen.toUpperCase(Locale.ROOT).startsWith("UTF") ? LEGACY : chosen;
	}

	static boolean isValidUtf8(byte[] bytes, int length, boolean complete) {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		ByteBuffer in = ByteBuffer.wrap(bytes, 0, length);
		CharBuffer out = CharBuffer.allocate(8192);
		while (true) {
			CoderResult result = decoder.decode(in, out, complete);
			if (result.isError()) {
				return false;
			}
			if (result.isOverflow()) {
				out.clear();
				continue;
			}
			return true; // underflow: everything was read (an unfinished character at the cut of a sample is fine)
		}
	}
}
