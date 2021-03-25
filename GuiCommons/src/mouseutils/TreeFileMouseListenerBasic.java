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
package mouseutils;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import dialogutils.JOptionHelperExtended;
import frameutils.frame.FilePropertiesFrame;
import frameutils.utils.GuiUtilsExtended;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.MenuSetter;

/**
 * This represents a basic extensible component, when each node is a File Object
 * @author Alessio Moraschini
 *
 */
public class TreeFileMouseListenerBasic {
	
	// KeyGenPanel logger
	static Logger logger = Logger.getLogger(TreeFileMouseListenerBasic.class); 
	
	// gui elements
	public JTree tree;
	public File selectedNode;
	public JFrame propertiesFrame = null; 
	public Component parent;

	public TreeFileMouseListenerBasic() {
		
	}
	// constructor
	public TreeFileMouseListenerBasic(Component parent) {
		this.parent = parent;
	}
	
	public TreeFileMouseListenerBasic(JTree treeIn) {
		tree = treeIn;
		tree.setComponentPopupMenu(getPopUpMenu());
		tree.addMouseListener(getMouseListener());
	}
	
	public MouseListener getMouseListener() {
	    return new MouseAdapter() {

	        @Override
	        public void mousePressed(MouseEvent arg0) {
	            if(arg0.getButton() == MouseEvent.BUTTON3){
	            
		            TreePath pathForLocation = tree.getPathForLocation(arg0.getPoint().x, arg0.getPoint().y);
		            if(pathForLocation != null){
		            	selectedNode = (File) pathForLocation.getLastPathComponent();
		            } else{
		            	selectedNode = null;
		            }
		           
		            super.mousePressed(arg0);
	            }
	        }
	    };
	}

	public JPopupMenu getPopUpMenu() {
	    JPopupMenu menu = new JPopupMenu();
	    
	    JMenuItem sysExplorerItem = new JMenuItem("View in system explorer");
	    sysExplorerItem.setFont(GeneralConfig.DEFAULT_POP_UP_MENUS_FONT);
	    sysExplorerItem.addActionListener(openInSystemExplorer());
	    menu.add(sysExplorerItem);
	    
	    JMenuItem deleteItem = new JMenuItem("Delete selected Configuration files");
	    deleteItem.setFont(GeneralConfig.DEFAULT_POP_UP_MENUS_FONT);
	    deleteItem.addActionListener(getDeleteActionListener());
	    menu.add(deleteItem);
	    
	    JMenuItem refreshItem = new JMenuItem("Refresh tree");
	    refreshItem.setFont(GeneralConfig.DEFAULT_POP_UP_MENUS_FONT);
	    refreshItem.addActionListener(refreshListener());
	    menu.add(refreshItem);
	    
	    JMenuItem propertiesItem = new JMenuItem("File Properties");
	    propertiesItem.setFont(GeneralConfig.DEFAULT_POP_UP_MENUS_FONT);
	    propertiesItem.addActionListener(propertiesListener());
	    menu.add(propertiesItem);

	    // set menu icons
	    MenuSetter.setIconTextGap(menu, 12);
	    
	    sysExplorerItem.setIcon(new ImageIcon(IconsPathConfigurator.iconFolderPath));
	    deleteItem.setIcon(new ImageIcon(IconsPathConfigurator.ICON_DELETE));
	    refreshItem.setIcon(new ImageIcon(IconsPathConfigurator.ICON_RELOAD_CONF));
	    propertiesItem.setIcon(new ImageIcon(IconsPathConfigurator.ICON_ABOUT));
	    
	    return menu;
	}
	
	public ActionListener openInSystemExplorer() {
		return new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				GuiUtilsExtended.openInFileSystem(selectedNode);
			}
		};
	}
	

	public ActionListener getDeleteActionListener() {
	    return new ActionListener() {

	        @Override
	        public void actionPerformed(ActionEvent arg0) {

	            TreePath[] treeSelArr = tree.getSelectionPaths();
            	if(treeSelArr.length!= 0 && new JOptionHelperExtended(null).yesOrNo("Are you sure to delete?", "Delete?")){	        		
		            for(TreePath tp : treeSelArr) {
		            	
	            		String path = tp.getLastPathComponent().toString();
		            	File selectedFile = new File(path);
		            	if(selectedFile.isFile()) {	
		            		logger.debug(path+" Deleted: "+selectedFile.delete());	
		            	}else {
		            		logger.info(path + " Not deleted beacuse it's in usage");
		            	}
			        }

	            }else {
	            	String msg = "Skipped after user request!";
			    	logger.debug(msg);
	            }
            	
            	refreshTree();
	        }
	    };
	}
	
	public ActionListener refreshListener() {
		return new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshTree();
			}
		};
	}
	
	public ActionListener propertiesListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (propertiesFrame == null || !propertiesFrame.isVisible()) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							try {
								if (selectedNode != null && selectedNode.exists()) {
									propertiesFrame = new FilePropertiesFrame(selectedNode, true);
									propertiesFrame.setLocationRelativeTo(parent);
									propertiesFrame.setTitle(selectedNode.getAbsolutePath());
									propertiesFrame.setVisible(true);
								}
							} catch (Exception e) {
								logger.error("Error in file properties panel: ", e);
							}
						}
					});
				} else {
					propertiesFrame.toFront();
					propertiesFrame.setVisible(true);
				} 
			}
		};
	}
	
	
	// utility methods //
	
	public void refreshTree() {
		tree.setModel(tree.getModel());
		tree.revalidate();
		tree.repaint();
	}
	
}
