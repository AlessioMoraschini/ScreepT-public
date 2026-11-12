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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsDevice.WindowTranslucency;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import various.common.light.utility.log.SafeLogger;

/**
 * Easter egg: stay on a page for a while and Feo and Nano come to play on the window: they chase a ball of yarn, steal it
 * from each other, and after a while Nano follows Feo and both walk out of the screen. Happens once each time the page
 * is shown and stays for the given time; leaving the page, minimizing or closing the window sends the cats away at once.
 * The overlay is a transparent window: the mouse goes through the empty parts.
 */
public final class CatsVisit {

	private static final SafeLogger logger = new SafeLogger(CatsVisit.class);
	private static final int TICK_MS = 33;
	private static final int FLOOR_DEPTH = 110;
	private static final int FLOOR_MARGIN = 34;

	private final JComponent page;
	private final int secondsBefore;
	private final java.util.function.Supplier<Rectangle> avoidSupplier;
	private final Random random = new Random();
	private Timer waiting;
	private Timer animation;
	private JWindow overlay;
	private CatsPlayground playground;

	private CatsVisit(JComponent page, int secondsBefore, java.util.function.Supplier<Rectangle> avoidSupplier) {
		this.page = page;
		this.secondsBefore = secondsBefore;
		this.avoidSupplier = avoidSupplier;
	}

	/**
	 * Starts watching the page: when it has been visible for {@code secondsBefore} seconds the cats arrive.
	 * Nothing happens if the system has no transparent windows.
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
		if (overlay != null || owner == null || !owner.isShowing() || !owner.isActive() || (owner instanceof java.awt.Frame && ((java.awt.Frame) owner).getState() == java.awt.Frame.ICONIFIED)) {
			return;
		}
		GraphicsConfiguration configuration = owner.getGraphicsConfiguration();
		GraphicsDevice device = configuration != null ? configuration.getDevice() : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		if (!device.isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
			logger.info("Cats visit skipped: transparent windows not supported");
			return;
		}
		try {
			final Rectangle area = owner.getBounds();
			playground = new CatsPlayground(3);
			playground.setArea(0, area.width, FLOOR_DEPTH);
			playground.leaveAfter(900 + random.nextInt(450)); // 30 to 45 seconds of play
			// owned by the ScreepT window and not "always on top": it stays over ScreepT only, never over other applications
			overlay = new JWindow(owner);
			overlay.setBounds(area);
			overlay.setBackground(new Color(0, 0, 0, 0));
			overlay.setFocusableWindowState(false);
			overlay.setAlwaysOnTop(false);
			overlay.setContentPane(new JComponent() {
				private static final long serialVersionUID = 1L;

				@Override
				protected void paintComponent(Graphics g) {
					playground.paint((Graphics2D) g, getHeight() - FLOOR_MARGIN);
				}
			});
			overlay.setVisible(true);
			animation = new Timer(TICK_MS, e -> step());
			animation.start();
		} catch (RuntimeException e) {
			logger.warn("Cats visit not started", e);
			endVisit();
		}
	}

	private void step() {
		Window owner = SwingUtilities.getWindowAncestor(page);
		if (overlay == null || owner == null || !page.isShowing() || !owner.isShowing()
				|| (owner instanceof java.awt.Frame && ((java.awt.Frame) owner).getState() == java.awt.Frame.ICONIFIED)) {
			endVisit();
			return;
		}
		// ScreepT is not the application in front (another window, another program): the cats wait out of sight
		boolean inFront = owner.isActive();
		if (inFront != overlay.isVisible()) {
			overlay.setVisible(inFront);
		}
		Rectangle bounds = owner.getBounds();
		if (!bounds.equals(overlay.getBounds())) { // the window was moved or resized: the floor follows it
			overlay.setBounds(bounds);
			playground.setArea(0, bounds.width, FLOOR_DEPTH);
		}
		updateForbiddenZone();
		playground.tick();
		overlay.repaint();
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
			java.awt.Point pageOrigin = page.getLocationOnScreen();
			java.awt.Point overlayOrigin = overlay.getLocationOnScreen();
			playground.setAvoid(new Rectangle(pageOrigin.x - overlayOrigin.x + zone.x, pageOrigin.y - overlayOrigin.y + zone.y, zone.width, zone.height),
					overlay.getHeight() - FLOOR_MARGIN);
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
			overlay.dispose();
			overlay = null;
		}
		playground = null;
	}
}
