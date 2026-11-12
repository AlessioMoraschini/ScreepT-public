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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.IntStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Image processing of the Image Studio: pure Java 2D / ImageIO (Java 8 to 21, every OS), no external library.
 * Everything works on ARGB int arrays and runs on all the cores.
 */
public final class ImageEdit {

	private ImageEdit() {
	}

	/** Groups of the filter list */
	public enum Category {
		COLOR("Color"), LOOKS("Looks"), TONE("Tone"), DETAIL("Detail"), ART("Art"), WILD("Wild");

		public final String label;

		Category(String label) {
			this.label = label;
		}
	}

	/** Ready made looks and effects, grouped by {@link Category} (the order here is the order shown) */
	public enum Filter {
		// color
		GRAYSCALE("B & W", "Classic grayscale", Category.COLOR),
		NOIR("Noir", "Dark, contrasty black and white", Category.COLOR),
		SEPIA("Sepia", "Warm brown old photo tone", Category.COLOR),
		WARM("Warm", "Sunset look: more red, less blue", Category.COLOR),
		COOL("Cool", "Cold look: more blue, less red", Category.COLOR),
		VIVID("Vivid", "Rich colors and a touch of contrast", Category.COLOR),
		CYANOTYPE("Cyanotype", "Blue print, two tones from navy to ice", Category.COLOR),
		DUSK("Dusk", "Two tones from violet shadows to peach lights", Category.COLOR),
		INFRARED("Infrared", "False color infrared: foliage turns pink", Category.COLOR),
		// film and cinema looks
		VINTAGE("Vintage", "Faded warm colors with a soft vignette", Category.LOOKS),
		LOMO("Lomo", "Toy camera: punchy colors, dark corners", Category.LOOKS),
		BLEACH("Bleach", "Bleach bypass: silvery, hard and desaturated", Category.LOOKS),
		GOLDEN("Golden hour", "Warm low sun on everything", Category.LOOKS),
		NIGHT("Night", "Dark and blue, moonlight mood", Category.LOOKS),
		FILM("Film", "Faded negative with teal shadows", Category.LOOKS),
		CROSS_PROCESS("Cross", "Slide film developed in the wrong chemistry", Category.LOOKS),
		CINEMATIC("Cinematic", "Teal shadows, orange highlights", Category.LOOKS),
		// tone and light
		AUTO_CONTRAST("Autocontrast", "Stretches the tones to use the full range", Category.TONE),
		FADE("Fade", "Lifted blacks, softer contrast", Category.TONE),
		MATTE("Matte", "Flat blacks, slightly muted colors", Category.TONE),
		VIGNETTE("Vignette", "Darker corners to focus the center", Category.TONE),
		GLOW("Glow", "Soft dreamy light on the highlights", Category.TONE),
		SOFT_FOCUS("Soft focus", "Portrait softness that keeps the shapes", Category.TONE),
		GRAIN("Film grain", "Fine analog noise", Category.TONE),
		INVERT("Invert", "Negative colors", Category.TONE),
		// detail
		SHARPEN("Sharpen", "Brings out the details", Category.DETAIL),
		SHARPEN_STRONG("Sharpen +", "Crisp, for soft photos", Category.DETAIL),
		CLARITY("Clarity", "Local contrast: texture without halos", Category.DETAIL),
		HDR("HDR", "Strong local contrast and rich color", Category.DETAIL),
		DENOISE("Denoise", "Median filter: removes specks and noise", Category.DETAIL),
		BLUR("Blur", "Soft blur", Category.DETAIL),
		BLUR_STRONG("Blur +", "Heavy blur, good for backgrounds", Category.DETAIL),
		// art
		SKETCH("Sketch", "Looks drawn with a pencil", Category.ART),
		CARTOON("Cartoon", "Flat colors with ink outlines", Category.ART),
		OIL("Oil paint", "Brush strokes: colors melt in patches", Category.ART),
		EMBOSS("Emboss", "Relief effect", Category.ART),
		EDGES("Edges", "Outlines of the shapes", Category.ART),
		SOLARIZE("Solarize", "Bright tones flip to negative", Category.ART),
		POSTERIZE("Posterize", "Few flat tones, poster style", Category.ART),
		THRESHOLD("Threshold", "Pure black and white, no grays", Category.ART),
		PIXELATE("Pixelate", "Big blocks, also to hide details", Category.ART),
		GLITCH("Glitch", "Split color channels and shifted bands", Category.ART),
		// wild and psychedelic
		RAINBOW("Rainbow", "Brightness mapped on the whole rainbow", Category.WILD),
		ACID("Acid", "Colors rotated by the light at full saturation", Category.WILD),
		NEON("Neon edges", "Edges as glowing neon lines on black", Category.WILD),
		THERMAL("Thermal", "Thermal camera: black, blue, magenta, orange, yellow, white", Category.WILD),
		POP_ART("Pop art", "Four flat screen print colors", Category.WILD),
		KALEIDOSCOPE("Kaleido", "Six mirrored wedges around the center", Category.WILD),
		SWIRL("Swirl", "Twists the picture around its center", Category.WILD),
		RIPPLE("Ripple", "Water ripples, the picture trembles", Category.WILD),
		BULGE("Bulge", "Fish-eye: the middle swells", Category.WILD),
		MELT("Melt", "Dripping colors that slide down", Category.WILD),
		MIRROR("Mirror", "The left half reflected on the right", Category.WILD),
		DREAM("Dream", "Glowing dream with shifted hues", Category.WILD);

		public final String label;
		public final String description;
		public final Category category;

