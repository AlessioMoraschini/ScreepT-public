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
package gui.commons.frameutils.frame.arch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

/**
 * Shared look of the small tool windows (hash checker, color picker, password generator, file properties...):
 * the same language of the Options and Plugin Manager windows. Every color comes from the look and feel, so the tools
 * stay readable with dark and light themes; fonts are derived from the LAF font, so they follow the interface scale.
 */
public final class ToolStyle {

	/** Main / positive action and "match" state (same green of the Plugin Manager) */
	public static final Color GREEN = new Color(61, 155, 92);
	/** Dangerous action and "mismatch" state */
	public static final Color RED = new Color(201, 72, 66);
	/** Informative accent */
	public static final Color BLUE = new Color(58, 124, 214);
	/** Warning accent */
	public static final Color ORANGE = new Color(214, 140, 38);

	private static final String[] MONO_FAMILIES = {"Cascadia Mono", "JetBrains Mono", "Consolas", "DejaVu Sans Mono", "Menlo", "Monaco"};
	private static String monoFamily;

	private ToolStyle() {
	}

	/** LAF label font with the given size delta and style */
	public static Font font(float delta, int style) {
		Font base = UIManager.getFont("Label.font");
		if (base == null) {
			base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
		}
		return base.deriveFont(style, base.getSize2D() + delta);
	}

	/** Monospaced font (for hashes, color codes, passwords), as big as the LAF font plus the given delta */
	public static Font mono(float delta) {
		Font base = font(delta, Font.PLAIN);
		return new Font(monoFamily(), Font.PLAIN, Math.round(base.getSize2D()));
	}

	private static synchronized String monoFamily() {
		if (monoFamily == null) {
			monoFamily = Font.MONOSPACED;
			try {
				Set<String> available = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
				for (String family : MONO_FAMILIES) {
					if (available.contains(family)) {
						monoFamily = family;
						break;
					}
				}
			} catch (Throwable e) {
				// headless or broken font configuration: logical font
			}
		}
		return monoFamily;
	}

	/** Secondary text color (descriptions, hints) */
	public static Color muted() {
		Color color = UIManager.getColor("Label.disabledForeground");
		return color != null ? color : Color.GRAY;
	}

	/** True when the current look and feel has a dark background */
	public static boolean isDark() {
		Color background = UIManager.getColor("Panel.background");
		return background != null && luminance(background) < 0.4;
	}

	/** Title and description of a tool window, as in the Options windows */
	public static JPanel header(String title, String description) {
		JPanel header = new JPanel(new MigLayout("wrap 1, insets 0, gapy 2", "[grow,fill]"));
		header.setOpaque(false);
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(font(8f, Font.BOLD));
		header.add(titleLabel);
		if (description != null && !description.isEmpty()) {
			header.add(mutedLabel(description));
		}
		return header;
	}

	/** Small bold caption over a group of controls */
	public static JLabel caption(String text) {
		JLabel label = new JLabel(text);
		label.setFont(font(1f, Font.BOLD));
		return label;
	}

