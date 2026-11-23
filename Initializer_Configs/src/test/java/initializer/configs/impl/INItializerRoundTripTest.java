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
package initializer.configs.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.ini4j.Wini;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import initializer.configs.impl.om.AesOption;
import initializer.configs.impl.om.FileOptions;
import initializer.configs.impl.om.GenericOption;
import initializer.configs.impl.om.GuiOption;
import initializer.configs.impl.om.ImgConverterOption;
import initializer.configs.impl.om.SteganOption;
import initializer.configs.impl.om.TextEditorOption;
import initializer.configs.impl.om.UiSizePreset;
import resources.GeneralConfig;
import various.common.light.files.om.FileNamed;
import various.common.light.om.ScalingAlgorithm;
import various.common.light.om.SecurityLevel;
import various.common.light.utility.string.StringWorker.EOL;

/**
 * Write/read cycle of {@link INItializer} on temporary ini files: defaults, custom values and corrupted values.
 */
public class INItializerRoundTripTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File iniFile;
	private EOL savedEol;
	private int savedMaxKb;

	@Before
	public void createIni() throws IOException {
		iniFile = tmp.newFile("CONFIG.ini");
		// some setters also change static state: restored in tearDown
		savedEol = EOL.defaultEol;
		savedMaxKb = GeneralConfig.MAX_FILE_SIZE_TXT_EDITOR_KB;
	}

	@After
	public void restoreStatics() {
		EOL.defaultEol = savedEol;
		GeneralConfig.MAX_FILE_SIZE_TXT_EDITOR_KB = savedMaxKb;
	}

	private INItializer open() throws IOException {
		return new INItializer(new Wini(iniFile));
	}

	/** Ini where every section already exists with the given extra values (as after a first write) */
	private INItializer openWithDefaultsPlus(String section, String key, String value) throws IOException {
		INItializer writer = open();
		writer.writeCurrentConfigIniFile();
		Wini wini = new Wini(iniFile);
		wini.put(section, key, value);
		wini.store();
		INItializer reader = open();
		reader.readConfigIniFile();
		return reader;
	}

	@Test
	public void defaultsSurviveAWriteAndReadCycle() throws IOException {
		INItializer first = open();
		first.writeCurrentConfigIniFile();
		assertTrue(iniFile.length() > 0);

		INItializer second = open();
		second.readConfigIniFile();

		assertEquals(AesOption.DEFAULT_FLAG_AES, second.getAesOpt().isFlagAes());
		assertEquals(AesOption.DEF_securityLevel, second.getAesOpt().getSecurityLevel());
		assertEquals(FileOptions.DEFAULT_FLAG_AUTOSAVE, second.getFileOpt().isFlagAutosave());
		assertEquals(FileOptions.DEF_fSearchPerformance, second.getFileOpt().getfSearchPerformance());
		assertEquals(GuiOption.DEFAULT_GUI_STYLE, second.getGuiOpt().getPreferredStyle());
		assertEquals(UiSizePreset.MEDIUM, second.getGuiOpt().getUiSizePreset());
		assertEquals(GenericOption.DEFAULT_CHARSET, second.getGenOpt().getCharset());
		assertEquals(GenericOption.DEFAULT_PANEL_CLASS_AT_START, second.getGenOpt().getLastPanelOpened());
		assertEquals(GenericOption.DEFAULT_VOLUME, second.getGenOpt().getGeneralVolume(), 0.001f);
		assertEquals(SteganOption.DEFAULT_WIDTH, second.getSteganOpt().getDefaultWidth());
		assertEquals(ImgConverterOption.DEFAULT_ALGORITHM, second.getImageConvertionOpt().getAlgorithm());
		TextEditorOption te = second.getTextEditorOpt();
		assertEquals(TextEditorOption.DEFAULT_FONT, te.getFont());
		assertEquals(TextEditorOption.DEFAULT_BACKGROUND_COL, te.getBackCol());
		assertEquals(TextEditorOption.DEFAULT_SELECTED_THEME, te.getSelectedTheme());
		assertEquals(TextEditorOption.DEFAULT_TAB_SIZE, te.getTabSize());
		assertEquals(TextEditorOption.DEF_dividerBottomPosRatio, te.getDividerBottomPosRatio());
		// known defect: TextEditorOption.init() never applies DEF_autosaveTempFiles (true), so the object default is false
	}

	@Test
	public void customValuesRoundTripForEverySection() throws IOException {
		INItializer w = open();

		w.getAesOpt().setFlagAes(false);
		w.getAesOpt().setHashKey(false);
		w.getAesOpt().setSecurityLevel(SecurityLevel.values()[SecurityLevel.values().length - 1]);

		FileOptions f = w.getFileOpt();
		f.setfNameSearchUseManual(true);
		f.setfNameSearchManualDir(tmp.getRoot());
		f.setfSearchBinaries(true);
		f.setfSearchMatchCase(true);
		f.setfSearchRegex(true);
		f.setfSearchWholeWord(true);
		f.setfSearchPerformance("FAST");
		f.getfSearchLastSearchStrings().addLast("alpha");
		f.getfSearchLastSearchStrings().addLast("beta gamma");
		f.getfSearchLastLoadedFiles().addLast(new FileNamed(new File(tmp.getRoot(), "a.txt")));
		f.getfSearchLastFileFilters().addLast(new String[] {"*.java", "*.xml"});
		f.setFlagAutosave(false);
		f.setFlagOverwrite(true);
		f.setLastSrcFolderPath(new File(tmp.getRoot(), "src").getAbsolutePath());
		f.setLastDstFolderPath(new File(tmp.getRoot(), "dst").getAbsolutePath());
		f.setAutosaveInterval(120000);

		GuiOption g = w.getGuiOpt();
		g.setPreferredStyle("some.Laf");
		g.setPreferredThemeName("Sunrise");
		g.setMenuFontSize(15);
		g.setLogFontSize(17);
		g.setNimbusSize(3);
		g.setDarkGuiNimbus(true);
		g.applyUiSizePreset(UiSizePreset.CUSTOM);
		g.setUiFontFamily("Consolas");
		g.setUiGeneralFontScale(111);
		g.setUiMenuFontScale(122);
		g.setUiToolbarFontScale(133);
		g.setUiComponentScale(144);

		GenericOption o = w.getGenOpt();
		o.setUserName("tester");
		o.setCharset("ISO-8859-1");
		o.setLastKnownPositionX(300);
		o.setLastKnownPositionY(200);
		o.setGeneralVolume(0.25f);
		o.setLastPanelOpened(java.awt.Panel.class);
		o.setAutomaticUpdateOn(false);
		o.setAutomaticPluginUpdateOn(false);
		o.setExceptionAdvicesEnabled(true);
		o.setTxtEditorFileMonitorActive(false);
		o.setFirstAccess(false);
		o.setLocale(new java.util.Locale("fr"));
		o.setLanguage("IT ");

		SteganOption s = w.getSteganOpt();
		s.setUltraSecure(false);
		s.setFlagSteg(false);
		s.setDefaultWidth(640);
		s.setDefaultHeight(480);
		s.setImgRecentPath(new Vector<>(Arrays.asList("one.png", "two.png", "three.png")));

		ImgConverterOption c = w.getImageConvertionOpt();
		c.setSameSize(false);
		c.setSameExtension(false);
		c.setQuality(42);
		c.setAlgorithm(ScalingAlgorithm.values()[ScalingAlgorithm.values().length - 1]);

		fillTextEditor(w.getTextEditorOpt());
		w.writeCurrentConfigIniFile();

		INItializer r = open();
		r.readConfigIniFile();

		assertFalse(r.getAesOpt().isFlagAes());
		assertFalse(r.getAesOpt().isHashKey());
		assertEquals(w.getAesOpt().getSecurityLevel(), r.getAesOpt().getSecurityLevel());

		FileOptions rf = r.getFileOpt();
		assertTrue(rf.isfNameSearchUseManual());
		assertEquals(tmp.getRoot().getCanonicalFile(), rf.getfNameSearchManualDir().getCanonicalFile());
		assertTrue(rf.isfSearchBinaries());
		assertTrue(rf.isfSearchRegex());
		assertTrue(rf.isfSearchWholeWord());
		assertEquals("FAST", rf.getfSearchPerformance());
		assertEquals(Arrays.asList("alpha", "beta gamma"), rf.getfSearchLastSearchStrings().getList());
		assertEquals(1, rf.getfSearchLastLoadedFiles().getList().size());
		assertEquals("a.txt", rf.getfSearchLastLoadedFiles().getList().get(0).mFile.getName());
		assertEquals(1, rf.getfSearchLastFileFilters().getList().size());
		assertEquals(Arrays.asList("*.java", "*.xml"), Arrays.asList(rf.getfSearchLastFileFilters().getList().get(0)));
		assertFalse(rf.isFlagAutosave());
		assertTrue(rf.isFlagOverwrite());
		assertEquals(f.getLastSrcFolderPath(), rf.getLastSrcFolderPath());
		assertEquals(f.getLastDstFolderPath(), rf.getLastDstFolderPath());
		assertEquals(120000, rf.getAutosaveInterval());

		GuiOption rg = r.getGuiOpt();
		assertEquals("some.Laf", rg.getPreferredStyle());
		assertEquals("Sunrise", rg.getPreferredThemeName());
		assertEquals(15, rg.getMenuFontSize());
		assertEquals(17, rg.getLogFontSize());
		assertEquals(3, rg.getNimbusSize());
		assertTrue(rg.isDarkGuiNimbus());
		assertEquals(UiSizePreset.CUSTOM, rg.getUiSizePreset());
		assertEquals("Consolas", rg.getUiFontFamily());
		assertEquals(111, rg.getUiGeneralFontScale());
		assertEquals(122, rg.getUiMenuFontScale());
		assertEquals(133, rg.getUiToolbarFontScale());
		assertEquals(144, rg.getUiComponentScale());

		GenericOption ro = r.getGenOpt();
		assertEquals("tester", ro.getUserName());
		assertEquals("ISO-8859-1", ro.getCharset());
		// the saved position is moved by 10 px at every load, so that a new window does not cover the old one
		assertEquals(Integer.valueOf(310), ro.getLastKnownPositionX());
		assertEquals(Integer.valueOf(210), ro.getLastKnownPositionY());
		assertEquals(0.25f, ro.getGeneralVolume(), 0.0001f);
		assertEquals(java.awt.Panel.class, ro.getLastPanelOpened());
		assertFalse(ro.isAutomaticUpdateOn());
		assertFalse(ro.isAutomaticPluginUpdateOn());
		assertTrue(ro.isExceptionAdvicesEnabled());
		assertFalse(ro.isTxtEditorFileMonitorActive());
		assertFalse(ro.isFirstAccess());
		assertEquals(new java.util.Locale("fr"), ro.getLocale());
		assertEquals("it", ro.getLanguage());

		SteganOption rs = r.getSteganOpt();
		assertFalse(rs.isUltraSecure());
		assertFalse(rs.isFlagSteg());
		assertEquals(640, rs.getDefaultWidth());
		assertEquals(480, rs.getDefaultHeight());
		assertEquals(Arrays.asList("one.png", "two.png", "three.png"), rs.getImgRecentPath());

		ImgConverterOption rc = r.getImageConvertionOpt();
		assertFalse(rc.isSameSize());
		assertFalse(rc.isSameExtension());
		assertEquals(42, rc.getQuality());
		assertEquals(c.getAlgorithm(), rc.getAlgorithm());

		assertTextEditorRestored(r.getTextEditorOpt());
		assertNotNull(r.toStringa());
		assertTrue(r.toStringa().contains("tester"));
	}

	private void fillTextEditor(TextEditorOption t) {
		t.setEol(EOL.values()[EOL.values().length - 1]);
		t.setFirstTimeOpened(false);
		t.setFont(new Font("Monospaced", Font.BOLD, 21));
		t.setBackCol(new Color(1, 2, 3));
		t.setForeCol(new Color(200, 100, 50));
		t.setFilePanelBackCol(new Color(9, 8, 7));
		t.setSelectedAtStart("start.txt");
		t.setDefaultWorkspacePath(tmp.getRoot().getAbsolutePath());
		t.setLastEscapeLanguage("JAVA");
		t.setLastSelectedTemplate("tpl");
		t.setLastFileOpenedClipboard("clip.txt");
		t.setClipboardSyncStyle(true);
		t.setPrefix_hash(true);
		t.setUppercase_hash(false);
		t.setAutosaveTempFiles(false);
		t.setInlinedTabs(false);
		t.setMargin(33);
		t.setVoiceRate(7);
		t.setTabSize(8);
		t.setDividerBottomPosRatio(0.55D);
		t.setDividerFileTreePosX(111);
		t.setDividerRightPanePosX(222);
		t.setDividerRightPanePosXClipboard(333);
		t.setDefaultAutoSyntaxSync(false);
		t.setPreferredSyntaxIfNotSync("JAVA");
		t.setWorkspaceViewSelected(true);
		t.setFooterVisible(false);
		t.setHeaderVisible(false);
		t.setSubHeaderVisible(false);
		t.setTreeLocked(true);
		t.setTabLineVisible(true);
		t.setFileTreeActive(false);
		t.setMapViewActive(true);
		t.setBottomViewActive(true);
		t.setClipboardViewActive(true);
		t.setBreaklineActive(true);
		t.setAntialiasedTxtArea(false);
		t.setCodeFoldingEnabled(false);
		t.setAutoIndentEnabled(false);
		t.setMarkOccurrences(false);
		t.setCloseMarkupTags(false);
		t.setCloseCurlyBraces(false);
		t.setBracketMatchingEnabled(false);
		t.setAnimateBracketMatching(false);
		t.setPaintMatchedBracketPair(false);
		t.setWhiteSpaceVisible(true);
		t.setClearWhitespaceLinesEnabled(false);
		t.setMarkEndOfLine(true);
		t.setFadeCurrentLineHighlight(false);
		t.setUseFocusableTips(false);
		t.setRoundedSelectionEdges(false);
		t.setPaintMarkOccurrencesBorder(false);
		t.setHighlightSecondaryLanguages(false);
		t.setFractionalFontMetricsEnabled(false);
		t.setHyperLinksEnabled(true);
		t.setInterline(5);
		t.setShellInteractive(true);
		t.setMaxFileSizeMb(250);
		t.setTabSpacesEmulated(true);
		t.setShowBracketsPopup(true);
		t.setSpellChecker(true);
		t.setPrefDictspellChecker("dict/it.dic");
		t.setSelectedTheme("themes/light.xml");
		t.viewerAssociations = "png=imageViewer;md=markdown";
		t.treeFilterRules = "rule1|rule2";

		t.getLastOpenedFiles().reset();
		t.getLastOpenedFiles().addLast(new FileNamed(new File(tmp.getRoot(), "open1.txt")));
		t.getLastOpenedFiles().addLast(new FileNamed(new File(tmp.getRoot(), "open2.txt")));
		t.getLastSearchTerms().reset();
		t.getLastSearchTerms().addLast("find me");
		t.getLastReplaceTerms().reset();
		t.getLastReplaceTerms().addLast("replace me");
		t.getLastTagsUsed().reset();
		t.getLastTagsUsed().addLast("div");
		t.getLastTagsUsed().addLast("span");
	}

	private void assertTextEditorRestored(TextEditorOption t) {
		assertEquals(EOL.values()[EOL.values().length - 1], t.getEol());
		assertFalse(t.isFirstTimeOpened());
		assertEquals("Monospaced", t.getFont().getFamily());
		assertEquals(21, t.getFont().getSize());
		assertTrue(t.getFont().isBold());
		assertEquals(new Color(1, 2, 3), t.getBackCol());
		assertEquals(new Color(200, 100, 50), t.getForeCol());
		assertEquals(new Color(9, 8, 7), t.getFilePanelBackCol());
		assertEquals("start.txt", t.getSelectedAtStart());
		assertEquals(tmp.getRoot().getAbsolutePath(), t.getDefaultWorkspacePath());
		assertEquals("JAVA", t.getLastEscapeLanguage());
		assertEquals("tpl", t.getLastSelectedTemplate());
		assertEquals("clip.txt", t.getLastFileOpenedClipboard());
		assertTrue(t.isClipboardSyncStyle());
		assertTrue(t.isPrefix_hash());
		assertFalse(t.isUppercase_hash());
		assertFalse(t.isAutosaveTempFiles());
		assertFalse(t.isInlinedTabs());
		assertEquals(33, t.getMargin());
		assertEquals(7, t.getVoiceRate());
		assertEquals(8, t.getTabSize());
		assertEquals(Double.valueOf(0.55D), t.getDividerBottomPosRatio());
		assertEquals(111, t.getDividerFileTreePosX());
		assertEquals(222, t.getDividerRightPanePosX());
		assertEquals(333, t.getDividerRightPanePosXClipboard());
		assertFalse(t.isDefaultAutoSyntaxSync());
		assertEquals("JAVA", t.getPreferredSyntaxIfNotSync());
		assertTrue(t.isWorkspaceViewSelected());
		assertFalse(t.isFooterVisible());
		assertFalse(t.isHeaderVisible());
		assertFalse(t.isSubHeaderVisible());
		assertTrue(t.isTreeLocked());
		assertTrue(t.isTabLineVisible());
		assertFalse(t.isFileTreeActive());
		assertTrue(t.isMapViewActive());
		assertTrue(t.isBottomViewActive());
		assertTrue(t.isClipboardViewActive());
		assertTrue(t.isBreaklineActive());
		assertFalse(t.isAntialiasedTxtArea());
		assertFalse(t.isCodeFoldingEnabled());
		assertFalse(t.isAutoIndentEnabled());
		assertFalse(t.isMarkOccurrences());
		assertFalse(t.isCloseMarkupTags());
		assertFalse(t.isCloseCurlyBraces());
		assertFalse(t.isBracketMatchingEnabled());
		assertFalse(t.isAnimateBracketMatching());
		assertFalse(t.isPaintMatchedBracketPair());
		assertTrue(t.isWhiteSpaceVisible());
		assertFalse(t.isClearWhitespaceLinesEnabled());
		assertTrue(t.isMarkEndOfLine());
		assertFalse(t.isFadeCurrentLineHighlight());
		assertFalse(t.isUseFocusableTips());
		assertFalse(t.isRoundedSelectionEdges());
		assertFalse(t.isPaintMarkOccurrencesBorder());
		assertFalse(t.isHighlightSecondaryLanguages());
		assertFalse(t.isFractionalFontMetricsEnabled());
		assertTrue(t.isHyperLinksEnabled());
		assertEquals(5, t.getInterline());
		assertTrue(t.isShellInteractive());
		assertEquals(250, t.getMaxFileSizeMb());
		assertEquals(250 * 1024, GeneralConfig.MAX_FILE_SIZE_TXT_EDITOR_KB);
		assertTrue(t.isTabSpacesEmulated());
		assertTrue(t.isShowBracketsPopup());
		assertTrue(t.isSpellChecker());
		assertEquals("dict/it.dic", t.getPrefDictspellChecker());
		assertEquals("themes/light.xml", t.getSelectedTheme());
		assertEquals("png=imageViewer;md=markdown", t.viewerAssociations);
		assertEquals("rule1|rule2", t.treeFilterRules);

		assertEquals(2, t.getLastOpenedFiles().getList().size());
		assertEquals("open2.txt", t.getLastOpenedFiles().getList().get(1).mFile.getName());
		assertEquals(Arrays.asList("find me"), t.getLastSearchTerms().getList());
		assertEquals(Arrays.asList("replace me"), t.getLastReplaceTerms().getList());
		assertEquals(Arrays.asList("div", "span"), t.getLastTagsUsed().getList());
	}

	/** Open files and carets are saved at closing by the application with the parent helpers: here the same format is read back */
	@Test
	public void openFilesCaretsAndTabsAreReadFromTheFormatOfTheParentHelpers() throws IOException {
		INItializer w = open();
		w.writeCurrentConfigIniFile();
		Map<String, String> carets = new HashMap<>();
		carets.put("fileA", "12");
		carets.put("fileB", "34");
		Wini wini = new Wini(iniFile);
		String sec = INItializer.TEXT_EDITOR_OPT;
		wini.put(sec, "openedFileListCaretPosition", INItializer.mapToString(carets));
		wini.put(sec, "loadedFilesAtStart", w.listToString(Arrays.asList("a.txt", "b.txt")));
		wini.put(sec, "pinnedFilesAtStart", "a.txt");
		wini.put(sec, "secondPanelFilesAtStart", "c.txt,d.txt");
		wini.put(sec, "viewerTabsAtStart", "img|x.png");
		wini.put(sec, "loadedTempFilesAtStart", "temp1");
		wini.store();

		INItializer r = open();
		r.readConfigIniFile();
		TextEditorOption t = r.getTextEditorOpt();
		assertEquals(carets, t.getOpenedFileListCaretPosition());
		assertEquals(Arrays.asList("a.txt", "b.txt"), t.getOpenedFileList());
		assertEquals(Arrays.asList("a.txt"), t.getPinnedFileList());
		assertEquals(Arrays.asList("c.txt", "d.txt"), t.getSecondPanelFileList());
		assertEquals(Arrays.asList("img|x.png"), t.getViewerTabList());
		assertEquals(Arrays.asList("temp1"), t.getOpenedTempFileList());
	}

	// ------------------------------------------------------------------ corrupted or out of range values

	@Test
	public void corruptedNumbersFallBackToDefaults() throws IOException {
		INItializer w = open();
		w.writeCurrentConfigIniFile();
		Wini wini = new Wini(iniFile);
		wini.put(INItializer.FILE_OPT, "autosaveInterval", "abc");
		wini.put(INItializer.GUI_OPT, "menuFontSize", "big");
		wini.put(INItializer.GUI_OPT, "nimbusSize", "x");
		wini.put(INItializer.STEGA_OPT, "defaultWidth", "wide");
		wini.put(INItializer.IMAGE_CONVERTER_OPT, "quality", "high");
		wini.put(INItializer.GEN_OPT, "generalVolume", "loud");
		wini.put(INItializer.TEXT_EDITOR_OPT, "margin", "m");
		wini.put(INItializer.TEXT_EDITOR_OPT, "voiceRate", "fast");
		wini.put(INItializer.TEXT_EDITOR_OPT, "tabSize", "tab");
		wini.put(INItializer.TEXT_EDITOR_OPT, "interline", "z");
		wini.put(INItializer.TEXT_EDITOR_OPT, "dividerBottomPosRatio", "half");
		wini.put(INItializer.TEXT_EDITOR_OPT, "dividerFileTreePosX", "left");
		wini.store();

		INItializer r = open();
		r.readConfigIniFile();

		assertEquals(FileOptions.DEFAULT_AUTOSAVE_INTERVAL, r.getFileOpt().getAutosaveInterval());
		assertEquals(GuiOption.DEFAULT_MENU_FONT_SIZE, r.getGuiOpt().getMenuFontSize());
		assertEquals(GuiOption.DEFAULT_LOG_FONT_SIZE, r.getGuiOpt().getLogFontSize());
		assertEquals(GuiOption.DEFAULT_NIMBUS_SIZE, r.getGuiOpt().getNimbusSize());
		assertEquals(SteganOption.DEFAULT_WIDTH, r.getSteganOpt().getDefaultWidth());
		assertEquals(SteganOption.DEFAULT_HEIGHT, r.getSteganOpt().getDefaultHeight());
		assertEquals(ImgConverterOption.DEFAULT_QUALITY, r.getImageConvertionOpt().getQuality());
		assertEquals(GenericOption.DEFAULT_VOLUME, r.getGenOpt().getGeneralVolume(), 0.0001f);
		TextEditorOption t = r.getTextEditorOpt();
		assertEquals(TextEditorOption.DEFAULT_MARGIN, t.getMargin());
		assertEquals(TextEditorOption.DEFAULT_VOICE_RATE, t.getVoiceRate());
		assertEquals(TextEditorOption.DEFAULT_TAB_SIZE, t.getTabSize());
		assertEquals(TextEditorOption.DEF_interline, t.getInterline());
		assertEquals(TextEditorOption.DEF_dividerBottomPosRatio, t.getDividerBottomPosRatio());
		assertEquals(TextEditorOption.DEFAULT_DIVIDER_POS_X, t.getDividerFileTreePosX());
	}

	@Test
	public void unknownEnumsAndClassesFallBackToDefaults() throws IOException {
		INItializer w = open();
		w.writeCurrentConfigIniFile();
		Wini wini = new Wini(iniFile);
		wini.put(INItializer.AES_OPT, "securityLevel", "NOPE");
		wini.put(INItializer.IMAGE_CONVERTER_OPT, "algorithm", "NOPE");
		wini.put(INItializer.GEN_OPT, "lastPanelOpened", "no.such.Panel");
		wini.put(INItializer.GUI_OPT, "uiSizePreset", "GIGANTIC");
		wini.put(INItializer.GEN_OPT, "lastKnownPositionX", "-50");
		wini.put(INItializer.GEN_OPT, "lastKnownPositionY", "abc");
		wini.store();

		INItializer r = open();
		r.readConfigIniFile();

		assertEquals(AesOption.DEF_securityLevel, r.getAesOpt().getSecurityLevel());
		assertEquals(ImgConverterOption.DEFAULT_ALGORITHM, r.getImageConvertionOpt().getAlgorithm());
		assertEquals(GenericOption.DEFAULT_PANEL_CLASS_AT_START, r.getGenOpt().getLastPanelOpened());
		assertEquals(UiSizePreset.MEDIUM, r.getGuiOpt().getUiSizePreset());
		assertEquals(GenericOption.DEFAULT_POS_X, r.getGenOpt().getLastKnownPositionX());
		assertEquals(GenericOption.DEFAULT_POS_Y, r.getGenOpt().getLastKnownPositionY());
	}

	@Test
	public void autosaveIntervalIsRaisedToTheMinimum() throws IOException {
		INItializer r = openWithDefaultsPlus(INItializer.FILE_OPT, "autosaveInterval", "1000");
		assertEquals(FileOptions.DEFAULT_MIN_AUTOSAVE_INTERVAL, r.getFileOpt().getAutosaveInterval());

		r = openWithDefaultsPlus(INItializer.FILE_OPT, "autosaveInterval", "90000");
		assertEquals(90000, r.getFileOpt().getAutosaveInterval());
	}

	@Test
	public void customPresetWithBrokenScalesGoesBackToDefaultPreset() throws IOException {
		INItializer w = open();
		w.getGuiOpt().applyUiSizePreset(UiSizePreset.CUSTOM);
		w.writeCurrentConfigIniFile();
		Wini wini = new Wini(iniFile);
		wini.put(INItializer.GUI_OPT, "uiMenuFontScale", "oops");
		wini.store();

		INItializer r = open();
		r.readConfigIniFile();
		assertEquals(GuiOption.DEFAULT_UI_SIZE_PRESET, r.getGuiOpt().getUiSizePreset());
		assertEquals(100, r.getGuiOpt().getUiMenuFontScale());
	}

	@Test
	public void namedPresetIgnoresStoredScales() throws IOException {
		INItializer w = open();
		w.getGuiOpt().applyUiSizePreset(UiSizePreset.BIG);
		w.writeCurrentConfigIniFile();
		Wini wini = new Wini(iniFile);
		wini.put(INItializer.GUI_OPT, "uiGeneralFontScale", "999");
		wini.store();

		INItializer r = open();
		r.readConfigIniFile();
		assertEquals(UiSizePreset.BIG, r.getGuiOpt().getUiSizePreset());
		assertEquals(UiSizePreset.BIG.generalFontScale, r.getGuiOpt().getUiGeneralFontScale());
		assertEquals(UiSizePreset.BIG.componentScale, r.getGuiOpt().getUiComponentScale());
	}

	@Test
	public void emptyOptionalTextsAreReplacedByDefaults() throws IOException {
		INItializer w = open();
		w.writeCurrentConfigIniFile();
		Wini wini = new Wini(iniFile);
		wini.put(INItializer.FILE_OPT, "lastDstFolder", "");
		wini.put(INItializer.FILE_OPT, "lastSrcFolder", "");
		wini.put(INItializer.GUI_OPT, "preferredStyle", "");
		wini.put(INItializer.GEN_OPT, "charset", "");
		wini.put(INItializer.TEXT_EDITOR_OPT, "font", "");
		wini.put(INItializer.TEXT_EDITOR_OPT, "backgroundCol", "");
		wini.put(INItializer.TEXT_EDITOR_OPT, "preferredTheme", "  ");
		wini.put(INItializer.TEXT_EDITOR_OPT, "defaultWorspaceLocation", "");
		wini.store();

		INItializer r = open();
		r.readConfigIniFile();
		assertEquals(FileOptions.DEFAULT_LAST_DEST_FOLDER_PATH, r.getFileOpt().getLastDstFolderPath());
		assertEquals(FileOptions.DEFAULT_LAST_SRC_FOLDER_PATH, r.getFileOpt().getLastSrcFolderPath());
		assertEquals(GuiOption.DEFAULT_GUI_STYLE, r.getGuiOpt().getPreferredStyle());
		assertEquals(GenericOption.DEFAULT_CHARSET, r.getGenOpt().getCharset());
		assertEquals(TextEditorOption.DEFAULT_FONT, r.getTextEditorOpt().getFont());
		assertEquals(TextEditorOption.DEFAULT_BACKGROUND_COL, r.getTextEditorOpt().getBackCol());
		assertEquals(TextEditorOption.DEFAULT_SELECTED_THEME, r.getTextEditorOpt().getSelectedTheme());
		assertEquals(INItializer.USER_PERSONAL_DIR, r.getTextEditorOpt().getDefaultWorkspacePath());
	}

	@Test
	public void writingBlanksRestoresDefaultsInTheFile() throws IOException {
		INItializer w = open();
		w.getFileOpt().setLastDstFolderPath("");
		w.getFileOpt().setLastSrcFolderPath("");
		w.getGuiOpt().setPreferredStyle("");
		w.getGenOpt().setUserName("");
		w.writeCurrentConfigIniFile();

		assertEquals(FileOptions.DEFAULT_LAST_DEST_FOLDER_PATH, w.getFileOpt().getLastDstFolderPath());
		assertEquals(GuiOption.DEFAULT_GUI_STYLE, w.getGuiOpt().getPreferredStyle());
		assertEquals(GenericOption.DEFAULT_USERNAME, w.getGenOpt().getUserName());
		Wini check = new Wini(iniFile);
		assertEquals(GuiOption.DEFAULT_GUI_STYLE, check.get(INItializer.GUI_OPT, "preferredStyle"));
	}

	@Test
	public void emptyStegoPathListIsStoredAsDefaultPath() throws IOException {
		INItializer w = open();
		w.writeCurrentConfigIniFile();
		Wini check = new Wini(iniFile);
		assertEquals(SteganOption.DEFAULT_IMG_RECENT_PATH, check.get(INItializer.STEGA_OPT, "imgKeyRecentPath"));

		INItializer r = open();
		r.readConfigIniFile();
		assertEquals(Arrays.asList(SteganOption.DEFAULT_IMG_RECENT_PATH), r.getSteganOpt().getImgRecentPath());
	}

	@Test
	public void singleLoadersOnlyTouchTheirSection() throws IOException {
		INItializer w = open();
		w.writeCurrentConfigIniFile();
		Wini wini = new Wini(iniFile);
		wini.put(INItializer.IMAGE_CONVERTER_OPT, "quality", "55");
		wini.put(INItializer.GEN_OPT, "userName", "other");
		wini.store();

		INItializer r = open();
		r.loadImageConverterOptions();
		assertEquals(55, r.getImageConvertionOpt().getQuality());
		assertEquals(GenericOption.DEFAULT_USERNAME, r.getGenOpt().getUserName());
		r.loadGeneralOptions();
		assertEquals("other", r.getGenOpt().getUserName());
	}

	@Test
	public void addFileToTopMovesAnExistingFileToTheEndAndPersistsIt() throws IOException {
		File one = tmp.newFile("one.txt");
		File two = tmp.newFile("two.txt");
		INItializer init = open();
		TextEditorOption t = new TextEditorOption(init);

		t.addFileToTopLimitedList(one, t.getLastOpenedFiles(), "lastOpenedFiles");
		t.addFileToTopLimitedList(two, t.getLastOpenedFiles(), "lastOpenedFiles");
		t.addFileToTopLimitedList(one, t.getLastOpenedFiles(), "lastOpenedFiles");
		// missing files and null arguments are ignored
		t.addFileToTopLimitedList(new File(tmp.getRoot(), "missing.txt"), t.getLastOpenedFiles(), "lastOpenedFiles");
		t.addFileToTopLimitedList(null, t.getLastOpenedFiles(), "lastOpenedFiles");
		t.addFileToTopLimitedList(one, null, "lastOpenedFiles");

		assertEquals(2, t.getLastOpenedFiles().getList().size());
		assertEquals(one, t.getLastOpenedFiles().getList().get(1).mFile);
		String stored = new Wini(iniFile).get(INItializer.TEXT_EDITOR_OPT, "lastOpenedFiles");
		assertEquals(two.getAbsolutePath() + "," + one.getAbsolutePath(), stored);
	}
}
