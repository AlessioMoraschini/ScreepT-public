package image;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.utility.manipulation.ImageWorker;

/**
 * The image helper used by the key images (a key image is a file of random pixels from which key material is read):
 * generation must be random and survive a lossless round trip, reading must follow the documented pixel order.
 */
public class ImageWorkerTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	/** Pixel (x, y) of a W x H image holds the opaque value y * W + x: the position can be read back from the value. */
	private static BufferedImage numbered(int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				img.setRGB(x, y, 0xFF000000 | (y * w + x));
			}
		}
		return img;
	}

	private static int position(int argb) {
		return argb & 0xFFFFFF;
	}

	// ------------------------------------------------------------------ key image generation

	@Test
	public void generatedKeyImageIsRandomAndSurvivesALosslessRoundTrip() throws Exception {
		File out = new File(tmp.getRoot(), "keys/key1.png");
		BufferedImage generated = ImageWorker.generateKeyImageAndWrite(64, 48, BufferedImage.TYPE_INT_RGB, out, "png");
		assertNotNull(generated);
		assertTrue("the missing folder was created", out.isFile());
		BufferedImage stored = ImageIO.read(out);
		assertEquals(64, stored.getWidth());
		assertEquals(48, stored.getHeight());
		Set<Integer> distinct = new HashSet<Integer>();
		for (int y = 0; y < 48; y++) {
			for (int x = 0; x < 64; x++) {
				assertEquals("pixel " + x + "," + y + " written = read", generated.getRGB(x, y), stored.getRGB(x, y));
				distinct.add(stored.getRGB(x, y));
			}
		}
		assertTrue("random pixels are (almost) all different: " + distinct.size() + " of " + 64 * 48, distinct.size() > 64 * 48 * 0.95);

		File second = new File(tmp.getRoot(), "keys/key2.png");
		BufferedImage other = ImageWorker.generateKeyImageAndWrite(64, 48, BufferedImage.TYPE_INT_RGB, second, "png");
		int same = 0;
		for (int y = 0; y < 48; y++) {
			for (int x = 0; x < 64; x++) {
				same += generated.getRGB(x, y) == other.getRGB(x, y) ? 1 : 0;
			}
		}
		assertTrue("two key images are not the same image (" + same + " equal pixels)", same < 20);
	}

	@Test
	public void unknownImageFormatGivesNoImage() throws Exception {
		assertNull(ImageWorker.generateKeyImageAndWrite(4, 4, BufferedImage.TYPE_INT_RGB, new File(tmp.getRoot(), "k.nofmt"), "nofmt"));
	}

	// ------------------------------------------------------------------ reading pixel values

	@Test
	public void pixelsAreIndexedByColumnThenRow() throws Exception {
		BufferedImage img = numbered(5, 3);
		Integer[][] px = new ImageWorker(img).readPxValue();
		assertEquals(5, px.length);
		assertEquals(3, px[0].length);
		assertEquals(position(img.getRGB(4, 2)), position(px[4][2]));
		assertEquals(2 * 5 + 4, position(px[4][2]));
		assertEquals(1 * 5 + 0, position(px[0][1]));
		ImageWorker w = new ImageWorker(img);
		assertEquals(5, w.Width());
		assertEquals(3, w.Height());
		assertEquals(img, w.getImage());
	}

	@Test
	public void horizontalAndVerticalReadsFollowTheirOrderAndWrapAround() throws Exception {
		BufferedImage img = numbered(4, 3);
		int[] row = ImageWorker.readNValuesFromImage(img, 6, ImageWorker.ORIZ_OPTION);
		assertEquals(6, row.length);
		for (int i = 0; i < 4; i++) {
			assertEquals("first row left to right", i, position(row[i]));
		}
		assertEquals("at the end of the row it continues on the next one", 4, position(row[4]));
		assertEquals(5, position(row[5]));

		int[] column = ImageWorker.readNValuesFromImage(img, 5, ImageWorker.VERT_OPTION);
		assertEquals(0, position(column[0]));
		assertEquals(4, position(column[1]));
		assertEquals(8, position(column[2]));
		assertEquals("after the last row, the next column", 1, position(column[3]));
		assertEquals(5, position(column[4]));
	}

	@Test
	public void readingMoreValuesThanPixelsOrFromNothingIsRefused() throws Exception {
		BufferedImage img = numbered(4, 3);
		assertEquals(12, ImageWorker.readNValuesFromImage(img, 12, ImageWorker.ORIZ_OPTION).length);
		assertNull(ImageWorker.readNValuesFromImage(img, 13, ImageWorker.ORIZ_OPTION));
		try {
			ImageWorker.readNValuesFromImage(img, 0, ImageWorker.ORIZ_OPTION);
			fail("N must be positive");
		} catch (Exception expected) {
			// ok
		}
		try {
			ImageWorker.readNValuesFromImage(null, 3, ImageWorker.VERT_OPTION);
			fail("an image is needed");
		} catch (Exception expected) {
			// ok
		}
		assertNull(ImageWorker.readNSpacedValuesFromImage(img, 13, 1));
	}

	@Test
	public void spacedReadSkipsPixelsAndAutomaticIntervalSpreadsOverTheWidth() throws Exception {
		BufferedImage img = numbered(10, 2);
		int[] every3 = ImageWorker.readNSpacedValuesFromImage(img, 4, 3);
		assertArrayEquals(new int[] { 0, 3, 6, 9 }, positions(every3));
		int[] every1 = ImageWorker.readNSpacedValuesFromImage(img, 4, 1);
		assertArrayEquals(new int[] { 0, 1, 2, 3 }, positions(every1));
		int[] auto = ImageWorker.readNSpacedValuesFromImage(img, 5, 0);
		assertArrayEquals("interval 0: W / N", new int[] { 0, 2, 4, 6, 8 }, positions(auto));
	}

	private static int[] positions(int[] argb) {
		int[] out = new int[argb.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = position(argb[i]);
		}
		return out;
	}

	@Test
	public void imageLoadedFromFileGivesTheSamePixelsAndCanBeLogged() throws Exception {
		BufferedImage img = numbered(6, 4);
		File png = new File(tmp.getRoot(), "numbered.png");
		assertTrue(ImageWorker.convertAndWrite(img, "png", png));
		ImageWorker byFile = new ImageWorker(png);
		ImageWorker byPath = new ImageWorker(png.getAbsolutePath());
		assertEquals(png, byFile.getImageFile());
		assertEquals(6, byPath.Width());
		Integer[][] px = byFile.readPxValue();
		assertEquals(img.getRGB(5, 3), (int) px[5][3]);

		File log = new File(tmp.getRoot(), "pixels.log");
		Integer[][] logged = ImageWorker.readPxValue(png, log);
		assertEquals(px[2][1], logged[2][1]);
		String text = new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8);
		assertTrue(text.contains("[X=0 ; Y=0] Value: " + img.getRGB(0, 0)));
		assertTrue(text.contains("[X=5 ; Y=3] Value: " + img.getRGB(5, 3)));
		File log2 = new File(tmp.getRoot(), "pixels2.log");
		assertEquals(px[1][1], byFile.readPxValueAndLog(log2)[1][1]);
		assertTrue(new String(Files.readAllBytes(log2.toPath()), StandardCharsets.UTF_8).contains("###"));
		File log3 = new File(tmp.getRoot(), "pixels3.log");
		assertEquals(px[3][2], ImageWorker.readPxValues(png.getAbsolutePath(), log3)[3][2]);
		byFile.setImageFile(null);
		assertNull(byFile.getImageFile());
		byFile.setImage(img);
		assertEquals(img, byFile.getImage());
	}

	@Test
	public void writingPixelsToAnotherImageTypeKeepsTheColours() throws Exception {
		BufferedImage src = numbered(3, 3);
		BufferedImage rgb = new ImageWorker(src).writePxValue(src, BufferedImage.TYPE_INT_RGB);
		assertEquals(BufferedImage.TYPE_INT_RGB, rgb.getType());
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				assertEquals(src.getRGB(x, y), rgb.getRGB(x, y));
			}
		}
	}

	// ------------------------------------------------------------------ scaling and conversion

	@Test
	public void scalingKeepsTheAspectRatioInsideTheRequestedBox() {
		BufferedImage wide = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
		BufferedImage tall = new BufferedImage(50, 100, BufferedImage.TYPE_INT_RGB);
		assertEquals(new Dimension(40, 20), ImageWorker.getDimensionSameRatio(wide, 40, 40));
		assertEquals(new Dimension(20, 40), ImageWorker.getDimensionSameRatio(tall, 40, 40));
		assertEquals(new Dimension(200, 100), ImageWorker.getDimensionSameRatio(wide, 200, 200));
		BufferedImage scaled = ImageWorker.scaleImage(wide, BufferedImage.TYPE_INT_RGB, 40, 40);
		assertEquals(40, scaled.getWidth());
		assertEquals(20, scaled.getHeight());
		Image smooth = ImageWorker.scaleImageSameRatio(tall, 10, 10);
		assertEquals(5, smooth.getWidth(null));
		assertEquals(10, smooth.getHeight(null));
		Image mode = ImageWorker.scaleImage((Image) wide, 20, 20, Image.SCALE_FAST);
		assertEquals(20, mode.getWidth(null));
		assertEquals(10, mode.getHeight(null));
	}

	@Test
	public void anyImageCanBecomeABufferedImage() {
		BufferedImage same = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		assertTrue("a buffered image is returned as it is", ImageWorker.toBufferedImage(same) == same);
		Image scaled = numbered(8, 8).getScaledInstance(4, 4, Image.SCALE_FAST);
		BufferedImage converted = ImageWorker.toBufferedImage(scaled);
		assertEquals(4, converted.getWidth());
		assertEquals(4, converted.getHeight());
		assertEquals(BufferedImage.TYPE_INT_ARGB, converted.getType());
	}

	@Test
	public void copiesAreIndependentAndPngConversionIsLossless() throws Exception {
		BufferedImage src = numbered(5, 5);
		BufferedImage copy = ImageWorker.copyImage(src);
		assertNotSame(src, copy);
		assertEquals(src.getRGB(3, 3), copy.getRGB(3, 3));
		copy.setRGB(3, 3, 0xFFFF0000);
		assertNotEquals("the source is untouched", copy.getRGB(3, 3), src.getRGB(3, 3));

		Image back = ImageWorker.convert(src, "png");
		assertNotNull(back);
		BufferedImage backBuffered = ImageWorker.toBufferedImage(back);
		for (int y = 0; y < 5; y++) {
			for (int x = 0; x < 5; x++) {
				assertEquals(src.getRGB(x, y), backBuffered.getRGB(x, y));
			}
		}
	}

	@Test
	public void writingToAnUnwritableTargetReportsFailure() throws Exception {
		assertFalse(ImageWorker.convertAndWrite(numbered(2, 2), "png", new File(tmp.getRoot(), "no/such/folder/x.png")));
	}

	@Test
	public void stricterJpegQualityGivesSmallerFilesThatStillDecode() throws Exception {
		BufferedImage photo = new BufferedImage(120, 80, BufferedImage.TYPE_3BYTE_BGR);
		for (int y = 0; y < 80; y++) {
			for (int x = 0; x < 120; x++) {
				photo.setRGB(x, y, new Color((x * 2) % 256, (y * 3) % 256, (x * y) % 256).getRGB());
			}
		}
		byte[] low = ImageWorker.compressIMGjpgNoMeta(photo, 0.05f);
		byte[] high = ImageWorker.compressIMGjpgNoMeta(photo, 0.95f);
		assertTrue("low quality " + low.length + " < high quality " + high.length, low.length < high.length);
		assertEquals((byte) 0xFF, high[0]);
		assertEquals((byte) 0xD8, high[1]);
		BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(high));
		assertEquals(120, decoded.getWidth());
		assertEquals(80, decoded.getHeight());

		File jpg = new File(tmp.getRoot(), "photo.jpg");
		Files.write(jpg.toPath(), high);
		byte[] again = ImageWorker.compressIMGjpg(jpg, 0.3f, false);
		assertTrue(again.length > 0 && again.length < high.length);
		byte[] withMeta = ImageWorker.compressIMGjpg(jpg, 0.3f, true);
		assertTrue(withMeta.length > 0);
	}

	// ------------------------------------------------------------------ colours

	@Test
	public void colourHelpers() {
		assertEquals(new Color(245, 235, 225), ImageWorker.getComplimentColor(new Color(10, 20, 30)));
		assertEquals(Color.WHITE, ImageWorker.getComplimentColor(null));
		assertEquals(Color.BLACK, ImageWorker.getComplimentColor(Color.WHITE));
		Color base = new Color(200, 100, 50);
		assertEquals(base, ImageWorker.getDarkerNtimes(base, 0));
		assertEquals(base, ImageWorker.getBrighterNtimes(base, 0));
		Color d1 = ImageWorker.getDarkerNtimes(base, 1);
		Color d3 = ImageWorker.getDarkerNtimes(base, 3);
		assertTrue(d1.getRed() < base.getRed() && d3.getRed() < d1.getRed());
		assertTrue(ImageWorker.getBrighterNtimes(base, 2).getRed() > base.getRed());
		assertEquals(255 + 10 + 20 + 30, ImageWorker.getColorComponentsTotRGBA(new Color(10, 20, 30)));
		for (int i = 0; i < 200; i++) {
			Color c = ImageWorker.randomColorRGB(1f);
			assertTrue(c.getAlpha() >= 100);
		}
	}
}
