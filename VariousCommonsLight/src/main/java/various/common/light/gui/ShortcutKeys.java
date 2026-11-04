/**
 *
 * =======================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =======================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.gui;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 * Cross-platform "main shortcut key": Ctrl on Windows and Linux, Cmd on macOS.
 * Every shortcut that used to test Ctrl directly goes through here, so that the Windows
 * behaviour is unchanged and macOS gets the Cmd equivalent (Ctrl keeps working there too).
 */
public final class ShortcutKeys {

	public static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");

	/** Legacy modifier mask (for KeyStroke.getKeyStroke(int, int)): CTRL_MASK, or META_MASK on macOS. */
	public static final int MENU_MASK = IS_MAC ? InputEvent.META_MASK : InputEvent.CTRL_MASK;

	/** Extended modifier mask: CTRL_DOWN_MASK, or META_DOWN_MASK on macOS. */
	public static final int MENU_DOWN_MASK = IS_MAC ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;

	private ShortcutKeys() {
	}

	/** True if Ctrl is down (or Cmd, on macOS). */
	public static boolean isCtrlDown(InputEvent e) {
		return e.isControlDown() || (IS_MAC && e.isMetaDown());
	}

	/** True if the key code is the main shortcut modifier key (Ctrl, or Ctrl/Cmd on macOS). */
	public static boolean isCtrlKey(int keyCode) {
		return keyCode == KeyEvent.VK_CONTROL || (IS_MAC && keyCode == KeyEvent.VK_META);
	}

	/** Platform menu shortcut mask as reported by AWT (same as MENU_MASK, kept for headless-safe callers). */
	public static int toolkitMenuMask() {
		try {
			return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		} catch (Exception e) {
			return MENU_MASK;
		}
	}
}
