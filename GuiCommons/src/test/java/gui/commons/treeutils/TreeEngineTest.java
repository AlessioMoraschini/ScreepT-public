package gui.commons.treeutils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.files.om.FileNamed;

/**
 * The engine behind the file tree search: filter rules (what a folder walk leaves out), the in-memory file name index
 * (build, search, results as a tree) and the tree helpers (colours, expansion state).
 */
public class TreeEngineTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File root;

	@Before
	public void project() throws Exception {
		root = tmp.newFolder("project");
		touch("README.md");
		touch("build.xml");
		touch("src/main/App.java");
		touch("src/main/Util.java");
		touch("src/main/deep/er/Hidden.java");
		touch("src/test/AppTest.java");
		touch("docs/Guide.TXT");
		touch("docs/notes.txt");
		touch(".git/config");
		touch(".git/objects/pack.bin");
		touch("target/classes/App.class");
		touch("node_modules/lib/index.js");
	}

	private void touch(String relative) throws Exception {
		File f = new File(root, relative);
		f.getParentFile().mkdirs();
		Files.write(f.toPath(), relative.getBytes(StandardCharsets.UTF_8));
	}

	private static List<String> names(FileNameIndex index, int[] entries) {
		// entries are ids: ask the tree view for the files behind them
		List<String> out = new ArrayList<String>();
		FileNameIndex.View view = index.view(entries, null);
		for (FileNamed[] group : view.children.values()) {
			for (FileNamed f : group) {
				if (!f.isDirectory()) {
					out.add(f.getFileName());
				}
			}
		}
		java.util.Collections.sort(out);
		return out;
	}

	// ------------------------------------------------------------------ filter rules

	@Test
	public void defaultRulesLeaveOutToolFoldersButNotSimilarFiles() {
		TreeFilterRules rules = TreeFilterRules.defaults();
		assertTrue(rules.excludes(".git", TreeFilterRules.Kind.FOLDER));
		assertTrue(rules.excludes("node_modules", TreeFilterRules.Kind.FOLDER));
		assertTrue(rules.excludes("TARGET", TreeFilterRules.Kind.FOLDER));
		assertFalse("rules for folders do not hit files", rules.excludes("target", TreeFilterRules.Kind.FILE));
		assertFalse(rules.excludes("src", TreeFilterRules.Kind.FOLDER));
		assertFalse(rules.excludes("targets", TreeFilterRules.Kind.FOLDER));
	}

	@Test
	public void globRulesWithKindsAndTheMasterSwitch() {
		TreeFilterRules rules = new TreeFilterRules();
		rules.getRules().add(new TreeFilterRules.Rule(true, "*.class", TreeFilterRules.Kind.FILE));
		rules.getRules().add(new TreeFilterRules.Rule(true, "~$*", TreeFilterRules.Kind.ANY));
		rules.getRules().add(new TreeFilterRules.Rule(false, "*.log", TreeFilterRules.Kind.FILE));
		rules.getRules().add(new TreeFilterRules.Rule(true, "  ", TreeFilterRules.Kind.FILE));
		assertTrue(rules.excludes("App.CLASS", TreeFilterRules.Kind.FILE));
		assertFalse(rules.excludes("App.class", TreeFilterRules.Kind.FOLDER));
		assertTrue(rules.excludes("~$report.docx", TreeFilterRules.Kind.FILE));
		assertTrue(rules.excludes("~$tmp", TreeFilterRules.Kind.FOLDER));
		assertFalse("disabled rule", rules.excludes("a.log", TreeFilterRules.Kind.FILE));
		assertFalse("blank rule matches nothing", rules.excludes("anything", TreeFilterRules.Kind.FILE));
		rules.setMasterOn(false);
		assertFalse("master switch off: nothing is excluded", rules.excludes("App.class", TreeFilterRules.Kind.FILE));
		assertFalse(rules.isMasterOn());
	}

	@Test
	public void rulesSurviveSerializationEvenWithSeparatorsInThePattern() {
		TreeFilterRules rules = new TreeFilterRules();
		rules.getRules().add(new TreeFilterRules.Rule(true, "a;b|c%d", TreeFilterRules.Kind.FILE));
		rules.getRules().add(new TreeFilterRules.Rule(false, "tmp?", TreeFilterRules.Kind.SYMLINK));
		rules.setMasterOn(false);
		String saved = rules.serialize();
		TreeFilterRules back = TreeFilterRules.parse(saved);
		assertTrue(back.sameAs(rules));
		assertEquals(2, back.getRules().size());
		assertEquals("a;b|c%d", back.getRules().get(0).pattern);
		assertEquals(TreeFilterRules.Kind.SYMLINK, back.getRules().get(1).kind);
		assertFalse(back.getRules().get(1).enabled);
		assertFalse(back.isMasterOn());
		assertEquals(saved, back.serialize());
	}

	@Test
	public void parsingDistinguishesNothingSavedFromAnEmptyChoice() {
		assertTrue("nothing saved yet: the defaults", TreeFilterRules.parse(null).sameAs(TreeFilterRules.defaults()));
		assertTrue(TreeFilterRules.parse("   ").sameAs(TreeFilterRules.defaults()));
		TreeFilterRules none = TreeFilterRules.parse("M1:-");
		assertTrue("the user removed every rule: it stays empty", none.getRules().isEmpty());
		assertFalse(none.excludes(".git", TreeFilterRules.Kind.FOLDER));
		TreeFilterRules damaged = TreeFilterRules.parse("M1:1|D|ok;garbage;1|D|;0|F|fine");
		assertEquals("malformed items are skipped, good ones kept", 2, damaged.getRules().size());
		TreeFilterRules emptyRules = new TreeFilterRules();
		assertEquals("M1:-", emptyRules.serialize());
	}

	@Test
	public void copiesAreIndependent() {
		TreeFilterRules a = TreeFilterRules.defaults();
		TreeFilterRules b = a.copy();
		assertTrue(a.sameAs(b));
		b.getRules().get(0).enabled = false;
		b.setMasterOn(false);
		assertFalse(a.sameAs(b));
		assertTrue(a.getRules().get(0).enabled);
		assertFalse(a.sameAs(null));
	}

	// ------------------------------------------------------------------ file name index

	@Test
	public void indexSeesEveryEntryExceptWhatTheRulesExclude() {
		FileNameIndex all = FileNameIndex.build(root, null, null, null);
		FileNameIndex filtered = FileNameIndex.build(root, TreeFilterRules.defaults(), null, null);
		assertNotNull(all);
		assertNotNull(filtered);
		assertEquals(root, all.getRoot());
		assertFalse(all.isTruncated());
		assertTrue(all.size() > filtered.size());
		assertTrue("the whole content of an excluded folder disappears", filtered.search("pack.bin", null, 10).entries.length == 0);
		assertEquals("without rules it is there", 1, all.search("pack.bin", null, 10).entries.length);
		assertEquals("node_modules is excluded by default", 0, filtered.search("index.js", null, 10).entries.length);
		assertEquals(1, filtered.search("hidden.java", null, 10).entries.length);
		assertTrue(all.memoryBytes() > 0);
		assertTrue(all.getBuiltAt() > 0);
	}

	@Test
	public void plainSearchIsCaseInsensitiveAndMatchesAnywhereInTheName() {
		FileNameIndex index = FileNameIndex.build(root, TreeFilterRules.defaults(), null, null);
		assertEquals(Arrays.asList("App.java", "AppTest.java"), names(index, index.search("app", null, 100).entries));
		assertEquals(Arrays.asList("App.java", "AppTest.java", "Hidden.java", "Util.java"), names(index, index.search(".JAVA", null, 100).entries));
		assertEquals(Arrays.asList("Guide.TXT", "notes.txt"), names(index, index.search(".txt", null, 100).entries));
		assertEquals(0, index.search("zzz-not-there", null, 100).entries.length);
		assertEquals("empty query finds nothing", 0, index.search("", null, 100).entries.length);
		// folders match too
		FileNameIndex.View view = index.view(index.search("deep", null, 10).entries, null);
		assertEquals(1, view.matches);
	}

	@Test
	public void wildcardSearchMatchesTheWholeName() {
		FileNameIndex index = FileNameIndex.build(root, TreeFilterRules.defaults(), null, null);
		assertEquals(Arrays.asList("App.java", "AppTest.java"), names(index, index.search("app*.java", null, 100).entries));
		assertEquals(Arrays.asList("App.java"), names(index, index.search("a??.java", null, 100).entries));
		assertEquals("whole name: a part is not enough", 0, index.search("pp*", null, 100).entries.length);
	}

	@Test
	public void refiningASearchWorksOnThePreviousResultOnly() {
		FileNameIndex index = FileNameIndex.build(root, TreeFilterRules.defaults(), null, null);
		int[] java = index.search(".java", null, 100).entries;
		int[] refined = index.search("test", java, 100).entries;
		assertEquals(Arrays.asList("AppTest.java"), names(index, refined));
		int[] none = index.search("guide", java, 100).entries;
		assertEquals("Guide.TXT is not in the previous result", 0, none.length);
		assertEquals(Arrays.asList("App.java", "AppTest.java"), names(index, index.search("app*", java, 100).entries));
	}

	@Test
	public void resultLimitIsReportedNotSilentlyApplied() {
		FileNameIndex index = FileNameIndex.build(root, TreeFilterRules.defaults(), null, null);
		FileNameIndex.Matches limited = index.search(".java", null, 2);
		assertEquals(2, limited.entries.length);
		assertTrue(limited.overLimit);
		FileNameIndex.Matches exact = index.search(".java", null, 4);
		assertEquals(4, exact.entries.length);
		assertFalse(exact.overLimit);
		FileNameIndex.Matches limitedPattern = index.search("*.java", null, 3);
		assertEquals(3, limitedPattern.entries.length);
		assertTrue(limitedPattern.overLimit);
	}

	@Test
	public void resultViewIsTheTreeOfMatchesWithTheirFoldersFoldersFirst() {
		FileNameIndex index = FileNameIndex.build(root, TreeFilterRules.defaults(), null, null);
		FileNameIndex.View view = index.view(index.search("java", null, 100).entries, null);
		assertEquals(4, view.matches);
		FileNamed[] top = view.children.get(root.getAbsolutePath());
		assertNotNull(top);
		assertEquals("only the folder that leads to a match", 1, top.length);
		assertEquals("src", top[0].getFileName());
		FileNamed[] src = view.children.get(new File(root, "src").getAbsolutePath());
		assertEquals(Arrays.asList("main", "test"), Arrays.asList(src[0].getFileName(), src[1].getFileName()));
		FileNamed[] main = view.children.get(new File(root, "src/main").getAbsolutePath());
		assertEquals("folders before files, then alphabetical", "deep", main[0].getFileName());
		assertTrue(main[0].isDirectory());
		assertEquals("App.java", main[1].getFileName());
		assertEquals("Util.java", main[2].getFileName());
		assertTrue(view.branches.contains(new File(root, "src/main/deep/er")));
		assertFalse(view.branches.contains(root));
	}

	@Test
	public void cancellingTheBuildGivesNothingInsteadOfAPartialIndex() {
		BooleanSupplier cancelled = new BooleanSupplier() {
			@Override
			public boolean getAsBoolean() {
				return true;
			}
		};
		assertNull(FileNameIndex.build(root, null, cancelled, null));
	}

	@Test
	public void missingRootGivesAnEmptyIndex() {
		FileNameIndex index = FileNameIndex.build(new File(root, "nope"), null, null, null);
		assertNotNull(index);
		assertEquals(0, index.size());
		assertEquals(0, index.search("a", null, 10).entries.length);
	}

	@Test
	public void bigFolderIsIndexedCompletely() throws Exception {
		File big = tmp.newFolder("big");
		int folders = 30;
		int perFolder = 120;
		for (int d = 0; d < folders; d++) {
			File dir = new File(big, "dir" + d);
			dir.mkdirs();
			for (int f = 0; f < perFolder; f++) {
				new File(dir, "file" + f + (f % 10 == 0 ? ".special" : ".dat")).createNewFile();
			}
		}
		FileNameIndex index = FileNameIndex.build(big, null, null, null);
		assertEquals(folders + folders * perFolder, index.size());
		assertEquals(folders * (perFolder / 10), index.search(".special", null, 100000).entries.length);
		assertEquals(folders, index.search("dir*", null, 100000).entries.length);
	}

	// ------------------------------------------------------------------ tree helpers

	@Test
	public void colourHelpersFollowLuminance() {
		assertTrue(TreeUtils.isDark(Color.BLACK));
		assertTrue(TreeUtils.isDark(new Color(30, 30, 60)));
		assertFalse(TreeUtils.isDark(Color.WHITE));
		assertFalse(TreeUtils.isDark(new Color(250, 250, 200)));
		assertTrue("light text on a dark background", TreeUtils.isDark(Color.BLACK) && !TreeUtils.isDark(TreeUtils.textOn(Color.BLACK)));
		assertTrue("dark text on a light background", TreeUtils.isDark(TreeUtils.textOn(Color.WHITE)));
		assertEquals(new Color(50, 100, 128), TreeUtils.blend(new Color(0, 0, 0), new Color(100, 200, 255), 0.5f));
		assertEquals(Color.RED, TreeUtils.blend(Color.RED, Color.BLUE, 0f));
		assertEquals(Color.BLUE, TreeUtils.blend(Color.RED, Color.BLUE, 1f));
		assertEquals("amount is clamped", Color.BLUE, TreeUtils.blend(Color.RED, Color.BLUE, 7f));
		assertEquals(new Color(128, 0, 128), TreeUtils.blend(Color.RED, Color.BLUE, 0.5f));
	}

	private static DefaultMutableTreeNode sampleTree() {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
		DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
		DefaultMutableTreeNode a1 = new DefaultMutableTreeNode("a1");
		DefaultMutableTreeNode b = new DefaultMutableTreeNode("b");
		rootNode.add(a);
		a.add(a1);
		a1.add(new DefaultMutableTreeNode("leaf"));
		rootNode.add(b);
		b.add(new DefaultMutableTreeNode("b1"));
		return rootNode;
	}

	@Test
	public void pathOfANodeStartsAtTheRoot() {
		DefaultMutableTreeNode rootNode = sampleTree();
		DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) rootNode.getFirstLeaf();
		TreePath path = TreeUtils.getPath(leaf);
		assertEquals(4, path.getPathCount());
		assertEquals(rootNode, path.getPathComponent(0));
		assertEquals(leaf, path.getLastPathComponent());
		assertNull(TreeUtils.getPath(null));
	}

	@Test
	public void descendantCheckNeedsALongerPathOnTheSameBranch() {
		DefaultMutableTreeNode rootNode = sampleTree();
		TreePath a = TreeUtils.getPath(rootNode.getChildAt(0));
		TreePath leaf = TreeUtils.getPath(rootNode.getFirstLeaf());
		TreePath b = TreeUtils.getPath(rootNode.getChildAt(1));
		assertTrue(TreeUtils.isDescendant(leaf, a));
		assertFalse(TreeUtils.isDescendant(a, leaf));
		assertFalse("a path is not its own descendant", TreeUtils.isDescendant(a, a));
		assertFalse(TreeUtils.isDescendant(leaf, b));
	}

	@Test
	public void expandAndCollapseWholeTrees() {
		JTree tree = new JTree(new DefaultTreeModel(sampleTree()));
		TreeUtils.collapseTree(tree);
		tree.expandRow(0);
		int collapsed = tree.getRowCount();
		TreeUtils.expandAllRecursively(tree, tree.getPathForRow(0));
		assertTrue(tree.getRowCount() > collapsed);
		assertEquals("root, a, a1, leaf, b, b1", 6, tree.getRowCount());
		TreeUtils.collapseTree(tree);
		assertEquals("everything but the root row is hidden again", 1, tree.getRowCount());

		TreeUtils.expandNodeChildren(tree, tree.getPathForRow(0));
		assertEquals(6, tree.getRowCount());
		TreeUtils.setTreeExpandedState(tree, false);
		assertEquals("collapse all keeps the root open: root, a, b", 3, tree.getRowCount());
		TreeUtils.setTreeExpandedState(tree, true);
		assertEquals(6, tree.getRowCount());
	}

	@Test
	public void expansionStateIsSavedAndRestoredRowByRow() {
		JTree tree = new JTree(new DefaultTreeModel(sampleTree()));
		TreeUtils.setTreeExpandedState(tree, true);
		tree.collapseRow(4); // b
		String[] saved = TreeUtils.getExpansionStates(tree);
		assertEquals(tree.getRowCount(), saved.length);
		int rowsBefore = tree.getRowCount();

		TreeUtils.collapseTree(tree);
		assertEquals(1, tree.getRowCount());
		TreeUtils.restoreExpansionStates(tree, saved);
		assertEquals("same rows visible as before", rowsBefore, tree.getRowCount());
		assertTrue(tree.isExpanded(tree.getPathForRow(1)));
		assertFalse("b stays collapsed", tree.isExpanded(tree.getPathForRow(4)));
		TreeUtils.restoreExpansionStates(null, saved);
		TreeUtils.restoreExpansionStates(tree, null);
		assertArrayEquals(new String[0], TreeUtils.getExpansionStates(null));
	}

	@Test
	public void pathOfAFileHasOneEntryPerLevelAndNothingToSelectGivesNull() throws Exception {
		TreePath p = TreeUtils.getTreePathFromFile(new FileNamed(new File(root, "src/main/App.java")), 3);
		assertEquals(3, p.getPathCount());
		assertEquals("App.java", ((File) p.getLastPathComponent()).getName());
		assertEquals("main", ((File) p.getPathComponent(1)).getName());
		assertEquals("src", ((File) p.getPathComponent(0)).getName());
		assertNull(TreeUtils.selectFileInNamedFileTree(null, new JTree(), true));
	}
}
