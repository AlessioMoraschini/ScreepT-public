/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
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
package gui.commons.tabbedpaneutils;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import gui.commons.tabbedpaneutils.TabHeadPanel_Full.CurrentTabHeader;
import resources.IconsPathConfigurator;

public class TabUtils {
	
	public static final int TAB_ICON_ORDER = 0;
	public static final int TAB_TITLE_ORDER = 1;
	public static final int TAB_BUTTON_CLOSE_ORDER = 2;
	
	public static final int MAX_TAB_LABELENGHT = 120;
	
	/**
	 * Tab style = title + icon + tooltip + close button (action listener correlated)
	 * @param title
	 * @param tooltip
	 * @param iconPath
	 * @return
	 */
	public static JPanel getCustomClosableTab(String title, String tooltip, String iconPath, ActionListener tabCloseListener, CurrentTabHeader tabHeaderRetriever) {

		return new TabHeadPanel_Full(title, tooltip, iconPath, tabCloseListener, tabHeaderRetriever);
	}
	
	/**
	 * Tab style = title + icon + tooltip
	 * @param title
	 * @param tooltip
	 * @param iconPath
	 * @return
	 */
	public static JPanel getCustomTab(String title, String tooltip, String iconPath) {
		JPanel pnlTab = new JPanel(new GridBagLayout());
		pnlTab.setOpaque(true);
		pnlTab.setPreferredSize(new Dimension(100,25));
		pnlTab.setToolTipText(tooltip);

		// tab title
		JLabel lblTitle = new JLabel(title);
		
		// image label
		JLabel imageLbl = new JLabel();
		imageLbl.setIcon(new ImageIcon(iconPath));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.ipadx = 4;
		gbc.ipady = 0;
		gbc.weightx = 4;

		// add elements to pnlTab : order must be same as "int TAB_XXX_ORDER" defined at start of this class
		pnlTab.add(imageLbl, gbc); // 1 - ICON
		gbc.gridx++;
		pnlTab.add(lblTitle, gbc); // 2 - TEXT TITLE

		return pnlTab;
	}
	
	
	public static JButton applyStyleToTabCloseBtn(JButton btnClose) {
		Color background = btnClose.getBackground();
		Color foreground = new Color(210,210,210);
		
		btnClose.setFont(new Font("SansSerif", Font.BOLD, 9));
		btnClose.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		btnClose.setMargin(new Insets(4, 4, 4, 4));
		btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnClose.setBackground(background);
		btnClose.setForeground(foreground);
		btnClose.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {
			}
			public void mousePressed(MouseEvent e) {
			}
			public void mouseExited(MouseEvent e) {
				btnClose.setIcon(IconsPathConfigurator.X_WHITE_IMG_RED);
				btnClose.revalidate();
				btnClose.repaint();
			}
			public void mouseEntered(MouseEvent e) {
				btnClose.setIcon(IconsPathConfigurator.X_GRAY_IMG_RED_LIGHT);
				btnClose.revalidate();
				btnClose.repaint();
			}
			public void mouseClicked(MouseEvent e) {
			}
		});
		return btnClose;
	}
	
	public static void scrollToNextTab(final JTabbedPane tabs) {
		ActionMap am = tabs.getActionMap();
		Action actionForward = am.get("scrollTabsForwardAction");
		actionForward.actionPerformed(new ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, ""));
	}

	public static void scrollToPreviousTab(final JTabbedPane tabs) {
		ActionMap am = tabs.getActionMap();
		Action action = am.get("scrollTabsBackwardAction");
		action.actionPerformed(new ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, ""));
	}

	public static void scrollToCurrentTab(final JTabbedPane tabs) {
		ActionMap am = tabs.getActionMap();
		Action actionForward = am.get("scrollTabsForwardAction");
		actionForward.actionPerformed(new ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, ""));
		Action action = am.get("scrollTabsBackwardAction");
		action.actionPerformed(new ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, ""));
	}
	
}
