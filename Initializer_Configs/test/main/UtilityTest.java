package main;

import static org.junit.Assert.assertEquals;

import java.util.Vector;

import org.junit.Test;

import arch.INItializerParent;

public class UtilityTest {

	@Test
	public void testIniReadMatrix() {
		String fSearchLastFileFilters = "*.java:;:;*.txt::*.xml:;:;*.hd:;:;*.ls";
		Vector<String[]> list = INItializerParent.getMatrixOfStringsFromString(fSearchLastFileFilters, INItializerParent.SEPARATOR_INNER_DEFAULT, INItializerParent.SEPARATOR_INNER_DEFAULT_LONG, new Vector<String[]>());
		
		assertEquals("*.java", list.get(0)[0]);
		assertEquals("*.txt", list.get(0)[1]);
		assertEquals("*.xml", list.get(1)[0]);
		assertEquals("*.hd", list.get(1)[1]);
		assertEquals("*.ls", list.get(1)[2]);
	}
}
