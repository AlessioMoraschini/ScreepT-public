package test.util.testsvarious.tree;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.text.Position.Bias;
import javax.swing.tree.TreePath;

public class TreeTest extends JFrame{
	private static final long serialVersionUID = -3904270394542044408L;
	
	static JTree tree;
	static FileTreeModel model;
	static JButton btnTest;
	static File testFile = new File("root/level2/level3/file.txt");
	static File root = new File("root");
	
	public static void main(String[] args) {
		
		TreeTest container = new TreeTest();
		container.setVisible(true);
		container.setBounds(0,0,400,500);
		
	
		selectFileInNamedFileTree(testFile, tree);
	}
	
	public TreeTest(){
		setBounds(0, 0, 600, 500);
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane);
//		add(new JLabel("PROVA"));
		scrollPane.setPreferredSize(new Dimension(500,500));
	
		model = new FileTreeModel(new FileNamedTest(root));
		tree = new JTree(model);
		tree.setVisibleRowCount(10);
		scrollPane.setViewportView(tree);
		
		btnTest = new JButton("Test debug");
		scrollPane.setRowHeaderView(btnTest);
		
		tree.setVisibleRowCount(10);
		
		btnTest.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				TreePath nodePath = tree.getSelectionPath();
			}
		});
	}
	
	/**
	 * Use this method to retrieve and select a node contained in the root of given tree.
	 * 
	 * This method is meant for jtree models that use only name of file as label for the node.
	 * @param fileToSelect
	 * @param root
	 * @param tree
	 */
	public static void selectFileInNamedFileTree(File fileToSelect, JTree tree){
		
		File root = ((FileNamedTest)tree.getModel().getRoot()).fileObj;
		LinkedList<File> nodes = new LinkedList<File>();
		File current = new File(fileToSelect.getAbsolutePath());
		
		// find all nodes till root
		while((current != null && current.exists()) && !current.getName().equals(root.getName())){
			nodes.add(current);
			current = current.getParentFile();
		}
		nodes.add(new File(root.getAbsolutePath()));
		
		// start selection from root node
		while(nodes.size() > 0){
			File currentSelection = nodes.getLast();
			TreePath nodePath = tree.getNextMatch(currentSelection.getName(), 0, Bias.Forward);
			tree.setExpandsSelectedPaths(true);
			tree.setSelectionPath(nodePath);
			tree.expandPath(nodePath);
			nodes.removeLast();
		}		
	}
	
	public static Rectangle getCurrentDisplaySize(Component reference){
	
	  final Rectangle currentScreenBounds = reference.getGraphicsConfiguration().getBounds();
	
	  return currentScreenBounds;
	}

}
