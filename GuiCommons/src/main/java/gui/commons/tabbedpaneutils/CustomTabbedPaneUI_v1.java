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
package gui.commons.tabbedpaneutils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import gui.commons.treeutils.TreeUtils;
import resources.IconsPathConfigurator;
import various.common.light.utility.string.StringWorker;

public class CustomTabbedPaneUI_v1 extends javax.swing.plaf.basic.BasicTabbedPaneUI {

	public static final Color DEFAULT_FILLING = Color.lightGray;
	public static final Color DEFAULT_DARK_FILLING = DEFAULT_FILLING.darker().darker();
	public static final Color DEFAULT_BORDER = Color.BLACK;
	public static final Color ACTIVE_BTN_CLOSE_COL = new Color(180, 10, 0);
	public static final Color INACTIVE_BTN_CLOSE_COL = new Color(50, 0, 0);

	public static final int THICKNESS_BORDER = 1;

	private static final int xGap = 0;
	private static final int yGap = 0;
	private static final int wPad = 0;
	private static final int hPad = 0;
	private static final int arch = 6;

	public Color fillingColor;
	public Color deSelfillingColor;
	public Color borderColor;
	public JTabbedPane mainTabPane;
	public int buttonOrdinalPos;
	public JButton currentButton;

	/**
	 * Use this if there is close button
	 */
	public CustomTabbedPaneUI_v1(Color filling, Color deselectedFilling, Color border, JTabbedPane mainPaneTabbed, int closeBtnPos) {
		super();
		tabPane = mainPaneTabbed;
		mainTabPane = mainPaneTabbed;
		buttonOrdinalPos = closeBtnPos;
		fillingColor = (filling != null)? filling : DEFAULT_FILLING;
		deSelfillingColor = (deselectedFilling != null)? deselectedFilling : DEFAULT_DARK_FILLING;
		borderColor = (border != null)? border : DEFAULT_BORDER;
	}

	/**
	 * Use this if there is no close button
	 */
	public CustomTabbedPaneUI_v1(Color filling, Color deselectedFilling, Color border, JTabbedPane mainPaneTabbed) {
		super();
		tabPane = mainPaneTabbed;
		mainTabPane = mainPaneTabbed;
		buttonOrdinalPos = Integer.MAX_VALUE;
		fillingColor = (filling != null)? filling : DEFAULT_FILLING;
		deSelfillingColor = (deselectedFilling != null)? deselectedFilling : DEFAULT_DARK_FILLING;
		borderColor = (border != null)? border : DEFAULT_BORDER;
	}

    @Override
    protected void installDefaults() {
    	super.installDefaults();
    	// compact and flat: no padding around the tab headers (their own panel carries the spacing), no content border
    	tabInsets = new Insets(0, 0, 0, 0);
    	selectedTabPadInsets = new Insets(0, 0, 0, 0);
    	tabAreaInsets = new Insets(2, 0, 0, 0);
    	contentBorderInsets = new Insets(0, 0, 0, 0);
    }

    /** Accent of the current look and feel, made sure it stands out from the tab strip */
    private Color accent(Color base) {
    	Color accent = UIManager.getColor("Component.accentColor");
    	if (accent == null) {
    		accent = UIManager.getColor("Component.focusColor");
    	}
    	if (accent == null || accent.getAlpha() < 255) {
    		accent = new Color(52, 132, 228);
    	}
    	return accent;
    }

