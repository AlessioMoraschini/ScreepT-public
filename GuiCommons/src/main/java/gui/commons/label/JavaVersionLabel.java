/*
 * Copyright (c) A.M. Design And Development. All rights reserved.
 */
package gui.commons.label;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.management.ManagementFactory;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Small label showing the version of the Java in use. On hover a popup shows path and details of the running JVM:
 * the text is selectable and a button copies it all to the clipboard. The popup stays while the mouse is over the
 * label or over the popup, and closes shortly after the mouse leaves both (or with Esc).
 */
public class JavaVersionLabel extends JLabel {

	private static final long serialVersionUID = 4180264517403271154L;

	private static final float FONT_DELTA = -2f;
	private static final int CLOSE_CHECK_MS = 250;
	private static final int OPEN_DELAY_MS = 350;

	private JWindow popup;
	private final Timer closeTimer = new Timer(CLOSE_CHECK_MS, e -> closeIfMouseAway());
	private final Timer openTimer = new Timer(OPEN_DELAY_MS, e -> showPopup());

	public JavaVersionLabel() {
		super("Java " + System.getProperty("java.version", "?"), SwingConstants.RIGHT);
		setVerticalAlignment(SwingConstants.CENTER);
		setAlignmentY(CENTER_ALIGNMENT);
		setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
		openTimer.setRepeats(false);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				openTimer.restart();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				openTimer.stop();
			}
		});
		updateColors();
	}

	/** Smaller than the default label font and muted, also after a look and feel switch */
	@Override
	public void updateUI() {
		super.updateUI();
		java.awt.Font base = UIManager.getFont("Label.font");
		if (base != null) {
			setFont(new FontUIResource(base.deriveFont(Math.max(9f, base.getSize2D() + FONT_DELTA))));
		}
		updateColors();
	}

	private void updateColors() {
		Color muted = UIManager.getColor("Label.disabledForeground");
		if (muted != null) {
			setForeground(muted);
		}
	}

	public void stop() {
		openTimer.stop();
		closeTimer.stop();
		hidePopup();
	}

	/** Plain text with all the details of the JVM in use */
	public static String buildDetails() {
		StringBuilder sb = new StringBuilder();
		line(sb, "Java version", System.getProperty("java.version"));
		line(sb, "Java home", System.getProperty("java.home"));
		line(sb, "Vendor", System.getProperty("java.vendor"));
		line(sb, "Runtime", System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version"));
		line(sb, "VM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " (" + System.getProperty("java.vm.info", "") + ")");
		line(sb, "Specification", System.getProperty("java.specification.version"));
		line(sb, "Class file version", System.getProperty("java.class.version"));
		line(sb, "Architecture", System.getProperty("os.arch") + " (" + System.getProperty("sun.arch.data.model", "?") + " bit)");
		line(sb, "System", System.getProperty("os.name") + " " + System.getProperty("os.version"));
		line(sb, "File encoding", System.getProperty("file.encoding"));
		line(sb, "Max heap", Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");
		line(sb, "Processors", String.valueOf(Runtime.getRuntime().availableProcessors()));
		try {
			String name = ManagementFactory.getRuntimeMXBean().getName();
			line(sb, "Process", name);
			List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
			line(sb, "JVM arguments", args.isEmpty() ? "-" : String.join(" ", args));
		} catch (Throwable ignored) {
			// management not available: the other details are enough
		}
		return sb.toString();
	}

	private static void line(StringBuilder sb, String key, String value) {
		sb.append(String.format("%-18s %s%n", key + ":", value == null ? "-" : value));
	}

	private void showPopup() {
		if (popup != null && popup.isVisible()) {
			return;
		}
		java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
		popup = new JWindow(owner);
		popup.setAlwaysOnTop(true);

		JTextArea area = new JTextArea(buildDetails());
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int) Math.max(11, getFont().getSize2D() + 1)));
		area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		area.setCaretPosition(0);

		JButton copy = new JButton("Copy all");
		copy.setFocusable(false);
		copy.addActionListener(e -> {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(area.getText()), null);
			copy.setText("Copied!");
		});

		JPanel footer = new JPanel(new BorderLayout());
		footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
		JLabel hint = new JLabel("Select text with the mouse, or copy it all");
		hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));
		footer.add(hint, BorderLayout.WEST);
		footer.add(copy, BorderLayout.EAST);

		JPanel content = new JPanel(new BorderLayout());
		Color border = UIManager.getColor("Component.borderColor");
		content.setBorder(BorderFactory.createLineBorder(border != null ? border : Color.GRAY));
		JScrollPane scroll = new JScrollPane(area);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		content.add(scroll, BorderLayout.CENTER);
		content.add(footer, BorderLayout.SOUTH);
		popup.setContentPane(content);

		Dimension size = new Dimension(560, Math.min(430, area.getPreferredSize().height + footer.getPreferredSize().height + 24));
		popup.setSize(size);
		popup.setLocation(bestLocation(size));

		// Esc closes it
		area.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "closePopup");
		area.getActionMap().put("closePopup", new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				hidePopup();
			}
		});

		popup.setVisible(true);
		closeTimer.start();
	}

	/** Under the label, kept inside the monitor where the label is */
	private Point bestLocation(Dimension size) {
		Point onScreen = getLocationOnScreen();
		int x = onScreen.x + getWidth() - size.width;
		int y = onScreen.y + getHeight() + 2;
		GraphicsConfiguration gc = getGraphicsConfiguration();
		if (gc != null) {
			Rectangle bounds = gc.getBounds();
			java.awt.Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
			x = Math.max(bounds.x + insets.left, Math.min(x, bounds.x + bounds.width - insets.right - size.width));
			y = Math.min(y, bounds.y + bounds.height - insets.bottom - size.height);
		}
		return new Point(x, y);
	}

	private void closeIfMouseAway() {
		if (popup == null || !popup.isVisible()) {
			closeTimer.stop();
			return;
		}
		PointerInfo info = MouseInfo.getPointerInfo();
		if (info == null) {
			return;
		}
		Point p = info.getLocation();
		Rectangle labelArea = new Rectangle(isShowing() ? getLocationOnScreen() : new Point(-1, -1), getSize());
		Rectangle popupArea = popup.getBounds();
		popupArea.grow(6, 6);
		if (!labelArea.contains(p) && !popupArea.contains(p)) {
			hidePopup();
		}
	}

	private void hidePopup() {
		closeTimer.stop();
		if (popup != null) {
			popup.setVisible(false);
			popup.dispose();
			popup = null;
		}
	}
}
