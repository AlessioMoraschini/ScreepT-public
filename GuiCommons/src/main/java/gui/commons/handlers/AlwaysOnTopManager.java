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
package gui.commons.handlers;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.RootPaneContainer;

import org.apache.log4j.Logger;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Single rule for "always on top" of secondary windows, whatever they set in their own code:
 * they are always on top (over other applications too) only while the application is pinned
 * (main window pin toggle), otherwise they are normal windows. Owned dialogs, like the color picker,
 * still stay above their owner window.
 * Excluded: the main window (handled by the pin itself), undecorated windows (splash, loaders),
 * message dialogs (JOptionPane), that keep their own behavior.
 * Windows registered with {@link #keepAboveApplication} (e.g. option frames) also stay above the main window:
 * every time the main window is activated they are raised over it, without taking the focus. Not topmost,
 * so they never cover other applications nor the menus and popups of the main window.
 */
public final class AlwaysOnTopManager {

	static Logger logger = Logger.getLogger(AlwaysOnTopManager.class);

	/** Root pane client property marking the windows kept above the main window */
	public static final String KEEP_ABOVE_APPLICATION = "screept.keepAboveApplication";

	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
	private static final int SWP_NOSIZE = 0x0001;
	private static final int SWP_NOMOVE = 0x0002;
	private static final int SWP_NOACTIVATE = 0x0010;

	private static Window mainWindow;
	private static volatile boolean pinned;
	private static boolean installed;

	private AlwaysOnTopManager() {
	}

	public static synchronized void install(Window applicationMainWindow) {

		mainWindow = applicationMainWindow;
		pinned = applicationMainWindow != null && applicationMainWindow.isAlwaysOnTop();
		if (installed) {
			return;
		}
		installed = true;

		// windows set their own always on top while being built or shown again: re-apply the rule
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			int id = event.getID();
			if (id == WindowEvent.WINDOW_OPENED || id == WindowEvent.WINDOW_ACTIVATED) {
				apply(((WindowEvent) event).getWindow());
				if (id == WindowEvent.WINDOW_ACTIVATED && ((WindowEvent) event).getWindow() == mainWindow) {
					raiseKeptAboveWindows();
				}
			}
		}, AWTEvent.WINDOW_EVENT_MASK);
	}

	/** Called by the main window pin toggle */
	public static void setPinned(boolean applicationPinned) {
		pinned = applicationPinned;
		for (Window window : Window.getWindows()) {
			if (window.isShowing()) {
				apply(window);
			}
		}
	}

	public static boolean isPinned() {
		return pinned;
	}

	/** The window will stay above the main window while both are open (see class description) */
	public static void keepAboveApplication(RootPaneContainer window) {
		window.getRootPane().putClientProperty(KEEP_ABOVE_APPLICATION, Boolean.TRUE);
	}

	private static void raiseKeptAboveWindows() {
		for (Window window : Window.getWindows()) {
			if (isKeptAbove(window) && window.isShowing()
					&& !(window instanceof Frame && (((Frame) window).getExtendedState() & Frame.ICONIFIED) != 0)) {
				raiseWithoutFocus(window);
			}
		}
	}

	private static boolean isKeptAbove(Window window) {
		return window instanceof RootPaneContainer
				&& Boolean.TRUE.equals(((RootPaneContainer) window).getRootPane().getClientProperty(KEEP_ABOVE_APPLICATION));
	}

	/** Z-order only: toFront() would also activate the window, stealing the focus from the main one */
	private static void raiseWithoutFocus(Window window) {
		if (!WINDOWS) {
			return;
		}
		try {
			Pointer hwnd = Native.getWindowPointer(window);
			if (hwnd != null) {
				// HWND_TOP = 0
				Function.getFunction("user32", "SetWindowPos", Function.ALT_CONVENTION).invokeInt(new Object[] {
						hwnd, Pointer.NULL, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE });
			}
		} catch (Throwable e) {
			logger.warn("Cannot raise window " + window.getName(), e);
		}
	}

	private static void apply(Window window) {
		if (isManaged(window) && window.isAlwaysOnTopSupported() && window.isAlwaysOnTop() != pinned) {
			window.setAlwaysOnTop(pinned);
		}
	}

	private static boolean isManaged(Window window) {
		if (window == null || window == mainWindow) {
			return false;
		}
		if (window instanceof Frame) {
			return !((Frame) window).isUndecorated();
		}
		if (window instanceof Dialog) {
			return !((Dialog) window).isUndecorated() && !isMessageDialog(window);
		}
		return false;
	}

	private static boolean isMessageDialog(Window window) {
		return window instanceof JDialog && ((JDialog) window).getContentPane().getComponentCount() > 0
				&& (((JDialog) window).getContentPane() instanceof JOptionPane
						|| ((JDialog) window).getContentPane().getComponent(0) instanceof JOptionPane);
	}
}
