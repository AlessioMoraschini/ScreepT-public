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
package gui.commons.imagestudio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Metadata removal without re-encoding: real JPEG / PNG / GIF files written by ImageIO with hand-built metadata segments
 * injected, WebP built by hand. The cleaned file must keep the image data byte for byte and lose the metadata.
 */
public class MetadataStripperTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final byte[] LAT = { 0, 0, 0, 45, 0, 0, 0, 1, 0, 0, 0, 41, 0, 0, 0, 1, 0, 0, 0x0B, (byte) 0xB8, 0, 0, 0, 100 };
	private static final byte[] LON = { 0, 0, 0, 9, 0, 0, 0, 1, 0, 0, 0, 12, 0, 0, 0, 1, 0, 0, 0x07, (byte) 0xD0, 0, 0, 0, 100 };
	private static final String SECRET_MAKE = "SecretCam";

	// ------------------------------------------------------------------ builders

	private static BufferedImage image(int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Random random = new Random(5);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				img.setRGB(x, y, (x * 255 / w) << 16 | (y * 255 / h) << 8 | (random.nextInt(40) + 100));
			}
		}
		return img;
	}

	private static byte[] encode(BufferedImage img, String format) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue(ImageIO.write(img, format, out));
		return out.toByteArray();
	}

	private static byte[] concat(byte[]... parts) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] p : parts) {
			out.write(p, 0, p.length);
		}
		return out.toByteArray();
	}

	private static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.ISO_8859_1);
	}

	/** TIFF structure: IFD0 with Make, Orientation and (optionally) a GPS IFD with latitude and longitude */
	private static byte[] tiff(boolean littleEndian, int orientation, boolean gps) {
		ByteBuffer b = ByteBuffer.allocate(512).order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
		b.put((byte) (littleEndian ? 'I' : 'M')).put((byte) (littleEndian ? 'I' : 'M'));
		b.putShort((short) 42).putInt(8);
		int entries = gps ? 3 : 2;
		int dataStart = 8 + 2 + 12 * entries + 4;
		b.putShort((short) entries);
		// Make (ASCII, 10 bytes stored after the directories)
		b.putShort((short) 0x010F).putShort((short) 2).putInt(SECRET_MAKE.length() + 1).putInt(dataStart);
		b.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0);
		int gpsIfd = dataStart + SECRET_MAKE.length() + 1;
		if (gps) {
			b.putShort((short) 0x8825).putShort((short) 4).putInt(1).putInt(gpsIfd);
		}
		b.putInt(0);
		b.put(ascii(SECRET_MAKE)).put((byte) 0);
		if (gps) {
			int latAt = gpsIfd + 2 + 24 + 4;
			int lonAt = latAt + 24;
			b.putShort((short) 2);
			b.putShort((short) 2).putShort((short) 5).putInt(3).putInt(latAt);
			b.putShort((short) 4).putShort((short) 5).putInt(3).putInt(lonAt);
			b.putInt(0);
			b.put(littleEndian ? swapWords(LAT) : LAT).put(littleEndian ? swapWords(LON) : LON);
		}
		return Arrays.copyOf(b.array(), b.position());
	}

	/** The 24 bytes of a rational triple written little endian (each 4 byte number reversed) */
	private static byte[] swapWords(byte[] big) {
		byte[] out = new byte[big.length];
		for (int i = 0; i < big.length; i += 4) {
			for (int k = 0; k < 4; k++) {
				out[i + k] = big[i + 3 - k];
			}
		}
		return out;
	}

	private static byte[] jpegSegment(int marker, byte[] payload) {
		int len = payload.length + 2;
		return concat(new byte[] { (byte) 0xFF, (byte) marker, (byte) (len >> 8), (byte) len }, payload);
	}

	private static byte[] exifSegment(byte[] tiff) {
		return jpegSegment(0xE1, concat(new byte[] { 'E', 'x', 'i', 'f', 0, 0 }, tiff));
	}

	private static byte[] xmpSegment(String xml) {
		return jpegSegment(0xE1, concat(ascii("http://ns.adobe.com/xap/1.0/\0"), ascii(xml)));
	}

	private static byte[] iccSegment() {
		byte[] profile = new byte[300];
		Arrays.fill(profile, (byte) 0x33);
		return jpegSegment(0xE2, concat(ascii("ICC_PROFILE\0"), new byte[] { 1, 1 }, profile));
	}

	/** Offset where metadata is inserted: right after SOI and the JFIF header written by ImageIO */
	private static int insertionPoint(byte[] jpeg) {
		if ((jpeg[2] & 0xff) == 0xFF && (jpeg[3] & 0xff) == 0xE0) {
			return 4 + (((jpeg[4] & 0xff) << 8) | (jpeg[5] & 0xff));
		}
		return 2;
	}

	private static byte[] withSegments(byte[] jpeg, byte[]... segments) {
		int at = insertionPoint(jpeg);
		return concat(Arrays.copyOfRange(jpeg, 0, at), concat(segments), Arrays.copyOfRange(jpeg, at, jpeg.length));
	}

	/** Marker codes of the segments before the scan */
	private static List<Integer> markers(byte[] jpeg) {
		List<Integer> out = new ArrayList<Integer>();
		int i = 2;
		while (i + 4 <= jpeg.length && (jpeg[i] & 0xff) == 0xFF) {
			int m = jpeg[i + 1] & 0xff;
			if (m == 0xDA || m == 0xD9) {
				break;
			}
			out.add(m);
			i += 2 + (((jpeg[i + 2] & 0xff) << 8) | (jpeg[i + 3] & 0xff));
		}
		return out;
	}

	private static void assertSamePixels(byte[] a, byte[] b) throws IOException {
		BufferedImage x = ImageIO.read(new ByteArrayInputStream(a));
		BufferedImage y = ImageIO.read(new ByteArrayInputStream(b));
		assertNotNull(x);
		assertNotNull(y);
		assertEquals(x.getWidth(), y.getWidth());
		assertEquals(x.getHeight(), y.getHeight());
		for (int py = 0; py < x.getHeight(); py++) {
			for (int px = 0; px < x.getWidth(); px++) {
				assertEquals("pixel " + px + "," + py, x.getRGB(px, py), y.getRGB(px, py));
			}
		}
	}

	private static boolean contains(byte[] data, byte[] needle) {
		outer:
		for (int i = 0; i + needle.length <= data.length; i++) {
			for (int k = 0; k < needle.length; k++) {
				if (data[i + k] != needle[k]) {
					continue outer;
				}
			}
			return true;
		}
		return false;
	}

	private static MetadataStripper.Options all() {
		return new MetadataStripper.Options();
	}

	private static MetadataStripper.Options location() {
		MetadataStripper.Options o = new MetadataStripper.Options();
		o.mode = MetadataStripper.Mode.LOCATION;
		return o;
	}

	// ------------------------------------------------------------------ EXIF helpers

	@Test
	public void exifHelpersReadBothByteOrders() {
		for (boolean le : new boolean[] { false, true }) {
			byte[] t = tiff(le, 6, true);
			assertEquals(6, MetadataStripper.exifOrientation(t, 0, t.length));
			assertTrue(MetadataStripper.exifHasGps(t, 0, t.length));
			byte[] noGps = tiff(le, 3, false);
			assertEquals(3, MetadataStripper.exifOrientation(noGps, 0, noGps.length));
			assertFalse(MetadataStripper.exifHasGps(noGps, 0, noGps.length));
		}
	}

	@Test
	public void zeroGpsEmptiesTheDirectoryAndTheValuesButKeepsTheRest() {
		for (boolean le : new boolean[] { false, true }) {
			byte[] t = tiff(le, 8, true);
			int length = t.length;
			assertTrue(MetadataStripper.zeroGps(t, 0, t.length));
			assertEquals(length, t.length);
			assertFalse(MetadataStripper.exifHasGps(t, 0, t.length));
			assertEquals(8, MetadataStripper.exifOrientation(t, 0, t.length));
			assertTrue("other tags stay", contains(t, ascii(SECRET_MAKE)));
			assertFalse("latitude gone", contains(t, le ? swapWords(LAT) : LAT));
			assertFalse("longitude gone", contains(t, le ? swapWords(LON) : LON));
			assertFalse("a second pass finds nothing to clean", MetadataStripper.exifHasGps(t, 0, t.length));
		}
	}

	@Test
	public void damagedExifGivesNoAnswerInsteadOfAnException() {
		assertEquals(-1, MetadataStripper.exifOrientation(new byte[0], 0, 0));
		assertEquals(-1, MetadataStripper.exifOrientation(new byte[] { 'M', 'M', 0, 42, 0, 0, 0, 8 }, 0, 8));
		assertFalse(MetadataStripper.exifHasGps(new byte[] { 'I', 'I', 42, 0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F, 1, 2, 3 }, 0, 11));
		assertFalse(MetadataStripper.zeroGps(new byte[4], 0, 4));
		assertFalse(MetadataStripper.zeroGps(new byte[] { 'X', 'X', 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 10));
		byte[] t = tiff(false, 9, false); // orientation out of range
		assertEquals(-1, MetadataStripper.exifOrientation(t, 0, t.length));
		byte[] cut = Arrays.copyOf(tiff(false, 6, true), 30);
		assertFalse(MetadataStripper.exifHasGps(cut, 0, cut.length));
	}

	// ------------------------------------------------------------------ JPEG

	@Test
	public void jpegAllMetadataIsRemovedAndTheImageDataIsUntouched() throws IOException {
		byte[] clean = encode(image(40, 30), "jpg");
		byte[] dirty = withSegments(clean,
				exifSegment(tiff(true, 1, true)),
				xmpSegment("<x:xmpmeta><rdf:Description exif:GPSLatitude=\"45.6\"/></x:xmpmeta>"),
				jpegSegment(0xFE, ascii("secret comment")),
				jpegSegment(0xED, ascii("Photoshop 3.0\0iptc data")),
				jpegSegment(0xE3, ascii("app3 stuff")),
				jpegSegment(0xE0, ascii("JFXX\0thumbnail")));
		MetadataStripper.Result r = MetadataStripper.process(dirty, all());
		assertTrue(r.supported);
		assertEquals("JPEG", r.format);
		assertTrue(r.changed());
		assertTrue(r.gps);
		assertArrayEquals("exactly the original file again", clean, r.output);
		assertEquals(6, r.removed.size());
		assertSamePixels(dirty, r.output);
		assertTrue(r.summary().contains("EXIF"));
		assertTrue(r.summary().contains("GPS"));
		assertTrue(r.summary().contains("XMP"));
		assertFalse(contains(r.output, ascii(SECRET_MAKE)));
	}

	@Test
	public void jpegOrientationIsKeptOnRequestAndOnlyWhenItMatters() throws IOException {
		byte[] clean = encode(image(32, 32), "jpg");
		byte[] sideways = withSegments(clean, exifSegment(tiff(false, 6, true)));
		MetadataStripper.Result kept = MetadataStripper.process(sideways, all());
		assertTrue(kept.changed());
		int at = insertionPoint(kept.output);
		assertEquals(0xE1, kept.output[at + 1] & 0xff);
		int segmentLength = ((kept.output[at + 2] & 0xff) << 8) | (kept.output[at + 3] & 0xff);
		byte[] exif = Arrays.copyOfRange(kept.output, at + 10, at + 2 + segmentLength);
		assertEquals(6, MetadataStripper.exifOrientation(exif, 0, exif.length));
		assertFalse("the location is gone", MetadataStripper.exifHasGps(exif, 0, exif.length));
		assertFalse(contains(kept.output, ascii(SECRET_MAKE)));
		assertSamePixels(sideways, kept.output);

		MetadataStripper.Options drop = all();
		drop.keepOrientation = false;
		assertArrayEquals(clean, MetadataStripper.process(sideways, drop).output);

		byte[] upright = withSegments(clean, exifSegment(tiff(false, 1, false)));
		assertArrayEquals("orientation 1 is the default: nothing to keep", clean, MetadataStripper.process(upright, all()).output);
	}

	@Test
	public void jpegColorProfileIsKeptByDefault() throws IOException {
		byte[] clean = encode(image(16, 16), "jpg");
		byte[] withIcc = withSegments(clean, iccSegment());
		MetadataStripper.Result kept = MetadataStripper.process(withIcc, all());
		assertFalse("only a profile: nothing to remove", kept.changed());
		MetadataStripper.Options o = all();
		o.keepIcc = false;
		MetadataStripper.Result dropped = MetadataStripper.process(withIcc, o);
		assertArrayEquals(clean, dropped.output);
		assertTrue(dropped.summary().contains("ICC"));
	}

	@Test
	public void jpegLocationModeZeroesTheGpsInPlace() throws IOException {
		byte[] clean = encode(image(32, 24), "jpg");
		byte[] dirty = withSegments(clean,
				exifSegment(tiff(true, 6, true)),
				xmpSegment("<x:xmpmeta><rdf:Description exif:GPSLongitude=\"9.1\"/></x:xmpmeta>"),
				xmpSegment("<x:xmpmeta><rdf:Description dc:title=\"holiday\"/></x:xmpmeta>"),
				jpegSegment(0xFE, ascii("keep this comment")));
		MetadataStripper.Result r = MetadataStripper.process(dirty, location());
		assertTrue(r.changed());
		assertTrue(r.gps);
		List<Integer> found = markers(r.output);
		found.remove(Integer.valueOf(0xE0)); // the JFIF header written by ImageIO
		assertEquals(Arrays.asList(0xE1, 0xE1, 0xFE), found.subList(0, 3));
		assertTrue("harmless metadata stays", contains(r.output, ascii("holiday")));
		assertTrue(contains(r.output, ascii("keep this comment")));
		assertTrue("the other EXIF tags stay", contains(r.output, ascii(SECRET_MAKE)));
		assertFalse(contains(r.output, swapWords(LAT)));
		assertFalse(contains(r.output, ascii("GPSLongitude")));
		assertEquals("same size: edited in place of the EXIF, one XMP removed", dirty.length - xmpSegment("<x:xmpmeta><rdf:Description exif:GPSLongitude=\"9.1\"/></x:xmpmeta>").length, r.output.length);
		assertSamePixels(dirty, r.output);
	}

	@Test
	public void jpegLocationModeLeavesAFileWithoutGpsAlone() throws IOException {
		byte[] clean = encode(image(16, 16), "jpg");
		byte[] dirty = withSegments(clean, exifSegment(tiff(true, 6, false)), jpegSegment(0xFE, ascii("c")));
		MetadataStripper.Result r = MetadataStripper.process(dirty, location());
		assertFalse(r.changed());
		assertNull(r.output);
		assertFalse(r.gps);
	}

	@Test
	public void jpegDataAfterTheEndOfTheImageIsDroppedOrKept() throws IOException {
		byte[] clean = encode(image(16, 16), "jpg");
		byte[] withTail = concat(clean, ascii("HIDDEN-VIDEO-PAYLOAD"));
		MetadataStripper.Result r = MetadataStripper.process(withTail, all());
		assertArrayEquals(clean, r.output);
		assertTrue(r.summary().contains("Data after"));
		MetadataStripper.Options keep = all();
		keep.removeTrailing = false;
		assertFalse(MetadataStripper.process(withTail, keep).changed());
		assertFalse("location mode never touches the tail", MetadataStripper.process(withTail, location()).changed());
	}

	@Test
	public void jpegWithNothingToRemoveIsReportedAsUnchanged() throws IOException {
		byte[] clean = encode(image(20, 20), "jpg");
		MetadataStripper.Result r = MetadataStripper.process(clean, all());
		assertFalse(r.changed());
		assertEquals("none", r.summary());
		assertTrue(r.removed.isEmpty());
	}

	@Test
	public void jpegCleaningTwiceChangesNothingMore() throws IOException {
		byte[] dirty = withSegments(encode(image(24, 24), "jpg"), exifSegment(tiff(false, 3, true)), jpegSegment(0xFE, ascii("x")));
		byte[] once = MetadataStripper.process(dirty, all()).output;
		assertNotNull(once);
		assertFalse(MetadataStripper.process(once, all()).changed());
	}

	@Test
	public void keptOrientationIsNotReportedAgainOnPngAndWebp() throws IOException {
		byte[] png = pngWithChunks(encode(image(16, 16), "png"), pngChunk("eXIf", tiff(false, 6, true)));
		byte[] pngOnce = MetadataStripper.process(png, all()).output;
		assertNotNull(pngOnce);
		assertFalse(MetadataStripper.process(pngOnce, all()).changed());
		byte[] webp = webp(0x08, riffChunk("VP8 ", new byte[] { 1, 2, 3, 4 }), riffChunk("EXIF", concat(ascii("Exif  "), tiff(false, 6, true))));
		byte[] webpOnce = MetadataStripper.process(webp, all()).output;
		assertNotNull(webpOnce);
		assertFalse(MetadataStripper.process(webpOnce, all()).changed());
	}

	// ------------------------------------------------------------------ PNG

	private static byte[] pngChunk(String type, byte[] data) {
		ByteBuffer b = ByteBuffer.allocate(12 + data.length);
		b.putInt(data.length).put(ascii(type)).put(data);
		CRC32 crc = new CRC32();
		crc.update(b.array(), 4, 4 + data.length);
		b.putInt((int) crc.getValue());
		return b.array();
	}

	/** Chunks of a PNG file: type -> payload, in order; also checks every CRC */
	private static List<Object[]> chunks(byte[] png) {
		List<Object[]> out = new ArrayList<Object[]>();
		int pos = 8;
		while (pos + 12 <= png.length) {
			int len = ByteBuffer.wrap(png, pos, 4).getInt();
			String type = new String(png, pos + 4, 4, StandardCharsets.ISO_8859_1);
			byte[] data = Arrays.copyOfRange(png, pos + 8, pos + 8 + len);
			CRC32 crc = new CRC32();
			crc.update(png, pos + 4, 4 + len);
			assertEquals("CRC of " + type, (int) crc.getValue(), ByteBuffer.wrap(png, pos + 8 + len, 4).getInt());
			out.add(new Object[] { type, data });
			pos += 12 + len;
		}
		return out;
	}

	private static List<String> types(byte[] png) {
		List<String> out = new ArrayList<String>();
		for (Object[] c : chunks(png)) {
			out.add((String) c[0]);
		}
		return out;
	}

	private static byte[] pngWithChunks(byte[] png, byte[]... extra) {
		int afterIhdr = 8 + 12 + 13;
		return concat(Arrays.copyOfRange(png, 0, afterIhdr), concat(extra), Arrays.copyOfRange(png, afterIhdr, png.length));
	}

	private static byte[] pngData(byte[] png) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (Object[] c : chunks(png)) {
			if (c[0].equals("IDAT")) {
				out.write((byte[]) c[1], 0, ((byte[]) c[1]).length);
			}
		}
		return out.toByteArray();
	}

	@Test
	public void pngAncillaryChunksAreRemovedAndTheImageDataIsUntouched() throws IOException {
		byte[] clean = encode(image(30, 20), "png");
		byte[] xmp = concat(ascii("XML:com.adobe.xmp\0\0\0\0\0"), ascii("<x:xmpmeta exif:GPSLatitude=\"1\"/>"));
		byte[] dirty = pngWithChunks(clean,
				pngChunk("pHYs", new byte[] { 0, 0, 0x0B, 0x13, 0, 0, 0x0B, 0x13, 1 }),
				pngChunk("tEXt", ascii("Comment\0my secret text")),
				pngChunk("tIME", new byte[] { 7, (byte) 0xEA, 11, 16, 12, 0, 0 }),
				pngChunk("iTXt", xmp),
				pngChunk("eXIf", tiff(false, 1, true)),
				pngChunk("prVt", ascii("private extra data")));
		MetadataStripper.Result r = MetadataStripper.process(dirty, all());
		assertEquals("PNG", r.format);
		assertTrue(r.changed());
		assertTrue(r.gps);
		assertEquals(Arrays.asList("IHDR", "pHYs", "IDAT", "IEND"), stripIdat(types(r.output)));
		assertArrayEquals("compressed pixel data identical", pngData(dirty), pngData(r.output));
		assertSamePixels(dirty, r.output);
		assertFalse(contains(r.output, ascii("my secret text")));
		assertEquals(5, r.removed.size());
	}

	private static List<String> stripIdat(List<String> types) {
		List<String> out = new ArrayList<String>();
		for (String t : types) {
			if (!t.equals("IDAT") || !out.contains("IDAT")) {
				out.add(t);
			}
		}
		return out;
	}

	@Test
	public void pngOrientationSurvivesAsAMinimalExifChunk() throws IOException {
		byte[] clean = encode(image(16, 16), "png");
		byte[] dirty = pngWithChunks(clean, pngChunk("eXIf", tiff(true, 8, true)));
		MetadataStripper.Result r = MetadataStripper.process(dirty, all());
		List<String> types = types(r.output);
		assertTrue(types.contains("eXIf"));
		for (Object[] c : chunks(r.output)) {
			if (c[0].equals("eXIf")) {
				byte[] t = (byte[]) c[1];
				assertEquals(8, MetadataStripper.exifOrientation(t, 0, t.length));
				assertFalse(MetadataStripper.exifHasGps(t, 0, t.length));
			}
		}
	}

	@Test
	public void pngLocationModeFixesTheCrcOfTheCleanedExif() throws IOException {
		byte[] clean = encode(image(16, 16), "png");
		byte[] dirty = pngWithChunks(clean, pngChunk("eXIf", tiff(false, 6, true)), pngChunk("tEXt", ascii("Comment\0keep me")));
		MetadataStripper.Result r = MetadataStripper.process(dirty, location());
		assertTrue(r.changed());
		List<Object[]> parsed = chunks(r.output); // asserts every CRC
		assertTrue(types(r.output).contains("tEXt"));
		for (Object[] c : parsed) {
			if (c[0].equals("eXIf")) {
				byte[] t = (byte[]) c[1];
				assertFalse(MetadataStripper.exifHasGps(t, 0, t.length));
				assertEquals(6, MetadataStripper.exifOrientation(t, 0, t.length));
				assertEquals(tiff(false, 6, true).length, t.length);
			}
		}
		assertSamePixels(dirty, r.output);
	}

	@Test
	public void pngXmpWithLocationIsRemovedInLocationMode() throws IOException {
		byte[] clean = encode(image(16, 16), "png");
		byte[] gpsXmp = pngChunk("iTXt", concat(ascii("XML:com.adobe.xmp\0\0\0\0\0"), ascii("<x:xmpmeta exif:GPSLongitude=\"1\"/>")));
		byte[] plainXmp = pngChunk("iTXt", concat(ascii("XML:com.adobe.xmp\0\0\0\0\0"), ascii("<x:xmpmeta dc:title=\"t\"/>")));
		MetadataStripper.Result r = MetadataStripper.process(pngWithChunks(clean, gpsXmp), location());
		assertArrayEquals(clean, r.output);
		assertFalse(MetadataStripper.process(pngWithChunks(clean, plainXmp), location()).changed());
	}

	@Test
	public void pngColorProfileAndGammaFollowTheOptions() throws IOException {
		byte[] clean = encode(image(16, 16), "png");
		byte[] dirty = pngWithChunks(clean, pngChunk("gAMA", new byte[] { 0, 1, (byte) 0x86, (byte) 0xA0 }), pngChunk("iCCP", ascii("profile\0\0data")));
		assertFalse("gamma and profile are kept by default", MetadataStripper.process(dirty, all()).changed());
		MetadataStripper.Options o = all();
		o.keepIcc = false;
		MetadataStripper.Result r = MetadataStripper.process(dirty, o);
		assertEquals(Arrays.asList("IHDR", "gAMA"), types(r.output).subList(0, 2));
		assertFalse(types(r.output).contains("iCCP"));
	}

	@Test
	public void pngDataAfterIendIsDropped() throws IOException {
		byte[] clean = encode(image(16, 16), "png");
		byte[] r = MetadataStripper.process(concat(clean, ascii("TRAILING")), all()).output;
		assertArrayEquals(clean, r);
	}

	// ------------------------------------------------------------------ WebP / GIF / others

	private static byte[] riffChunk(String id, byte[] data) {
		int padded = data.length + (data.length & 1);
		ByteBuffer b = ByteBuffer.allocate(8 + padded).order(ByteOrder.LITTLE_ENDIAN);
		b.put(ascii(id)).putInt(data.length).put(data);
		return b.array();
	}

	private static byte[] webp(int flags, byte[]... chunks) {
		byte[] vp8x = new byte[10];
		vp8x[0] = (byte) flags;
		byte[] body = concat(ascii("WEBP"), riffChunk("VP8X", vp8x), concat(chunks));
		ByteBuffer head = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		head.put(ascii("RIFF")).putInt(body.length);
		return concat(head.array(), body);
	}

	@Test
	public void webpMetadataChunksAreRemovedAndTheFlagsAndSizeFixed() {
		byte[] image = riffChunk("VP8 ", new byte[] { 1, 2, 3, 4, 5, 6, 7 }); // odd size: padded
		byte[] dirty = webp(0x08 | 0x04 | 0x20, image, riffChunk("EXIF", concat(ascii("Exif\0\0"), tiff(false, 1, true))), riffChunk("XMP ", ascii("<x:xmpmeta exif:GPSLatitude=\"1\"/>")), riffChunk("ICCP", new byte[] { 9, 9 }));
		MetadataStripper.Result r = MetadataStripper.process(dirty, all());
		assertEquals("WebP", r.format);
		assertTrue(r.changed());
		assertTrue(r.gps);
		byte[] expected = webp(0x20, image, riffChunk("ICCP", new byte[] { 9, 9 })); // the profile is kept, so is its flag
		assertArrayEquals(expected, r.output);
		assertEquals(r.output.length - 8, ByteBuffer.wrap(r.output, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
	}

	@Test
	public void webpLocationModeKeepsHarmlessChunksAndClearsTheGps() {
		byte[] image = riffChunk("VP8 ", new byte[] { 1, 2, 3, 4 });
		byte[] dirty = webp(0x08 | 0x04, image, riffChunk("EXIF", concat(ascii("Exif\0\0"), tiff(true, 6, true))), riffChunk("XMP ", ascii("<x:xmpmeta dc:title=\"t\"/>")));
		MetadataStripper.Result r = MetadataStripper.process(dirty, location());
		assertTrue(r.changed());
		assertEquals(dirty.length, r.output.length);
		assertTrue(contains(r.output, ascii("dc:title")));
		assertFalse(contains(r.output, swapWords(LAT)));
		assertEquals("flags untouched", 0x0C, r.output[20] & 0xff);
	}

	@Test
	public void webpWithoutMetadataIsLeftAlone() {
		byte[] w = webp(0, riffChunk("VP8 ", new byte[] { 1, 2, 3, 4 }));
		assertFalse(MetadataStripper.process(w, all()).changed());
	}

	@Test
	public void gifCommentsAndApplicationBlocksAreRemovedButAnimationAndImageStay() throws IOException {
		byte[] clean = encode(image(12, 10), "gif");
		int flags = clean[10] & 0xff;
		int pos = 13 + ((flags & 0x80) != 0 ? 3 * (1 << ((flags & 7) + 1)) : 0);
		byte[] comment = concat(new byte[] { 0x21, (byte) 0xFE, 6 }, ascii("secret"), new byte[] { 0 });
		byte[] app = concat(new byte[] { 0x21, (byte) 0xFF, 11 }, ascii("XMP DataXMP"), new byte[] { 3, 1, 2, 3, 0 });
		byte[] netscape = concat(new byte[] { 0x21, (byte) 0xFF, 11 }, ascii("NETSCAPE2.0"), new byte[] { 3, 1, 0, 0, 0 });
		byte[] dirty = concat(Arrays.copyOfRange(clean, 0, pos), netscape, comment, app, Arrays.copyOfRange(clean, pos, clean.length));
		MetadataStripper.Result r = MetadataStripper.process(dirty, all());
		assertEquals("GIF", r.format);
		assertTrue(r.changed());
		assertEquals(2, r.removed.size());
		assertArrayEquals(concat(Arrays.copyOfRange(clean, 0, pos), netscape, Arrays.copyOfRange(clean, pos, clean.length)), r.output);
		assertSamePixels(dirty, r.output);
		assertFalse("GIF carries no GPS", MetadataStripper.process(dirty, location()).changed());
	}

	@Test
	public void gifTrailingBytesAreDropped() throws IOException {
		byte[] clean = encode(image(8, 8), "gif");
		MetadataStripper.Result r = MetadataStripper.process(concat(clean, ascii("TAIL")), all());
		assertArrayEquals(clean, r.output);
	}

	@Test
	public void bmpAndUnknownFormats() throws IOException {
		byte[] bmp = encode(image(8, 8), "bmp");
		MetadataStripper.Result b = MetadataStripper.process(bmp, all());
		assertTrue(b.supported);
		assertEquals("BMP", b.format);
		assertFalse(b.changed());
		for (byte[] junk : new byte[][] { new byte[0], new byte[] { 1 }, ascii("hello, this is not an image at all"), new byte[] { (byte) 0xFF, (byte) 0xD8 } }) {
			MetadataStripper.Result r = MetadataStripper.process(junk, all());
			assertFalse(r.changed());
		}
		MetadataStripper.Result text = MetadataStripper.process(ascii("hello, this is not an image at all"), all());
		assertFalse(text.supported);
		assertEquals("not supported", text.summary());
	}

	// ------------------------------------------------------------------ robustness

	@Test
	public void damagedFilesNeverThrow() throws IOException {
		byte[] jpeg = withSegments(encode(image(24, 16), "jpg"), exifSegment(tiff(true, 6, true)), xmpSegment("<x GPSLatitude/>"), jpegSegment(0xFE, ascii("c")));
		byte[] png = pngWithChunks(encode(image(24, 16), "png"), pngChunk("eXIf", tiff(false, 6, true)), pngChunk("tEXt", ascii("a\0b")));
		byte[] webp = webp(0x0C, riffChunk("VP8 ", new byte[] { 1, 2, 3, 4, 5 }), riffChunk("EXIF", concat(ascii("Exif\0\0"), tiff(false, 6, true))), riffChunk("XMP ", ascii("<x/>")));
		byte[] gif = encode(image(10, 10), "gif");
		byte[][] samples = { jpeg, png, webp, gif };
		Random random = new Random(2026);
		MetadataStripper.Options[] modes = { all(), location() };
		for (byte[] sample : samples) {
			for (int round = 0; round < 1500; round++) {
				byte[] d = sample.clone();
				switch (random.nextInt(3)) {
				case 0:
					d = Arrays.copyOf(d, random.nextInt(d.length + 1));
					break;
				case 1:
					for (int k = 0, n = 1 + random.nextInt(6); k < n; k++) {
						d[random.nextInt(d.length)] = (byte) random.nextInt(256);
					}
					break;
				default:
					int at = random.nextInt(Math.max(1, Math.min(d.length, 120)));
					d[at] = (byte) (random.nextBoolean() ? 0xFF : 0);
					d = Arrays.copyOf(d, Math.max(1, d.length - random.nextInt(20)));
					break;
				}
				for (MetadataStripper.Options o : modes) {
					try {
						MetadataStripper.process(d, o);
					} catch (RuntimeException e) {
						fail("round " + round + " format " + Integer.toHexString(sample[0] & 0xff) + " mode " + o.mode + ": " + e + " at " + e.getStackTrace()[0]);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ files

	@Test
	public void analyzeDoesNotModifyAndProcessFileCleansWriteSafely() throws IOException {
		byte[] dirty = withSegments(encode(image(20, 20), "jpg"), exifSegment(tiff(true, 1, true)));
		File file = new File(tmp.getRoot(), "photo.jpg");
		Files.write(file.toPath(), dirty);
		MetadataStripper.Result analysis = MetadataStripper.analyze(file);
		assertTrue(analysis.changed());
		assertArrayEquals("analyze never writes", dirty, Files.readAllBytes(file.toPath()));

		MetadataStripper.Result r = MetadataStripper.processFile(file, all());
		File target = new File(tmp.getRoot(), "out/deeper/clean.jpg");
		MetadataStripper.writeSafely(r.output, target);
		assertArrayEquals(r.output, Files.readAllBytes(target.toPath()));
		MetadataStripper.writeSafely(dirty, target); // replaces an existing file
		assertArrayEquals(dirty, Files.readAllBytes(target.toPath()));
		for (File f : target.getParentFile().listFiles()) {
			assertFalse("no temp file left: " + f, f.getName().endsWith(".tmp"));
		}
	}

	@Test
	public void unsupportedFilesAreNotRewrittenUnlessAskedAndPossible() throws IOException {
		File file = new File(tmp.getRoot(), "notes.tiff");
		Files.write(file.toPath(), ascii("definitely not a tiff"));
		MetadataStripper.Options o = all();
		o.reencodeOthers = true;
		MetadataStripper.Result r = MetadataStripper.processFile(file, o);
		assertFalse("ImageIO cannot read it: reported as not supported", r.supported);
		assertNull(r.output);
	}

	@Test
	public void summaryOfAnEmptyResultAndNames() {
		MetadataStripper.Result r = new MetadataStripper.Result();
		assertEquals("none", r.summary());
		r.removed.add("EXIF (1.2 KB, GPS location)");
		r.removed.add("XMP (2 KB)");
		r.removed.add("EXIF (3 KB)");
		r.gps = true;
		assertEquals("EXIF, GPS, XMP", r.summary());
		assertEquals("Everything", MetadataStripper.Mode.ALL.toString());
	}
}
