/**
 *
 * =======================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =======================================================================================
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
package various.common.light.gui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;

import javax.swing.JComponent;

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

	/**
	 * Client property (Boolean.TRUE) of a component that wants every key for itself, as the interactive terminal does:
	 * while it has the focus the global shortcut listeners and the menu accelerators stay out of the way.
	 */
	public static final String CAPTURE_ALL_KEYS = "screept.captureAllKeys";

	/** True when the component with the keyboard focus asked to receive all the keys (see {@link #CAPTURE_ALL_KEYS}) */
	public static boolean isFocusOwnerCapturingKeys() {
		Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return owner instanceof JComponent && Boolean.TRUE.equals(((JComponent) owner).getClientProperty(CAPTURE_ALL_KEYS));
	}

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
