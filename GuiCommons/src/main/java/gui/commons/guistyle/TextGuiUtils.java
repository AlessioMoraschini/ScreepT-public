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
