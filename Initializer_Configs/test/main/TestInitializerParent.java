package main;

import org.junit.Test;

import initializer.configs.arch.INItializerParent;
import various.common.light.utility.string.StringWorker;

import static initializer.configs.arch.INItializerParent.SEPARATOR_DEFAULT;
import static initializer.configs.arch.INItializerParent.SEPARATOR_INNER_DEFAULT;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;;

public class TestInitializerParent {

	private static final String MAP_STR = 
			"a"+SEPARATOR_INNER_DEFAULT+"b"+SEPARATOR_DEFAULT+
			"c"+SEPARATOR_INNER_DEFAULT+SEPARATOR_DEFAULT+
			"d"+SEPARATOR_INNER_DEFAULT+"e";
	
	@Test
	public void testStringMap() {
		Map<String, String> map = INItializerParent.getStringMapFromString(MAP_STR, SEPARATOR_DEFAULT, SEPARATOR_INNER_DEFAULT, new HashMap<String, String>());
		assertEquals("b", map.get("a"));
		assertEquals("", map.get("c"));
		assertEquals("e", map.get("d"));
		
		String stringMap = StringWorker.getMapToString(map);
		System.out.println(stringMap);
		
		String stringMap2 = INItializerParent.mapToString(map);
		System.out.println(stringMap2);
		assertEquals(MAP_STR, stringMap2);
	}
}
