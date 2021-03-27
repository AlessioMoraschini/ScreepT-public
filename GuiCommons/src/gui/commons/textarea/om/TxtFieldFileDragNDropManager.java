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
	package gui.commons.textarea.om;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

public class TxtFieldFileDragNDropManager extends JTextField{
	private static final long serialVersionUID = 6345611258321094551L;

	// KeyGenPanel logger
	static Logger logger = Logger.getLogger(TxtFieldFileDragNDropManager.class); 
    
	public ArrayList<File> loadedFilesList;
	
	private DropTarget dropTarget;
    private DropTargetHandler dropTargetHandler;
    public Runnable dropRunner;
    public Component targetDropComponent;
    public boolean allowDirectoriesDrop = false;

    private boolean dragOver = false;
    @SuppressWarnings("unused")
	private Point dragPoint;

    // constructor
    public TxtFieldFileDragNDropManager(Runnable dropAction, Component targetDrop) {
    	
    	loadedFilesList = new ArrayList<File>();
    	dropRunner = dropAction;
    	targetDropComponent = (targetDrop == null)? this : targetDrop;
    	
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 400);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        try {
            getMyDropTarget().addDropTargetListener(getDropTargetHandler());
        } catch (TooManyListenersException ex) {
            logger.error(ex);
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        getMyDropTarget().removeDropTargetListener(getDropTargetHandler());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dragOver) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(new Color(61, 111, 191,80));
            g2d.fill(new Rectangle(getWidth(), getHeight()));
            g2d.dispose();
            
        }
    }

    protected DropTarget getMyDropTarget() {
        if (dropTarget == null) {
            dropTarget = new DropTarget(targetDropComponent, DnDConstants.ACTION_COPY_OR_MOVE, null);
        }
        return dropTarget;
    }

    protected DropTargetHandler getDropTargetHandler() {
        if (dropTargetHandler == null) {
            dropTargetHandler = new DropTargetHandler();
        }
        return dropTargetHandler;
    }

    // here there is action to update given JTree
    protected void importFiles(final List<File> files) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
            	try {
					for (File file : files) {
						if (file.exists()) {
							if (file.isFile()) {
								// Insert new nodes as children of root node
								loadedFilesList.add(file);
							} else if (allowDirectoriesDrop) {
								// directory actions
								loadedFilesList.add(file);
							} 
						}
					}
					logger.debug(files.size() + " files selected");
				} catch (Exception e) {
					logger.error(e);
				}
            }
        };
        SwingUtilities.invokeLater(run);
        
        // start callback on drop
        SwingUtilities.invokeLater(dropRunner);
    }
        
    protected class DropTargetHandler implements DropTargetListener {

        protected void processDrag(DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
                dtde.rejectDrag();
            }
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            processDrag(dtde);
            SwingUtilities.invokeLater(new DragUpdate(true, dtde.getLocation()));
            
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            processDrag(dtde);
            SwingUtilities.invokeLater(new DragUpdate(true, dtde.getLocation()));
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
        }

        @Override
        public void dragExit(DropTargetEvent dte) {
            SwingUtilities.invokeLater(new DragUpdate(false, null));
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {

            SwingUtilities.invokeLater(new DragUpdate(false, null));

            Transferable transferable = dtde.getTransferable();
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(dtde.getDropAction());
                try {

                    @SuppressWarnings("unchecked")
					List<File> transferData = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    if (transferData != null && transferData.size() > 0) {
                        importFiles(transferData);
                        dtde.dropComplete(true);
                    }

                } catch (Exception ex) {
                    logger.error(ex);
                }
            } else {
                dtde.rejectDrop();
            }
        }
    }

    public class DragUpdate implements Runnable {

        private boolean dragOver;
        private Point dragPoint;

        public DragUpdate(boolean dragOver, Point dragPoint) {
            this.dragOver = dragOver;
            this.dragPoint = dragPoint;
        }

        @Override
        public void run() {
            TxtFieldFileDragNDropManager.this.dragOver = dragOver;
            TxtFieldFileDragNDropManager.this.dragPoint = dragPoint;
            TxtFieldFileDragNDropManager.this.repaint();
        }
    }
    
    public void emptyLoadedList() {
    	this.loadedFilesList.clear();
    }

	public ArrayList<File> getLoadedFilesList() {
		return loadedFilesList;
	}

	public void setLoadedFilesList(ArrayList<File> loadedFilesList) {
		this.loadedFilesList = loadedFilesList;
	}

	public Runnable getDropRunner() {
		return dropRunner;
	}

	public void setDropRunner(Runnable dropRunner) {
		this.dropRunner = dropRunner;
	}

	public boolean isAllowDirectoriesDrop() {
		return allowDirectoriesDrop;
	}

	public void setAllowDirectoriesDrop(boolean allowDirectoriesDrop) {
		this.allowDirectoriesDrop = allowDirectoriesDrop;
	}
	
}
