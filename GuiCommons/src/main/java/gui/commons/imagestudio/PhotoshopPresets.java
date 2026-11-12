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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Brings the settings of Adobe tools into the Image Studio: Camera Raw presets and Lightroom develop settings (.xmp,
 * the "crs:" values), Lightroom Classic presets (.lrtemplate) and the XMP stored inside Photoshop files. The values are
 * translated to the closest Image Studio setting; what has no equivalent (curves, HSL mixer, masks, lens corrections...)
 * is listed in the report instead of being silently dropped. Pure Java (the JDK XML parser), no library.
 */
public final class PhotoshopPresets {

	private PhotoshopPresets() {
	}

	/** Shown as hint wherever a preset can be imported */
	public static final String SUPPORTED_HINT = "<html><b>Presets you can import:</b><br>"
			+ "&bull; Image Studio preset (.properties)<br>"
			+ "&bull; Camera Raw preset or Lightroom develop settings (.xmp)<br>"
			+ "&bull; Lightroom Classic preset (.lrtemplate)<br>"
			+ "&bull; Camera Raw settings stored in a Photoshop file (.psd)<br>"
			+ "Exposure, contrast, highlights, shadows, whites, blacks, saturation, vibrance, temperature, tint, clarity, sharpening,<br>"
			+ "vignette, grain, black &amp; white, crop and straighten are translated; curves, HSL, masks and lens<br>"
			+ "corrections are listed as not applied.</html>";

	/** What an import did */
	public static final class Report {
		public String name = "";
		public final List<String> applied = new ArrayList<String>();
		public final List<String> ignored = new ArrayList<String>();

		public boolean anythingApplied() {
			return !applied.isEmpty();
		}

		public String toHtml() {
			StringBuilder t = new StringBuilder("<html><body style='width:430px'>");
			t.append("<b>").append(name.isEmpty() ? "Imported settings" : escape(name)).append("</b><br><br>");
			if (applied.isEmpty()) {
				t.append("No setting that the Image Studio understands was found.<br>");
			} else {
				t.append("<b>Applied</b><br>");
				for (String line : applied) {
					t.append("&bull; ").append(escape(line)).append("<br>");
				}
			}
			if (!ignored.isEmpty()) {
				t.append("<br><b>Not applied (no equivalent)</b><br>");
				for (String line : ignored) {
					t.append("&bull; ").append(escape(line)).append("<br>");
				}
			}
			return t.append("</body></html>").toString();
		}

		private static String escape(String s) {
			return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
	}

	public static boolean isAdobePreset(File f) {
		if (f == null) {
			return false;
		}
		String n = f.getName().toLowerCase(Locale.ROOT);
		return n.endsWith(".xmp") || n.endsWith(".lrtemplate");
	}

	/** Reads the develop settings of a .xmp or .lrtemplate file (names without prefix, like "Exposure2012") */
	public static Map<String, String> read(File file) throws IOException {
		String name = file.getName().toLowerCase(Locale.ROOT);
		String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		if (name.endsWith(".lrtemplate")) {
			return parseLrTemplate(text);
		}
		return parseXmp(text);
	}

