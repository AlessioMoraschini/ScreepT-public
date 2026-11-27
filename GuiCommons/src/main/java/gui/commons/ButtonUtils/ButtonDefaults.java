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
