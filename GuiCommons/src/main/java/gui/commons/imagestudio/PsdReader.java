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

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.InflaterInputStream;

import javax.imageio.ImageIO;

/**
 * Reads Photoshop files (.psd) in pure Java, no library. It gives what Photoshop stores as the "flattened" picture
 * (the merged composite saved with "Maximize Compatibility"), the information of the file (size, color mode, depth,
 * resolution, layers, profile) and the XMP block, which can carry the Camera Raw settings. Layers are not rebuilt.
 * <p>
 * If the composite is missing the small preview Photoshop embeds is used instead (marked as such); if neither exists the
 * file is reported as not supported.
 */
public final class PsdReader {

	private PsdReader() {
	}

	/** What was found in a PSD file */
	public static final class Psd {
		public BufferedImage image;
		/** True when {@link #image} is the small embedded preview, not the full picture */
		public boolean fromPreview;
		public int width;
		public int height;
		public int channels;
		public int depth;
		public int colorMode;
		public int layers = -1;
		public boolean big;
		public String xmp = "";
		public boolean iccProfile;
		public double dpiX;
		public double dpiY;
		/** What the user should know about the way the picture was read */
		public String note = "";

		public String modeName() {
			switch (colorMode) {
			case 0:
				return "Bitmap";
			case 1:
				return "Grayscale";
			case 2:
				return "Indexed";
			case 3:
				return "RGB";
			case 4:
				return "CMYK";
			case 7:
				return "Multichannel";
			case 8:
				return "Duotone";
			case 9:
				return "Lab";
			default:
				return "Mode " + colorMode;
			}
		}

		/** Readable summary, for the metadata window */
		public String describe() {
			StringBuilder t = new StringBuilder();
			t.append("Photoshop document").append(big ? " (large format, PSB)" : "").append('\n');
			t.append("Size: ").append(width).append(" x ").append(height).append(" px\n");
			t.append("Color mode: ").append(modeName()).append(", ").append(depth).append(" bit, ").append(channels).append(" channels\n");
			if (dpiX > 0) {
				t.append(String.format(Locale.ROOT, "Resolution: %.0f x %.0f dpi%n", dpiX, dpiY));
			}
			if (layers >= 0) {
				t.append("Layers: ").append(layers).append(" (the picture shown is the flattened composite)\n");
			}
			t.append("Color profile: ").append(iccProfile ? "embedded" : "none").append('\n');
			t.append("XMP: ").append(xmp.isEmpty() ? "none" : (xmp.length() / 1024 + 1) + " KB").append('\n');
			if (hasCameraRaw()) {
				t.append("Camera Raw settings: found in the XMP (see the button in Details)\n");
			}
			if (!note.isEmpty()) {
				t.append('\n').append(note).append('\n');
			}
			return t.toString();
		}

		public boolean hasCameraRaw() {
			return xmp.contains("crs:");
		}
	}

	public static boolean isPsd(File f) {
		if (f == null) {
			return false;
		}
		String n = f.getName().toLowerCase(Locale.ROOT);
		return n.endsWith(".psd") || n.endsWith(".psb");
	}

	/** Reads the whole file: information and picture */
	public static Psd read(File file) throws IOException {
		return read(file, true);
	}

	/** Information only (no pixels decoded): fast, also for huge files */
	public static Psd readInfo(File file) throws IOException {
		return read(file, false);
	}

