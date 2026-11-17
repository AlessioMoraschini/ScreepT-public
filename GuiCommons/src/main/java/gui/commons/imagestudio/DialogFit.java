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
package gui.commons.imagestudio;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.RootPaneContainer;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

import various.common.light.gui.ScreenMemory;

/**
 * Rule for every window: it opens fitting its content, and when the content does not fit the screen it has scroll bars,
 * both vertical and horizontal, instead of being cut or overlapping.
 */
public final class DialogFit {

	private DialogFit() {
	}

	/** The content pane of the dialog goes in a scroll pane and the dialog is sized to the content (at most 92% x 90% of the screen) */
	public static void apply(JDialog dialog, Window owner) {
		apply(dialog, owner, 0, 0);
	}

	/**
	 * @param wantedWidth  width the window should have at least when the screen allows it (0: just the content)
	 * @param wantedHeight same for the height
	 */
	public static void apply(JDialog dialog, Window owner, int wantedWidth, int wantedHeight) {
		fit(dialog, dialog, owner, wantedWidth, wantedHeight);
	}

	/**
	 * Same rule for a frame: the content goes in a scroll pane, the frame is sized to the content (at most 92% x 90% of the screen)
	 * and opens centered over the owner, so on the monitor of the application (without owner: the monitor last used by the application).
	 */
	public static void apply(JFrame frame, Window owner, int wantedWidth, int wantedHeight) {
		fit(frame, frame, owner, wantedWidth, wantedHeight);
	}

	private static void fit(Window window, RootPaneContainer container, Window owner, int wantedWidth, int wantedHeight) {
		JComponent content = (JComponent) container.getContentPane();
		JScrollPane scroll = new JScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getHorizontalScrollBar().setUnitIncrement(16);
		container.setContentPane(scroll);

		Rectangle screen = screenOf(owner);
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configOf(screen));
		int maxW = (int) ((screen.width - insets.left - insets.right) * 0.92);
		int maxH = (int) ((screen.height - insets.top - insets.bottom) * 0.90);
		Dimension pref = content.getPreferredSize();
		int bar = Math.max(12, UIManager.getInt("ScrollBar.width"));
		int w = Math.max(pref.width + 4, wantedWidth);
		int h = Math.max(pref.height + 4, wantedHeight);
		boolean tooTall = h > maxH;
		boolean tooWide = w > maxW;
		if (tooTall) {
			w += bar;
		}
		if (tooWide) {
			h += bar;
		}
		w = Math.min(w, maxW);
		h = Math.min(h, maxH);
		// pack(): the size of the scroll pane is the one of the content, and the window adds its own title bar and borders on top,
		// so the content fits and the scroll bars show only when the screen limit was reached
		scroll.setPreferredSize(new Dimension(w, h));
		window.pack();
		window.setMinimumSize(new Dimension(Math.min(520, window.getWidth()), Math.min(360, window.getHeight())));
		if (owner != null && owner.isShowing()) {
			window.setLocationRelativeTo(owner);
		} else {
			window.setLocation(screen.x + Math.max(0, (screen.width - window.getWidth()) / 2), screen.y + Math.max(0, (screen.height - window.getHeight()) / 2));
		}
	}

	/** Configuration of the monitor with the given bounds (to read its taskbar insets), the default one if not found */
	private static GraphicsConfiguration configOf(Rectangle screen) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (GraphicsDevice device : ge.getScreenDevices()) {
			if (device.getDefaultConfiguration().getBounds().equals(screen)) {
				return device.getDefaultConfiguration();
			}
		}
		return ge.getDefaultScreenDevice().getDefaultConfiguration();
	}

	private static Rectangle screenOf(Window owner) {
		GraphicsConfiguration gc = owner != null ? owner.getGraphicsConfiguration() : null;
		if (gc != null) {
			return gc.getBounds();
		}
		return ScreenMemory.getLastScreenBounds();
	}

	/**
	 * Brings a window in front of everything (also of other programs), gives it the focus and shows it again if it is minimized. It stays
	 * always on top for 2 seconds, so that the operating system does not leave it behind, then it is a normal window again (it stays
	 * on top only if the window is pinned by the user or another window of the application is pinned on top).
	 */
	public static void bringToFront(final Window window) {
		if (window instanceof Frame && ((Frame) window).getState() == Frame.ICONIFIED) {
			((Frame) window).setState(Frame.NORMAL);
		}
		final boolean wasOnTop = window.isAlwaysOnTop();
		window.setVisible(true);
		window.setAlwaysOnTop(true);
		window.toFront();
		window.requestFocus();
		Timer release = new Timer(2000, e -> {
			boolean otherPinned = false;
			for (Window other : Window.getWindows()) {
				if (other != window && other.isVisible() && other.isAlwaysOnTop()) {
					otherPinned = true;
					break;
				}
			}
			if (window.isDisplayable()) {
				window.setAlwaysOnTop(wasOnTop || otherPinned);
			}
		});
		release.setRepeats(false);
		release.start();
	}
}
