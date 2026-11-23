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
package com.minhasKamal.ultimateCalculator.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JLabel;

import org.junit.Test;

import com.minhasKamal.ultimateCalculator.calculators.advancedCalculator.AdvancedCalculator;
import com.minhasKamal.ultimateCalculator.calculators.baseCalculator.BaseCalculator;
import com.minhasKamal.ultimateCalculator.calculators.simpleCalculator.SimpleCalculator;

/** Simple, Base and Advanced calculators driven through their buttons and their keyboard handler. */
public class ArithmeticCalculatorsGuiTest extends CalculatorWindowTestBase {

	private static String entry(Object calc) throws Exception {
		return text(((JLabel[]) field(calc, "jLabelDisplay"))[0]);
	}

	private static String result(Object calc) throws Exception {
		return text(((JLabel[]) field(calc, "jLabelDisplay"))[1]);
	}

	@Test
	public void simpleCalculatorAddsSubtractsMultipliesDivides() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		type(c, "2");
		click(c, "+");
		assertEquals("the operator clears the entry", "", entry(c));
		type(c, "3");
		assertEquals("3", entry(c));
		click(c, "=");
		assertEquals("5.0", result(c));

		click(c, "AC");
		type(c, "9");
		click(c, "-");
		type(c, "12");
		click(c, "=");
		assertEquals("-3.0", result(c));

		click(c, "AC");
		type(c, "6");
		click(c, "x");
		type(c, "7");
		click(c, "=");
		assertEquals("42.0", result(c));

