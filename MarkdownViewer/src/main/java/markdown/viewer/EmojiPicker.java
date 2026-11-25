package markdown.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

/**
 * Small window to choose an emoji: the last used ones on top, a search box (name and keywords, English and Italian)
 * and a scrollable grid. A click (or Enter in the search box: the first match) inserts the emoji and closes the window; Esc closes it.
 */
final class EmojiPicker extends JDialog {

	private static final long serialVersionUID = 1L;
	private static final int COLUMNS = 8;
	private static final int MAX_RECENT = 16;

	private final EmojiCatalog.Recents recents;
	private final Consumer<String> inserter;
	private final Consumer<String> recentsSaver;
	private final JTextField search = new JTextField();
	private final JPanel recentGrid = new JPanel(new GridLayout(0, COLUMNS, 2, 2));
	private final JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 2, 2));
	private final JLabel recentLabel = new JLabel("Recent");
	private final JLabel count = new JLabel(" ");
	private final Font emojiFont;
	private List<EmojiCatalog.Entry> shown;

	/**
	 * @param storedRecents the saved "a,b,c" list of the last used emoji
	 * @param inserter receives the emoji character to put in the text
	 * @param recentsSaver receives the new "a,b,c" list to save
	 */
	EmojiPicker(Window owner, String storedRecents, Consumer<String> inserter, Consumer<String> recentsSaver) {
		super(owner, "Emoji", ModalityType.MODELESS);
		this.recents = new EmojiCatalog.Recents(storedRecents, MAX_RECENT);
		this.inserter = inserter;
		this.recentsSaver = recentsSaver;
		this.emojiFont = pickEmojiFont();

		setUndecorated(true);
		getRootPane().setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor") != null ? UIManager.getColor("Component.borderColor") : Color.GRAY));
		setLayout(new BorderLayout());

		search.setToolTipText("Search by name or keyword (smile, fire, gatto, bug...). Enter inserts the first one, Esc closes");
		JPanel top = new JPanel(new MigLayout("insets 6 6 2 6, fillx, wrap 1", "[grow,fill]", "[]2[]"));
		top.add(search);
		top.add(count);
		add(top, BorderLayout.NORTH);

		JPanel center = new JPanel(new BorderLayout());
		JPanel recentBox = new JPanel(new BorderLayout(0, 2));
		recentLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 0, 0));
		recentLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
		recentBox.add(recentLabel, BorderLayout.NORTH);
		JPanel recentWrap = new JPanel(new BorderLayout());
		recentWrap.setBorder(BorderFactory.createEmptyBorder(0, 6, 4, 6));
		recentWrap.add(recentGrid, BorderLayout.NORTH);
		recentBox.add(recentWrap, BorderLayout.CENTER);
		center.add(recentBox, BorderLayout.NORTH);

		JPanel gridWrap = new JPanel(new BorderLayout());
		gridWrap.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		gridWrap.add(grid, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(gridWrap);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(24);
		scroll.setPreferredSize(new Dimension(COLUMNS * 44 + 30, 290));
		center.add(scroll, BorderLayout.CENTER);
		add(center, BorderLayout.CENTER);

		search.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) { refresh(); }
			@Override public void removeUpdate(DocumentEvent e) { refresh(); }
			@Override public void changedUpdate(DocumentEvent e) { }
		});
		search.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					dispose();
					e.consume();
				} else if(e.getKeyCode() == KeyEvent.VK_ENTER && shown != null && !shown.isEmpty()) {
					choose(shown.get(0));
					e.consume();
				}
			}
		});
		// closes when the window loses the focus (a click elsewhere)
		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				if(e.getOppositeWindow() != EmojiPicker.this && isShowing())
					SwingUtilities.invokeLater(() -> dispose());
			}
		});
		refresh();
		pack();
	}

	private static Font pickEmojiFont() {
		Set<String> names = new HashSet<String>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
		for(String candidate : new String[] {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Segoe UI Symbol"}) {
			if(names.contains(candidate))
				return new Font(candidate, Font.PLAIN, 22);
		}
		return new Font(Font.DIALOG, Font.PLAIN, 22);
	}

	/** Opens the window under a component (the button that opened it). */
	void showUnder(Component anchor) {
		Point p = anchor.isShowing() ? anchor.getLocationOnScreen() : new Point(100, 100);
		setLocation(p.x, p.y + anchor.getHeight());
		setVisible(true);
		search.requestFocusInWindow();
	}

	List<EmojiCatalog.Entry> shownEntries() {
		return shown;
	}

	JTextField searchField() {
		return search;
	}

	List<EmojiCatalog.Entry> recentEntries() {
		return recents.entries();
	}

	private void refresh() {
		String query = search.getText();
		shown = EmojiCatalog.search(query);
		grid.removeAll();
		for(EmojiCatalog.Entry e : shown)
			grid.add(button(e));
		// the grid keeps its columns even with few results
		for(int i = shown.size(); i < COLUMNS && i > 0; i++)
			grid.add(new JLabel());
		count.setText(query.trim().isEmpty() ? shown.size() + " emoji" : shown.isEmpty() ? "no emoji for \"" + query.trim() + "\"" : shown.size() + " found");
		count.setForeground(UIManager.getColor("Label.disabledForeground"));

		recentGrid.removeAll();
		List<EmojiCatalog.Entry> last = recents.entries();
		boolean showRecent = query.trim().isEmpty() && !last.isEmpty();
		for(EmojiCatalog.Entry e : last)
			recentGrid.add(button(e));
		for(int i = last.size(); i < COLUMNS && !last.isEmpty(); i++)
			recentGrid.add(new JLabel());
		recentGrid.getParent().getParent().setVisible(showRecent);
		revalidate();
		repaint();
	}

	private JButton button(final EmojiCatalog.Entry e) {
		JButton b = new JButton(e.text);
		b.setFont(emojiFont);
		b.setToolTipText(":" + e.name + ":");
		b.setFocusable(false);
		b.setMargin(new Insets(2, 2, 2, 2));
		b.setPreferredSize(new Dimension(42, 38));
		b.putClientProperty("JButton.buttonType", "toolBarButton");
		b.addActionListener(a -> choose(e));
		return b;
	}

	/** Inserts the emoji, remembers it as the last used and closes the window. */
	void choose(EmojiCatalog.Entry e) {
		recents.use(e.name);
		recentsSaver.accept(recents.serialize());
		dispose();
		inserter.accept(e.text);
	}
}
