/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package gui.commons.imagestudio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Adobe presets (.xmp and .lrtemplate): parsing, translation to Image Studio settings and the report */
public class PhotoshopPresetsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String XMP_ATTRIBUTES = "<?xpacket begin=\"\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n"
			+ "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
			+ "<rdf:Description xmlns:crs=\"http://ns.adobe.com/camera-raw-settings/1.0/\" crs:PresetName=\"Warm &amp; Moody\" crs:Exposure2012=\"+1.00\" crs:Contrast2012=\"+20\""
			+ " crs:Saturation=\"-10\" crs:Vibrance=\"15\" crs:ToneCurvePV2012=\"{...}\" crs:Temperature=\"6500\" crs:Tint=\"+30\"/>\n"
			+ "</rdf:RDF></x:xmpmeta>\n<?xpacket end=\"w\"?>          \n";

	private static Map<String, String> map(String... keyValues) {
		Map<String, String> m = new LinkedHashMap<String, String>();
		for (int i = 0; i < keyValues.length; i += 2) {
			m.put(keyValues[i], keyValues[i + 1]);
		}
		return m;
	}

	// ------------------------------------------------------------------ parsing

	@Test
	public void xmpAttributesAndPacketWrappers() throws IOException {
		Map<String, String> crs = PhotoshopPresets.parseXmp(XMP_ATTRIBUTES);
		assertEquals("Warm & Moody", crs.get("PresetName"));
		assertEquals("+1.00", crs.get("Exposure2012"));
		assertEquals("+20", crs.get("Contrast2012"));
		assertEquals("6500", crs.get("Temperature"));
		assertEquals("Warm & Moody", PhotoshopPresets.nameOf(crs));
	}

	@Test
	public void xmpSimpleAndStructuredElements() throws IOException {
		String xml = "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" xmlns:crs=\"c\"><rdf:RDF xmlns:rdf=\"r\"><rdf:Description>"
				+ "<crs:Exposure2012> +0.50 </crs:Exposure2012><crs:ToneCurvePV2012><rdf:Seq><rdf:li>0, 0</rdf:li></rdf:Seq></crs:ToneCurvePV2012><crs:Empty></crs:Empty>"
				+ "</rdf:Description></rdf:RDF></x:xmpmeta>";
		Map<String, String> crs = PhotoshopPresets.parseXmp(xml);
		assertEquals("+0.50", crs.get("Exposure2012"));
		assertEquals("{...}", crs.get("ToneCurvePV2012"));
		assertFalse(crs.containsKey("Empty"));
		assertNull(PhotoshopPresets.nameOf(crs));
	}

	@Test
	public void xmpWithoutSettingsIsEmpty() throws IOException {
		assertTrue(PhotoshopPresets.parseXmp("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF xmlns:rdf=\"r\"/></x:xmpmeta>").isEmpty());
	}

	@Test
	public void xmpWithADoctypeOrBrokenXmlIsRefused() {
		String xxe = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x:xmpmeta xmlns:x=\"a\">&e;</x:xmpmeta>";
		for (String bad : new String[] { xxe, "<x:xmpmeta", "not xml at all", "" }) {
			try {
				PhotoshopPresets.parseXmp(bad);
				fail("accepted: " + bad);
			} catch (IOException expected) {
				assertTrue(expected.getMessage().startsWith("Cannot read the XMP"));
			}
		}
	}

	@Test
	public void lrTemplateLuaTable() {
		String lua = "s = {\n\tid = \"ABC\",\n\ttitle = \"Film look\",\n\tvalue = {\n\t\tsettings = {\n\t\t\tExposure2012 = 0.5,\n\t\t\tContrast2012 = -25,\n\t\t\tToneCurve = {\n\t\t\t\t0,\n\t\t\t},\n"
				+ "\t\t\tConvertToGrayscale = true,\n\t\t\tCameraProfile = \"Adobe Standard\",\n\t\t},\n\t},\n}\r\n";
		Map<String, String> crs = PhotoshopPresets.parseLrTemplate(lua);
		assertEquals("Film look", crs.get("title"));
		assertEquals("0.5", crs.get("Exposure2012"));
		assertEquals("-25", crs.get("Contrast2012"));
		assertEquals("true", crs.get("ConvertToGrayscale"));
		assertEquals("Adobe Standard", crs.get("CameraProfile"));
		assertEquals("{...}", crs.get("ToneCurve"));
		assertEquals("{...}", crs.get("value"));
		assertEquals("Film look", PhotoshopPresets.nameOf(crs));
		assertTrue(PhotoshopPresets.parseLrTemplate("").isEmpty());
		assertTrue(PhotoshopPresets.parseLrTemplate("-- just a comment\n}\n").isEmpty());
	}

	@Test
	public void readPicksTheParserByExtension() throws IOException {
		File xmp = new File(tmp.getRoot(), "Look.XMP");
		Files.write(xmp.toPath(), XMP_ATTRIBUTES.getBytes(StandardCharsets.UTF_8));
		assertEquals("+1.00", PhotoshopPresets.read(xmp).get("Exposure2012"));
		File lr = new File(tmp.getRoot(), "look.lrtemplate");
		Files.write(lr.toPath(), "Exposure2012 = 0.25,\n".getBytes(StandardCharsets.UTF_8));
		assertEquals("0.25", PhotoshopPresets.read(lr).get("Exposure2012"));
		assertTrue(PhotoshopPresets.isAdobePreset(xmp));
		assertTrue(PhotoshopPresets.isAdobePreset(lr));
		assertFalse(PhotoshopPresets.isAdobePreset(new File("a.properties")));
		assertFalse(PhotoshopPresets.isAdobePreset(null));
	}

	// ------------------------------------------------------------------ translation

	@Test
	public void basicTonesAreTranslated() throws IOException {
		ImageSettings s = new ImageSettings();
		s.format = "jpg";
		s.quality = 77;
		s.brightness = 40; // an old edit: the import replaces the editing part
		PhotoshopPresets.Report r = PhotoshopPresets.apply(PhotoshopPresets.parseXmp(XMP_ATTRIBUTES), s, null);
		assertEquals("Warm & Moody", r.name);
		assertEquals("exposure +1 EV = 50", 50, s.exposure);
		assertEquals(20, s.contrast);
		assertEquals(-10, s.saturation);
		assertEquals(15, s.vibrance);
		assertEquals("(6500 - 5500) / 40", 25, s.temperature);
		assertEquals("30 * 100 / 150", 20, s.tint);
		assertEquals("edits replaced", 0, s.brightness);
		assertEquals("the output choices stay", "jpg", s.format);
		assertEquals(77, s.quality);
		assertTrue(r.anythingApplied());
		assertEquals(1, r.ignored.size());
		assertEquals("Tone curve", r.ignored.get(0));
		String html = r.toHtml();
		assertTrue(html.contains("Warm &amp; Moody"));
		assertTrue(html.contains("Not applied"));
	}

	@Test
	public void valuesAreClampedToTheRanges() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.apply(map("Exposure2012", "+5", "Contrast2012", "300", "Saturation", "-500", "Vibrance", "101", "Temperature", "50000", "Tint", "-900"), s, "x");
		assertEquals(100, s.exposure);
		assertEquals(100, s.contrast);
		assertEquals(-100, s.saturation);
		assertEquals(100, s.vibrance);
		assertEquals(100, s.temperature);
		assertEquals(-100, s.tint);
	}

	@Test
	public void highlightsShadowsWhitesBlacksCombine() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.Report r = PhotoshopPresets.apply(map("Highlights2012", "-50", "Whites2012", "20", "Shadows2012", "40", "Blacks2012", "-30"), s, "t");
		assertEquals(-30, s.highlights); // -50 * 0.8 + 20 * 0.5
		assertEquals(17, s.shadows); // 40 * 0.8 - 30 * 0.5 = 17
		assertEquals(2, r.applied.size());
	}

	@Test
	public void relativeAndAbsoluteTemperature() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.apply(map("Temperature", "-30"), s, "t");
		assertEquals("relative values are taken as they are", -30, s.temperature);
		PhotoshopPresets.apply(map("Temperature", "5500"), s, "t");
		assertEquals("the neutral Kelvin value changes nothing", 0, s.temperature);
		PhotoshopPresets.apply(map("Temperature", "3500"), s, "t");
		assertEquals(-50, s.temperature);
	}

	@Test
	public void numbersWithPlusSignsAndGarbage() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.Report r = PhotoshopPresets.apply(map("Contrast2012", "abc", "Saturation", "+25", "Vibrance", ""), s, "t");
		assertEquals(0, s.contrast);
		assertEquals(25, s.saturation);
		assertEquals(1, r.applied.size());
	}

	@Test
	public void oldProcessAndNewProcessNames() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.apply(map("Exposure", "-0.5", "Contrast", "10", "Brightness", "75", "Shadows", "10"), s, "old");
		assertEquals(-25, s.exposure);
		assertEquals(10, s.contrast);
		assertEquals("(75 - 50) * 0.8", 20, s.brightness);
		assertEquals(8, s.shadows);
		ImageSettings withNew = new ImageSettings();
		PhotoshopPresets.apply(map("Exposure2012", "0.5", "Brightness", "75"), withNew, "new");
		assertEquals("brightness of the old process is ignored next to the new exposure", 0, withNew.brightness);
	}

	@Test
	public void detailEffectsAndFilters() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.Report r = PhotoshopPresets.apply(map("Clarity2012", "40", "Sharpness", "60", "LuminanceSmoothing", "30", "PostCropVignetteAmount", "-35", "GrainAmount", "20",
				"Dehaze", "50", "Contrast2012", "10"), s, "t");
		assertTrue(s.filters.contains(ImageEdit.Filter.CLARITY));
		assertTrue(s.filters.contains(ImageEdit.Filter.DENOISE));
		assertEquals(40, s.sharpness);
		assertEquals(35, s.vignette);
		assertEquals(20, s.grain);
		assertEquals("10 + 50 * 0.3", 25, s.contrast);
		assertEquals("50 * 0.2", 10, s.saturation);
		assertTrue(r.ignored.contains("Detail panel (sharpening radius, masking, color noise)"));

		ImageSettings soft = new ImageSettings();
		PhotoshopPresets.apply(map("Clarity2012", "-30"), soft, "t");
		assertTrue(soft.filters.contains(ImageEdit.Filter.SOFT_FOCUS));
		ImageSettings small = new ImageSettings();
		PhotoshopPresets.apply(map("Clarity2012", "10"), small, "t");
		assertTrue("under 15 is too small to matter", small.filters.isEmpty());
	}

	@Test
	public void defaultSharpnessOfOldFilesIsNotImported() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.apply(map("Sharpness", "40"), s, "t");
		assertEquals(0, s.sharpness);
		PhotoshopPresets.apply(map("Sharpness", "40", "SharpenRadius", "1.0"), s, "t");
		assertEquals(27, s.sharpness);
	}

	@Test
	public void blackAndWhiteTreatments() {
		ImageSettings a = new ImageSettings();
		PhotoshopPresets.apply(map("ConvertToGrayscale", "True"), a, "t");
		assertTrue(a.filters.contains(ImageEdit.Filter.GRAYSCALE));
		ImageSettings b = new ImageSettings();
		PhotoshopPresets.apply(map("Treatment", "black & white"), b, "t");
		assertTrue(b.filters.contains(ImageEdit.Filter.GRAYSCALE));
		ImageSettings c = new ImageSettings();
		PhotoshopPresets.apply(map("ConvertToGrayscale", "false", "Treatment", "Color"), c, "t");
		assertTrue(c.filters.isEmpty());
	}

	@Test
	public void cropAndStraighten() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.Report r = PhotoshopPresets.apply(map("HasCrop", "True", "CropLeft", "0.1", "CropTop", "0.05", "CropRight", "0.9", "CropBottom", "0.8", "CropAngle", "-3.4"), s, "t");
		assertEquals(10, s.cropLeft);
		assertEquals(5, s.cropTop);
		assertEquals(10, s.cropRight);
		assertEquals(20, s.cropBottom);
		assertEquals(-3, s.straighten);
		assertEquals(2, r.applied.size());
		ImageSettings partial = new ImageSettings();
		PhotoshopPresets.apply(map("HasCrop", "True", "CropLeft", "0.1"), partial, "t");
		assertFalse("an incomplete crop is not guessed", partial.hasCrop());
		ImageSettings none = new ImageSettings();
		PhotoshopPresets.apply(map("HasCrop", "False", "CropLeft", "0.1", "CropTop", "0.1", "CropRight", "1", "CropBottom", "1"), none, "t");
		assertFalse(none.hasCrop());
		ImageSettings huge = new ImageSettings();
		PhotoshopPresets.apply(map("HasCrop", "1", "CropLeft", "0.9", "CropTop", "0", "CropRight", "0.1", "CropBottom", "1"), huge, "t");
		assertEquals("margins are limited to 45%", 45, huge.cropLeft);
		assertEquals(45, huge.cropRight);
	}

	@Test
	public void unsupportedAdjustmentsAreListedOnlyWhenSet() {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.Report r = PhotoshopPresets.apply(map("HueAdjustmentRed", "0", "SaturationAdjustmentBlue", "+12", "Texture", "0", "LensProfileName", "Canon", "GradientBasedCorrections", "{...}",
				"CameraProfile", "none", "Look", "false", "ParametricShadows", "0", "RedHue", "-5", "SplitToningShadowHue", "0"), s, "t");
		assertTrue(r.ignored.contains("HSL / color mixer"));
		assertFalse("Texture = 0 is not set", r.ignored.contains("Texture"));
		assertTrue(r.ignored.contains("Lens corrections / Upright / perspective"));
		assertTrue(r.ignored.contains("Local adjustments (masks, gradients, brushes)"));
		assertFalse("none and false mean nothing", r.ignored.contains("Profile / look"));
		assertFalse(r.ignored.contains("Tone curve"));
		assertTrue(r.ignored.contains("Calibration"));
		assertFalse(r.ignored.contains("Split toning / color grading"));
		assertFalse(r.anythingApplied());
		assertTrue(r.toHtml().contains("No setting that the Image Studio understands"));
	}

	@Test
	public void reportNamesAndHtmlEscaping() {
		ImageSettings s = new ImageSettings();
		assertEquals("title", PhotoshopPresets.apply(map("title", "title"), s, "").name);
		assertEquals("given wins", PhotoshopPresets.apply(map("PresetName", "inside"), s, "given wins").name);
		assertEquals("inside", PhotoshopPresets.apply(map("PresetName", "inside"), s, null).name);
		PhotoshopPresets.Report r = new PhotoshopPresets.Report();
		assertTrue(r.toHtml().contains("Imported settings"));
		r.name = "<b>&";
		r.applied.add("a < b");
		assertTrue(r.toHtml().contains("&lt;b&gt;&amp;"));
		assertTrue(r.toHtml().contains("a &lt; b"));
	}

	@Test
	public void everyAppliedSettingKeepsTheImageEngineUsable() throws IOException {
		ImageSettings s = new ImageSettings();
		PhotoshopPresets.apply(PhotoshopPresets.parseXmp(XMP_ATTRIBUTES), s, null);
		java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_RGB);
		java.awt.image.BufferedImage out = ImageEdit.apply(img, s);
		assertEquals(20, out.getWidth());
		assertEquals(20, out.getHeight());
	}
}
