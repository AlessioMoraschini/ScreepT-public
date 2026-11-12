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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

/**
 * Removes the metadata (EXIF, GPS location, XMP, IPTC, comments, thumbnails, trailing hidden data) from image files
 * WITHOUT touching the pixels: the files are rewritten segment by segment, never decoded and re-encoded, so there is no
 * quality loss. JPEG, PNG, WebP and GIF are supported; other formats can optionally be re-saved. Pure Java, no library.
 * <p>
 * The color profile (ICC) and the orientation of the photo can be kept, since removing them changes how the image looks.
 */
public final class MetadataStripper {

	private MetadataStripper() {
	}

	public enum Mode {
		ALL("Everything"), LOCATION("Location only (GPS)");

		private final String label;

		Mode(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	public static class Options {
		public Mode mode = Mode.ALL;
		/** Keeps the color profile: without it the colors can look different */
		public boolean keepIcc = true;
		/** Keeps only the orientation tag of the EXIF, so photos taken sideways do not turn */
		public boolean keepOrientation = true;
		/** Drops what follows the end of the image (video of motion photos, extra images, appended data) */
		public boolean removeTrailing = true;
		/** Formats that cannot be cleaned directly (TIFF...) are re-saved without metadata */
		public boolean reencodeOthers = false;
	}

	public static final class Result {
		/** What was (or would be) removed, one line each */
		public final List<String> removed = new ArrayList<String>();
		public boolean gps;
		public boolean supported = true;
		public String format = "";
		public String message = "";
		/** The cleaned file; null when there is nothing to change */
		public byte[] output;

		public boolean changed() {
			return output != null;
		}

		/** Short names of what is there: "EXIF, GPS, XMP" */
		public String summary() {
			if (!supported) {
				return "not supported";
			}
			List<String> names = new ArrayList<String>();
			for (String item : removed) {
				int cut = item.indexOf(" (");
				String name = cut > 0 ? item.substring(0, cut) : item;
				if (!names.contains(name)) {
					names.add(name);
				}
			}
			if (gps && !names.contains("GPS")) {
				names.add(1 < names.size() ? 1 : names.size(), "GPS");
			}
			if (names.isEmpty()) {
				return "none";
			}
			StringBuilder text = new StringBuilder();
			for (String name : names) {
				text.append(text.length() > 0 ? ", " : "").append(name);
			}
			return text.toString();
		}
	}

	// ------------------------------------------------------------------ entry points

	/** What the default cleaning would remove from the file (nothing is written) */
	public static Result analyze(File file) throws IOException {
		Options options = new Options();
		return process(Files.readAllBytes(file.toPath()), options);
	}

	/** Cleans the content of a file; with {@link Options#reencodeOthers} unsupported formats are re-saved */
	public static Result processFile(File file, Options options) throws IOException {
		Result result = process(Files.readAllBytes(file.toPath()), options);
		if (!result.supported && options.reencodeOthers) {
			String name = file.getName().toLowerCase(Locale.ROOT);
			String ext = name.substring(name.lastIndexOf('.') + 1);
			ext = ext.equals("jpeg") ? "jpg" : ext.equals("tif") ? "tiff" : ext;
			BufferedImage image = ImageIO.read(file);
			if (image != null && ImageEdit.writableFormats().contains(ext)) {
				ImageSettings settings = new ImageSettings();
				settings.format = ext;
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageEdit.write(image, out, settings);
				Result redone = new Result();
				redone.format = ext.toUpperCase(Locale.ROOT);
				redone.removed.add("Everything (file re-saved)");
				redone.output = out.toByteArray();
				return redone;
			}
		}
		return result;
	}

	/** Writes the bytes next to the target and moves them in place, so a failure never leaves a half written file */
	public static void writeSafely(byte[] data, File target) throws IOException {
		File dir = target.getAbsoluteFile().getParentFile();
		if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
			throw new IOException("Cannot create the folder " + dir);
		}
		File temp = File.createTempFile("clean_", ".tmp", dir);
		try {
			Files.write(temp.toPath(), data);
			Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			if (temp.exists()) {
				temp.delete();
			}
		}
	}

