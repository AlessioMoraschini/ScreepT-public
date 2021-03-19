package utility;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import utility.manipulation.ArrayHelper;

public class ArrayHelperTest {

	@Test
	public void testRemoveEmpty() {
		String[] testEmpty = new String[]{null, "", "This is a text"};
		
		System.out.println(Arrays.toString(ArrayHelper.removeEmpties(testEmpty)));
		assertTrue(ArrayHelper.removeEmpties(testEmpty).length == 1);
		assertTrue(ArrayHelper.removeNulls(testEmpty).size() == 2);
	}
}
