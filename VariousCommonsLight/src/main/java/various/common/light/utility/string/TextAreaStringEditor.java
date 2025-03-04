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
package various.common.light.utility.string;

import javax.swing.JTextArea;
import javax.swing.text.View;

import various.common.light.gui.GuiUtils;
import various.common.light.om.SelectionDTO;
import various.common.light.om.SelectionDtoFull;
import various.common.light.utility.log.SafeLogger;

public class TextAreaStringEditor {
	
	private static SafeLogger logger = new SafeLogger(TextAreaStringEditor.class);
	
	public static void makeUppercasePostDotsNSeparators(JTextArea textArea) {
		
		SelectionDtoFull txtAreaDto = new SelectionDtoFull(textArea);
		SelectionDTO result = getUppercasePostDotsNSeparators(txtAreaDto);

		SelectionDTO.applySelectionDTO(result, textArea);
	}

	public static SelectionDTO getUppercasePostDotsNSeparators(SelectionDtoFull selectionDtoFull) {
		SelectionDTO result = new SelectionDTO(selectionDtoFull.first, selectionDtoFull.last, "");

		if (selectionDtoFull.selected) {
			result.text = selectionDtoFull.text
					.concat(StringWorker.upperAfterDots(selectionDtoFull.selection, '.').concat(selectionDtoFull.end));
		} else {
			result.text = StringWorker.upperAfterDots(selectionDtoFull.getCompleteString(), '.');
		}
		
		return result;
	}

	public static void makeLowercasePostDotsNSeparators(JTextArea textArea) {
		
		SelectionDtoFull txtAreaDto = new SelectionDtoFull(textArea);
		SelectionDTO result = getLowercasePostDotsNSeparators(txtAreaDto);

		SelectionDTO.applySelectionDTO(result, textArea);
	}
	
	public static SelectionDTO getLowercasePostDotsNSeparators(SelectionDtoFull selectionDtoFull) {
		SelectionDTO result = new SelectionDTO(selectionDtoFull.first, selectionDtoFull.last, "");
		
		if (selectionDtoFull.selected) {
		result.text = result.text
				.concat(StringWorker.lowerAfterDots(selectionDtoFull.selection, '.').concat(selectionDtoFull.end));
		} else {
			result.text = StringWorker.lowerAfterDots(selectionDtoFull.getCompleteString(), '.');
		}

		return result;
	}
	
	public static void makeWordsUppercase(JTextArea textArea) {
		SelectionDTO result = getWordsUppercase(new SelectionDtoFull(textArea));
		SelectionDTO.applySelectionDTO(result, textArea);
	}
	
	public static SelectionDTO getWordsUppercase(SelectionDtoFull selectionDtoFull) {
		String textBackup = new String(selectionDtoFull.text);
		String selected = selectionDtoFull.selection;
		String start = "";
		String end = "";
		int strtN = 0;
		int endN = textBackup.length();
		if ("".equals(selected)) {
			// select ALL if no selection
			selected = textBackup;
		} else {
			strtN = selectionDtoFull.first;
			endN = selectionDtoFull.last;
			start = textBackup.substring(0, strtN);
			end = textBackup.substring(endN, textBackup.length());
		}
		String backupText = new String(selected);// create backup copy by value

		selected = StringWorker.uppercaseEveryFirstLetterNoSpace(selected);
		if (selected.equals(backupText)) {
			selected = StringWorker.lowerCaseEveryFirstLetterNoSpace(selected);
		}
		
		selected = start + selected + end;
		
		logger.debug("Tt Tt function start selection: " + strtN);
		logger.debug("Tt Tt function end selection: " + endN);
		
		return new SelectionDTO(strtN, endN, selected);
	}
	
	public static void makeToggleUppercase(JTextArea textArea) {
		SelectionDTO result = getToggleUppercase(new SelectionDtoFull(textArea), false);
		SelectionDTO.applySelectionDTO(result, textArea);
	}

	public static void makeLowercaseSelection(JTextArea textArea) {
		SelectionDTO result = getToggleUppercase(new SelectionDtoFull(textArea), true);
		SelectionDTO.applySelectionDTO(result, textArea);
	}
	
	public static SelectionDTO getToggleUppercase(SelectionDtoFull selectionDtoFull, boolean forceLowerCase) {
		String text = selectionDtoFull.text;
		String textBackup = new String(text);
		String selected = selectionDtoFull.selection;
		String start = "";
		String end = "";
		int strtN = 0;
		int endN = text.length();
		if ("".equals(selected)) {
			// select ALL if no selection
			selected = new String(text);
		} else {
			strtN = selectionDtoFull.first;
			endN = selectionDtoFull.last;
			start = textBackup.substring(0, strtN);
			end = textBackup.substring(endN, textBackup.length());
		}
		
		if (!forceLowerCase && !StringWorker.isStringUpperCase(selected)) {
			selected = selected.toUpperCase();
		} else {
			selected = selected.toLowerCase();
		}

		selected = start + selected + end;
		
		logger.debug("TT function start selection: " + strtN);
		logger.debug("TT function start selection: " + endN);
		
		return new SelectionDTO(strtN, endN, selected, selected);
	}

	public static int getCurrentCaretLine(JTextArea textArea) {
		try {
			int offset = textArea.getCaretPosition();
			return textArea.getLineOfOffset(offset);
		} catch (Exception e) {
			return -1;
		}
	}

	public static void replaceTextAreaSelection(JTextArea textArea, String selectionReplacement, Runnable doAfter, boolean ifNotSelThenAppend) {
		
		SelectionDtoFull selDto = new SelectionDtoFull(textArea);
		textArea.setCursor(GuiUtils.CURSOR_WAIT);
		selectionReplacement = selectionReplacement == null ? "" : selectionReplacement;
		
		try {

			selDto.replaceSelection(selectionReplacement, true, ifNotSelThenAppend);
//			textArea.setText(selDto.getCompleteString());
			selDto.applySelectionDTO(textArea);

		} catch (Exception e) {
			logger.error("Error with textarea selection replacement", e);
		
		} finally {
			
			textArea.setCursor(GuiUtils.CURSOR_DEFAULT);
			if(doAfter != null) {
				doAfter.run();
			}
		}
	}
	
	public static void replaceTextAreaSelectionOrInsert(JTextArea textArea, String selectionReplacement, Runnable doAfter) {
		
		SelectionDtoFull selDto = new SelectionDtoFull(textArea);
		textArea.setCursor(GuiUtils.CURSOR_WAIT);
		selectionReplacement = selectionReplacement == null ? "" : selectionReplacement;
		
		try {

			selDto.replaceSelectionOrInsAtGivenPosition(selectionReplacement, true, textArea.getCaretPosition());
//			textArea.setText(selDto.getCompleteString());
			selDto.applySelectionDTO(textArea);

		} catch (Exception e) {
			logger.error("Error with textarea selection replacement", e);
		
		} finally {
			
			textArea.setCursor(GuiUtils.CURSOR_DEFAULT);
			if(doAfter != null) {
				doAfter.run();
			}
		}
	}

	/*
	 *  Return the number of lines of text, including wrapped lines.
	 */
	public static int getWrappedLines(JTextArea component)
	{
		View view = component.getUI().getRootView(component).getView(0);
		int preferredHeight = (int)view.getPreferredSpan(View.Y_AXIS);
		int lineHeight = component.getFontMetrics( component.getFont() ).getHeight();
		return preferredHeight / lineHeight;
	}
}
