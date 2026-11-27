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
package gui.commons.guistyle.laf;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatSystemProperties;

import gui.commons.guistyle.LookAndFeelInfoCustom;

/**
 * Dark look and feel in IntelliJ IDEA "New UI" style, built on FlatLaf.
 * Colors and metrics are in ScreepTDarkLaf.properties (same package, loaded by FlatLaf), so the theme
 * can be tuned without touching code.
 * Kept light on purpose: no animations, no smooth scrolling, no popup shadows, native window frames
 * (only the title bar is darkened through DWM, see {@link DarkTitleBar}).
 */
public class ScreepTDarkLaf extends FlatDarkLaf {

	private static final long serialVersionUID = -4319566035781227706L;

	static Logger logger = Logger.getLogger(ScreepTDarkLaf.class);

	public static final String NAME = "ScreepT Dark";
	public static final String CLASS_NAME = ScreepTDarkLaf.class.getName();

	/**
	 * Developer defaults put by GUIStyelSettings for the light/Nimbus styles: they have priority over
	 * any LAF default, so they must be removed or they would leak into this theme (e.g. black menu text).
	 */
	private static final String[] LEGACY_OVERRIDES = {
			"Button.foreground", "Button.focus", "CheckBox.disabledText", "RadioButton.disabledText",
			"MenuBar.foreground", "MenuItem.textIconGap", "ScrollBar.background", "ToolTip.font",
			"Tree.selectionBackground", "OptionPane.buttonFont"
	};

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "ScreepT dark theme, IntelliJ IDEA style";
	}

	public static boolean isScreepTDark(String lafClassName) {
		return CLASS_NAME.equals(lafClassName);
	}

	public static LookAndFeelInfoCustom getInfo() {
		return new LookAndFeelInfoCustom(NAME, CLASS_NAME);
	}

	/**
	 * Sets this LAF as current one. Caller is in charge of refreshing already opened windows.
	 */
	public static void apply() throws UnsupportedLookAndFeelException {

		if (System.getProperty(FlatSystemProperties.ANIMATION) == null) {
			System.setProperty(FlatSystemProperties.ANIMATION, "false");
		}

		for (String key : LEGACY_OVERRIDES) {
			UIManager.put(key, null);
		}

		try {
			DarkTitleBar.install();
		} catch (Throwable e) {
			logger.warn("Dark title bar not available", e);
		}
		DarkColorAdapter.install();

		UIManager.setLookAndFeel(new ScreepTDarkLaf());
	}
}
