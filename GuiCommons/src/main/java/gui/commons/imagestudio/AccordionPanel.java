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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import gui.commons.frameutils.frame.arch.ToolStyle;
import net.miginfocom.swing.MigLayout;

/**
 * Vertical list of collapsible sections. Each section has the lowest {@link StudioMode} it is shown in, so the same
 * panel is simple in Basic and complete in Nerd. By default only one section is open at a time (so the list fits the
 * window without scrolling) and each header can show a short badge with what is set inside, visible when closed too.
 */
public class AccordionPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private final List<Section> sections = new ArrayList<Section>();
	private boolean exclusive = true;

	public AccordionPanel() {
		super(new MigLayout("insets 0, wrap 1, gapy 2, hidemode 3", "[grow,fill]", ""));
		setOpaque(false);
	}

	/** true (default): opening a section closes the others */
	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	/** Adds a section; it starts expanded when {@code expanded} is true (in exclusive mode only the first one stays open) */
	public void addSection(String title, JComponent content, StudioMode minimumMode, boolean expanded) {
		boolean open = expanded && !(exclusive && hasOpenSection());
		Section section = new Section(this, title, content, minimumMode, open);
		sections.add(section);
		add(section, "growx");
	}

	private boolean hasOpenSection() {
		for (Section section : sections) {
			if (section.expanded) {
				return true;
			}
		}
		return false;
	}

	/** Shows the sections available in the mode; if the open one is not available, the first one that is gets opened */
	public void applyMode(StudioMode mode) {
		boolean open = false;
		Section first = null;
		for (Section section : sections) {
			boolean visible = mode.atLeast(section.minimumMode);
			section.setVisible(visible);
			if (visible && first == null) {
				first = section;
			}
			open |= visible && section.expanded;
		}
		if (!open && first != null) {
			first.setExpanded(true);
		}
		revalidate();
		repaint();
	}

	/** Short text on the right of the section header (empty hides it) */
	public void setBadge(String title, String text) {
		for (Section section : sections) {
			if (section.title.equals(title)) {
				section.setBadge(text);
			}
		}
	}

	private void opened(Section opened) {
		if (!exclusive) {
			return;
		}
		for (Section section : sections) {
			if (section != opened && section.expanded) {
				section.setExpanded(false);
			}
		}
	}

	/** One section: clickable header with an arrow and a badge, and the content below */
	private static class Section extends JPanel {
		private static final long serialVersionUID = 1L;

		private final AccordionPanel owner;
		private final String title;
		private final StudioMode minimumMode;
		private final JPanel header;
		private final JLabel titleLabel;
		private final JLabel badge;
		private final JPanel body;
		private boolean expanded;

		Section(AccordionPanel owner, String title, JComponent content, StudioMode minimumMode, boolean expanded) {
			super(new MigLayout("insets 0, wrap 1, gapy 0, hidemode 3", "[grow,fill]", ""));
			setOpaque(false);
			this.owner = owner;
			this.title = title;
			this.minimumMode = minimumMode;
			this.expanded = expanded;

			header = new JPanel(new MigLayout("insets 4 8 4 8, gapx 6", "[grow,fill,shrink][]", "[]")) {
				private static final long serialVersionUID = 1L;

				@Override
				protected void paintComponent(Graphics g0) {
					Graphics2D g = (Graphics2D) g0.create();
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setColor(getBackground());
					g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
					if (Section.this.expanded) {
						g.setColor(ToolStyle.BLUE);
						g.fillRoundRect(0, 5, 3, getHeight() - 10, 3, 3);
					}
					g.dispose();
				}
			};
			header.setOpaque(false);
			header.setBackground(base());
			header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			titleLabel = new JLabel(title);
			titleLabel.setFont(ToolStyle.font(0f, Font.BOLD));
			titleLabel.setIconTextGap(8);
			titleLabel.setIcon(new Arrow());
			badge = ToolStyle.mutedLabel("");
			badge.setFont(ToolStyle.font(-1f, Font.PLAIN));
			badge.setVisible(false);
			header.add(titleLabel, "wmin 0");
			header.add(badge, "wmin 0");
			header.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					toggle();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					header.setBackground(hover());
					header.repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					header.setBackground(base());
					header.repaint();
				}
			});
			add(header, "growx");

			body = new JPanel(new MigLayout("insets 8 8 10 8, wrap 1, gapy 4", "[grow,fill]", ""));
			body.setOpaque(false);
			body.add(content, "growx, wmin 0");
			add(body, "growx");
			body.setVisible(expanded);
		}

		private static Color base() {
			Color panel = UIManager.getColor("Panel.background");
			return panel == null ? Color.LIGHT_GRAY : ToolStyle.blend(panel, ToolStyle.isDark() ? Color.WHITE : Color.BLACK, 0.06f);
		}

		private static Color hover() {
			Color panel = UIManager.getColor("Panel.background");
			return panel == null ? Color.GRAY : ToolStyle.blend(panel, ToolStyle.isDark() ? Color.WHITE : Color.BLACK, 0.12f);
		}

		void setBadge(String text) {
			boolean show = text != null && !text.isEmpty();
			badge.setText(show ? text : "");
			badge.setVisible(show);
			header.revalidate();
		}

		void toggle() {
			setExpanded(!expanded);
		}

		void setExpanded(boolean value) {
			if (expanded == value) {
				return;
			}
			expanded = value;
			body.setVisible(expanded);
			header.repaint();
			titleLabel.repaint();
			if (expanded) {
				owner.opened(this);
			}
			owner.revalidate();
			owner.repaint();
		}

		private class Arrow implements Icon {
			@Override
			public void paintIcon(Component c, Graphics g0, int x, int y) {
				Graphics2D g = (Graphics2D) g0.create();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(c.getForeground());
				if (expanded) {
					g.fillPolygon(new int[] {x, x + 9, x + 4}, new int[] {y + 3, y + 3, y + 8}, 3);
				} else {
					g.fillPolygon(new int[] {x + 2, x + 2, x + 7}, new int[] {y, y + 9, y + 4}, 3);
				}
				g.dispose();
			}

			@Override
			public int getIconWidth() {
				return 10;
			}

			@Override
			public int getIconHeight() {
				return 10;
			}
		}
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(200, super.getMinimumSize().height);
	}
}