	/** Recognizes the format from the first bytes, not from the extension */
	public static Result process(byte[] d, Options o) {
		if (d.length > 4 && (d[0] & 0xff) == 0xFF && (d[1] & 0xff) == 0xD8) {
			return jpeg(d, o);
		}
		if (d.length > 8 && (d[0] & 0xff) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
			return png(d, o);
		}
		if (d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F' && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
			return webp(d, o);
		}
		if (d.length > 13 && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8') {
			return gif(d, o);
		}
		Result r = new Result();
		if (d.length > 2 && d[0] == 'B' && d[1] == 'M') {
			r.format = "BMP";
			r.message = "BMP carries no metadata";
			return r;
		}
		r.supported = false;
		r.message = "Format not supported for direct cleaning";
		return r;
	}

	// ------------------------------------------------------------------ JPEG

	private static Result jpeg(byte[] d, Options o) {
		Result r = new Result();
		r.format = "JPEG";
		boolean all = o.mode == Mode.ALL;
		ByteArrayOutputStream out = new ByteArrayOutputStream(d.length);
		out.write(0xFF);
		out.write(0xD8);
		int i = 2;
		boolean changed = false;
		while (i + 4 <= d.length) {
			if ((d[i] & 0xff) != 0xFF) {
				break;
			}
			int m = d[i + 1] & 0xff;
			if (m == 0xFF) {
				i++;
				continue;
			}
			if (m == 0xD8 || m == 0x01 || (m >= 0xD0 && m <= 0xD7)) {
				out.write(d, i, 2);
				i += 2;
				continue;
			}
			if (m == 0xD9 || m == 0xDA) {
				break;
			}
			int len = ((d[i + 2] & 0xff) << 8) | (d[i + 3] & 0xff);
			int end = i + 2 + len;
			if (len < 2 || end > d.length) {
				break;
			}
			int payload = i + 4;
			boolean keep = true;
			byte[] replacement = null;
			String what = null;
			if (m == 0xE1 && startsWith(d, payload, end, "Exif\0\0")) {
				int tiff = payload + 6;
				boolean hasGps = exifHasGps(d, tiff, end);
				r.gps |= hasGps;
				if (all) {
					what = "EXIF (" + size(len) + (hasGps ? ", GPS location" : "") + ")";
					int orientation = exifOrientation(d, tiff, end);
					keep = false;
					if (o.keepOrientation && orientation > 1) {
						replacement = jpegExifSegment(orientation);
						what += " - orientation kept";
					}
				} else if (hasGps) {
					byte[] copy = Arrays.copyOfRange(d, i, end);
					if (zeroGps(copy, 10, copy.length)) {
						keep = false;
						replacement = copy;
						what = "GPS location (EXIF)";
					}
				}
			} else if (m == 0xE1 && (startsWith(d, payload, end, "http://ns.adobe.com/xap/1.0/\0") || startsWith(d, payload, end, "http://ns.adobe.com/xmp/extension/\0"))) {
				boolean withGps = indexOf(d, payload, end, "GPSLatitude") >= 0 || indexOf(d, payload, end, "GPSLongitude") >= 0;
				if (all || withGps) {
					keep = false;
					what = "XMP (" + size(len) + (withGps ? ", GPS location" : "") + ")";
					r.gps |= withGps;
				}
			} else if (all && m == 0xE1) {
				keep = false;
				what = "APP1 data (" + size(len) + ")";
			} else if (m == 0xE2 && startsWith(d, payload, end, "ICC_PROFILE\0")) {
				if (all && !o.keepIcc) {
					keep = false;
					what = "ICC color profile (" + size(len) + ")";
				}
			} else if (all && m == 0xE2) {
				keep = false;
				what = "Multi-picture / FlashPix data (" + size(len) + ")";
			} else if (all && m == 0xED) {
				keep = false;
				what = "Photoshop / IPTC (" + size(len) + ")";
			} else if (all && m == 0xFE) {
				keep = false;
				what = "Comment (" + size(len) + ")";
			} else if (all && m == 0xE0 && startsWith(d, payload, end, "JFXX")) {
				keep = false;
				what = "Embedded thumbnail (" + size(len) + ")";
			} else if (all && m >= 0xE3 && m <= 0xEF && m != 0xEE) {
				keep = false;
				what = "Application data APP" + (m - 0xE0) + " (" + size(len) + ")";
			}
			if (keep) {
				out.write(d, i, end - i);
			} else {
				changed |= what != null;
				if (what != null) {
					r.removed.add(what);
				}
				if (replacement != null) {
					out.write(replacement, 0, replacement.length);
				}
			}
			i = end;
		}
		// the rest: scan data up to the end of the image, then whatever follows it
		if (i + 2 <= d.length && (d[i] & 0xff) == 0xFF && (d[i + 1] & 0xff) == 0xDA) {
			int eoi = findJpegEnd(d, i);
			if (eoi < 0) {
				out.write(d, i, d.length - i);
			} else {
				out.write(d, i, eoi - i);
				changed |= handleTrailing(d, eoi, out, o, r);
			}
		} else if (i + 2 <= d.length && (d[i] & 0xff) == 0xFF && (d[i + 1] & 0xff) == 0xD9) {
			out.write(d, i, 2);
			changed |= handleTrailing(d, i + 2, out, o, r);
		} else if (i < d.length) {
			out.write(d, i, d.length - i);
		}
		if (changed) {
			r.output = out.toByteArray();
		}
		return r;
	}

