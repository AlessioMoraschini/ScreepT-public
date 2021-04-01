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

import java.io.File;

import javax.swing.JTextArea;

import various.common.light.gui.GuiUtils;
import various.common.light.utility.string.StringWorker;

public class SelectionDtoFull {

	public File file;
	public String prefix = "";
	public String text = "";
	public String selection = "";
	public String end = "";
	public int first = 0;
	public int last = 0;
	public int length = 0;
	public boolean selected;
	public JTextArea textArea;

	public SelectionDtoFull(JTextArea textArea) {
		this(textArea, null);
	}

	public SelectionDtoFull(JTextArea textArea, File file) {
		this.file = file;
		textArea.requestFocus();
		this.textArea = textArea;
		first = textArea.getSelectionStart();
		last = textArea.getSelectionEnd();
		text = textArea.getText();
		length = text.length();
		selection = StringWorker.nullToEmpty(textArea.getSelectedText());
		end = text.substring(last, text.length());
		prefix = text.substring(0, first);
		if(first == last || "".equals(selection)){
			selected = false;
		}else {
			selected = true;
		}
	}

	public void replaceSelection(String replacement, boolean makeNewSelectedIfNothingSelected, boolean ifNotSelThenAppend) {
		if(replacement != null) {
			selection = replacement;
			if(selected) {
				last = first + replacement.length();
				text = getCompleteString();
				end = text.substring(last, text.length());
				length = text.length();
			} else {
				if(ifNotSelThenAppend)
					appendText(replacement, makeNewSelectedIfNothingSelected);
				else
					replaceAllText(replacement, makeNewSelectedIfNothingSelected);
			}
		}
	}

	public void replaceSelectionOrInsAtGivenPosition(String replacement, boolean makeNewSelectedIfNothingSelected, int lineToInsertAt) throws Exception {
		if(replacement != null) {
			selection = replacement;
			if(selected) {
				text = getCompleteString();
				length = text.length();
				last = first + replacement.length();
				end = text.substring(last, text.length());
			} else {
				insertText(replacement, lineToInsertAt);
			}
		}
	}

	public void appendText(String textToAppend, boolean makeSelected) {
		if(textToAppend != null) {
			selection = textToAppend;
			prefix = text;
			end = "";
			if(makeSelected) {
				first = length;
				last = first + textToAppend.length();
			}
			text = getCompleteString();
			length = text.length();
		}
	}

	public String getSelectedOrAllText() {
		return selected ? selection : getCompleteString();
	}

	public void insertText(String textToInsert, int caretPos) {
		if(!StringWorker.isEmptyNoTrim(textToInsert)) {
			selection = textToInsert;
			prefix = text.substring(0, caretPos);
			end = text.substring(caretPos);
			first = caretPos;
			last = first + textToInsert.length();
			text = getCompleteString();
			length = text.length();
		}
	}

	public void replaceAllText(String textReplacement, boolean makeSelected) {
		textReplacement = StringWorker.trimToEmpty(textReplacement);
		end = "";
		if(makeSelected) {
			prefix = "";
			selection = textReplacement;
			first = 0;
			last = textReplacement.length();
		} else {
			prefix = textReplacement;
			selection = "";
			end = "";
			first = textReplacement.length();
			last = textReplacement.length();
		}

		text = getCompleteString();
		length = text.length();

		applySelectionDTO(textArea);
	}

	public String getCompleteString() {
		return prefix.concat(selection).concat(end);
	}

	public void applySelectionDTO(final JTextArea textArea) {
		applySelectionDTO(true, textArea);
	}

	public void applySelectionDTO(boolean setText, final JTextArea textArea) {
		if(setText)
			textArea.setText(text);

		GuiUtils.launchThreadSafeSwing(()->{
			try {
				Thread.sleep(50);
				GuiUtils.giveFocusToComponent(textArea);
				textArea.setSelectionStart(first);
				textArea.setSelectionEnd(last);
			} catch (Exception e) {
			}
		});
	}
}
