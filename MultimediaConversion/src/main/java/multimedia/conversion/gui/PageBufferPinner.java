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
package multimedia.conversion.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.icepdf.ri.common.views.AbstractPageViewComponent;

/**
 * ICEpdf keeps the rendered image of a page only through a soft reference: when the garbage collector clears it (after
 * some time in another tab or application) the page turns blank and is rendered again. This keeps a strong reference to
 * the images of the pages seen last, within a memory budget shared by all the viewers. Used on the event thread only.
 */
class PageBufferPinner {

	private static final Logger logger = Logger.getLogger(PageBufferPinner.class);

	private static final long MB = 1024L * 1024;
	private static final long BUDGET_BYTES = budgetFor(Runtime.getRuntime().maxMemory());
	private static final int PERIOD_MS = 300;

	/** Page component -> its image, least recently seen first */
	private static final Map<Component, BufferedImage> pinned = new LinkedHashMap<>(16, 0.75f, true);
	private final Component root;
	private final Timer timer;
	private Field storeField;
	private Method imageGetter;

	/** 48 MB, or 20% of the maximum heap when this exceeds 700 MB; never more than 25% of it */
	static long budgetFor(long maxHeap) {
		long budget = maxHeap > 700 * MB ? maxHeap / 5 : 48 * MB;
		return Math.min(budget, maxHeap / 4);
	}

	PageBufferPinner(Component root) {
		this.root = root;
		timer = new Timer(PERIOD_MS, e -> pinVisiblePages());
	}

	void start() {
		timer.start();
	}

	void stop() {
		timer.stop();
		pinned.keySet().removeIf(page -> SwingUtilities.isDescendingFrom(page, root));
	}

	private void pinVisiblePages() {
		if (!root.isShowing()) {
			return; // hidden tab: what was pinned stays
		}
		try {
			pinned.keySet().removeIf(page -> !page.isDisplayable()); // pages of documents closed or reopened
			pin(root);
			trim();
		} catch (Exception e) {
			logger.warn("Cannot keep the rendered pages, the viewer renders them again when needed: " + e.getMessage());
			stop();
		}
	}

	private void pin(Component component) throws Exception {
		if (component instanceof AbstractPageViewComponent) {
			if (!((AbstractPageViewComponent) component).getVisibleRect().isEmpty()) {
				BufferedImage image = imageOf((AbstractPageViewComponent) component);
				if (image != null) {
					pinned.put(component, image);
				}
			}
		} else if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				pin(child);
			}
		}
	}

	private BufferedImage imageOf(AbstractPageViewComponent page) throws Exception {
		if (storeField == null) {
			storeField = AbstractPageViewComponent.class.getDeclaredField("pageBufferStore");
			storeField.setAccessible(true);
		}
		Object store = storeField.get(page);
		if (imageGetter == null) {
			imageGetter = store.getClass().getMethod("getImageReference");
		}
		return (BufferedImage) imageGetter.invoke(store);
	}

	/** Drops the pages not seen for longest once over budget; the most recent one always stays */
	private void trim() {
		long total = 0;
		for (BufferedImage image : pinned.values()) {
			total += 4L * image.getWidth() * image.getHeight();
		}
		Iterator<BufferedImage> eldestFirst = pinned.values().iterator();
		while (total > BUDGET_BYTES && pinned.size() > 1) {
			BufferedImage image = eldestFirst.next();
			total -= 4L * image.getWidth() * image.getHeight();
			eldestFirst.remove();
		}
	}
}
