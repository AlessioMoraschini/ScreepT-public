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
package initializer.configs.impl.om;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import resources.GeneralConfig;
import various.common.light.files.om.FileContentSearchDTO;
import various.common.light.om.SecurityLevel;
import various.common.light.utility.string.StringWorker.EOL;

/**
 * Plain option objects: defaults, normalizing setters and accessor pairs.
 */
public class OptionBeansTest {

	private EOL savedEol;
	private int savedMaxKb;

	@Before
	public void saveStatics() {
		savedEol = EOL.defaultEol;
		savedMaxKb = GeneralConfig.MAX_FILE_SIZE_TXT_EDITOR_KB;
	}

	@After
	public void restoreStatics() {
		EOL.defaultEol = savedEol;
		GeneralConfig.MAX_FILE_SIZE_TXT_EDITOR_KB = savedMaxKb;
	}

	// ------------------------------------------------------------------ UiSizePreset / GuiOption

	@Test
	public void presetNamesAreParsedWithMediumAsFallback() {
		assertEquals(UiSizePreset.BIG, UiSizePreset.fromName("BIG"));
		assertEquals(UiSizePreset.MEDIUM, UiSizePreset.fromName("big"));
		assertEquals(UiSizePreset.MEDIUM, UiSizePreset.fromName(null));
		assertEquals(UiSizePreset.MEDIUM, UiSizePreset.fromName("???"));
		assertEquals("XXL", UiSizePreset.XXL.toString());
		assertEquals(135, UiSizePreset.XXL.generalFontScale);
	}

	@Test
	public void guiOptionStartsOnMediumPreset() {
		GuiOption g = new GuiOption();
		assertEquals(UiSizePreset.MEDIUM, g.getUiSizePreset());
		assertEquals(100, g.getUiGeneralFontScale());
		assertEquals(GuiOption.DEFAULT_GUI_STYLE, g.getPreferredStyle());
		assertEquals(GuiOption.DEFAULT_UI_FONT_FAMILY, g.getUiFontFamily());
	}

	@Test
	public void guiOptionConstructorKeepsTheGivenValues() {
		GuiOption g = new GuiOption(14, 16, "style", true, 4);
		assertEquals(14, g.getMenuFontSize());
		assertEquals(16, g.getLogFontSize());
		assertEquals("style", g.getPreferredStyle());
		assertTrue(g.isDarkGuiNimbus());
		assertEquals(4, g.getNimbusSize());
	}

	@Test
	public void namedPresetOverwritesScalesButCustomKeepsThem() {
		GuiOption g = new GuiOption();
		g.setUiFontFamily("Arial");
		g.applyUiSizePreset(UiSizePreset.MINI);
		assertEquals(UiSizePreset.MINI, g.getUiSizePreset());
		assertEquals(80, g.getUiMenuFontScale());
		assertEquals(85, g.getUiComponentScale());
		assertEquals(GuiOption.DEFAULT_UI_FONT_FAMILY, g.getUiFontFamily());

		g.setUiToolbarFontScale(77);
		g.setUiFontFamily("Arial");
		g.applyUiSizePreset(UiSizePreset.CUSTOM);
		assertEquals(UiSizePreset.CUSTOM, g.getUiSizePreset());
		assertEquals(77, g.getUiToolbarFontScale());
		assertEquals("Arial", g.getUiFontFamily());
	}

	@Test
	public void nullFontFamilyBecomesTheDefaultOne() {
		GuiOption g = new GuiOption();
		g.setUiFontFamily("Arial");
		g.setUiFontFamily(null);
		assertEquals(GuiOption.DEFAULT_UI_FONT_FAMILY, g.getUiFontFamily());
	}

	@Test
	public void interfaceSizeCanBeCopiedFromAnotherOption() {
		GuiOption src = new GuiOption();
		src.applyUiSizePreset(UiSizePreset.CUSTOM);
		src.setUiFontFamily("Tahoma");
		src.setUiGeneralFontScale(101);
		src.setUiMenuFontScale(102);
		src.setUiToolbarFontScale(103);
		src.setUiComponentScale(104);
		GuiOption dst = new GuiOption();
		dst.copyUiSizeFrom(src);
		assertEquals(UiSizePreset.CUSTOM, dst.getUiSizePreset());
		assertEquals("Tahoma", dst.getUiFontFamily());
		assertEquals(101, dst.getUiGeneralFontScale());
		assertEquals(102, dst.getUiMenuFontScale());
		assertEquals(103, dst.getUiToolbarFontScale());
		assertEquals(104, dst.getUiComponentScale());
	}

