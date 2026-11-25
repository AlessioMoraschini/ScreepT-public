package markdown.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import net.miginfocom.swing.MigLayout;

/**
 * Find and replace bar of the viewer: case, whole word, regular expression; finds in the markdown editor, or in the
 * rendered text when only the preview is shown (replace works on the editor only). Esc closes it, Enter / Shift+Enter
 * go to the next / previous match, the counter shows how many there are.
 */
final class FindBar extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final Color RED = new Color(220, 70, 70);

	private final RSyntaxTextArea editor;
	private final JEditorPane preview;
	private final BooleanSupplier previewOnly;
	private final Runnable onClose;

	private final JTextField find = new JTextField(18);
	private final JTextField replace = new JTextField(14);
	private final JToggleButton caseButton = toggle("Aa", "Match case");
	private final JToggleButton wordButton = toggle("W", "Whole word");
	private final JToggleButton regexButton = toggle(".*", "Regular expression (the replacement accepts $1, $2...)");
	private final JLabel count = new JLabel(" ");
	private final JPanel replaceRow = new JPanel(new MigLayout("insets 0, gap 4", "[]", "[]"));

	FindBar(RSyntaxTextArea editor, JEditorPane preview, BooleanSupplier previewOnly, Runnable onClose) {
		super(new MigLayout("insets 3 8 3 8, gap 4", "[][grow,fill][][][][][]push[]", "[]"));
		this.editor = editor;
		this.preview = preview;
		this.previewOnly = previewOnly;
		this.onClose = onClose;

		find.setToolTipText("Text to find (Enter next, Shift+Enter previous, Esc closes)");
		replace.setToolTipText("Replacement text (Enter replaces the current match)");
		find.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) { queryChanged(); }
			@Override public void removeUpdate(DocumentEvent e) { queryChanged(); }
			@Override public void changedUpdate(DocumentEvent e) { }
		});
		JButton previous = button("▲", "Previous match (Shift+F3)");
		JButton next = button("▼", "Next match (F3)");
		JButton close = button("✕", "Close (Esc)");
		JButton replaceOne = button("Replace", "Replaces the current match and goes to the next");
		JButton replaceAll = button("All", "Replaces every match");
		previous.addActionListener(e -> findNext(false));
		next.addActionListener(e -> findNext(true));
		close.addActionListener(e -> close());
		replaceOne.addActionListener(e -> replaceCurrent());
		replaceAll.addActionListener(e -> replaceEverything());
		for(JToggleButton t : new JToggleButton[] {caseButton, wordButton, regexButton})
			t.addActionListener(e -> queryChanged());
		count.setForeground(UIManager.getColor("Label.disabledForeground"));

		add(new JLabel("Find"));
		add(find);
		add(previous);
		add(next);
		add(count, "gapleft 6, gapright 6");
		add(caseButton);
		add(wordButton);
		add(regexButton);
		add(close, "gapleft 10");

		replaceRow.add(new JLabel("Replace"));
		replaceRow.add(replace, "w 180!");
		replaceRow.add(replaceOne);
		replaceRow.add(replaceAll);
		add(replaceRow, "newline, span, hidemode 3");
		replaceRow.setVisible(false);

		KeyAdapter keys = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					close();
					e.consume();
				} else if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					if(e.getSource() == replace)
						replaceCurrent();
					else
						findNext(!e.isShiftDown());
					e.consume();
				} else if(e.getKeyCode() == KeyEvent.VK_F3) {
					findNext(!e.isShiftDown());
					e.consume();
				}
			}
		};
		find.addKeyListener(keys);
		replace.addKeyListener(keys);
		setVisible(false);
	}

	private static JToggleButton toggle(String text, String tip) {
		JToggleButton b = new JToggleButton(text);
		b.setToolTipText(tip);
		b.setFocusable(false);
		b.setMargin(new Insets(1, 5, 1, 5));
		b.setMinimumSize(new Dimension(30, 10));
		b.setPreferredSize(new Dimension(Math.max(30, b.getPreferredSize().width + 6), b.getPreferredSize().height));
		return b;
	}

	private static JButton button(String text, String tip) {
		JButton b = new JButton(text);
		b.setToolTipText(tip);
		b.setFocusable(false);
		b.setMargin(new Insets(1, 6, 1, 6));
		return b;
	}

	// ------------------------------------------------------------------ api

	/** Shows the bar (with the replace row when asked) and puts the selected text, if any, in the search field. */
	void open(boolean withReplace, String seed) {
		setVisible(true);
		replaceRow.setVisible(withReplace && !previewOnly.getAsBoolean());
		if(seed != null && !seed.isEmpty() && seed.indexOf('\n') < 0)
			find.setText(seed);
		find.requestFocusInWindow();
		find.selectAll();
		revalidate();
		queryChanged();
	}

	void close() {
		setVisible(false);
		revalidate();
		if(onClose != null)
			onClose.run();
	}

	void setQuery(String text, boolean matchCase, boolean wholeWord, boolean regex) {
		caseButton.setSelected(matchCase);
		wordButton.setSelected(wholeWord);
		regexButton.setSelected(regex);
		find.setText(text);
		queryChanged();
	}

	void setReplacement(String text) {
		replace.setText(text);
	}

	String countText() {
		return count.getText().trim();
	}

	JTextField findField() {
		return find;
	}

	private SearchContext context() {
		SearchContext ctx = new SearchContext(find.getText());
		ctx.setMatchCase(caseButton.isSelected());
		ctx.setWholeWord(wordButton.isSelected());
		ctx.setRegularExpression(regexButton.isSelected());
		ctx.setSearchForward(true);
		ctx.setReplaceWith(replace.getText());
		ctx.setMarkAll(false);
		return ctx;
	}

	/** The pattern of the current query (null when empty or when the regular expression is wrong). */
	Pattern pattern() {
		String q = find.getText();
		if(q.isEmpty())
			return null;
		try {
			String body = regexButton.isSelected() ? q : Pattern.quote(q);
			if(wordButton.isSelected())
				body = "\\b(?:" + body + ")\\b";
			return Pattern.compile(body, caseButton.isSelected() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		} catch (PatternSyntaxException e) {
			return null;
		}
	}

	private void queryChanged() {
		find.setForeground(null);
		if(find.getText().isEmpty()) {
			count.setText(" ");
			return;
		}
		Pattern p = pattern();
		if(p == null) {
			find.setForeground(RED);
			count.setText("invalid expression");
			return;
		}
		int n = 0;
		Matcher m = p.matcher(target());
		while(m.find() && n < 100000) {
			if(m.end() > m.start())
				n++;
		}
		count.setText(n == 0 ? "no match" : n + (n == 1 ? " match" : " matches"));
		find.setForeground(n == 0 ? RED : null);
	}

	private String target() {
		if(previewOnly.getAsBoolean()) {
			try {
				return preview.getDocument().getText(0, preview.getDocument().getLength());
			} catch (BadLocationException e) {
				return "";
			}
		}
		return editor.getText();
	}

	/** Goes to the next / previous match from the caret, wrapping around at the end. Returns false when there is none. */
	boolean findNext(boolean forward) {
		if(find.getText().isEmpty() || pattern() == null)
			return false;
		return previewOnly.getAsBoolean() ? findInPreview(forward) : findInEditor(forward);
	}

	private boolean findInEditor(boolean forward) {
		SearchContext ctx = context();
		ctx.setSearchForward(forward);
		SearchResult r = SearchEngine.find(editor, ctx);
		if(!r.wasFound()) {
			editor.setCaretPosition(forward ? 0 : editor.getDocument().getLength());
			r = SearchEngine.find(editor, ctx);
		}
		return r.wasFound();
	}

	private boolean findInPreview(boolean forward) {
		String text = target();
		Matcher m = pattern().matcher(text);
		int start = -1, end = -1;
		if(forward) {
			int from = Math.min(preview.getSelectionEnd(), text.length());
			boolean found = m.find(from);
			while(found && m.end() == m.start())
				found = m.find();
			if(!found) {
				m.reset();
				found = m.find();
				while(found && m.end() == m.start())
					found = m.find();
			}
			if(found) {
				start = m.start();
				end = m.end();
			}
		} else {
			int from = preview.getSelectionStart();
			int lastStart = -1, lastEnd = -1;
			while(m.find()) {
				if(m.end() == m.start())
					continue;
				if(m.start() < from) {
					start = m.start();
					end = m.end();
				}
				lastStart = m.start();
				lastEnd = m.end();
			}
			if(start < 0) { // none before the caret: wrap to the last one
				start = lastStart;
				end = lastEnd;
			}
		}
		if(start < 0)
			return false;
		preview.getCaret().setSelectionVisible(true);
		preview.select(start, end);
		try {
			Rectangle r = preview.modelToView(start);
			if(r != null)
				preview.scrollRectToVisible(new Rectangle(r.x, Math.max(0, r.y - 40), 1, r.height + 80));
		} catch (BadLocationException e) {
			// the text changed under us
		}
		return true;
	}

	private void replaceCurrent() {
		if(previewOnly.getAsBoolean() || find.getText().isEmpty() || pattern() == null)
			return;
		SearchContext ctx = context();
		SearchResult r = SearchEngine.replace(editor, ctx);
		if(!r.wasFound()) {
			editor.setCaretPosition(0);
			SearchEngine.replace(editor, ctx);
		}
		queryChanged();
	}

	private void replaceEverything() {
		if(previewOnly.getAsBoolean() || find.getText().isEmpty() || pattern() == null)
			return;
		int before = editor.getCaretPosition();
		SearchResult r = SearchEngine.replaceAll(editor, context());
		editor.setCaretPosition(Math.min(before, editor.getDocument().getLength()));
		queryChanged();
		count.setText(r.getCount() + " replaced");
	}
}
