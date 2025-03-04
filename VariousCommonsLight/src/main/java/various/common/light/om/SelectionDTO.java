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
package various.common.light.om;

import javax.swing.JTextArea;

import various.common.light.gui.GuiUtils;

public class SelectionDTO {

	public String text;
	public int start = 0;
	public Integer end = null;
	public String selected;
	
	public SelectionDTO(int start, Integer end) {
		this.start = start;
		this.end = end;
		this.text = "";
	}

	public SelectionDTO(int start, Integer end, String text) {
		this(start, end);
		this.text = text;
	}

	public SelectionDTO(int start, Integer end, String text, String selected) {
		this(start, end, text);
		this.selected = selected;
	}

	@Override
	public String toString() {
		return "SelectionDTO [start=" + start + ", end=" + end + "]";
	}

	public static void applySelectionDTO(final SelectionDTO result, final JTextArea textArea) {
		if (result != null) {
			textArea.setText(result.text);
			
			GuiUtils.launchThreadSafeSwing(()->{
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
				}
				textArea.requestFocus();
				textArea.setSelectionStart(result.start);
				textArea.setSelectionEnd(result.end != null ? result.end.intValue() : result.end);
			});
		}
	}
	
}
