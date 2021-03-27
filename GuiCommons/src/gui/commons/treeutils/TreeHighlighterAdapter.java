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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

public class TreeHighlighterAdapter extends MouseMotionAdapter implements MouseListener{
	TreePath lastSelected;
	TreePath originalSelection;
	
	public TreeHighlighterAdapter() {
	}
	
    public void mouseMoved(MouseEvent e)
    {
    	super.mouseMoved(e);
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }

	@Override
	public void mouseEntered(MouseEvent e) {
		try {
			JTree tree = (JTree) e.getSource();
			TreePath treeSel = tree.getSelectionPath();
			if (treeSel != null) {
				originalSelection = treeSel;
			}
		} catch (Exception e1) {
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
}
