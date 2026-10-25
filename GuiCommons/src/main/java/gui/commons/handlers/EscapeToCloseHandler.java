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

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

/**
 * ESC closes the focused frame or dialog exactly like its title bar X (WINDOW_CLOSING event, so close
 * listeners and default close operation apply). Runs after component key bindings: menus, combo popups and
 * dialogs already handling ESC consume it first.
 * Excluded: the main window, undecorated windows (no X), windows whose root pane has DISABLE_PROPERTY = TRUE.
 */
public final class EscapeToCloseHandler {

	/** Root pane client property (Boolean.TRUE) disabling ESC close for that window */
	public static final String DISABLE_PROPERTY = "screept.escapeToClose.disabled";

	private static Window mainWindow;
	private static boolean installed;

	private EscapeToCloseHandler() {
	}

	public static synchronized void install(Window applicationMainWindow) {

		mainWindow = applicationMainWindow;
		if (installed) {
			return;
		}
		installed = true;

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(event -> {
			if (event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_ESCAPE
					|| event.getModifiersEx() != 0 || event.isConsumed()) {
				return false;
			}

			Window window = windowOf(event.getComponent());
			if (!canClose(window)) {
				return false;
			}

			event.consume();
			window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
			return true;
		});
	}

	private static Window windowOf(Component component) {
		if (component == null) {
			return null;
		}
		return component instanceof Window ? (Window) component : SwingUtilities.getWindowAncestor(component);
	}

	private static boolean canClose(Window window) {

		if (window == null || window == mainWindow || !window.isShowing()) {
			return false;
		}
		boolean decorated = (window instanceof Frame && !((Frame) window).isUndecorated())
				|| (window instanceof Dialog && !((Dialog) window).isUndecorated());
		if (!decorated) {
			return false;
		}
		if (window instanceof RootPaneContainer) {
			JRootPane rootPane = ((RootPaneContainer) window).getRootPane();
			return !Boolean.TRUE.equals(rootPane.getClientProperty(DISABLE_PROPERTY));
		}
		return true;
	}
}
