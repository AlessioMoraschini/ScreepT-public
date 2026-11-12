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
package gui.commons.treeutils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import gui.commons.frameutils.frame.arch.ToolStyle;
import gui.commons.treeutils.om.FileNameCellRenderer;

/**
 * A file tree that shows its levels: a thin guide line runs down each folder along its children (the one of the folder that
 * holds the selected row is lit in the accent color), the whole row is highlighted on selection and on hover, and the
 * indentation is a bit wider. Every color comes from the tree background and the look and feel, so it reads well on any
 * theme, light or dark.
 */
public class LevelsTree extends JTree {
	private static final long serialVersionUID = 1L;

	/** Distance of a guide line from the left of the icon of its folder */
	private static final int GUIDE_OFFSET = 8;

	public LevelsTree(TreeModel model) {
		super(model);
		adjustIndent();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		adjustIndent();
	}

	private void adjustIndent() {
		if (getUI() instanceof BasicTreeUI) {
			BasicTreeUI ui = (BasicTreeUI) getUI();
			ui.setLeftChildIndent(9);
			ui.setRightChildIndent(14);
		}
	}

	/** Accent of the current theme (the one of its buttons and focus), made sure it stands out from the tree background */
	private static Color accent(Color background, Color text) {
		Color accent = UIManager.getColor("Component.accentColor");
		if (accent == null) {
			accent = UIManager.getColor("Component.focusColor");
		}
		if (accent == null) {
			accent = UIManager.getColor("List.selectionBackground");
		}
		if (accent == null || accent.getAlpha() < 255) {
			accent = ToolStyle.BLUE;
		}
		if (Math.abs(luminance(accent) - luminance(background)) < 0.22) {
			accent = TreeUtils.blend(accent, text, 0.4f);
		}
		return accent;
	}

	private static double luminance(Color c) {
		return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
	}

	private static Color alpha(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0.create();
		try {
			Rectangle clip = g.getClipBounds() != null ? g.getClipBounds() : getVisibleRect();
			int first = getClosestRowForLocation(0, clip.y);
			int last = getClosestRowForLocation(0, clip.y + clip.height);
			if (first < 0 || last < 0) {
				return;
			}
			Color background = getBackground();
			Color text = TreeUtils.textOn(background);
			boolean dark = TreeUtils.isDark(background);
			Color accent = accent(background, text);
			// the lit guide: the accent a little lighter on dark themes, a little deeper on light ones
			Color lit = dark ? TreeUtils.blend(accent, Color.WHITE, 0.18f) : TreeUtils.blend(accent, Color.BLACK, 0.12f);
			TreePath lead = getLeadSelectionPath();
			TreePath activeParent = lead == null ? null : lead.getParentPath();
			int hover = getCellRenderer() instanceof FileNameCellRenderer ? ((FileNameCellRenderer) getCellRenderer()).hoveringRow : -1;

			for (int row = first; row <= last; row++) {
				TreePath path = getPathForRow(row);
				Rectangle bounds = getRowBounds(row);
				if (path == null || bounds == null) {
					continue;
				}
				boolean inActiveLevel = activeParent != null && activeParent.isDescendant(path) && !activeParent.equals(path);
				if (isRowSelected(row)) {
					// selected row: a soft wash of the accent and a thin bar on the left edge
					g.setColor(alpha(accent, dark ? 52 : 40));
					g.fillRect(0, bounds.y, getWidth(), bounds.height);
					g.setColor(lit);
					g.fillRect(0, bounds.y + 1, 2, Math.max(1, bounds.height - 2));
				} else if (row == hover) {
					g.setColor(alpha(text, dark ? 16 : 14));
					g.fillRect(0, bounds.y, getWidth(), bounds.height);
				} else if (inActiveLevel) {
					// the rows of the level that holds the selection, barely tinted
					g.setColor(alpha(accent, dark ? 14 : 12));
					g.fillRect(0, bounds.y, getWidth(), bounds.height);
				}
				// one guide line for every level above this row: quiet, and lit for the level of the selection
				for (TreePath ancestor = path.getParentPath(); ancestor != null; ancestor = ancestor.getParentPath()) {
					if (ancestor.getParentPath() == null && !isRootVisible()) {
						break;
					}
					Rectangle ab = getPathBounds(ancestor);
					if (ab == null) {
						continue;
					}
					if (ancestor.equals(activeParent)) {
						g.setColor(alpha(lit, 190));
						g.fillRect(ab.x + GUIDE_OFFSET, bounds.y, 2, bounds.height);
					} else {
						g.setColor(alpha(text, dark ? 34 : 40));
						g.fillRect(ab.x + GUIDE_OFFSET, bounds.y, 1, bounds.height);
					}
				}
			}
		} finally {
			g.dispose();
		}
	}
}
