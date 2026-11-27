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