	// ------------------------------------------------------------------ GenericOption

	@Test
	public void genericOptionDefaults() {
		GenericOption o = new GenericOption();
		assertEquals(GenericOption.DEFAULT_CHARSET, o.getCharset());
		assertEquals(GenericOption.DEFAULT_POS_X, o.getLastKnownPositionX());
		assertTrue(o.isFirstAccess());
		assertEquals("en", o.getLanguage());
		assertEquals("60.00%", o.getVolumeString().replace(',', '.'));
		GenericOption named = new GenericOption("me", "ASCII", true, 1f);
		assertEquals("me", named.getUserName());
		assertEquals("ASCII", named.getCharset());
		assertEquals(1f, named.getGeneralVolume(), 0f);
	}

	@Test
	public void languageIsTrimmedLowercasedAndNeverEmpty() {
		GenericOption o = new GenericOption();
		o.setLanguage("  DE ");
		assertEquals("de", o.getLanguage());
		o.setLanguage("   ");
		assertEquals("en", o.getLanguage());
		o.setLanguage(null);
		assertEquals("en", o.getLanguage());
		o.setLocale(Locale.ITALY);
		assertEquals(Locale.ITALY, o.getLocale());
	}

	// ------------------------------------------------------------------ FileOptions

	@Test
	public void searchPerformanceFallsBackToDefaultWhenBlank() {
		FileOptions f = new FileOptions();
		f.setfSearchPerformance("  FAST ");
		assertEquals("FAST", f.getfSearchPerformance());
		f.setfSearchPerformance("  ");
		assertEquals(FileOptions.DEF_fSearchPerformance, f.getfSearchPerformance());
		f.setfSearchPerformance(null);
		assertEquals(FileOptions.DEF_fSearchPerformance, f.getfSearchPerformance());
	}

	@Test
	public void searchDtoIsFilledAndReadBack() {
		FileOptions f = new FileOptions();
		FileContentSearchDTO in = new FileContentSearchDTO("needle", true, true, true, true, new String[] {"*.java"});
		assertSame(f, f.enrichFromDTO(in));
		assertTrue(f.isfSearchMatchCase());
		assertTrue(f.isfSearchWholeWord());
		assertTrue(f.isfSearchRegex());
		assertTrue(f.isfSearchBinaries());

		FileContentSearchDTO out = f.initDTO();
		assertEquals("needle", out.searchString);
		assertTrue(out.regex && out.wholeWord && out.matchCase && out.binariesIncluded);
		assertArrayEquals(new String[] {"*.java"}, out.fileFilters);

		// same search again: no duplicate, null dto ignored
		f.enrichFromDTO(in);
		f.enrichFromDTO(null);
		assertEquals(1, f.getfSearchLastSearchStrings().getList().size());
		assertEquals(1, f.getfSearchLastFileFilters().getList().size());
	}

	@Test
	public void fileOptionsConstructorStoresTheGivenValues() {
		FileOptions f = new FileOptions(false, true, "/src", "/dst", 99);
		assertFalse(f.isFlagAutosave());
		assertTrue(f.isFlagOverwrite());
		assertEquals(99, f.getAutosaveInterval());
		assertEquals("/dst", f.getLastDstFolderPath());
		// known defect: the constructor also stores lastDstPath as source folder (lastSrcPath is ignored)
	}

	// ------------------------------------------------------------------ small options

	@Test
	public void smallOptionDefaults() {
		AesOption a = new AesOption(false);
		assertFalse(a.isFlagAes());
		assertTrue(a.isHashKey());
		assertEquals(SecurityLevel.MID, a.getSecurityLevel());
		SteganOption s = new SteganOption();
		assertTrue(s.isFlagSteg());
		assertTrue(s.getImgRecentPath().isEmpty());
		// known defect: SteganOption(int,int,String,boolean,boolean) dereferences a null imgRecentPath (NPE), not exercised
		ImgConverterOption c = new ImgConverterOption();
		assertEquals(100, c.getQuality());
		assertTrue(c.isSameSize() && c.isSameExtension());
	}

