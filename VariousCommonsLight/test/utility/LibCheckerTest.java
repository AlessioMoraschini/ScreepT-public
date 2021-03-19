package utility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import utility.reflection.ReflectionLibChecker;

public class LibCheckerTest {
	
	boolean loggerEnabled = true;
	
	@Test
	@Ignore
	public void TestClass() {
		ReflectionLibChecker.setLoggerEnabled(loggerEnabled);
		
		assertFalse(ReflectionLibChecker.checkClassInstantiation("fakeClass", null));
		assertTrue(ReflectionLibChecker.checkClassExisting("org.junit.Test"));
	}
	
	@Test
	public void TestClassInstantioation() {
		ReflectionLibChecker.setLoggerEnabled(loggerEnabled);
		String[] args = {""};
		assertTrue(ReflectionLibChecker.checkClassInstantiation("java.lang.String", args));
//		assertTrue(LibChecker.checkClassInstantiation("org.junit.Assert", new Boolean(true)));
	}
	
	@Test
	@Ignore
	public void TestMethod() {
		ReflectionLibChecker.setLoggerEnabled(loggerEnabled);
		assertTrue(ReflectionLibChecker.checkMethod("java.lang.String", "equals", Object.class));
	}
}
