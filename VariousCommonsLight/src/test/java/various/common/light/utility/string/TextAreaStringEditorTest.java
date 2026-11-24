package various.common.light.utility.string;

import static org.junit.Assert.assertEquals;

import javax.swing.JTextArea;

import org.junit.Test;

/** Upper/lower case after the dots on a selection: the text around the selection must stay as it is. */
public class TextAreaStringEditorTest {

	private static JTextArea area(String text, int from, int to) {
		JTextArea a = new JTextArea(text);
		a.setSelectionStart(from);
		a.setSelectionEnd(to);
		return a;
	}

	@Test
	public void uppercaseAfterDotsOnASelectionKeepsTheRest() throws Exception {
		JTextArea a = area("first. second. third", 7, 20);
		TextAreaStringEditor.makeUppercasePostDotsNSeparators(a);
		assertEquals("first. Second. Third", a.getText());
	}

	@Test
	public void lowercaseAfterDotsOnASelectionKeepsTheRest() throws Exception {
		JTextArea a = area("First. Second. Third", 7, 20);
		TextAreaStringEditor.makeLowercasePostDotsNSeparators(a);
		assertEquals("First. second. third", a.getText());
	}

	@Test
	public void withoutSelectionTheWholeTextIsConverted() throws Exception {
		JTextArea a = area("one. two. three", 0, 0);
		TextAreaStringEditor.makeUppercasePostDotsNSeparators(a);
		assertEquals("One. Two. Three", a.getText());
	}
}
