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

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Saves and loads {@link ImageSettings} as plain text files (presets): to move a configuration to another PC, share
 * it, or keep one per use ("web photos", "thumbnails"...). The last used configuration is also kept automatically.
 */
public final class ImageSettingsStore {

	private static final String LAST_NAME = "last.properties";

	private ImageSettingsStore() {
	}

	/** Folder of the presets and of the last used configuration (in the user's home, valid on every OS) */
	public static File presetsDir() {
		File dir = new File(System.getProperty("user.home"), ".screept" + File.separator + "image_studio");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public static Properties toProperties(ImageSettings s) {
		Properties p = new Properties();
		p.setProperty("brightness", String.valueOf(s.brightness));
		p.setProperty("contrast", String.valueOf(s.contrast));
		p.setProperty("saturation", String.valueOf(s.saturation));
		p.setProperty("gamma", String.valueOf(s.gamma));
		p.setProperty("temperature", String.valueOf(s.temperature));
		p.setProperty("hue", String.valueOf(s.hue));
		StringBuilder filters = new StringBuilder();
		for (ImageEdit.Filter filter : s.filters) {
			filters.append(filters.length() > 0 ? "," : "").append(filter.name());
		}
		p.setProperty("filters", filters.toString());
		p.setProperty("rotation", String.valueOf(s.rotation));
		p.setProperty("flipH", String.valueOf(s.flipH));
		p.setProperty("flipV", String.valueOf(s.flipV));
		p.setProperty("resizeMode", s.resizeMode.name());
		p.setProperty("percent", String.valueOf(s.percent));
		p.setProperty("width", String.valueOf(s.width));
		p.setProperty("height", String.valueOf(s.height));
		p.setProperty("resampling", s.resampling.name());
		p.setProperty("format", s.format);
		p.setProperty("quality", String.valueOf(s.quality));
		p.setProperty("progressive", String.valueOf(s.progressive));
		p.setProperty("background", String.valueOf(s.background == null ? 0xFFFFFF : s.background.getRGB() & 0xFFFFFF));
		return p;
	}

	/** Text used to detect whether the settings changed since a preset was loaded or saved */
	public static String fingerprint(ImageSettings s) {
		java.util.TreeMap<Object, Object> sorted = new java.util.TreeMap<Object, Object>(toProperties(s));
		return sorted.toString();
	}

	/**
	 * Reads the values present in the properties (missing or invalid ones keep the current value)
	 *
	 * @param outputOnly true: only format, quality, resize and resampling (what you usually want to keep from the last
	 *                   session); false: also adjustments, filters, rotation and flips
	 */
	public static void read(Properties p, ImageSettings s, boolean outputOnly) {
		s.format = text(p, "format", s.format);
		s.quality = number(p, "quality", s.quality, 1, 100);
		s.progressive = Boolean.parseBoolean(text(p, "progressive", String.valueOf(s.progressive)));
		s.background = new Color(number(p, "background", s.background == null ? 0xFFFFFF : s.background.getRGB() & 0xFFFFFF, 0, 0xFFFFFF));
		s.resizeMode = enumValue(p, "resizeMode", s.resizeMode, ImageSettings.ResizeMode.class);
		s.percent = number(p, "percent", s.percent, 1, 800);
		s.width = number(p, "width", s.width, 1, 30000);
		s.height = number(p, "height", s.height, 1, 30000);
		s.resampling = enumValue(p, "resampling", s.resampling, ImageSettings.Resampling.class);
		if (outputOnly) {
			return;
		}
		s.brightness = number(p, "brightness", s.brightness, -100, 100);
		s.contrast = number(p, "contrast", s.contrast, -100, 100);
		s.saturation = number(p, "saturation", s.saturation, -100, 100);
		s.temperature = number(p, "temperature", s.temperature, -100, 100);
		s.hue = number(p, "hue", s.hue, -180, 180);
		try {
			s.gamma = Math.max(0.2, Math.min(3.0, Double.parseDouble(text(p, "gamma", String.valueOf(s.gamma)))));
		} catch (NumberFormatException e) {
			// keeps the current gamma
		}
		s.filters.clear();
		for (String name : text(p, "filters", "").split(",")) {
			try {
				if (!name.trim().isEmpty()) {
					s.filters.add(ImageEdit.Filter.valueOf(name.trim()));
				}
			} catch (IllegalArgumentException e) {
				// a filter of a newer version: ignored
			}
		}
		s.rotation = ((number(p, "rotation", s.rotation, 0, 360) / 90) % 4) * 90;
		s.flipH = Boolean.parseBoolean(text(p, "flipH", String.valueOf(s.flipH)));
		s.flipV = Boolean.parseBoolean(text(p, "flipV", String.valueOf(s.flipV)));
	}

	private static String text(Properties p, String key, String fallback) {
		String value = p.getProperty(key);
		return value == null ? fallback : value.trim();
	}

	private static int number(Properties p, String key, int fallback, int min, int max) {
		try {
			return Math.max(min, Math.min(max, Integer.parseInt(text(p, key, String.valueOf(fallback)))));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static <E extends Enum<E>> E enumValue(Properties p, String key, E fallback, Class<E> type) {
		try {
			return Enum.valueOf(type, text(p, key, fallback.name()));
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	public static void write(File file, ImageSettings s) throws IOException {
		File parent = file.getAbsoluteFile().getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		try (OutputStream out = new FileOutputStream(file)) {
			toProperties(s).store(out, "ScreepT Image Studio preset");
		}
	}

	public static void load(File file, ImageSettings s, boolean outputOnly) throws IOException {
		Properties p = new Properties();
		try (InputStream in = new FileInputStream(file)) {
			p.load(in);
		}
		if (!p.containsKey("format") && !p.containsKey("resizeMode")) {
			throw new IOException("This is not an Image Studio preset");
		}
		read(p, s, outputOnly);
	}

	/** Keeps the last used configuration; never fails the caller */
	public static void saveLast(ImageSettings s) {
		try {
			write(new File(presetsDir(), LAST_NAME), s);
		} catch (IOException | RuntimeException e) {
			// not being able to remember the last configuration is not worth an error dialog
		}
	}

	/** Restores the last used configuration if there is one */
	public static boolean loadLast(ImageSettings s, boolean outputOnly) {
		File last = new File(presetsDir(), LAST_NAME);
		if (!last.isFile()) {
			return false;
		}
		try {
			load(last, s, outputOnly);
			return true;
		} catch (IOException | RuntimeException e) {
			return false;
		}
	}
}
