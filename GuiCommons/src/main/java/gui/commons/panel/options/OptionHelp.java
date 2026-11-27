/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package gui.commons.panel.options;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Description popup of an option: a small card with a title and a scrollable text that appears when the mouse rests
 * on the option. It closes with ESC, with a click outside it, or when the mouse moves away from both the option and
 * the card. The card can be resized by dragging its edges; its default size is about a quarter of the width and of
 * the height of the monitor of the window that owns the option. It takes the keyboard focus when the mouse enters it,
 * so the text scrolls with the wheel and with the keys.
 */
public final class OptionHelp {

	private static final int SHOW_DELAY_MS = 450;
	private static final int WATCH_PERIOD_MS = 120;
	private static final int SHADOW = 7;
	private static final int EDGE = 7;
	private static final Dimension MIN_SIZE = new Dimension(240, 150);

	private static final class Entry {
		final String title;
		final String text;
		/** Live entries: the text comes from here and is read again every second while the card is shown */
		final java.util.function.Supplier<String> live;
		/** The text has **bold** parts and lines indented with two spaces per level (see {@link #attachLiveRich}) */
		final boolean rich;

		Entry(String title, String text) {
			this(title, text, null, false);
		}

		Entry(String title, String text, java.util.function.Supplier<String> live, boolean rich) {
			this.title = title;
			this.text = text;
			this.live = live;
			this.rich = rich;
		}

		String currentText() {
			if (live != null) {
				try {
					String value = live.get();
					return value == null ? "" : value;
				} catch (RuntimeException e) {
					return text;
				}
			}
			return text;
		}
	}

	/** Components that show a description (the deepest hovered component is searched upwards in this map) */
	private static final Map<Component, Entry> ENTRIES = new WeakHashMap<Component, Entry>();

	private static boolean installed;
	private static Entry hovered;
	private static Component hoveredAnchor;
	private static Entry shown;
	/** Option whose popup the user closed: it does not come back until the mouse has been on another option */
	private static Entry dismissed;
	private static Component shownAnchor;
	private static JWindow window;
	private static Timer showTimer;
	private static Timer watchTimer;

	private OptionHelp() {
	}

	/** Shows the description when the mouse rests on any of the components (the plain tooltip of those is removed) */
	public static void attach(String title, String text, JComponent... components) {
		if (text == null || text.trim().isEmpty()) {
			return;
		}
		Entry entry = new Entry(title, text);
		for (JComponent component : components) {
			if (component != null) {
				component.setToolTipText(null);
				ENTRIES.put(component, entry);
			}
		}
		install();
	}

	/**
	 * Like {@link #attach}, for a text that changes (resource usage): the card is a bit taller and its text is refreshed every
	 * second while it is shown, without moving the scroll position.
	 */
	public static void attachLive(String title, java.util.function.Supplier<String> text, JComponent... components) {
		attachLive(title, text, false, components);
	}

	/**
	 * Like {@link #attachLive}, with a little formatting: text between ** ** is bold (the labels), the rest plain (the values); a
	 * line that starts with two spaces per level is indented, also when it wraps.
	 */
	public static void attachLiveRich(String title, java.util.function.Supplier<String> text, JComponent... components) {
		attachLive(title, text, true, components);
	}

	private static void attachLive(String title, java.util.function.Supplier<String> text, boolean rich, JComponent... components) {
		if (text == null) {
			return;
		}
		Entry entry = new Entry(title, "", text, rich);
		for (JComponent component : components) {
			if (component != null) {
				component.setToolTipText(null);
				ENTRIES.put(component, entry);
			}
		}
		install();
	}

