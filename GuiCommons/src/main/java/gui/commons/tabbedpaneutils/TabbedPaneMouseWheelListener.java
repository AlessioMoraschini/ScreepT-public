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

import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JTabbedPane;

public class TabbedPaneMouseWheelListener implements MouseWheelListener {

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		JTabbedPane pane = (JTabbedPane) e.getSource();
        int units = e.getWheelRotation();
        int oldIndex = pane.getSelectedIndex();
        int newIndex = oldIndex + units;

        try {
	        if (newIndex < 0) {
	        	pane.setSelectedIndex(0);
	        } else if (newIndex >= pane.getTabCount()) {
	        	pane.setSelectedIndex(pane.getTabCount() - 1);
	        } else {
	        	pane.setSelectedIndex(newIndex);
	        }

			if(newIndex > 0) {
				scrollToNextTab(pane);
			} else {
				scrollToPreviousTab(pane);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}

	private void clickArrowButton(String actionKey, final JTabbedPane tabs) {
	    try {
			ActionMap map = tabs.getActionMap();
			if (map != null) {
			    Action action = map.get(actionKey);
			    if (action != null && action.isEnabled()) {
			        action.actionPerformed(new ActionEvent(tabs, ActionEvent.ACTION_PERFORMED, "", 0, 0));
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void scrollToNextTab(final JTabbedPane tabs) {
		clickArrowButton("scrollTabsForwardAction", tabs);
	}

	public void scrollToPreviousTab(final JTabbedPane tabs) {
		clickArrowButton("scrollTabsBackwardAction", tabs);
	}

}
