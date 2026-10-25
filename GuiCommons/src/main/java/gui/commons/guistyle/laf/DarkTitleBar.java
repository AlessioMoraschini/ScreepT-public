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
package gui.commons.guistyle.laf;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.UIManager;

import org.apache.log4j.Logger;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Dark native title bar (Windows 10 20H1+, caption/border colors on Windows 11) for every window while
 * {@link ScreepTDarkLaf} is the current LAF; restored to the system default when another LAF is set.
 * Native frames are kept, so resizing, snapping and always-on-top behave exactly as before.
 */
final class DarkTitleBar {

	static Logger logger = Logger.getLogger(DarkTitleBar.class);

	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");

	private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19;
	private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
	private static final int DWMWA_BORDER_COLOR = 34;
	private static final int DWMWA_CAPTION_COLOR = 35;
	private static final int DWMWA_COLOR_DEFAULT = 0xFFFFFFFF;

	// COLORREF is 0x00BBGGRR: #2B2D30 caption, #393B40 border (same as ScreepTDarkLaf.properties)
	private static final int CAPTION_COLOR = 0x00302D2B;
	private static final int BORDER_COLOR = 0x00403B39;

	private static boolean installed;

	private DarkTitleBar() {
	}

	static synchronized void install() {

		if (installed || !WINDOWS) {
			return;
		}
		installed = true;

		// new windows: only darkened, light LAFs keep the untouched system default
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (event.getID() == WindowEvent.WINDOW_OPENED && isDarkLafActive()) {
				apply(((WindowEvent) event).getWindow(), true);
			}
		}, AWTEvent.WINDOW_EVENT_MASK);

		// opened windows: updated on every LAF switch
		UIManager.addPropertyChangeListener(event -> {
			if ("lookAndFeel".equals(event.getPropertyName())) {
				boolean dark = isDarkLafActive();
				for (Window window : Window.getWindows()) {
					apply(window, dark);
				}
			}
		});
	}

	private static boolean isDarkLafActive() {
		return UIManager.getLookAndFeel() instanceof ScreepTDarkLaf;
	}

	private static void apply(Window window, boolean dark) {

		if (window == null || !window.isDisplayable()) {
			return;
		}

		try {
			Pointer hwnd = Native.getWindowPointer(window);
			if (hwnd == null) {
				return;
			}
			if (setAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, dark ? 1 : 0) != 0) {
				setAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1, dark ? 1 : 0);
			}
			// not supported before Windows 11: the call just fails
			setAttribute(hwnd, DWMWA_CAPTION_COLOR, dark ? CAPTION_COLOR : DWMWA_COLOR_DEFAULT);
			setAttribute(hwnd, DWMWA_BORDER_COLOR, dark ? BORDER_COLOR : DWMWA_COLOR_DEFAULT);
		} catch (Throwable e) {
			logger.debug("Cannot update title bar of " + window.getName(), e);
		}
	}

	private static int setAttribute(Pointer hwnd, int attribute, int value) {
		return Function.getFunction("dwmapi", "DwmSetWindowAttribute", Function.ALT_CONVENTION)
				.invokeInt(new Object[] { hwnd, attribute, new IntByReference(value), 4 });
	}
}
