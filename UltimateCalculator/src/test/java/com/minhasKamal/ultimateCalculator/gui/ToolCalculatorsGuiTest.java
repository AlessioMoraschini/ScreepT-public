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

import java.awt.KeyEventDispatcher;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.junit.Test;

import com.minhasKamal.ultimateCalculator.calculators.advancedCalculator.AdvancedCalculator;
import com.minhasKamal.ultimateCalculator.calculators.baseCalculator.BaseCalculator;
import com.minhasKamal.ultimateCalculator.calculators.dateCalculator.DateCalculator;
import com.minhasKamal.ultimateCalculator.calculators.equationCalculator.EquationCalculator;
import com.minhasKamal.ultimateCalculator.calculators.listeners.DigitKeyBaseListener;
import com.minhasKamal.ultimateCalculator.calculators.primeNumberHunter.PrimeNumberHunter;
import com.minhasKamal.ultimateCalculator.calculators.simpleCalculator.SimpleCalculator;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverter;
import com.minhasKamal.ultimateCalculator.mainFrame.UltimateCalculatorFrame;

/** Equation solver, unit converter, date calculator, prime hunter, mode menu and numpad listener driven through their windows. */
public class ToolCalculatorsGuiTest extends CalculatorWindowTestBase {

	private static void setText(final Object field, final String value) throws Exception {
		onEdt(new Callable<Void>() {
			public Void call() {
				((JTextField) field).setText(value);
				return null;
			}
		});
	}

	private static void select(final JComboBox<?> combo, final int index) throws Exception {
		onEdt(new Callable<Void>() {
			public Void call() {
				combo.setSelectedIndex(index);
				return null;
			}
		});
	}

	@Test
	public void equationCalculatorSolvesTheLinearSystems() throws Exception {
		EquationCalculator c = build(EquationCalculator.class);
		JTextField[] two = field(c, "jTFieldaxbyc");
		String[] in2 = { "1", "1", "3", "1", "-1", "1" };
		for (int i = 0; i < in2.length; i++) {
			setText(two[i], in2[i]);
		}
		clickButton((JButton) field(c, "jButtonaxbycSolve"));
		assertEquals("2.0", two[6].getText());
		assertEquals("1.0", two[7].getText());

		JTextField[] three = field(c, "jTFieldaxbyczd");
		// x + y + z = 6, x - y = -1, y + z = 5 -> (1, 2, 3)
		String[] in3 = { "1", "1", "1", "6", "1", "-1", "0", "-1", "0", "1", "1", "5" };
		for (int i = 0; i < in3.length; i++) {
			setText(three[i], in3[i]);
		}
		clickButton((JButton) field(c, "jButtonaxbyczdSolve"));
		assertEquals("1.0", three[12].getText());
		assertEquals("2.0", three[13].getText());
		assertEquals("3.0", three[14].getText());
	}

	@Test
	public void equationCalculatorSolvesQuadraticAndCubic() throws Exception {
		EquationCalculator c = build(EquationCalculator.class);
		JTextField[] quad = field(c, "jTFieldax2bxc");
		setText(quad[0], "1");
		setText(quad[1], "-3");
		setText(quad[2], "2");
		clickButton((JButton) field(c, "jButtonax2bxcSolve"));
		assertEquals("2.0", quad[3].getText());
		assertEquals("1.0", quad[4].getText());

		JTextField[] cubic = field(c, "jTFieldax3bx2cxd");
		String[] coefficients = { "1", "-6", "11", "6" };
		for (int i = 0; i < coefficients.length; i++) {
			setText(cubic[i], coefficients[i]);
		}
		clickButton((JButton) field(c, "jButtonax3bx2cxdSolve"));
		java.util.Set<Double> roots = new HashSet<Double>();
		for (int i = 4; i <= 6; i++) {
			roots.add(Double.parseDouble(cubic[i].getText()));
		}
		assertEquals(new HashSet<Double>(Arrays.asList(1.0, 2.0, 3.0)), roots);
	}

