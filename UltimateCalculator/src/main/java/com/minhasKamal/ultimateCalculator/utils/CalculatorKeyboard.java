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
package com.minhasKamal.ultimateCalculator.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

/**
 * Keyboard input for the calculators, based on the typed character instead of the physical key:
 * it works with any layout (on an Italian keyboard "*" is Shift+"+" and "=" is Shift+"0"), with the numpad,
 * and Enter/Backspace/Delete act as "=", "&lt;B" and "C".
 * Every key is routed to the button with the same text, so the behaviour is always the one of the mouse.
 */
public final class CalculatorKeyboard implements KeyEventDispatcher {

	private final JFrame frame;
	private final Map<Character, AbstractButton> byChar = new HashMap<>();
	private final Map<String, AbstractButton> byText = new HashMap<>();

	private CalculatorKeyboard(JFrame frame) {
		this.frame = frame;
	}

	/**
	 * Installs the keyboard handling on a calculator frame whose content is already built.
	 * @param hexMode letters A-F (any case) are digits, so the other letter shortcuts are not used
	 */
	public static void install(JFrame frame, boolean hexMode) {

		CalculatorKeyboard keyboard = new CalculatorKeyboard(frame);
		keyboard.collect(frame.getContentPane());
		keyboard.mapCharacters(hexMode);

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyboard);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyboard);
			}
		});
	}

	private void collect(Component component) {
		if (component instanceof AbstractButton) {
			AbstractButton button = (AbstractButton) component;
			dropLayoutDependentBindings(button);
			String text = button.getText();
			if (text != null && !text.isEmpty()) {
				byText.put(text, button);
			}
		}
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				collect(child);
			}
		}
	}

	/** Old bindings assumed a US layout and are replaced by this handler: only Ctrl shortcuts are kept */
	private void dropLayoutDependentBindings(AbstractButton button) {
		InputMap inputMap = button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke[] keys = inputMap.keys();
		if (keys == null) {
			return;
		}
		for (KeyStroke key : keys) {
			if ((key.getModifiers() & InputEvent.CTRL_DOWN_MASK) == 0) {
				inputMap.remove(key);
			}
		}
	}

	private void mapCharacters(boolean hexMode) {

		for (Map.Entry<String, AbstractButton> entry : byText.entrySet()) {
			if (entry.getKey().length() == 1) {
				byChar.put(entry.getKey().charAt(0), entry.getValue());
			}
		}

		alias('*', "x");
		alias(',', ".");
		if (hexMode) {
			for (char letter = 'A'; letter <= 'F'; letter++) {
				alias(Character.toLowerCase(letter), String.valueOf(letter));
			}
		} else {
			alias('r', "sqrt");
			alias('m', "M");
			alias('s', "MS");
			alias('c', "C");
			alias('a', "AC");
		}
	}

	private void alias(char key, String buttonText) {
		AbstractButton button = byText.get(buttonText);
		if (button != null) {
			byChar.put(key, button);
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {

		if (!frame.isActive() || e.isControlDown() || e.isAltDown() || isTypingInTextField()) {
			return false;
		}

		AbstractButton target = null;
		if (e.getID() == KeyEvent.KEY_TYPED) {
			target = byChar.get(e.getKeyChar());
		} else if (e.getID() == KeyEvent.KEY_PRESSED) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					target = byText.get("=");
					break;
				case KeyEvent.VK_BACK_SPACE:
					target = byText.get("<B");
					break;
				case KeyEvent.VK_DELETE:
					target = byText.get("C");
					break;
				default:
					break;
			}
		}

		if (target != null && target.isEnabled()) {
			target.doClick(40);
			e.consume();
			return true;
		}
		return false;
	}

	/** Editable fields get their own typing: only read-only displays leave the keys to the calculator */
	private boolean isTypingInTextField() {
		Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return owner instanceof JTextComponent && ((JTextComponent) owner).isEditable();
	}
}
