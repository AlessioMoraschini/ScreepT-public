package generic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ReferenceTest {
	
	@Test
	public void testReference() {
		List<String> list = new ArrayList<>();
		list.add("one");
		testAssign(list);
		assertFalse(list.isEmpty());
		
		testRemove(list);
		assertTrue(list.isEmpty());
	}

	private void testAssign(List<String> list) {
		list = new ArrayList<>();
	}
	
	private void testRemove(List<String> list) {
		list.remove(0);
	}
}