	@Test
	public void unitConverterConvertsWithinEachCategoryFromTheCombos() throws Exception {
		UnitConverter c = build(UnitConverter.class);
		JComboBox<?> category = field(c, "jCBoxTypeSelection");
		JComboBox<?>[] units = field(c, "jComboBoxType");
		JTextField[] io = field(c, "jTFieldIO");

		// length: 1500 Meter -> KiloMeter
		select(category, 0);
		setText(io[0], "1500");
		select(units[0], 5);
		select(units[1], 6);
		assertEquals("1.5", io[1].getText());

		// temperature: 100 Celsius -> Fahrenheit
		select(category, 2);
		setText(io[0], "100");
		select(units[0], 0);
		select(units[1], 1);
		assertEquals("212.0", io[1].getText());

		// time: 2 Hour -> Minute
		select(category, 5);
		setText(io[0], "2");
		select(units[0], 4);
		select(units[1], 3);
		assertEquals("120.0", io[1].getText());

		// weight: 1 Kilogram -> Gram
		select(category, 1);
		setText(io[0], "1");
		select(units[0], 2);
		select(units[1], 1);
		assertEquals("1000.0", io[1].getText());

		// every other category answers with a number (from the first unit to the second)
		for (int cat : new int[] { 3, 4, 6, 7 }) {
			select(category, cat);
			setText(io[0], "10");
			select(units[0], 0);
			select(units[1], 1);
			assertTrue("category " + cat + ": " + io[1].getText(), Double.parseDouble(io[1].getText()) > 0);
		}
	}

	@Test
	public void unitConverterConvertButtonAndInvalidInput() throws Exception {
		UnitConverter c = build(UnitConverter.class);
		JComboBox<?>[] units = field(c, "jComboBoxType");
		JTextField[] io = field(c, "jTFieldIO");
		setText(io[0], "3");
		clickButton((JButton) field(c, "jButtonConvert"));
		assertEquals("same unit keeps the value", "3.0", io[1].getText());
		// not a number: the combos clear the output without any dialog
		setText(io[0], "abc");
		select(units[1], 1);
		assertEquals("", io[1].getText());
		// changing the category resets the output
		setText(io[0], "5");
		select(units[1], 0);
		assertEquals("5.0", io[1].getText());
		select((JComboBox<?>) field(c, "jCBoxTypeSelection"), 1);
		assertEquals("", io[1].getText());
	}

	@Test
	public void dateCalculatorShowsTheDifferenceBetweenTwoDates() throws Exception {
		DateCalculator c = build(DateCalculator.class);
		JTextField[] day = field(c, "jTFieldDay");
		JTextField[] month = field(c, "jTFieldMonth");
		JTextField[] year = field(c, "jTFieldYear");
		JTextField output = field(c, "jTFieldOutput");
		// the second date starts as today
		java.util.Calendar today = java.util.Calendar.getInstance();
		assertEquals(today.get(java.util.Calendar.YEAR), Integer.parseInt(year[1].getText()));
		assertEquals(today.get(java.util.Calendar.MONTH) + 1, Integer.parseInt(month[1].getText()));
		setText(day[0], "15");
		setText(month[0], "6");
		setText(year[0], "1990");
		setText(day[1], "20");
		setText(month[1], "7");
		setText(year[1], "2020");
		clickButton((JButton) field(c, "jButtonCalculate"));
		assertTrue(output.getText(), output.getText().contains("30Y") && output.getText().contains("1M"));
	}

	@Test
	public void dateCalculatorRejectsTextInTheDateFields() throws Exception {
		DateCalculator c = build(DateCalculator.class);
		JTextField[] day = field(c, "jTFieldDay");
		setText(day[0], "xx");
		// a modal "Wrong Input" dialog opens: the test guards close it
		clickButton((JButton) field(c, "jButtonCalculate"));
		assertEquals("", ((JTextField) field(c, "jTFieldOutput")).getText());
	}

	@Test
	public void primeNumberHunterListsThePrimesOfARange() throws Exception {
		PrimeNumberHunter c = build(PrimeNumberHunter.class);
		JTextField[] range = field(c, "jTFieldFT");
		setText(range[0], "1");
		setText(range[1], "30");
		clickButton((JButton) field(c, "jButtonFind"));
		final JTextPane pane = field(c, "jTPaneOutput");
		final JTextField count = field(c, "jTFieldOutput");
		long deadline = System.currentTimeMillis() + 5000;
		String shown = "";
		while (System.currentTimeMillis() < deadline && !shown.endsWith("End.")) {
			Thread.sleep(20);
			shown = onEdt(new Callable<String>() {
				public String call() {
					return pane.getText();
				}
			});
		}
		assertEquals("2, 3, 5, 7, 11, 13, 17, 19, 23, 29, End.", shown);
		assertTrue(count.getText().trim().length() > 0);
	}

