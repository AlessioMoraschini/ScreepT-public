package test.util.testsvarious.tree;

import java.io.File;
import java.util.Arrays;

import javax.swing.tree.TreeModel;

public class FileTreeModel implements TreeModel {

	    private FileNamedTest root;

	    public FileTreeModel(FileNamedTest root) {
	        this.root = root;
	    }

	    @Override
	    public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
	        //do nothing
	    }

	    @Override
	    public Object getChild(Object parent, int index) {
	    	FileNamedTest f = (FileNamedTest) parent;
	    	File[] sons = f.fileObj.listFiles();
			return new FileNamedTest(sons[index]);
			
	    }

	    @Override
	    public int getChildCount(Object parent) {
	    	File f = (parent instanceof FileNamedTest)?((FileNamedTest) parent).fileObj : (File)parent;
	    	File[] sons = f.listFiles();
	        if (!f.isDirectory()) {
	            return 0;
	        } else {
		        if (sons.length!=0) {
		        	return sons.length;
		        }else return 0;  
	        }
	    }

	    @Override
	    public int getIndexOfChild(Object parent, Object child) {
	    	FileNamedTest par = (FileNamedTest) parent;
	    	FileNamedTest ch = (FileNamedTest) child;
	        if(par!=null && ch != null) {
		        return Arrays.asList(par.fileObj.listFiles()).indexOf(ch.fileObj);
	        } else
	        	return 0;
	    }

	    @Override
	    public Object getRoot() {
	        return root;
	    }

	    @Override
	    public boolean isLeaf(Object node) {
	    	File f = (node instanceof FileNamedTest)?((FileNamedTest) node).fileObj : (File)node;
	        return !f.isDirectory();
	    }

	    @Override
	    public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
	        //do nothing
	    }

	    @Override
	    public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
	        //do nothing
	    }
	}
