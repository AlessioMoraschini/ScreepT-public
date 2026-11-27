/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Ultimate Calculator - Copyright (c) 2016 Minhas Kamal (original project)
 * Modifications Copyright (C) 2019-2026 AM-Design-Development - (Alessio Moraschini)
 *
 * Licensed under the MIT License: see the file license/LICENSE.txt of this module.
 */
package com.minhasKamal.ultimateCalculator.calculators;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.minhasKamal.ultimateCalculator.calculators.advancedCalculator.AdvancedCalculatorOperation;
import com.minhasKamal.ultimateCalculator.calculators.advancedCalculator.AdvancedCalculatorOperationsExecutor;
import com.minhasKamal.ultimateCalculator.calculators.baseCalculator.BaseCalculatorOperation;
import com.minhasKamal.ultimateCalculator.calculators.dateCalculator.DateCalculatorOperation;
import com.minhasKamal.ultimateCalculator.calculators.equationCalculator.EquationSovlerOperation;
import com.minhasKamal.ultimateCalculator.calculators.primeNumberHunter.PrimeNumberHunterOperation;

/** Arithmetic engines of the calculators (no windows involved). */
public class CalculatorsLogicTest {

	@Test
	public void baseCalculatorArithmeticInEveryBase() {
		BaseCalculatorOperation b = new BaseCalculatorOperation();
		// 1 binary, 2 octal, 3 decimal, 4 hexadecimal
		assertEquals("1000", b.operation(1, "101", "11", "+"));
		assertEquals("10", b.operation(1, "101", "11", "-"));
		assertEquals("1111", b.operation(1, "101", "11", "x"));
		assertEquals("24", b.operation(2, "17", "5", "+"));
		assertEquals("12", b.operation(2, "17", "5", "-"));
		assertEquals("113", b.operation(2, "17", "5", "*"));
		assertEquals("3", b.operation(2, "17", "5", "/"));
		assertEquals("29", b.operation(3, "25", "4", "+"));
		assertEquals("21", b.operation(3, "25", "4", "-"));
		assertEquals("100", b.operation(3, "25", "4", "x"));
		assertEquals("6", b.operation(3, "25", "4", "/"));
		assertEquals("29", b.operation(4, "1F", "A", "+"));
		assertEquals("15", b.operation(4, "1F", "A", "-"));
		assertEquals("136", b.operation(4, "1F", "A", "x"));
		assertEquals("3", b.operation(4, "1F", "A", "/"));
	}

	@Test
	public void baseConversionsAgreeWithIntegerParsing() {
		BaseCalculatorOperation b = new BaseCalculatorOperation();
		int[] radix = { 0, 2, 8, 10, 16 };
		for (int value : new int[] { 1, 7, 25, 31, 255, 1000, 4095 }) {
			for (int from = 1; from <= 4; from++) {
				for (int to = 1; to <= 4; to++) {
					String src = Integer.toString(value, radix[from]).toUpperCase();
					// the converter refuses long operands for the "wide" -> "narrow" conversions (see its length checks)
					if ((from == 2 && to == 1 && src.length() >= 7) || (from == 3 && to == 1 && src.length() >= 6)
							|| (from == 4 && to == 1 && src.length() >= 5) || (from == 4 && to == 2 && src.length() >= 10))
						continue;
					String expected = Integer.toString(value, radix[to]).toUpperCase();
					assertEquals(src + " " + from + "->" + to, expected, b.conversion(from, to, src).toUpperCase());
				}
			}
		}
	}

