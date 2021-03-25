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
package treeutils;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.text.Position.Bias;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import various.common.light.files.om.FileNamed;
import various.common.light.om.LimitedConcurrentList;

public class TreeUtils {
	
	public static final Color DEF_BACK_SELECTION = new Color(10, 102, 153, 40);
	public static final Color DEF_BACK_HOVER = new Color(0, 102, 153, 20);
	
	public static int MAX_EXPAND_ITERATIONS = 256;
	
	public static TreePath getPath(TreeNode treeNode) {
		List<Object> nodes = new ArrayList<Object>();
		if (treeNode != null) {
			nodes.add(treeNode);
			treeNode = treeNode.getParent();
			while (treeNode != null) {
				nodes.add(0, treeNode);
				treeNode = treeNode.getParent();
			}
		}

		return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
	}
	
	public static void enableTooltipsOnTree(JTree tree) {
		if(tree != null) {
			ToolTipManager.sharedInstance().registerComponent(tree);
		}
	}
	
	public static void scrollCenteredToCurrentSelection(JTree tree, boolean scrollLeft) {

		TreePath selectionPath = tree.getLeadSelectionPath();
		
		if (selectionPath != null) {
			
			tree.makeVisible(selectionPath);
			Rectangle bounds = tree.getPathBounds(selectionPath);
			
			if (bounds != null) {
				
				int firstVisibleRow = tree.getRowForLocation(0, bounds.y);
				int currentRow = tree.getLeadSelectionRow(); // This is the new half -> newfirst + half = current
				int rowHeight = tree.getRowHeight();
				int nRowsVisible = bounds.height / rowHeight;

				int half = nRowsVisible / 2;
				int newFirstRow = currentRow - half;
				
				// calculate the delta to apply to bounds
				int deltaRows = newFirstRow - firstVisibleRow;

				bounds.x = (scrollLeft) ? 0 : bounds.x;
				bounds.y += deltaRows;
				tree.scrollRectToVisible(bounds);
			} 
			
		}
	}
	
	public static void scrollToTreeCurrentRow(JTree tree, int row) {
		int nRows = tree.getRowCount();
		int currentRow = tree.getLeadSelectionRow();
		
		if (currentRow + 2 < nRows) {
			tree.scrollRowToVisible(tree.getLeadSelectionRow() + 2);
		} else {
			tree.scrollRowToVisible(tree.getLeadSelectionRow());
		}
	}
	
	public static TreePath getTreePathFromFile(FileNamed file, int levels) {
		File[] result = new File[levels];
		for (int i = levels-1; i >= 0; i--) {
			if (file != null) {
				result[i] = file.mFile;
				file = new FileNamed(file.mFile.getParentFile());
			}else {
				break;
			}
		}
		return new TreePath(result);
	}
	