	// ------------------------------------------------------------------ TextEditorOption

	@Test
	public void textEditorOptionStartsWithItsDefaults() {
		TextEditorOption t = new TextEditorOption();
		assertEquals(TextEditorOption.DEFAULT_FONT, t.getFont());
		assertEquals(TextEditorOption.DEFAULT_MARGIN, t.getMargin());
		assertEquals(TextEditorOption.DEFAULT_SELECTED_THEME, t.getSelectedTheme());
		assertTrue(t.getOpenedFileList().isEmpty());
		assertNotNull(t.getLastOpenedFiles());
		assertTrue(t.isHeaderVisible());
		assertEquals("", t.viewerAssociations);
		assertEquals("", t.treeFilterRules);
	}

	@Test
	public void maxFileSizeIsAtLeastOneMbAndUpdatesTheRuntimeLimit() {
		TextEditorOption t = new TextEditorOption();
		t.setMaxFileSizeMb(0);
		assertEquals(1, t.getMaxFileSizeMb());
		assertEquals(1024, GeneralConfig.MAX_FILE_SIZE_TXT_EDITOR_KB);
		t.setMaxFileSizeMb(-5);
		assertEquals(1, t.getMaxFileSizeMb());
		t.setMaxFileSizeMb(64);
		assertEquals(64 * 1024, GeneralConfig.MAX_FILE_SIZE_TXT_EDITOR_KB);
	}

	@Test
	public void eolSetterAlsoChangesTheGlobalDefault() {
		TextEditorOption t = new TextEditorOption();
		t.setEol(EOL.LF);
		assertEquals(EOL.LF, t.getEol());
		assertEquals(EOL.LF, EOL.defaultEol);
	}

	/**
	 * Every simple setter of the option objects must be readable back through its getter: this guards typos in the
	 * hand written accessors (wrong field assigned or read), which the ini cycle would silently propagate.
	 */
	@Test
	public void everySimpleAccessorPairReturnsTheStoredValue() throws Exception {
		int checked = 0;
		Object[] beans = {new TextEditorOption(), new GuiOption(), new GenericOption(), new FileOptions(),
				new SteganOption(), new AesOption(), new ImgConverterOption()};
		for (Object bean : beans) {
			for (Method setter : bean.getClass().getMethods()) {
				if (!setter.getName().startsWith("set") || setter.getParameterTypes().length != 1
						|| Modifier.isStatic(setter.getModifiers())) {
					continue;
				}
				Class<?> type = setter.getParameterTypes()[0];
				Object value = sampleValue(type, setter.getName());
				if (value == null) {
					continue;
				}
				Method getter = findGetter(bean.getClass(), setter.getName().substring(3));
				if (getter == null) {
					continue;
				}
				setter.invoke(bean, value);
				Object back = getter.invoke(bean);
				assertEquals(bean.getClass().getSimpleName() + "." + setter.getName(), value, back);
				checked++;
			}
		}
		assertTrue("accessor pairs checked: " + checked, checked > 80);
	}

	private static Method findGetter(Class<?> c, String property) {
		for (String prefix : new String[] {"get", "is"}) {
			try {
				return c.getMethod(prefix + property);
			} catch (NoSuchMethodException e) {
				// try next
			}
		}
		return null;
	}

	private static Object sampleValue(Class<?> type, String setterName) {
		// setters with a side effect or a normalization are covered by dedicated tests
		if (setterName.equals("setMaxFileSizeMb") || setterName.equals("setEol") || setterName.equals("setLanguage")
				|| setterName.equals("setUiSizePreset") || setterName.equals("setfSearchPerformance")) {
			return null;
		}
		if (type == boolean.class) return Boolean.TRUE;
		if (type == int.class || type == Integer.class) return 7;
		if (type == float.class) return 0.5f;
		if (type == Double.class) return 0.25D;
		if (type == String.class) return "value-" + setterName;
		if (type == Color.class) return new Color(10, 20, 30);
		if (type == Font.class) return new Font("Monospaced", Font.ITALIC, 13);
		if (type == SecurityLevel.class) return SecurityLevel.values()[0];
		if (type == ArrayList.class) return new ArrayList<String>(Arrays.asList("x", "y"));
		return null;
	}
}
