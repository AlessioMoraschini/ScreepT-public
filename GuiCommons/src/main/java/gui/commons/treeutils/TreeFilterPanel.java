/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package gui.commons.treeutils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreeModel;

import gui.commons.treeutils.om.FileTreeNamedModel;
import various.common.light.files.om.FileNamed;
import various.common.light.i18n.I18n;

/**
 * Filter bar of a file tree: what is typed hides every entry whose name does not contain the text (case insensitive), keeping the
 * folders that lead to the ones that match. Clear (the button or Esc) shows everything again, with the folders open as they were.
 * The folders are read in the background, never on the interface thread; over {@link #WARN_LIMIT} elements the user is asked
 * before going on (it can take long and slow things down), and over {@link #HARD_LIMIT} the filter is refused. It follows the
 * colors of the tree, so it fits any look and feel.
 */
public class TreeFilterPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Elements read before the user is asked to confirm */
	public static final int WARN_LIMIT = 5000;
	/** Elements over which the filter is refused */
	public static final int HARD_LIMIT = 200000;
	/** Rows opened at most to show the matches */
	private static final int MAX_EXPANDED_ROWS = 4000;

	private static final Color WARN_COLOR = new Color(214, 140, 30);

	private final JTree tree;
	private final Predicate<String> confirmHeavy;
	private final JTextField field = new JTextField();
	private final JButton clear = new JButton("✕");
	private final JLabel status = new JLabel(" ");
	private final Timer debounce;
	private SwingWorker<ScanResult, Void> running;
	private String[] expansionBefore;
	private boolean filtering;

	/** The outcome of reading the folders */
	private static final class ScanResult {
		Set<String> visible = new HashSet<String>();
		int matches;
		boolean tooMany;
		boolean overHardLimit;
		boolean cancelled;
	}

	/**
	 * @param tree the tree to filter (its model must be a {@link FileTreeNamedModel})
	 * @param confirmHeavy asked with the message when the folder is big: true to go on
	 */
	public TreeFilterPanel(JTree tree, Predicate<String> confirmHeavy) {
		super(new BorderLayout(2, 0));
		this.tree = tree;
		this.confirmHeavy = confirmHeavy;
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(3, 3, 1, 3));

		field.putClientProperty("JTextField.placeholderText", I18n.t("tree.filter_placeholder"));
		field.setToolTipText(I18n.t("tree.filter_placeholder"));
		field.setPreferredSize(new Dimension(80, 24));

		clear.setToolTipText(I18n.t("tree.filter_clear_tip"));
		clear.setFocusable(false);
		clear.setMargin(new java.awt.Insets(0, 4, 0, 4));
		clear.setPreferredSize(new Dimension(24, 24));
		clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		clear.putClientProperty("JButton.buttonType", "toolBarButton");
		clear.setVisible(false);
		clear.addActionListener(e -> clearFilter());

		status.setFont(status.getFont().deriveFont(Font.PLAIN, 11f));
		status.setBorder(BorderFactory.createEmptyBorder(1, 4, 0, 0));
		status.setVisible(false);

		debounce = new Timer(350, e -> apply());
		debounce.setRepeats(false);
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				changed();
			}
		});
		field.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !field.getText().isEmpty()) {
					clearFilter();
					e.consume();
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					debounce.stop();
					apply();
				}
			}
		});

		JPanel row = new JPanel(new BorderLayout(2, 0));
		row.setOpaque(false);
		row.add(field, BorderLayout.CENTER);
		row.add(clear, BorderLayout.EAST);
		add(row, BorderLayout.NORTH);
		add(status, BorderLayout.SOUTH);
	}

	/** The text field, for the owner that wants to focus it */
	public JTextField getField() {
		return field;
	}

	public String getQuery() {
		return field.getText().trim();
	}

	/** Colors the status line from the background of the tree (call it after the theme changes) */
	public void applyColors(Color treeBackground) {
		status.setForeground(TreeUtils.blend(TreeUtils.textOn(treeBackground), treeBackground, 0.35f));
	}

	private void changed() {
		clear.setVisible(!field.getText().isEmpty());
		debounce.restart();
	}

	/** Empties the filter and shows the whole tree again */
	public void clearFilter() {
		debounce.stop();
		cancelRunning();
		field.setText("");
		clear.setVisible(false);
		status.setVisible(false);
		removeFilterFromModel();
	}

	private void removeFilterFromModel() {
		TreeModel model = tree.getModel();
		if (filtering && model instanceof FileTreeNamedModel) {
			((FileTreeNamedModel) model).setVisiblePaths(null);
			if (expansionBefore != null) {
				TreeUtils.restoreExpansionStates(tree, expansionBefore);
			}
		}
		filtering = false;
		expansionBefore = null;
	}

	/** Applies the filter again to the current model (to call after the tree got a new model: the filter would be lost) */
	public void reapply() {
		filtering = false;
		expansionBefore = null;
		if (!getQuery().isEmpty()) {
			apply();
		}
	}

	private void cancelRunning() {
		if (running != null) {
			running.cancel(true);
			running = null;
		}
	}

	private void apply() {
		String query = getQuery();
		cancelRunning();
		if (query.isEmpty()) {
			status.setVisible(false);
			removeFilterFromModel();
			return;
		}
		scan(query, WARN_LIMIT);
	}

	private void showStatus(String text, boolean warning) {
		status.setText(text);
		status.setForeground(warning ? WARN_COLOR : TreeUtils.blend(TreeUtils.textOn(tree.getBackground()), tree.getBackground(), 0.35f));
		status.setVisible(true);
		revalidate();
	}

	private void scan(final String query, final int limit) {
		final Object rootObject = tree.getModel().getRoot();
		if (!(rootObject instanceof FileNamed) || !(tree.getModel() instanceof FileTreeNamedModel)) {
			return;
		}
		final File root = ((FileNamed) rootObject).getFile();
		showStatus(I18n.t("tree.filter_scanning"), false);

		final SwingWorker<ScanResult, Void> worker = new SwingWorker<ScanResult, Void>() {
			@Override
			protected ScanResult doInBackground() {
				return scanDisk(root, query.toLowerCase(), limit, this);
			}

			@Override
			protected void done() {
				if (isCancelled() || running != this) {
					return;
				}
				running = null;
				ScanResult result;
				try {
					result = get();
				} catch (Exception e) {
					return;
				}
				finish(query, limit, result);
			}
		};
		running = worker;
		worker.execute();
	}

	private void finish(String query, int limit, ScanResult result) {
		if (!query.equals(getQuery())) {
			return; // the text changed meanwhile: a newer scan is coming
		}
		if (result.overHardLimit) {
			showStatus(I18n.fmt("tree.filter_limit", HARD_LIMIT), true);
			return;
		}
		if (result.tooMany) {
			String msg = I18n.fmt("tree.filter_too_many_ask", WARN_LIMIT);
			if (confirmHeavy != null && confirmHeavy.test(msg)) {
				scan(query, 0);
			} else {
				showStatus(I18n.t("tree.filter_skipped"), true);
			}
			return;
		}
		TreeModel model = tree.getModel();
		if (!(model instanceof FileTreeNamedModel)) {
			return;
		}
		if (!filtering) {
			expansionBefore = TreeUtils.getExpansionStates(tree);
			filtering = true;
		}
		((FileTreeNamedModel) model).setVisiblePaths(result.visible);
		for (int i = 0; i < tree.getRowCount() && i < MAX_EXPANDED_ROWS; i++) {
			tree.expandRow(i);
		}
		tree.scrollRowToVisible(0);
		showStatus(result.matches == 0 ? I18n.t("tree.filter_none") : I18n.fmt("tree.filter_matches", result.matches), result.matches == 0);
	}

	/**
	 * Reads the folders under the root looking for names that contain the query. limit is the number of elements after which it
	 * stops asking for confirmation (0 = no question, only the hard limit applies).
	 */
	private static ScanResult scanDisk(File root, String queryLower, int limit, SwingWorker<?, ?> worker) {
		ScanResult result = new ScanResult();
		String rootPath = root.getAbsolutePath();
		java.util.ArrayDeque<File> stack = new java.util.ArrayDeque<File>();
		stack.push(root);
		int count = 0;
		while (!stack.isEmpty()) {
			if (worker.isCancelled()) {
				result.cancelled = true;
				return result;
			}
			File dir = stack.pop();
			File[] sons = dir.listFiles();
			if (sons == null) {
				continue;
			}
			for (File son : sons) {
				count++;
				if (limit > 0 && count > limit) {
					result.tooMany = true;
					return result;
				}
				if (count > HARD_LIMIT) {
					result.overHardLimit = true;
					return result;
				}
				boolean isDir = son.isDirectory();
				if (son.getName().toLowerCase().contains(queryLower)) {
					result.matches++;
					// the entry and every folder that leads to it from the root
					for (File f = son; f != null && !f.getAbsolutePath().equals(rootPath); f = f.getParentFile()) {
						if (!result.visible.add(f.getAbsolutePath()) && f != son) {
							break; // from here up it is already there
						}
					}
				}
				if (isDir && !java.nio.file.Files.isSymbolicLink(son.toPath())) {
					stack.push(son);
				}
			}
		}
		return result;
	}
}