	private static boolean handleTrailing(byte[] d, int from, ByteArrayOutputStream out, Options o, Result r) {
		int rest = d.length - from;
		if (rest <= 0) {
			return false;
		}
		if (o.mode == Mode.ALL && o.removeTrailing) {
			r.removed.add("Data after the image end (" + size(rest) + ")");
			return true;
		}
		out.write(d, from, rest);
		return false;
	}

	/** Index just after the EOI marker that closes the image, following the scans and the markers between them */
	private static int findJpegEnd(byte[] d, int sos) {
		int p = sos;
		while (p + 4 <= d.length) {
			// at a marker segment with length (SOS header, or DHT/DQT/DRI... between scans)
			int len = ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
			p += 2 + len;
			// entropy coded data until the next real marker
			while (p + 1 < d.length) {
				if ((d[p] & 0xff) == 0xFF) {
					int b = d[p + 1] & 0xff;
					if (b == 0x00 || (b >= 0xD0 && b <= 0xD7) || b == 0xFF) {
						p += (b == 0xFF) ? 1 : 2;
						continue;
					}
					if (b == 0xD9) {
						return p + 2;
					}
					break;
				}
				p++;
			}
			if (p + 1 >= d.length) {
				return -1;
			}
		}
		return -1;
	}

	private static byte[] jpegExifSegment(int orientation) {
		byte[] tiff = minimalTiff(orientation);
		byte[] seg = new byte[4 + 6 + tiff.length];
		seg[0] = (byte) 0xFF;
		seg[1] = (byte) 0xE1;
		int len = 2 + 6 + tiff.length;
		seg[2] = (byte) (len >> 8);
		seg[3] = (byte) len;
		byte[] id = {'E', 'x', 'i', 'f', 0, 0};
		System.arraycopy(id, 0, seg, 4, 6);
		System.arraycopy(tiff, 0, seg, 10, tiff.length);
		return seg;
	}

	// ------------------------------------------------------------------ PNG

	private static final String[] PNG_KEEP = {"tRNS", "gAMA", "cHRM", "sRGB", "sBIT", "bKGD", "pHYs", "hIST", "sPLT", "acTL", "fcTL", "fdAT", "cICP", "mDCV", "cLLI"};

