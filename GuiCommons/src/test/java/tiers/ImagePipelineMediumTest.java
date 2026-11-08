package tiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Test;

import gui.commons.imagestudio.ImageEdit;
import gui.commons.imagestudio.ImageSettings;

/** MEDIUM tier: real files in a temporary folder and big images; a few tens of seconds. Daily. */
public class ImagePipelineMediumTest {

	private static BufferedImage sample(int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				img.setRGB(x, y, 0xFF000000 | ((x * 255 / w) << 16) | ((y * 255 / h) << 8) | ((x ^ y) & 0xFF));
			}
		}
		return img;
	}

	@Test
	public void aBatchOfFilesIsConvertedResizedAndReadable() throws Exception {
		File dir = java.nio.file.Files.createTempDirectory("studio_batch").toFile();
		try {
			ImageSettings s = new ImageSettings();
			s.format = "jpg";
			s.quality = 80;
			s.resizeMode = ImageSettings.ResizeMode.FIT_BOX;
			s.width = 300;
			s.height = 300;
			s.filters.add(ImageEdit.Filter.SEPIA);
			s.filters.add(ImageEdit.Filter.VIGNETTE);
			int[][] sizes = {{1200, 800}, {800, 1200}, {500, 500}};
			for (int i = 0; i < sizes.length; i++) {
				BufferedImage result = ImageEdit.apply(sample(sizes[i][0], sizes[i][1]), s);
				assertTrue(result.getWidth() <= 300 && result.getHeight() <= 300);
				File out = new File(dir, "img_" + (i + 1) + ".jpg");
				ImageEdit.write(result, out, s);
				BufferedImage back = ImageIO.read(out);
				assertNotNull(out.getName(), back);
				assertEquals(result.getWidth(), back.getWidth());
			}
			assertEquals(3, dir.listFiles().length);
		} finally {
			for (File f : dir.listFiles()) {
				f.delete();
			}
			dir.delete();
		}
	}

	@Test
	public void everyWritableFormatRoundTrips() throws Exception {
		BufferedImage img = sample(120, 80);
		for (String format : ImageEdit.writableFormats()) {
			ImageSettings s = new ImageSettings();
			s.format = format;
			File out = File.createTempFile("fmt", "." + format);
			try {
				ImageEdit.write(img, out, s);
				BufferedImage back = ImageIO.read(out);
				assertNotNull(format, back);
				assertEquals(format, 120, back.getWidth());
			} finally {
				out.delete();
			}
		}
	}

	@Test(timeout = 60000)
	public void everyFilterOnAFourMegapixelImageIsFastEnough() {
		BufferedImage big = sample(2400, 1700);
		for (ImageEdit.Filter filter : ImageEdit.Filter.values()) {
			long start = System.nanoTime();
			BufferedImage out = ImageEdit.filter(big, filter);
			assertEquals(2400, out.getWidth());
			assertTrue(filter + " took too long", (System.nanoTime() - start) / 1_000_000 < 8000);
		}
	}

	@Test
	public void resizeAlgorithmsAllReachTheTargetSize() {
		BufferedImage img = sample(1000, 700);
		for (ImageSettings.Resampling mode : ImageSettings.Resampling.values()) {
			BufferedImage down = ImageEdit.resize(img, 123, 87, mode);
			assertEquals(mode.name(), 123, down.getWidth());
			assertEquals(mode.name(), 87, down.getHeight());
			BufferedImage up = ImageEdit.resize(down, 400, 300, mode);
			assertEquals(mode.name(), 400, up.getWidth());
		}
	}
}
