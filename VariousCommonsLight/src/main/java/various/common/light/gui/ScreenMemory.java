/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
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
package various.common.light.gui;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.Timer;

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

	private static final int X11_CHECK_MILLIS = 300;
	private static final int X11_CHECKS = 8;

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

	/** Centers the window, keeping its current size, on the last used monitor */
	public static void centerOnLastScreen(Window window) {
		centerOnLastScreen(window, window.getWidth(), window.getHeight());
	}

	/** Moves the frame on the last used monitor and maximizes it there */
	public static void maximizeOnLastScreen(JFrame frame) {
		Rectangle screen = getLastScreenBounds();
		if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("linux")) {
			maximizeOnX11(frame, screen);
			return;
		}
		if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
			// a maximized frame does not move: restore it first
			frame.setExtendedState(JFrame.NORMAL);
		}
		frame.setLocation(screen.x, screen.y);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
	}

	/**
	 * The X11 window manager applies the state changes later: restoring and maximizing in a row can leave the window restored.
	 * So it is restored only to change monitor, and the result is checked for a couple of seconds.
	 */
	private static void maximizeOnX11(JFrame frame, Rectangle screen) {
		Rectangle at = frame.getBounds();
		if (!screen.contains(new Point((int) at.getCenterX(), (int) at.getCenterY()))) {
			frame.setExtendedState(JFrame.NORMAL);
			frame.setLocation(screen.x, screen.y);
		}
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

		final int[] checks = { 0 };
		Timer timer = new Timer(X11_CHECK_MILLIS, null);
		timer.addActionListener(e -> {
			int state = frame.getExtendedState();
			if ((state & JFrame.ICONIFIED) != 0 || fills(state, frame.getSize(), screen)) {
				timer.stop();
			} else if (++checks[0] >= X11_CHECKS) {
				// the window manager does not maximize: at least take all the space left free by the panels
				Insets panels = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
				frame.setBounds(screen.x + panels.left, screen.y + panels.top, screen.width - panels.left - panels.right,
						screen.height - panels.top - panels.bottom);
				timer.stop();
			} else {
				// a state believed already set is not sent again: restore now and maximize at the next check
				boolean believed = (state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
				frame.setExtendedState(believed ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
			}
		});
		timer.start();
	}

	/** Maximized for real: the state alone is not enough, and panels take only a small part of the screen */
	static boolean fills(int state, Dimension size, Rectangle screen) {
		return (state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH && size.width >= screen.width * 0.85
				&& size.height >= screen.height * 0.85;
	}

	private static boolean guardInstalled = false;

	/**
	 * Every window (plugins included) opens on the monitor of the main window of the application: when a window is shown on another
	 * monitor, it is centered on the one of the main window. Windows that already are on that monitor, maximized or undecorated
	 * ones (splash, loaders, animations) are not touched, and neither are windows opened while the main window is not visible.
	 */
	public static synchronized void keepWindowsOnMainScreen(final Supplier<? extends Window> mainWindow) {
		if (guardInstalled) {
			return;
		}
		guardInstalled = true;
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (event.getID() == WindowEvent.WINDOW_OPENED && event instanceof WindowEvent) {
				try {
					moveToScreenOf(mainWindow.get(), ((WindowEvent) event).getWindow());
				} catch (Exception e) {
					// cosmetic only
				}
			}
		}, AWTEvent.WINDOW_EVENT_MASK);
	}

	private static void moveToScreenOf(Window main, Window window) {
		if (main == null || window == null || window == main || !main.isShowing() || main.getGraphicsConfiguration() == null) {
			return;
		}
		if (!(window instanceof Frame || window instanceof Dialog)) {
			return;
		}
		if (window instanceof Frame && ((Frame) window).getExtendedState() != Frame.NORMAL) {
			return;
		}
		boolean undecorated = window instanceof Frame ? ((Frame) window).isUndecorated() : ((Dialog) window).isUndecorated();
		if (undecorated || window.getGraphicsConfiguration() == null) {
			return;
		}
		Rectangle screen = main.getGraphicsConfiguration().getBounds();
		Rectangle at = window.getBounds();
		if (screen.contains(new Point((int) at.getCenterX(), (int) at.getCenterY()))) {
			return;
		}
		int w = Math.min(at.width, screen.width);
		int h = Math.min(at.height, screen.height);
		window.setBounds(screen.x + (screen.width - w) / 2, screen.y + (screen.height - h) / 2, w, h);
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
