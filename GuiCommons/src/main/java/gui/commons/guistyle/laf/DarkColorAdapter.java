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
package gui.commons.guistyle.laf;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;

import com.formdev.flatlaf.FlatLaf;

/**
 * Makes colors hardcoded in the application readable while a dark LAF is active:
 * - light neutral backgrounds set by code (white, light grays) are replaced by the matching theme background;
 * - text with too low contrast against its (effective) background gets the theme or a black/white foreground.
 * Saturated backgrounds (red, yellow buttons...) are kept. Replaced colors are UIResources, so a LAF switch
 * resets them; original colors are restored when a light LAF is set again.
 * Windows opened later and components added to visible windows are adapted automatically.
 */
public final class DarkColorAdapter {

	/** Client property (Boolean.TRUE) excluding a component from color adaptation */
	public static final String EXCLUDE_PROPERTY = "screept.darkColors.exclude";

	private static final String ORIGINAL_BACKGROUND = "screept.darkColors.originalBackground";
	private static final String ORIGINAL_FOREGROUND = "screept.darkColors.originalForeground";
	/** Original foreground was a LAF color: nothing to restore */
	private static final Object LAF_FOREGROUND = new Object();

	private static final double MIN_CONTRAST = 3.0;
	private static final float MAX_NEUTRAL_SATURATION = 0.2f;
	private static final float MIN_LIGHT_BRIGHTNESS = 0.6f;
	private static final Color DARK_TEXT = new Color(0x1E1F22);

	private static boolean installed;

	private DarkColorAdapter() {
	}

	static synchronized void install() {

		if (installed) {
			return;
		}
		installed = true;

		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (!isDarkLafActive()) {
				return;
			}
			if (event.getID() == WindowEvent.WINDOW_OPENED) {
				adaptTree(((WindowEvent) event).getWindow());
			} else if (event.getID() == ContainerEvent.COMPONENT_ADDED) {
				ContainerEvent containerEvent = (ContainerEvent) event;
				if (containerEvent.getContainer().isShowing()) {
					adaptTree(containerEvent.getChild());
				}
			}
		}, AWTEvent.WINDOW_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);

		// after the LAF switch has updated all the windows
		UIManager.addPropertyChangeListener(event -> {
			if ("lookAndFeel".equals(event.getPropertyName())) {
				SwingUtilities.invokeLater(DarkColorAdapter::refreshAllWindows);
			}
		});
	}

	public static boolean isDarkLafActive() {
		return UIManager.getLookAndFeel() instanceof FlatLaf && ((FlatLaf) UIManager.getLookAndFeel()).isDark();
	}

	private static void refreshAllWindows() {
		boolean dark = isDarkLafActive();
		for (Window window : Window.getWindows()) {
			if (dark) {
				adaptTree(window);
			} else {
				restoreTree(window);
			}
			window.repaint();
		}
	}

	private static void adaptTree(Component component) {
		if (component instanceof JComponent && !isExcluded((JComponent) component)) {
			adaptBackground((JComponent) component);
			adaptForeground((JComponent) component);
		}
		forEachChild(component, DarkColorAdapter::adaptTree);
	}

	private static void restoreTree(Component component) {
		if (component instanceof JComponent) {
			restore((JComponent) component);
		}
		forEachChild(component, DarkColorAdapter::restoreTree);
	}

	private static void forEachChild(Component component, java.util.function.Consumer<Component> action) {
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				action.accept(child);
			}
		}
		// menu items are children of the popup, not of the menu
		if (component instanceof JMenu) {
			action.accept(((JMenu) component).getPopupMenu());
		}
	}

	private static void adaptBackground(JComponent component) {

		if (!component.isBackgroundSet()) {
			return;
		}
		Color background = component.getBackground();

		if (!(background instanceof UIResource)) {
			// set by code: light neutral colors are replaced, anything else is a deliberate color
			if (!isLightNeutral(background)) {
				component.putClientProperty(ORIGINAL_BACKGROUND, null);
				return;
			}
			component.putClientProperty(ORIGINAL_BACKGROUND, background);
		} else if (component.getClientProperty(ORIGINAL_BACKGROUND) == null) {
			return;
		}

		Color themed = themeBackground(component);
		if (themed != null && !themed.equals(background)) {
			component.setBackground(new ColorUIResource(themed));
		}
	}

	private static void adaptForeground(JComponent component) {

		Color foreground = component.getForeground();
		Color background = effectiveBackground(component);
		if (foreground == null || background == null || contrast(foreground, background) >= MIN_CONTRAST) {
			return;
		}

		if (component.getClientProperty(ORIGINAL_FOREGROUND) == null) {
			component.putClientProperty(ORIGINAL_FOREGROUND, foreground instanceof UIResource ? LAF_FOREGROUND : foreground);
		}
		component.setForeground(new ColorUIResource(readableForeground(background)));
	}

	private static void restore(JComponent component) {

		Object background = component.getClientProperty(ORIGINAL_BACKGROUND);
		if (background instanceof Color && component.getBackground() instanceof UIResource) {
			component.setBackground((Color) background);
		}
		component.putClientProperty(ORIGINAL_BACKGROUND, null);

		Object foreground = component.getClientProperty(ORIGINAL_FOREGROUND);
		if (foreground instanceof Color && component.getForeground() instanceof UIResource) {
			component.setForeground((Color) foreground);
		}
		component.putClientProperty(ORIGINAL_FOREGROUND, null);
	}

	private static Color themeBackground(JComponent component) {
		String key = "Panel.background";
		if (component instanceof JTextComponent || component instanceof JList || component instanceof JTable
				|| component instanceof JTree || component instanceof JSpinner || component instanceof JComboBox) {
			key = "TextField.background";
		} else if (component instanceof JMenuItem || component instanceof JPopupMenu) {
			key = "PopupMenu.background";
		} else if (component instanceof JMenuBar) {
			key = "MenuBar.background";
		} else if (component instanceof AbstractButton) {
			key = "Button.background";
		}
		Color color = UIManager.getColor(key);
		return color != null ? color : UIManager.getColor("Panel.background");
	}

	/** Background really painted behind the component: its own if opaque, else the first opaque ancestor's one */
	private static Color effectiveBackground(Component component) {
		for (Component current = component; current != null; current = current.getParent()) {
			if (current.isOpaque() || current instanceof Window) {
				return current.getBackground();
			}
		}
		return null;
	}

	private static Color readableForeground(Color background) {
		Color themeText = UIManager.getColor("Label.foreground");
		if (themeText != null && contrast(themeText, background) >= MIN_CONTRAST) {
			return themeText;
		}
		return contrast(Color.WHITE, background) >= contrast(DARK_TEXT, background) ? Color.WHITE : DARK_TEXT;
	}

	private static boolean isLightNeutral(Color color) {
		if (color.getAlpha() < 200) {
			return false;
		}
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		return hsb[1] <= MAX_NEUTRAL_SATURATION && hsb[2] >= MIN_LIGHT_BRIGHTNESS;
	}

	private static boolean isExcluded(JComponent component) {
		return Boolean.TRUE.equals(component.getClientProperty(EXCLUDE_PROPERTY))
				|| component.getClass().getName().startsWith("org.fife.");
	}

	/** WCAG contrast ratio (1..21) */
	private static double contrast(Color first, Color second) {
		double l1 = luminance(first);
		double l2 = luminance(second);
		return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
	}

	private static double luminance(Color color) {
		return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
	}

	private static double linear(int channel) {
		double value = channel / 255.0;
		return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
	}
}