	public static JLabel mutedLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(font(1f, Font.PLAIN));
		label.setForeground(muted());
		return label;
	}

	/** Colored main action button with white text (green = do it, red = dangerous) */
	public static <B extends AbstractButton> B accent(B button, Color background) {
		button.putClientProperty("JButton.buttonType", "roundRect");
		button.setBackground(background);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return button;
	}

	/** Normal button with an icon drawn with its own text color */
	public static JButton button(String text, ToolIcons.Kind icon, String tooltip) {
		JButton button = new JButton(text);
		if (icon != null) {
			button.setIcon(ToolIcons.of(icon, 16));
			button.setIconTextGap(6);
		}
		button.setToolTipText(tooltip);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return button;
	}

	/** Borderless icon only button, as the toolbar ones */
	public static JButton iconButton(ToolIcons.Kind icon, String tooltip) {
		JButton button = new JButton(ToolIcons.of(icon, 16));
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		button.setToolTipText(tooltip);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setFocusable(true);
		return button;
	}

	/** Rounded panel slightly lighter/darker than the window: groups related controls */
	public static JPanel card(LayoutManager layout) {
		return new Card(layout);
	}

	/** Pill shaped status label: white text on a colored background, or muted text when the color is null */
	public static Badge badge() {
		return new Badge();
	}

	/** Runs the action when the key is pressed anywhere in the window */
	public static void bindKey(RootPaneContainer window, KeyStroke key, String name, Runnable action) {
		JComponent root = window.getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
		root.getActionMap().put(name, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	public static void copyToClipboard(String text) {
		if (text == null) {
			return;
		}
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
		} catch (Exception e) {
			// clipboard busy (another application holds it): nothing to do
		}
	}

	/** Text in the system clipboard, or null */
	public static String clipboardText() {
		try {
			Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if (content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				return (String) content.getTransferData(DataFlavor.stringFlavor);
			}
		} catch (Exception e) {
			// clipboard busy or not text
		}
		return null;
	}

	/** Human readable size: 0 B, 812 B, 1.4 MB, 3.21 GB */
	public static String size(long bytes) {
		if (bytes < 1024) {
			return Math.max(0, bytes) + " B";
		}
		String[] units = {"kB", "MB", "GB", "TB", "PB"};
		double value = bytes / 1024.0;
		int unit = 0;
		while (value >= 1024 && unit < units.length - 1) {
			value /= 1024;
			unit++;
		}
		return new java.text.DecimalFormat(value < 10 ? "0.##" : value < 100 ? "0.#" : "0").format(value) + " " + units[unit];
	}

	public static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/** Mix of two colors, ratio 0 = first, 1 = second */
	public static Color blend(Color first, Color second, float ratio) {
		float inverse = 1f - ratio;
		return new Color(
				Math.round(first.getRed() * inverse + second.getRed() * ratio),
				Math.round(first.getGreen() * inverse + second.getGreen() * ratio),
				Math.round(first.getBlue() * inverse + second.getBlue() * ratio));
	}

	/** WCAG relative luminance (0..1) */
	public static double luminance(Color color) {
		return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
	}

	/** WCAG contrast ratio (1..21) */
	public static double contrast(Color first, Color second) {
		double l1 = luminance(first);
		double l2 = luminance(second);
		return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
	}

	private static double linear(int channel) {
		double value = channel / 255.0;
		return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
	}

	/** Background of the cards: the window background moved a bit towards the text color */
	static Color cardBackground() {
		Color background = UIManager.getColor("Panel.background");
		Color foreground = UIManager.getColor("Label.foreground");
		if (background == null || foreground == null) {
			return new Color(128, 128, 128, 20);
		}
		return blend(background, foreground, isDark() ? 0.05f : 0.035f);
	}

	static Color cardBorder() {
		Color border = UIManager.getColor("Component.borderColor");
		if (border == null) {
			border = UIManager.getColor("Separator.foreground");
		}
		return border != null ? border : Color.GRAY;
	}

	/** Rounded group panel, colors read at paint time so a theme switch is followed */
	public static final class Card extends JPanel {
		private static final long serialVersionUID = 1L;

		Card(LayoutManager layout) {
			super(layout);
			setOpaque(false);
			setBorder(new EmptyBorder(10, 12, 10, 12));
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int arc = 14;
				g.setColor(cardBackground());
				g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
				g.setColor(cardBorder());
				g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
			} finally {
				g.dispose();
			}
			super.paintComponent(g0);
		}
	}

	/** Pill status label */
	public static final class Badge extends JLabel {
		private static final long serialVersionUID = 1L;
		private Color pill;

		Badge() {
			setOpaque(false);
			setFont(font(1f, Font.BOLD));
			setBorder(new EmptyBorder(4, 10, 4, 12));
			setHorizontalAlignment(SwingConstants.LEFT);
			setIconTextGap(6);
		}

		/** Shows the text on a colored pill (null color: plain muted text, no pill) */
		public void setStatus(String text, Icon icon, Color color) {
			pill = color;
			setText(text);
			setIcon(icon);
			setForeground(color != null ? Color.WHITE : muted());
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g0) {
			if (pill != null) {
				Graphics2D g = (Graphics2D) g0.create();
				try {
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setColor(pill);
					g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
				} finally {
					g.dispose();
				}
			}
			super.paintComponent(g0);
		}
	}

	/** Makes the component show the hand cursor */
	public static <C extends Component> C hand(C component) {
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return component;
	}
}
