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
package various.common.light.gui;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Every normal window opens a little bigger than its content (10% more, at most 92% x 90% of the screen), so that texts that are
 * longer in other languages do not overlap or get cut. It is done once per window, when it is opened, keeping the same center.
 * Maximized, undecorated (splash, loaders, animations) and nearly full screen windows are left as they are.
 */
public final class WindowMargin {

	public static final double FACTOR = 1.10;

	private static boolean installed = false;
	private static final Set<Window> DONE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Window, Boolean>()));

	private WindowMargin() {
	}

	public static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (event.getID() == WindowEvent.WINDOW_OPENED && event instanceof WindowEvent) {
				try {
					grow(((WindowEvent) event).getWindow());
				} catch (Exception e) {
					// cosmetic only
				}
			}
		}, AWTEvent.WINDOW_EVENT_MASK);
	}

	private static void grow(Window window) {
		if (window == null || !DONE.add(window) || !(window instanceof Frame || window instanceof Dialog)) {
			return;
		}
		if (window instanceof Frame && ((Frame) window).getExtendedState() != Frame.NORMAL) {
			return;
		}
		boolean undecorated = window instanceof Frame ? ((Frame) window).isUndecorated() : ((Dialog) window).isUndecorated();
		java.awt.GraphicsConfiguration gc = window.getGraphicsConfiguration();
		if (gc == null) {
			return;
		}
		Rectangle screen = gc.getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
		int maxW = (int) ((screen.width - insets.left - insets.right) * 0.92);
		int maxH = (int) ((screen.height - insets.top - insets.bottom) * 0.90);
		int w = window.getWidth();
		int h = window.getHeight();
		if (w <= 0 || h <= 0 || w >= maxW * 0.75 || h >= maxH * 0.75) {
			return; // large windows (main window, maximized-like): left as they are
		}
		if (undecorated && !isLafDecorated(window)) {
			return;
		}
		int newW = Math.min((int) Math.round(w * FACTOR), maxW);
		int newH = Math.min((int) Math.round(h * FACTOR), maxH);
		if (newW == w && newH == h) {
			return;
		}
		int x = window.getX() - (newW - w) / 2;
		int y = window.getY() - (newH - h) / 2;
		Rectangle area = new Rectangle(screen.x + insets.left, screen.y + insets.top, screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom);
		x = Math.max(area.x, Math.min(x, area.x + area.width - newW));
		y = Math.max(area.y, Math.min(y, area.y + area.height - newH));
		window.setBounds(x, y, newW, newH);
		window.validate();
	}

	/** Windows drawn by the Look and Feel (title bar of the LAF) count as normal windows */
	private static boolean isLafDecorated(Window window) {
		if (window instanceof javax.swing.RootPaneContainer) {
			return ((javax.swing.RootPaneContainer) window).getRootPane().getWindowDecorationStyle() != javax.swing.JRootPane.NONE;
		}
		return false;
	}
}