    @Override
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects,
               int tabIndex, Rectangle iconRect, Rectangle textRect) {
    	try {
	    	Rectangle r = rects[tabIndex];
	    	boolean selected = mainTabPane.getSelectedIndex() == tabIndex;
	    	Color base = mainTabPane.getBackground();
	    	boolean dark = TreeUtils.isDark(base);
	    	Color strong = TreeUtils.textOn(base);

	    	// close button: same custom icons as ever, white on the selected tab and gray on the others
	    	currentButton = getCloseButton(tabIndex, buttonOrdinalPos);
	    	if (buttonOrdinalPos != Integer.MAX_VALUE && currentButton != null) {
	    		if (currentButton.getParent() instanceof TabHeadPanel_Full && ((TabHeadPanel_Full) currentButton.getParent()).isPinned()) {
	    			// pinned tab: a pin instead of the close button
	    			((TabHeadPanel_Full) currentButton.getParent()).showPinIcon(selected);
	    		} else {
	    			currentButton.setIcon(selected ? IconsPathConfigurator.X_WHITE_IMG_RED : IconsPathConfigurator.X_GRAY_IMG_RED);
	    		}
	    		currentButton.setContentAreaFilled(false);
	    		currentButton.setBorderPainted(false);
	    		currentButton.setFocusPainted(false);
	    	}

	    	// fill: the selected tab is lifted from the strip, the others sink into it
	    	Color fill = selected
	    			? TreeUtils.blend(base, strong, dark ? 0.12f : 0.07f)
	    			: TreeUtils.blend(base, dark ? Color.BLACK : strong, dark ? 0.18f : 0.03f);
	    	g.setColor(fill);
	    	g.fillRect(r.x, r.y, r.width, r.height);

	    	if (selected) {
	    		// a slim accent bar on the outer edge of the selected tab
	    		g.setColor(accent(base));
	    		if (tabPlacement == BOTTOM) {
	    			g.fillRect(r.x, r.y + r.height - 2, r.width, 2);
	    		} else {
	    			g.fillRect(r.x, r.y, r.width, 2);
	    		}
	    	} else {
	    		// a quiet divider between the tabs that are not selected
	    		g.setColor(TreeUtils.blend(base, strong, 0.14f));
	    		g.fillRect(r.x + r.width - 1, r.y + 5, 1, Math.max(1, r.height - 10));
	    	}

	    	JLabel label = getLabelText(tabIndex, buttonOrdinalPos - 1);
	    	boolean unsaved = StringWorker.trimToEmpty(label.getText()).contains("(*)");
	    	Color text = selected ? strong : TreeUtils.blend(strong, base, 0.4f);
	    	if (unsaved) {
	    		text = dark ? new Color(selected ? 240 : 205, selected ? 110 : 95, selected ? 100 : 90) : new Color(selected ? 176 : 150, 30, 30);
	    	}
	    	label.setForeground(text);
		} catch (Exception e) {
		}
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
    	// only a thin line between the strip and the content, in a tone of the strip
    	try {
    		if (selectedIndex < 0 || selectedIndex >= rects.length) {
    			return;
    		}
    		Rectangle r = rects[selectedIndex];
    		Color base = mainTabPane.getBackground();
    		g.setColor(TreeUtils.blend(base, TreeUtils.textOn(base), 0.16f));
    		if (tabPlacement == BOTTOM) {
    			g.fillRect(0, r.y, mainTabPane.getWidth(), 1);
    		} else if (tabPlacement == TOP) {
    			g.fillRect(0, r.y + r.height - 1, mainTabPane.getWidth(), 1);
    		}
    	} catch (Exception e) {
    	}
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
    	// no focus dotted rectangle
    }

    public JButton getCloseButton(int index, int buttonOrdinalPosition) {
    	JButton buttonCloseTab = new JButton();
    	try {
			buttonCloseTab = (JButton)(getPanelFromTabHeader(index)).getComponent(buttonOrdinalPosition);
		} catch (Exception e) {
		}
    	return buttonCloseTab;
    }

    public JLabel getLabelText(int index, int labelOrdinalPosition) {
    	JLabel textLabel = new JLabel();
    	try {
    		textLabel = (JLabel)(getPanelFromTabHeader(index)).getComponent(labelOrdinalPosition);
    	} catch (Exception e) {
    	}
    	return textLabel;
    }

    public JPanel getPanelFromTabHeader(int index) {
		JPanel tabPanel = null;
		try {
			tabPanel = (JPanel) mainTabPane.getTabComponentAt(index);
		} catch (Exception e) {
		}
		return tabPanel;
	}
 }
