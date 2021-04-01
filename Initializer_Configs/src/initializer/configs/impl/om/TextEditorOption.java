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
package initializer.configs.impl.om;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import initializer.configs.impl.INItializer;
import resources.GeneralConfig;
import various.common.light.files.om.FileNamed;
import various.common.light.om.LimitedConcurrentList;
import various.common.light.utility.string.StringWorker.EOL;

public class TextEditorOption {

	INItializer parentConfig;

	// DEFAULT VALUES (usfeul to keep valid object during iniLoad, in case of null read or exception)
	public static final boolean DEF_firstTimeOpened = true;
	public static final int DEFAULT_MARGIN = 18;
	public static final int DEFAULT_VOICE_RATE = 3;
	public static final boolean DEFAULT_TAB_INLINE = true;
	public static final Font DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, 16);
	public static final Color DEFAULT_BACKGROUND_COL = new Color(250,250,250);
	public static final Color DEFAULT_FILE_PANEL_BACKGROUND_COL = new Color(41, 49, 52);
	public static final Color DEFAULT_FOREGROUND_COL = Color.BLACK;
	public static final String DEFAULT_SELECTED_FILE_AT_START = "";
	public static final String DEF_LastEscapeLanguage = "HTML_4";
	public static final EOL DEF_eol = EOL.defaultEol;

	//TEXT AREA
	public static final String DEFAULT_SELECTED_THEME = GeneralConfig.THEMES_FOLDER_DEFAULT + "dark.xml";
	public static final boolean DEF_headerVisible = true;
	public static final boolean DEF_subHeaderVisible = true;
	public static final boolean DEF_footerVisible = true;
	public static final boolean DEFAULT_FTREE_ACTIVE = true;
	public static final boolean DEF_bottomViewActive = false;
	public static final boolean DEF_mapViewActive = false;
	public static final boolean DEF_clipboardViewActive = false;
	public static final boolean DEF_clipboardSyncStyle = false;
	public static final int DEFAULT_DIVIDER_POS_X = 230;
	public static final Double DEF_dividerBottomPosRatio = 0.8D;
	public static final int DEF_dividerRightPanePosX = 60;
	public static final int DEF_dividerRightPanePosXClipboard = 150;
	public static final String DEF_lastFileOpenedClipboard = GeneralConfig.CLIPBOARD_PANEL_DEFAULT;
	public static final String DEF_lastSelectedTemplate = "";
	public static final int DEFAULT_TAB_SIZE = 2;
	public static final boolean DEFAULT_TAB_LINE_VISIBLE = false;
	public static final boolean DEFAULT_BREAK_LINE = false;
	public static final boolean DEFAULT_TREE_LOCKED = false;
	public static final boolean DEFAULT_WORKSPACE_VIEW_SELECTED = false;
	public static final boolean DFAULT_AUTO_SYNTAX_SYN_FLAG = true;
	public static final String DEF_preferredSyntaxIfNotSync = "TXT";
	public static final boolean DEF_uppercase_hash = true;
	public static final boolean DEF_prefix_hash = false;
	public static final boolean DEF_autosaveTempFiles = true;
	public static LimitedConcurrentList<FileNamed> 	DEF_lastOpenedFiles = new LimitedConcurrentList<FileNamed>(GeneralConfig.MAX_LAST_OPENED_LIST_SIZE);
	public static LimitedConcurrentList<FileNamed> 	DEF_lastSelectedWorkspaces = new LimitedConcurrentList<FileNamed>(GeneralConfig.MAX_WORKSPACE_LIST_SIZE);
	public static LimitedConcurrentList<FileNamed> 	DEF_lastOpenedShellFiles = new LimitedConcurrentList<FileNamed>(GeneralConfig.MAX_LAST_OPENED_LIST_SIZE);
	public static LimitedConcurrentList<String>    DEF_lastTagsUsed = new LimitedConcurrentList<String>(60);
	public static LimitedConcurrentList<String>    DEF_lastReplaceTerms = new LimitedConcurrentList<String>(GeneralConfig.MAX_REPLACE_DIAL_LIST_SIZE);
	public static LimitedConcurrentList<String>    DEF_lastSearchTerms = new LimitedConcurrentList<String>(GeneralConfig.MAX_SEARCH_DIAL_LIST_SIZE);

	// TEXT AREA DETAIL
	public static final boolean DEF_codeFoldingEnabled = true;
	public static final boolean DEF_autoIndentEnabled = true;
	public static final boolean DEF_markOccurrences = true;
	public static final boolean DEF_closeMarkupTags = true;
	public static final boolean DEF_closeCurlyBraces = true;
	public static final boolean DEF_antialiasedTxtArea = true;
	public static final boolean DEF_bracketMatchingEnabled = true;
	public static final boolean DEF_animateBracketMatching = true;
	public static final boolean DEF_fadeCurrentLineHighlight = true;
	public static final boolean DEF_paintMatchedBracketPair = true;
	public static final boolean DEF_whiteSpaceVisible = false;
	public static final boolean DEF_useFocusableTips = true;
	public static final boolean DEF_roundedSelectionEdges = true;
	public static final boolean DEF_paintMarkOccurrencesBorder = true;
	public static final boolean DEF_clearWhitespaceLinesEnabled = true;
	public static final boolean DEF_highlightSecondaryLanguages = true;
	public static final boolean DEF_fractionalFontMetricsEnabled = true;
	public static final boolean DEF_hyperLinksEnabled = false;
	public static final boolean DEF_tabsSpacesEmulated = false;
	public static final int DEF_interline = 2;
	public static final boolean DEF_markEndOfLine = false;
	public static final boolean DEF_showBracketsPopup = false;
	public static final boolean DEF_spellChecker = false;
	public static final String DEF_prefDictspellChecker = GeneralConfig.SPELLCHECKER_DICT_FOLDER + GeneralConfig.SPELLCHECKER_DEFAULT_DICT;

	// FIELDS
	public EOL eol;
	public boolean firstTimeOpened;
	public boolean headerVisible;
	public boolean subHeaderVisible;
	public boolean footerVisible;
	public int margin;
	public int voiceRate;
	public boolean inlinedTabs;
	public Font font;
	public Color filePanelBackCol;
	public Color backCol;
	public Color foreCol;
	public ArrayList<String> openedTempFileList;
	public ArrayList<String> openedFileList;
	public Map<String, String> openedFileListCaretPosition;
	public String selectedAtStart;
	public String lastEscapeLanguage;
	public LimitedConcurrentList<FileNamed> lastOpenedFiles;
	public LimitedConcurrentList<FileNamed> lastOpenedShellFiles;
	public LimitedConcurrentList<FileNamed> lastSelectedWorkspaces;
	public LimitedConcurrentList<String> lastReplaceTerms;
	public LimitedConcurrentList<String> lastSearchTerms;
	public LimitedConcurrentList<String> lastTagsUsed;
	String[] lastTagsInit = new String[] {"a", "span", "p", "div", "br", "image", "h1", "h2", "h3", "h4", "h5"};

	// TEXT AREA
	public String defaultWorkspacePath;
	public String selectedTheme;
	public Double dividerBottomPosRatio;
	public int dividerFileTreePosX;
	public int dividerRightPanePosX;
	public int dividerRightPanePosXClipboard;
	public String lastFileOpenedClipboard;
	public String lastSelectedTemplate;
	public int tabSize = 2;
	public boolean tabLineVisible;
	public boolean fileTreeActive;
	public boolean bottomViewActive;
	public boolean mapViewActive;
	public boolean clipboardViewActive;
	public boolean clipboardSyncStyle;
	public boolean treeLocked;
	public boolean workspaceViewSelected;
	public boolean defaultAutoSyntaxSync;
	public String preferredSyntaxIfNotSync;
	public boolean breaklineActive;
	public boolean prefix_hash;
	public boolean uppercase_hash;
	public boolean autosaveTempFiles;

	public boolean antialiasedTxtArea;
	public boolean bracketMatchingEnabled;
	public boolean clearWhitespaceLinesEnabled;
	public boolean animateBracketMatching;
	public boolean paintMatchedBracketPair;
	public boolean whiteSpaceVisible;
	public boolean autoIndentEnabled;
	public boolean closeCurlyBraces;
	public boolean closeMarkupTags;
	public boolean codeFoldingEnabled;
	public boolean fadeCurrentLineHighlight;
	public boolean fractionalFontMetricsEnabled;
	public boolean highlightSecondaryLanguages;
	public boolean hyperLinksEnabled;
	public boolean markOccurrences;
	public boolean paintMarkOccurrencesBorder;
	public boolean roundedSelectionEdges;
	public boolean useFocusableTips;
	public int interline;
	public boolean tabSpacesEmulated;
	public boolean markEndOfLine;
	public boolean showBracketsPopup;
	public boolean spellChecker;
	public String prefDictspellChecker;


	// CONSTRUCTOR
	public TextEditorOption() {
		this.parentConfig = new INItializer();
		init();
	}

	public void init() {
		eol = DEF_eol;

		firstTimeOpened = DEF_firstTimeOpened;
		uppercase_hash = DEF_uppercase_hash;
		prefix_hash = DEF_prefix_hash;

		headerVisible = DEF_headerVisible;
		subHeaderVisible = DEF_subHeaderVisible;
		footerVisible = DEF_footerVisible;

		openedFileList = new ArrayList<String>();
		openedTempFileList = new ArrayList<String>();
		openedFileListCaretPosition = new HashMap<>();
		lastOpenedFiles = DEF_lastOpenedFiles;
		lastOpenedShellFiles = DEF_lastOpenedShellFiles;
		lastSelectedWorkspaces = DEF_lastSelectedWorkspaces;
		lastReplaceTerms = DEF_lastReplaceTerms;
		lastSearchTerms = DEF_lastSearchTerms;
		lastTagsUsed = DEF_lastTagsUsed;
		margin = DEFAULT_MARGIN;
		voiceRate = DEFAULT_VOICE_RATE;
		inlinedTabs = DEFAULT_TAB_INLINE;
		font = DEFAULT_FONT;
		filePanelBackCol = DEFAULT_FILE_PANEL_BACKGROUND_COL;
		backCol = DEFAULT_BACKGROUND_COL;
		foreCol = DEFAULT_FOREGROUND_COL;
		selectedAtStart = DEFAULT_SELECTED_FILE_AT_START;
		lastEscapeLanguage = DEF_LastEscapeLanguage;
		defaultWorkspacePath = GeneralConfig.USER_DESKTOP;
		selectedTheme = DEFAULT_SELECTED_THEME;
		dividerBottomPosRatio = DEF_dividerBottomPosRatio;
		dividerFileTreePosX = DEFAULT_DIVIDER_POS_X;
		dividerRightPanePosX = DEF_dividerRightPanePosX;
		dividerRightPanePosXClipboard = DEF_dividerRightPanePosXClipboard;
		lastFileOpenedClipboard = DEF_lastFileOpenedClipboard;
		lastSelectedTemplate = DEF_lastSelectedTemplate;
		tabSize = DEFAULT_TAB_SIZE;

		fileTreeActive = DEFAULT_FTREE_ACTIVE;
		bottomViewActive = DEF_bottomViewActive;
		clipboardViewActive = DEF_clipboardViewActive;
		clipboardSyncStyle = DEF_clipboardSyncStyle;
		mapViewActive = DEF_mapViewActive;
		treeLocked = DEFAULT_TREE_LOCKED;
		workspaceViewSelected = DEFAULT_WORKSPACE_VIEW_SELECTED;
		defaultAutoSyntaxSync = DFAULT_AUTO_SYNTAX_SYN_FLAG;
		preferredSyntaxIfNotSync = DEF_preferredSyntaxIfNotSync;

		tabLineVisible = DEFAULT_TAB_LINE_VISIBLE;
		breaklineActive = DEFAULT_BREAK_LINE;
		antialiasedTxtArea = DEF_antialiasedTxtArea;
		codeFoldingEnabled = DEF_codeFoldingEnabled;
		autoIndentEnabled = DEF_autoIndentEnabled;
		markOccurrences = DEF_markOccurrences;
		closeMarkupTags = DEF_closeMarkupTags;
		closeCurlyBraces = DEF_closeCurlyBraces;
		bracketMatchingEnabled = DEF_bracketMatchingEnabled;
		animateBracketMatching = DEF_animateBracketMatching;
		paintMatchedBracketPair = DEF_paintMatchedBracketPair;
		whiteSpaceVisible = DEF_whiteSpaceVisible;
		fadeCurrentLineHighlight = DEF_fadeCurrentLineHighlight;
		useFocusableTips = DEF_useFocusableTips;
		roundedSelectionEdges = DEF_roundedSelectionEdges;
		paintMarkOccurrencesBorder = DEF_paintMarkOccurrencesBorder;
		clearWhitespaceLinesEnabled = DEF_clearWhitespaceLinesEnabled;
		highlightSecondaryLanguages = DEF_highlightSecondaryLanguages;
		fractionalFontMetricsEnabled = DEF_fractionalFontMetricsEnabled;
		hyperLinksEnabled = DEF_fractionalFontMetricsEnabled;
		interline = DEF_interline;
		tabSpacesEmulated = DEF_tabsSpacesEmulated;
		markEndOfLine = DEF_markEndOfLine;
		showBracketsPopup = DEF_showBracketsPopup;
		spellChecker = DEF_spellChecker;
		prefDictspellChecker = DEF_prefDictspellChecker;
	}

	public TextEditorOption(INItializer parentConfig) {
		init();
		DEF_lastOpenedFiles = new LimitedConcurrentList<FileNamed>(GeneralConfig.MAX_LAST_OPENED_LIST_SIZE);
		DEF_lastSelectedWorkspaces = new LimitedConcurrentList<FileNamed>(GeneralConfig.MAX_WORKSPACE_LIST_SIZE);
		this.parentConfig = parentConfig;
	}


	/**
	 * If already present move to top, else if not present add it as last
	 */
	public synchronized void addFileToTopLimitedList(File toAdd, LimitedConcurrentList<FileNamed> targetList, String varToSave) {

		if(toAdd == null || targetList == null || !toAdd.exists()) {return;}

		// FIRST REMOVE ALL REFERENCES
		synchronized (targetList) {
			Iterator<FileNamed> iter = targetList.getList().iterator();
			List<FileNamed> toRemove = new ArrayList<FileNamed>();
			while (iter.hasNext()) {
				FileNamed elem = iter.next();
				File file = elem.mFile;
				if (file.equals(toAdd)) {
					toRemove.add(elem);
				}
			}

			if (toRemove.size() != 0) {
				// remove and re-add on top
				targetList.getList().removeAll(toRemove);
			}
			targetList.addLast(new FileNamed(toAdd));
			parentConfig.writeSingleValue(INItializer.TEXT_EDITOR_OPT, varToSave, targetList.toCharSeparatedString(","));
		}
	}

	// GETTERS AND SETTERS

	public boolean isTabLineVisible() {
		return tabLineVisible;
	}

	public LimitedConcurrentList<String> getLastTagsUsed() {
		return lastTagsUsed;
	}

	public void setLastTagsUsed(LimitedConcurrentList<String> lastTagsUsed) {
		this.lastTagsUsed = lastTagsUsed;
	}

	public int getInterline() {
		return interline;
	}

	public void setInterline(int interline) {
		this.interline = interline;
	}

	public EOL getEol() {
		return eol;
	}

	public void setEol(EOL eol) {
		this.eol = eol;
		EOL.defaultEol = eol;
	}

	public boolean isHeaderVisible() {
		return headerVisible;
	}

	public void setHeaderVisible(boolean headerVisible) {
		this.headerVisible = headerVisible;
	}

	public boolean isSubHeaderVisible() {
		return subHeaderVisible;
	}

	public void setSubHeaderVisible(boolean subHeaderVisible) {
		this.subHeaderVisible = subHeaderVisible;
	}

	public boolean isFooterVisible() {
		return footerVisible;
	}

	public void setFooterVisible(boolean footerVisible) {
		this.footerVisible = footerVisible;
	}

	public String getPreferredSyntaxIfNotSync() {
		return preferredSyntaxIfNotSync;
	}

	public void setPreferredSyntaxIfNotSync(String preferredSyntaxIfNotSync) {
		this.preferredSyntaxIfNotSync = preferredSyntaxIfNotSync;
	}

	public boolean isPrefix_hash() {
		return prefix_hash;
	}

	public void setPrefix_hash(boolean prefix_hash) {
		this.prefix_hash = prefix_hash;
	}

	public boolean isAutosaveTempFiles() {
		return autosaveTempFiles;
	}

	public void setAutosaveTempFiles(boolean autosaveTempFiles) {
		this.autosaveTempFiles = autosaveTempFiles;
	}

	public boolean isUppercase_hash() {
		return uppercase_hash;
	}

	public void setUppercase_hash(boolean uppercase_hash) {
		this.uppercase_hash = uppercase_hash;
	}

	public boolean isPaintMatchedBracketPair() {
		return paintMatchedBracketPair;
	}

	public void setPaintMatchedBracketPair(boolean paintMatchedBracketPair) {
		this.paintMatchedBracketPair = paintMatchedBracketPair;
	}

	public boolean isWhiteSpaceVisible() {
		return whiteSpaceVisible;
	}

	public void setWhiteSpaceVisible(boolean whiteSpaceVisible) {
		this.whiteSpaceVisible = whiteSpaceVisible;
	}

	public LimitedConcurrentList<String> getLastSearchTerms() {
		return lastSearchTerms;
	}

	public void setLastSearchTerms(LimitedConcurrentList<String> lastSearchTerms) {
		this.lastSearchTerms = lastSearchTerms;
	}

	public LimitedConcurrentList<FileNamed> getLastSelectedWorkspaces() {
		return lastSelectedWorkspaces;
	}

	public void setLastSelectedWorkspaces(LimitedConcurrentList<FileNamed> lastSelectedWorkspaces) {
		this.lastSelectedWorkspaces = lastSelectedWorkspaces;
	}

	public LimitedConcurrentList<FileNamed> getLastOpenedShellFrameFiles() {
		return lastOpenedShellFiles;
	}

	public void setLastOpenedShellFrameFiles(LimitedConcurrentList<FileNamed> lastOpenedShellFrameFiles) {
		this.lastOpenedShellFiles = lastOpenedShellFrameFiles;
	}

	public LimitedConcurrentList<String> getLastReplaceTerms() {
		return lastReplaceTerms;
	}

	public void setLastReplaceTerms(LimitedConcurrentList<String> lastReplaceTerms) {
		this.lastReplaceTerms = lastReplaceTerms;
	}

	public boolean isClipboardSyncStyle() {
		return clipboardSyncStyle;
	}

	public void setClipboardSyncStyle(boolean clipboardSyncStyle) {
		this.clipboardSyncStyle = clipboardSyncStyle;
	}

	public String getPrefDictspellChecker() {
		return prefDictspellChecker;
	}

	public void setPrefDictspellChecker(String prefDictspellChecker) {
		this.prefDictspellChecker = prefDictspellChecker;
	}

	public boolean isSpellChecker() {
		return spellChecker;
	}

	public void setSpellChecker(boolean spellChecker) {
		this.spellChecker = spellChecker;
	}

	public String getLastFileOpenedClipboard() {
		return lastFileOpenedClipboard;
	}

	public void setLastFileOpenedClipboard(String lastFileOpenedClipboard) {
		this.lastFileOpenedClipboard = lastFileOpenedClipboard;
	}

	public String getLastSelectedTemplate() {
		return lastSelectedTemplate;
	}

	public void setLastSelectedTemplate(String lastSelectedTemplate) {
		this.lastSelectedTemplate = lastSelectedTemplate;
	}

	public int getDividerRightPanePosXClipboard() {
		return dividerRightPanePosXClipboard;
	}


	public void setDividerRightPanePosXClipboard(int dividerRightPanePosXClipboard) {
		this.dividerRightPanePosXClipboard = dividerRightPanePosXClipboard;
	}


	public boolean isDefaultAutoSyntaxSync() {
		return defaultAutoSyntaxSync;
	}


	public void setDefaultAutoSyntaxSync(boolean defaultAutoSyntax) {
		this.defaultAutoSyntaxSync = defaultAutoSyntax;
	}


	public boolean isWorkspaceViewSelected() {
		return workspaceViewSelected;
	}

	public void setWorkspaceViewSelected(boolean workspaceViewSelected) {
		this.workspaceViewSelected = workspaceViewSelected;
	}

	public boolean isTreeLocked() {
		return treeLocked;
	}

	public void setTreeLocked(boolean treeLocked) {
		this.treeLocked = treeLocked;
	}

	public void setTabLineVisible(boolean tabLineVisible) {
		this.tabLineVisible = tabLineVisible;
	}

	public ArrayList<String> getOpenedFileList() {
		return openedFileList;
	}

	public ArrayList<String> getOpenedTempFileList() {
		return openedTempFileList;
	}

	public void setOpenedTempFileList(ArrayList<String> openedTempFileList) {
		this.openedTempFileList = openedTempFileList;
	}

	public boolean isBreaklineActive() {
		return breaklineActive;
	}

	public int getTabSize() {
		return tabSize;
	}

	public void setTabSize(int tabSize) {
		this.tabSize = tabSize;
	}

	public void setBreaklineActive(boolean breaklineActive) {
		this.breaklineActive = breaklineActive;
	}

	public boolean isAntialiasedTxtArea() {
		return antialiasedTxtArea;
	}

	public void setAntialiasedTxtArea(boolean antialiasedTxtArea) {
		this.antialiasedTxtArea = antialiasedTxtArea;
	}

	public boolean isFileTreeActive() {
		return fileTreeActive;
	}

	public void setFileTreeActive(boolean fileTreeActive) {
		this.fileTreeActive = fileTreeActive;
	}

	public int getDividerFileTreePosX() {
		return dividerFileTreePosX;
	}

	public void setDividerFileTreePosX(int dividerFileTreePosX) {
		this.dividerFileTreePosX = dividerFileTreePosX;
	}

	public void setOpenedFileList(ArrayList<String> openedList) {
		this.openedFileList = openedList;
	}

	public Map<String, String> getOpenedFileListCaretPosition() {
		return openedFileListCaretPosition;
	}

	public void setOpenedFileListCaretPosition(Map<String, String> openedFileListCaretPosition) {
		this.openedFileListCaretPosition = openedFileListCaretPosition;
	}

	public void setInlinedTabs(boolean inlinedTabs) {
		this.inlinedTabs = inlinedTabs;
	}

	public boolean isInlinedTabs() {
		return inlinedTabs;
	}

	public String getSelectedTheme() {
		return selectedTheme;
	}

	public void setSelectedTheme(String selectedTheme) {
		this.selectedTheme = selectedTheme;
	}

	public String getSelectedAtStart() {
		return selectedAtStart;
	}

	public void setSelectedAtStart(String selectedAtStart) {
		this.selectedAtStart = selectedAtStart;
	}

	public int getMargin() {
		return margin;
	}

	public void setMargin(int margin) {
		this.margin = margin;
	}

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Color getBackCol() {
		return backCol;
	}

	public void setBackCol(Color backCol) {
		this.backCol = backCol;
	}

	public Color getForeCol() {
		return foreCol;
	}

	public void setForeCol(Color foreCol) {
		this.foreCol = foreCol;
	}

	public Color getFilePanelBackCol() {
		return filePanelBackCol;
	}

	public void setFilePanelBackCol(Color filePanelBackCol) {
		this.filePanelBackCol = filePanelBackCol;
	}

	public int getVoiceRate() {
		return voiceRate;
	}

	public void setVoiceRate(int voiceRate) {
		this.voiceRate = voiceRate;
	}


	public String getDefaultWorkspacePath() {
		return defaultWorkspacePath;
	}


	public void setDefaultWorkspacePath(String defaultWorkspacePath) {
		this.defaultWorkspacePath = defaultWorkspacePath;
	}


	public boolean isCodeFoldingEnabled() {
		return codeFoldingEnabled;
	}


	public void setCodeFoldingEnabled(boolean codeFoldingEnabled) {
		this.codeFoldingEnabled = codeFoldingEnabled;
	}


	public boolean isAutoIndentEnabled() {
		return autoIndentEnabled;
	}


	public void setAutoIndentEnabled(boolean autoIndentEnabled) {
		this.autoIndentEnabled = autoIndentEnabled;
	}


	public boolean isMarkOccurrences() {
		return markOccurrences;
	}


	public void setMarkOccurrences(boolean markOccurrences) {
		this.markOccurrences = markOccurrences;
	}


	public boolean isCloseMarkupTags() {
		return closeMarkupTags;
	}


	public void setCloseMarkupTags(boolean closeMarkupTags) {
		this.closeMarkupTags = closeMarkupTags;
	}


	public boolean isCloseCurlyBraces() {
		return closeCurlyBraces;
	}


	public void setCloseCurlyBraces(boolean closeCurlyBraces) {
		this.closeCurlyBraces = closeCurlyBraces;
	}


	public boolean isBracketMatchingEnabled() {
		return bracketMatchingEnabled;
	}


	public void setBracketMatchingEnabled(boolean bracketMatchingEnabled) {
		this.bracketMatchingEnabled = bracketMatchingEnabled;
	}


	public boolean isAnimateBracketMatching() {
		return animateBracketMatching;
	}


	public void setAnimateBracketMatching(boolean animateBracketMatching) {
		this.animateBracketMatching = animateBracketMatching;
	}


	public boolean isFadeCurrentLineHighlight() {
		return fadeCurrentLineHighlight;
	}


	public void setFadeCurrentLineHighlight(boolean fadeCurrentLineHighlight) {
		this.fadeCurrentLineHighlight = fadeCurrentLineHighlight;
	}


	public boolean isUseFocusableTips() {
		return useFocusableTips;
	}


	public void setUseFocusableTips(boolean useFocusableTips) {
		this.useFocusableTips = useFocusableTips;
	}


	public boolean isRoundedSelectionEdges() {
		return roundedSelectionEdges;
	}


	public void setRoundedSelectionEdges(boolean roundedSelectionEdges) {
		this.roundedSelectionEdges = roundedSelectionEdges;
	}


	public boolean isPaintMarkOccurrencesBorder() {
		return paintMarkOccurrencesBorder;
	}


	public void setPaintMarkOccurrencesBorder(boolean paintMarkOccurrencesBorder) {
		this.paintMarkOccurrencesBorder = paintMarkOccurrencesBorder;
	}


	public boolean isClearWhitespaceLinesEnabled() {
		return clearWhitespaceLinesEnabled;
	}


	public void setClearWhitespaceLinesEnabled(boolean clearWhitespaceLinesEnabled) {
		this.clearWhitespaceLinesEnabled = clearWhitespaceLinesEnabled;
	}


	public boolean isHighlightSecondaryLanguages() {
		return highlightSecondaryLanguages;
	}


	public void setHighlightSecondaryLanguages(boolean highlightSecondaryLanguages) {
		this.highlightSecondaryLanguages = highlightSecondaryLanguages;
	}


	public boolean isFractionalFontMetricsEnabled() {
		return fractionalFontMetricsEnabled;
	}


	public void setFractionalFontMetricsEnabled(boolean fractionalFontMetricsEnabled) {
		this.fractionalFontMetricsEnabled = fractionalFontMetricsEnabled;
	}


	public boolean isHyperLinksEnabled() {
		return hyperLinksEnabled;
	}


	public void setHyperLinksEnabled(boolean hyperLinksEnabled) {
		this.hyperLinksEnabled = hyperLinksEnabled;
	}


	public boolean isTabSpacesEmulated() {
		return tabSpacesEmulated;
	}


	public void setTabSpacesEmulated(boolean tabSpacesEmulated) {
		this.tabSpacesEmulated = tabSpacesEmulated;
	}


	public boolean isMarkEndOfLine() {
		return markEndOfLine;
	}


	public void setMarkEndOfLine(boolean markEndOfLine) {
		this.markEndOfLine = markEndOfLine;
	}


	public boolean isShowBracketsPopup() {
		return showBracketsPopup;
	}


	public void setShowBracketsPopup(boolean showBracketsPopup) {
		this.showBracketsPopup = showBracketsPopup;
	}


	public String getLastEscapeLanguage() {
		return lastEscapeLanguage;
	}


	public void setLastEscapeLanguage(String lastEscapeLanguage) {
		this.lastEscapeLanguage = lastEscapeLanguage;
	}


	public boolean isMapViewActive() {
		return mapViewActive;
	}


	public void setMapViewActive(boolean mapViewActive) {
		this.mapViewActive = mapViewActive;
	}

	public Double getDividerBottomPosRatio() {
		return dividerBottomPosRatio;
	}

	public void setDividerBottomPosRatio(Double dividerBottomPosRatio) {
		this.dividerBottomPosRatio = dividerBottomPosRatio;
	}

	public boolean isBottomViewActive() {
		return bottomViewActive;
	}

	public void setBottomViewActive(boolean bottomViewActive) {
		this.bottomViewActive = bottomViewActive;
	}

	public boolean isClipboardViewActive() {
		return clipboardViewActive;
	}


	public void setClipboardViewActive(boolean clipboardViewActive) {
		this.clipboardViewActive = clipboardViewActive;
	}


	public int getDividerRightPanePosX() {
		return dividerRightPanePosX;
	}


	public void setDividerRightPanePosX(int dividerRightPanePosX) {
		this.dividerRightPanePosX = dividerRightPanePosX;
	}


	public LimitedConcurrentList<FileNamed> getLastOpenedFiles() {
		return lastOpenedFiles == null ? DEF_lastOpenedFiles : lastOpenedFiles;
	}


	public void setLastOpenedFiles(LimitedConcurrentList<FileNamed> lastOpenedFiles) {
		this.lastOpenedFiles = lastOpenedFiles;
	}

	public boolean isFirstTimeOpened() {
		return firstTimeOpened;
	}

	public void setFirstTimeOpened(boolean firstTimeOpened) {
		this.firstTimeOpened = firstTimeOpened;
	}

}
