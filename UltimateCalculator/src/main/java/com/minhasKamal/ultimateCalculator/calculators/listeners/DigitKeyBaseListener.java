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
package com.minhasKamal.ultimateCalculator.calculators.listeners;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;

import com.minhasKamal.ultimateCalculator.calculators.advancedCalculator.AdvancedCalculatorGui;
import com.minhasKamal.ultimateCalculator.calculators.baseCalculator.BaseCalculatorGui;
import com.minhasKamal.ultimateCalculator.calculators.simpleCalculator.SimpleCalculatorGui;


public class DigitKeyBaseListener implements KeyEventDispatcher{
	
	BaseCalculatorGui basicGui;
	List<JButton> buttonsNumerical = new ArrayList<JButton>();
	List<JButton> buttonsOperation = new ArrayList<JButton>();
	JButton point;
	
	public DigitKeyBaseListener(JButton[] buttonsNumerical, JButton point, JButton[] baseOperation) {
		this.buttonsNumerical = Arrays.asList(buttonsNumerical);
		this.buttonsOperation = Arrays.asList(baseOperation);
		this.point = point;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		
		if (e.getID() == KeyEvent.KEY_PRESSED) {
			
			// catch numbers numpad (96 = 0 numpad)
			if(e.getKeyCode() >= 96 && e.getKeyCode() <= 105) {
				try {
					int i = e.getKeyCode() - 96;
					buttonsNumerical.get(i).doClick();
				} catch (Exception e2) {
				}
				
			// Numpad point
			}else if(e.getKeyCode() == 110) {
				try {
					point.doClick();
				} catch (Exception e2) {
				}
				
			// Operation +
			}else if(e.getKeyCode() == 107) {
				try {
					buttonsOperation.get(0).doClick();
				} catch (Exception e2) {
				}
			// Operation -
			}else if(e.getKeyCode() == 109) {
				try {
					buttonsOperation.get(1).doClick();
				} catch (Exception e2) {
				}
			// Operation *
			}else if(e.getKeyCode() == 106) {
				try {
					buttonsOperation.get(2).doClick();
				} catch (Exception e2) {
				}
			// Operation / -> Enter
			}else if(e.getKeyCode() == 111) {
				try {
					buttonsOperation.get(3).doClick();
				} catch (Exception e2) {
				}
			// Operation "=" -> Enter
			}else if(e.getKeyCode() == 10) {
				try {
					buttonsOperation.get(4).doClick();
				} catch (Exception e2) {
				}
			}
			
		}
		
		if (e.getID() == KeyEvent.KEY_RELEASED) {
		    
		
		}
		
		return false;
	}
	
	/**
	 * This function adds a key listener to extend the given one.
	 * Just pass the gui object in reference and call this method
	 */
	public static boolean addRightListenerForClass(Object instance) {
		
		if(instance instanceof BaseCalculatorGui) {
			addKeyHandlerBase((BaseCalculatorGui)instance);
			return true;
		
		}else if(instance instanceof SimpleCalculatorGui) {
			addKeyHandlerSimple((SimpleCalculatorGui)instance);
			return true;
			
		}else if(instance instanceof AdvancedCalculatorGui) {
			addKeyHandlerSimple((AdvancedCalculatorGui)instance);
			return true;
		}
		
		return false;
	}
	
	private static void addKeyHandlerBase(BaseCalculatorGui reference){
		
		DigitKeyBaseListener shortcutsListener = new DigitKeyBaseListener(
				reference.getjButtonNumerical(), reference.getjButtonPoint(), adaptOperators(reference.getjButtonOperator(), true));
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shortcutsListener);
	}
	
	private static void addKeyHandlerSimple(SimpleCalculatorGui reference){
		
		DigitKeyBaseListener shortcutsListener = new DigitKeyBaseListener(
				reference.getjButtonNumerical(), reference.getjButtonPoint(), adaptOperators(reference.getjButtonOperator(), false));
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shortcutsListener);
	}
	
	private static void addKeyHandlerSimple(AdvancedCalculatorGui reference){
		
		DigitKeyBaseListener shortcutsListener = new DigitKeyBaseListener(
				reference.getjButtonNumerical(), reference.getjButtonPoint(), adaptOperators(reference.getjButtonOperator(), false));
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shortcutsListener);
	}

	/**
	 * If basic calculator use baseCalc = true
	 * @param rawOperators
	 * @param baseCalc
	 * @return
	 */
	private static JButton[] adaptOperators(JButton[] rawOperators, boolean baseCalc) {
		JButton[] result = new JButton[5]; // +, -, *, /, =
		for(int i = 0; i < result.length; i++) {
			if (baseCalc) {
				result[i] = rawOperators[i];
			}else {
				if(i != 4) {
					result[i] = rawOperators[i];
				}else {
					// = is fifth element in simple and advanced calculator
					result[i] = rawOperators[i+1];
				}
			}
		}
		
		return result;
	}
}
