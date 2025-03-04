package utility;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import org.junit.Test;

import various.common.light.utility.reflection.ReflectionUtils;

public class ReflectionUtilsTest {

	@Test
	public void testGetFieldsAndFilterTest() throws IllegalArgumentException, IllegalAccessException {
		
		ArrayList<Component> headerList = new ArrayList<Component>();
		PojoReflectionTest objectSrc = new PojoReflectionTest();
						
		// JSEPARATORS
		List<JButton> jbuttons = ReflectionUtils.retrieveFieldsByTypeContaininingFilter(objectSrc , JButton.class, "ALESSIO", false, true);
		headerList.addAll(jbuttons);
			
	}
}