	private static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				if (event instanceof MouseEvent) {
					onMouse((MouseEvent) event);
				}
			}
		}, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if (window != null && window.isVisible() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				dismissed = shown;
				hidePopup();
				e.consume();
				return true;
			}
			return false;
		});

		showTimer = new Timer(SHOW_DELAY_MS, e -> {
			if (hovered != null && hoveredAnchor != null && hoveredAnchor.isShowing()) {
				showPopup(hovered, hoveredAnchor);
			}
		});
		showTimer.setRepeats(false);
		watchTimer = new Timer(WATCH_PERIOD_MS, e -> watch());
	}

	////////// EVENTS ////////////////

	private static void onMouse(MouseEvent e) {
		Object source = e.getSource();
		if (!(source instanceof Component)) {
			return;
		}
		Component component = (Component) source;

		// events can name the window as source: look for the real component under the pointer
		Window eventWindow = component instanceof Window ? (Window) component : SwingUtilities.getWindowAncestor(component);
		if (eventWindow != null && eventWindow.isShowing() && e.getID() != MouseEvent.MOUSE_EXITED) {
			Point point = e.getLocationOnScreen();
			SwingUtilities.convertPointFromScreen(point, eventWindow);
			Component deepest = SwingUtilities.getDeepestComponentAt(eventWindow, point.x, point.y);
			if (deepest != null) {
				component = deepest;
			}
		}

		if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			if (window != null && window.isVisible() && SwingUtilities.getWindowAncestor(component) != window && component != window) {
				dismissed = shown;
				hidePopup();
			}
			showTimer.stop();
			return;
		}
		if (e.getID() != MouseEvent.MOUSE_MOVED && e.getID() != MouseEvent.MOUSE_ENTERED) {
			return;
		}
		if (window != null && (component == window || SwingUtilities.getWindowAncestor(component) == window)) {
			return;
		}

		Component anchor = component;
		Entry entry = null;
		while (anchor != null && entry == null) {
			entry = ENTRIES.get(anchor);
			if (entry == null) {
				anchor = anchor.getParent();
			}
		}

		if (entry != dismissed) {
			dismissed = null;
		}
		if (entry != null && entry == dismissed) {
			return;
		}
		if (entry == hovered && anchor == hoveredAnchor) {
			return;
		}
		hovered = entry;
		hoveredAnchor = anchor;
		showTimer.stop();
		if (entry != null && (shown != entry)) {
			// a popup of another option is closed as soon as the mouse is on a new one
			if (window != null && window.isVisible()) {
				hidePopup();
			}
			showTimer.restart();
		}
	}

	/** Keeps the popup while the mouse is on the option or on the card (and in the gap between them) */
	private static void watch() {
		if (window == null || !window.isVisible()) {
			watchTimer.stop();
			return;
		}
		if (shownAnchor == null || !shownAnchor.isShowing()) {
			hidePopup();
			return;
		}
		PointerInfo info = MouseInfo.getPointerInfo();
		if (info == null) {
			return;
		}
		Point p = info.getLocation();
		Rectangle anchor = new Rectangle(shownAnchor.getLocationOnScreen(), shownAnchor.getSize());
		Rectangle card = window.getBounds();
		Rectangle zone = anchor.union(card);
		zone.grow(2, 2);
		if (!zone.contains(p)) {
			hidePopup();
		}
	}

	////////// POPUP ////////////////

	private static void hidePopup() {
		showTimer.stop();
		if (watchTimer != null) {
			watchTimer.stop();
		}
		if (window != null) {
			window.setVisible(false);
			window.dispose();
			window = null;
		}
		shown = null;
		shownAnchor = null;
		hovered = null;
		hoveredAnchor = null;
	}

	private static void showPopup(Entry entry, Component anchor) {
		hidePopup();
		hovered = entry;
		hoveredAnchor = anchor;

		Window owner = SwingUtilities.getWindowAncestor(anchor);
		GraphicsConfiguration configuration = owner != null ? owner.getGraphicsConfiguration() : null;
		Rectangle screen = configuration != null ? configuration.getBounds() : new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		if (configuration != null) {
			// do not cover the task bar
			java.awt.Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
			screen = new Rectangle(screen.x + insets.left, screen.y + insets.top, screen.width - insets.left - insets.right,
					screen.height - insets.top - insets.bottom);
		}

		int width = Math.max(MIN_SIZE.width, screen.width / 4) + 2 * SHADOW;
		int height = Math.max(MIN_SIZE.height, entry.live != null ? screen.height / 3 : screen.height / 4) + 2 * SHADOW;
		width = Math.min(width, screen.width);
		height = Math.min(height, screen.height);

		JWindow popup = new JWindow(owner);
		popup.setAlwaysOnTop(owner != null && owner.isAlwaysOnTop());
		try {
			popup.setBackground(new Color(0, 0, 0, 0));
		} catch (UnsupportedOperationException e) {
			// no per-pixel translucency: the card is painted on an opaque window
		}
		Card card = new Card(entry, popup);
		popup.setContentPane(card);
		popup.setSize(width, height);

		Rectangle target = new Rectangle(anchor.getLocationOnScreen(), anchor.getSize());
		Point pointer = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : target.getLocation();
		int x = pointer.x - width / 3;
		int y = target.y + target.height - SHADOW + 4;
		if (y + height > screen.y + screen.height) {
			y = target.y - height + SHADOW - 4;
		}
		x = Math.max(screen.x, Math.min(x, screen.x + screen.width - width));
		y = Math.max(screen.y, Math.min(y, screen.y + screen.height - height));
		popup.setLocation(x, y);

		window = popup;
		shown = entry;
		shownAnchor = anchor;
		popup.setVisible(true);
		watchTimer.restart();
	}

	/** The card: rounded, with a soft shadow, an accent stripe, the title and the scrollable text; resizable by its edges */
	private static final class Card extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final int NONE = 0;
		private static final int W = 1;
		private static final int E = 2;
		private static final int N = 4;
		private static final int S = 8;

		private final JWindow popup;
		private final JTextComponent body;
		private final boolean rich;
		private String shownText = "";
		private Timer refresher;
		private int dragZone;
		private Point dragStart;
		private Rectangle dragBounds;

		Card(Entry entry, JWindow popup) {
			super(new java.awt.BorderLayout(0, 6));
			this.popup = popup;
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(SHADOW + 12, SHADOW + 18, SHADOW + 12, SHADOW + 14));

			this.rich = entry.rich;
			body = entry.rich ? new JTextPane() : new JTextArea();
			Color foreground = color("Label.foreground", Color.BLACK);
			JLabel title = new JLabel(entry.title);
			Font base = title.getFont();
			title.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 3f));
			title.setForeground(foreground);

			if (body instanceof JTextArea) {
				((JTextArea) body).setLineWrap(true);
				((JTextArea) body).setWrapStyleWord(true);
			}
			body.setEditable(false);
			body.setOpaque(false);
			body.setForeground(blend(foreground, background(), 0.12));
			body.setFont(base.deriveFont(Font.PLAIN, base.getSize2D() + 1f));
			showText(entry.currentText());
			body.setCaretPosition(0);
			body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
			body.setHighlighter(null);

			JScrollPane scroll = new JScrollPane(body);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			scroll.setOpaque(false);
			scroll.getViewport().setOpaque(false);
			scroll.getVerticalScrollBar().setUnitIncrement(14);

			add(title, java.awt.BorderLayout.NORTH);
			add(scroll, java.awt.BorderLayout.CENTER);

			MouseAdapter mouse = new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					// the text takes the focus: wheel and keys scroll it
					popup.toFront();
					body.requestFocusInWindow();
				}

				@Override
				public void mouseMoved(MouseEvent e) {
					setCursor(Cursor.getPredefinedCursor(cursorFor(zoneAt(e.getPoint()))));
				}

				@Override
				public void mousePressed(MouseEvent e) {
					dragZone = zoneAt(e.getPoint());
					dragStart = e.getLocationOnScreen();
					dragBounds = popup.getBounds();
					body.requestFocusInWindow();
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					if (dragZone == NONE || dragStart == null) {
						return;
					}
					int dx = e.getXOnScreen() - dragStart.x;
					int dy = e.getYOnScreen() - dragStart.y;
					Rectangle r = new Rectangle(dragBounds);
					if ((dragZone & E) != 0) {
						r.width = Math.max(MIN_SIZE.width, dragBounds.width + dx);
					}
					if ((dragZone & S) != 0) {
						r.height = Math.max(MIN_SIZE.height, dragBounds.height + dy);
					}
					if ((dragZone & W) != 0) {
						int width = Math.max(MIN_SIZE.width, dragBounds.width - dx);
						r.x = dragBounds.x + dragBounds.width - width;
						r.width = width;
					}
					if ((dragZone & N) != 0) {
						int height = Math.max(MIN_SIZE.height, dragBounds.height - dy);
						r.y = dragBounds.y + dragBounds.height - height;
						r.height = height;
					}
					popup.setBounds(r);
					popup.validate();
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					dragZone = NONE;
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
			// the edges are over the text too: the same handler serves the whole card
			scroll.addMouseListener(mouse);
			scroll.addMouseMotionListener(mouse);
			body.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					mouse.mouseEntered(e);
				}
			});

			if (entry.live != null) {
				refresher = new Timer(1000, e -> {
					String text = entry.currentText();
					if (!text.equals(shownText)) {
						java.awt.Point position = scroll.getViewport().getViewPosition();
						showText(text);
						SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(position));
					}
				});
				refresher.start();
			}
		}

		/** Puts the text in the body: as it is, or (rich) with the bold labels and the indented lines */
		private void showText(String text) {
			shownText = text;
			if (!rich) {
				body.setText(text);
				return;
			}
			JTextPane pane = (JTextPane) body;
			StyledDocument doc = pane.getStyledDocument();
			Font font = body.getFont();
			SimpleAttributeSet plain = new SimpleAttributeSet();
			StyleConstants.setFontFamily(plain, font.getFamily());
			StyleConstants.setFontSize(plain, font.getSize());
			StyleConstants.setForeground(plain, body.getForeground());
			SimpleAttributeSet bold = new SimpleAttributeSet(plain);
			StyleConstants.setBold(bold, true);
			StyleConstants.setForeground(bold, color("Label.foreground", body.getForeground()));
			try {
				doc.remove(0, doc.getLength());
				String[] lines = text.split("\n", -1);
				for (int i = 0; i < lines.length; i++) {
					String line = lines[i];
					int level = 0;
					while (line.startsWith("  ")) {
						line = line.substring(2);
						level++;
					}
					int paragraphStart = doc.getLength();
					String[] parts = line.split("\\*\\*", -1);
					for (int k = 0; k < parts.length; k++) {
						if (!parts[k].isEmpty()) {
							doc.insertString(doc.getLength(), parts[k], k % 2 == 1 ? bold : plain);
						}
					}
					if (i < lines.length - 1) {
						doc.insertString(doc.getLength(), "\n", plain);
					}
					SimpleAttributeSet paragraph = new SimpleAttributeSet();
					StyleConstants.setLeftIndent(paragraph, level * 16f);
					StyleConstants.setSpaceBelow(paragraph, 2f);
					doc.setParagraphAttributes(paragraphStart, Math.max(1, doc.getLength() - paragraphStart), paragraph, false);
				}
			} catch (javax.swing.text.BadLocationException e) {
				body.setText(text.replace("**", ""));
			}
		}

		@Override
		public void removeNotify() {
			if (refresher != null) {
				refresher.stop();
			}
			super.removeNotify();
		}

		private int zoneAt(Point p) {
			int zone = NONE;
			int left = SHADOW;
			int top = SHADOW;
			int right = getWidth() - SHADOW;
			int bottom = getHeight() - SHADOW;
			if (p.x >= left - 2 && p.x <= left + EDGE) {
				zone |= W;
			}
			if (p.x >= right - EDGE && p.x <= right + 2) {
				zone |= E;
			}
			if (p.y >= top - 2 && p.y <= top + EDGE) {
				zone |= N;
			}
			if (p.y >= bottom - EDGE && p.y <= bottom + 2) {
				zone |= S;
			}
			return zone;
		}

		private static int cursorFor(int zone) {
			switch (zone) {
			case W:
				return Cursor.W_RESIZE_CURSOR;
			case E:
				return Cursor.E_RESIZE_CURSOR;
			case N:
				return Cursor.N_RESIZE_CURSOR;
			case S:
				return Cursor.S_RESIZE_CURSOR;
			case W | N:
				return Cursor.NW_RESIZE_CURSOR;
			case E | N:
				return Cursor.NE_RESIZE_CURSOR;
			case W | S:
				return Cursor.SW_RESIZE_CURSOR;
			case E | S:
				return Cursor.SE_RESIZE_CURSOR;
			default:
				return Cursor.DEFAULT_CURSOR;
			}
		}

		private static Color color(String key, Color fallback) {
			Color c = UIManager.getColor(key);
			return c != null ? c : fallback;
		}

		private static Color background() {
			return color("Panel.background", new Color(245, 245, 245));
		}

		private static Color blend(Color a, Color b, double amount) {
			return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * amount), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
					(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int x = SHADOW;
			int y = SHADOW;
			int w = getWidth() - 2 * SHADOW;
			int h = getHeight() - 2 * SHADOW;

			for (int i = SHADOW; i >= 1; i--) {
				g.setColor(new Color(0, 0, 0, 7));
				g.fillRoundRect(x - i, y - i + 2, w + 2 * i, h + 2 * i, 18 + i, 18 + i);
			}
			Color background = background();
			Color foreground = color("Label.foreground", Color.BLACK);
			Color accent = color("Component.accentColor", new Color(0, 150, 136));
			boolean dark = (background.getRed() + background.getGreen() + background.getBlue()) < 384;
			Color card = dark ? blend(background, Color.WHITE, 0.07) : blend(background, Color.WHITE, 0.6);
			g.setColor(card);
			g.fillRoundRect(x, y, w, h, 16, 16);
			g.setColor(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 50));
			g.setStroke(new BasicStroke(1f));
			g.drawRoundRect(x, y, w - 1, h - 1, 16, 16);

			// accent stripe on the left edge
			g.setClip(new java.awt.geom.RoundRectangle2D.Float(x, y, w, h, 16, 16));
			g.setColor(accent);
			g.fillRect(x, y, 5, h);
			g.dispose();
		}
	}
}