	@Test
	public void modeMenuOpensTheOtherCalculators() throws Exception {
		SimpleCalculator c = build(SimpleCalculator.class);
		JCheckBoxMenuItem[] modes = field(c, "jCBItemMode");
		assertTrue("the current mode is ticked", modes[0].isSelected());
		String[] titles = { "Advanced Calculator", "Base Calculator", "Equation Calculator", "Unit Converter", "Date Calculator", "Prime Number Hunter" };
		for (int i = 0; i < titles.length; i++) {
			// every mode creates its calculator and closes the one that asked
			final UltimateCalculatorFrame current = c;
			final JCheckBoxMenuItem item = modes[i + 1];
			onEdt(new Callable<Void>() {
				public Void call() {
					item.doClick(0);
					return null;
				}
			});
			assertFalse("the old window is closed", current.gui.isDisplayable());
			JFrame opened = visibleFrame(titles[i]);
			assertTrue("opened " + titles[i], opened != null);
			opened.dispose();
		}
		// the first mode returns to the simple calculator
		SimpleCalculator again = build(SimpleCalculator.class);
		assertEquals("Simple Calculator", again.gui.getTitle());
	}

	private static JFrame visibleFrame(String title) {
		for (Window w : Window.getWindows()) {
			if (w instanceof JFrame && title.equals(((JFrame) w).getTitle()) && w.isDisplayable()) {
				return (JFrame) w;
			}
		}
		return null;
	}

	@Test
	public void helpMenuOpensTheInstructionsWindow() throws Exception {
		AdvancedCalculator c = build(AdvancedCalculator.class);
		JMenuItem help = field(c, "jMenuItemInstruction");
		int before = Window.getWindows().length;
		clickButton(help);
		assertTrue("the instruction window was created", Window.getWindows().length > before);
	}

	@Test
	public void numpadListenerClicksTheCalculatorButtons() throws Exception {
		SimpleCalculator simple = build(SimpleCalculator.class);
		assertTrue(DigitKeyBaseListener.addRightListenerForClass(field(simple, "simpleCalcGui")));
		DigitKeyBaseListener listener = lastListener();
		numpad(listener, 96 + 7);
		numpad(listener, 107); // +
		numpad(listener, 96 + 5);
		numpad(listener, 10); // Enter acts as "="
		assertEquals("12.0", text(((JLabel[]) field(simple, "jLabelDisplay"))[1]));
		numpad(listener, 106); // *
		numpad(listener, 96 + 2);
		numpad(listener, 10);
		assertEquals("24.0", text(((JLabel[]) field(simple, "jLabelDisplay"))[1]));
		numpad(listener, 109); // -
		numpad(listener, 96 + 4);
		numpad(listener, 10);
		assertEquals("20.0", text(((JLabel[]) field(simple, "jLabelDisplay"))[1]));
		numpad(listener, 111); // /
		numpad(listener, 96 + 4);
		numpad(listener, 10);
		assertEquals("5.0", text(((JLabel[]) field(simple, "jLabelDisplay"))[1]));
		numpad(listener, 96 + 1);
		numpad(listener, 110); // point
		numpad(listener, 96 + 5);
		assertEquals("1.5", text(((JLabel[]) field(simple, "jLabelDisplay"))[0]));
		assertFalse("the listener never consumes the key", listener.dispatchKeyEvent(new KeyEvent(simple.gui, KeyEvent.KEY_RELEASED, 0, 0, 107, KeyEvent.CHAR_UNDEFINED)));
	}

	@Test
	public void numpadListenerSupportsBaseAndAdvancedCalculators() throws Exception {
		BaseCalculator base = build(BaseCalculator.class);
		assertTrue(DigitKeyBaseListener.addRightListenerForClass(field(base, "baseCalcGui")));
		DigitKeyBaseListener listener = lastListener();
		numpad(listener, 96 + 6);
		numpad(listener, 107);
		numpad(listener, 96 + 3);
		numpad(listener, 10);
		assertEquals("9", text(((JLabel[]) field(base, "jLabelDisplay"))[1]));

		AdvancedCalculator advanced = build(AdvancedCalculator.class);
		assertTrue(DigitKeyBaseListener.addRightListenerForClass(field(advanced, "AdvCalcGui")));
		DigitKeyBaseListener second = lastListener();
		numpad(second, 96 + 8);
		numpad(second, 106);
		numpad(second, 96 + 2);
		numpad(second, 10);
		assertEquals("16.0", text(field(advanced, "jLabelDisplay")));

		assertFalse("other objects are not supported", DigitKeyBaseListener.addRightListenerForClass("not a calculator"));
	}

	private static DigitKeyBaseListener lastListener() {
		DigitKeyBaseListener found = null;
		for (KeyEventDispatcher d : dispatchers()) {
			if (d instanceof DigitKeyBaseListener) {
				found = (DigitKeyBaseListener) d;
			}
		}
		assertTrue("listener installed", found != null);
		return found;
	}

	private static void numpad(final DigitKeyBaseListener listener, final int code) throws Exception {
		onEdt(new Callable<Void>() {
			public Void call() {
				listener.dispatchKeyEvent(new KeyEvent(new JFrame(), KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED));
				return null;
			}
		});
	}
}
