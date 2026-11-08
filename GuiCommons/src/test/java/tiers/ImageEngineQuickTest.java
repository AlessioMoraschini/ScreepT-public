package tiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.junit.Test;

import gui.commons.imagestudio.ImageEdit;
import gui.commons.imagestudio.ImageSettings;
import gui.commons.imagestudio.ImageSettingsStore;
import gui.commons.tabs.TabContent;
import gui.commons.tabs.TabImplementor;
import gui.commons.tabs.TabImplementors;

/** QUICK tier: pure logic, no window, a few seconds in total. Runs on every build. */
public class ImageEngineQuickTest {

	private static BufferedImage sample(int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				img.setRGB(x, y, 0xFF000000 | ((x * 255 / w) << 16) | ((y * 255 / h) << 8) | 0x80);
			}
		}
		return img;
	}

	@Test
	public void targetSizeFollowsTheResizeMode() {
		ImageSettings s = new ImageSettings();
		s.resizeMode = ImageSettings.ResizeMode.PERCENT;
		s.percent = 50;
		assertEquals(50, ImageEdit.targetSize(100, 80, s)[0]);
		assertEquals(40, ImageEdit.targetSize(100, 80, s)[1]);
		s.resizeMode = ImageSettings.ResizeMode.FIT_BOX;
		s.width = 50;
		s.height = 50;
		assertEquals(50, ImageEdit.targetSize(100, 80, s)[0]);
		assertEquals(40, ImageEdit.targetSize(100, 80, s)[1]);
		s.resizeMode = ImageSettings.ResizeMode.WIDTH;
		s.width = 200;
		assertEquals(160, ImageEdit.targetSize(100, 80, s)[1]);
		s.resizeMode = ImageSettings.ResizeMode.EXACT;
		s.width = 7;
		s.height = 9;
		assertEquals(7, ImageEdit.targetSize(100, 80, s)[0]);
		assertEquals(9, ImageEdit.targetSize(100, 80, s)[1]);
	}

	@Test
	public void rotationSwapsTheSidesAndEveryFilterKeepsTheSize() {
		BufferedImage img = sample(40, 20);
		BufferedImage rotated = ImageEdit.rotateFlip(img, 90, false, false);
		assertEquals(20, rotated.getWidth());
		assertEquals(40, rotated.getHeight());
		for (ImageEdit.Filter filter : ImageEdit.Filter.values()) {
			BufferedImage out = ImageEdit.filter(img, filter);
			assertEquals(filter.name(), 40, out.getWidth());
			assertEquals(filter.name(), 20, out.getHeight());
		}
	}

	@Test
	public void identitySettingsChangeNothing() {
		ImageSettings s = new ImageSettings();
		assertTrue(s.isIdentity());
		BufferedImage img = sample(10, 10);
		BufferedImage out = ImageEdit.apply(img, s);
		assertEquals(img.getRGB(3, 4), out.getRGB(3, 4));
		s.brightness = 10;
		assertFalse(s.isIdentity());
	}

	@Test
	public void formatsAndLossyQualityBehave() throws Exception {
		assertTrue(ImageEdit.writableFormats().contains("png"));
		assertTrue(ImageEdit.writableFormats().contains("jpg"));
		BufferedImage img = sample(200, 150);
		ImageSettings low = new ImageSettings();
		low.format = "jpg";
		low.quality = 10;
		ImageSettings high = low.copy();
		high.quality = 95;
		ByteArrayOutputStream a = new ByteArrayOutputStream();
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ImageEdit.write(img, a, low);
		ImageEdit.write(img, b, high);
		assertTrue("higher quality must weigh more", b.size() > a.size());
		assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(b.toByteArray())));
	}

	@Test
	public void presetRoundTripKeepsEverythingAndOutputOnlyKeepsJustTheOutput() throws Exception {
		ImageSettings s = new ImageSettings();
		s.format = "jpg";
		s.quality = 72;
		s.brightness = 15;
		s.gamma = 1.4;
		s.filters.add(ImageEdit.Filter.SEPIA);
		s.rotation = 90;
		s.flipH = true;
		s.background = new Color(10, 20, 30);
		File file = File.createTempFile("preset", ".properties");
		try {
			ImageSettingsStore.write(file, s);
			ImageSettings whole = new ImageSettings();
			ImageSettingsStore.load(file, whole, false);
			assertEquals(ImageSettingsStore.fingerprint(s), ImageSettingsStore.fingerprint(whole));
			ImageSettings output = new ImageSettings();
			ImageSettingsStore.load(file, output, true);
			assertEquals("jpg", output.format);
			assertEquals(72, output.quality);
			assertEquals(0, output.brightness);
			assertTrue(output.filters.isEmpty());
		} finally {
			file.delete();
		}
	}

	@Test
	public void unknownFilterAndBadValuesInAPresetAreIgnored() {
		Properties p = new Properties();
		p.setProperty("format", "png");
		p.setProperty("filters", "SEPIA,FILTER_OF_THE_FUTURE");
		p.setProperty("quality", "not a number");
		ImageSettings s = new ImageSettings();
		ImageSettingsStore.read(p, s, false);
		assertEquals(1, s.filters.size());
		assertEquals(90, s.quality);
	}

	/** Viewer used only to test the registry */
	private static class FakeViewer implements TabImplementor {
		private final String id;
		private final String ext;

		FakeViewer(String id, String ext) {
			this.id = id;
			this.ext = ext;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getDisplayName() {
			return id;
		}

		@Override
		public java.util.Set<String> getDefaultExtensions() {
			return java.util.Collections.singleton(ext);
		}

		@Override
		public boolean canOpen(File file) {
			return true;
		}

		@Override
		public TabContent open(File file) {
			return null;
		}
	}

	@Test
	public void associationsKeepOnlyTheDifferencesFromTheDefaults() {
		TabImplementors.register(new FakeViewer("fakeviewer", "zzfake"));
		TabImplementors.setOverrides(null);
		assertEquals("fakeviewer", TabImplementors.effectiveIdFor("zzfake"));
		assertEquals(TabImplementors.TEXT_ID, TabImplementors.effectiveIdFor("zzunknown"));
		TabImplementors.associate("zzfake", TabImplementors.TEXT_ID);
		assertEquals(TabImplementors.TEXT_ID, TabImplementors.effectiveIdFor("zzfake"));
		assertEquals("zzfake=text", TabImplementors.serializeOverrides());
		TabImplementors.associate("zzfake", "fakeviewer"); // back to the default: nothing to keep
		assertEquals("", TabImplementors.serializeOverrides());
		TabImplementors.loadOverrides("zzfake=text;broken;=x");
		assertEquals(1, TabImplementors.getOverrides().size());
		TabImplementors.setOverrides(null);
	}
}
