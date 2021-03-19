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
package gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class MenuSetter {

	/**
	 * set the distance between icon and text for all menuitems and menu in the given menuBar
	 * @param menuBar
	 */
	public static void setIconTextGap(JMenuBar menuBar, int distanceInPx) {
		for(Component currentComponent : menuBar.getComponents()) {
			if(currentComponent instanceof JMenu) {
				((JMenu) currentComponent).setIconTextGap(distanceInPx);
				setIconTextGap((JMenu) currentComponent, distanceInPx);
			}else if(currentComponent instanceof JMenuItem) {
				((JMenuItem) currentComponent).setIconTextGap(distanceInPx);
			}
		}
	}
	
	public static void setIconTextGap(JMenu menuBar, int distanceInPx) {
		JMenu menu;
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getItemCount(); i++) {
			Component currentComponent = menuBar.getItem(i);
			if(currentComponent instanceof JMenu) {
				menu = (JMenu) currentComponent;
				setIconTextGap(menu, distanceInPx);
				menu.setIconTextGap(distanceInPx);
			}else if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.setIconTextGap(distanceInPx);
			}
		}
	}
	
	public static void setIconTextGap(JPopupMenu menuBar, int distanceInPx) {
		JMenu menu;
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getComponentCount(); i++) {
			Component currentComponent = menuBar.getComponent(i);
			if(currentComponent instanceof JMenu) {
				menu = (JMenu) currentComponent;
				setIconTextGap(menu, distanceInPx);
				menu.setIconTextGap(distanceInPx);
			}else if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.setIconTextGap(distanceInPx);
			}
		}
	}
	
	public static void setMargin(JMenu menuBar, Insets margin) {
		JMenu menu;
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getItemCount(); i++) {
			Component currentComponent = menuBar.getItem(i);
			if(currentComponent instanceof JMenu) {
				menu = (JMenu) currentComponent;
				setMargin(menu, margin);
				menu.setMargin(margin);
			}else if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.setMargin(margin);
			}
		}
	}
	
	public static void setMargin(JPopupMenu menuBar, Insets margin) {
		JMenu menu;
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getComponentCount(); i++) {
			Component currentComponent = menuBar.getComponent(i);
			if(currentComponent instanceof JMenu) {
				menu = (JMenu) currentComponent;
				setMargin(menu, margin);
				menu.setMargin(margin);
			}else if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.setMargin(margin);
			}
		}
	}
	public static void setFont(JMenu menuBar, Font font) {
		JMenu menu;
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getItemCount(); i++) {
			Component currentComponent = menuBar.getItem(i);
			if(currentComponent instanceof JMenu) {
				menu = (JMenu) currentComponent;
				setFont(menu, font);
				menu.setFont(font);
			}else if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.setFont(font);
			}
		}
	}
	
	public static void setFont(JPopupMenu menuBar, Font font) {
		JMenu menu;
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getComponentCount(); i++) {
			Component currentComponent = menuBar.getComponent(i);
			if(currentComponent instanceof JMenu) {
				menu = (JMenu) currentComponent;
				setFont(menu, font);
				menu.setFont(font);
			}else if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.setFont(font);
			}
		}
	}
	
	public static void addAction(JMenuItem[] items, ActionListener actionListener) {
		for (JMenuItem item : items) {
			item.addActionListener(actionListener);
		}
	}
	
	public static void addActionToAllJMenuItems(JMenu menuBar, ActionListener actionListener) {
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getItemCount(); i++) {
			Component currentComponent = menuBar.getItem(i);
			if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.addActionListener(actionListener);
			}
		}
	}

	public static void setSubItemsEnabled(JMenu menuBar, boolean enabled) {
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getItemCount(); i++) {
			Component currentComponent = menuBar.getItem(i);
			if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.setEnabled(enabled);
			}
		}
	}

	public static void setSubItemsCommonTooltip(JMenu menuBar, String tooltip) {
		JMenuItem menuItem;
		
		for(int i=0; i < menuBar.getItemCount(); i++) {
			Component currentComponent = menuBar.getItem(i);
			if(currentComponent instanceof JMenuItem) {
				menuItem = (JMenuItem) currentComponent;
				menuItem.setToolTipText(tooltip);
			}
		}
	}
	
	public static void addToDoLater(JMenuItem item, ActionListener listener) {
		item.addActionListener(listener);
	}
	
	public static void removeAllListenersFromItem(JMenuItem item) {
		for(ActionListener listener : item.getActionListeners()) {
			item.removeActionListener(listener);
		}
	}
}
