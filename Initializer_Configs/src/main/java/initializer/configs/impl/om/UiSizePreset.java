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

/**
 * Interface size presets: percentages applied to general text, menus, toolbars and graphic elements.
 * CUSTOM keeps the values chosen by the user.
 */
public enum UiSizePreset {

	MINI("Mini", 80, 80, 80, 85),
	SMALL("Small", 90, 90, 90, 90),
	MEDIUM("Medium", 100, 100, 100, 100),
	BIG("Big", 115, 115, 115, 115),
	XXL("XXL", 135, 135, 135, 130),
	CUSTOM("Custom", 100, 100, 100, 100);

	public final String label;
	public final int generalFontScale;
	public final int menuFontScale;
	public final int toolbarFontScale;
	public final int componentScale;

	private UiSizePreset(String label, int generalFontScale, int menuFontScale, int toolbarFontScale, int componentScale) {
		this.label = label;
		this.generalFontScale = generalFontScale;
		this.menuFontScale = menuFontScale;
		this.toolbarFontScale = toolbarFontScale;
		this.componentScale = componentScale;
	}

	public static UiSizePreset fromName(String name) {
		try {
			return valueOf(name);
		} catch (RuntimeException e) {
			return MEDIUM;
		}
	}

	@Override
	public String toString() {
		return label;
	}
}
