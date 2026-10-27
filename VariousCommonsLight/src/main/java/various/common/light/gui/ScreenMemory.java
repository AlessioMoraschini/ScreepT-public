/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.gui;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.prefs.Preferences;

import javax.swing.JFrame;

import various.common.light.utility.log.SafeLogger;

/**
 * Remembers the monitor last used by the application, so launcher loader and main frame reopen on it.
 * Stored in user preferences (readable before any configuration file is loaded, shared by launcher and app).
 * Falls back to the primary monitor when the saved one is no longer connected.
 */
public final class ScreenMemory {

	private static final SafeLogger logger = new SafeLogger(ScreenMemory.class);

	private static final String PREFS_NODE = "screept/gui";
	private static final String KEY_LAST_SCREEN = "lastScreenBounds";

	private static String lastSavedValue;

	private ScreenMemory() {
	}

	/** Bounds of the last used monitor if still connected, otherwise of the primary one */
	public static Rectangle getLastScreenBounds() {
		Rectangle saved = readSavedBounds();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if (saved != null) {
			Point savedCenter = new Point((int) saved.getCenterX(), (int) saved.getCenterY());
			for (GraphicsDevice device : ge.getScreenDevices()) {
				Rectangle bounds = device.getDefaultConfiguration().getBounds();
				if (bounds.equals(saved) || bounds.contains(savedCenter)) {
					return bounds;
				}
			}
		}
		return ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
	}

	/** Saves the monitor currently hosting the window (writes only when it changes) */
	public static void remember(Window window) {
		if (window == null || window.getGraphicsConfiguration() == null) {
			return;
		}
		Rectangle bounds = window.getGraphicsConfiguration().getBounds();
		String value = bounds.x + "," + bounds.y + "," + bounds.width + "," + bounds.height;
		if (value.equals(lastSavedValue)) {
			return;
		}
		try {
			Preferences.userRoot().node(PREFS_NODE).put(KEY_LAST_SCREEN, value);
			lastSavedValue = value;
		} catch (Exception e) {
			logger.warn("Cannot save last used screen", e);
		}
	}

	/** Centers the window, with the given size, on the last used monitor */
	public static void centerOnLastScreen(Window window, int width, int height) {
		Rectangle screen = getLastScreenBounds();
		window.setBounds(screen.x + (screen.width - width) / 2, screen.y + (screen.height - height) / 2, width, height);
	}

	/** Moves the frame on the last used monitor and maximizes it there */
	public static void maximizeOnLastScreen(JFrame frame) {
		Rectangle screen = getLastScreenBounds();
		if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
			// a maximized frame does not move: restore it first
			frame.setExtendedState(JFrame.NORMAL);
		}
		frame.setLocation(screen.x, screen.y);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
	}

	private static Rectangle readSavedBounds() {
		try {
			String value = Preferences.userRoot().node(PREFS_NODE).get(KEY_LAST_SCREEN, null);
			if (value == null) {
				return null;
			}
			String[] parts = value.split(",");
			lastSavedValue = value;
			return new Rectangle(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
					Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()));
		} catch (Exception e) {
			logger.warn("Cannot read last used screen, using primary", e);
			return null;
		}
	}
}