	private static Psd read(File file, boolean wantImage) throws IOException {
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 1 << 16))) {
			byte[] sig = new byte[4];
			in.readFully(sig);
			if (sig[0] != '8' || sig[1] != 'B' || sig[2] != 'P' || sig[3] != 'S') {
				throw new IOException("Not a Photoshop file");
			}
			Psd psd = new Psd();
			int version = in.readUnsignedShort();
			if (version != 1 && version != 2) {
				throw new IOException("Unknown Photoshop file version " + version);
			}
			psd.big = version == 2;
			in.skipBytes(6);
			psd.channels = in.readUnsignedShort();
			psd.height = in.readInt();
			psd.width = in.readInt();
			psd.depth = in.readUnsignedShort();
			psd.colorMode = in.readUnsignedShort();
			if (psd.width <= 0 || psd.height <= 0 || psd.channels <= 0) {
				throw new IOException("Damaged Photoshop header");
			}

			// color mode data (the palette of indexed files)
			int modeLength = in.readInt();
			byte[] modeData = new byte[Math.max(0, modeLength)];
			in.readFully(modeData);

			// image resources: resolution, profile, XMP, preview
			int resLength = in.readInt();
			byte[] resources = new byte[Math.max(0, resLength)];
			in.readFully(resources);
			byte[] previewJpeg = parseResources(resources, psd);

			// layer and mask information: only the number of layers is read, the rest is skipped
			long layerSection = psd.big ? in.readLong() : (in.readInt() & 0xffffffffL);
			if (layerSection > 0) {
				long left = layerSection;
				long infoLength = psd.big ? in.readLong() : (in.readInt() & 0xffffffffL);
				left -= psd.big ? 8 : 4;
				if (infoLength > 1) {
					psd.layers = Math.abs((short) in.readUnsignedShort());
					left -= 2;
				}
				skipFully(in, left);
			}

			if (!wantImage) {
				return psd;
			}
			String refusal = refusalReason(psd);
			if (refusal == null) {
				try {
					psd.image = readComposite(in, psd, modeData);
				} catch (EOFException e) {
					psd.image = null;
				}
			} else {
				psd.note = refusal;
			}
			if (psd.image == null && previewJpeg != null) {
				BufferedImage preview = ImageIO.read(new ByteArrayInputStream(previewJpeg));
				if (preview != null) {
					psd.image = preview;
					psd.fromPreview = true;
					psd.note = (refusal != null ? refusal + "\n" : "") + "The merged picture is not stored in this file: the small preview saved by Photoshop ("
							+ preview.getWidth() + " x " + preview.getHeight() + " px) is shown instead.";
				}
			}
			if (psd.image == null) {
				throw new IOException(refusal != null ? refusal : "This PSD has no merged picture (saved without Maximize Compatibility): it cannot be shown without Photoshop");
			}
			if (psd.image != null && !psd.fromPreview && psd.note.isEmpty() && psd.colorMode == 4) {
				psd.note = "CMYK colors are converted to RGB without the printer profile: they can look different from Photoshop.";
			}
			return psd;
		}
	}

	private static String refusalReason(Psd psd) {
		if (psd.depth == 32) {
			return "32 bit (HDR) Photoshop files are not supported.";
		}
		if (psd.depth != 1 && psd.depth != 8 && psd.depth != 16) {
			return "Photoshop files with " + psd.depth + " bit per channel are not supported.";
		}
		if (psd.colorMode != 0 && psd.colorMode != 1 && psd.colorMode != 2 && psd.colorMode != 3 && psd.colorMode != 4 && psd.colorMode != 7 && psd.colorMode != 8 && psd.colorMode != 9) {
			return "Photoshop color mode " + psd.colorMode + " is not supported.";
		}
		long pixels = (long) psd.width * psd.height;
		if (pixels * 4L * 12 / 10 > Runtime.getRuntime().maxMemory() * 7 / 10) {
			return "The picture (" + psd.width + " x " + psd.height + " px) is too big for the memory given to ScreepT.";
		}
		return null;
	}

	/** Reads resolution, profile and XMP; returns the embedded preview JPEG if there is one */
	private static byte[] parseResources(byte[] r, Psd psd) {
		byte[] preview = null;
		int p = 0;
		while (p + 12 <= r.length) {
			if (r[p] != '8' || r[p + 1] != 'B' || r[p + 2] != 'I' || r[p + 3] != 'M') {
				break;
			}
			int id = ((r[p + 4] & 0xff) << 8) | (r[p + 5] & 0xff);
			int nameLength = r[p + 6] & 0xff;
			int nameBytes = 1 + nameLength;
			if ((nameBytes & 1) != 0) {
				nameBytes++;
			}
			int sizePos = p + 6 + nameBytes;
			if (sizePos + 4 > r.length) {
				break;
			}
			int size = ((r[sizePos] & 0xff) << 24) | ((r[sizePos + 1] & 0xff) << 16) | ((r[sizePos + 2] & 0xff) << 8) | (r[sizePos + 3] & 0xff);
			int data = sizePos + 4;
			if (size < 0 || data + size > r.length) {
				break;
			}
			if (id == 1005 && size >= 16) {
				psd.dpiX = fixed(r, data);
				psd.dpiY = fixed(r, data + 8);
			} else if (id == 1039 && size > 0) {
				psd.iccProfile = true;
			} else if (id == 1060 && size > 0) {
				psd.xmp = new String(r, data, size, StandardCharsets.UTF_8);
			} else if ((id == 1036 || id == 1033) && size > 28 && preview == null) {
				// thumbnail resource: 28 bytes of header, then a JPEG (format 1)
				int format = ((r[data] & 0xff) << 24) | ((r[data + 1] & 0xff) << 16) | ((r[data + 2] & 0xff) << 8) | (r[data + 3] & 0xff);
				if (format == 1) {
					preview = new byte[size - 28];
					System.arraycopy(r, data + 28, preview, 0, preview.length);
				}
			}
			p = data + size + (size & 1);
		}
		return preview;
	}

	private static double fixed(byte[] r, int pos) {
		int v = ((r[pos] & 0xff) << 24) | ((r[pos + 1] & 0xff) << 16) | ((r[pos + 2] & 0xff) << 8) | (r[pos + 3] & 0xff);
		return v / 65536.0;
	}

	private static void skipFully(InputStream in, long count) throws IOException {
		long left = count;
		while (left > 0) {
			long skipped = in.skip(left);
			if (skipped <= 0) {
				if (in.read() < 0) {
					throw new EOFException();
				}
				skipped = 1;
			}
			left -= skipped;
		}
	}

	// ------------------------------------------------------------------ composite

	private static BufferedImage readComposite(DataInputStream in, Psd psd, byte[] modeData) throws IOException {
		int compression = in.readUnsignedShort();
		int w = psd.width;
		int h = psd.height;
		int channels = psd.channels;
		int depth = psd.depth;
		int rowBytes = depth == 1 ? (w + 7) / 8 : w * (depth / 8);

		int[] rowLengths = null;
		InputStream data = in;
		if (compression == 1) {
			rowLengths = new int[h * channels];
			for (int i = 0; i < rowLengths.length; i++) {
				rowLengths[i] = psd.big ? in.readInt() : in.readUnsignedShort();
			}
		} else if (compression == 2 || compression == 3) {
			data = new InflaterInputStream(in);
		} else if (compression != 0) {
			throw new IOException("Unknown compression " + compression + " in the Photoshop file");
		}
		DataInputStream source = data == in ? in : new DataInputStream(data);

		// color channels used and alpha
		int colorChannels;
		switch (psd.colorMode) {
		case 3:
		case 9:
			colorChannels = 3;
			break;
		case 4:
			colorChannels = 4;
			break;
		default:
			colorChannels = 1;
			break;
		}
		if (channels < colorChannels) {
			throw new IOException("The Photoshop file has fewer channels than its color mode needs");
		}
		boolean alpha = channels > colorChannels && psd.colorMode != 2 && psd.colorMode != 0;
		int used = Math.min(channels, colorChannels + (alpha ? 1 : 0));

		// planes: 8 bit value per pixel and per used channel
		byte[][] planes = new byte[used][];
		byte[] row = new byte[rowBytes];
		byte[] packed = new byte[Math.max(rowBytes * 2 + 16, 1024)];
		for (int c = 0; c < channels; c++) {
			byte[] plane = c < used ? new byte[w * h] : null;
			for (int y = 0; y < h; y++) {
				if (compression == 1) {
					int n = rowLengths[c * h + y];
					if (n > packed.length) {
						packed = new byte[n];
					}
					source.readFully(packed, 0, n);
					unpackBits(packed, n, row, rowBytes);
				} else {
					source.readFully(row);
					if (compression == 3) {
						predict(row, rowBytes, depth);
					}
				}
				if (plane != null) {
					toEightBit(row, plane, y * w, w, depth);
				}
			}
			if (c < used) {
				planes[c] = plane;
			}
		}
		return compose(psd, planes, alpha, modeData);
	}

	private static void unpackBits(byte[] src, int length, byte[] dst, int expected) throws IOException {
		int s = 0;
		int d = 0;
		while (s < length && d < expected) {
			int n = src[s++];
			if (n >= 0) {
				int count = n + 1;
				for (int k = 0; k < count && s < length && d < expected; k++) {
					dst[d++] = src[s++];
				}
			} else if (n != -128) {
				int count = 1 - n;
				byte value = s < length ? src[s++] : 0;
				for (int k = 0; k < count && d < expected; k++) {
					dst[d++] = value;
				}
			}
		}
		while (d < expected) {
			dst[d++] = 0;
		}
	}

	/** ZIP with prediction: every value is stored as the difference from the one on its left */
	private static void predict(byte[] row, int rowBytes, int depth) {
		if (depth == 8) {
			for (int i = 1; i < rowBytes; i++) {
				row[i] = (byte) (row[i] + row[i - 1]);
			}
		} else if (depth == 16) {
			for (int i = 2; i + 1 < rowBytes; i += 2) {
				int prev = ((row[i - 2] & 0xff) << 8) | (row[i - 1] & 0xff);
				int cur = ((row[i] & 0xff) << 8) | (row[i + 1] & 0xff);
				int sum = (prev + cur) & 0xffff;
				row[i] = (byte) (sum >> 8);
				row[i + 1] = (byte) sum;
			}
		}
	}

	private static void toEightBit(byte[] row, byte[] plane, int offset, int w, int depth) {
		if (depth == 8) {
			System.arraycopy(row, 0, plane, offset, w);
		} else if (depth == 16) {
			for (int x = 0; x < w; x++) {
				plane[offset + x] = row[x * 2];
			}
		} else {
			// 1 bit: set bit = black in Photoshop bitmaps
			for (int x = 0; x < w; x++) {
				boolean bit = (row[x >> 3] & (0x80 >> (x & 7))) != 0;
				plane[offset + x] = (byte) (bit ? 0 : 255);
			}
		}
	}

	private static BufferedImage compose(Psd psd, byte[][] planes, boolean alpha, byte[] modeData) {
		int w = psd.width;
		int h = psd.height;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] px = new int[w * h];
		byte[] a = alpha ? planes[planes.length - 1] : null;
		for (int i = 0; i < px.length; i++) {
			int r;
			int g;
			int b;
			switch (psd.colorMode) {
			case 3: {
				r = planes[0][i] & 0xff;
				g = planes[1][i] & 0xff;
				b = planes[2][i] & 0xff;
				break;
			}
			case 4: {
				// Photoshop stores CMYK inverted (255 = no ink)
				int c = planes[0][i] & 0xff;
				int m = planes[1][i] & 0xff;
				int y = planes[2][i] & 0xff;
				int k = planes[3][i] & 0xff;
				r = c * k / 255;
				g = m * k / 255;
				b = y * k / 255;
				break;
			}
			case 9: {
				int[] rgb = labToRgb((planes[0][i] & 0xff) * 100.0 / 255.0, (planes[1][i] & 0xff) - 128, (planes[2][i] & 0xff) - 128);
				r = rgb[0];
				g = rgb[1];
				b = rgb[2];
				break;
			}
			case 2: {
				int index = planes[0][i] & 0xff;
				if (modeData.length >= 768) {
					r = modeData[index] & 0xff;
					g = modeData[256 + index] & 0xff;
					b = modeData[512 + index] & 0xff;
				} else {
					r = index;
					g = index;
					b = index;
				}
				break;
			}
			default: {
				int v = planes[0][i] & 0xff;
				r = v;
				g = v;
				b = v;
				break;
			}
			}
			int alphaValue = a == null ? 255 : (a[i] & 0xff);
			px[i] = (alphaValue << 24) | (r << 16) | (g << 8) | b;
		}
		image.setRGB(0, 0, w, h, px, 0, w);
		return image;
	}

	/** CIE Lab (D50) to sRGB, the way Photoshop reads the 8 bit Lab composite */
	private static int[] labToRgb(double l, double a, double b) {
		double fy = (l + 16) / 116.0;
		double fx = fy + a / 500.0;
		double fz = fy - b / 200.0;
		double x = 0.9642 * finv(fx);
		double y = 1.0 * finv(fy);
		double z = 0.8249 * finv(fz);
		// XYZ D50 to linear sRGB (Bradford adapted)
		double rl = 3.1338561 * x - 1.6168667 * y - 0.4906146 * z;
		double gl = -0.9787684 * x + 1.9161415 * y + 0.0334540 * z;
		double bl = 0.0719453 * x - 0.2289914 * y + 1.4052427 * z;
		return new int[] {gamma(rl), gamma(gl), gamma(bl)};
	}

	private static double finv(double t) {
		double t3 = t * t * t;
		return t3 > 0.008856 ? t3 : (t - 16.0 / 116.0) / 7.787;
	}

	private static int gamma(double v) {
		double c = v <= 0.0031308 ? 12.92 * v : 1.055 * Math.pow(Math.max(v, 0), 1 / 2.4) - 0.055;
		return (int) Math.max(0, Math.min(255, Math.round(c * 255)));
	}
}