	/** The "crs:" settings of an XMP packet (attributes and simple elements) */
	public static Map<String, String> parseXmp(String xml) throws IOException {
		Map<String, String> out = new LinkedHashMap<String, String>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			int start = xml.indexOf("<?xpacket");
			String cleaned = xml;
			if (start >= 0) {
				// the packet wrappers are processing instructions: harmless, but trailing padding after the end is cut
				int end = xml.lastIndexOf("</x:xmpmeta>");
				if (end > 0) {
					cleaned = xml.substring(xml.indexOf("<x:xmpmeta"), end + "</x:xmpmeta>".length());
				}
			}
			DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(cleaned.getBytes(StandardCharsets.UTF_8)));
			collect(doc.getDocumentElement(), out);
		} catch (Exception e) {
			throw new IOException("Cannot read the XMP: " + e.getMessage(), e);
		}
		return out;
	}

	private static void collect(Element element, Map<String, String> out) {
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node a = attributes.item(i);
			if (a.getNodeName().startsWith("crs:")) {
				out.put(a.getNodeName().substring(4), a.getNodeValue().trim());
			}
		}
		if (element.getNodeName().startsWith("crs:")) {
			NodeList kids = element.getChildNodes();
			boolean simple = true;
			for (int i = 0; i < kids.getLength(); i++) {
				if (kids.item(i).getNodeType() == Node.ELEMENT_NODE) {
					simple = false;
				}
			}
			if (simple && !element.getTextContent().trim().isEmpty()) {
				out.put(element.getNodeName().substring(4), element.getTextContent().trim());
			} else if (!simple) {
				out.put(element.getNodeName().substring(4), "{...}");
			}
		}
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				collect((Element) children.item(i), out);
			}
		}
	}

	private static final Pattern LUA_LINE = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*=\\s*(.+?)\\s*,?\\s*$");

	/** The settings of a Lightroom Classic preset: a Lua table with "Key = value" lines */
	public static Map<String, String> parseLrTemplate(String text) {
		Map<String, String> out = new LinkedHashMap<String, String>();
		for (String line : text.split("\\r?\\n")) {
			Matcher m = LUA_LINE.matcher(line);
			if (!m.matches()) {
				continue;
			}
			String value = m.group(2);
			if (value.startsWith("{")) {
				out.put(m.group(1), "{...}");
				continue;
			}
			if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
				value = value.substring(1, value.length() - 1);
			}
			out.put(m.group(1), value);
		}
		return out;
	}

	// ------------------------------------------------------------------ translation

	private static Double num(Map<String, String> m, String... keys) {
		for (String key : keys) {
			String v = m.get(key);
			if (v != null) {
				try {
					return Double.valueOf(v.replace("+", "").trim());
				} catch (NumberFormatException e) {
					// not a number: try the next name
				}
			}
		}
		return null;
	}

	private static boolean flag(Map<String, String> m, String key) {
		String v = m.get(key);
		return v != null && (v.equalsIgnoreCase("true") || v.equals("1"));
	}

	private static int clamp(double v, int min, int max) {
		return (int) Math.max(min, Math.min(max, Math.round(v)));
	}

	private static String signed(double v) {
		return (v > 0 ? "+" : "") + (Math.abs(v - Math.rint(v)) < 0.005 ? String.valueOf((long) Math.rint(v)) : String.format(Locale.ROOT, "%.2f", v));
	}

	/**
	 * Replaces the editing part of the settings (the output choices stay) with the translation of the Adobe values.
	 * The report says what was applied and what has no equivalent.
	 */
	public static Report apply(Map<String, String> crs, ImageSettings s, String presetName) {
		Report report = new Report();
		report.name = presetName == null ? "" : presetName;
		String title = crs.get("PresetName");
		if (title == null) {
			title = crs.get("title");
		}
		if (title != null && report.name.isEmpty()) {
			report.name = title;
		}
		s.resetEdits();
		Double v;

		if ((v = num(crs, "Exposure2012", "Exposure")) != null && v != 0) {
			s.exposure = clamp(v * 50, -100, 100);
			report.applied.add("Exposure " + signed(v) + " EV  ->  Exposure " + s.exposure);
		}
		if ((v = num(crs, "Contrast2012", "Contrast")) != null && v != 0) {
			s.contrast = clamp(v, -100, 100);
			report.applied.add("Contrast " + signed(v) + "  ->  Contrast " + s.contrast);
		}
		if ((v = num(crs, "Brightness")) != null && Math.abs(v - 50) > 0.5 && crs.get("Exposure2012") == null) {
			s.brightness = clamp((v - 50) * 0.8, -100, 100);
			report.applied.add("Brightness " + signed(v) + " (old process)  ->  Brightness " + s.brightness);
		}
		Double highlights = num(crs, "Highlights2012", "Highlights");
		Double whites = num(crs, "Whites2012");
		if ((highlights != null && highlights != 0) || (whites != null && whites != 0)) {
			double h = (highlights == null ? 0 : highlights) * 0.8 + (whites == null ? 0 : whites) * 0.5;
			s.highlights = clamp(h, -100, 100);
			report.applied.add("Highlights " + (highlights == null ? "0" : signed(highlights)) + " and Whites " + (whites == null ? "0" : signed(whites)) + "  ->  Highlights " + s.highlights);
		}
		Double shadows = num(crs, "Shadows2012", "Shadows");
		Double blacks = num(crs, "Blacks2012", "Blacks");
		if ((shadows != null && shadows != 0) || (blacks != null && blacks != 0)) {
			double sh = (shadows == null ? 0 : shadows) * 0.8 + (blacks == null ? 0 : blacks) * 0.5;
			s.shadows = clamp(sh, -100, 100);
			report.applied.add("Shadows " + (shadows == null ? "0" : signed(shadows)) + " and Blacks " + (blacks == null ? "0" : signed(blacks)) + "  ->  Shadows " + s.shadows);
		}
		if ((v = num(crs, "Saturation")) != null && v != 0) {
			s.saturation = clamp(v, -100, 100);
			report.applied.add("Saturation " + signed(v) + "  ->  Saturation " + s.saturation);
		}
		if ((v = num(crs, "Vibrance")) != null && v != 0) {
			s.vibrance = clamp(v, -100, 100);
			report.applied.add("Vibrance " + signed(v) + "  ->  Vibrance " + s.vibrance);
		}
		if ((v = num(crs, "Temperature")) != null && v != 0 && !(v > 100 && Math.abs(v - 5500) < 1)) {
			// absolute Kelvin (raw files) or a relative -100..100 value (other files)
			s.temperature = Math.abs(v) <= 100 ? clamp(v, -100, 100) : clamp((v - 5500) / 40.0, -100, 100);
			report.applied.add("Temperature " + signed(v) + (Math.abs(v) > 100 ? " K" : "") + "  ->  Temperature " + s.temperature);
		}
		if ((v = num(crs, "Tint")) != null && v != 0) {
			s.tint = clamp(v * 100 / 150.0, -100, 100);
			report.applied.add("Tint " + signed(v) + "  ->  Tint " + s.tint);
		}
		if ((v = num(crs, "Dehaze")) != null && v > 0) {
			s.contrast = clamp(s.contrast + v * 0.3, -100, 100);
			s.saturation = clamp(s.saturation + v * 0.2, -100, 100);
			report.applied.add("Dehaze " + signed(v) + "  ->  a little more Contrast and Saturation (approximation)");
		}
		if ((v = num(crs, "Clarity2012", "Clarity")) != null && Math.abs(v) >= 15) {
			s.filters.add(v > 0 ? ImageEdit.Filter.CLARITY : ImageEdit.Filter.SOFT_FOCUS);
			report.applied.add("Clarity " + signed(v) + "  ->  filter " + (v > 0 ? "Clarity" : "Soft focus") + " (on or off, no amount)");
		}
		if ((v = num(crs, "Sharpness")) != null && v > 0 && !(v == 40 && crs.get("SharpenRadius") == null && crs.get("ProcessVersion") == null)) {
			s.sharpness = clamp(v * 100 / 150.0, 0, 100);
			report.applied.add("Sharpening " + signed(v) + "  ->  Sharpness " + s.sharpness);
		}
		if ((v = num(crs, "LuminanceSmoothing")) != null && v >= 25) {
			s.filters.add(ImageEdit.Filter.DENOISE);
			report.applied.add("Noise reduction " + signed(v) + "  ->  filter Denoise");
		}
		if ((v = num(crs, "PostCropVignetteAmount", "VignetteAmount")) != null && v < 0) {
			s.vignette = clamp(-v, 0, 100);
			report.applied.add("Vignette " + signed(v) + "  ->  Vignette " + s.vignette);
		}
		if ((v = num(crs, "GrainAmount")) != null && v > 0) {
			s.grain = clamp(v, 0, 100);
			report.applied.add("Grain " + signed(v) + "  ->  Film grain " + s.grain);
		}
		if (flag(crs, "ConvertToGrayscale") || "Black & White".equalsIgnoreCase(crs.get("Treatment"))) {
			s.filters.add(ImageEdit.Filter.GRAYSCALE);
			report.applied.add("Black & White treatment  ->  filter B & W");
		}
		if (flag(crs, "HasCrop")) {
			Double l = num(crs, "CropLeft");
			Double t = num(crs, "CropTop");
			Double r = num(crs, "CropRight");
			Double b = num(crs, "CropBottom");
			if (l != null && t != null && r != null && b != null) {
				s.cropLeft = clamp(l * 100, 0, 45);
				s.cropTop = clamp(t * 100, 0, 45);
				s.cropRight = clamp((1 - r) * 100, 0, 45);
				s.cropBottom = clamp((1 - b) * 100, 0, 45);
				report.applied.add("Crop  ->  margins L " + s.cropLeft + "%, T " + s.cropTop + "%, R " + s.cropRight + "%, B " + s.cropBottom + "%");
			}
			if ((v = num(crs, "CropAngle")) != null && Math.abs(v) >= 0.5) {
				s.straighten = clamp(v, -45, 45);
				report.applied.add("Straighten " + signed(v) + " deg  ->  Straighten " + s.straighten + " (check the direction)");
			}
		}

		// what has no equivalent: listed only when it is actually set
		addIgnored(report, crs, "Tone curve", "ToneCurve", "ParametricShadows", "ParametricDarks", "ParametricLights", "ParametricHighlights");
		addIgnored(report, crs, "HSL / color mixer", "HueAdjustment", "SaturationAdjustment", "LuminanceAdjustment");
		addIgnored(report, crs, "Split toning / color grading", "SplitToning", "ColorGrade");
		addIgnored(report, crs, "Texture", "Texture");
		addIgnored(report, crs, "Calibration", "ShadowTint", "RedHue", "RedSaturation", "GreenHue", "GreenSaturation", "BlueHue", "BlueSaturation");
		addIgnored(report, crs, "Lens corrections / Upright / perspective", "LensProfile", "LensManualDistortionAmount", "ChromaticAberration", "PerspectiveVertical",
				"PerspectiveHorizontal", "PerspectiveRotate", "PerspectiveScale", "AutoLateralCA", "UprightVersion");
		addIgnored(report, crs, "Local adjustments (masks, gradients, brushes)", "GradientBasedCorrections", "CircularGradientBasedCorrections", "PaintBasedCorrections",
				"MaskGroupBasedCorrections", "RetouchAreas", "RedEyeInfo");
		addIgnored(report, crs, "Detail panel (sharpening radius, masking, color noise)", "SharpenRadius", "SharpenDetail", "SharpenEdgeMasking", "ColorNoiseReduction",
				"LuminanceSmoothing");
		addIgnored(report, crs, "Profile / look", "CameraProfile", "LookTable", "Look");
		return report;
	}

	private static void addIgnored(Report report, Map<String, String> crs, String group, String... prefixes) {
		for (Map.Entry<String, String> e : crs.entrySet()) {
			for (String prefix : prefixes) {
				if (e.getKey().startsWith(prefix) && meaningful(e.getValue())) {
					report.ignored.add(group);
					return;
				}
			}
		}
	}

	private static boolean meaningful(String value) {
		if (value == null) {
			return false;
		}
		String v = value.trim();
		if (v.isEmpty() || v.equalsIgnoreCase("false") || v.equalsIgnoreCase("none")) {
			return false;
		}
		try {
			return Double.parseDouble(v.replace("+", "")) != 0;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	/** The name given to the preset, if the file has one */
	public static String nameOf(Map<String, String> crs) {
		String n = crs.get("PresetName");
		return n != null ? n : crs.get("title");
	}
}
