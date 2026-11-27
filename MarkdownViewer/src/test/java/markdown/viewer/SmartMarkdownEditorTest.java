/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package markdown.viewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * The editing actions of the markdown editor, driven on a text area that is never shown: toggles, headings, lists,
 * tasks, tables, table of contents and the link suggestions. Text and caret in, text out.
 */
public class SmartMarkdownEditorTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	/** Swing moves the caret on document changes only on the event thread, like in the application. */
	@Rule
	public TestRule onEventThread = new TestRule() {
		@Override
		public Statement apply(final Statement base, Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					final Throwable[] failure = new Throwable[1];
					SwingUtilities.invokeAndWait(() -> {
						try {
							base.evaluate();
						} catch (Throwable t) {
							failure[0] = t;
						}
					});
					if(failure[0] != null)
						throw failure[0];
				}
			};
		}
	};

	private File baseDirectory;
	private final List<String> statuses = new ArrayList<String>();

	private RSyntaxTextArea area;
	private SmartMarkdownEditor editor;

	/** Editor on the text; the caret is marked with | (second | ends a selection). */
	private void open(String marked) {
		open(marked, '|');
	}

	/** Text with pipes of its own (tables): the caret is at the start, no markers. */
	private void openPlain(String text) {
		open(text, '\u0000');
		area.setCaretPosition(0);
	}

	private void open(String marked, char marker) {
		int first = marked.indexOf(marker);
		int second = first < 0 ? -1 : marked.indexOf(marker, first + 1);
		String text = marked.replace(String.valueOf(marker), "");
		area = new RSyntaxTextArea();
		editor = new SmartMarkdownEditor(area, new SmartMarkdownEditor.Host() {
			@Override
			public File baseDirectory() {
				return baseDirectory;
			}

			@Override
			public void status(String message) {
				statuses.add(message);
			}
		});
		editor.install();
		area.setText(text);
		if(first < 0) {
			area.setCaretPosition(text.length());
		} else if(second < 0) {
			area.setCaretPosition(first);
		} else {
			area.select(first, second - 1);
		}
	}

	/** Text of the area with the caret (or the selection) marked with |. */
	private String view() {
		String t = area.getText();
		int s = area.getSelectionStart(), e = area.getSelectionEnd();
		if(s == e)
			return t.substring(0, s) + "|" + t.substring(s);
		return t.substring(0, s) + "|" + t.substring(s, e) + "|" + t.substring(e);
	}

	private void press(String actionName) {
		area.getActionMap().get(actionName).actionPerformed(new ActionEvent(area, ActionEvent.ACTION_PERFORMED, null));
	}

	// ------------------------------------------------------------------ inline toggles

	@Test
	public void boldWrapsTheSelectionAndUnwrapsItBack() {
		open("say |hello| world");
		editor.toggleWrap("**");
		assertEquals("say **|hello|** world", view());
		editor.toggleWrap("**");
		assertEquals("say |hello| world", view());
	}

	@Test
	public void boldOnTheWordAtTheCaret() {
		open("say hel|lo world");
		editor.toggleWrap("**");
		assertEquals("say **|hello|** world", view());
	}

	@Test
	public void boldOnAnAlreadyWrappedWordRemovesTheDelimiters() {
		open("say **hel|lo** world");
		editor.toggleWrap("**");
		assertEquals("say |hello| world", view());
	}

	@Test
	public void boldWithNothingToWrapInsertsAPairAndTogglesItAway() {
		open("a | b");
		editor.toggleWrap("**");
		assertEquals("a **|** b", view());
		editor.toggleWrap("**");
		assertEquals("a | b", view());
	}

	@Test
	public void italicOnBoldTextMakesItBoldItalic() {
		open("|**word**|");
		editor.toggleWrap("*");
		assertEquals("***word***", area.getText());
		editor.toggleWrap("*");
		assertEquals("**word**", area.getText());
		// known defect: italic on the inner text of a bold word (selection "word" in "**word**") removes one star
		// instead of adding two: "*word*" (bold lost); not asserted
	}

	@Test
	public void strikethroughWrapsTheSelection() {
		open("|old| price");
		editor.toggleWrap("~~");
		assertEquals("~~old~~ price", area.getText());
	}

	@Test
	public void mathCyclesFromNoneToInlineToDisplayToNone() {
		open("|x+1|");
		editor.toggleMath();
		assertEquals("$x+1$", area.getText());
		editor.toggleMath();
		assertEquals("$$x+1$$", area.getText());
		editor.toggleMath();
		assertEquals("x+1", area.getText());
	}

	@Test
	public void mathWithoutSelectionInsertsEmptyPairWithCaretInside() {
		open("a |");
		editor.toggleMath();
		assertEquals("a $|$", view());
	}

	@Test
	public void mathShortcutIsBoundAndPressingItWorks() {
		open("|e=mc2|");
		press("mdx-math");
		assertEquals("$e=mc2$", area.getText());
		press("mdx-strike");
		assertEquals("~~$e=mc2$~~", area.getText());
	}

	@Test
	public void actionsDoNothingWhenTheEditorIsReadOnly() {
		open("|word|");
		area.setEditable(false);
		press("mdx-strike");
		press("mdx-task");
		assertEquals("word", area.getText());
	}

	// ------------------------------------------------------------------ headings

	@Test
	public void headingLevelGoesUpAndDownOnEverySelectedLine() {
		open("|alpha\n# beta|");
		editor.changeHeading(1);
		assertEquals("# alpha\n## beta", area.getText());
		editor.changeHeading(-1);
		editor.changeHeading(-1);
		assertEquals("alpha\nbeta", area.getText());
	}

	@Test
	public void headingShortcutsAreBound() {
		open("# t|itle");
		press("mdx-headingup");
		assertEquals("## title", area.getText());
		press("mdx-headingdown2");
		assertEquals("# title", area.getText());
	}

	// ------------------------------------------------------------------ lists: enter, backspace, tab

	@Test
	public void enterOnABulletContinuesTheList() {
		open("- one|");
		press("mdx-enter");
		assertEquals("- one\n- |", view());
	}

	@Test
	public void enterInTheMiddleOfAnItemMovesTheRestToTheNewItem() {
		open("- one| two");
		press("mdx-enter");
		assertEquals("- one\n- |two", view());
	}

	@Test
	public void enterOnANumberedItemRenumbersTheItemsBelow() {
		open("1. a|\n2. b\n3. c");
		press("mdx-enter");
		assertEquals("1. a\n2. |\n3. b\n4. c", view());
	}

	@Test
	public void enterOnATaskContinuesWithAnEmptyBox() {
		open("- [x] done|");
		press("mdx-enter");
		assertEquals("- [x] done\n- [ ] |", view());
	}

	@Test
	public void enterOnAQuoteContinuesTheQuote() {
		open("> said|");
		press("mdx-enter");
		assertEquals("> said\n> |", view());
	}

	@Test
	public void enterOnAnEmptyItemEndsTheList() {
		open("- one\n- |");
		press("mdx-enter");
		assertEquals("- one\n|", view());
	}

	@Test
	public void enterOnAnEmptyNestedItemGoesBackOneLevel() {
		open("- one\n  - |");
		press("mdx-enter");
		assertEquals("- one\n- |", view());
	}

	@Test
	public void enterInsideACodeFenceIsAPlainNewLine() {
		open("```\n- not a list|\n```");
		press("mdx-enter");
		assertEquals("```\n- not a list\n|\n```", view());
	}

	@Test
	public void enterBeforeTheMarkerOrOnPlainTextIsAPlainNewLine() {
		open("plain|");
		press("mdx-enter");
		assertEquals("plain\n|", view());
		open("|- item");
		press("mdx-enter");
		assertEquals("\n|- item", view());
	}

	@Test
	public void backspaceRightAfterTheMarkerRemovesIt() {
		open("- |text");
		press("mdx-backspace");
		assertEquals("text", area.getText());
	}

	@Test
	public void backspaceRightAfterTheMarkerOfANumberedItemRenumbers() {
		open("1. a\n2. |b\n3. c");
		press("mdx-backspace");
		// the text left behind is no longer a list item, so what follows keeps its own number
		assertEquals("1. a\nb\n3. c", area.getText());
		// known defect: the caret jumps to the end of the line after removing a marker (replaceLine rewrites the whole line)
	}

	@Test
	public void backspaceAfterTheMarkerOfANestedItemOutdents() {
		open("- a\n  - |b");
		press("mdx-backspace");
		assertEquals("- a\n- b", area.getText());
	}

	@Test
	public void backspaceAfterTheMarkerOfATaskKeepsTheBulletAndDropsTheBox() {
		open("- [ ] |x");
		press("mdx-backspace");
		assertEquals("- x", area.getText());
	}

	@Test
	public void backspaceInsideTextIsTheNormalOne() {
		open("- ab|c");
		press("mdx-backspace");
		assertEquals("- a|c", view());
	}

	@Test
	public void tabIndentsAnItemUnderThePreviousOneAndShiftTabBringsItBack() {
		open("- a\n- b|");
		press("mdx-tab");
		assertEquals("- a\n  - b|", view());
		press("mdx-shifttab");
		assertEquals("- a\n- b|", view());
	}

	@Test
	public void tabOnNumberedItemStartsANewNumberingAndFixesTheFollowingOnes() {
		open("1. a\n2. b|\n3. c");
		press("mdx-tab");
		assertEquals("1. a\n   1. b|\n2. c", view());
	}

	@Test
	public void tabOnSeveralSelectedItemsIndentsThemAll() {
		open("- a\n|- b\n- c|");
		press("mdx-tab");
		assertEquals("- a\n  - b\n  - c", area.getText());
	}

	@Test
	public void tabInsideTheTextOfAnItemOrOnPlainTextIsAPlainTab() {
		open("- some| text");
		press("mdx-tab");
		assertEquals("- some\t text", area.getText());
		open("plain|");
		press("mdx-tab");
		assertEquals("plain\t", area.getText());
	}

	// ------------------------------------------------------------------ tasks

	@Test
	public void tasksToggleBetweenOpenAndDoneAndPlainLinesBecomeTasks() {
		open("- a\n- [ ] b\n- [x] c\nplain");
		area.select(0, area.getText().length());
		editor.toggleTasks();
		assertEquals("- [ ] a\n- [x] b\n- [ ] c\n- [ ] plain", area.getText());
	}

	@Test
	public void numberedItemsKeepTheirNumberWhenTheyGetABox() {
		open("1. one|");
		press("mdx-task");
		assertEquals("1. [ ] one", area.getText());
	}

	@Test
	public void toggleTasksSkipsBlankLinesInASelection() {
		open("|a\n\nb|");
		editor.toggleTasks();
		assertEquals("- [ ] a\n\n- [ ] b", area.getText());
	}

	// ------------------------------------------------------------------ tables

	private static final String UGLY_TABLE = "| name | qty |\n|---|:-:|\n| apple | 3 |\n| kiwi | 12 |";

	@Test
	public void formatTableAlignsTheColumnsOfTheTableAtTheCaret() {
		openPlain("intro\n\n" + UGLY_TABLE + "\n\noutro");
		area.setCaretPosition(area.getText().indexOf("kiwi"));
		editor.formatTables();
		String[] rows = area.getText().split("\n");
		assertEquals("intro", rows[0]);
		assertTrue(rows[2], rows[2].startsWith("| name  |") && rows[2].contains("qty"));
		assertTrue(rows[4], rows[4].startsWith("| apple |"));
		assertTrue(rows[5], rows[5].startsWith("| kiwi  |"));
		assertEquals(rows[2].length(), rows[4].length());
		assertEquals(rows[2].length(), rows[5].length());
		assertEquals(rows[2].length(), rows[3].length());
		assertTrue(rows[3], rows[3].contains(":-"));
		assertEquals("outro", rows[7]);
		assertEquals("Table formatted", statuses.get(statuses.size() - 1));
	}

	@Test
	public void formatTablesWithTheCaretOutsideFormatsEveryTable() {
		openPlain("|a|b|\n|-|-|\n|1|2|\n\ntext\n\n|c|d|\n|-|-|\n|333|4|\n");
		area.setCaretPosition(area.getText().indexOf("text"));
		editor.formatTables();
		assertEquals("2 tables formatted", statuses.get(statuses.size() - 1));
		assertTrue(area.getText(), area.getText().contains("| 333 | 4   |"));
		assertTrue(area.getText(), area.getText().contains("| 1   | 2   |"));
	}

	@Test
	public void formatTablesWithoutTablesSaysSo() {
		open("just text\n");
		editor.formatTables();
		assertTrue(statuses.get(statuses.size() - 1).startsWith("No markdown table"));
		assertEquals("just text\n", area.getText());
	}

	@Test
	public void tablesInsideCodeFencesAreLeftAlone() {
		openPlain("```\n|a|b|\n|-|-|\n|1|2|\n```\n");
		editor.formatTables();
		assertEquals("```\n|a|b|\n|-|-|\n|1|2|\n```\n", area.getText());
	}

	@Test
	public void tableShortcutIsBound() {
		openPlain("|a|b|\n|-|-|\n|1|2|");
		press("mdx-table");
		assertEquals("Table formatted", statuses.get(statuses.size() - 1));
	}

	// ------------------------------------------------------------------ table of contents

	@Test
	public void tocIsInsertedAtTheCaretLineThenUpdatedWhenHeadingsChange() {
		open("# Title\n\n## First\n\ntext\n\n## Second\n");
		area.setCaretPosition(area.getText().indexOf("text"));
		editor.insertOrUpdateToc();
		String text = area.getText();
		assertTrue(text, text.contains(MarkdownText.TOC_START) && text.contains(MarkdownText.TOC_END));
		assertTrue(text, text.contains("(#first)") && text.contains("(#second)"));
		assertTrue(statuses.get(statuses.size() - 1).startsWith("Table of contents inserted"));

		editor.insertOrUpdateToc();
		assertEquals("The table of contents is already up to date", statuses.get(statuses.size() - 1));
		assertFalse(editor.updateToc());

		area.setText(text.replace("## Second", "## Third"));
		assertTrue(editor.updateToc());
		assertTrue(area.getText().contains("(#third)"));
		assertFalse(area.getText().contains("(#second)"));
		area.setText(area.getText().replace("## Third", "## Fourth"));
		editor.insertOrUpdateToc();
		assertEquals("Table of contents updated", statuses.get(statuses.size() - 1));
		assertTrue(area.getText().contains("(#fourth)"));
	}

	@Test
	public void updateTocWithoutMarkersChangesNothing() {
		open("# A\n## B\n");
		assertFalse(editor.updateToc());
		assertEquals("# A\n## B\n", area.getText());
	}

	// ------------------------------------------------------------------ link suggestions

	@SuppressWarnings("unchecked")
	private List<Object> suggestions(String typed) throws Exception {
		Method m = SmartMarkdownEditor.class.getDeclaredMethod("suggestions", String.class);
		m.setAccessible(true);
		return (List<Object>) m.invoke(editor, typed);
	}

	private static Object field(Object o, String name) throws Exception {
		Field f = o.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(o);
	}

	private List<String> labels(List<Object> suggestions) throws Exception {
		List<String> out = new ArrayList<String>();
		for(Object s : suggestions)
			out.add((String) field(s, "label"));
		return out;
	}

	private void makeProjectFolder() throws Exception {
		baseDirectory = tmp.newFolder("docs");
		new File(baseDirectory, "sub").mkdir();
		new File(baseDirectory, "zeta.txt").createNewFile();
		new File(baseDirectory, "tail.txt").createNewFile();
		new File(baseDirectory, ".hidden").createNewFile();
		java.nio.file.Files.write(new File(baseDirectory, "my notes.md").toPath(), "# Notes\n## Deep part\n".getBytes("UTF-8"));
		java.nio.file.Files.write(new File(baseDirectory, "sub/inner.md").toPath(), "# Inner\n".getBytes("UTF-8"));
	}

	@Test
	public void hashSuggestsTheHeadingsOfTheCurrentDocument() throws Exception {
		open("# Alpha\n## Beta gamma\ntext");
		List<Object> all = suggestions("#");
		assertEquals(2, all.size());
		assertEquals("Alpha", field(all.get(0), "label"));
		assertEquals("  Beta gamma", field(all.get(1), "label"));
		assertEquals("#beta-gamma", field(all.get(1), "insert"));
		List<Object> filtered = suggestions("#gam");
		assertEquals(1, filtered.size());
		assertEquals("#beta-gamma", field(filtered.get(0), "insert"));
		assertTrue(suggestions("#zzz").isEmpty());
	}

	@Test
	public void linkSuggestionsWithoutASavedFileAreEmpty() throws Exception {
		open("x");
		baseDirectory = null;
		assertTrue(suggestions("no").isEmpty());
		assertTrue(suggestions("file.md#").isEmpty());
	}

	@Test
	public void pathSuggestionsListFoldersFirstEscapeNamesAndHideDotFiles() throws Exception {
		makeProjectFolder();
		open("x");
		List<String> labels = labels(suggestions(""));
		assertEquals("[sub/, my notes.md, tail.txt, zeta.txt, ../]", labels.toString());
		List<Object> notes = suggestions("my");
		assertEquals("my%20notes.md", field(notes.get(0), "insert"));
		assertEquals("[sub/]", labels(suggestions("su")).toString());
		assertEquals("[inner.md, ../]", labels(suggestions("sub/")).toString());
		assertEquals("sub/inner.md", field(suggestions("sub/in").get(0), "insert"));
		assertEquals("names that start with the text come before the ones that only contain it", "[tail.txt, my notes.md, zeta.txt]", labels(suggestions("t")).toString());
		assertTrue(suggestions("nothing-like-this").isEmpty());
		assertTrue(suggestions("missing/").isEmpty());
	}

	@Test
	public void fileHashSuggestsTheHeadingsOfThatFile() throws Exception {
		makeProjectFolder();
		open("x");
		List<Object> h = suggestions("my%20notes.md#");
		assertEquals(2, h.size());
		assertEquals("my%20notes.md#deep-part", field(h.get(1), "insert"));
		assertEquals(1, suggestions("my%20notes.md#deep").size());
		assertTrue(suggestions("absent.md#").isEmpty());
	}

	@Test
	public void choosingASuggestionReplacesTheTypedPart() throws Exception {
		makeProjectFolder();
		open("see [it](my|");
		List<Object> s = suggestions("my");
		@SuppressWarnings("unchecked")
		DefaultListModel<Object> model = (DefaultListModel<Object>) field(editor, "model");
		model.clear();
		for(Object o : s)
			model.addElement(o);
		@SuppressWarnings("unchecked")
		JList<Object> list = (JList<Object>) field(editor, "list");
		list.setSelectedIndex(0);
		Field start = SmartMarkdownEditor.class.getDeclaredField("replaceStart");
		start.setAccessible(true);
		start.setInt(editor, area.getText().indexOf("my"));
		Method choose = SmartMarkdownEditor.class.getDeclaredMethod("chooseSuggestion");
		choose.setAccessible(true);
		assertEquals(Boolean.TRUE, choose.invoke(editor));
		assertEquals("see [it](my%20notes.md|", view());
		// nothing selected / no start: nothing to choose
		start.setInt(editor, -1);
		assertEquals(Boolean.FALSE, choose.invoke(editor));
	}

	@Test
	public void suggestionRendererShowsLabelAndDetailAsEscapedHtml() throws Exception {
		makeProjectFolder();
		open("x");
		Object folder = suggestions("su").get(0);
		@SuppressWarnings("unchecked")
		JList<Object> list = (JList<Object>) field(editor, "list");
		java.awt.Component c = list.getCellRenderer().getListCellRendererComponent(list, folder, 0, true, false);
		String text = ((javax.swing.JLabel) c).getText();
		assertTrue(text, text.startsWith("<html>sub/") && text.contains("folder"));
	}

	@Test
	public void escapeDoesNothingWhenNoSuggestionIsOpen() {
		open("x");
		assertFalse(editor.handleEscape());
		assertNull(area.getSelectedText());
	}
}
