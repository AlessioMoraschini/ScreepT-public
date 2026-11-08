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
import java.util.ArrayList;
import java.util.List;

/**
 * Everything the Image Studio can do to an image, as plain data: the side panel edits it, the live preview and the
 * batch run apply it with {@link ImageEdit#apply}. Defaults mean "leave the image as it is".
 */
public class ImageSettings {

	public enum ResizeMode {
		NONE("Keep size"), PERCENT("Percentage"), FIT_BOX("Fit in a box"), WIDTH("Width, keep ratio"), HEIGHT("Height, keep ratio"), EXACT("Exact size (stretch)");

		private final String label;

		ResizeMode(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	public enum Resampling {
		AREA("Best for reducing (area)"), BICUBIC("Smooth (bicubic)"), BILINEAR("Fast (bilinear)"), NEAREST("Pixels (nearest)");

		private final String label;

		Resampling(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	// color
	public int brightness;
	public int contrast;
	public int saturation;
	/** 1.0 = unchanged */
	public double gamma = 1.0;
	/** negative: cooler, positive: warmer */
	public int temperature;
	/** degrees, -180..180 */
	public int hue;

	// filters, applied in order after the color adjustments
	public final List<ImageEdit.Filter> filters = new ArrayList<ImageEdit.Filter>();

	// geometry
	/** 0, 90, 180, 270 clockwise */
	public int rotation;
	public boolean flipH;
	public boolean flipV;
	public ResizeMode resizeMode = ResizeMode.NONE;
	public int percent = 100;
	public int width = 1920;
	public int height = 1080;
	public Resampling resampling = Resampling.AREA;

	// output
	public String format = "png";
	/** JPEG quality 1..100 */
	public int quality = 90;
	public boolean progressive;
	/** Used instead of transparency for formats without alpha (JPEG, BMP) */
	public Color background = Color.WHITE;

	public boolean hasColorChanges() {
		return brightness != 0 || contrast != 0 || saturation != 0 || temperature != 0 || hue != 0 || Math.abs(gamma - 1.0) > 0.001;
	}

	public boolean isIdentity() {
		return !hasColorChanges() && filters.isEmpty() && rotation == 0 && !flipH && !flipV && resizeMode == ResizeMode.NONE;
	}

	/** Resets the editing part, keeps the output choices */
	public void resetEdits() {
		brightness = 0;
		contrast = 0;
		saturation = 0;
		gamma = 1.0;
		temperature = 0;
		hue = 0;
		filters.clear();
		rotation = 0;
		flipH = false;
		flipV = false;
		resizeMode = ResizeMode.NONE;
	}

	public ImageSettings copy() {
		ImageSettings c = new ImageSettings();
		c.brightness = brightness;
		c.contrast = contrast;
		c.saturation = saturation;
		c.gamma = gamma;
		c.temperature = temperature;
		c.hue = hue;
		c.filters.addAll(filters);
		c.rotation = rotation;
		c.flipH = flipH;
		c.flipV = flipV;
		c.resizeMode = resizeMode;
		c.percent = percent;
		c.width = width;
		c.height = height;
		c.resampling = resampling;
		c.format = format;
		c.quality = quality;
		c.progressive = progressive;
		c.background = background;
		return c;
	}
}
