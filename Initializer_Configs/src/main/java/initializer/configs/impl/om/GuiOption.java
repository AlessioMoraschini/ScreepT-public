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


public class GuiOption {

	// DEFAULT VALUES (usfeul to keep valid object during iniLoad, in case of null read or exception)
	public final static int DEFAULT_MENU_FONT_SIZE = 12; 
	public final static int DEFAULT_LOG_FONT_SIZE = 12; 
	/** ScreepT Dark (GuiCommons, gui.commons.guistyle.laf.ScreepTDarkLaf) */
	public final static String DEFAULT_GUI_STYLE ="gui.commons.guistyle.laf.ScreepTDarkLaf";
	public final static String DEFAULT_GUI_PREF_THEME ="DarkStar";
	public final static boolean DEFAULT_GUI_DARK_NIMBUS = false;
	public final static int DEFAULT_NIMBUS_SIZE = 1;
	public final static UiSizePreset DEFAULT_UI_SIZE_PRESET = UiSizePreset.MEDIUM;
	/** Empty: keeps the font families of LAF and components */
	public final static String DEFAULT_UI_FONT_FAMILY = "";

	// FIELDS
	private String preferredStyle;
	private String preferredThemeName;
	private int menuFontSize;
	private int logFontSize;
	private boolean darkGuiNimbus;
	public int nimbusSize = 1;

	// interface size (font scales and component scale are percentages)
	private UiSizePreset uiSizePreset;
	private String uiFontFamily;
	private int uiGeneralFontScale;
	private int uiMenuFontScale;
	private int uiToolbarFontScale;
	private int uiComponentScale;

	public GuiOption() {
		preferredStyle = DEFAULT_GUI_STYLE;
		preferredThemeName = DEFAULT_GUI_PREF_THEME;
		menuFontSize = DEFAULT_MENU_FONT_SIZE;
		logFontSize = DEFAULT_LOG_FONT_SIZE;
		darkGuiNimbus = DEFAULT_GUI_DARK_NIMBUS;
		nimbusSize = DEFAULT_NIMBUS_SIZE;
		applyUiSizePreset(DEFAULT_UI_SIZE_PRESET);
	}
	
	public GuiOption(int menuSize, int logSize, String style, boolean darkGuiNim, int nimbusSize) {
		this();
		menuFontSize = menuSize;
		logFontSize = logSize;
		preferredStyle = style;
		darkGuiNimbus = darkGuiNim;
		this.nimbusSize = nimbusSize;
	}
	
	
	public String getPreferredThemeName() {
		return preferredThemeName;
	}

	public void setPreferredThemeName(String preferredThemeName) {
		this.preferredThemeName = preferredThemeName;
	}

	public int getNimbusSize() {
		return nimbusSize;
	}

	public void setNimbusSize(int nimbusSize) {
		this.nimbusSize = nimbusSize;
	}

	public boolean isDarkGuiNimbus() {
		return darkGuiNimbus;
	}

	public void setDarkGuiNimbus(boolean darkGuiNimbus) {
		this.darkGuiNimbus = darkGuiNimbus;
	}

	public String getPreferredStyle() {
		return preferredStyle;
	}

	public void setPreferredStyle(String preferredStyle) {
		this.preferredStyle = preferredStyle;
	}

	public int getMenuFontSize() {
		return menuFontSize;
	}

	public void setMenuFontSize(int menuFontSize) {
		this.menuFontSize = menuFontSize;
	}

	public int getLogFontSize() {
		return logFontSize;
	}

	public void setLogFontSize(int logFontSize) {
		this.logFontSize = logFontSize;
	}

	/**
	 * Sets the preset and its default values (font family included); CUSTOM keeps the current values
	 */
	public void applyUiSizePreset(UiSizePreset preset) {
		uiSizePreset = preset;
		if (preset != UiSizePreset.CUSTOM) {
			uiFontFamily = DEFAULT_UI_FONT_FAMILY;
			uiGeneralFontScale = preset.generalFontScale;
			uiMenuFontScale = preset.menuFontScale;
			uiToolbarFontScale = preset.toolbarFontScale;
			uiComponentScale = preset.componentScale;
		}
	}

	public void copyUiSizeFrom(GuiOption other) {
		uiSizePreset = other.uiSizePreset;
		uiFontFamily = other.uiFontFamily;
		uiGeneralFontScale = other.uiGeneralFontScale;
		uiMenuFontScale = other.uiMenuFontScale;
		uiToolbarFontScale = other.uiToolbarFontScale;
		uiComponentScale = other.uiComponentScale;
	}

	public UiSizePreset getUiSizePreset() {
		return uiSizePreset;
	}

	public void setUiSizePreset(UiSizePreset uiSizePreset) {
		this.uiSizePreset = uiSizePreset;
	}

	public String getUiFontFamily() {
		return uiFontFamily;
	}

	public void setUiFontFamily(String uiFontFamily) {
		this.uiFontFamily = uiFontFamily == null ? DEFAULT_UI_FONT_FAMILY : uiFontFamily;
	}

	public int getUiGeneralFontScale() {
		return uiGeneralFontScale;
	}

	public void setUiGeneralFontScale(int uiGeneralFontScale) {
		this.uiGeneralFontScale = uiGeneralFontScale;
	}

	public int getUiMenuFontScale() {
		return uiMenuFontScale;
	}

	public void setUiMenuFontScale(int uiMenuFontScale) {
		this.uiMenuFontScale = uiMenuFontScale;
	}

	public int getUiToolbarFontScale() {
		return uiToolbarFontScale;
	}

	public void setUiToolbarFontScale(int uiToolbarFontScale) {
		this.uiToolbarFontScale = uiToolbarFontScale;
	}

	public int getUiComponentScale() {
		return uiComponentScale;
	}

	public void setUiComponentScale(int uiComponentScale) {
		this.uiComponentScale = uiComponentScale;
	}
	
}
