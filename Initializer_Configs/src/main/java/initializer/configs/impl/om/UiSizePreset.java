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
