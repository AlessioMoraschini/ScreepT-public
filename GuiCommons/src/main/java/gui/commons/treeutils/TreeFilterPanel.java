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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import gui.commons.treeutils.om.FileTreeNamedModel;
import various.common.light.files.om.FileNamed;
import various.common.light.i18n.I18n;

/**
 * Filter bar of a file tree: what is typed hides every entry whose name does not contain the text (case insensitive), keeping the
 * folders that lead to the ones that match. Clear (the button or Esc) shows everything again, with the folders open as they were.
 * <p>
 * Fast by design: the names under the root are read once, in the background, into a {@link FileNameIndex}; typing then searches
 * memory only (a longer text refines the previous result). The index is kept for the last roots used (switching tab between two
 * folders does not read them again), is refreshed in the background when it gets old or the tree was reloaded, and is released
 * after a few minutes without filtering. Folders and files that are not interesting (.git, node_modules...) are skipped by the
 * rules of the funnel button ({@link TreeFilterRules}, edited with {@link TreeFilterRulesDialog}). Every background result carries
 * the generation it was started for and the root it belongs to: an answer for a previous text, tab or folder is dropped, never
 * applied to the tree that is showing now. It follows the colors of the tree, so it fits any look and feel.
 */
public class TreeFilterPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Matches over which the result is not shown (the text must be narrowed) */
	public static final int MATCH_CAP = 20000;
	/** An index older than this is refreshed in the background the next time it is used */
	private static final long STALE_MS = 60000L;
	/** Without filtering for this long the indexes are released (memory) */
	private static final int IDLE_RELEASE_MS = 5 * 60 * 1000;
	/** Indexes kept (the roots used last) */
	private static final int CACHE_SIZE = 2;
	/** Folders opened at most to show the matches */
	private static final int MAX_EXPANDED = 2500;

	private static final Color WARN_COLOR = new Color(214, 140, 30);

	private final JTree tree;
	private final Supplier<String> rulesLoader;
	private final Consumer<String> rulesSaver;
	private final JTextField field = new JTextField();
	private final JButton clear = new JButton("✕");
	private final JButton rulesButton = new JButton(new FunnelIcon());
	private final JLabel status = new JLabel(" ");
	private final Timer debounce;
	private final Timer idle;

	private TreeFilterRules rules;
	private int rulesVersion;
	/** Indexes by root path, the most recently used last */
	private final Map<String, FileNameIndex> cache = new LinkedHashMap<String, FileNameIndex>(4, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, FileNameIndex> eldest) {
			return size() > CACHE_SIZE;
		}
	};
	/** Roots whose index is known to be old (the tree was reloaded): refreshed at the next use */
	private final Set<String> staleRoots = new HashSet<String>();

	private SwingWorker<FileNameIndex, Integer> building;
	private String buildingRoot;
	private SwingWorker<SearchResult, Void> searching;
	/** Bumped at every new search, clear and reload: older answers are dropped */
	private int generation;

	/** Last search, to refine it when the text grows */
	private FileNameIndex lastIndex;
	private String lastQuery = "";
	private int[] lastMatches;
	private boolean lastOver;

	private String[] expansionBefore;
	private boolean filtering;

	private static final class SearchResult {
		final FileNameIndex.Matches matches;
		final FileNameIndex.View view;

		SearchResult(FileNameIndex.Matches matches, FileNameIndex.View view) {
			this.matches = matches;
			this.view = view;
		}
	}

	/** Funnel drawn with the color of the button text, so it fits every theme */
	private static final class FunnelIcon implements Icon {
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(c.getForeground());
			Path2D funnel = new Path2D.Float();
			funnel.moveTo(x, y + 1);
			funnel.lineTo(x + 12, y + 1);
			funnel.lineTo(x + 7.6f, y + 6.2f);
			funnel.lineTo(x + 7.6f, y + 12);
			funnel.lineTo(x + 4.4f, y + 10.2f);
			funnel.lineTo(x + 4.4f, y + 6.2f);
			funnel.closePath();
			g2.fill(funnel);
			g2.dispose();
		}

		@Override
		public int getIconWidth() {
			return 12;
		}

		@Override
		public int getIconHeight() {
			return 14;
		}
	}

	/**
	 * @param tree the tree to filter (its model must be a {@link FileTreeNamedModel})
	 * @param rulesLoader gives the saved rules (see {@link TreeFilterRules#parse}); null or empty = defaults
	 * @param rulesSaver receives the rules (as {@link TreeFilterRules#serialize()}) when the user changes them
	 */
	public TreeFilterPanel(JTree tree, Supplier<String> rulesLoader, Consumer<String> rulesSaver) {
		super(new BorderLayout(2, 0));
		this.tree = tree;
		this.rulesLoader = rulesLoader;
		this.rulesSaver = rulesSaver;
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

		rulesButton.setToolTipText(I18n.t("tree.filter_rules_tip"));
		rulesButton.setFocusable(false);
		rulesButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
		rulesButton.setPreferredSize(new Dimension(26, 24));
		rulesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		rulesButton.putClientProperty("JButton.buttonType", "toolBarButton");
		rulesButton.addActionListener(e -> editRules());

		status.setFont(status.getFont().deriveFont(Font.PLAIN, 11f));
		status.setBorder(BorderFactory.createEmptyBorder(1, 4, 0, 0));
		status.setVisible(false);

		debounce = new Timer(180, e -> apply());
		debounce.setRepeats(false);
		idle = new Timer(IDLE_RELEASE_MS, e -> releaseIndexes());
		idle.setRepeats(false);
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

		JPanel buttons = new JPanel(new BorderLayout(0, 0));
		buttons.setOpaque(false);
		buttons.add(clear, BorderLayout.WEST);
		buttons.add(rulesButton, BorderLayout.EAST);
		JPanel row = new JPanel(new BorderLayout(2, 0));
		row.setOpaque(false);
		row.add(field, BorderLayout.CENTER);
		row.add(buttons, BorderLayout.EAST);
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

	private TreeFilterRules rules() {
		if (rules == null) {
			rules = TreeFilterRules.parse(rulesLoader == null ? "" : rulesLoader.get());
		}
		return rules;
	}

	private void changed() {
		clear.setVisible(!field.getText().isEmpty());
		debounce.restart();
	}

	/** Root of the tree that is showing now (null when the tree has no file model) */
	private File currentRoot() {
		TreeModel model = tree.getModel();
		if (model instanceof FileTreeNamedModel && model.getRoot() instanceof FileNamed) {
			return ((FileNamed) model.getRoot()).getFile();
		}
		return null;
	}

	/** Empties the filter and shows the whole tree again */
	public void clearFilter() {
		debounce.stop();
		generation++;
		cancelSearch();
		cancelBuild();
		field.setText("");
		clear.setVisible(false);
		status.setVisible(false);
		removeFilterFromModel();
	}

	private void removeFilterFromModel() {
		TreeModel model = tree.getModel();
		if (filtering && model instanceof FileTreeNamedModel) {
			((FileTreeNamedModel) model).setFilterView(null);
			if (expansionBefore != null) {
				TreeUtils.restoreExpansionStates(tree, expansionBefore);
			}
		}
		filtering = false;
		expansionBefore = null;
		lastMatches = null;
		lastIndex = null;
		lastQuery = "";
	}

	/**
	 * To call after the tree got a new model (another root, or the same one read again): the view of the old model is gone, so the
	 * filter is applied again to the new one. Answers still on their way for the old model are dropped.
	 */
	public void reapply() {
		generation++;
		cancelSearch();
		filtering = false;
		expansionBefore = null;
		lastMatches = null;
		lastIndex = null;
		File root = currentRoot();
		if (root != null) {
			staleRoots.add(root.getAbsolutePath());
		}
		if (!getQuery().isEmpty()) {
			apply();
		}
	}

	/** The indexes are not worth their memory when nobody filters */
	private void releaseIndexes() {
		if (building == null) {
			cache.clear();
			staleRoots.clear();
			lastIndex = null;
			lastMatches = null;
		}
	}

	private void cancelSearch() {
		if (searching != null) {
			searching.cancel(true);
			searching = null;
		}
	}

	private void cancelBuild() {
		if (building != null) {
			building.cancel(true);
			building = null;
			buildingRoot = null;
		}
	}

	private void editRules() {
		Window owner = SwingUtilities.getWindowAncestor(this);
		TreeFilterRules edited = TreeFilterRulesDialog.edit(owner, rules().copy());
		if (edited == null || edited.sameAs(rules())) {
			return; // cancelled by user, or nothing changed
		}
		rules = edited;
		rulesVersion++;
		if (rulesSaver != null) {
			rulesSaver.accept(edited.serialize());
		}
		// the indexes were made with the old rules
		generation++;
		cancelSearch();
		cancelBuild();
		cache.clear();
		staleRoots.clear();
		lastIndex = null;
		lastMatches = null;
		if (!getQuery().isEmpty()) {
			apply();
		}
	}

	private void apply() {
		String query = getQuery();
		idle.restart();
		cancelSearch();
		if (query.isEmpty()) {
			generation++;
			status.setVisible(false);
			removeFilterFromModel();
			return;
		}
		File root = currentRoot();
		if (root == null) {
			return;
		}
		String key = root.getAbsolutePath();
		FileNameIndex index = cache.get(key);
		if (index == null) {
			if (building == null || !key.equals(buildingRoot)) {
				startBuild(root, false);
			} else {
				showStatus(I18n.fmt("tree.filter_indexing", 0), false);
			}
			return;
		}
		if (building == null && (staleRoots.remove(key) || System.currentTimeMillis() - index.getBuiltAt() > STALE_MS)) {
			startBuild(root, true); // refreshed in the background, the current one answers meanwhile
		}
		search(index, query);
	}

	private void showStatus(String text, boolean warning) {
		status.setText(text);
		status.setForeground(warning ? WARN_COLOR : TreeUtils.blend(TreeUtils.textOn(tree.getBackground()), tree.getBackground(), 0.35f));
		status.setVisible(true);
		revalidate();
	}

	private void startBuild(final File root, final boolean silent) {
		cancelBuild();
		final String key = root.getAbsolutePath();
		final TreeFilterRules snapshot = rules().copy();
		final int version = rulesVersion;
		if (!silent) {
			showStatus(I18n.fmt("tree.filter_indexing", 0), false);
		}
		SwingWorker<FileNameIndex, Integer> worker = new SwingWorker<FileNameIndex, Integer>() {
			@Override
			protected FileNameIndex doInBackground() {
				return FileNameIndex.build(root, snapshot, this::isCancelled, n -> publish(n));
			}

			@Override
			protected void process(List<Integer> counts) {
				if (!silent && building == this && !counts.isEmpty()) {
					showStatus(I18n.fmt("tree.filter_indexing", counts.get(counts.size() - 1)), false);
				}
			}

			@Override
			protected void done() {
				if (isCancelled() || building != this) {
					return;
				}
				building = null;
				buildingRoot = null;
				FileNameIndex built;
				try {
					built = get();
				} catch (Exception e) {
					return;
				}
				if (built == null || version != rulesVersion) {
					return;
				}
				cache.put(key, built);
				staleRoots.remove(key);
				File now = currentRoot();
				String query = getQuery();
				if (now != null && key.equals(now.getAbsolutePath()) && !query.isEmpty()) {
					search(built, query);
				}
			}
		};
		building = worker;
		buildingRoot = key;
		worker.execute();
	}

	private void search(final FileNameIndex index, final String query) {
		cancelSearch();
		final int gen = ++generation;
		String lower = query.toLowerCase(Locale.ROOT);
		boolean refine = index == lastIndex && lastMatches != null && !lastOver && !lastQuery.isEmpty() && lower.contains(lastQuery);
		final int[] within = refine ? lastMatches : null;
		SwingWorker<SearchResult, Void> worker = new SwingWorker<SearchResult, Void>() {
			@Override
			protected SearchResult doInBackground() {
				FileNameIndex.Matches matches = index.search(query, within, MATCH_CAP);
				FileNameIndex.View view = matches.overLimit ? null : index.view(matches.entries, this::isCancelled);
				return new SearchResult(matches, view);
			}

			@Override
			protected void done() {
				if (isCancelled() || gen != generation) {
					return; // an answer for a previous text, tab or folder
				}
				searching = null;
				SearchResult result;
				try {
					result = get();
				} catch (Exception e) {
					return;
				}
				finish(index, query, result);
			}
		};
		searching = worker;
		worker.execute();
	}

	private void finish(FileNameIndex index, String query, SearchResult result) {
		File root = currentRoot();
		if (!query.equals(getQuery()) || root == null || !root.equals(index.getRoot())) {
			return; // text or folder changed meanwhile
		}
		lastIndex = index;
		lastQuery = query.toLowerCase(Locale.ROOT);
		lastMatches = result.matches.entries;
		lastOver = result.matches.overLimit;
		if (result.matches.overLimit || result.view == null) {
			showStatus(I18n.fmt("tree.filter_too_many_matches", MATCH_CAP), true);
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
		((FileTreeNamedModel) model).setFilterView(result.view);
		expandBranches(result.view, (FileNamed) model.getRoot());
		tree.scrollRowToVisible(0);
		int count = result.matches.entries.length;
		String text;
		if (count == 0) {
			text = I18n.t("tree.filter_none");
		} else if (index.isTruncated()) {
			text = I18n.fmt("tree.filter_matches_truncated", count, FileNameIndex.MAX_ENTRIES);
		} else {
			text = I18n.fmt("tree.filter_matches", count);
		}
		showStatus(text, count == 0 || index.isTruncated());
	}

	/** Opens the folders that lead to the matches, from the top down (only those: nothing else is read or laid out) */
	private void expandBranches(FileNameIndex.View view, FileNamed rootNode) {
		List<File> branches = new ArrayList<File>(view.branches);
		final Map<File, Integer> depth = new HashMap<File, Integer>();
		for (File f : branches) {
			int d = 0;
			for (File p = f; p != null; p = p.getParentFile()) {
				d++;
			}
			depth.put(f, d);
		}
		Collections.sort(branches, new Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				return Integer.compare(depth.get(a), depth.get(b));
			}
		});
		Map<String, TreePath> paths = new HashMap<String, TreePath>();
		paths.put(rootNode.getFileAbsPath(), new TreePath(rootNode));
		int opened = 0;
		for (File dir : branches) {
			if (opened >= MAX_EXPANDED) {
				break;
			}
			File parent = dir.getParentFile();
			TreePath parentPath = parent == null ? null : paths.get(parent.getAbsolutePath());
			if (parentPath == null) {
				continue;
			}
			TreePath path = parentPath.pathByAddingChild(new FileNamed(dir, true));
			paths.put(dir.getAbsolutePath(), path);
			tree.expandPath(path);
			opened++;
		}
	}
}