	public static void expandNodeChildren(JTree tree, TreePath parent) {
	    try {
			if (tree != null && parent != null) {
				TreeNode node = (TreeNode) parent.getLastPathComponent();
				if (node.getChildCount() >= 0) {
					for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
						TreeNode n = (TreeNode) e.nextElement();
						TreePath path = parent.pathByAddingChild(n);
						expandNodeChildren(tree, path);
					}
				}
				tree.expandPath(parent);
			} 
		} catch (Exception e) {
		}
	}
	
	/**
	 *  Recursively expand all  given path (This expands all child paths in the given tree). Implicit use of only one row, the selected one
	 * @param tree
	 * @param treePath
	 */
	public static void expandAllChildNodes(JTree tree, TreePath treePath){
		
		int startingIndex = tree.getRowForPath(treePath);
		expandAllChildNodes(tree, startingIndex);
	}
	
	/**
	 *  Recursively expand all  given path (This expands all child paths in the given tree). Implicit use of only one row, the selected one
	 * @param tree
	 * @param treePath
	 */
	public static void expandAllChildNodes(final JTree tree, final int startingIndex){
		final int rowCount = tree.getRowCount();
		
		tree.expandRow(startingIndex);
		
		if(tree.getRowCount()!=rowCount){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// IF a folder is expandend rowcount increase, and this will be executed
					expandAllChildNodes(tree, startingIndex + 1, new Integer(rowCount - startingIndex), MAX_EXPAND_ITERATIONS);
				}
			});
		}
	}

	/**
	 * End distance is the number of rows after current at time of start (used to stop when current line is expanded)
	 * @param tree
	 * @param startingIndex
	 * @param endDistance
	 */
	private static void expandAllChildNodes(JTree tree, int startingIndex, Integer endDistance, int maxIterations){
		boolean stop = tree.getRowCount() - startingIndex -1 < endDistance;
		
		tree.expandRow(startingIndex);
		
		maxIterations --;
		
		try {
			if(!stop && maxIterations > 0){
				expandAllChildNodes(tree, startingIndex+1, endDistance, maxIterations);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 *  Recursively expand all  given path (This expands all child paths in the given tree)
	 * @param tree
	 * @param treePath
	 */
	public static void expandAllChildNodes(JTree tree, int startingIndex, int rowCount){
	    for(int i=startingIndex;i<rowCount;++i){
	        tree.expandRow(i);
	    }

	    if(tree.getRowCount()!=rowCount){
	    	expandAllChildNodes(tree, rowCount, tree.getRowCount());
	    }
	}
	
	public static void expandRoot(JTree tree) {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
		tree.expandPath(new TreePath(root.getPath()));
	}

	public static void expandNode(JTree tree, DefaultMutableTreeNode node) {
		tree.expandPath(new TreePath(node.getPath()));
	}
	
	/**
	 *  Recursively expand the given path (This does not expand child paths in the given tree)
	 * @param tree
	 * @param treePath
	 */
    public static void expandAllRecursively(JTree tree, TreePath treePath)
    {
        TreeModel model = tree.getModel();
        Object lastPathComponent = treePath.getLastPathComponent();
        int childCount = model.getChildCount(lastPathComponent);
        if (childCount == 0)
        {
            return;
        }
        tree.setExpandsSelectedPaths(true);
        tree.setSelectionPath(treePath);
        tree.expandPath(treePath);
        for (int i=0; i<childCount; i++)
        {
            Object child = model.getChild(lastPathComponent, i);
            int grandChildCount = model.getChildCount(child);
            if (grandChildCount > 0)
            {
                class LocalTreePath extends TreePath
                {
                    private static final long serialVersionUID = 0;
                    public LocalTreePath(
                        TreePath parent, Object lastPathComponent)
                    {
                        super(parent, lastPathComponent);
                    }
                }
                TreePath nextTreePath = new LocalTreePath(treePath, child);
                expandAllRecursively(tree, nextTreePath);
            }
        }
    }

    /**
	 * Close each single expanded node that is descendant of selected one
	 */
	public static void collapseTreeSelectionChildren(JTree tree, int row) {
		try {
			TreePath startingPath = tree.getPathForRow(row);
			for (int i = tree.getRowCount(); i >= row; i--) {
				TreePath son = tree.getPathForRow(row);
				if(startingPath.isDescendant(son) || startingPath.equals(son)) {
					try {
						tree.collapseRow(i);
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Close each single node from bottom to top
	 */
	public static void collapseTree(JTree tree) {
		try {
			for (int i = tree.getRowCount(); i >= 0; i--) {
				tree.collapseRow(i);
			}
		} catch (Exception e) {
		}
	}
	
	/**
	 * Use this method to retrieve and select a node contained in the root of given tree.
	 * 
	 * This method is meant for jtree models that use only name of file as label for the node.
	 * 
	 * If check existance is true, then seletion will be performed only if file exists
	 * @param fileToSelect
	 * @param root
	 * @param tree
	 * @throws IOException 
	 */
	public static TreePath selectFileInNamedFileTree(File fileToSelect, JTree tree, boolean checkExistance) throws IOException{
		
		TreePath nodePath;
		try {
			nodePath = null;
			boolean error = fileToSelect == null 
					|| tree == null 
					|| tree.getModel() == null 
					|| tree.getModel().getRoot() == null 
					|| (checkExistance && !fileToSelect.exists());
			
			if(error) {
				return nodePath;
			}
			
			File root = ((FileNamed)tree.getModel().getRoot()).mFile;
			if(root != null) {
				root = root.getAbsoluteFile();
			}
			
			LimitedConcurrentList<File> nodes = new LimitedConcurrentList<File>(Integer.MAX_VALUE);
			File current = new File(fileToSelect.getCanonicalPath());
			
			// find all nodes till root
			while((current != null && current.exists()) && !current.getName().equals(root.getName())){
				nodes.add(current);
				current = current.getParentFile();
			}
			nodes.add(new File(root.getCanonicalPath()));
			// start selection from root node
			int lastSelRow = 0;
			while(nodes.getList().size() > 0){
				File currentSelection = nodes.getLast();
				nodePath = tree.getNextMatch(currentSelection.getName(), lastSelRow, Bias.Forward);
				tree.setExpandsSelectedPaths(true);
				tree.setSelectionPath(nodePath);
				tree.expandPath(nodePath);
				nodes.removeLast();
				lastSelRow = tree.getRowForPath(nodePath);
			}
			return nodePath;
		} catch (Exception e) {
			return tree.getPathForRow(0);
		} finally {
			scrollToVisible(tree, true);
		}
		
	}

	public static void scrollToVisible(JTree tree, boolean scrollLeft) {
		tree.scrollRowToVisible(tree.getLeadSelectionRow());
		if (scrollLeft) {
			Rectangle rect = tree.getVisibleRect();
			tree.scrollRectToVisible(new Rectangle(0, rect.y, rect.width, rect.height));
		}
	}
	
    public static boolean isDescendant(TreePath path1, TreePath path2){
        int count1 = path1.getPathCount();
        int count2 = path2.getPathCount();
        if(count1<=count2)
            return false;
        while(count1!=count2){
            path1 = path1.getParentPath();
            count1--;
        }
        return path1.equals(path2);
    }
    
    public static String[] getExpansionStates(JTree tree) {
    	if(tree == null) {return new String[0];}
    	
    	String[] rowsSelectionBack = new String[tree.getRowCount()];
    	for(int i = 0; i< tree.getRowCount(); i++) {
    		rowsSelectionBack[i] = getExpansionState(tree, i);
    	}
    	return rowsSelectionBack;
    }
    
    public static void restoreExpansionStates(JTree tree, String[] selectionBackup) {
    	if(selectionBackup == null || tree == null) {return;}
    	
    	int sourcesNum = selectionBackup.length;
    	for(int i = 0; i< sourcesNum; i++) {
    		restoreExpanstionState(tree, i, selectionBackup[i]);
    	}
    }
 
    public static String getExpansionState(JTree tree, int row){
        TreePath rowPath = tree.getPathForRow(row);
        StringBuffer buf = new StringBuffer();
        int rowCount = tree.getRowCount();
        for(int i=row; i<rowCount; i++){
            TreePath path = tree.getPathForRow(i);
            if(i==row || isDescendant(path, rowPath)){
                if(tree.isExpanded(path))
                    buf.append(","+String.valueOf(i-row));
            }else
                break;
        }
        return buf.toString();
    }
 
    public static void restoreExpanstionState(JTree tree, int row, String expansionState){
        StringTokenizer stok = new StringTokenizer(expansionState, ",");
        while(stok.hasMoreTokens()){
            int token = row + Integer.parseInt(stok.nextToken());
            tree.expandRow(token);
        }
    }

	public static void setTreeExpandedState(JTree tree, boolean expanded) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
		setNodeExpandedState(tree, node, expanded);
	}

	@SuppressWarnings("unchecked")
	public static void setNodeExpandedState(JTree tree, DefaultMutableTreeNode node, boolean expanded) {
		ArrayList<DefaultMutableTreeNode> list = Collections.list(node.children());
		for (DefaultMutableTreeNode treeNode : list) {
			setNodeExpandedState(tree, treeNode, expanded);
		}
		if (!expanded && node.isRoot()) {
			return;
		}
		TreePath path = new TreePath(node.getPath());
		if (expanded) {
			tree.expandPath(path);
		} else {
			tree.collapsePath(path);
		}
	}
}
