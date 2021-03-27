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
package gui.commons.ButtonUtils;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ButtonDefaults {
	
	public static void setDefaultOnFocus(JButton button, JPanel panel) {

		button.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				panel.getRootPane().setDefaultButton(button);
			}
		});
	}
	
	public static void setDefaultOnFocus(JButton button, JFrame panel) {

		button.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				panel.getRootPane().setDefaultButton(null);
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				panel.getRootPane().setDefaultButton(button);
			}
		});
	}
}
