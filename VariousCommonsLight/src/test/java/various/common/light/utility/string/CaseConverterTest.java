package various.common.light.utility.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CaseConverterTest {

	@Test
	public void identifiers() {
		assertEquals("helloWorldFoo", CaseConverter.camel("Hello world_foo"));
		assertEquals("HelloWorldFoo", CaseConverter.pascal("hello-world foo"));
		assertEquals("hello_world_foo", CaseConverter.snake("helloWorldFoo"));
		assertEquals("hello-world-foo", CaseConverter.kebab("HelloWorldFoo"));
		assertEquals("HELLO_WORLD_FOO", CaseConverter.constant("hello World foo"));
	}

	@Test
	public void acronymsAndDigits() {
		assertEquals("xml_http_request", CaseConverter.snake("XMLHttpRequest"));
		assertEquals("value2_go", CaseConverter.snake("value2Go"));
	}

	@Test
	public void linesIndentationAndEolKept() {
		assertEquals("  fooBar\r\nbazQux  \r\n\r\nx", CaseConverter.camel("  foo bar\r\nbaz_qux  \r\n\r\nx"));
		assertEquals("a_b\n", CaseConverter.snake("aB\n"));
	}

	@Test
	public void noWordsLeftAsIs() {
		assertEquals("--- ", CaseConverter.snake("--- "));
	}

	@Test
	public void sentenceAndInvert() {
		assertEquals("Hello world. Next one! Yes?\nOk", CaseConverter.sentence("hELLO WORLD. nEXT ONE! yES?\nOK"));
		assertEquals("hELLO wORLD 1", CaseConverter.invert("Hello World 1"));
	}

	@Test
	public void nullAndEmpty() {
		assertNull(CaseConverter.camel(null));
		assertNull(CaseConverter.constant(null));
		assertEquals("", CaseConverter.camel(""));
		assertNull(CaseConverter.sentence(null));
	}
}
