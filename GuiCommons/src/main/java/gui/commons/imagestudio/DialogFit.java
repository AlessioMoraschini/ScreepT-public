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
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

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
		JComponent content = (JComponent) dialog.getContentPane();
		JScrollPane scroll = new JScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getHorizontalScrollBar().setUnitIncrement(16);
		dialog.setContentPane(scroll);

		Rectangle screen = screenOf(owner);
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(dialog.getGraphicsConfiguration() != null ? dialog.getGraphicsConfiguration() : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
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
		dialog.pack();
		dialog.setMinimumSize(new Dimension(Math.min(520, dialog.getWidth()), Math.min(360, dialog.getHeight())));
		dialog.setLocationRelativeTo(owner);
	}

	private static Rectangle screenOf(Window owner) {
		GraphicsConfiguration gc = owner != null ? owner.getGraphicsConfiguration() : null;
		if (gc != null) {
			return gc.getBounds();
		}
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
	}

	/** Brings a window in front of the others and gives it the focus, also when it is minimized or behind other programs */
	public static void bringToFront(Window window) {
		if (window instanceof Frame && ((Frame) window).getState() == Frame.ICONIFIED) {
			((Frame) window).setState(Frame.NORMAL);
		}
		window.setVisible(true);
		boolean wasOnTop = window.isAlwaysOnTop();
		window.setAlwaysOnTop(true);
		window.toFront();
		window.requestFocus();
		window.setAlwaysOnTop(wasOnTop);
	}
}
