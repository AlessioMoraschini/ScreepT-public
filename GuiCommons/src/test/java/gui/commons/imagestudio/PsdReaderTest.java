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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.Deflater;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** The Photoshop reader against tiny PSD files built by hand (every compression, mode and depth that it supports) */
public class PsdReaderTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	// ------------------------------------------------------------------ builder

	private static byte[] concat(byte[]... parts) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] p : parts) {
			out.write(p, 0, p.length);
		}
		return out.toByteArray();
	}

	private static byte[] be(int bytes, long value) {
		byte[] out = new byte[bytes];
		for (int i = 0; i < bytes; i++) {
			out[i] = (byte) (value >> (8 * (bytes - 1 - i)));
		}
		return out;
	}

	private static byte[] resource(int id, byte[] data) {
		byte[] padded = data.length % 2 == 0 ? data : concat(data, new byte[1]);
		return concat("8BIM".getBytes(StandardCharsets.ISO_8859_1), be(2, id), new byte[] { 0, 0 }, be(4, data.length), padded);
	}

	/** Parameters of a file: everything is a plain field so each test names only what it changes */
	private static final class Spec {
		int version = 1;
		int channels = 3;
		int width = 2;
		int height = 2;
		int depth = 8;
		int mode = 3;
		byte[] modeData = new byte[0];
		byte[] resources = new byte[0];
		byte[] layerContent = new byte[0];
		int compression = 0;
		/** channel after channel, row after row, raw (uncompressed) bytes of the planes */
		byte[] planes = new byte[0];
		boolean truncateImage;
		long resourcesLengthOverride = -1;

		byte[] build() {
			boolean big = version == 2;
			byte[] image;
			int rowBytes = depth == 1 ? (width + 7) / 8 : width * (depth / 8);
			switch (compression) {
			case 1: {
				ByteArrayOutputStream table = new ByteArrayOutputStream();
				ByteArrayOutputStream data = new ByteArrayOutputStream();
				for (int row = 0; row < channels * height; row++) {
					byte[] packed = packBits(Arrays.copyOfRange(planes, row * rowBytes, (row + 1) * rowBytes));
					byte[] len = big ? be(4, packed.length) : be(2, packed.length);
					table.write(len, 0, len.length);
					data.write(packed, 0, packed.length);
				}
				image = concat(be(2, 1), table.toByteArray(), data.toByteArray());
				break;
			}
			case 2:
				image = concat(be(2, 2), deflate(planes));
				break;
			case 3: {
				byte[] predicted = planes.clone();
				for (int row = 0; row < channels * height; row++) {
					int base = row * rowBytes;
					if (depth == 8) {
						for (int i = rowBytes - 1; i >= 1; i--) {
							predicted[base + i] = (byte) (planes[base + i] - planes[base + i - 1]);
						}
					} else {
						for (int i = rowBytes - 2; i >= 2; i -= 2) {
							int cur = ((planes[base + i] & 0xff) << 8) | (planes[base + i + 1] & 0xff);
							int prev = ((planes[base + i - 2] & 0xff) << 8) | (planes[base + i - 1] & 0xff);
							int diff = (cur - prev) & 0xffff;
							predicted[base + i] = (byte) (diff >> 8);
							predicted[base + i + 1] = (byte) diff;
						}
					}
				}
				image = concat(be(2, 3), deflate(predicted));
				break;
			}
			default:
				image = concat(be(2, compression), planes);
				break;
			}
			if (truncateImage) {
				image = Arrays.copyOf(image, Math.max(2, image.length / 2));
			}
			byte[] layers = layerContent.length == 0 ? new byte[0] : layerContent;
			return concat("8BPS".getBytes(StandardCharsets.ISO_8859_1), be(2, version), new byte[6], be(2, channels), be(4, height), be(4, width), be(2, depth), be(2, mode),
					be(4, modeData.length), modeData,
					be(4, resourcesLengthOverride >= 0 ? resourcesLengthOverride : resources.length), resources,
					big ? be(8, layers.length) : be(4, layers.length), layers, image);
		}
	}

	private static byte[] packBits(byte[] row) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int i = 0;
		while (i < row.length) {
			int run = 1;
			while (i + run < row.length && row[i + run] == row[i] && run < 128) {
				run++;
			}
			if (run >= 3) {
				out.write(1 - run);
				out.write(row[i]);
				i += run;
			} else {
				int lit = 0;
				while (i + lit < row.length && lit < 128 && !(i + lit + 2 < row.length && row[i + lit] == row[i + lit + 1] && row[i + lit] == row[i + lit + 2])) {
					lit++;
				}
				out.write(lit - 1);
				out.write(row, i, lit);
				i += lit;
			}
		}
		return out.toByteArray();
	}

	private static byte[] deflate(byte[] data) {
		Deflater d = new Deflater();
		d.setInput(data);
		d.finish();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[256];
		while (!d.finished()) {
			out.write(buf, 0, d.deflate(buf));
		}
		d.end();
		return out.toByteArray();
	}

	private File file(Spec spec) throws IOException {
		File f = new File(tmp.getRoot(), "t" + System.nanoTime() + ".psd");
		Files.write(f.toPath(), spec.build());
		return f;
	}

	/** 2x2 RGB: red, green / blue, white, as three planes */
	private static byte[] rgbPlanes() {
		return new byte[] { (byte) 255, 0, 0, (byte) 255, /* R */ 0, (byte) 255, 0, (byte) 255, /* G */ 0, 0, (byte) 255, (byte) 255 /* B */ };
	}

	private static void assertRgb2x2(BufferedImage img) {
		assertEquals(2, img.getWidth());
		assertEquals(2, img.getHeight());
		assertEquals(0xFFFF0000, img.getRGB(0, 0));
		assertEquals(0xFF00FF00, img.getRGB(1, 0));
		assertEquals(0xFF0000FF, img.getRGB(0, 1));
		assertEquals(0xFFFFFFFF, img.getRGB(1, 1));
	}

	// ------------------------------------------------------------------ compressions

	@Test
	public void rawRgbComposite() throws IOException {
		Spec s = new Spec();
		s.planes = rgbPlanes();
		PsdReader.Psd psd = PsdReader.read(file(s));
		assertRgb2x2(psd.image);
		assertFalse(psd.fromPreview);
		assertEquals(3, psd.channels);
		assertEquals(8, psd.depth);
		assertEquals("RGB", psd.modeName());
		assertEquals(-1, psd.layers);
		assertFalse(psd.big);
	}

	@Test
	public void everyCompressionGivesTheSamePicture() throws IOException {
		Spec s = new Spec();
		s.planes = rgbPlanes();
		for (int compression = 0; compression <= 3; compression++) {
			s.compression = compression;
			assertRgb2x2(PsdReader.read(file(s)).image);
		}
	}

	@Test
	public void largeFormatUsesWiderFields() throws IOException {
		Spec s = new Spec();
		s.version = 2;
		s.planes = rgbPlanes();
		s.layerContent = concat(be(8, 10), be(2, 0xFFFD), new byte[8]); // 3 layers (negative: the first has transparency)
		for (int compression : new int[] { 0, 1 }) {
			s.compression = compression;
			PsdReader.Psd psd = PsdReader.read(file(s));
			assertTrue(psd.big);
			assertRgb2x2(psd.image);
			assertEquals(3, psd.layers);
			assertTrue(psd.describe().contains("large format"));
		}
	}

	@Test
	public void rleHandlesLongRunsAndLiterals() throws IOException {
		Spec s = new Spec();
		s.width = 300;
		s.height = 3;
		s.channels = 1;
		s.mode = 1;
		s.compression = 1;
		byte[] planes = new byte[300 * 3];
		for (int i = 0; i < planes.length; i++) {
			planes[i] = (byte) (i < 300 ? 77 : (i < 600 ? i * 7 : (i / 5) * 3));
		}
		s.planes = planes;
		BufferedImage img = PsdReader.read(file(s)).image;
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 300; x++) {
				int v = planes[y * 300 + x] & 0xff;
				assertEquals("pixel " + x + "," + y, 0xFF000000 | v << 16 | v << 8 | v, img.getRGB(x, y));
			}
		}
	}

	@Test
	public void sixteenBitUsesTheHighByteAndZipPredictionWorks() throws IOException {
		Spec s = new Spec();
		s.channels = 1;
		s.mode = 1;
		s.depth = 16;
		s.width = 3;
		s.height = 2;
		s.planes = new byte[] { 0x10, 0x01, 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, (byte) 0x80, 0x00, 0x7F, (byte) 0xFF };
		int[] expected = { 0x10, 0x20, 0xFF, 0x00, 0x80, 0x7F };
		for (int compression : new int[] { 0, 1, 2, 3 }) {
			s.compression = compression;
			BufferedImage img = PsdReader.read(file(s)).image;
			for (int i = 0; i < expected.length; i++) {
				assertEquals("compression " + compression + " pixel " + i, expected[i], img.getRGB(i % 3, i / 3) & 0xff);
			}
		}
	}

	@Test
	public void zipWithPredictionRestoresTheEightBitRows() throws IOException {
		Spec s = new Spec();
		s.channels = 1;
		s.mode = 1;
		s.width = 5;
		s.height = 2;
		s.compression = 3;
		s.planes = new byte[] { 10, 20, (byte) 250, 3, 3, 0, 0, 1, (byte) 255, (byte) 128 };
		BufferedImage img = PsdReader.read(file(s)).image;
		for (int i = 0; i < 10; i++) {
			assertEquals(s.planes[i] & 0xff, img.getRGB(i % 5, i / 5) & 0xff);
		}
	}

	// ------------------------------------------------------------------ color modes

	@Test
	public void grayscaleWithAlpha() throws IOException {
		Spec s = new Spec();
		s.channels = 2;
		s.mode = 1;
		s.width = 2;
		s.height = 1;
		s.planes = new byte[] { 100, (byte) 200, (byte) 255, 0 };
		BufferedImage img = PsdReader.read(file(s)).image;
		assertEquals(0xFF646464, img.getRGB(0, 0));
		assertEquals(0x00C8C8C8, img.getRGB(1, 0));
	}

	@Test
	public void rgbWithAlpha() throws IOException {
		Spec s = new Spec();
		s.channels = 4;
		s.width = 1;
		s.height = 1;
		s.planes = new byte[] { 10, 20, 30, (byte) 128 };
		assertEquals(0x800A141E, PsdReader.read(file(s)).image.getRGB(0, 0));
	}

	@Test
	public void bitmapSetBitIsBlack() throws IOException {
		Spec s = new Spec();
		s.channels = 1;
		s.mode = 0;
		s.depth = 1;
		s.width = 9;
		s.height = 1;
		s.planes = new byte[] { (byte) 0xA0, (byte) 0x80 }; // 1010 0000 1
		BufferedImage img = PsdReader.read(file(s)).image;
		assertEquals(0xFF000000, img.getRGB(0, 0));
		assertEquals(0xFFFFFFFF, img.getRGB(1, 0));
		assertEquals(0xFF000000, img.getRGB(2, 0));
		assertEquals(0xFFFFFFFF, img.getRGB(7, 0));
		assertEquals(0xFF000000, img.getRGB(8, 0));
	}

	@Test
	public void indexedUsesThePalette() throws IOException {
		Spec s = new Spec();
		s.channels = 1;
		s.mode = 2;
		s.width = 2;
		s.height = 1;
		byte[] palette = new byte[768];
		palette[1] = (byte) 200;
		palette[256 + 1] = 100;
		palette[512 + 1] = 50;
		s.modeData = palette;
		s.planes = new byte[] { 1, 0 };
		BufferedImage img = PsdReader.read(file(s)).image;
		assertEquals(0xFFC86432, img.getRGB(0, 0));
		assertEquals(0xFF000000, img.getRGB(1, 0));
	}

	@Test
	public void cmykIsStoredInvertedAndConvertedToRgb() throws IOException {
		Spec s = new Spec();
		s.channels = 4;
		s.mode = 4;
		s.width = 2;
		s.height = 1;
		// pixel 0: no ink at all (255 everywhere) = white; pixel 1: full black ink (K = 0)
		s.planes = new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0 };
		PsdReader.Psd psd = PsdReader.read(file(s));
		assertEquals(0xFFFFFFFF, psd.image.getRGB(0, 0));
		assertEquals(0xFF000000, psd.image.getRGB(1, 0));
		assertTrue(psd.note.contains("CMYK"));
	}

	@Test
	public void labWhiteAndBlack() throws IOException {
		Spec s = new Spec();
		s.channels = 3;
		s.mode = 9;
		s.width = 2;
		s.height = 1;
		s.planes = new byte[] { (byte) 255, 0, /* L */ (byte) 128, (byte) 128, /* a */ (byte) 128, (byte) 128 /* b */ };
		BufferedImage img = PsdReader.read(file(s)).image;
		int white = img.getRGB(0, 0);
		int black = img.getRGB(1, 0);
		for (int shift = 0; shift <= 16; shift += 8) {
			assertTrue("white channel " + ((white >> shift) & 0xff), ((white >> shift) & 0xff) >= 248);
			assertTrue("black channel " + ((black >> shift) & 0xff), ((black >> shift) & 0xff) <= 6);
		}
	}

	@Test
	public void multichannelShowsTheFirstChannelAsGray() throws IOException {
		Spec s = new Spec();
		s.channels = 2;
		s.mode = 7;
		s.width = 1;
		s.height = 1;
		s.planes = new byte[] { 90, 10 };
		PsdReader.Psd psd = PsdReader.read(file(s));
		assertEquals("Multichannel", psd.modeName());
		assertEquals(90, psd.image.getRGB(0, 0) & 0xff);
	}

	// ------------------------------------------------------------------ information

	@Test
	public void informationFromTheResourcesAndTheLayerSection() throws IOException {
		Spec s = new Spec();
		s.planes = rgbPlanes();
		byte[] resolution = concat(be(4, 72L << 16), be(2, 1), be(2, 1), be(4, 300L << 16), be(2, 1), be(2, 1));
		String xmp = "<x:xmpmeta xmlns:crs=\"http://ns.adobe.com/camera-raw-settings/1.0/\" crs:Exposure2012=\"+0.50\"/>";
		s.resources = concat(resource(1005, resolution), resource(1039, new byte[] { 1, 2, 3 }), resource(1060, xmp.getBytes(StandardCharsets.UTF_8)), resource(2000, new byte[] { 1 }));
		s.layerContent = concat(be(4, 10), be(2, 0xFFFD), new byte[8]);
		PsdReader.Psd psd = PsdReader.readInfo(file(s));
		assertNull("readInfo decodes no pixels", psd.image);
		assertEquals(72.0, psd.dpiX, 1e-9);
		assertEquals(300.0, psd.dpiY, 1e-9);
		assertTrue(psd.iccProfile);
		assertEquals(xmp, psd.xmp);
		assertTrue(psd.hasCameraRaw());
		assertEquals(3, psd.layers);
		String text = psd.describe();
		assertTrue(text.contains("Size: 2 x 2 px"));
		assertTrue(text.contains("Color mode: RGB, 8 bit, 3 channels"));
		assertTrue(text.contains("Resolution: 72 x 300 dpi"));
		assertTrue(text.contains("Layers: 3"));
		assertTrue(text.contains("Color profile: embedded"));
		assertTrue(text.contains("Camera Raw settings"));
	}

	@Test
	public void emptyLayerSectionAndNoResources() throws IOException {
		Spec s = new Spec();
		s.planes = rgbPlanes();
		PsdReader.Psd psd = PsdReader.read(file(s));
		assertFalse(psd.iccProfile);
		assertEquals("", psd.xmp);
		assertFalse(psd.hasCameraRaw());
		assertTrue(psd.describe().contains("XMP: none"));
		assertFalse(psd.describe().contains("Layers:"));
	}

	@Test
	public void modeNames() {
		String[] names = { "Bitmap", "Grayscale", "Indexed", "RGB", "CMYK", "Mode 5", "Mode 6", "Multichannel", "Duotone", "Lab", "Mode 10" };
		for (int mode = 0; mode < names.length; mode++) {
			PsdReader.Psd psd = new PsdReader.Psd();
			psd.colorMode = mode;
			assertEquals(names[mode], psd.modeName());
		}
	}

	@Test
	public void psdExtensions() {
		assertTrue(PsdReader.isPsd(new File("a.PSD")));
		assertTrue(PsdReader.isPsd(new File("a.psb")));
		assertFalse(PsdReader.isPsd(new File("a.png")));
		assertFalse(PsdReader.isPsd(null));
	}

	// ------------------------------------------------------------------ preview and errors

	private static byte[] thumbnailResource() throws IOException {
		BufferedImage preview = new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 8; x++) {
				preview.setRGB(x, y, 0x336699);
			}
		}
		ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
		assertTrue(ImageIO.write(preview, "jpg", jpeg));
		return resource(1036, concat(be(4, 1), new byte[24], jpeg.toByteArray()));
	}

	@Test
	public void unsupportedDepthFallsBackToTheEmbeddedPreview() throws IOException {
		Spec s = new Spec();
		s.depth = 32;
		s.resources = thumbnailResource();
		s.planes = new byte[0];
		PsdReader.Psd psd = PsdReader.read(file(s));
		assertTrue(psd.fromPreview);
		assertEquals(8, psd.image.getWidth());
		assertEquals(6, psd.image.getHeight());
		assertTrue(psd.note.contains("32 bit"));
		assertTrue(psd.note.contains("preview"));
	}

	@Test
	public void truncatedCompositeFallsBackToThePreviewOrFails() throws IOException {
		Spec withPreview = new Spec();
		withPreview.planes = rgbPlanes();
		withPreview.truncateImage = true;
		withPreview.resources = thumbnailResource();
		assertTrue(PsdReader.read(file(withPreview)).fromPreview);

		Spec without = new Spec();
		without.planes = rgbPlanes();
		without.truncateImage = true;
		try {
			PsdReader.read(file(without));
			fail("no picture can be read");
		} catch (IOException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("no merged picture"));
		}
	}

	@Test
	public void unsupportedModeAndDepthWithoutPreviewAreReported() throws IOException {
		Spec depth = new Spec();
		depth.depth = 32;
		try {
			PsdReader.read(file(depth));
			fail();
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("32 bit"));
		}
		Spec odd = new Spec();
		odd.depth = 4;
		try {
			PsdReader.read(file(odd));
			fail();
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("4 bit"));
		}
		Spec mode = new Spec();
		mode.mode = 5;
		try {
			PsdReader.read(file(mode));
			fail();
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("color mode 5"));
		}
		Spec fewChannels = new Spec();
		fewChannels.channels = 2; // RGB needs 3
		fewChannels.planes = new byte[8];
		try {
			PsdReader.read(file(fewChannels));
			fail();
		} catch (IOException expected) {
			assertNotNull(expected.getMessage());
		}
	}

	@Test
	public void badHeadersAreIoExceptions() throws IOException {
		File notPsd = new File(tmp.getRoot(), "x.psd");
		Files.write(notPsd.toPath(), "GIF89a not a photoshop file".getBytes(StandardCharsets.ISO_8859_1));
		try {
			PsdReader.read(notPsd);
			fail();
		} catch (IOException expected) {
			assertEquals("Not a Photoshop file", expected.getMessage());
		}
		Spec version = new Spec();
		version.version = 3;
		try {
			PsdReader.read(file(version));
			fail();
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("version 3"));
		}
		Spec empty = new Spec();
		empty.width = 0;
		try {
			PsdReader.read(file(empty));
			fail();
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("Damaged"));
		}
		File tiny = new File(tmp.getRoot(), "tiny.psd");
		Files.write(tiny.toPath(), new byte[] { '8', 'B' });
		try {
			PsdReader.read(tiny);
			fail();
		} catch (IOException expected) {
			// end of file
		}
	}

	@Test
	public void absurdSectionLengthsInADamagedFileAreIoExceptionsNotOutOfMemory() throws IOException {
		Spec s = new Spec();
		s.planes = rgbPlanes();
		s.resourcesLengthOverride = 0x7FFFFFF0L;
		try {
			PsdReader.read(file(s));
			fail();
		} catch (IOException expected) {
			// a damaged file is refused (an OutOfMemoryError would not be caught by the callers)
		}
		Spec mode = new Spec();
		mode.planes = rgbPlanes();
		byte[] bytes = mode.build();
		ByteBuffer.wrap(bytes).putInt(26, 0x7FFFFFF0); // color mode data length
		File f = new File(tmp.getRoot(), "bigmode.psd");
		Files.write(f.toPath(), bytes);
		try {
			PsdReader.readInfo(f);
			fail();
		} catch (IOException expected) {
			// ok
		}
	}

	@Test
	public void randomDamageNeverEscapesAsARuntimeException() throws IOException {
		Spec s = new Spec();
		s.compression = 1;
		s.planes = rgbPlanes();
		s.resources = concat(resource(1005, new byte[16]), resource(1060, "<x/>".getBytes(StandardCharsets.UTF_8)));
		s.layerContent = concat(be(4, 10), be(2, 2), new byte[8]);
		byte[] original = s.build();
		java.util.Random random = new java.util.Random(11);
		for (int round = 0; round < 400; round++) {
			byte[] d = original.clone();
			for (int k = 0, n = 1 + random.nextInt(4); k < n; k++) {
				d[random.nextInt(d.length)] = (byte) random.nextInt(256);
			}
			if (random.nextBoolean()) {
				d = Arrays.copyOf(d, random.nextInt(d.length));
			}
			File f = new File(tmp.getRoot(), "fuzz.psd");
			Files.write(f.toPath(), d);
			try {
				PsdReader.read(f);
			} catch (IOException e) {
				// expected for damaged files
			} catch (RuntimeException e) {
				fail("round " + round + ": " + e + " at " + e.getStackTrace()[0]);
			}
		}
	}
}
