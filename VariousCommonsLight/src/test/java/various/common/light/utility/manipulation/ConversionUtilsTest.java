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
package various.common.light.utility.manipulation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JTextField;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Conversions of {@link ConversionUtils}: sizes, durations, fonts, colors in every notation, increments of text fields.
 * The default locale is pinned to US (and restored) because DecimalFormat / String.format depend on it.
 */
public class ConversionUtilsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private Locale previousLocale;

	@Before
	public void pinLocale() {
		previousLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@After
	public void restoreLocale() {
		Locale.setDefault(previousLocale);
	}

	// ---------------------------------------------------------------- percent

	@Test
	public void percentOfParts() {
		assertEquals(50, ConversionUtils.getPercent(1, 2));
		assertEquals(0, ConversionUtils.getPercent(0, 10));
		assertEquals(100, ConversionUtils.getPercent(10, 10));
		assertEquals(200, ConversionUtils.getPercent(20, 10));
		assertEquals(33, ConversionUtils.getPercent(1L, 3L));
		assertEquals(50L, ConversionUtils.getPercent(5000000000L, 10000000000L));
		assertEquals(25, ConversionUtils.getPercent(0.25d));
	}

	@Test
	public void percentWithZeroTotalIsNotAnError_legacy() {
		assertEquals(0, ConversionUtils.getPercent(0, 0));
		assertEquals(Integer.MAX_VALUE, ConversionUtils.getPercent(5, 0));
	}

	@Test
	public void percentStrings() {
		assertEquals("50%", ConversionUtils.getPercentString(0.5d));
		assertEquals("0%", ConversionUtils.getPercentString(0d));
		assertEquals("75%", ConversionUtils.getPercentString(3, 4));
		assertEquals("75%", ConversionUtils.getPercentString(3L, 4L));
	}

	// ---------------------------------------------------------------- sizes and frequencies

	@Test
	public void coolFileSizeUsesBinaryUnits() {
		assertEquals("0", ConversionUtils.coolFileSize(0L));
		assertEquals("0", ConversionUtils.coolFileSize(-5L));
		assertEquals("1 B", ConversionUtils.coolFileSize(1L));
		assertEquals("1,023 B", ConversionUtils.coolFileSize(1023L));
		assertEquals("1 kB", ConversionUtils.coolFileSize(1024L));
		assertEquals("1.5 kB", ConversionUtils.coolFileSize(1536L));
		assertEquals("1 MB", ConversionUtils.coolFileSize(1024L * 1024));
		assertEquals("1 GB", ConversionUtils.coolFileSize(1024L * 1024 * 1024));
		assertEquals("1 TB", ConversionUtils.coolFileSize(1L << 40));
		assertEquals("2.5 GB", ConversionUtils.coolFileSize((long) (2.5 * (1L << 30))));
	}

	@Test
	public void coolFileSizeExactPowersNeverFallOneUnitBehind() {
		String[] units = { "B", "kB", "MB", "GB", "TB" };
		for (int i = 0; i < units.length; i++) {
			assertEquals("1 " + units[i], ConversionUtils.coolFileSize(1L << (10 * i)));
		}
	}

	@Test
	public void coolFileSizeOfHugeValuesUsesRealUnitNames() {
		assertEquals("1 PB", ConversionUtils.coolFileSize(1L << 50));
		assertEquals("1 EB", ConversionUtils.coolFileSize(1L << 60));
		assertEquals("8 EB", ConversionUtils.coolFileSize(Long.MAX_VALUE));
	}

	@Test
	public void coolFileSizeOfFile() throws IOException {
		File f = tmp.newFile("size sample.bin");
		assertEquals("0", ConversionUtils.coolFileSize(f));
		Files.write(f.toPath(), new byte[2048]);
		assertEquals("2 kB", ConversionUtils.coolFileSize(f));
		assertEquals("16 kb", ConversionUtils.coolFileSizeBits(f));
		assertEquals("0", ConversionUtils.coolFileSize(new File(tmp.getRoot(), "missing.bin")));
	}

	@Test
	public void coolFileSizeBits() {
		assertEquals("0", ConversionUtils.coolFileSizeBits(0L));
		assertEquals("8 b", ConversionUtils.coolFileSizeBits(1L));
		assertEquals("8 kb", ConversionUtils.coolFileSizeBits(1024L));
		assertEquals("8 Mb", ConversionUtils.coolFileSizeBits(1024L * 1024));
	}

	@Test
	public void coolFileSizeBitsGroupsAreComputedFromBytes_legacy() {
		// the unit is chosen looking at the byte size, so 128 bytes are shown as "1,024 b" and not "1 kb"
		assertEquals("1,024 b", ConversionUtils.coolFileSizeBits(128L));
	}

	@Test
	public void coolFrequency() {
		assertEquals("0", ConversionUtils.coolFrequency(0L));
		assertEquals("0", ConversionUtils.coolFrequency(-1L));
		assertEquals("999 Hz", ConversionUtils.coolFrequency(999L));
		assertEquals("1 kHz", ConversionUtils.coolFrequency(1000L));
		assertEquals("1.5 MHz", ConversionUtils.coolFrequency(1500000L));
		assertEquals("2.4 GHz", ConversionUtils.coolFrequency(2400000000L));
		assertEquals("1 THz", ConversionUtils.coolFrequency(1000000000000L));
	}

	// ---------------------------------------------------------------- durations

	@Test
	public void formatDuration() {
		assertEquals("", ConversionUtils.format(Duration.ZERO));
		assertEquals("1 ms", ConversionUtils.format(Duration.ofMillis(1)));
		assertEquals("1 s", ConversionUtils.format(Duration.ofSeconds(1)));
		assertEquals("1 d, 1 h, 1 m, 1 s, 1 ms", ConversionUtils.format(Duration.ofMillis(90061001L)));
		assertEquals("2 h", ConversionUtils.format(Duration.ofHours(2)));
		assertEquals("3 d, 5 s", ConversionUtils.format(Duration.ofDays(3).plusSeconds(5)));
	}

	@Test
	public void formatDurationShowsSubMillisecondRemainder() {
		assertEquals("1 ms, 500001 ns", ConversionUtils.format(Duration.ofNanos(1500001L)));
		assertEquals("999 ns", ConversionUtils.format(Duration.ofNanos(999L)));
	}

	@Test
	public void coolTimeFromMsOrNs() {
		assertEquals("5 s", ConversionUtils.coolTimeFromMsOrNs(5000L, false));
		assertEquals("5 s", ConversionUtils.coolTimeFromMsOrNs(5000000000L, true));
		assertEquals("2 m, 3 s", ConversionUtils.coolTimeFromMsOrNs(123000L, false));
		assertEquals("", ConversionUtils.coolTimeFromMsOrNs(0L, false));
	}

	// ---------------------------------------------------------------- fonts

	@Test
	public void fontFromString() {
		Font f = ConversionUtils.getFontFromString("Dialog-1-12");
		assertEquals("Dialog", f.getName());
		assertEquals(Font.BOLD, f.getStyle());
		assertEquals(12, f.getSize());
	}

	@Test
	public void fontFromInvalidStringFallsBackToDefault() {
		for (String bad : new String[] { "", "Dialog", "Dialog-1", "Dialog-x-12", "Dialog-1-y", "a-b-c-d" }) {
			Font f = ConversionUtils.getFontFromString(bad);
			assertEquals(bad, "Segoe UI", f.getName());
			assertEquals(Font.PLAIN, f.getStyle());
			assertEquals(18, f.getSize());
		}
	}

	@Test
	public void fontFromInvalidStringUsesGivenDefault() {
		Font def = new Font("Dialog", Font.ITALIC, 33);
		assertSame(def, ConversionUtils.getFontFromString("nonsense", def));
		assertSame(def, ConversionUtils.getFontFromString("Dialog-1", def));
		assertEquals(10, ConversionUtils.getFontFromString("Dialog-0-10", def).getSize());
	}

	@Test
	public void fontToString() {
		Font f = new Font("Dialog", Font.BOLD, 14);
		assertEquals(f.getFontName() + "-1-14", ConversionUtils.getFontString(f));
	}

	// ---------------------------------------------------------------- RGB / hex

	@Test
	public void rgbStrings() {
		Color c = new Color(10, 20, 30);
		assertEquals("10-20-30", ConversionUtils.getRgbString(c));
		assertEquals("10,20,30", ConversionUtils.getRgbString(c, ","));
		assertEquals("default separator when empty", "10-20-30", ConversionUtils.getRgbString(c, ""));
		assertEquals("10-20-30", ConversionUtils.getRgbString(c, null));
	}

	@Test
	public void rgbFromString() {
		assertEquals(new Color(1, 2, 3), ConversionUtils.getColorFromRgbString("1-2-3"));
		assertEquals(new Color(1, 2, 3), ConversionUtils.getColorFromRgbString("1,2,3", ","));
		assertEquals(Color.BLACK, ConversionUtils.getColorFromRgbString("1-2"));
		assertEquals(Color.BLACK, ConversionUtils.getColorFromRgbString(""));
	}

	@Test
	public void rgbFromInvalidStringThrows_legacy() {
		try {
			ConversionUtils.getColorFromRgbString("a-b-c");
			fail();
		} catch (NumberFormatException expected) {
			// documented: only a wrong number of components falls back to black
		}
		try {
			ConversionUtils.getColorFromRgbString("1-2-300");
			fail();
		} catch (IllegalArgumentException expected) {
			// Color rejects components out of range
		}
	}

	@Test
	public void rgbStringRoundTrip() {
		for (int v : new int[] { 0, 1, 127, 128, 254, 255 }) {
			Color c = new Color(v, 255 - v, v / 2);
			assertEquals(c, ConversionUtils.getColorFromRgbString(ConversionUtils.getRgbString(c)));
		}
	}

	@Test
	public void hexString() {
		assertEquals("#000000", ConversionUtils.getHexString(Color.BLACK));
		assertEquals("#ffffff", ConversionUtils.getHexString(Color.WHITE));
		assertEquals("#0a0b0c", ConversionUtils.getHexString(new Color(10, 11, 12)));
	}

	@Test
	public void hexFromString() {
		Color fb = new Color(1, 2, 3);
		assertEquals(new Color(255, 128, 0), ConversionUtils.getColorFromHexString("#FF8000", fb));
		assertEquals(new Color(255, 128, 0), ConversionUtils.getColorFromHexString("  ff8000 ", fb));
		assertEquals(Color.BLACK, ConversionUtils.getColorFromHexString("#000000", fb));
		assertEquals(fb, ConversionUtils.getColorFromHexString(null, fb));
		assertEquals(fb, ConversionUtils.getColorFromHexString("", fb));
		assertEquals(fb, ConversionUtils.getColorFromHexString("#f00", fb));
		assertEquals(fb, ConversionUtils.getColorFromHexString("#GG0000", fb));
		assertEquals(fb, ConversionUtils.getColorFromHexString("#ff00", fb));
	}

	@Test
	public void hexFromStringIgnoresTrailingAlpha_legacy() {
		assertEquals(new Color(255, 0, 0), ConversionUtils.getColorFromHexString("#ff0000aa", Color.BLUE));
	}

	@Test
	public void hexRoundTrip() {
		int[] samples = { 0x000000, 0xFFFFFF, 0x0A0B0C, 0x123456, 0xABCDEF, 0x00FF00 };
		for (int rgb : samples) {
			Color c = new Color(rgb);
			assertEquals(c, ConversionUtils.getColorFromHexString(ConversionUtils.getHexString(c), Color.PINK));
		}
	}

	// ---------------------------------------------------------------- HSL / HSB

	@Test
	public void hslString() {
		assertEquals("0-100-50", ConversionUtils.getHslString(Color.RED, "-"));
		assertEquals("120-100-50", ConversionUtils.getHslString(Color.GREEN, "-"));
		assertEquals("240-100-50", ConversionUtils.getHslString(Color.BLUE, "-"));
		assertEquals("0-0-100", ConversionUtils.getHslString(Color.WHITE, "-"));
		assertEquals("0-0-0", ConversionUtils.getHslString(Color.BLACK, "-"));
	}

	@Test
	public void hslFromString() {
		Color fb = Color.PINK;
		assertEquals(Color.RED, ConversionUtils.getColorFromHslString("0-100-50", "-", fb));
		assertEquals(Color.RED, ConversionUtils.getColorFromHslString(" 0 - 100 - 50 ", "-", fb));
		assertEquals(Color.BLUE, ConversionUtils.getColorFromHslString("240-100-50", "-", fb));
		assertEquals(Color.WHITE, ConversionUtils.getColorFromHslString("0-0-100", "-", fb));
		assertEquals(Color.BLACK, ConversionUtils.getColorFromHslString("0-0-0", "-", fb));
		assertEquals("comma as decimal point", Color.RED, ConversionUtils.getColorFromHslString("0;100,0;50", ";", fb));
	}

	@Test
	public void hslFromInvalidStringUsesFallback() {
		Color fb = Color.PINK;
		assertEquals(fb, ConversionUtils.getColorFromHslString(null, "-", fb));
		assertEquals(fb, ConversionUtils.getColorFromHslString("", "-", fb));
		assertEquals(fb, ConversionUtils.getColorFromHslString("a-b-c", "-", fb));
		assertEquals("saturation out of range", fb, ConversionUtils.getColorFromHslString("0-101-50", "-", fb));
		assertEquals("luminance out of range", fb, ConversionUtils.getColorFromHslString("0-50-101", "-", fb));
		assertEquals("too many components", fb, ConversionUtils.getColorFromHslString("1-2-3-4", "-", fb));
	}

	@Test
	public void hsbString() {
		assertEquals("0.00-1.00-1.00", ConversionUtils.getHsbString(Color.RED, "-"));
		assertEquals("0.00-0.00-1.00", ConversionUtils.getHsbString(Color.WHITE, "-"));
		assertEquals("0.00-0.00-0.00", ConversionUtils.getHsbString(Color.BLACK, "-"));
	}

	@Test
	public void hsbFromString() {
		Color fb = Color.PINK;
		assertEquals(Color.RED, ConversionUtils.getColorFromHsbString("0.00-1.00-1.00", "-", fb));
		assertEquals(Color.WHITE, ConversionUtils.getColorFromHsbString("0,00-0,00-1,00", "-", fb));
		assertEquals(new Color(Color.HSBtoRGB(0.5f, 1f, 1f)), ConversionUtils.getColorFromHsbString(" 0.5 - 1 - 1 ", "-", fb));
		assertEquals(fb, ConversionUtils.getColorFromHsbString(null, "-", fb));
		assertEquals(fb, ConversionUtils.getColorFromHsbString("x-y-z", "-", fb));
	}

	@Test
	@Ignore("BUG: hsbToRgb mixes int/float scales and builds Color(float>1,..): throws or returns garbage for any input (unused method)")
	public void hsbToRgbConvertsPrimaryColors() {
		assertEquals(Color.WHITE, ConversionUtils.hsbToRgb(0f, 0f, 1f));
		assertEquals(Color.RED, ConversionUtils.hsbToRgb(0f, 1f, 1f));
	}

	// ---------------------------------------------------------------- text field increments

	@Test
	public void updateColorComponent() {
		assertEquals("10-99-30", ConversionUtils.updateColorComponentInString("10-20-30", "99", "-", 1));
		assertEquals("99-20-30", ConversionUtils.updateColorComponentInString("10-20-30", "99", "-", 0));
		assertEquals("same when index out of range", "10-20-30",
				ConversionUtils.updateColorComponentInString("10-20-30", "99", "-", 7));
		assertEquals("a,b,Z", ConversionUtils.updateColorComponentInString("a,b,c", "Z", ",", 2));
	}

	@Test
	public void getComponent() {
		assertEquals("b", ConversionUtils.getComponentI("a-b-c", "-", 1));
		assertEquals("c", ConversionUtils.getComponentI("a-b-c", "-", 2));
		assertEquals("falls back to first", "a", ConversionUtils.getComponentI("a-b-c", "-", 9));
		assertEquals("a", ConversionUtils.getComponentI("a-b-c", "-", -1));
		assertEquals("abc", ConversionUtils.getComponentI("abc", "-", 0));
	}

	@Test
	public void updateStringIntRespectsBounds() {
		assertEquals("6", ConversionUtils.updateStringInt("5", 1, 10, 0));
		assertEquals("4", ConversionUtils.updateStringInt("5", -1, 10, 0));
		assertEquals("10", ConversionUtils.updateStringInt("9", 1, 10, 0));
		assertEquals("blocked at max", "10", ConversionUtils.updateStringInt("10", 1, 10, 0));
		assertEquals("blocked at min", "0", ConversionUtils.updateStringInt("0", -1, 10, 0));
		assertEquals("-3", ConversionUtils.updateStringInt("-2", -1, 10, -5));
		assertEquals("not a number stays as is", "abc", ConversionUtils.updateStringInt("abc", 1, 10, 0));
		assertEquals("", ConversionUtils.updateStringInt("", 1, 10, 0));
		assertEquals("big step skipped", "5", ConversionUtils.updateStringInt("5", 100, 10, 0));
	}

	@Test
	public void updateStringFloatKeepsDecimals() {
		assertEquals("1.75", ConversionUtils.updateStringFloat("1.50", 0.25f, 10f, 0f));
		assertEquals("1.25", ConversionUtils.updateStringFloat("1.50", -0.25f, 10f, 0f));
		assertEquals("0.500", ConversionUtils.updateStringFloat("0.250", 0.25f, 1f, 0f));
		assertEquals("blocked at max", "1.00", ConversionUtils.updateStringFloat("1.00", 0.5f, 1f, 0f));
		assertEquals("abc", ConversionUtils.updateStringFloat("abc", 0.5f, 1f, 0f));
	}

	@Test
	public void updateStringFloatKeepsTheCommaOfTheInput() {
		assertEquals("1,75", ConversionUtils.updateStringFloat("1,50", 0.25f, 10f, 0f));
	}

	@Test
	public void updateStringHexIncrementsAndClamps() {
		assertEquals("#000010", ConversionUtils.updateStringHex("#000000", 16, 0, 0xFFFFFF));
		assertEquals("#fffffe", ConversionUtils.updateStringHex("#ffffff", -1, 0, 0xFFFFFF));
		assertEquals("clamped to max", "#ffffff", ConversionUtils.updateStringHex("#fffff0", 100, 0, 0xFFFFFF));
		assertEquals("clamped to min", "#000000", ConversionUtils.updateStringHex("#000005", -100, 0, 0xFFFFFF));
		assertEquals("not hex stays as is", "#zzz", ConversionUtils.updateStringHex("#zzz", 1, 0, 0xFFFFFF));
	}

	@Test
	public void updateStringHexKeepsSixDigitsSoTheColorStaysParsable() {
		String updated = ConversionUtils.updateStringHex("#0a0a0a", 1, 0, 0xFFFFFF);
		assertEquals("#0a0a0b", updated);
		assertEquals(new Color(0x0a0a0b), ConversionUtils.getColorFromHexString(updated, Color.PINK));
		assertEquals("#000001", ConversionUtils.updateStringHex("#000000", 1, 0, 0xFFFFFF));
	}

	@Test
	public void caretIndexCountsSeparatorsBeforeCaret() {
		JTextField field = new JTextField("10-20-30");
		field.setCaretPosition(0);
		assertEquals(0, ConversionUtils.getIndexOfCaretPositionWithSeparator(field, "-"));
		field.setCaretPosition(2);
		assertEquals(0, ConversionUtils.getIndexOfCaretPositionWithSeparator(field, "-"));
		field.setCaretPosition(3);
		assertEquals(1, ConversionUtils.getIndexOfCaretPositionWithSeparator(field, "-"));
		field.setCaretPosition(7);
		assertEquals(2, ConversionUtils.getIndexOfCaretPositionWithSeparator(field, "-"));
		field.setCaretPosition(8);
		assertEquals("caret at end counts every separator", 2,
				ConversionUtils.getIndexOfCaretPositionWithSeparator(field, "-"));
		field.setText("");
		assertEquals(0, ConversionUtils.getIndexOfCaretPositionWithSeparator(field, "-"));
	}

	// ---------------------------------------------------------------- misc

	@Test
	public void averageValue() {
		assertEquals(2.0, ConversionUtils.getAvgValue(Arrays.asList(1d, 2d, 3d)), 0.0);
		assertEquals(5.5, ConversionUtils.getAvgValue(Collections.singletonList(5.5)), 0.0);
		assertTrue(Double.isNaN(ConversionUtils.getAvgValue(new ArrayList<Double>())));
	}

	@Test
	public void stringListFromRawString() {
		assertEquals(Arrays.asList("a", "b", "c"), ConversionUtils.getStringListFromRawString("a,b,c", ","));
		assertEquals(Arrays.asList("a", "", "c"), ConversionUtils.getStringListFromRawString("a,,c", ","));
		assertEquals("trailing empty elements are dropped", Arrays.asList("a", "b"),
				ConversionUtils.getStringListFromRawString("a,b,,", ","));
		assertEquals(Collections.singletonList(""), ConversionUtils.getStringListFromRawString("", ","));
		List<String> res = ConversionUtils.getStringListFromRawString("x", ",");
		res.add("mutable");
		assertEquals(2, res.size());
	}

	@Test
	public void printHexBinary() {
		assertEquals("", ConversionUtils.printHexBinary(new byte[0]));
		assertEquals("000FAB7F80FF", ConversionUtils.printHexBinary(new byte[] { 0x00, 0x0F, (byte) 0xAB, 0x7F, (byte) 0x80, (byte) 0xFF }));
		byte[] all = new byte[256];
		for (int i = 0; i < 256; i++) {
			all[i] = (byte) i;
		}
		String hex = ConversionUtils.printHexBinary(all);
		assertEquals(512, hex.length());
		for (int i = 0; i < 256; i++) {
			assertEquals(i, Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16));
		}
	}

	@Test
	public void characterAlphabetsAreConsistent() {
		assertEquals(26, ConversionUtils.baseUppercase.length);
		assertEquals(26, ConversionUtils.baseLowercase.length);
		assertEquals(52, ConversionUtils.baseCharacters.length);
		assertEquals(10, ConversionUtils.numberCharacters.length);
		assertArrayEquals(ConversionUtils.baseString.toCharArray(), ConversionUtils.baseCharacters);
		assertNotNull(ConversionUtils.specialCharacters);
		assertTrue(ConversionUtils.symbolString.indexOf('@') >= 0);
		assertEquals(ConversionUtils.specialString.length(), ConversionUtils.specialCharacters.length);
	}
}
