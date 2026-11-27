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
package gui.commons.treeutils.om;

import java.io.File;
import java.util.Arrays;

import javax.swing.tree.TreeModel;

public class FileTreeModel implements TreeModel {

	    private File root;

	    public FileTreeModel(File root) {
	        this.root = root;
	    }

	    @Override
	    public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
	        //do nothing
	    }

	    @Override
	    public Object getChild(Object parent, int index) {
	        File f = (File) parent;
		    File[] sons = f.listFiles();
			return sons[index];
			
	    }

	    @Override
	    public int getChildCount(Object parent) {
	        File f = (File) parent;
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
	        File par = (File) parent;
	        File ch = (File) child;
	        if(par!=null) {
		        return Arrays.asList(par.listFiles()).indexOf(ch);
	        }else return 0;
	    }

	    @Override
	    public Object getRoot() {
	        return root;
	    }

	    @Override
	    public boolean isLeaf(Object node) {
	        File f = (File) node;
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
