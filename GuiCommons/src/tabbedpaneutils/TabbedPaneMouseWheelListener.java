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
package tabbedpaneutils;

import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JTabbedPane;

public class TabbedPaneMouseWheelListener implements MouseWheelListener{

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		JTabbedPane pane = (JTabbedPane) e.getSource();
        int units = e.getWheelRotation();
        int oldIndex = pane.getSelectedIndex();
        int newIndex = oldIndex + units;

        if (newIndex < 0) {
        	pane.setSelectedIndex(0);
        } else if (newIndex >= pane.getTabCount()) {
        	pane.setSelectedIndex(pane.getTabCount() - 1);
        } else {
        	pane.setSelectedIndex(newIndex);
        }
        
        try {
			if(newIndex > 0) {
				scrollToNextTab(pane);
			} else {
				scrollToPreviousTab(pane);
			}
		} catch (Exception e1) {
		}
        
	}
	
	private void clickArrowButton(String actionKey, final JTabbedPane tabs) {
	    ActionMap map = tabs.getActionMap();
	    if (map != null) {
	        Action action = map.get(actionKey);
	        if (action != null && action.isEnabled()) {
	            action.actionPerformed(new ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, "", 0, 0));
	        }
	    }
	}
	
	public void scrollToNextTab(final JTabbedPane tabs) {
		clickArrowButton("scrollTabsForwardAction", tabs);
	}

	public void scrollToPreviousTab(final JTabbedPane tabs) {
		clickArrowButton("scrollTabsBackwardAction", tabs);
	}

}
