/*
 * Copyright (c) AM-Design-Development. All rights reserved.
 */
package markdown.viewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import various.common.light.files.FileWorker;
import various.common.light.utility.log.SafeLogger;

/**
 * "Markdown All in One" style editing on the markdown editor of the viewer:
 * <ul>
 * <li>Ctrl+B / Ctrl+I / Alt+S / Ctrl+M: toggle bold, italic, strikethrough, math on the selection or the word at the caret</li>
 * <li>Ctrl+Shift+] / Ctrl+Shift+[ (or Ctrl+Shift+Up / Down): heading level up / down</li>
 * <li>Enter continues lists, task lists and quotes (an empty item ends the list), Tab / Shift+Tab indent list items,
 * Backspace right after a marker removes it; numbered lists are numbered again after every change</li>
 * <li>Alt+C: toggle the task checkboxes of the selected lines</li>
 * <li>Shift+Alt+F: format the table at the caret (all the tables when the caret is not in one)</li>
 * <li>Ctrl+Shift+T: insert / update the table of contents between {@code <!-- TOC -->} markers (also updated on save)</li>
 * <li>"](#" and "](" suggest the headings of the document and the files next to it; paste of a url on a selection makes a link</li>
 * </ul>
 */
public class SmartMarkdownEditor {

	private static final SafeLogger logger = new SafeLogger(SmartMarkdownEditor.class);

	private static final int MENU = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	private static final Pattern LINK_CONTEXT = Pattern.compile("\\]\\(([^)\\s]*)$");
	private static final Pattern URL = Pattern.compile("^(https?|ftp|file|mailto):\\S+$|^www\\.\\S+\\.\\S+$");
	private static final int MAX_SUGGESTIONS = 300;

	/** What the editor needs from its window. */
	public interface Host {
		/** Folder of the markdown file (for relative links), null when it has not been saved yet. */
		File baseDirectory();

		void status(String text);
	}

	private final RSyntaxTextArea area;
	private final Host host;

	// link completion
	private final JPopupMenu popup = new JPopupMenu();
	private final DefaultListModel<Suggestion> model = new DefaultListModel<Suggestion>();
	private final JList<Suggestion> list = new JList<Suggestion>(model);
	private int replaceStart = -1;

	public SmartMarkdownEditor(RSyntaxTextArea area, Host host) {
		this.area = area;
		this.host = host;
	}

	// ------------------------------------------------------------------ installation