		click(c, "AC");
		type(c, "1");
		click(c, "/");
		type(c, "4");
		click(c, "=");
		assertEquals("0.25", result(c));
	}

	@Test
	public void simpleCalculatorChainsOperationsAndKeepsTheAnswer() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		type(c, "2");
		click(c, "+");
		type(c, "3");
		click(c, "+");
		assertEquals("5.0", result(c));
		type(c, "4");
		click(c, "=");
		assertEquals("9.0", result(c));
		// an operator right after "=" works on the previous answer
		click(c, "x");
		type(c, "2");
		click(c, "=");
		assertEquals("18.0", result(c));
	}

	@Test
	public void simpleCalculatorPowerAndSquareRoot() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		type(c, "2");
		click(c, "^");
		type(c, "10");
		click(c, "=");
		assertEquals("1024.0", result(c));
		click(c, "sqrt");
		assertEquals("32.0", result(c));
	}

	@Test
	public void simpleCalculatorEntryEditing() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		type(c, "12");
		click(c, ".");
		assertEquals("12.", entry(c));
		click(c, ".");
		assertEquals("a second decimal point is ignored", "12.", entry(c));
		type(c, "5");
		assertEquals("12.5", entry(c));
		click(c, "<B");
		assertEquals("12.", entry(c));
		click(c, "C");
		assertEquals("", entry(c));
		type(c, "7");
		click(c, "+");
		type(c, "1");
		click(c, "=");
		assertEquals("8.0", result(c));
		click(c, "AC");
		assertEquals("", entry(c));
		assertEquals("", result(c));
	}

	@Test
	public void simpleCalculatorMemoryStoresTheResult() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		type(c, "3");
		click(c, "+");
		type(c, "4");
		click(c, "=");
		click(c, "MS");
		click(c, "AC");
		click(c, "M");
		assertEquals("7.0", entry(c));
		click(c, "+");
		type(c, "1");
		click(c, "=");
		assertEquals("8.0", result(c));
	}

	@Test
	public void simpleCalculatorDivisionByZeroGivesZero() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		type(c, "5");
		click(c, "/");
		type(c, "0");
		click(c, "=");
		assertEquals("0", result(c));
	}

	@Test
	public void simpleCalculatorLimitsTheEntryLength() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		type(c, "123456789012345678901234");
		assertEquals(20, entry(c).length());
	}

	@Test
	public void keyboardIsRoutedToTheButtons() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		activate(c.gui);
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '8'));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '*'));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '5'));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, '\n'));
		assertEquals("40.0", result(c));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_PRESSED, KeyEvent.VK_DELETE, (char) 127));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '9'));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '7'));
		assertEquals("97", entry(c));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_PRESSED, KeyEvent.VK_BACK_SPACE, '\b'));
		assertEquals("9", entry(c));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, 'a'));
		assertEquals("", result(c));
		assertFalse("a character without button is not consumed", pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, 'z'));
	}

	@Test
	public void keyboardIsIgnoredWhenTheWindowIsNotActive() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		activate(null);
		assertFalse(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '8'));
		assertEquals("", entry(c));
	}

	@Test
	public void baseCalculatorWorksInEveryBase() throws Exception {
		BaseCalculator c = build(BaseCalculator.class);
		// decimal is the default
		type(c, "25");
		click(c, "+");
		type(c, "4");
		click(c, "=");
		assertEquals("29", result(c));

		click(c, "Hex");
		assertEquals("29 is shown in hexadecimal", "1D", result(c));
		click(c, "Bin");
		assertEquals("11101", result(c));
		click(c, "Oct");
		assertEquals("35", result(c));
		click(c, "Dec");
		assertEquals("29", result(c));

		click(c, "AC");
		click(c, "Hex");
		click(c, "F");
		click(c, "F");
		click(c, "+");
		type(c, "1");
		click(c, "=");
		assertEquals("100", result(c));

		click(c, "AC");
		click(c, "Bin");
		type(c, "101");
		click(c, "x");
		type(c, "11");
		click(c, "=");
		assertEquals("1111", result(c));
	}

	@Test
	public void baseCalculatorDisablesTheDigitsOutsideTheBase() throws Exception {
		BaseCalculator c = build(BaseCalculator.class);
		JButton[] digits = field(c, "jButtonNumerical");
		JButton[] letters = field(c, "jButtonAlphaNumerical");
		click(c, "Bin");
		assertTrue(digits[1].isEnabled());
		assertFalse(digits[2].isEnabled());
		assertFalse(letters[0].isEnabled());
		click(c, "Oct");
		assertTrue(digits[7].isEnabled());
		assertFalse(digits[8].isEnabled());
		click(c, "Dec");
		assertTrue(digits[9].isEnabled());
		assertFalse(letters[5].isEnabled());
		click(c, "Hex");
		assertTrue(letters[5].isEnabled());
		assertTrue(digits[9].isEnabled());
	}

	@Test
	public void baseCalculatorMemoryAndClearButtons() throws Exception {
		BaseCalculator c = build(BaseCalculator.class);
		type(c, "12");
		click(c, "+");
		type(c, "30");
		click(c, "=");
		assertEquals("42", result(c));
		click(c, "MS");
		click(c, "AC");
		click(c, "M");
		assertEquals("42", entry(c));
		click(c, "<B");
		assertEquals("4", entry(c));
		// "C" is also a hexadecimal digit: the clear button is taken from the field
		clickButton(((JButton[]) field(c, "jButtonClear"))[0]);
		assertEquals("", entry(c));
		// the entry stops growing at the width of the display
		type(c, "1234567890123456");
		assertEquals(15, entry(c).length());
	}

	@Test
	public void baseCalculatorKeyboardUsesHexLetters() throws Exception {
		BaseCalculator c = build(BaseCalculator.class);
		activate(c.gui);
		click(c, "Hex");
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, 'f'));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, 'A'));
		assertEquals("FA", entry(c));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '+'));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '1'));
		assertTrue(pressKey(c.gui, KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, '\n'));
		assertEquals("FB", result(c));
	}

	@Test
	public void advancedCalculatorEvaluatesExpressionsTypedWithTheButtons() throws Exception {
		AdvancedCalculator c = build(AdvancedCalculator.class);
		type(c, "2");
		click(c, "+");
		type(c, "3");
		click(c, "x");
		type(c, "4");
		click(c, "=");
		assertEquals("14.0", text(field(c, "jLabelDisplay")));

		click(c, "AC");
		click(c, "(");
		type(c, "2");
		click(c, "+");
		type(c, "3");
		click(c, ")");
		click(c, "x");
		type(c, "4");
		click(c, "=");
		assertEquals("20.0", text(field(c, "jLabelDisplay")));
	}

	@Test
	public void advancedCalculatorFunctionsConstantsAndAnswer() throws Exception {
		AdvancedCalculator c = build(AdvancedCalculator.class);
		click(c, "sqre");
		type(c, "9");
		click(c, "=");
		assertEquals("81.0", text(field(c, "jLabelDisplay")));
		click(c, "C");
		click(c, "Ans");
		click(c, "+");
		type(c, "1");
		click(c, "=");
		assertEquals("82.0", text(field(c, "jLabelDisplay")));
		click(c, "C");
		click(c, "cube");
		type(c, "3");
		click(c, "=");
		assertEquals("27.0", text(field(c, "jLabelDisplay")));
		click(c, "C");
		type(c, "5");
		click(c, "n!");
		click(c, "=");
		assertEquals("120", text(field(c, "jLabelDisplay")));
	}

	@Test
	public void advancedCalculatorShiftSwitchesTheInverseFunctions() throws Exception {
		AdvancedCalculator c = build(AdvancedCalculator.class);
		JButton[] ops = field(c, "jButtonComplexOperator");
		int tanX = ops[0].getX();
		click(c, "Shift");
		assertEquals("atan takes the place of tan", tanX, ops[2].getX());
		click(c, "<~Shift~>");
		assertEquals(tanX, ops[0].getX());
		assertTrue(ops[2].getX() != tanX);
	}

	@Test
	public void advancedCalculatorMemoryClearAndConstants() throws Exception {
		AdvancedCalculator c = build(AdvancedCalculator.class);
		type(c, "6");
		click(c, "x");
		type(c, "7");
		click(c, "=");
		assertEquals("42.0", text(field(c, "jLabelDisplay")));
		click(c, "MS");
		click(c, "AC");
		assertEquals("", text(field(c, "jTextDisplay")));
		click(c, "M");
		assertEquals("42.0", text(field(c, "jTextDisplay")));
		click(c, "<B");
		assertEquals("42.", text(field(c, "jTextDisplay")));
		click(c, "C");
		click(c, "PI");
		click(c, "E");
		assertEquals("3.141592654 2.718281828 ", text(field(c, "jTextDisplay")));
		click(c, "nPr");
		click(c, "nCr");
		click(c, "x 10^");
		assertTrue(text(field(c, "jTextDisplay")).endsWith("P C x 10 ^ "));
	}
}
