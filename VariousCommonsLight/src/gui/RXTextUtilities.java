package gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

import utility.log.SafeLogger;
import utility.string.StringWorker;

/*
 *	A collection of static methods that provide added functionality for
 *  text components (most notably, JTextArea and JTextPane)
 *
 *  See also: javax.swing.text.Utilities
 */
public class RXTextUtilities {
	private static final SafeLogger logger = new SafeLogger(RXTextUtilities.class);

	/*
	 * Attempt to center the line containing the caret at the center of the scroll
	 * pane.
	 *
	 * @param component the text component in the sroll pane
	 */
	public static void centerLineInScrollPane(JTextComponent component) {
		Container container = SwingUtilities.getAncestorOfClass(JViewport.class, component);

		if (container == null)
			return;

		try {
			Rectangle r = component.modelToView(component.getCaretPosition());
			JViewport viewport = (JViewport) container;
			int extentHeight = viewport.getExtentSize().height;
			int viewHeight = viewport.getViewSize().height;

			int y = Math.max(0, r.y - ((extentHeight - r.height) / 2));
			y = Math.min(y, viewHeight - extentHeight);

			viewport.setViewPosition(new Point(0, y));
		} catch (BadLocationException ble) {
		}
	}

	/*
	 * Attempt to center the line horizontally containing the caret at the center of
	 * the scroll pane.
	 *
	 * @param component the text component in the sroll pane
	 */
	public static void centerLineFullInScrollPane(JTextComponent component) {
		centerLineFullInScrollPane(component, true, true);
	}

	public static void centerLineFullInScrollPane(JTextComponent component, boolean vertical, boolean horizontal) {
		Container container = SwingUtilities.getAncestorOfClass(JViewport.class, component);

		if (container == null)
			return;

		try {
			Rectangle r = component.modelToView(component.getCaretPosition());
			JViewport viewport = (JViewport) container;
			int extentWidth = viewport.getExtentSize().width;
			int viewWidth = viewport.getViewSize().width;

			int extentHeight = viewport.getExtentSize().height;
			int viewHeight = viewport.getViewSize().height;

			int x = r != null ? Math.max(0, r.x - ((extentWidth - r.width) / 2)) : 0;
			x = horizontal ? Math.min(x, viewWidth - extentWidth) : 0;

			int y = r != null ? Math.max(0, r.y - ((extentHeight - r.height) / 2)) : 0;
			y = vertical ? Math.min(y, viewHeight - extentHeight) : r.y;

			viewport.setViewPosition(new Point(x, y));
		} catch (Exception ble) {
			logger.warn("Error resetting position", ble);
		}
	}

	/*
	 * Return the column number at the Caret position.
	 *
	 * The column returned will only make sense when using a Monospaced font.
	 */
	public static int getColumnAtCaret(JTextComponent component) {
		// Since we assume a monospaced font we can use the width of a single
		// character to represent the width of each character

		FontMetrics fm = component.getFontMetrics(component.getFont());
		int characterWidth = fm.stringWidth("0");
		int column = 0;

		try {
			Rectangle r = component.modelToView(component.getCaretPosition());
			int width = r.x - component.getInsets().left;
			column = width / characterWidth;
		} catch (BadLocationException ble) {
		}

		return column + 1;
	}

	/*
	 * Return the line number at the Caret position.
	 */
	public static int getLineAtCaret(JTextComponent component) throws BadLocationException {
		if(component instanceof JTextArea) {
			return ((JTextArea)component).getLineOfOffset(component.getCaretPosition());

		} else {
			
			int caretPosition = component.getCaretPosition();
			Element root = component.getDocument().getDefaultRootElement();
			
			return root.getElementIndex(caretPosition) + 1;
		}
	}
	
	/**
	 * Get text block at current caret position delimited by document start/end or a blank line
	 * @throws BadLocationException 
	 */
	public static String getCurrentCaretTextBlock(JTextComponent component, String lineSeparator, boolean selectText) throws BadLocationException {
		if(component == null || StringWorker.isEmpty(component.getText())) 
			return "";
		
		StringBuilder builder = new StringBuilder();
		
		// Search back from current line to first blank line (or document start)
		String temp = "initialized";
		int line = getLineAtCaret(component);
		while(line >= 0) {
			temp = getLineText(component, line, false);
			if(StringWorker.isEmpty(temp)) {
				line++;
				temp = getLineText(component, line, false);
				break;
			}
			
			if(line > 0)
				line --;
			else
				break;
		}
		int start = getStartOfLine(component, line);
		
		
		// Search forward until next blank line, and add to stringBuilder
		int totLines = getLines(component);
		int separatorsAdded = 0;
		while(line < totLines && !StringWorker.isEmpty(temp)) {
			temp = getLineText(component, line, false);
			
			if(!StringWorker.isEmpty(temp)) {
				builder.append(temp);
			
			} else {
				line --;
				break;
			}
			
			boolean lastLine = line == totLines - 1;
			if(line < totLines && !lastLine && !StringWorker.isEmpty(getLineText(component, line+1, false))) {
				builder.append(lineSeparator);
				separatorsAdded ++;
			}
			
			line ++;
		}
		
		String output = builder.toString();
		int end = start + (output.length() - (lineSeparator.length() * separatorsAdded) + separatorsAdded);
		
		if(selectText)
			setSelection(component, start, end);
		
		return output;
	}
	
