package various.common.light.utility;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.text.JTextComponent;

import various.common.light.utility.manipulation.ConversionUtils;

public class KeyListenerUtils {

	public static KeyListener getKeyListenerHexTxtField(JTextComponent textComponent, String separator, int increment, int min, int max) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(keyUp(e) || keyDown(e)) {
					int incr;
					if (keyUp(e)) {
						incr = Math.abs(increment);
					} else {
						incr = - Math.abs(increment);
					}
					
					textComponent.setText(ConversionUtils.updateStringHex(textComponent.getText(), incr, min, max));
				}
				super.keyPressed(e);
			}
		};
	}

	public static KeyListener getKeyListenerIntTxtField(JTextComponent textComponent, String separator, int increment, int min, int max) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(keyUp(e) || keyDown(e)) {
					int caretPosition = textComponent.getCaretPosition();
					
					String textColor = textComponent.getText();
					int position = ConversionUtils.getIndexOfCaretPositionWithSeparator(textComponent, separator);
					String toUpdate = ConversionUtils.getComponentI(textColor, separator, position);
					String updated = toUpdate;
					if (keyUp(e)) {
						updated = ConversionUtils.updateStringInt(toUpdate, Math.abs(increment), max, min);
					} else {
						updated = ConversionUtils.updateStringInt(toUpdate, - Math.abs(increment), max, min);
					}
					
					updated = ConversionUtils.updateColorComponentInString(textColor, updated, separator, position);
					textComponent.setText(updated);
					textComponent.setCaretPosition(caretPosition);
				}
				super.keyPressed(e);
			}
		};
	}
	
	public static KeyListener getKeyListenerIntTxtField(JTextComponent textComponent, String separator, int increment, int min, int[] max) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(keyUp(e) || keyDown(e)) {
					int caretPosition = textComponent.getCaretPosition();
					
					String textColor = textComponent.getText();
					int position = ConversionUtils.getIndexOfCaretPositionWithSeparator(textComponent, separator);
					String toUpdate = ConversionUtils.getComponentI(textColor, separator, position);
					String updated = toUpdate;
					if (keyUp(e)) {
						updated = ConversionUtils.updateStringInt(toUpdate, Math.abs(increment), max[position], min);
					} else {
						updated = ConversionUtils.updateStringInt(toUpdate, - Math.abs(increment), max[position], min);
					}
					
					updated = ConversionUtils.updateColorComponentInString(textColor, updated, separator, position);
					textComponent.setText(updated);
					textComponent.setCaretPosition(caretPosition);
				}
				super.keyPressed(e);
			}
		};
	}

	public static KeyListener getKeyListenerFloatTxtField(JTextComponent textComponent, String separator, float increment, float min, float max) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(keyUp(e) || keyDown(e)) {
					int caretPosition = textComponent.getCaretPosition();
					String textColor = textComponent.getText();
					int position = ConversionUtils.getIndexOfCaretPositionWithSeparator(textComponent, separator);
					String toUpdate = ConversionUtils.getComponentI(textColor, separator, position);
					String updated = toUpdate;
					if (keyUp(e)) {
						updated = ConversionUtils.updateStringFloat(toUpdate,  Math.abs(increment), max, min);
					} else {
						updated = ConversionUtils.updateStringFloat(toUpdate, - Math.abs(increment), max, min);
					}
					
					updated = ConversionUtils.updateColorComponentInString(textColor, updated, separator, position);
					textComponent.setText(updated);
					textComponent.setCaretPosition(caretPosition);
				}
				super.keyPressed(e);
			}
		};
	}

	public static boolean keyUp(KeyEvent e) {
		return e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_KP_UP;
	}

	public static boolean keyDown(KeyEvent e) {
		return e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_KP_DOWN;
	}

}
