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

	/** Shape of the crop: the biggest centered area with this ratio inside the margins */
	public enum CropRatio {
		FREE("Free", 0, 0), SQUARE("1:1 square", 1, 1), R43("4:3", 4, 3), R32("3:2", 3, 2), R169("16:9", 16, 9),
		R54("5:4", 5, 4), R45("4:5 portrait", 4, 5), R916("9:16 story", 9, 16), R21("2:1 banner", 2, 1);

		private final String label;
		public final int w;
		public final int h;

		CropRatio(String label, int w, int h) {
			this.label = label;
			this.w = w;
			this.h = h;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	public enum TextPosition {
		BOTTOM_RIGHT("Bottom right"), BOTTOM_LEFT("Bottom left"), TOP_RIGHT("Top right"), TOP_LEFT("Top left"), CENTER("Center"),
		BOTTOM_CENTER("Bottom center"), TOP_CENTER("Top center");

		private final String label;

		TextPosition(String label) {
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
	/** -100..100: about 2 stops each way */
	public int exposure;
	/** -100..100: lifts (+) or deepens (-) the dark tones */
	public int shadows;
	/** -100..100: brightens (+) or recovers (-) the light tones */
	public int highlights;
	/** -100..100: saturation that protects the colors already strong */
	public int vibrance;
	/** negative: green, positive: magenta */
	public int tint;

	// detail and effects, applied after the color adjustments
	/** 0..100 */
	public int sharpness;
	/** 0..20 pixels of radius */
	public int blur;
	/** 0..100 */
	public int vignette;
	/** 0..100 */
	public int grain;

	// filters, applied in order after the color adjustments
	public final List<ImageEdit.Filter> filters = new ArrayList<ImageEdit.Filter>();
	/** 0..100: how much of the filters shows (the rest is the image before them) */
	public int filterStrength = 100;

	// geometry
	public CropRatio cropRatio = CropRatio.FREE;
	/** margins cut away, percent of the side, 0..45 */
	public int cropLeft;
	public int cropTop;
	public int cropRight;
	public int cropBottom;
	/** -45..45 degrees: levels the horizon, the image is enlarged just enough to leave no empty corner */
	public int straighten;
	/** 0, 90, 180, 270 clockwise */
	public int rotation;
	public boolean flipH;
	public boolean flipV;
	public ResizeMode resizeMode = ResizeMode.NONE;
	public int percent = 100;
	public int width = 1920;
	public int height = 1080;
	public Resampling resampling = Resampling.AREA;

	// decoration, the last step
	/** frame around the image, in pixels */
	public int borderWidth;
	public Color borderColor = Color.WHITE;
	/** 0..50: rounded corners, percent of the shorter side */
	public int corners;
	public String watermark = "";
	public TextPosition watermarkPosition = TextPosition.BOTTOM_RIGHT;
	/** text height, percent of the image height */
	public int watermarkSize = 5;
	/** 5..100 */
	public int watermarkOpacity = 70;
	public Color watermarkColor = Color.WHITE;

	// output
	public String format = "png";
	/** JPEG quality 1..100 */
	public int quality = 90;
	public boolean progressive;
	/** Used instead of transparency for formats without alpha (JPEG, BMP) */
	public Color background = Color.WHITE;

	public boolean hasColorChanges() {
		return brightness != 0 || contrast != 0 || saturation != 0 || temperature != 0 || hue != 0 || Math.abs(gamma - 1.0) > 0.001
				|| exposure != 0 || shadows != 0 || highlights != 0 || vibrance != 0 || tint != 0;
	}

	public boolean hasEffects() {
		return sharpness != 0 || blur != 0 || vignette != 0 || grain != 0;
	}

	public boolean hasCrop() {
		return cropRatio != CropRatio.FREE || cropLeft != 0 || cropTop != 0 || cropRight != 0 || cropBottom != 0;
	}

	public boolean hasWatermark() {
		return watermark != null && !watermark.trim().isEmpty();
	}

	public boolean hasDecoration() {
		return borderWidth > 0 || corners > 0 || hasWatermark();
	}

	public boolean isIdentity() {
		return !hasColorChanges() && !hasEffects() && !hasCrop() && !hasDecoration() && filters.isEmpty() && straighten == 0
				&& rotation == 0 && !flipH && !flipV && resizeMode == ResizeMode.NONE;
	}

	/** Resets the editing part, keeps the output choices */
	public void resetEdits() {
		resetColor();
		resetEffects();
		filters.clear();
		filterStrength = 100;
		resetCrop();
		straighten = 0;
		rotation = 0;
		flipH = false;
		flipV = false;
		resizeMode = ResizeMode.NONE;
		borderWidth = 0;
		corners = 0;
		watermark = "";
	}

	public void resetColor() {
		brightness = 0;
		contrast = 0;
		saturation = 0;
		gamma = 1.0;
		temperature = 0;
		hue = 0;
		exposure = 0;
		shadows = 0;
		highlights = 0;
		vibrance = 0;
		tint = 0;
	}

	public void resetEffects() {
		sharpness = 0;
		blur = 0;
		vignette = 0;
		grain = 0;
	}

	public void resetCrop() {
		cropRatio = CropRatio.FREE;
		cropLeft = 0;
		cropTop = 0;
		cropRight = 0;
		cropBottom = 0;
	}

	public ImageSettings copy() {
		ImageSettings c = new ImageSettings();
		c.brightness = brightness;
		c.contrast = contrast;
		c.saturation = saturation;
		c.gamma = gamma;
		c.temperature = temperature;
		c.hue = hue;
		c.exposure = exposure;
		c.shadows = shadows;
		c.highlights = highlights;
		c.vibrance = vibrance;
		c.tint = tint;
		c.sharpness = sharpness;
		c.blur = blur;
		c.vignette = vignette;
		c.grain = grain;
		c.filters.addAll(filters);
		c.filterStrength = filterStrength;
		c.cropRatio = cropRatio;
		c.cropLeft = cropLeft;
		c.cropTop = cropTop;
		c.cropRight = cropRight;
		c.cropBottom = cropBottom;
		c.straighten = straighten;
		c.rotation = rotation;
		c.flipH = flipH;
		c.flipV = flipV;
		c.resizeMode = resizeMode;
		c.percent = percent;
		c.width = width;
		c.height = height;
		c.resampling = resampling;
		c.borderWidth = borderWidth;
		c.borderColor = borderColor;
		c.corners = corners;
		c.watermark = watermark;
		c.watermarkPosition = watermarkPosition;
		c.watermarkSize = watermarkSize;
		c.watermarkOpacity = watermarkOpacity;
		c.watermarkColor = watermarkColor;
		c.format = format;
		c.quality = quality;
		c.progressive = progressive;
		c.background = background;
		return c;
	}
}
