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
package various.common.light.easteregg.cats;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import various.common.light.utility.log.SafeLogger;

/**
 * Easter egg: stay on a page for a while and Feo and Nano come to play on the window: they chase a ball of yarn, steal it
 * from each other, and after a while Nano follows Feo and both walk out of the screen. Happens once each time the page
 * is shown and stays for the given time; leaving the page, minimizing or closing the window sends the cats away at once.
 * The cats are drawn on a layer of the window above the page, in the same drawing buffer of the page (no transparent window above it,
 * which flickers on some systems); the mouse goes through it.
 */
public final class CatsVisit {

	private static final SafeLogger logger = new SafeLogger(CatsVisit.class);
	private static final int TICK_MS = 33;
	private static final int FLOOR_DEPTH = 110;
	private static final int FLOOR_MARGIN = 34;
	/** Height, from the bottom, of the band that is painted again at each step: floor, highest jump, cat and its balloon */
	private static final int BAND_HEIGHT = FLOOR_DEPTH + FLOOR_MARGIN + 190;

	private final JComponent page;
	private final int secondsBefore;
	private final java.util.function.Supplier<Rectangle> avoidSupplier;
	private final Random random = new Random();
	private Timer waiting;
	private Timer animation;
	private JComponent overlay;
	private JRootPane host;
	private CatsPlayground playground;

	private CatsVisit(JComponent page, int secondsBefore, java.util.function.Supplier<Rectangle> avoidSupplier) {
		this.page = page;
		this.secondsBefore = secondsBefore;
		this.avoidSupplier = avoidSupplier;
	}

	/**
	 * Starts watching the page: when it has been visible for {@code secondsBefore} seconds the cats arrive.
	 */
	public static void attach(final JComponent page, int secondsBefore) {
		attach(page, secondsBefore, null);
	}

	/**
	 * Same, with a zone of the page the cats never cover (for instance the text being typed): {@code avoid} gives its
	 * rectangle in the coordinates of the page, or null when there is nothing to protect. The cats stay beside it and now
	 * and then one touches it with a paw and goes away.
	 */
	public static void attach(final JComponent page, int secondsBefore, java.util.function.Supplier<Rectangle> avoid) {
		final CatsVisit visit = new CatsVisit(page, Math.max(1, secondsBefore), avoid);
		page.addHierarchyListener(e -> {
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
				if (page.isShowing()) {
					visit.pageShown();
				} else {
					visit.pageHidden();
				}
			}
		});
		if (page.isShowing()) {
			visit.pageShown();
		}
	}

	private void pageShown() {
		pageHidden();
		waiting = new Timer(secondsBefore * 1000, e -> startVisit());
		waiting.setRepeats(false);
		waiting.start();
	}

	private void pageHidden() {
		if (waiting != null) {
			waiting.stop();
			waiting = null;
		}
		endVisit();
	}

	private void startVisit() {
		Window owner = SwingUtilities.getWindowAncestor(page);
		if (overlay != null || !(owner instanceof RootPaneContainer) || !owner.isShowing() || !owner.isActive() || isIconified(owner)) {
			return;
		}
		try {
			host = ((RootPaneContainer) owner).getRootPane();
			playground = new CatsPlayground(3);
			playground.setArea(0, host.getWidth(), FLOOR_DEPTH);
			playground.leaveAfter(900 + random.nextInt(450)); // 30 to 45 seconds of play
			overlay = new JComponent() {
				private static final long serialVersionUID = 1L;

				@Override
				protected void paintComponent(Graphics g) {
					if (playground != null) {
						playground.paint((Graphics2D) g, getHeight() - FLOOR_MARGIN);
					}
				}

				/** Never under the mouse: clicks and cursor belong to what is below */
				@Override
				public boolean contains(int x, int y) {
					return false;
				}
			};
			overlay.setOpaque(false);
			overlay.setBounds(0, 0, host.getLayeredPane().getWidth(), host.getLayeredPane().getHeight());
			// below menus and popups; the glass pane is left to the wait cursor and the drag of the tabs
			host.getLayeredPane().add(overlay, JLayeredPane.PALETTE_LAYER);
			animation = new Timer(TICK_MS, e -> step());
			animation.start();
		} catch (RuntimeException e) {
			logger.warn("Cats visit not started", e);
			endVisit();
		}
	}

	private static boolean isIconified(Window window) {
		return window instanceof java.awt.Frame && (((java.awt.Frame) window).getExtendedState() & java.awt.Frame.ICONIFIED) != 0;
	}

	private void step() {
		Window owner = SwingUtilities.getWindowAncestor(page);
		if (overlay == null || owner == null || !page.isShowing() || !owner.isShowing() || isIconified(owner) || overlay.getParent() == null) {
			endVisit();
			return;
		}
		// ScreepT is not the application in front (another window, another program): the cats wait out of sight
		boolean inFront = owner.isActive();
		if (inFront != overlay.isVisible()) {
			overlay.setVisible(inFront);
		}
		Component layers = overlay.getParent();
		if (overlay.getWidth() != layers.getWidth() || overlay.getHeight() != layers.getHeight()) {
			overlay.setBounds(0, 0, layers.getWidth(), layers.getHeight());
		}
		playground.setArea(0, overlay.getWidth(), FLOOR_DEPTH); // the floor follows the size of the window
		updateForbiddenZone();
		playground.tick();
		overlay.repaint(0, overlay.getHeight() - BAND_HEIGHT, overlay.getWidth(), BAND_HEIGHT);
		if (playground.isFinished()) {
			endVisit();
		}
	}

	/** Hands the playground the rectangle of the text to protect, converted to the coordinates of the overlay */
	private void updateForbiddenZone() {
		if (avoidSupplier == null) {
			return;
		}
		try {
			Rectangle zone = avoidSupplier.get();
			if (zone == null || zone.isEmpty()) {
				playground.setAvoid(null, overlay.getHeight() - FLOOR_MARGIN);
				return;
			}
			playground.setAvoid(SwingUtilities.convertRectangle(page, zone, overlay), overlay.getHeight() - FLOOR_MARGIN);
		} catch (RuntimeException e) {
			playground.setAvoid(null, overlay.getHeight() - FLOOR_MARGIN);
		}
	}

	private void endVisit() {
		if (animation != null) {
			animation.stop();
			animation = null;
		}
		if (overlay != null) {
			overlay.setVisible(false);
			if (host != null) {
				host.getLayeredPane().remove(overlay);
				host.repaint();
			}
			overlay = null;
			host = null;
		}
		playground = null;
	}
}
