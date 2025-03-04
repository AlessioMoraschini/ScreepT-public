package various.common.light.om;

import javax.swing.JTextArea;

public class TextPositionDTO {

	public static int caretPosition;
	public static int selectionStart;
	public static int selectionEnd;
	
	public JTextArea textArea;
	
	public TextPositionDTO(JTextArea textArea) {
		this.textArea = textArea;
		caretPosition = textArea.getCaretPosition();
		selectionStart = textArea.getSelectionStart();
		selectionEnd = textArea.getSelectionEnd();
	}
	
	public void resetPosition() {
		
		textArea.requestFocus();
		
		if(selectionStart == selectionEnd) {
			resetCaretPosition();
		} else {
			textArea.setSelectionStart(selectionStart);
			textArea.setSelectionEnd(selectionEnd);
		}
	}
	
	private void resetCaretPosition() {
		if(caretPosition > 0) {
			textArea.setCaretPosition(caretPosition - 1);
		} else try {
			textArea.setCaretPosition(caretPosition + 1);
		} catch(Exception e) {}
		textArea.setCaretPosition(caretPosition);
	}
}
