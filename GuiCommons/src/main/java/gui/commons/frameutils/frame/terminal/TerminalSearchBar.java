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
package gui.commons.frameutils.frame.terminal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

/**
 * Search bar of the interactive terminal, docked above the screen (as the one of the terminal of an IDE):
 * text field, match case / regular expression / whole word switches, match counter, previous / next and close.
 * It only collects the query and shows the result: the search itself is done by {@link TerminalPanel}.
 */
final class TerminalSearchBar extends JPanel {

	private static final long serialVersionUID = 1L;

	/** What the owner does on the user's requests */
	interface Host {
		/** Query or options changed */
		void queryChanged();

		/** Go to another match: older (up in the history) or newer (down) */
		void step(boolean older);

		/** Close the bar */
		void closeRequested();
	}

	private enum Glyph { MATCH_CASE, REGEX, WORD, UP, DOWN, CLOSE }

	private final Host host;
	private final JTextField field = new JTextField();
	private final JLabel counter = new JLabel("", SwingConstants.RIGHT);
	private final GlyphButton caseButton = new GlyphButton(Glyph.MATCH_CASE, true, "Match case");
	private final GlyphButton regexButton = new GlyphButton(Glyph.REGEX, true, "Regular expression");
	private final GlyphButton wordButton = new GlyphButton(Glyph.WORD, true, "Whole word");
	private final GlyphButton upButton = new GlyphButton(Glyph.UP, false, "Older match  (Enter)");
	private final GlyphButton downButton = new GlyphButton(Glyph.DOWN, false, "Newer match  (Shift+Enter)");
	private final GlyphButton closeButton = new GlyphButton(Glyph.CLOSE, false, "Close  (Esc)");