	private static Result png(byte[] d, Options o) {
		Result r = new Result();
		r.format = "PNG";
		boolean all = o.mode == Mode.ALL;
		ByteArrayOutputStream out = new ByteArrayOutputStream(d.length);
		out.write(d, 0, 8);
		int pos = 8;
		boolean changed = false;
		boolean finished = false;
		while (pos + 12 <= d.length) {
			long lenL = u32(d, pos, false);
			int len = (int) lenL;
			int end = pos + 12 + len;
			if (lenL < 0 || lenL > Integer.MAX_VALUE - 12 || end > d.length) {
				break;
			}
			String type = new String(d, pos + 4, 4, StandardCharsets.ISO_8859_1);
			boolean critical = (type.charAt(0) & 0x20) == 0;
			boolean keep = true;
			byte[] replacement = null;
			String what = null;
			if (!critical) {
				if (type.equals("eXIf")) {
					boolean hasGps = exifHasGps(d, pos + 8, pos + 8 + len);
					r.gps |= hasGps;
					if (all) {
						keep = false;
						what = "EXIF (" + size(len) + (hasGps ? ", GPS location" : "") + ")";
						int orientation = exifOrientation(d, pos + 8, pos + 8 + len);
						if (o.keepOrientation && orientation > 1) {
							replacement = pngChunk("eXIf", minimalTiff(orientation));
							what += " - orientation kept";
						}
					} else if (hasGps) {
						byte[] copy = Arrays.copyOfRange(d, pos, end);
						if (zeroGps(copy, 8, 8 + len)) {
							CRC32 crc = new CRC32();
							crc.update(copy, 4, 4 + len);
							putU32(copy, 8 + len, crc.getValue());
							keep = false;
							replacement = copy;
							what = "GPS location (EXIF)";
						}
					}
				} else if (type.equals("tEXt") || type.equals("zTXt") || type.equals("tIME")) {
					if (all) {
						keep = false;
						what = (type.equals("tIME") ? "Modification time" : "Text") + " (" + size(len) + ")";
					}
				} else if (type.equals("iTXt")) {
					boolean xmp = indexOf(d, pos + 8, pos + 8 + len, "XML:com.adobe.xmp") >= 0;
					boolean withGps = xmp && (indexOf(d, pos + 8, pos + 8 + len, "GPSLatitude") >= 0 || indexOf(d, pos + 8, pos + 8 + len, "GPSLongitude") >= 0);
					if (all || withGps) {
						keep = false;
						what = (xmp ? "XMP" : "Text") + " (" + size(len) + (withGps ? ", GPS location" : "") + ")";
						r.gps |= withGps;
					}
				} else if (type.equals("iCCP")) {
					if (all && !o.keepIcc) {
						keep = false;
						what = "ICC color profile (" + size(len) + ")";
					}
				} else if (all && !Arrays.asList(PNG_KEEP).contains(type)) {
					keep = false;
					what = "Extra chunk " + type + " (" + size(len) + ")";
				}
			}
			if (keep) {
				out.write(d, pos, end - pos);
			} else {
				changed |= what != null;
				if (what != null) {
					r.removed.add(what);
				}
				if (replacement != null) {
					out.write(replacement, 0, replacement.length);
				}
			}
			pos = end;
			if (type.equals("IEND")) {
				finished = true;
				break;
			}
		}
		if (finished) {
			changed |= handleTrailing(d, pos, out, o, r);
		} else if (pos < d.length) {
			out.write(d, pos, d.length - pos);
		}
		if (changed) {
			r.output = out.toByteArray();
		}
		return r;
	}

	private static byte[] pngChunk(String type, byte[] data) {
		byte[] chunk = new byte[12 + data.length];
		putU32(chunk, 0, data.length);
		System.arraycopy(type.getBytes(StandardCharsets.ISO_8859_1), 0, chunk, 4, 4);
		System.arraycopy(data, 0, chunk, 8, data.length);
		CRC32 crc = new CRC32();
		crc.update(chunk, 4, 4 + data.length);
		putU32(chunk, 8 + data.length, crc.getValue());
		return chunk;
	}

	// ------------------------------------------------------------------ WebP

