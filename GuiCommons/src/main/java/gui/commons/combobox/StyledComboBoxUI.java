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
package gui.commons.combobox;

import java.awt.Rectangle;

import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

public class StyledComboBoxUI extends BasicComboBoxUI {
	protected ComboPopup createPopup() {
		BasicComboPopup popup = new BasicComboPopup(comboBox) {
			private static final long serialVersionUID = -7591549935284626568L;

			@Override
			protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
				return super.computePopupBounds(px, py, Math.max(comboBox.getPreferredSize().width, pw), ph);
			}
		};
		popup.getAccessibleContext().setAccessibleParent(comboBox);
		return popup;
	}
}

class StyledComboBox extends JComboBox<Object> {
	private static final long serialVersionUID = -1771296583702725887L;

	public StyledComboBox() {
		setUI(new StyledComboBoxUI());
	}
}
