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
package gui.commons.guistyle;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Map;

public class TextGuiUtils {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void underlineText(Component sourceElem) {
		Map attributes = sourceElem.getFont().getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		sourceElem.setFont(sourceElem.getFont().deriveFont(attributes));
	}
	
	public static void hoverForeground(Component sourceElem, Color hoverColor) {
		Color backupCol = sourceElem.getForeground();
		sourceElem.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				sourceElem.setForeground(backupCol);
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				sourceElem.setForeground(backupCol);
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				sourceElem.setForeground(hoverColor);
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});
	}
}
