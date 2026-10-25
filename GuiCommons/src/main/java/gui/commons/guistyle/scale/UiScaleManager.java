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
package gui.commons.guistyle.scale;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;

import org.apache.log4j.Logger;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.UIScale;

import initializer.configs.impl.om.GuiOption;

/**
 * Applies the interface size options (GuiOption.uiXXX) to every window, independently from the LAF:
 * - fonts are scaled per category (general, menu, toolbar) starting from each component's original font,
 *   so repeated applies never accumulate and LAF/app font changes are picked up as new originals;
 * - graphic elements are scaled through FlatLaf zoom when a FlatLaf LAF (ScreepT Dark) is active.
 * Text content areas (JTextArea, JEditorPane, RSyntaxTextArea) keep their own font settings.
 * Windows opened later and components added to visible windows are scaled automatically.
 */
public final class UiScaleManager {

	static Logger logger = Logger.getLogger(UiScaleManager.class);

	public enum Category { GENERAL, MENU, TOOLBAR }

	/** Client property (Category) forcing the category of a component and its children */
	public static final String CATEGORY_PROPERTY = "screept.uiScale.category";
	/** Client property (Boolean.TRUE) excluding a component from font scaling */
	public static final String EXCLUDE_PROPERTY = "screept.uiScale.exclude";

	private static final String BASE_FONT = "screept.uiScale.baseFont";
	private static final String APPLIED_FONT = "screept.uiScale.appliedFont";
	private static final String BASE_ROW_HEIGHT = "screept.uiScale.baseRowHeight";
	private static final String APPLIED_ROW_HEIGHT = "screept.uiScale.appliedRowHeight";
	private static final int MIN_FONT_SIZE = 6;

	private static Settings settings = Settings.IDENTITY;
	private static boolean everApplied;
	private static boolean appliedOnce;
	private static boolean installed;

	private UiScaleManager() {
	}

	/**
	 * Applies the given options to all windows (only if changed) and keeps them for windows opened later.
	 * To be called on the EDT.
	 */
	public static synchronized void apply(GuiOption guiOpt) {

		install();

		// first apply happens at startup, on windows already sized by the app: no proportional resize
		boolean startup = !appliedOnce;
		appliedOnce = true;

		Settings newSettings = new Settings(guiOpt);
		if (newSettings.equals(settings)) {
			return;
		}
		float windowsRatio = startup ? 1f : newSettings.overall() / settings.overall();
		settings = newSettings;
		everApplied |= !newSettings.isIdentity();

		if (everApplied) {
			refreshAllWindows(windowsRatio);
		}
	}