	public void install() {
		final Action enter = originalAction(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		final Action tab = originalAction(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		final Action shiftTab = originalAction(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
		final Action backspace = originalAction(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));

		bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "mdx-enter", () -> {
			if(!smartEnter())
				fallback(enter);
		});
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "mdx-tab", () -> {
			if(!indentListItems(true))
				fallback(tab);
		});
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK), "mdx-shifttab", () -> {
			if(!indentListItems(false))
				fallback(shiftTab);
		});
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "mdx-backspace", () -> {
			if(!smartBackspace())
				fallback(backspace);
		});

		bind(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK), "mdx-strike", () -> toggleWrap("~~"));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_M, MENU), "mdx-math", () -> toggleMath());
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK), "mdx-task", () -> toggleTasks());
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, MENU | InputEvent.SHIFT_MASK), "mdx-headingup", () -> changeHeading(1));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, MENU | InputEvent.SHIFT_MASK), "mdx-headingdown", () -> changeHeading(-1));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_UP, MENU | InputEvent.SHIFT_MASK), "mdx-headingup2", () -> changeHeading(1));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, MENU | InputEvent.SHIFT_MASK), "mdx-headingdown2", () -> changeHeading(-1));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_MASK | InputEvent.ALT_MASK), "mdx-table", () -> formatTables());
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_T, MENU | InputEvent.SHIFT_MASK), "mdx-toc", () -> insertOrUpdateToc());
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_V, MENU), "mdx-paste", () -> smartPaste());
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK | InputEvent.ALT_MASK), "mdx-sortasc", () -> sortTable(true));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK | InputEvent.ALT_MASK), "mdx-sortdesc", () -> sortTable(false));

		installCompletion();
	}

	private Action originalAction(KeyStroke stroke) {
		Object name = area.getInputMap(JComponent.WHEN_FOCUSED).get(stroke);
		return name == null ? null : area.getActionMap().get(name);
	}

	private void fallback(Action action) {
		if(action != null)
			action.actionPerformed(new ActionEvent(area, ActionEvent.ACTION_PERFORMED, null));
	}

	/** An editing action (the text methods throw BadLocationException). */
	private interface EditAction {
		void run() throws Exception;
	}

	private void bind(KeyStroke stroke, String name, final EditAction action) {
		area.getInputMap(JComponent.WHEN_FOCUSED).put(stroke, name);
		area.getActionMap().put(name, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if(!area.isEditable())
					return;
				try {
					action.run();
				} catch (Exception ex) {
					logger.warn("Markdown editing action failed", ex);
				}
			}
		});
	}

	// ------------------------------------------------------------------ helpers on lines

	private String line(int index) throws Exception {
		int start = area.getLineStartOffset(index);
		int end = area.getLineEndOffset(index);
		String text = area.getText(start, end - start);
		return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
	}

	private void replaceLine(int index, String text) throws Exception {
		int start = area.getLineStartOffset(index);
		String old = line(index);
		if(!old.equals(text))
			area.replaceRange(text, start, start + old.length());
	}

	private int caretLine() throws Exception {
		return area.getLineOfOffset(area.getCaretPosition());
	}

	private String[] allLines() {
		return MarkdownText.lines(area.getText());
	}

	private int[] selectedLines() throws Exception {
		int first = area.getLineOfOffset(area.getSelectionStart());
		int end = area.getSelectionEnd();
		int last = area.getLineOfOffset(end > area.getSelectionStart() && end > 0 ? end - 1 : end);
		return new int[] {first, last};
	}

	// ------------------------------------------------------------------ inline toggles

	/** Wraps / unwraps the selection (or the word at the caret) with the delimiter; with nothing to wrap, caret between two delimiters. */
	public void toggleWrap(String d) {
		try {
			String text = area.getText();
			int s = area.getSelectionStart(), e = area.getSelectionEnd();
			if(s == e) {
				int[] word = wordAt(text, s);
				if(word == null) {
					// "**|**" -> remove, else insert the pair
					if(s >= d.length() && s + d.length() <= text.length() && text.startsWith(d, s - d.length()) && text.startsWith(d, s)) {
						area.replaceRange("", s - d.length(), s + d.length());
					} else {
						area.insert(d + d, s);
						area.setCaretPosition(s + d.length());
					}
					return;
				}
				s = word[0];
				e = word[1];
			}
			String selected = text.substring(s, e);
			if(isWrapped(selected, d)) {
				String inner = selected.substring(d.length(), selected.length() - d.length());
				area.replaceRange(inner, s, e);
				area.select(s, s + inner.length());
			} else if(s >= d.length() && e + d.length() <= text.length() && text.startsWith(d, s - d.length()) && text.startsWith(d, e)
					&& isWrapped(text.substring(s - d.length(), e + d.length()), d)) {
				area.beginAtomicEdit();
				try {
					area.replaceRange("", e, e + d.length());
					area.replaceRange("", s - d.length(), s);
				} finally {
					area.endAtomicEdit();
				}
				area.select(s - d.length(), e - d.length());
			} else {
				area.replaceRange(d + selected + d, s, e);
				area.select(s + d.length(), e + d.length());
			}
		} finally {
			area.requestFocusInWindow();
		}
	}

	/** "*x*" is italic, "**x**" is not (for the "*" delimiter); "***x***" is both. */
	private boolean isWrapped(String s, String d) {
		if(s.length() < d.length() * 2 + 1 || !s.startsWith(d) || !s.endsWith(d))
			return false;
		if(d.equals("*"))
			return !(s.startsWith("**") && !s.startsWith("***"));
		return true;
	}

	/** [start, end) of the word around the offset, null when the caret is not on a word. */
	private int[] wordAt(String text, int offset) {
		int s = offset, e = offset;
		while(s > 0 && isWordChar(text.charAt(s - 1)))
			s--;
		while(e < text.length() && isWordChar(text.charAt(e)))
			e++;
		return s == e ? null : new int[] {s, e};
	}

	private boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '\'';
	}

	/** none -> $x$ -> $$x$$ -> none. */
	public void toggleMath() {
		String sel = area.getSelectedText();
		int s = area.getSelectionStart();
		if(sel == null || sel.isEmpty()) {
			area.insert("$$", s);
			area.setCaretPosition(s + 1);
		} else if(sel.startsWith("$$") && sel.endsWith("$$") && sel.length() >= 4) {
			String inner = sel.substring(2, sel.length() - 2);
			area.replaceSelection(inner);
			area.select(s, s + inner.length());
		} else if(sel.startsWith("$") && sel.endsWith("$") && sel.length() >= 2) {
			String next = "$" + sel + "$";
			area.replaceSelection(next);
			area.select(s, s + next.length());
		} else {
			String next = "$" + sel + "$";
			area.replaceSelection(next);
			area.select(s, s + next.length());
		}
		area.requestFocusInWindow();
	}

	// ------------------------------------------------------------------ headings

	public void changeHeading(int delta) {
		try {
			int[] lines = selectedLines();
			area.beginAtomicEdit();
			try {
				for(int i = lines[0]; i <= lines[1]; i++)
					replaceLine(i, MarkdownText.changeHeadingLevel(line(i), delta));
			} finally {
				area.endAtomicEdit();
			}
		} catch (Exception e) {
			logger.warn("Cannot change the heading", e);
		}
		area.requestFocusInWindow();
	}

	/** Sets the heading level (0 = plain paragraph) of the selected lines, or of the line of the caret. */
	public void setHeading(int level) {
		try {
			int[] lines = selectedLines();
			String[] all = allLines();
			area.beginAtomicEdit();
			try {
				for(int i = lines[0]; i <= lines[1]; i++) {
					if(MarkdownText.isInsideFence(all, i))
						continue;
					String text = line(i);
					if(lines[0] != lines[1] && text.trim().isEmpty())
						continue; // blank lines between selected paragraphs stay blank
					replaceLine(i, MarkdownText.changeHeadingLevel(text, level - MarkdownText.headingLevel(text)));
				}
			} finally {
				area.endAtomicEdit();
			}
		} catch (Exception e) {
			logger.warn("Cannot set the heading level", e);
		}
		area.requestFocusInWindow();
	}

	/** Heading level of the line of the caret (0: not a heading, or inside a code fence). */
	public int headingLevelAtCaret() {
		try {
			int index = caretLine();
			return MarkdownText.isInsideFence(allLines(), index) ? 0 : MarkdownText.headingLevel(line(index));
		} catch (Exception e) {
			return 0;
		}
	}

	// ------------------------------------------------------------------ lists

	/** Enter on a list item / quote: next item with the same prefix; an empty item ends the list (or goes up one level). */
	boolean smartEnter() throws Exception {
		if(area.getSelectionStart() != area.getSelectionEnd() || popup.isVisible())
			return popup.isVisible() && chooseSuggestion();
		int caret = area.getCaretPosition();
		int index = area.getLineOfOffset(caret);
		String text = line(index);
		int column = caret - area.getLineStartOffset(index);
		MarkdownText.ListPrefix p = MarkdownText.listPrefix(text);
		if(p == null || column < p.length || MarkdownText.isInsideFence(allLines(), index))
			return false;

		if(text.substring(p.length).trim().isEmpty()) {
			if(!p.isQuote() && p.indentWidth() > 0) {
				outdent(index, p);
			} else {
				replaceLine(index, "");
			}
			return true;
		}

		String next = p.next();
		area.beginAtomicEdit();
		try {
			// the text after the caret goes to the new item, without its leading spaces
			int lineEnd = area.getLineStartOffset(index) + text.length();
			String rest = area.getText(caret, lineEnd - caret);
			String trimmedRest = rest.replaceAll("^[ \\t]+", "");
			area.replaceRange("\n" + next + trimmedRest, caret, lineEnd);
			area.setCaretPosition(caret + 1 + next.length());
			if(p.isOrdered())
				renumberAround(index + 1);
		} finally {
			area.endAtomicEdit();
		}
		return true;
	}

	/** Backspace right after the marker of an item: one level up, or the marker goes away. */
	boolean smartBackspace() throws Exception {
		if(area.getSelectionStart() != area.getSelectionEnd())
			return false;
		int caret = area.getCaretPosition();
		int index = area.getLineOfOffset(caret);
		String text = line(index);
		MarkdownText.ListPrefix p = MarkdownText.listPrefix(text);
		if(p == null || p.isQuote() || caret - area.getLineStartOffset(index) != p.length || p.length > text.length())
			return false;
		if(MarkdownText.isInsideFence(allLines(), index))
			return false;
		area.beginAtomicEdit();
		try {
			if(p.indentWidth() > 0) {
				outdent(index, p);
			} else if(!p.task.isEmpty()) {
				replaceLine(index, text.substring(0, p.length - p.task.length()) + text.substring(p.length));
			} else {
				replaceLine(index, text.substring(p.length));
				renumberAround(index);
			}
		} finally {
			area.endAtomicEdit();
		}
		return true;
	}

	/** Tab / Shift+Tab on list items (all the selected lines must be items), numbers fixed afterwards. */
	boolean indentListItems(boolean indent) throws Exception {
		if(popup.isVisible())
			return chooseSuggestion();
		int[] lines = selectedLines();
		String[] all = allLines();
		List<MarkdownText.ListPrefix> prefixes = new ArrayList<MarkdownText.ListPrefix>();
		for(int i = lines[0]; i <= lines[1]; i++) {
			MarkdownText.ListPrefix p = MarkdownText.listPrefix(MarkdownText.stripCr(all[i]));
			if(p == null || p.isQuote() || MarkdownText.isInsideFence(all, i))
				return false;
			prefixes.add(p);
		}
		// single caret in the middle of an item text: a tab stays a tab
		if(indent && lines[0] == lines[1] && area.getSelectionStart() == area.getSelectionEnd()) {
			int column = area.getCaretPosition() - area.getLineStartOffset(lines[0]);
			if(column > prefixes.get(0).length && column < all[lines[0]].length())
				return false;
		}

		int caretLine = area.getLineOfOffset(area.getCaretPosition());
		int fromLineEnd = line(caretLine).length() - (area.getCaretPosition() - area.getLineStartOffset(caretLine));
		area.beginAtomicEdit();
		try {
			for(int i = lines[0]; i <= lines[1]; i++) {
				MarkdownText.ListPrefix p = MarkdownText.listPrefix(line(i));
				if(indent)
					indent(i, p);
				else
					outdent(i, p);
			}
			renumberAround(lines[0]);
		} finally {
			area.endAtomicEdit();
		}
		if(lines[0] == lines[1]) {
			int start = area.getLineStartOffset(caretLine);
			area.setCaretPosition(Math.max(start, start + line(caretLine).length() - fromLineEnd));
		}
		return true;
	}

	private void indent(int index, MarkdownText.ListPrefix p) throws Exception {
		String text = line(index);
		int current = p.indentWidth();
		// a child goes under the text of the item above it
		MarkdownText.ListPrefix sibling = previousItem(index, current, true);
		int target = sibling != null ? current + sibling.contentOffset() : current + p.contentOffset();
		String body = text.substring(p.indent.length());
		if(p.isOrdered()) {
			MarkdownText.ListPrefix newSibling = previousItem(index, target, true);
			if(newSibling == null || !newSibling.isOrdered())
				body = "1" + p.delimiter + body.substring(p.marker.length());
		}
		replaceLine(index, MarkdownText.repeat(' ', target) + body);
	}

	private void outdent(int index, MarkdownText.ListPrefix p) throws Exception {
		String text = line(index);
		int current = p.indentWidth();
		if(current == 0)
			return;
		MarkdownText.ListPrefix parent = previousItem(index, current, false);
		int target = parent != null ? parent.indentWidth() : 0;
		replaceLine(index, MarkdownText.repeat(' ', target) + text.substring(p.indent.length()));
	}

	/** Nearest item above: with the same indentation (sameLevel) or a smaller one; null at the start of the list. */
	private MarkdownText.ListPrefix previousItem(int index, int indent, boolean sameLevel) throws Exception {
		for(int i = index - 1; i >= 0; i--) {
			String l = line(i);
			if(l.trim().isEmpty())
				continue;
			MarkdownText.ListPrefix p = MarkdownText.listPrefix(l);
			int w = MarkdownText.leadingWidth(l);
			if(p == null || p.isQuote()) {
				if(w == 0)
					return null;
				continue;
			}
			if(sameLevel ? w == indent : w < indent)
				return p;
			if(w < indent)
				return null;
		}
		return null;
	}

	/** Numbers again the list that contains the line. */
	private void renumberAround(int index) throws Exception {
		int count = area.getLineCount();
		int first = index, last = index;
		while(first > 0 && belongsToList(line(first - 1)))
			first--;
		while(last < count - 1 && belongsToList(line(last + 1)))
			last++;
		String[] block = new String[last - first + 1];
		for(int i = first; i <= last; i++)
			block[i - first] = line(i);
		String[] fixed = MarkdownText.renumber(block);
		int caret = area.getCaretPosition();
		int caretLine = area.getLineOfOffset(caret);
		int caretColumnFromEnd = line(caretLine).length() - (caret - area.getLineStartOffset(caretLine));
		boolean changed = false;
		for(int i = 0; i < block.length; i++) {
			if(!block[i].equals(fixed[i])) {
				replaceLine(first + i, fixed[i]);
				changed = true;
			}
		}
		if(changed) {
			int start = area.getLineStartOffset(caretLine);
			area.setCaretPosition(Math.max(start, start + line(caretLine).length() - caretColumnFromEnd));
		}
	}

	private boolean belongsToList(String l) {
		return l.trim().isEmpty() || MarkdownText.leadingWidth(l) > 0 || (MarkdownText.listPrefix(l) != null && !MarkdownText.listPrefix(l).isQuote());
	}

	/** Alt+C: [ ] <-> [x] on the selected lines; list lines without a box get one. */
	public void toggleTasks() {
		try {
			int[] lines = selectedLines();
			area.beginAtomicEdit();
			try {
				for(int i = lines[0]; i <= lines[1]; i++) {
					String l = line(i);
					MarkdownText.ListPrefix p = MarkdownText.listPrefix(l);
					if(p == null || p.isQuote()) {
						if(!l.trim().isEmpty() || lines[0] == lines[1])
							replaceLine(i, "- [ ] " + l);
					} else if(p.task.isEmpty()) {
						int at = p.length;
						replaceLine(i, l.substring(0, at) + "[ ] " + l.substring(at));
					} else {
						int box = p.length - p.task.length();
						boolean done = p.task.charAt(1) != ' ';
						replaceLine(i, l.substring(0, box) + (done ? "[ ]" : "[x]") + l.substring(box + 3));
					}
				}
			} finally {
				area.endAtomicEdit();
			}
		} catch (Exception e) {
			logger.warn("Cannot toggle the tasks", e);
		}
	}

	// ------------------------------------------------------------------ tables

	/** Formats the table at the caret, or every table of the document when the caret is not in one. */
	public void formatTables() {
		try {
			String[] all = allLines();
			int caretLine = caretLine();
			int caretColumn = area.getCaretPosition() - area.getLineStartOffset(caretLine);
			int formatted = 0;
			area.beginAtomicEdit();
			try {
				if(MarkdownText.isTableLine(all[caretLine]) && !MarkdownText.isInsideFence(all, caretLine)) {
					int first = caretLine, last = caretLine;
					while(first > 0 && MarkdownText.isTableLine(all[first - 1]))
						first--;
					while(last < all.length - 1 && MarkdownText.isTableLine(all[last + 1]))
						last++;
					formatted += formatTable(all, first, last) ? 1 : 0;
				} else {
					// bottom up, so that the line numbers above stay valid
					List<int[]> tables = new ArrayList<int[]>();
					for(int i = 0; i < all.length; i++) {
						if(!MarkdownText.isTableLine(all[i]) || MarkdownText.isInsideFence(all, i))
							continue;
						int last = i;
						while(last < all.length - 1 && MarkdownText.isTableLine(all[last + 1]))
							last++;
						tables.add(new int[] {i, last});
						i = last;
					}
					for(int t = tables.size() - 1; t >= 0; t--)
						formatted += formatTable(all, tables.get(t)[0], tables.get(t)[1]) ? 1 : 0;
				}
			} finally {
				area.endAtomicEdit();
			}
			int start = area.getLineStartOffset(Math.min(caretLine, area.getLineCount() - 1));
			area.setCaretPosition(Math.min(start + caretColumn, start + line(Math.min(caretLine, area.getLineCount() - 1)).length()));
			host.status(formatted == 0 ? "No markdown table to format (a table needs its |---| row)" : formatted == 1 ? "Table formatted" : formatted + " tables formatted");
		} catch (Exception e) {
			logger.warn("Cannot format the table", e);
		}
	}

	private boolean formatTable(String[] all, int first, int last) throws Exception {
		String[] rows = Arrays.copyOfRange(all, first, last + 1);
		for(int i = 0; i < rows.length; i++)
			rows[i] = MarkdownText.stripCr(rows[i]);
		// the table starts at the header row: the line before the first delimiter row
		int delimiter = -1;
		for(int i = 1; i < rows.length; i++) {
			if(MarkdownText.isDelimiterRow(rows[i])) {
				delimiter = i;
				break;
			}
		}
		if(delimiter < 0)
			return false;
		String[] table = Arrays.copyOfRange(rows, delimiter - 1, rows.length);
		String[] out = MarkdownText.formatTable(table);
		if(out == null)
			return false;
		for(int i = out.length - 1; i >= 0; i--)
			replaceLine(first + delimiter - 1 + i, out[i]);
		return true;
	}

	/** Sorts the table at the caret by the column of the caret (header and |---| row stay on top). */
	public void sortTable(boolean ascending) {
		try {
			String[] all = allLines();
			int caretLine = caretLine();
			if(!MarkdownText.isTableLine(all[caretLine]) || MarkdownText.isInsideFence(all, caretLine)) {
				host.status("Put the caret in a table cell to sort that table by its column");
				return;
			}
			int first = caretLine, last = caretLine;
			while(first > 0 && MarkdownText.isTableLine(all[first - 1]))
				first--;
			while(last < all.length - 1 && MarkdownText.isTableLine(all[last + 1]))
				last++;
			int delimiter = -1;
			for(int i = first + 1; i <= last; i++) {
				if(MarkdownText.isDelimiterRow(all[i])) {
					delimiter = i;
					break;
				}
			}
			if(delimiter < 0 || caretLine < delimiter) {
				host.status(caretLine < delimiter ? "Put the caret in a row of the table body to choose the column" : "No markdown table here (a table needs its |---| row)");
				return;
			}
			int column = MarkdownExtras.columnAt(all[caretLine], area.getCaretPosition() - area.getLineStartOffset(caretLine));
			String[] rows = Arrays.copyOfRange(all, delimiter - 1, last + 1);
			for(int i = 0; i < rows.length; i++)
				rows[i] = MarkdownText.stripCr(rows[i]);
			String[] sorted = MarkdownExtras.sortTable(rows, column, ascending);
			if(sorted == null)
				return;
			area.beginAtomicEdit();
			try {
				for(int i = 0; i < sorted.length; i++)
					replaceLine(delimiter - 1 + i, sorted[i]);
			} finally {
				area.endAtomicEdit();
			}
			host.status("Table sorted by column " + (column + 1) + (ascending ? " (ascending)" : " (descending)"));
		} catch (Exception e) {
			logger.warn("Cannot sort the table", e);
		}
	}

	// ------------------------------------------------------------------ table of contents

	/** Updates the table of contents between the markers, or inserts it (with the markers) at the caret line. */
	public void insertOrUpdateToc() {
		try {
			if(updateToc()) {
				host.status("Table of contents updated");
				return;
			}
			if(MarkdownText.tocRange(area.getText()) != null) {
				host.status("The table of contents is already up to date");
				return;
			}
			String text = area.getText();
			String toc = MarkdownText.buildToc(text, "\n");
			int lineStart = area.getLineStartOffset(caretLine());
			String block = MarkdownText.TOC_START + "\n" + toc + MarkdownText.TOC_END + "\n";
			if(lineStart > 0 && !text.substring(0, lineStart).endsWith("\n\n"))
				block = "\n" + block;
			area.insert(block + "\n", lineStart);
			host.status("Table of contents inserted: it is updated every time the file is saved (" + MarkdownText.OMIT_IN_TOC + " leaves a heading out)");
		} catch (Exception e) {
			logger.warn("Cannot insert the table of contents", e);
		}
	}

	/** Updates the text between the TOC markers; false when there are no markers or nothing changed. */
	public boolean updateToc() {
		String text = area.getText();
		int[] range = MarkdownText.tocRange(text);
		if(range == null)
			return false;
		String toc = MarkdownText.buildToc(text, "\n");
		if(text.substring(range[0], range[1]).equals(toc))
			return false;
		int caret = area.getCaretPosition();
		int delta = toc.length() - (range[1] - range[0]);
		area.replaceRange(toc, range[0], range[1]);
		area.setCaretPosition(Math.max(0, Math.min(area.getDocument().getLength(), caret > range[1] ? caret + delta : Math.min(caret, range[0]))));
		return true;
	}

	// ------------------------------------------------------------------ paste

	/** Pastes the image of the clipboard as images/image-....png and a link to it. */
	public void pasteImage() {
		File base = host.baseDirectory();
		if(base == null) {
			host.status("Save the file first: the image is saved in an images folder next to it");
			return;
		}
		java.awt.image.BufferedImage image = ImagePaste.fromClipboard(ImagePaste.clipboard());
		if(image == null) {
			host.status("No image on the clipboard");
			return;
		}
		try {
			File saved = ImagePaste.save(image, base);
			area.replaceSelection(ImagePaste.link(base, saved));
			host.status("Image saved as " + ImagePaste.FOLDER + "/" + saved.getName());
		} catch (Exception e) {
			logger.warn("Cannot save the pasted image", e);
			host.status("Cannot save the image: " + e.getMessage());
		}
	}

	/** Paste: an url on a selected text makes the link [text](url), spreadsheet cells make a table, only an image is saved and linked. */
	private void smartPaste() {
		try {
			java.awt.datatransfer.Clipboard clipboard = ImagePaste.clipboard();
			if(!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor) && clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor) && host.baseDirectory() != null) {
				pasteImage();
				return;
			}
			if(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
				String table = MarkdownExtras.tabularToTable((String) clipboard.getData(DataFlavor.stringFlavor));
				if(table != null && !MarkdownText.isInsideFence(allLines(), caretLine())) {
					// a table cannot touch the paragraph above it: one blank line before
					String before = area.getText(0, area.getSelectionStart());
					String gap = before.isEmpty() || before.endsWith("\n\n") ? "" : before.endsWith("\n") ? "\n" : "\n\n";
					area.replaceSelection(gap + table + "\n");
					host.status("Table created from the pasted cells");
					return;
				}
			}
		} catch (Exception e) {
			// not text / no clipboard access: normal paste below
		}
		String selected = area.getSelectedText();
		if(selected != null && !selected.isEmpty() && selected.indexOf('\n') < 0) {
			try {
				Object data = ImagePaste.clipboard().getData(DataFlavor.stringFlavor);
				String url = data == null ? "" : data.toString().trim();
				if(URL.matcher(url).matches()) {
					int s = area.getSelectionStart();
					area.replaceSelection("[" + selected + "](" + url + ")");
					area.setCaretPosition(s + selected.length() + url.length() + 4);
					host.status("Link created from the pasted url");
					return;
				}
			} catch (Exception e) {
				// not text: normal paste
			}
		}
		if(ImagePaste.testClipboard == null)
			area.paste();
		else
			area.getTransferHandler().importData(area, ImagePaste.testClipboard.getContents(null)); // tests: their own clipboard
	}

	// ------------------------------------------------------------------ link completion

	/** One suggestion of the popup. */
	private static final class Suggestion {
		final String label;
		final String detail;
		final String insert;
		final boolean folder;

		Suggestion(String label, String detail, String insert, boolean folder) {
			this.label = label;
			this.detail = detail;
			this.insert = insert;
			this.folder = folder;
		}
	}

	private void installCompletion() {
		popup.setFocusable(false);
		popup.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY));
		list.setFocusable(false);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(10);
		list.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
				Suggestion s = (Suggestion) value;
				String detail = s.detail == null || s.detail.isEmpty() ? "" : "&nbsp;&nbsp;<font color=gray>" + MarkdownText.htmlEscape(s.detail) + "</font>";
				super.getListCellRendererComponent(l, "<html>" + MarkdownText.htmlEscape(s.label) + detail + "</html>", index, selected, focus);
				setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 10));
				return this;
			}
		});
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = list.locationToIndex(e.getPoint());
				if(index >= 0) {
					list.setSelectedIndex(index);
					chooseSuggestion();
				}
			}
		});
		JScrollPane scroll = new JScrollPane(list);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		popup.add(scroll);

		area.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				SwingUtilities.invokeLater(() -> checkCompletion(true));
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if(!popup.isVisible())
					return;
				int size = model.size();
				switch(e.getKeyCode()) {
				case KeyEvent.VK_DOWN:
					list.setSelectedIndex((list.getSelectedIndex() + 1) % size);
					break;
				case KeyEvent.VK_UP:
					list.setSelectedIndex((list.getSelectedIndex() - 1 + size) % size);
					break;
				case KeyEvent.VK_PAGE_DOWN:
					list.setSelectedIndex(Math.min(size - 1, list.getSelectedIndex() + 9));
					break;
				case KeyEvent.VK_PAGE_UP:
					list.setSelectedIndex(Math.max(0, list.getSelectedIndex() - 9));
					break;
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_TAB:
					chooseSuggestion();
					break;
				default:
					return;
				}
				list.ensureIndexIsVisible(list.getSelectedIndex());
				e.consume();
			}
		});
		area.addCaretListener(e -> {
			if(popup.isVisible())
				SwingUtilities.invokeLater(() -> checkCompletion(false));
		});
		area.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				hidePopup();
			}
		});
	}

	/** Esc: closes the suggestions (true when they were open). */
	public boolean handleEscape() {
		if(!popup.isVisible())
			return false;
		hidePopup();
		return true;
	}

	private void hidePopup() {
		popup.setVisible(false);
		replaceStart = -1;
	}

	private void checkCompletion(boolean typed) {
		try {
			if(!area.isFocusOwner() || area.getSelectionStart() != area.getSelectionEnd()) {
				hidePopup();
				return;
			}
			int caret = area.getCaretPosition();
			int index = area.getLineOfOffset(caret);
			int lineStart = area.getLineStartOffset(index);
			String before = area.getText(lineStart, caret - lineStart);
			Matcher m = LINK_CONTEXT.matcher(before);
			if(!m.find() || MarkdownText.isInsideFence(allLines(), index)) {
				hidePopup();
				return;
			}
			if(!typed && !popup.isVisible())
				return;
			String typedPath = m.group(1);
			List<Suggestion> suggestions = suggestions(typedPath);
			if(suggestions.isEmpty()) {
				hidePopup();
				return;
			}
			replaceStart = lineStart + m.start(1);
			model.clear();
			for(Suggestion s : suggestions)
				model.addElement(s);
			list.setSelectedIndex(0);
			showPopupAt(caret);
		} catch (Exception e) {
			logger.warn("Link completion failed", e);
			hidePopup();
		}
	}

	@SuppressWarnings("deprecation")
	private void showPopupAt(int offset) throws Exception {
		Rectangle r = area.modelToView(offset);
		if(r == null)
			return;
		popup.pack();
		Dimension size = popup.getPreferredSize();
		size.width = Math.min(Math.max(size.width, 260), 620);
		size.height = Math.min(size.height, 260);
		popup.setPopupSize(size);
		if(popup.isVisible()) {
			Point p = new Point(r.x, r.y + r.height);
			SwingUtilities.convertPointToScreen(p, area);
			popup.setLocation(p.x, p.y);
			popup.revalidate();
			popup.repaint();
		} else {
			popup.show(area, r.x, r.y + r.height);
		}
	}

	/** Headings for "#...", files / folders for a path, headings of another markdown file for "file.md#...". */
	private List<Suggestion> suggestions(String typed) {
		List<Suggestion> out = new ArrayList<Suggestion>();
		int hash = typed.indexOf('#');
		if(hash == 0) {
			addHeadings(out, area.getText(), "#", typed.substring(1));
			return out;
		}
		File base = host.baseDirectory();
		if(base == null)
			return out;
		if(hash > 0) {
			File target = new File(base, decode(typed.substring(0, hash)));
			if(target.isFile() && target.length() < 4L * 1024 * 1024) {
				try {
					addHeadings(out, FileWorker.readFileAsString(target), typed.substring(0, hash + 1), typed.substring(hash + 1));
				} catch (Exception e) {
					// unreadable: no suggestions
				}
			}
			return out;
		}
		int slash = typed.lastIndexOf('/');
		String folderPart = slash >= 0 ? typed.substring(0, slash + 1) : "";
		String namePart = decode(slash >= 0 ? typed.substring(slash + 1) : typed).toLowerCase(Locale.ROOT);
		File folder = folderPart.isEmpty() ? base : new File(base, decode(folderPart));
		File[] files = folder.listFiles();
		if(files == null)
			return out;
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				if(a.isDirectory() != b.isDirectory())
					return a.isDirectory() ? -1 : 1;
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});
		List<Suggestion> contains = new ArrayList<Suggestion>();
		for(File f : files) {
			String name = f.getName();
			if(name.startsWith(".") || f.isHidden())
				continue;
			String lower = name.toLowerCase(Locale.ROOT);
			if(!lower.contains(namePart))
				continue;
			boolean dir = f.isDirectory();
			Suggestion s = new Suggestion(name + (dir ? "/" : ""), dir ? "folder" : sizeOf(f), folderPart + encode(name) + (dir ? "/" : ""), dir);
			(lower.startsWith(namePart) ? out : contains).add(s);
			if(out.size() + contains.size() >= MAX_SUGGESTIONS)
				break;
		}
		out.addAll(contains);
		if(namePart.isEmpty() && folder.getAbsoluteFile().getParentFile() != null)
			out.add(new Suggestion("../", "parent folder", folderPart + "../", true));
		return out;
	}

	private void addHeadings(List<Suggestion> out, String markdown, String prefix, String filter) {
		String f = filter.toLowerCase(Locale.ROOT);
		for(MarkdownText.HeadingRef h : MarkdownText.headings(markdown)) {
			if(!f.isEmpty() && !h.slug.contains(f) && !h.text.toLowerCase(Locale.ROOT).contains(f))
				continue;
			out.add(new Suggestion(MarkdownText.repeat(' ', (h.level - 1) * 2) + MarkdownText.plainText(h.text), "#" + h.slug, prefix + h.slug, false));
			if(out.size() >= MAX_SUGGESTIONS)
				break;
		}
	}

	private boolean chooseSuggestion() {
		Suggestion s = list.getSelectedValue();
		if(s == null || replaceStart < 0) {
			hidePopup();
			return false;
		}
		int caret = area.getCaretPosition();
		area.replaceRange(s.insert, replaceStart, caret);
		area.setCaretPosition(replaceStart + s.insert.length());
		if(s.folder) {
			SwingUtilities.invokeLater(() -> checkCompletion(true));
		} else {
			hidePopup();
		}
		return true;
	}

	private static String sizeOf(File f) {
		long b = f.length();
		return b < 1024 ? b + " B" : b < 1024 * 1024 ? (b / 1024) + " KB" : String.format(Locale.ROOT, "%.1f MB", b / 1048576d);
	}

	private static String encode(String name) {
		return name.replace("%", "%25").replace(" ", "%20").replace("(", "%28").replace(")", "%29");
	}

	private static String decode(String path) {
		try {
			return URLDecoder.decode(path.replace("+", "%2B"), "UTF-8");
		} catch (Exception e) {
			return path;
		}
	}
}