	private static Result webp(byte[] d, Options o) {
		Result r = new Result();
		r.format = "WebP";
		boolean all = o.mode == Mode.ALL;
		ByteArrayOutputStream body = new ByteArrayOutputStream(d.length);
		int pos = 12;
		boolean changed = false;
		int flagsPos = -1;
		boolean lostExif = false;
		boolean lostXmp = false;
		boolean lostIcc = false;
		while (pos + 8 <= d.length) {
			long sizeL = u32(d, pos + 4, true);
			int size = (int) sizeL;
			int padded = size + (size & 1);
			int end = pos + 8 + padded;
			if (sizeL < 0 || sizeL > Integer.MAX_VALUE - 16 || end > d.length + 1 || pos + 8 + size > d.length) {
				break;
			}
			end = Math.min(end, d.length);
			String id = new String(d, pos, 4, StandardCharsets.ISO_8859_1);
			boolean keep = true;
			byte[] replacement = null;
			String what = null;
			int payload = pos + 8;
			if (id.equals("EXIF")) {
				int tiff = startsWith(d, payload, payload + size, "Exif\0\0") ? payload + 6 : payload;
				boolean hasGps = exifHasGps(d, tiff, payload + size);
				r.gps |= hasGps;
				if (all) {
					keep = false;
					what = "EXIF (" + size(size) + (hasGps ? ", GPS location" : "") + ")";
					int orientation = exifOrientation(d, tiff, payload + size);
					if (o.keepOrientation && orientation > 1) {
						byte[] tiffBytes = minimalTiff(orientation);
						replacement = riffChunk("EXIF", tiffBytes);
						what += " - orientation kept";
					} else {
						lostExif = true;
					}
				} else if (hasGps) {
					byte[] copy = Arrays.copyOfRange(d, pos, end);
					if (zeroGps(copy, tiff - pos, 8 + size)) {
						keep = false;
						replacement = copy;
						what = "GPS location (EXIF)";
					}
				}
			} else if (id.equals("XMP ")) {
				boolean withGps = indexOf(d, payload, payload + size, "GPSLatitude") >= 0 || indexOf(d, payload, payload + size, "GPSLongitude") >= 0;
				if (all || withGps) {
					keep = false;
					lostXmp = true;
					what = "XMP (" + size(size) + (withGps ? ", GPS location" : "") + ")";
					r.gps |= withGps;
				}
			} else if (id.equals("ICCP") && all && !o.keepIcc) {
				keep = false;
				lostIcc = true;
				what = "ICC color profile (" + size(size) + ")";
			}
			if (id.equals("VP8X") && size >= 10) {
				flagsPos = body.size() + 8;
			}
			if (keep) {
				body.write(d, pos, end - pos);
			} else {
				changed |= what != null;
				if (what != null) {
					r.removed.add(what);
				}
				if (replacement != null) {
					body.write(replacement, 0, replacement.length);
				}
			}
			pos = end;
		}
		if (pos < d.length) {
			body.write(d, pos, d.length - pos);
		}
		if (changed) {
			byte[] content = body.toByteArray();
			if (flagsPos >= 0 && flagsPos < content.length) {
				int flags = content[flagsPos] & 0xff;
				if (lostExif) {
					flags &= ~0x08;
				}
				if (lostXmp) {
					flags &= ~0x04;
				}
				if (lostIcc) {
					flags &= ~0x20;
				}
				content[flagsPos] = (byte) flags;
			}
			byte[] outBytes = new byte[12 + content.length];
			System.arraycopy(d, 0, outBytes, 0, 12);
			putU32(outBytes, 4, 4 + content.length, true);
			System.arraycopy(content, 0, outBytes, 12, content.length);
			r.output = outBytes;
		}
		return r;
	}

	private static byte[] riffChunk(String id, byte[] data) {
		int padded = data.length + (data.length & 1);
		byte[] chunk = new byte[8 + padded];
		System.arraycopy(id.getBytes(StandardCharsets.ISO_8859_1), 0, chunk, 0, 4);
		putU32(chunk, 4, data.length, true);
		System.arraycopy(data, 0, chunk, 8, data.length);
		return chunk;
	}

	// ------------------------------------------------------------------ GIF

