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
 * panel is simple in Basic and complete in Nerd.
 */
public class AccordionPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private final List<Section> sections = new ArrayList<Section>();

	public AccordionPanel() {
		super(new MigLayout("insets 0, wrap 1, gapy 6, hidemode 3", "[grow,fill]", ""));
		setOpaque(false);
	}

	/** Adds a section; it starts expanded when {@code expanded} is true */
	public void addSection(String title, JComponent content, StudioMode minimumMode, boolean expanded) {
		Section section = new Section(title, content, minimumMode, expanded);
		sections.add(section);
		add(section, "growx");
	}

	/** Shows the sections available in the mode */
	public void applyMode(StudioMode mode) {
		for (Section section : sections) {
			section.setVisible(mode.atLeast(section.minimumMode));
		}
		revalidate();
		repaint();
	}

	/** One section: clickable header with an arrow and the content card below */
	private static class Section extends JPanel {
		private static final long serialVersionUID = 1L;

		private final StudioMode minimumMode;
		private final JComponent content;
		private final JLabel header;
		private boolean expanded;

		Section(String title, JComponent content, StudioMode minimumMode, boolean expanded) {
			super(new MigLayout("insets 0, wrap 1, gapy 0", "[grow,fill]", ""));
			setOpaque(false);
			this.minimumMode = minimumMode;
			this.content = content;
			this.expanded = expanded;

			header = new JLabel(title);
			header.setFont(ToolStyle.font(0f, Font.BOLD));
			header.setIconTextGap(8);
			header.setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 6, 7, 6));
			header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			header.setOpaque(true);
			header.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					toggle();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					header.setBackground(hover());
				}

				@Override
				public void mouseExited(MouseEvent e) {
					header.setBackground(base());
				}
			});
			header.setBackground(base());
			header.setIcon(new Arrow());
			add(header, "growx");

			JPanel body = new JPanel(new MigLayout("insets 8 8 10 8, wrap 1, gapy 4", "[grow,fill]", ""));
			body.setOpaque(false);
			body.add(content, "growx");
			content = body;
			this.contentHolder = body;
			add(body, "growx");
			body.setVisible(expanded);
		}

		private final JComponent contentHolder;

		private static Color base() {
			Color panel = UIManager.getColor("Panel.background");
			return panel == null ? Color.LIGHT_GRAY : ToolStyle.blend(panel, ToolStyle.isDark() ? Color.WHITE : Color.BLACK, 0.06f);
		}

		private static Color hover() {
			Color panel = UIManager.getColor("Panel.background");
			return panel == null ? Color.GRAY : ToolStyle.blend(panel, ToolStyle.isDark() ? Color.WHITE : Color.BLACK, 0.12f);
		}

		void toggle() {
			expanded = !expanded;
			contentHolder.setVisible(expanded);
			header.repaint();
			revalidate();
			Component top = getParent();
			if (top != null) {
				top.revalidate();
				top.repaint();
			}
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