	TerminalSearchBar(Host host) {
		super(new MigLayout("insets 4 8 4 6, hidemode 3, fillx", "[grow,fill,180:300:]8[][][]10[60:60:]6[][]10[]", "[]"));
		this.host = host;
		setOpaque(true);

		field.putClientProperty("JTextField.placeholderText", "Search in terminal");
		field.setToolTipText("Search in the terminal history  (Enter: older match, Shift+Enter: newer match)");
		field.setColumns(18);
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				host.queryChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				host.queryChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				host.queryChanged();
			}
		});
		field.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_F3) {
					host.step(!e.isShiftDown());
					e.consume();
				} else if (code == KeyEvent.VK_ESCAPE) {
					host.closeRequested();
					e.consume();
				} else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
					host.step(code == KeyEvent.VK_UP);
					e.consume();
				}
			}
		});

		counter.setFont(counter.getFont().deriveFont(Font.PLAIN, 12f));

		for (GlyphButton toggle : new GlyphButton[] { caseButton, regexButton, wordButton }) {
			toggle.addActionListener(e -> {
				host.queryChanged();
				field.requestFocusInWindow();
			});
		}
		upButton.addActionListener(e -> host.step(true));
		downButton.addActionListener(e -> host.step(false));
		closeButton.addActionListener(e -> host.closeRequested());

		add(field, "h 26!");
		add(caseButton, "w 26!, h 26!");
		add(regexButton, "w 26!, h 26!");
		add(wordButton, "w 26!, h 26!");
		add(counter, "w 60!");
		add(upButton, "w 26!, h 26!");
		add(downButton, "w 26!, h 26!");
		add(closeButton, "w 26!, h 26!");
	}

	////////// STATE ////////////////

	String getQuery() {
		return field.getText();
	}

	void setQuery(String text) {
		field.setText(text);
	}

	boolean isMatchCase() {
		return caseButton.isSelected();
	}

	boolean isRegex() {
		return regexButton.isSelected();
	}

	boolean isWholeWord() {
		return wordButton.isSelected();
	}

	void focusField() {
		field.requestFocusInWindow();
		field.selectAll();
	}

	/** Shows the outcome: position of the current match, number of matches, wrong expression */
	void setResult(int current, int total, boolean capped, boolean invalid) {
		boolean empty = field.getText().isEmpty();
		String text;
		boolean error = false;
		if (empty) {
			text = "";
		} else if (invalid) {
			text = "Invalid";
			error = true;
		} else if (total == 0) {
			text = "No results";
			error = true;
		} else {
			text = (current + 1) + " / " + total + (capped ? "+" : "");
		}
		counter.setText(text);
		counter.setForeground(error ? new Color(0xE0, 0x6C, 0x75) : mutedForeground());
		field.putClientProperty("JComponent.outline", error ? "error" : null);
		field.repaint();
		boolean enabled = !empty && !invalid && total > 0;
		upButton.setEnabled(enabled);
		downButton.setEnabled(enabled);
	}

	private static Color mutedForeground() {
		Color c = UIManager.getColor("Label.disabledForeground");
		return c != null ? c : new Color(0x8A, 0x8A, 0x8A);
	}

	private static Color panelBackground() {
		Color c = UIManager.getColor("Panel.background");
		return c != null ? c : new Color(0x2B, 0x2B, 0x2B);
	}

	private static Color accent() {
		Color c = UIManager.getColor("Component.focusColor");
		return c != null ? c : new Color(0x2D, 0x8C, 0xC8);
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(panelBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		Color line = UIManager.getColor("Component.borderColor");
		g.setColor(line != null ? line : new Color(0x55, 0x55, 0x55));
		g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
	}

	////////// BUTTONS ////////////////

	/** Flat button with a glyph drawn by code: sharp at every scale, no image files, follows the colors of the look and feel */
	private static final class GlyphButton extends JComponent {
		private static final long serialVersionUID = 1L;

		private final Glyph glyph;
		private final boolean toggle;
		private boolean selected;
		private boolean hover;
		private boolean pressed;
		private final java.util.List<java.awt.event.ActionListener> listeners = new java.util.ArrayList<java.awt.event.ActionListener>();

		GlyphButton(Glyph glyph, boolean toggle, String tooltip) {
			this.glyph = glyph;
			this.toggle = toggle;
			setToolTipText(tooltip);
			setFocusable(false);
			setOpaque(false);
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					hover = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					hover = false;
					pressed = false;
					repaint();
				}

				@Override
				public void mousePressed(MouseEvent e) {
					pressed = isEnabled();
					repaint();
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					boolean fire = pressed && hover && isEnabled();
					pressed = false;
					if (fire) {
						if (GlyphButton.this.toggle) {
							selected = !selected;
						}
						for (java.awt.event.ActionListener l : listeners) {
							l.actionPerformed(new java.awt.event.ActionEvent(GlyphButton.this, java.awt.event.ActionEvent.ACTION_PERFORMED, glyph.name()));
						}
					}
					repaint();
				}
			});
		}

		void addActionListener(java.awt.event.ActionListener l) {
			listeners.add(l);
		}

		boolean isSelected() {
			return selected;
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(26, 26);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			Color base = UIManager.getColor("Label.foreground");
			Color fg = base != null ? base : new Color(0xCC, 0xCC, 0xCC);
			Color accent = accent();
			int w = getWidth();
			int h = getHeight();

			if (selected) {
				g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
				g.fillRoundRect(1, 1, w - 3, h - 3, 8, 8);
				g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190));
				g.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
			} else if (isEnabled() && (hover || pressed)) {
				g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), pressed ? 55 : 32));
				g.fillRoundRect(1, 1, w - 3, h - 3, 8, 8);
			}

			Color ink = !isEnabled() ? new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 70) : selected ? Color.WHITE : fg;
			g.setColor(ink);
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			float cx = w / 2f;
			float cy = h / 2f;
			switch (glyph) {
			case MATCH_CASE:
				text(g, "Aa", cx, cy, 12.5f, true);
				break;
			case REGEX:
				text(g, ".*", cx, cy + 2, 14f, true);
				break;
			case WORD:
				text(g, "ab", cx, cy - 1, 11.5f, true);
				g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(new java.awt.geom.Line2D.Float(cx - 7, cy + 6, cx + 7, cy + 6));
				g.draw(new java.awt.geom.Line2D.Float(cx - 7, cy + 3.5f, cx - 7, cy + 6));
				g.draw(new java.awt.geom.Line2D.Float(cx + 7, cy + 3.5f, cx + 7, cy + 6));
				break;
			case UP:
				g.draw(new java.awt.geom.Path2D.Float() {
					private static final long serialVersionUID = 1L;
					{
						moveTo(cx - 5, cy + 2.5f);
						lineTo(cx, cy - 2.5f);
						lineTo(cx + 5, cy + 2.5f);
					}
				});
				break;
			case DOWN:
				g.draw(new java.awt.geom.Path2D.Float() {
					private static final long serialVersionUID = 1L;
					{
						moveTo(cx - 5, cy - 2.5f);
						lineTo(cx, cy + 2.5f);
						lineTo(cx + 5, cy - 2.5f);
					}
				});
				break;
			default:
				g.draw(new java.awt.geom.Line2D.Float(cx - 4.5f, cy - 4.5f, cx + 4.5f, cy + 4.5f));
				g.draw(new java.awt.geom.Line2D.Float(cx - 4.5f, cy + 4.5f, cx + 4.5f, cy - 4.5f));
			}
			g.dispose();
		}

		private void text(Graphics2D g, String value, float cx, float cy, float size, boolean bold) {
			Font font = getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, size);
			g.setFont(font);
			FontMetrics metrics = g.getFontMetrics();
			float x = cx - metrics.stringWidth(value) / 2f;
			float y = cy + (metrics.getAscent() - metrics.getDescent()) / 2f;
			g.drawString(value, x, y);
		}
	}
}
