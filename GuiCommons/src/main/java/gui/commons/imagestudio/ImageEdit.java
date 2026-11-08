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

	/** Ready made looks and effects, from the most used for photos to the creative ones */
	public enum Filter {
		AUTO_CONTRAST("Auto contrast", "Stretches the tones to use the full range"),
		GRAYSCALE("Black & white", "Classic grayscale"),
		SEPIA("Sepia", "Warm brown old photo tone"),
		VINTAGE("Vintage", "Faded warm colors with a soft vignette"),
		WARM("Warm", "Sunset look: more red, less blue"),
		COOL("Cool", "Cold look: more blue, less red"),
		FADE("Fade", "Lifted blacks, softer contrast"),
		VIGNETTE("Vignette", "Darker corners to focus the center"),
		SHARPEN("Sharpen", "Brings out the details"),
		BLUR("Blur", "Soft blur"),
		BLUR_STRONG("Blur (strong)", "Heavy blur, good for backgrounds"),
		EMBOSS("Emboss", "Relief effect"),
		EDGES("Edges", "Outlines of the shapes"),
		POSTERIZE("Posterize", "Few flat tones, poster style"),
		THRESHOLD("Threshold", "Pure black and white, no grays"),
		PIXELATE("Pixelate", "Big blocks, also to hide details"),
		INVERT("Invert", "Negative colors");

		public final String label;
		public final String description;

		Filter(String label, String description) {
			this.label = label;
			this.description = description;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	// ------------------------------------------------------------------------------------ pipeline

	/** Applies every edit of the settings, in this order: rotate / flip, resize, color adjustments, filters */
	public static BufferedImage apply(BufferedImage source, ImageSettings s) {
		BufferedImage current = source;
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
		for (Filter filter : s.filters) {
			current = filter(current, filter);
		}
		return current;
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
		double contrastScale = 1.0 + s.contrast / 100.0;
		for (int i = 0; i < 256; i++) {
			double v = i / 255.0;
			v = Math.pow(v, 1.0 / Math.max(0.05, s.gamma));
			v = (v * 255.0 - 128.0) * contrastScale + 128.0 + s.brightness * 2.55;
			lut[i] = clamp(v);
		}
		final double satFactor = 1.0 + s.saturation / 100.0;
		final int temp = s.temperature;
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
		return filter == Filter.VINTAGE ? vignette(out, 0.35) : out;
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
		for (String suffix : ImageIO.getReaderFileSuffixes()) {
			if (suffix.equalsIgnoreCase(ext)) {
				return true;
			}
		}
		return false;
	}
}