		Filter(String label, String description, Category category) {
			this.label = label;
			this.description = description;
			this.category = category;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	// ------------------------------------------------------------------------------------ pipeline

	/**
	 * Applies every edit of the settings, in this order: crop, straighten, rotate / flip, resize, color adjustments,
	 * sharpness and blur, filters, vignette and grain, border, rounded corners, watermark
	 */
	public static BufferedImage apply(BufferedImage source, ImageSettings s) {
		BufferedImage current = source;
		if (s.hasCrop()) {
			current = crop(current, s);
		}
		if (s.straighten != 0) {
			current = straighten(current, s.straighten);
		}
		if (s.rotation != 0 || s.flipH || s.flipV) {
			current = rotateFlip(current, s.rotation, s.flipH, s.flipV);
		}
		if (s.resizeMode != ImageSettings.ResizeMode.NONE) {
			int[] size = targetSize(current.getWidth(), current.getHeight(), s);
			if (size[0] != current.getWidth() || size[1] != current.getHeight()) {
				current = resize(current, size[0], size[1], s.resampling);
			}
		}
		if (s.hasColorChanges()) {
			current = adjust(current, s);
		}
		if (s.sharpness > 0) {
			current = unsharp(current, 2, s.sharpness / 100.0 * 2.5);
		}
		if (s.blur > 0) {
			current = fromPixels(blurPixels(pixels(current), current.getWidth(), current.getHeight(), s.blur), current.getWidth(), current.getHeight());
		}
		if (!s.filters.isEmpty()) {
			BufferedImage before = current;
			for (Filter filter : s.filters) {
				current = filter(current, filter);
			}
			if (s.filterStrength < 100) {
				current = mix(before, current, s.filterStrength / 100.0);
			}
		}
		if (s.vignette > 0) {
			current = vignette(current, s.vignette / 100.0 * 0.9);
		}
		if (s.grain > 0) {
			current = grain(current, s.grain / 100.0 * 45);
		}
		if (s.borderWidth > 0) {
			current = border(current, s.borderWidth, s.borderColor == null ? Color.WHITE : s.borderColor);
		}
		if (s.corners > 0) {
			current = roundCorners(current, s.corners);
		}
		if (s.hasWatermark()) {
			current = watermark(current, s);
		}
		return current;
	}

	/** Area kept by the crop: x, y, width, height. The ratio, when set, takes the biggest centered area inside the margins. */
	public static int[] cropRect(int w, int h, ImageSettings s) {
		int x0 = (int) Math.round(w * s.cropLeft / 100.0);
		int y0 = (int) Math.round(h * s.cropTop / 100.0);
		int cw = Math.max(1, w - x0 - (int) Math.round(w * s.cropRight / 100.0));
		int ch = Math.max(1, h - y0 - (int) Math.round(h * s.cropBottom / 100.0));
		if (s.cropRatio != ImageSettings.CropRatio.FREE) {
			double ratio = (double) s.cropRatio.w / s.cropRatio.h;
			if (cw / (double) ch > ratio) {
				int nw = Math.max(1, (int) Math.round(ch * ratio));
				x0 += (cw - nw) / 2;
				cw = nw;
			} else {
				int nh = Math.max(1, (int) Math.round(cw / ratio));
				y0 += (ch - nh) / 2;
				ch = nh;
			}
		}
		x0 = Math.max(0, Math.min(w - 1, x0));
		y0 = Math.max(0, Math.min(h - 1, y0));
		return new int[] {x0, y0, Math.max(1, Math.min(cw, w - x0)), Math.max(1, Math.min(ch, h - y0))};
	}

	public static BufferedImage crop(BufferedImage src, ImageSettings s) {
		int[] r = cropRect(src.getWidth(), src.getHeight(), s);
		BufferedImage out = new BufferedImage(r[2], r[3], BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(src, -r[0], -r[1], null);
		g.dispose();
		return out;
	}

	/** Rotates by a small angle and enlarges just enough to leave no empty corner: same size as the source */
	public static BufferedImage straighten(BufferedImage src, int degrees) {
		int w = src.getWidth();
		int h = src.getHeight();
		double theta = Math.toRadians(degrees);
		double scale = Math.abs(Math.cos(theta)) + Math.abs(Math.sin(theta)) * Math.max(w / (double) h, h / (double) w);
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		AffineTransform at = new AffineTransform();
		at.translate(w / 2.0, h / 2.0);
		at.rotate(theta);
		at.scale(scale, scale);
		at.translate(-w / 2.0, -h / 2.0);
		g.drawImage(src, at, null);
		g.dispose();
		return out;
	}

	public static BufferedImage border(BufferedImage src, int width, Color color) {
		int bw = Math.max(1, Math.min(width, (30000 - Math.max(src.getWidth(), src.getHeight())) / 2));
		BufferedImage out = new BufferedImage(src.getWidth() + 2 * bw, src.getHeight() + 2 * bw, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setColor(color);
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.drawImage(src, bw, bw, null);
		g.dispose();
		return out;
	}

	/** Rounded corners with a smooth edge: percent of the shorter side (50 makes a circle on a square) */
	public static BufferedImage roundCorners(BufferedImage src, int percent) {
		int w = src.getWidth();
		int h = src.getHeight();
		double diameter = Math.min(w, h) * percent / 100.0 * 2;
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, diameter, diameter));
		g.setComposite(java.awt.AlphaComposite.SrcIn);
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return out;
	}

	/** Text on the image (with a soft shadow, to read on any background) */
	public static BufferedImage watermark(BufferedImage src, ImageSettings s) {
		int w = src.getWidth();
		int h = src.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		int size = Math.max(10, (int) Math.round(h * s.watermarkSize / 100.0));
		g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, size));
		java.awt.FontMetrics fm = g.getFontMetrics();
		String text = s.watermark.trim();
		int tw = fm.stringWidth(text);
		int margin = Math.max(6, size * 6 / 10);
		int x;
		int y;
		switch (s.watermarkPosition) {
		case BOTTOM_LEFT:
			x = margin;
			y = h - margin - fm.getDescent();
			break;
		case TOP_RIGHT:
			x = w - tw - margin;
			y = margin + fm.getAscent();
			break;
		case TOP_LEFT:
			x = margin;
			y = margin + fm.getAscent();
			break;
		case CENTER:
			x = (w - tw) / 2;
			y = (h + fm.getAscent() - fm.getDescent()) / 2;
			break;
		case BOTTOM_CENTER:
			x = (w - tw) / 2;
			y = h - margin - fm.getDescent();
			break;
		case TOP_CENTER:
			x = (w - tw) / 2;
			y = margin + fm.getAscent();
			break;
		default:
			x = w - tw - margin;
			y = h - margin - fm.getDescent();
			break;
		}
		double alpha = s.watermarkOpacity / 100.0;
		int offset = Math.max(1, size / 16);
		g.setColor(new Color(0, 0, 0, (int) Math.round(alpha * 150)));
		g.drawString(text, x + offset, y + offset);
		Color c = s.watermarkColor == null ? Color.WHITE : s.watermarkColor;
		g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(alpha * 255)));
		g.drawString(text, x, y);
		g.dispose();
		return out;
	}

	/** Blend of two images of the same size: 0 = first, 1 = second */
	private static BufferedImage mix(BufferedImage first, BufferedImage second, double amount) {
		final int w = second.getWidth();
		final int h = second.getHeight();
		if (first.getWidth() != w || first.getHeight() != h) {
			return second;
		}
		final int[] a = pixels(first);
		final int[] b = pixels(second);
		final double keep = 1.0 - amount;
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				int p = a[i];
				int q = b[i];
				int alpha = clamp((p >>> 24) * keep + (q >>> 24) * amount);
				int r = clamp(((p >> 16) & 0xff) * keep + ((q >> 16) & 0xff) * amount);
				int g = clamp(((p >> 8) & 0xff) * keep + ((q >> 8) & 0xff) * amount);
				int bl = clamp((p & 0xff) * keep + (q & 0xff) * amount);
				a[i] = (alpha << 24) | (r << 16) | (g << 8) | bl;
			}
		});
		return fromPixels(a, w, h);
	}

	/** Output size for the resize mode: never zero, never beyond a safe limit */
	public static int[] targetSize(int w, int h, ImageSettings s) {
		double rw = w;
		double rh = h;
		switch (s.resizeMode) {
		case PERCENT:
			rw = w * s.percent / 100.0;
			rh = h * s.percent / 100.0;
			break;
		case FIT_BOX: {
			double k = Math.min(s.width / (double) w, s.height / (double) h);
			rw = w * k;
			rh = h * k;
			break;
		}
		case WIDTH:
			rw = s.width;
			rh = h * (s.width / (double) w);
			break;
		case HEIGHT:
			rh = s.height;
			rw = w * (s.height / (double) h);
			break;
		case EXACT:
			rw = s.width;
			rh = s.height;
			break;
		default:
			break;
		}
		return new int[] {(int) Math.max(1, Math.min(30000, Math.round(rw))), (int) Math.max(1, Math.min(30000, Math.round(rh)))};
	}

	// ------------------------------------------------------------------------------------ geometry

	public static BufferedImage argb(BufferedImage source) {
		if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
			return source;
		}
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(source, 0, 0, null);
		g.dispose();
		return out;
	}

	/** Clockwise rotation by multiples of 90 degrees, then optional flips */
	public static BufferedImage rotateFlip(BufferedImage src, int degreesClockwise, boolean flipH, boolean flipV) {
		int w = src.getWidth();
		int h = src.getHeight();
		int turns = ((degreesClockwise / 90) % 4 + 4) % 4;
		boolean quarter = turns % 2 == 1;
		BufferedImage out = new BufferedImage(quarter ? h : w, quarter ? w : h, BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		if (turns == 1) {
			at.translate(h, 0);
			at.rotate(Math.PI / 2);
		} else if (turns == 2) {
			at.translate(w, h);
			at.rotate(Math.PI);
		} else if (turns == 3) {
			at.translate(0, w);
			at.rotate(-Math.PI / 2);
		}
		if (flipH) {
			at.translate(w, 0);
			at.scale(-1, 1);
		}
		if (flipV) {
			at.translate(0, h);
			at.scale(1, -1);
		}
		Graphics2D g = out.createGraphics();
		g.drawImage(src, at, null);
		g.dispose();
		return out;
	}

	public static BufferedImage resize(BufferedImage src, int nw, int nh, ImageSettings.Resampling mode) {
		BufferedImage current = argb(src);
		if (mode == ImageSettings.Resampling.NEAREST) {
			return draw(current, nw, nh, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		}
		if (mode == ImageSettings.Resampling.BILINEAR) {
			return draw(current, nw, nh, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		}
		Object hint = mode == ImageSettings.Resampling.BICUBIC ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		// reducing a lot in one step makes aliasing: halve until the last step
		while (current.getWidth() / 2 >= nw && current.getHeight() / 2 >= nh) {
			current = draw(current, current.getWidth() / 2, current.getHeight() / 2, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		}
		if (current.getWidth() != nw || current.getHeight() != nh) {
			current = draw(current, nw, nh, hint);
		}
		return current;
	}

	private static BufferedImage draw(BufferedImage src, int nw, int nh, Object hint) {
		BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(src, 0, 0, nw, nh, null);
		g.dispose();
		return out;
	}

	// ------------------------------------------------------------------------------------ color

	private static int clamp(double v) {
		return v < 0 ? 0 : (v > 255 ? 255 : (int) Math.round(v));
	}

	private static int[] pixels(BufferedImage img) {
		BufferedImage a = argb(img);
		return a.getRGB(0, 0, a.getWidth(), a.getHeight(), null, 0, a.getWidth());
	}

	private static BufferedImage fromPixels(int[] px, int w, int h) {
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		out.setRGB(0, 0, w, h, px, 0, w);
		return out;
	}

	/** Brightness, contrast, gamma (one lookup table), then temperature, hue and saturation per pixel */
	public static BufferedImage adjust(BufferedImage src, ImageSettings s) {
		int w = src.getWidth();
		int h = src.getHeight();
		final int[] px = pixels(src);

		final int[] lut = new int[256];
		final double exposureGain = Math.pow(2.0, s.exposure / 50.0);
		double contrastScale = 1.0 + s.contrast / 100.0;
		for (int i = 0; i < 256; i++) {
			double v = Math.min(1.0, i / 255.0 * exposureGain);
			if (s.shadows != 0) {
				v += s.shadows / 100.0 * 0.25 * (1 - v) * (1 - v);
			}
			if (s.highlights != 0) {
				v += s.highlights / 100.0 * 0.25 * v * v;
			}
			v = Math.max(0.0, Math.min(1.0, v));
			v = Math.pow(v, 1.0 / Math.max(0.05, s.gamma));
			v = (v * 255.0 - 128.0) * contrastScale + 128.0 + s.brightness * 2.55;
			lut[i] = clamp(v);
		}
		final double satFactor = 1.0 + s.saturation / 100.0;
		final int temp = s.temperature;
		final int tint = s.tint;
		final double vib = s.vibrance / 100.0;
		final boolean doHue = s.hue != 0;
		double rad = Math.toRadians(s.hue);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		// hue rotation matrix in RGB space
		final double[] m = {
				0.213 + cos * 0.787 - sin * 0.213, 0.715 - cos * 0.715 - sin * 0.715, 0.072 - cos * 0.072 + sin * 0.928,
				0.213 - cos * 0.213 + sin * 0.143, 0.715 + cos * 0.285 + sin * 0.140, 0.072 - cos * 0.072 - sin * 0.283,
				0.213 - cos * 0.213 - sin * 0.787, 0.715 - cos * 0.715 + sin * 0.715, 0.072 + cos * 0.928 + sin * 0.072};

		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				int p = px[i];
				double r = lut[(p >> 16) & 0xff];
				double g = lut[(p >> 8) & 0xff];
				double b = lut[p & 0xff];
				if (temp != 0) {
					r += temp * 0.6;
					b -= temp * 0.6;
				}
				if (tint != 0) {
					g -= tint * 0.4;
				}
				if (doHue) {
					double nr = m[0] * r + m[1] * g + m[2] * b;
					double ng = m[3] * r + m[4] * g + m[5] * b;
					double nb = m[6] * r + m[7] * g + m[8] * b;
					r = nr;
					g = ng;
					b = nb;
				}
				if (satFactor != 1.0) {
					double lum = 0.299 * r + 0.587 * g + 0.114 * b;
					r = lum + (r - lum) * satFactor;
					g = lum + (g - lum) * satFactor;
					b = lum + (b - lum) * satFactor;
				}
				if (vib != 0) {
					double mx = Math.max(r, Math.max(g, b));
					double mn = Math.min(r, Math.min(g, b));
					double factor = 1 + vib * (1 - Math.min(1.0, (mx - mn) / 255.0));
					double lum2 = 0.299 * r + 0.587 * g + 0.114 * b;
					r = lum2 + (r - lum2) * factor;
					g = lum2 + (g - lum2) * factor;
					b = lum2 + (b - lum2) * factor;
				}
				px[i] = (p & 0xff000000) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
			}
		});
		return fromPixels(px, w, h);
	}

	// ------------------------------------------------------------------------------------ filters

	public static BufferedImage filter(BufferedImage src, Filter filter) {
		int w = src.getWidth();
		int h = src.getHeight();
		switch (filter) {
		case SHARPEN:
			return convolve(src, new float[] {0, -1, 0, -1, 5, -1, 0, -1, 0});
		case BLUR:
			return convolve(src, new float[] {1 / 16f, 2 / 16f, 1 / 16f, 2 / 16f, 4 / 16f, 2 / 16f, 1 / 16f, 2 / 16f, 1 / 16f});
		case BLUR_STRONG: {
			float[] k = new float[25];
			float[] row = {1, 4, 6, 4, 1};
			for (int y = 0; y < 5; y++) {
				for (int x = 0; x < 5; x++) {
					k[y * 5 + x] = row[y] * row[x] / 256f;
				}
			}
			BufferedImage once = convolve(src, k, 5);
			return convolve(once, k, 5);
		}
		case PIXELATE:
			return pixelate(src, Math.max(4, Math.max(w, h) / 80));
		case EDGES:
			return edges(src);
		case EMBOSS:
			return emboss(src);
		case AUTO_CONTRAST:
			return autoContrast(src);
		case VIGNETTE:
			return vignette(src, 0.55);
		case SHARPEN_STRONG:
			return convolve(src, new float[] {-1, -1, -1, -1, 9, -1, -1, -1, -1});
		case DENOISE:
			return median(src);
		case GRAIN:
			return grain(src, 26);
		case GLOW:
			return glow(src);
		case SKETCH:
			return sketch(src);
		case CLARITY:
			return clarity(src, 0.7);
		case HDR:
			return filter(clarity(src, 1.3), Filter.VIVID);
		case SOFT_FOCUS:
			return softFocus(src);
		case OIL:
			return oil(src);
		case CARTOON:
			return cartoon(src);
		case GLITCH:
			return glitch(src);
		case RAINBOW:
			return mapPixels(src, ImageEdit::rainbow);
		case ACID:
			return mapPixels(src, ImageEdit::acid);
		case THERMAL:
			return mapPixels(src, ImageEdit::thermal);
		case POP_ART:
			return mapPixels(src, ImageEdit::popArt);
		case NEON:
			return neon(src);
		case KALEIDOSCOPE:
			return kaleidoscope(src);
		case SWIRL:
			return swirl(src);
		case RIPPLE:
			return ripple(src);
		case BULGE:
			return bulge(src);
		case MELT:
			return melt(src);
		case MIRROR:
			return mirror(src);
		case DREAM:
			return dream(src);
		default:
			break;
		}
		final int[] px = pixels(src);
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				px[i] = pixelFilter(filter, px[i]);
			}
		});
		BufferedImage out = fromPixels(px, w, h);
		return filter == Filter.VINTAGE ? vignette(out, 0.35) : filter == Filter.LOMO ? vignette(out, 0.6) : out;
	}

	private static int pixelFilter(Filter filter, int p) {
		int a = p & 0xff000000;
		double r = (p >> 16) & 0xff;
		double g = (p >> 8) & 0xff;
		double b = p & 0xff;
		double lum = 0.299 * r + 0.587 * g + 0.114 * b;
		switch (filter) {
		case GRAYSCALE:
			r = lum;
			g = lum;
			b = lum;
			break;
		case SEPIA: {
			double nr = 0.393 * r + 0.769 * g + 0.189 * b;
			double ng = 0.349 * r + 0.686 * g + 0.168 * b;
			double nb = 0.272 * r + 0.534 * g + 0.131 * b;
			r = nr;
			g = ng;
			b = nb;
			break;
		}
		case VINTAGE: {
			double nr = 0.393 * r + 0.769 * g + 0.189 * b;
			double ng = 0.349 * r + 0.686 * g + 0.168 * b;
			double nb = 0.272 * r + 0.534 * g + 0.131 * b;
			// half sepia, half original, lifted blacks
			r = (r + nr) / 2 * 0.88 + 24;
			g = (g + ng) / 2 * 0.88 + 22;
			b = (b + nb) / 2 * 0.88 + 26;
			break;
		}
		case WARM:
			r += 18;
			g += 4;
			b -= 18;
			break;
		case COOL:
			r -= 16;
			g += 2;
			b += 20;
			break;
		case FADE:
			r = (r - 128) * 0.8 + 128 + 14;
			g = (g - 128) * 0.8 + 128 + 14;
			b = (b - 128) * 0.8 + 128 + 14;
			break;
		case INVERT:
			r = 255 - r;
			g = 255 - g;
			b = 255 - b;
			break;
		case NOIR: {
			double v = (lum - 128) * 1.45 + 118;
			r = v;
			g = v;
			b = v;
			break;
		}
		case VIVID:
			r = (lum + (r - lum) * 1.38 - 128) * 1.08 + 128;
			g = (lum + (g - lum) * 1.38 - 128) * 1.08 + 128;
			b = (lum + (b - lum) * 1.38 - 128) * 1.08 + 128;
			break;
		case MATTE:
			r = (lum + (r - lum) * 0.82) * 0.86 + 30;
			g = (lum + (g - lum) * 0.82) * 0.86 + 30;
			b = (lum + (b - lum) * 0.82) * 0.86 + 34;
			break;
		case CROSS_PROCESS:
			r = (r - 128) * 1.25 + 138;
			g = (g - 128) * 1.1 + 128;
			b = b * 0.78 + 42;
			break;
		case CINEMATIC: {
			double t = (lum - 128) / 128.0; // -1 shadows .. +1 highlights
			r += t * 30 + 4;
			g += 6 - Math.abs(t) * 8;
			b -= t * 30 - 6;
			r = (r - 128) * 1.1 + 128;
			g = (g - 128) * 1.1 + 128;
			b = (b - 128) * 1.1 + 128;
			break;
		}
		case CYANOTYPE: {
			double t = lum / 255.0;
			r = 4 + t * 210;
			g = 28 + t * 208;
			b = 64 + t * 191;
			break;
		}
		case SOLARIZE:
			r = r > 128 ? 255 - r : r;
			g = g > 128 ? 255 - g : g;
			b = b > 128 ? 255 - b : b;
			r *= 1.6;
			g *= 1.6;
			b *= 1.6;
			break;
		case LOMO:
			r = (lum + ((r - 128) * 1.3 + 134 - lum) * 1.25);
			g = (lum + ((g - 128) * 1.25 + 128 - lum) * 1.25);
			b = (lum + ((b - 128) * 1.15 + 120 - lum) * 1.25);
			break;
		case BLEACH:
			r = ((r + lum) / 2 - 128) * 1.35 + 128;
			g = ((g + lum) / 2 - 128) * 1.35 + 128;
			b = ((b + lum) / 2 - 128) * 1.35 + 128;
			break;
		case GOLDEN:
			r = r * 1.1 + 16;
			g = g * 1.04 + 8;
			b = b * 0.86;
			break;
		case NIGHT:
			r = (r * 0.62 - 128) * 1.1 + 128;
			g = (g * 0.78 - 128) * 1.1 + 128;
			b = (b + 18 - 128) * 1.1 + 128;
			break;
		case DUSK: {
			double t = lum / 255.0;
			r = 40 + t * 215;
			g = 16 + t * 160;
			b = 80 + t * 40;
			break;
		}
		case FILM: {
			double t = 1 - lum / 255.0;
			r = (r - 128) * 0.88 + 136 - t * 6;
			g = (g - 128) * 0.9 + 138 + t * 8;
			b = (b - 128) * 0.88 + 142 + t * 14;
			break;
		}
		case INFRARED: {
			double nr = g * 1.15 + 10;
			double ng = r * 0.55 + b * 0.45;
			double nb = b * 0.85;
			r = nr;
			g = ng;
			b = nb;
			break;
		}
		case POSTERIZE:
			r = Math.round(r / 64.0) * 64;
			g = Math.round(g / 64.0) * 64;
			b = Math.round(b / 64.0) * 64;
			break;
		case THRESHOLD: {
			double v = lum >= 128 ? 255 : 0;
			r = v;
			g = v;
			b = v;
			break;
		}
		default:
			break;
		}
		return a | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
	}

	private static BufferedImage convolve(BufferedImage src, float[] kernel) {
		return convolve(src, kernel, 3);
	}

	private static BufferedImage convolve(BufferedImage src, float[] kernel, int size) {
		BufferedImage in = argb(src);
		return new ConvolveOp(new Kernel(size, size, kernel), ConvolveOp.EDGE_NO_OP, null).filter(in, null);
	}

	private static BufferedImage pixelate(BufferedImage src, int block) {
		int w = src.getWidth();
		int h = src.getHeight();
		int[] px = pixels(src);
		for (int by = 0; by < h; by += block) {
			for (int bx = 0; bx < w; bx += block) {
				int ex = Math.min(w, bx + block);
				int ey = Math.min(h, by + block);
				long a = 0;
				long r = 0;
				long g = 0;
				long b = 0;
				int n = 0;
				for (int y = by; y < ey; y++) {
					for (int x = bx; x < ex; x++) {
						int p = px[y * w + x];
						a += p >>> 24;
						r += (p >> 16) & 0xff;
						g += (p >> 8) & 0xff;
						b += p & 0xff;
						n++;
					}
				}
				int avg = ((int) (a / n) << 24) | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
				for (int y = by; y < ey; y++) {
					Arrays.fill(px, y * w + bx, y * w + ex, avg);
				}
			}
		}
		return fromPixels(px, w, h);
	}

	/** 3x3 median per channel: removes isolated specks without softening the edges much */
	private static BufferedImage median(BufferedImage src) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		final int[] out = px.clone();
		IntStream.range(1, Math.max(1, h - 1)).parallel().forEach(y -> {
			int[] win = new int[9];
			for (int x = 1; x < w - 1; x++) {
				int i = y * w + x;
				int result = px[i] & 0xff000000;
				for (int shift = 16; shift >= 0; shift -= 8) {
					int n = 0;
					for (int dy = -1; dy <= 1; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							win[n++] = (px[i + dy * w + dx] >> shift) & 0xff;
						}
					}
					Arrays.sort(win);
					result |= win[4] << shift;
				}
				out[i] = result;
			}
		});
		return fromPixels(out, w, h);
	}

	/** Analog grain: noise from a hash of the position, so the same image always gets the same grain (stable preview) */
	private static BufferedImage grain(BufferedImage src, double amplitude) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int hash = x * 73856093 ^ y * 19349663;
				hash ^= hash >>> 13;
				hash *= 0x5bd1e995;
				hash ^= hash >>> 15;
				double noise = ((hash & 0xff) - 128) / 128.0 * amplitude;
				int i = y * w + x;
				int p = px[i];
				px[i] = (p & 0xff000000) | (clamp(((p >> 16) & 0xff) + noise) << 16) | (clamp(((p >> 8) & 0xff) + noise) << 8) | clamp((p & 0xff) + noise);
			}
		});
		return fromPixels(px, w, h);
	}

	/** Screen blend of a heavy blur over the image: bright areas bleed light */
	private static BufferedImage glow(BufferedImage src) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		final int[] blur = pixels(filter(src, Filter.BLUR_STRONG));
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				int p = px[i];
				int q = blur[i];
				px[i] = (p & 0xff000000) | (screen(p >> 16, q >> 16) << 16) | (screen(p >> 8, q >> 8) << 8) | screen(p, q);
			}
		});
		return fromPixels(px, w, h);
	}

	private static int screen(int base, int top) {
		double a = (base & 0xff) / 255.0;
		double b = (top & 0xff) / 255.0 * 0.6;
		return clamp((1 - (1 - a) * (1 - b)) * 255.0);
	}

	/** Color dodge of the gray image over its blur: only the edges stay dark, like a pencil drawing */
	private static BufferedImage sketch(BufferedImage src) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		final int[] gray = grayOf(px);
		final int[] blur = grayOf(pixels(filter(src, Filter.BLUR_STRONG)));
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				double v = blur[i] <= 0 ? 255 : Math.min(255.0, gray[i] * 255.0 / blur[i]);
				int c = clamp(255.0 * Math.pow(v / 255.0, 1.6));
				px[i] = (px[i] & 0xff000000) | (c << 16) | (c << 8) | c;
			}
		});
		return fromPixels(px, w, h);
	}

	private static int[] grayOf(int[] px) {
		int[] gray = new int[px.length];
		for (int i = 0; i < px.length; i++) {
			int p = px[i];
			gray[i] = (int) (0.299 * ((p >> 16) & 0xff) + 0.587 * ((p >> 8) & 0xff) + 0.114 * (p & 0xff));
		}
		return gray;
	}

	private static BufferedImage edges(BufferedImage src) {
		int w = src.getWidth();
		int h = src.getHeight();
		int[] px = pixels(src);
		int[] gray = grayOf(px);
		int[] out = new int[px.length];
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
					out[i] = 0xff000000;
					continue;
				}
				int gx = -gray[i - w - 1] - 2 * gray[i - 1] - gray[i + w - 1] + gray[i - w + 1] + 2 * gray[i + 1] + gray[i + w + 1];
				int gy = -gray[i - w - 1] - 2 * gray[i - w] - gray[i - w + 1] + gray[i + w - 1] + 2 * gray[i + w] + gray[i + w + 1];
				int v = clamp(Math.sqrt((double) gx * gx + (double) gy * gy));
				out[i] = 0xff000000 | (v << 16) | (v << 8) | v;
			}
		});
		return fromPixels(out, w, h);
	}

	private static BufferedImage emboss(BufferedImage src) {
		int w = src.getWidth();
		int h = src.getHeight();
		int[] px = pixels(src);
		int[] gray = grayOf(px);
		int[] out = new int[px.length];
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
					out[i] = 0xff808080;
					continue;
				}
				int v = clamp(128 + (-2 * gray[i - w - 1] - gray[i - w] - gray[i - 1] + gray[i + 1] + gray[i + w] + 2 * gray[i + w + 1]) / 2.0);
				out[i] = 0xff000000 | (v << 16) | (v << 8) | v;
			}
		});
		return fromPixels(out, w, h);
	}

	/** Stretches each channel between the 0.5% and 99.5% percentiles */
	private static BufferedImage autoContrast(BufferedImage src) {
		int w = src.getWidth();
		int h = src.getHeight();
		int[] px = pixels(src);
		int[] hist = new int[256];
		for (int p : px) {
			hist[(int) (0.299 * ((p >> 16) & 0xff) + 0.587 * ((p >> 8) & 0xff) + 0.114 * (p & 0xff))]++;
		}
		long cut = Math.max(1, (long) (px.length * 0.005));
		int lo = 0;
		long acc = 0;
		while (lo < 254 && (acc += hist[lo]) < cut) {
			lo++;
		}
		int hi = 255;
		acc = 0;
		while (hi > lo + 1 && (acc += hist[hi]) < cut) {
			hi--;
		}
		if (hi - lo < 8) {
			return fromPixels(px, w, h);
		}
		final int[] lut = new int[256];
		for (int i = 0; i < 256; i++) {
			lut[i] = clamp((i - lo) * 255.0 / (hi - lo));
		}
		for (int i = 0; i < px.length; i++) {
			int p = px[i];
			px[i] = (p & 0xff000000) | (lut[(p >> 16) & 0xff] << 16) | (lut[(p >> 8) & 0xff] << 8) | lut[p & 0xff];
		}
		return fromPixels(px, w, h);
	}

	private static BufferedImage vignette(BufferedImage src, double strength) {
		int w = src.getWidth();
		int h = src.getHeight();
		int[] px = pixels(src);
		double cx = w / 2.0;
		double cy = h / 2.0;
		double maxDist = Math.sqrt(cx * cx + cy * cy);
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / maxDist;
				double f = 1.0 - strength * Math.max(0, d - 0.35) / 0.65 * Math.max(0, d - 0.35) / 0.65 * 1.6;
				f = Math.max(0.0, f);
				int i = y * w + x;
				int p = px[i];
				px[i] = (p & 0xff000000) | (clamp(((p >> 16) & 0xff) * f) << 16) | (clamp(((p >> 8) & 0xff) * f) << 8) | clamp((p & 0xff) * f);
			}
		});
		return fromPixels(px, w, h);
	}

	// ------------------------------------------------------------------------------------ blur based tools

	private static int clampInt(int v, int min, int max) {
		return v < min ? min : (v > max ? max : v);
	}

	/** One box blur pass over every channel (sliding window, all cores) */
	private static int[] boxPass(final int[] src, final int w, final int h, final int r, final boolean horizontal) {
		final int[] out = new int[src.length];
		final int lines = horizontal ? h : w;
		final int len = horizontal ? w : h;
		final int step = horizontal ? 1 : w;
		final int size = 2 * r + 1;
		IntStream.range(0, lines).parallel().forEach(line -> {
			int base = horizontal ? line * w : line;
			int a = 0;
			int rr = 0;
			int g = 0;
			int b = 0;
			for (int i = -r; i <= r; i++) {
				int p = src[base + clampInt(i, 0, len - 1) * step];
				a += p >>> 24;
				rr += (p >> 16) & 0xff;
				g += (p >> 8) & 0xff;
				b += p & 0xff;
			}
			for (int i = 0; i < len; i++) {
				out[base + i * step] = ((a / size) << 24) | ((rr / size) << 16) | ((g / size) << 8) | (b / size);
				int add = src[base + Math.min(len - 1, i + r + 1) * step];
				int rem = src[base + Math.max(0, i - r) * step];
				a += (add >>> 24) - (rem >>> 24);
				rr += ((add >> 16) & 0xff) - ((rem >> 16) & 0xff);
				g += ((add >> 8) & 0xff) - ((rem >> 8) & 0xff);
				b += (add & 0xff) - (rem & 0xff);
			}
		});
		return out;
	}

	/** Two box blurs in both directions: close to a Gaussian blur of that radius, in linear time */
	private static int[] blurPixels(int[] px, int w, int h, int radius) {
		int r = clampInt(radius, 1, Math.max(1, Math.min(w, h) / 2 - 1));
		int[] a = boxPass(boxPass(px, w, h, r, true), w, h, r, false);
		return boxPass(boxPass(a, w, h, r, true), w, h, r, false);
	}

	/** Unsharp mask: the difference from the blurred copy is added back; the radius sets the size of what is sharpened */
	private static BufferedImage unsharp(BufferedImage src, int radius, double amount) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		final int[] blur = blurPixels(px, w, h, radius);
		final int[] out = new int[px.length];
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				int p = px[i];
				int q = blur[i];
				int r = clamp(((p >> 16) & 0xff) + (((p >> 16) & 0xff) - ((q >> 16) & 0xff)) * amount);
				int g = clamp(((p >> 8) & 0xff) + (((p >> 8) & 0xff) - ((q >> 8) & 0xff)) * amount);
				int b = clamp((p & 0xff) + ((p & 0xff) - (q & 0xff)) * amount);
				out[i] = (p & 0xff000000) | (r << 16) | (g << 8) | b;
			}
		});
		return fromPixels(out, w, h);
	}

	private static BufferedImage clarity(BufferedImage src, double amount) {
		int radius = clampInt(Math.max(src.getWidth(), src.getHeight()) / 120, 3, 40);
		return unsharp(src, radius, amount);
	}

	private static BufferedImage softFocus(BufferedImage src) {
		int radius = clampInt(Math.max(src.getWidth(), src.getHeight()) / 150, 2, 30);
		BufferedImage blurred = fromPixels(blurPixels(pixels(src), src.getWidth(), src.getHeight(), radius), src.getWidth(), src.getHeight());
		return mix(src, blurred, 0.55);
	}

	/** Oil paint: each pixel takes the average color of the most frequent brightness around it */
	private static BufferedImage oil(BufferedImage src) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		final int[] out = new int[px.length];
		final int radius = (long) w * h > 4000000L ? 2 : 3;
		final int levels = 20;
		IntStream.range(0, h).parallel().forEach(y -> {
			int[] count = new int[levels];
			int[] sr = new int[levels];
			int[] sg = new int[levels];
			int[] sb = new int[levels];
			for (int x = 0; x < w; x++) {
				Arrays.fill(count, 0);
				Arrays.fill(sr, 0);
				Arrays.fill(sg, 0);
				Arrays.fill(sb, 0);
				for (int dy = -radius; dy <= radius; dy++) {
					int yy = clampInt(y + dy, 0, h - 1) * w;
					for (int dx = -radius; dx <= radius; dx++) {
						int p = px[yy + clampInt(x + dx, 0, w - 1)];
						int r = (p >> 16) & 0xff;
						int g = (p >> 8) & 0xff;
						int b = p & 0xff;
						int bin = (r + g + b) * (levels - 1) / 765;
						count[bin]++;
						sr[bin] += r;
						sg[bin] += g;
						sb[bin] += b;
					}
				}
				int best = 0;
				for (int k = 1; k < levels; k++) {
					if (count[k] > count[best]) {
						best = k;
					}
				}
				int n = Math.max(1, count[best]);
				out[y * w + x] = (px[y * w + x] & 0xff000000) | ((sr[best] / n) << 16) | ((sg[best] / n) << 8) | (sb[best] / n);
			}
		});
		return fromPixels(out, w, h);
	}

	/** Smoothed flat colors with dark outlines where the edge filter is strong */
	private static BufferedImage cartoon(BufferedImage src) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(median(src));
		final int[] edge = grayOf(pixels(edges(src)));
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				int p = px[i];
				if (edge[i] > 60) {
					px[i] = (p & 0xff000000) | 0x00181818;
				} else {
					int r = clamp(Math.round(((p >> 16) & 0xff) / 51.0) * 51);
					int g = clamp(Math.round(((p >> 8) & 0xff) / 51.0) * 51);
					int b = clamp(Math.round((p & 0xff) / 51.0) * 51);
					px[i] = (p & 0xff000000) | (r << 16) | (g << 8) | b;
				}
			}
		});
		return fromPixels(px, w, h);
	}

	/** Digital glitch: red and blue slide apart, some horizontal bands jump sideways */
	private static BufferedImage glitch(BufferedImage src) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		final int[] out = new int[px.length];
		final int shift = Math.max(2, w / 90);
		final int bandHeight = Math.max(4, h / 14);
		IntStream.range(0, h).parallel().forEach(y -> {
			int band = y / bandHeight;
			int hash = band * 0x9E3779B1;
			hash ^= hash >>> 15;
			hash &= 0x7fffffff;
			int jump = hash % 4 == 0 ? (hash >>> 4) % (shift * 6 + 1) - shift * 3 : 0;
			int row = y * w;
			for (int x = 0; x < w; x++) {
				int rp = px[row + clampInt(x + shift + jump, 0, w - 1)];
				int gp = px[row + clampInt(x + jump, 0, w - 1)];
				int bp = px[row + clampInt(x - shift + jump, 0, w - 1)];
				out[row + x] = (gp & 0xff000000) | (rp & 0x00ff0000) | (gp & 0x0000ff00) | (bp & 0x000000ff);
			}
		});
		return fromPixels(out, w, h);
	}

	// ------------------------------------------------------------------------------------ wild filters

	private static int hsv(double h, double s, double v, int alpha) {
		double hue = h - Math.floor(h);
		double c = v * s;
		double x = c * (1 - Math.abs((hue * 6) % 2 - 1));
		double m = v - c;
		double r;
		double g;
		double b;
		switch ((int) (hue * 6) % 6) {
		case 0:
			r = c; g = x; b = 0;
			break;
		case 1:
			r = x; g = c; b = 0;
			break;
		case 2:
			r = 0; g = c; b = x;
			break;
		case 3:
			r = 0; g = x; b = c;
			break;
		case 4:
			r = x; g = 0; b = c;
			break;
		default:
			r = c; g = 0; b = x;
			break;
		}
		return (alpha & 0xff000000) | (clamp((r + m) * 255) << 16) | (clamp((g + m) * 255) << 8) | clamp((b + m) * 255);
	}

	private static double hueOf(int r, int g, int b) {
		int max = Math.max(r, Math.max(g, b));
		int min = Math.min(r, Math.min(g, b));
		if (max == min) {
			return 0;
		}
		double d = max - min;
		double h;
		if (max == r) {
			h = ((g - b) / d) % 6;
		} else if (max == g) {
			h = (b - r) / d + 2;
		} else {
			h = (r - g) / d + 4;
		}
		h /= 6;
		return h < 0 ? h + 1 : h;
	}

	private static int lumOf(int p) {
		return (int) (0.299 * ((p >> 16) & 0xff) + 0.587 * ((p >> 8) & 0xff) + 0.114 * (p & 0xff));
	}

	private static BufferedImage mapPixels(BufferedImage src, final java.util.function.IntUnaryOperator op) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				px[y * w + x] = op.applyAsInt(px[y * w + x]);
			}
		});
		return fromPixels(px, w, h);
	}

	/** Brightness mapped on the whole rainbow: dark = red-violet, light = yellow-green-cyan */
	private static int rainbow(int p) {
		double t = lumOf(p) / 255.0;
		return hsv(t * 1.6 + 0.02, 1, 0.3 + 0.7 * Math.sqrt(t), p);
	}

	/** Colors rotated by the brightness, at full saturation and brighter: an acid trip */
	private static int acid(int p) {
		int r = (p >> 16) & 0xff;
		int g = (p >> 8) & 0xff;
		int b = p & 0xff;
		double v = Math.min(1.0, Math.max(r, Math.max(g, b)) / 255.0 * 1.35 + 0.08);
		return hsv(hueOf(r, g, b) + lumOf(p) / 255.0 * 0.9, 1, v, p);
	}

	private static final int[][] THERMAL = {{0, 0, 0}, {40, 0, 160}, {200, 0, 120}, {255, 120, 0}, {255, 230, 60}, {255, 255, 255}};

	/** Thermal camera palette: black, blue, magenta, orange, yellow, white */
	private static int thermal(int p) {
		double t = lumOf(p) / 255.0 * (THERMAL.length - 1);
		int i = Math.min(THERMAL.length - 2, (int) t);
		double f = t - i;
		int r = clamp(THERMAL[i][0] + (THERMAL[i + 1][0] - THERMAL[i][0]) * f);
		int g = clamp(THERMAL[i][1] + (THERMAL[i + 1][1] - THERMAL[i][1]) * f);
		int b = clamp(THERMAL[i][2] + (THERMAL[i + 1][2] - THERMAL[i][2]) * f);
		return (p & 0xff000000) | (r << 16) | (g << 8) | b;
	}

	private static final int[] POP = {0x14145A, 0xDC1E46, 0xFFC81E, 0xFFF5DC};

	/** Four flat colors chosen by brightness, like a screen print */
	private static int popArt(int p) {
		int l = lumOf(p);
		int color = l < 64 ? POP[0] : l < 128 ? POP[1] : l < 192 ? POP[2] : POP[3];
		return (p & 0xff000000) | color;
	}

	private static int dreamShift(int p) {
		int r = (p >> 16) & 0xff;
		int g = (p >> 8) & 0xff;
		int b = p & 0xff;
		int max = Math.max(r, Math.max(g, b));
		int min = Math.min(r, Math.min(g, b));
		double s = max == 0 ? 0 : (max - min) / (double) max;
		return hsv(hueOf(r, g, b) + 0.09, Math.min(1.0, s * 1.5 + 0.1), Math.min(1.0, max / 255.0 * 1.08 + 0.04), p);
	}

	private static BufferedImage dream(BufferedImage src) {
		return mapPixels(softFocus(glow(src)), ImageEdit::dreamShift);
	}

	/** Edges drawn as glowing neon lines: the color follows the direction of the edge */
	private static BufferedImage neon(BufferedImage src) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		final int[] gray = grayOf(px);
		final int[] out = new int[px.length];
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
					out[i] = 0xff0a0a14;
					continue;
				}
				int gx = -gray[i - w - 1] - 2 * gray[i - 1] - gray[i + w - 1] + gray[i - w + 1] + 2 * gray[i + 1] + gray[i + w + 1];
				int gy = -gray[i - w - 1] - 2 * gray[i - w] - gray[i - w + 1] + gray[i + w - 1] + 2 * gray[i + w] + gray[i + w + 1];
				double mag = Math.sqrt((double) gx * gx + (double) gy * gy);
				double v = Math.min(1.0, Math.pow(mag / 220.0, 0.7));
				int color = hsv(Math.atan2(gy, gx) / (2 * Math.PI) + 0.5, 1, v, 0xff000000);
				out[i] = 0xff000000 | Math.max(0x0a0a14, color);
			}
		});
		return fromPixels(out, w, h);
	}

	/** Where in the source a pixel of the result comes from */
	private interface Mapper {
		void map(double x, double y, double[] source);
	}

	private static int sample(int[] px, int w, int h, double fx, double fy) {
		double sx = Math.max(0, Math.min(w - 1, fx));
		double sy = Math.max(0, Math.min(h - 1, fy));
		int x0 = (int) sx;
		int y0 = (int) sy;
		int x1 = Math.min(w - 1, x0 + 1);
		int y1 = Math.min(h - 1, y0 + 1);
		double ax = sx - x0;
		double ay = sy - y0;
		int p00 = px[y0 * w + x0];
		int p10 = px[y0 * w + x1];
		int p01 = px[y1 * w + x0];
		int p11 = px[y1 * w + x1];
		int result = 0;
		for (int shift = 24; shift >= 0; shift -= 8) {
			double top = ((p00 >>> shift) & 0xff) * (1 - ax) + ((p10 >>> shift) & 0xff) * ax;
			double bottom = ((p01 >>> shift) & 0xff) * (1 - ax) + ((p11 >>> shift) & 0xff) * ax;
			result |= clamp(top * (1 - ay) + bottom * ay) << shift;
		}
		return result;
	}

	private static BufferedImage warp(BufferedImage src, final Mapper mapper) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = pixels(src);
		final int[] out = new int[px.length];
		IntStream.range(0, h).parallel().forEach(y -> {
			double[] source = new double[2];
			for (int x = 0; x < w; x++) {
				mapper.map(x, y, source);
				out[y * w + x] = sample(px, w, h, source[0], source[1]);
			}
		});
		return fromPixels(out, w, h);
	}

	/** Twists the picture around its center, more in the middle */
	private static BufferedImage swirl(BufferedImage src) {
		final double cx = (src.getWidth() - 1) / 2.0;
		final double cy = (src.getHeight() - 1) / 2.0;
		final double radius = Math.min(src.getWidth(), src.getHeight()) / 2.0 * 1.15;
		return warp(src, (x, y, o) -> {
			double dx = x - cx;
			double dy = y - cy;
			double r = Math.sqrt(dx * dx + dy * dy);
			if (r >= radius) {
				o[0] = x;
				o[1] = y;
				return;
			}
			double t = 1 - r / radius;
			double a = t * t * 5.0;
			double cos = Math.cos(a);
			double sin = Math.sin(a);
			o[0] = cx + dx * cos - dy * sin;
			o[1] = cy + dx * sin + dy * cos;
		});
	}

	/** Fish-eye: the middle swells */
	private static BufferedImage bulge(BufferedImage src) {
		final double cx = (src.getWidth() - 1) / 2.0;
		final double cy = (src.getHeight() - 1) / 2.0;
		final double radius = Math.min(src.getWidth(), src.getHeight()) / 2.0 * 1.2;
		return warp(src, (x, y, o) -> {
			double dx = x - cx;
			double dy = y - cy;
			double r = Math.sqrt(dx * dx + dy * dy);
			double f = r < radius ? Math.pow(r / radius, 0.65) : 1.0;
			o[0] = cx + dx * f;
			o[1] = cy + dy * f;
		});
	}

	/** Water ripples: the picture trembles in both directions */
	private static BufferedImage ripple(BufferedImage src) {
		final double side = Math.min(src.getWidth(), src.getHeight());
		final double amplitude = Math.max(4, side / 45.0);
		final double length = side / 7.0;
		return warp(src, (x, y, o) -> {
			o[0] = x + amplitude * Math.sin(2 * Math.PI * y / length);
			o[1] = y + amplitude * 0.7 * Math.sin(2 * Math.PI * x / (length * 1.3));
		});
	}

	/** Dripping colors: the lower the picture, the more it slides down */
	private static BufferedImage melt(BufferedImage src) {
		final double height = src.getHeight();
		final double amplitude = height / 9.0;
		return warp(src, (x, y, o) -> {
			o[0] = x;
			o[1] = y - amplitude * (0.5 + 0.5 * Math.sin(x * 0.021) + 0.35 * Math.sin(x * 0.057 + 1.3)) * (y / height);
		});
	}

	/** Six mirrored wedges around the center */
	private static BufferedImage kaleidoscope(BufferedImage src) {
		final double cx = (src.getWidth() - 1) / 2.0;
		final double cy = (src.getHeight() - 1) / 2.0;
		final double segment = 2 * Math.PI / 6;
		return warp(src, (x, y, o) -> {
			double dx = x - cx;
			double dy = y - cy;
			double r = Math.sqrt(dx * dx + dy * dy);
			double a = ((Math.atan2(dy, dx) + 0.4) % segment + segment) % segment;
			if (a > segment / 2) {
				a = segment - a;
			}
			a += 0.35;
			o[0] = cx + r * Math.cos(a) * 0.9;
			o[1] = cy + r * Math.sin(a) * 0.9;
		});
	}

	/** The left half reflected on the right */
	private static BufferedImage mirror(BufferedImage src) {
		final double half = src.getWidth() / 2.0;
		final double last = src.getWidth() - 1;
		return warp(src, (x, y, o) -> {
			o[0] = x < half ? x : last - x;
			o[1] = y;
		});
	}

	// ------------------------------------------------------------------------------------ output

	/** Formats this JVM can write, lowercase, with "jpg" for JPEG; same list on Java 8 to 21 (TIFF only where available) */
	public static List<String> writableFormats() {
		TreeSet<String> set = new TreeSet<String>();
		for (String suffix : ImageIO.getWriterFileSuffixes()) {
			String s = suffix.toLowerCase(Locale.ROOT);
			if (s.equals("jpeg")) {
				s = "jpg";
			} else if (s.equals("tif")) {
				s = "tiff";
			}
			if (!s.equals("wbmp")) {
				set.add(s);
			}
		}
		List<String> list = new ArrayList<String>();
		for (String preferred : new String[] {"png", "jpg", "bmp", "gif", "tiff"}) {
			if (set.remove(preferred)) {
				list.add(preferred);
			}
		}
		list.addAll(set);
		return list;
	}

	/** Writes the image in the format of the settings; alpha is flattened on the background color where the format has none */
	public static void write(BufferedImage image, File target, ImageSettings s) throws IOException {
		File parent = target.getAbsoluteFile().getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IOException("Cannot create folder " + parent);
		}
		try (ImageOutputStream out = ImageIO.createImageOutputStream(target)) {
			encode(image, out, s);
		}
	}

	/** Same, to a stream (used to estimate the size of the result without writing a file) */
	public static void write(BufferedImage image, OutputStream target, ImageSettings s) throws IOException {
		try (ImageOutputStream out = ImageIO.createImageOutputStream(target)) {
			encode(image, out, s);
		}
	}

	private static void encode(BufferedImage image, ImageOutputStream out, ImageSettings s) throws IOException {
		String format = s.format.toLowerCase(Locale.ROOT);
		boolean noAlpha = format.equals("jpg") || format.equals("jpeg") || format.equals("bmp");
		BufferedImage toWrite = image;
		if (noAlpha) {
			toWrite = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = toWrite.createGraphics();
			g.setColor(s.background == null ? Color.WHITE : s.background);
			g.fillRect(0, 0, toWrite.getWidth(), toWrite.getHeight());
			g.drawImage(image, 0, 0, null);
			g.dispose();
		}
		String writerFormat = format.equals("jpg") ? "jpeg" : format;
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(writerFormat);
		ImageWriter writer = null;
		while (writers.hasNext()) {
			ImageWriter candidate = writers.next();
			if (candidate.getOriginatingProvider().canEncodeImage(ImageTypeSpecifier.createFromRenderedImage(toWrite))) {
				writer = candidate;
				break;
			}
		}
		if (writer == null) {
			throw new IOException("No writer available for " + format);
		}
		try {
			writer.setOutput(out);
			ImageWriteParam param = writer.getDefaultWriteParam();
			if (writerFormat.equals("jpeg") && param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(Math.max(1, Math.min(100, s.quality)) / 100f);
				if (s.progressive && param.canWriteProgressive()) {
					param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
				}
			}
			writer.write(null, new IIOImage(toWrite, null, null), param);
		} finally {
			writer.dispose();
		}
	}

	/** Reads an image of any supported kind: the ImageIO formats and Photoshop files (the merged picture) */
	public static BufferedImage read(File file) throws IOException {
		if (PsdReader.isPsd(file)) {
			return PsdReader.read(file).image;
		}
		return ImageIO.read(file);
	}

	public static boolean isImageFile(File file) {
		if (file == null || !file.isFile()) {
			return false;
		}
		String name = file.getName().toLowerCase(Locale.ROOT);
		int dot = name.lastIndexOf('.');
		if (dot < 0) {
			return false;
		}
		String ext = name.substring(dot + 1);
		if (ext.equals("psd") || ext.equals("psb") || ext.equals("jfif") || ext.equals("jpe") || ext.equals("dib")) {
			return true;
		}
		for (String suffix : ImageIO.getReaderFileSuffixes()) {
			if (suffix.equalsIgnoreCase(ext)) {
				return true;
			}
		}
		return false;
	}
}