	@Test
	public void advancedBinaryAndUnaryOperators() {
		AdvancedCalculatorOperation o = new AdvancedCalculatorOperation();
		assertEquals(10.0, Double.parseDouble(o.operation("7", "3", "+")), 0);
		assertEquals(4.0, Double.parseDouble(o.operation("7", "3", "-")), 0);
		assertEquals(21.0, Double.parseDouble(o.operation("7", "3", "x")), 0);
		assertEquals(7.0 / 3, Double.parseDouble(o.operation("7", "3", "/")), 1e-12);
		assertEquals(343.0, Double.parseDouble(o.operation("7", "3", "^")), 1e-9);
		assertEquals(35.0, Double.parseDouble(o.operation("7", "3", "C")), 1e-9);
		assertEquals(210.0, Double.parseDouble(o.operation("7", "3", "P")), 1e-9);
		assertEquals(1.0, Double.parseDouble(o.operation("7", "3", "mod")), 1e-9);
		assertEquals("", o.operation("7", "3", "unknown"));
		assertEquals(3.0, Double.parseDouble(o.operation("9", "sqrt")), 1e-12);
		assertEquals(Math.cbrt(9), Double.parseDouble(o.operation("9", "cbrt")), 1e-9);
		assertEquals("362880", o.operation("9", "fact"));
		assertEquals(81.0, Double.parseDouble(o.operation("9", "sqre")), 0);
		assertEquals(729.0, Double.parseDouble(o.operation("9", "cube")), 0);
		assertEquals(Math.sin(Math.toRadians(9)), Double.parseDouble(o.operation("9", "sin")), 1e-9);
		assertEquals(Math.cos(Math.toRadians(9)), Double.parseDouble(o.operation("9", "cos")), 1e-9);
		assertEquals(Math.tan(Math.toRadians(9)), Double.parseDouble(o.operation("9", "tan")), 1e-9);
		assertEquals(Math.log10(9), Double.parseDouble(o.operation("9", "log")), 1e-9);
		assertEquals(Math.log(9), Double.parseDouble(o.operation("9", "ln")), 1e-9);
	}

	@Test
	public void infixEvaluationRespectsPrecedence() {
		AdvancedCalculatorOperationsExecutor e = new AdvancedCalculatorOperationsExecutor();
		assertEquals(200.099, Double.parseDouble(e.infixEvaluation("1.099 + 223 - 12 x 2 ")), 1e-9);
		assertEquals(14.0, Double.parseDouble(e.infixEvaluation("2 + 3 x 4 ")), 1e-9);
	}

	@Test
	public void equationSolvers() {
		EquationSovlerOperation e = new EquationSovlerOperation();
		// x + y = 3, x - y = 1
		assertArrayEquals(new String[] { "2.0", "1.0" }, e.AXBYC("1", "1", "3", "1", "-1", "1"));
		// x^2 - 3x + 2 = 0
		assertArrayEquals(new String[] { "2.0", "1.0" }, e.AX2BXC("1", "-3", "2"));
		// x^2 + 1 = 0: complex roots
		String[] complex = e.AX2BXC("1", "0", "1");
		assertEquals(2, complex.length);
		assertTrue(complex[0].contains("i") && complex[1].contains("i"));
		// the cubic form is ax^3 + bx^2 + cx = d: x^3 - 6x^2 + 11x = 6 has the roots 1, 2, 3
		String[] roots = e.AX3BX2CXD("1", "-6", "11", "6");
		java.util.List<Double> found = new java.util.ArrayList<Double>();
		for (String r : roots)
			found.add(Double.parseDouble(r));
		java.util.Collections.sort(found);
		assertEquals(java.util.Arrays.asList(1.0, 2.0, 3.0), found);
		// without real solutions the answer says so
		assertTrue(e.AX3BX2CXD("1", "0", "1", "-5")[0] != null);
	}

	@Test
	public void primeHunter() {
		PrimeNumberHunterOperation p = new PrimeNumberHunterOperation();
		assertEquals("2, 3, 5, 7, 11, 13, 17, 19, 23, 29, End.", p.primeNumberFinder(1, 30));
		assertEquals("the range is normalised when reversed", p.primeNumberFinder(1, 30), p.primeNumberFinder(30, 1).replace("the range is normalised when reversed", ""));
		assertEquals("503, 509, 521, 523, End.", p.primeNumberFinder(500, 540));
		assertEquals("No Number Found!", p.primeNumberFinder(0, 1));
		assertEquals(3, p.charNumber("a b c d", ' '));
	}

	@Test
	public void dateDifference() {
		DateCalculatorOperation d = new DateCalculatorOperation();
		String same = d.Date(1, 1, 2000, 1, 1, 2000);
		assertTrue(same, same.contains("0Y"));
		String r = d.Date(15, 6, 1990, 20, 7, 2020);
		assertTrue(r, r.contains("30Y") && r.contains("1M"));
	}
}
