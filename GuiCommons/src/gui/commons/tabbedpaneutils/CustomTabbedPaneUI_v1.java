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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import resources.IconsPathConfigurator;

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
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, 
               int tabIndex, Rectangle iconRect, Rectangle textRect) {
    	int selectedIndex = mainTabPane.getSelectedIndex();
    	Color savedColor = g.getColor();
    	Graphics2D g2d = (Graphics2D)g;
        int x = rects[tabIndex].x+xGap;
        int y = rects[tabIndex].y+yGap;
        
        currentButton = getCloseButton(tabIndex, buttonOrdinalPos);
        if (buttonOrdinalPos != Integer.MAX_VALUE && currentButton != null) {
			if (selectedIndex == tabIndex) {
				g.setColor(fillingColor);
//				currentButton.setBackground(ACTIVE_BTN_CLOSE_COL);
				currentButton.setIcon(IconsPathConfigurator.X_WHITE_IMG_RED);
			} else {
				g.setColor(deSelfillingColor);
//				currentButton.setBackground(INACTIVE_BTN_CLOSE_COL);
				currentButton.setIcon(IconsPathConfigurator.X_GRAY_IMG_RED);
			} 
		}
		g.fillRoundRect(x, y, rects[tabIndex].width+wPad, rects[tabIndex].height+hPad, arch, arch);
        
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(THICKNESS_BORDER));
        g2d.drawRoundRect(x, y, rects[tabIndex].width+wPad, rects[tabIndex].height+hPad, arch, arch);
        g2d.setColor(savedColor);
        
        try {
        	Color foregroundCompl = new Color(10,10,10);
        	getLabelText(tabIndex, buttonOrdinalPos-1).setForeground(foregroundCompl);
		} catch (Exception e) {
		}
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
