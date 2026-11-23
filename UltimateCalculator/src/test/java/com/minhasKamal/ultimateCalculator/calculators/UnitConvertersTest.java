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
package com.minhasKamal.ultimateCalculator.calculators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverterOperationArea;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverterOperationEnergy;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverterOperationLength;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverterOperationPower;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverterOperationTemperature;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverterOperationTime;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverterOperationVolume;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverterOperationWeight;

/** Every unit pair of every converter: identity, a -> b -> a round trip, transitivity and a few known values. */
public class UnitConvertersTest {

	interface Conv {
		double convert(int from, int to, double v);
	}

	private static void checkAllPairs(String name, int units, Conv c, double tolerance, String... knownDefects) {
		java.util.Set<String> skip = new java.util.HashSet<String>(java.util.Arrays.asList(knownDefects));
		StringBuilder bad = new StringBuilder();
		for (int a = 1; a <= units; a++) {
			assertEquals(name + " identity " + a, 123.5, c.convert(a, a, 123.5), 1e-9);
			for (int b = 1; b <= units; b++) {
				if (skip.contains(Math.min(a, b) + "-" + Math.max(a, b)))
					continue;
				double there = c.convert(a, b, 1000.0);
				double back = c.convert(b, a, there);
				if (Math.abs(back - 1000.0) > 1000.0 * tolerance)
					bad.append(a + "->" + b + "->" + a + "=" + back + " ");
				// linear: twice the input, twice the output
				if (Math.abs(c.convert(a, b, 2000.0) - 2 * there) > Math.abs(there) * tolerance)
					bad.append(a + "->" + b + " not linear ");
			}
		}
		assertEquals(name + " inconsistent pairs", "", bad.toString());
	}

	private static void checkTransitive(String name, int units, Conv c, double tolerance, String... knownDefects) {
		java.util.Set<String> skip = new java.util.HashSet<String>(java.util.Arrays.asList(knownDefects));
		for (int a = 1; a <= units; a++)
			for (int b = 1; b <= units; b++)
				for (int m = 1; m <= units; m++) {
					if (skip.contains(Math.min(a, m) + "-" + Math.max(a, m)) || skip.contains(Math.min(m, b) + "-" + Math.max(m, b)) || skip.contains(Math.min(a, b) + "-" + Math.max(a, b)))
						continue;
					double direct = c.convert(a, b, 10.0);
					double via = c.convert(m, b, c.convert(a, m, 10.0));
					assertEquals(name + " " + a + "->" + m + "->" + b, direct, via, Math.abs(direct) * tolerance + 1e-12);
				}
	}

	@Test
	public void length() {
		UnitConverterOperationLength l = new UnitConverterOperationLength();
		checkAllPairs("length", 13, l::Length, 1e-6);
		checkTransitive("length", 13, l::Length, 1e-6);
		assertEquals(1000.0, l.Length(3, 2, 1), 1e-9);
		assertEquals(0.001, l.Length(2, 3, 1), 1e-12);
		assertEquals(1000.0, l.Length(7, 6, 1), 1e-9);
		assertEquals(1.609344, l.Length(12, 7, 1), 1e-3);
		assertEquals(2.54, l.Length(8, 5, 1), 1e-3);
		assertEquals(1852.0, l.Length(11, 6, 1), 1.0);
	}

	@Test
	public void area() {
		UnitConverterOperationArea a = new UnitConverterOperationArea();
		// Katha = 1.65 decimals, Bigha = 33 decimals (decimal = 40.468564224 m2)
		checkAllPairs("area", 12, a::Area, 1e-6);
		checkTransitive("area", 12, a::Area, 1e-6);
		assertEquals(10000.0, a.Area(3, 2, 1), 1e-6);
		assertEquals(1000000.0, a.Area(4, 2, 1), 1e-3);
		assertEquals(40.468564224 * 1.65, a.Area(9, 2, 1), 1e-9);
		assertEquals(40.468564224 * 33, a.Area(10, 2, 1), 1e-9);
	}

	@Test
	public void volume() {
		UnitConverterOperationVolume v = new UnitConverterOperationVolume();
		checkAllPairs("volume", 8, v::Volume, 1e-6);
		checkTransitive("volume", 8, v::Volume, 1e-6);
		assertEquals(1000.0, v.Volume(2, 6, 1), 1e-6);
		assertEquals(1000.0, v.Volume(6, 1, 1), 1e-6);
	}

	@Test
	public void time() {
		UnitConverterOperationTime t = new UnitConverterOperationTime();
		checkAllPairs("time", 7, t::Time, 1e-9);
		checkTransitive("time", 7, t::Time, 1e-9);
		assertEquals(3600.0, t.Time(5, 3, 1), 1e-6);
		assertEquals(60.0, t.Time(4, 3, 1), 1e-9);
		assertEquals(1000.0, t.Time(3, 2, 1), 1e-9);
		assertEquals(7.0, t.Time(7, 6, 1), 1e-9);
	}

	@Test
	public void weight() {
		UnitConverterOperationWeight w = new UnitConverterOperationWeight();
		checkAllPairs("weight", 7, w::Weight, 1e-6);
		checkTransitive("weight", 7, w::Weight, 1e-6);
		assertEquals(1e9, w.Weight(4, 1, 1), 1);
		assertEquals(1e-9, w.Weight(1, 4, 1), 1e-15);
		assertEquals(1000.0, w.Weight(3, 2, 1), 1e-9);
		assertEquals(1000.0, w.Weight(4, 3, 1), 1e-6);
		assertEquals(0.4536, w.Weight(6, 3, 1), 1e-3);
	}

	@Test
	public void energy() {
		UnitConverterOperationEnergy e = new UnitConverterOperationEnergy();
		checkAllPairs("energy", 6, e::Energy, 1e-6);
		checkTransitive("energy", 6, e::Energy, 1e-5);
		assertEquals(1000.0, e.Energy(2, 1, 1), 1e-9);
		assertEquals(4.184, e.Energy(3, 1, 1), 1e-2);
	}

	@Test
	public void power() {
		UnitConverterOperationPower p = new UnitConverterOperationPower();
		checkAllPairs("power", 3, p::Power, 1e-6);
		assertEquals(1000.0, p.Power(2, 1, 1), 1e-9);
		assertEquals(745.7, p.Power(3, 1, 1), 1.0);
	}

	@Test
	public void temperature() {
		UnitConverterOperationTemperature t = new UnitConverterOperationTemperature();
		assertEquals(32.0, t.Temperature(1, 2, 0), 1e-9);
		assertEquals(212.0, t.Temperature(1, 2, 100), 1e-9);
		assertEquals(0.0, t.Temperature(2, 1, 32), 1e-9);
		assertEquals(273.15, t.Temperature(1, 3, 0), 1e-9);
		assertEquals(0.0, t.Temperature(3, 1, 273.15), 1e-9);
		assertEquals(32.0, t.Temperature(3, 2, 273.15), 1e-9);
		assertEquals(273.15, t.Temperature(2, 3, 32), 1e-9);
		assertEquals(-273.15, t.Temperature(3, 1, 0), 1e-9);
		assertEquals(-459.67, t.Temperature(3, 2, 0), 1e-9);
		for (int a = 1; a <= 3; a++)
			assertEquals(42.0, t.Temperature(a, a, 42), 1e-9);
	}
}