	public static String getCurrentCaretLineText(JTextComponent component, boolean selectText) throws BadLocationException {
		if(component == null || StringWorker.isEmpty(component.getText())) 
			return "";
		
		int currLine = getLineAtCaret(component);
		int start = getStartOfLine(component, currLine);
		int end = getEndOfLine(component, currLine) - 1;
		
		if(selectText)
			setSelection(component, start, end);
		
		return component.getText(start, end-start);
	}
	
	public static void setSelection(JTextComponent component, int start, int end) {
		SwingUtilities.invokeLater(() -> {
			int safeEnd = (end > component.getText().length()) ? component.getText().length() - 1 : end;
			int safeStart = (start < 0) ? 0 : start; 
			
			try {
				component.requestFocus();
				component.setSelectionStart(safeStart);
				component.setSelectionEnd(safeEnd);
			} catch (Exception e) {
				logger.warn("Cannot apply selection to component");
			}
		});
	}

	/**
	 * Line >= 0
	 */
	public static String getLineText(JTextComponent component, int line, boolean selectText) throws BadLocationException {
		if(component == null || StringWorker.isEmpty(component.getText())) 
			return "";
		
		int start, end;
		
		if(component instanceof JTextArea) {
			int lineCount = ((JTextArea)component).getLineCount();
			if(line >= lineCount)
				line = lineCount - 1;
			
			end = ((JTextArea)component).getLineEndOffset(line);
			start = ((JTextArea)component).getLineStartOffset(line);
		} else {
			start = Utilities.getRowStart(component, line);
			end = Utilities.getRowEnd(component, line);    
		}
		
		if(selectText) {
			component.setSelectionStart(start);
			component.setSelectionEnd(end);
			component.setCaretPosition(end);
		}
		String normalized = StringWorker.normalizeStringToLF(component.getText(start, end-start));
		
		if(normalized.endsWith("\n"))
			return normalized.substring(0, Math.max(0, normalized.length() - 1));
		else
			return normalized.substring(0, Math.max(0, normalized.length()));
	}

	/*
	 * Return the number of lines of text in the Document
	 */
	public static int getLines(JTextComponent component) {
		Element root = component.getDocument().getDefaultRootElement();
		return root.getElementCount();
	}

	/*
	 * Position the caret at the start of a line.
	 */
	public static int gotoStartOfLine(JTextComponent component, int line) {
		int startOfLineOffset = getStartOfLine(component, line);
		component.setCaretPosition(startOfLineOffset);
		
		return startOfLineOffset;
	}
	
	/**
	 * Line >= 0
	 */
	public static void gotoLine(JTextArea component, int line) throws BadLocationException {
		component.setCaretPosition(component.getLineStartOffset(line));
	}
	
	/*
	 * Position the caret at the start of a line.
	 */
	public static int getStartOfLine(JTextComponent component, int line) {
		Element root = component.getDocument().getDefaultRootElement();
		line = Math.max(line, 1);
		line = Math.min(line, root.getElementCount());
		return root.getElement(line - 1).getStartOffset();
	}
	
	/**
	 * Line >= 1
	 */
	public static int gotoEndOfLine(JTextComponent component, int line) {
		int endOfLineOffset = getEndOfLine(component, line);
		component.setCaretPosition(endOfLineOffset);
		
		return endOfLineOffset;
	}

	/**
	 * Line >= 1
	 */
	public static int getEndOfLine(JTextComponent component, int line) {
		Element root = component.getDocument().getDefaultRootElement();
		line = Math.max(line, 1);
		line = Math.min(line, root.getElementCount());
		return root.getElement(line - 1).getEndOffset();
	}

	/*
	 * Position the caret on the first word of a line.
	 */
	public static void gotoFirstWordOnLine(final JTextComponent component, int line) {
		gotoStartOfLine(component, line);

		// The following will position the caret at the start of the first word

		try {
			int position = component.getCaretPosition();
			String first = component.getDocument().getText(position, 1);

			if (Character.isWhitespace(first.charAt(0))) {
				component.setCaretPosition(Utilities.getNextWord(component, position));
			}
		} catch (Exception e) {
		}
	}

	/*
	 * Return the number of lines of text, including wrapped lines.
	 */
	public static int getWrappedLines(JTextComponent component) {
		int lines = 0;

		View view = component.getUI().getRootView(component).getView(0);

		int paragraphs = view.getViewCount();

		for (int i = 0; i < paragraphs; i++) {
			lines += view.getView(i).getViewCount();
		}

		return lines;
	}

	/**
	 * @param blockMode if true all non bloank contiguous lines will be used, else only the current caret line
	 * @param lineSeparator the separator to put between the retrieved lines
	 */
	public static String getSelectedOrBlockText(JTextArea textArea, boolean blockMode, String lineSeparator) throws BadLocationException {
		if(textArea == null || StringWorker.isEmpty(textArea.getText()))
			return "";
		
		if(!StringWorker.isEmpty(textArea.getSelectedText()))
			return StringWorker.normalizeStringToEol(textArea.getSelectedText(), lineSeparator);
		else if(blockMode)
			return getCurrentCaretTextBlock(textArea, lineSeparator, true);
		else
			return getCurrentCaretLineText(textArea, true);
			
	}

}