	private static Result gif(byte[] d, Options o) {
		Result r = new Result();
		r.format = "GIF";
		if (o.mode != Mode.ALL) {
			r.message = "GIF carries no GPS location";
			return r;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(d.length);
		int flags = d[10] & 0xff;
		int pos = 13 + ((flags & 0x80) != 0 ? 3 * (1 << ((flags & 7) + 1)) : 0);
		if (pos > d.length) {
			r.supported = false;
			return r;
		}
		out.write(d, 0, pos);
		boolean changed = false;
		boolean finished = false;
		while (pos < d.length) {
			int b = d[pos] & 0xff;
			if (b == 0x3B) {
				out.write(0x3B);
				pos++;
				finished = true;
				break;
			}
			if (b == 0x21 && pos + 2 <= d.length) {
				int label = d[pos + 1] & 0xff;
				int end = skipBlocks(d, pos + 2);
				boolean keep = label == 0xF9;
				String what = null;
				if (label == 0xFF && pos + 14 <= d.length) {
					keep = startsWith(d, pos + 3, pos + 14, "NETSCAPE2.0") || startsWith(d, pos + 3, pos + 14, "ANIMEXTS1.0");
					what = "Application data (" + size(end - pos) + ")";
				} else if (label == 0xFE) {
					what = "Comment (" + size(end - pos) + ")";
				} else if (!keep) {
					what = "Extension data (" + size(end - pos) + ")";
				}
				if (keep) {
					out.write(d, pos, end - pos);
				} else {
					changed = true;
					r.removed.add(what);
				}
				pos = end;
			} else if (b == 0x2C && pos + 11 <= d.length) {
				int packed = d[pos + 9] & 0xff;
				int header = 10 + ((packed & 0x80) != 0 ? 3 * (1 << ((packed & 7) + 1)) : 0) + 1;
				int end = skipBlocks(d, Math.min(d.length, pos + header));
				out.write(d, pos, end - pos);
				pos = end;
			} else {
				break;
			}
		}
		if (finished) {
			changed |= handleTrailing(d, pos, out, o, r);
		} else if (pos < d.length) {
			out.write(d, pos, d.length - pos);
		}
		if (changed) {
			r.output = out.toByteArray();
		}
		return r;
	}

	private static int skipBlocks(byte[] d, int p) {
		while (p < d.length) {
			int n = d[p] & 0xff;
			p += 1 + n;
			if (n == 0) {
				break;
			}
		}
		return Math.min(p, d.length);
	}

	// ------------------------------------------------------------------ EXIF (TIFF structure)

	private static byte[] minimalTiff(int orientation) {
		return new byte[] {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0, 0, 0, 0, 0};
	}

	private static boolean tiffValid(byte[] d, int base, int end) {
		return base >= 0 && end - base >= 8 && end <= d.length && ((d[base] == 'I' && d[base + 1] == 'I') || (d[base] == 'M' && d[base + 1] == 'M'));
	}

	private static int t16(byte[] d, int base, int pos, int end) {
		if (pos < base || pos + 2 > end) {
			throw new IndexOutOfBoundsException();
		}
		boolean le = d[base] == 'I';
		return le ? ((d[pos] & 0xff) | ((d[pos + 1] & 0xff) << 8)) : (((d[pos] & 0xff) << 8) | (d[pos + 1] & 0xff));
	}

	private static long t32(byte[] d, int base, int pos, int end) {
		if (pos < base || pos + 4 > end) {
			throw new IndexOutOfBoundsException();
		}
		return u32(d, pos, d[base] == 'I');
	}

	/** Orientation 1..8 of the EXIF, or -1 */
	static int exifOrientation(byte[] d, int base, int end) {
		try {
			if (!tiffValid(d, base, end)) {
				return -1;
			}
			int ifd = base + (int) t32(d, base, base + 4, end);
			int count = t16(d, base, ifd, end);
			for (int e = 0; e < count; e++) {
				int entry = ifd + 2 + 12 * e;
				if (t16(d, base, entry, end) == 0x0112) {
					int v = t16(d, base, entry + 8, end);
					return v >= 1 && v <= 8 ? v : -1;
				}
			}
		} catch (RuntimeException ex) {
			// damaged EXIF: no orientation
		}
		return -1;
	}

	static boolean exifHasGps(byte[] d, int base, int end) {
		try {
			if (!tiffValid(d, base, end)) {
				return false;
			}
			int ifd = base + (int) t32(d, base, base + 4, end);
			int count = t16(d, base, ifd, end);
			for (int e = 0; e < count; e++) {
				int entry = ifd + 2 + 12 * e;
				if (t16(d, base, entry, end) == 0x8825) {
					int gps = base + (int) t32(d, base, entry + 8, end);
					return t16(d, base, gps, end) > 0;
				}
			}
		} catch (RuntimeException ex) {
			// damaged EXIF: nothing found
		}
		return false;
	}

	private static int typeSize(int type) {
		switch (type) {
		case 3: case 8:
			return 2;
		case 4: case 9: case 11:
			return 4;
		case 5: case 10: case 12:
			return 8;
		default:
			return 1;
		}
	}

	/**
	 * Empties the GPS directory of the EXIF in place: the values stored elsewhere in the block are zeroed too, so the
	 * coordinates cannot be read from the leftover bytes. The file keeps its size and stays a valid EXIF.
	 */
	static boolean zeroGps(byte[] d, int base, int end) {
		try {
			if (!tiffValid(d, base, end)) {
				return false;
			}
			int ifd = base + (int) t32(d, base, base + 4, end);
			int count = t16(d, base, ifd, end);
			for (int e = 0; e < count; e++) {
				int entry = ifd + 2 + 12 * e;
				if (t16(d, base, entry, end) != 0x8825) {
					continue;
				}
				int gps = base + (int) t32(d, base, entry + 8, end);
				int n = t16(d, base, gps, end);
				for (int k = 0; k < n; k++) {
					int ent = gps + 2 + 12 * k;
					long bytes = (long) typeSize(t16(d, base, ent + 2, end)) * t32(d, base, ent + 4, end);
					if (bytes > 4) {
						int at = base + (int) t32(d, base, ent + 8, end);
						if (at >= base && at + bytes <= end) {
							Arrays.fill(d, at, (int) (at + bytes), (byte) 0);
						}
					}
					Arrays.fill(d, ent, ent + 12, (byte) 0);
				}
				d[gps] = 0;
				d[gps + 1] = 0;
				return true;
			}
		} catch (RuntimeException ex) {
			// damaged EXIF: left as it is
		}
		return false;
	}

	// ------------------------------------------------------------------ small helpers

	private static long u32(byte[] d, int pos, boolean littleEndian) {
		if (littleEndian) {
			return (d[pos] & 0xffL) | ((d[pos + 1] & 0xffL) << 8) | ((d[pos + 2] & 0xffL) << 16) | ((d[pos + 3] & 0xffL) << 24);
		}
		return ((d[pos] & 0xffL) << 24) | ((d[pos + 1] & 0xffL) << 16) | ((d[pos + 2] & 0xffL) << 8) | (d[pos + 3] & 0xffL);
	}

	private static void putU32(byte[] d, int pos, long value) {
		putU32(d, pos, value, false);
	}

	private static void putU32(byte[] d, int pos, long value, boolean littleEndian) {
		for (int k = 0; k < 4; k++) {
			int shift = littleEndian ? 8 * k : 8 * (3 - k);
			d[pos + k] = (byte) (value >> shift);
		}
	}

	private static boolean startsWith(byte[] d, int from, int end, String text) {
		byte[] t = text.getBytes(StandardCharsets.ISO_8859_1);
		if (from < 0 || from + t.length > end || end > d.length) {
			return false;
		}
		for (int k = 0; k < t.length; k++) {
			if (d[from + k] != t[k]) {
				return false;
			}
		}
		return true;
	}

	private static int indexOf(byte[] d, int from, int end, String text) {
		byte[] t = text.getBytes(StandardCharsets.ISO_8859_1);
		for (int p = Math.max(0, from); p + t.length <= Math.min(end, d.length); p++) {
			int k = 0;
			while (k < t.length && d[p + k] == t[k]) {
				k++;
			}
			if (k == t.length) {
				return p;
			}
		}
		return -1;
	}

	private static String size(long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		}
		if (bytes < 1024L * 1024) {
			return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
		}
		return String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0);
	}
}