	private static void install() {

		if (installed) {
			return;
		}
		installed = true;

		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (!everApplied) {
				return;
			}
			if (event.getID() == WindowEvent.WINDOW_OPENED) {
				Window window = ((WindowEvent) event).getWindow();
				scaleTree(window, null);
				fitToContent(window);
			} else if (event.getID() == ContainerEvent.COMPONENT_ADDED) {
				// components added while building a window are handled on WINDOW_OPENED, after the app set their fonts
				ContainerEvent containerEvent = (ContainerEvent) event;
				if (containerEvent.getContainer().isShowing()) {
					scaleTree(containerEvent.getChild(), inheritedCategory(containerEvent.getContainer()));
				}
			}
		}, AWTEvent.WINDOW_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);

		// LAF switch resets fonts and zoom: re-apply after the windows have been updated
		UIManager.addPropertyChangeListener(event -> {
			if ("lookAndFeel".equals(event.getPropertyName()) && everApplied) {
				SwingUtilities.invokeLater(() -> refreshAllWindows(1f));
			}
		});
	}

	/**
	 * @param windowsRatio size change to apply to the visible windows (1 = keep their size)
	 */
	private static void refreshAllWindows(float windowsRatio) {

		if (isFlatLafActive() && UIScale.getZoomFactor() != settings.zoom) {
			// FlatLaf updates all windows UI, fonts are scaled after that
			UIScale.setZoomFactor(settings.zoom);
			SwingUtilities.invokeLater(() -> scaleAllWindows(windowsRatio));
		} else {
			scaleAllWindows(windowsRatio);
		}
	}

	private static void scaleAllWindows(float windowsRatio) {
		long start = System.currentTimeMillis();
		for (Window window : Window.getWindows()) {
			scaleTree(window, null);
			if (window.isShowing()) {
				resizeWindow(window, windowsRatio);
			}
		}
		logger.debug("Interface size applied in " + (System.currentTimeMillis() - start) + " ms");
	}

	/**
	 * Scales the window size by the given ratio (at least enough to show its content), keeping its center
	 * and keeping it inside its screen. Maximized frames and popups are only re-laid out.
	 */
	private static void resizeWindow(Window window, float ratio) {

		boolean maximized = window instanceof Frame && (((Frame) window).getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
		if (maximized || !(window instanceof Frame || window instanceof Dialog)) {
			window.validate();
			return;
		}

		Rectangle bounds = window.getBounds();
		Dimension preferred = window.getPreferredSize();
		int width = Math.max(Math.round(bounds.width * ratio), ratio > 1 ? preferred.width : 0);
		int height = Math.max(Math.round(bounds.height * ratio), ratio > 1 ? preferred.height : 0);
		setBoundsInScreen(window, bounds, width, height);
	}

	/** Grows a just opened window when its scaled content does not fit */
	private static void fitToContent(Window window) {
		if (settings.overall() <= 1f || !(window instanceof Frame || window instanceof Dialog)
				|| (window instanceof Frame && (((Frame) window).getExtendedState() & Frame.MAXIMIZED_BOTH) != 0)) {
			return;
		}
		Rectangle bounds = window.getBounds();
		Dimension preferred = window.getPreferredSize();
		if (preferred.width > bounds.width || preferred.height > bounds.height) {
			setBoundsInScreen(window, bounds, Math.max(bounds.width, preferred.width), Math.max(bounds.height, preferred.height));
		}
	}

	private static void setBoundsInScreen(Window window, Rectangle bounds, int width, int height) {

		GraphicsConfiguration config = window.getGraphicsConfiguration();
		Rectangle screen = config.getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
		Rectangle usable = new Rectangle(screen.x + insets.left, screen.y + insets.top,
				screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom);

		int newWidth = Math.min(width, usable.width);
		int newHeight = Math.min(height, usable.height);
		int x = bounds.x + (bounds.width - newWidth) / 2;
		int y = bounds.y + (bounds.height - newHeight) / 2;
		x = Math.max(usable.x, Math.min(x, usable.x + usable.width - newWidth));
		y = Math.max(usable.y, Math.min(y, usable.y + usable.height - newHeight));

		window.setBounds(x, y, newWidth, newHeight);
		window.validate();
	}

	private static void scaleTree(Component component, Category inherited) {

		Category category = categoryOf(component, inherited);
		if (component instanceof JComponent) {
			scaleComponent((JComponent) component, category);
		}
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				scaleTree(child, category);
			}
		}
		// menu items are children of the popup, not of the menu
		if (component instanceof JMenu) {
			scaleTree(((JMenu) component).getPopupMenu(), Category.MENU);
		}
	}

	private static void scaleComponent(JComponent component, Category category) {

		// inherited fonts follow their parent
		if (!component.isFontSet() || isExcluded(component)) {
			return;
		}

		Font current = component.getFont();
		Font base = (Font) component.getClientProperty(BASE_FONT);
		if (base == null || !current.equals(component.getClientProperty(APPLIED_FONT))) {
			// first time, or font changed by LAF/app since last apply: current one is the new original
			base = current;
			component.putClientProperty(BASE_FONT, base);
		}

		float factor = settings.fontFactor(category);
		if (base instanceof UIResource && isFlatLafActive()) {
			// LAF fonts are already zoomed by FlatLaf
			factor /= UIScale.getZoomFactor();
		}

		Font target = scaledFont(base, factor, settings.family);
		if (!target.equals(current)) {
			component.setFont(target);
		}
		component.putClientProperty(APPLIED_FONT, target);

		if (component instanceof JTable) {
			scaleRowHeight((JTable) component, target.getSize2D() / base.getSize2D());
		}
	}

	private static void scaleRowHeight(JTable table, float factor) {
		Integer base = (Integer) table.getClientProperty(BASE_ROW_HEIGHT);
		if (base == null || !Objects.equals(table.getRowHeight(), table.getClientProperty(APPLIED_ROW_HEIGHT))) {
			base = table.getRowHeight();
			table.putClientProperty(BASE_ROW_HEIGHT, base);
		}
		int target = Math.max(1, Math.round(base * factor));
		if (target != table.getRowHeight()) {
			table.setRowHeight(target);
		}
		table.putClientProperty(APPLIED_ROW_HEIGHT, target);
	}

	private static Font scaledFont(Font base, float factor, String family) {

		float size = Math.max(MIN_FONT_SIZE, Math.round(base.getSize2D() * factor));
		Font font = (family.isEmpty() || family.equals(base.getFamily()) || isMonospaced(base))
				? base.deriveFont(size)
				: new Font(family, base.getStyle(), Math.round(size));

		return (base instanceof UIResource) ? new FontUIResource(font) : font;
	}

	private static boolean isMonospaced(Font font) {
		String family = font.getFamily().toLowerCase();
		return family.contains("mono") || family.contains("consol") || family.contains("courier");
	}

	private static boolean isExcluded(JComponent component) {
		return Boolean.TRUE.equals(component.getClientProperty(EXCLUDE_PROPERTY))
				|| component instanceof JTextArea
				|| component instanceof JEditorPane
				|| component.getClass().getName().startsWith("org.fife.");
	}

	private static Category categoryOf(Component component, Category inherited) {
		if (component instanceof JComponent) {
			Object forced = ((JComponent) component).getClientProperty(CATEGORY_PROPERTY);
			if (forced instanceof Category) {
				return (Category) forced;
			}
		}
		if (component instanceof JMenuItem || component instanceof JMenuBar || component instanceof JPopupMenu) {
			return Category.MENU;
		}
		if (component instanceof JToolBar) {
			return Category.TOOLBAR;
		}
		return inherited == null ? Category.GENERAL : inherited;
	}

	private static Category inheritedCategory(Container container) {
		for (Component current = container; current != null; current = current.getParent()) {
			Category category = categoryOf(current, null);
			if (category != Category.GENERAL) {
				return category;
			}
		}
		return Category.GENERAL;
	}

	private static boolean isFlatLafActive() {
		return UIManager.getLookAndFeel() instanceof FlatLaf;
	}

	private static final class Settings {

		static final Settings IDENTITY = new Settings(100, 100, 100, 100, "");

		final float general;
		final float menu;
		final float toolbar;
		final float zoom;
		final String family;

		Settings(GuiOption guiOpt) {
			this(guiOpt.getUiGeneralFontScale(), guiOpt.getUiMenuFontScale(), guiOpt.getUiToolbarFontScale(),
					guiOpt.getUiComponentScale(), guiOpt.getUiFontFamily());
		}

		Settings(int general, int menu, int toolbar, int component, String family) {
			this.general = general / 100f;
			this.menu = menu / 100f;
			this.toolbar = toolbar / 100f;
			this.zoom = component / 100f;
			this.family = family == null ? "" : family;
		}

		float fontFactor(Category category) {
			switch (category) {
				case MENU: return menu;
				case TOOLBAR: return toolbar;
				default: return general;
			}
		}

		/** Overall size of the interface: fonts, and graphic elements when FlatLaf zoom is effective */
		float overall() {
			return isFlatLafActive() ? Math.max(general, zoom) : general;
		}

		boolean isIdentity() {
			return equals(IDENTITY);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Settings)) {
				return false;
			}
			Settings other = (Settings) obj;
			return general == other.general && menu == other.menu && toolbar == other.toolbar
					&& zoom == other.zoom && family.equals(other.family);
		}

		@Override
		public int hashCode() {
			return Objects.hash(general, menu, toolbar, zoom, family);
		}
	}
}
