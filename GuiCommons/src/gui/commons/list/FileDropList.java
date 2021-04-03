package gui.commons.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileSystemView;

import net.miginfocom.swing.MigLayout;
import various.common.light.gui.dnd.FileDropListener;
import various.common.light.gui.dnd.FileDropListenerSingle;
import various.common.light.utility.log.SafeLogger;

public class FileDropList extends JPanel implements DropTargetListener {
	private static final long serialVersionUID = -2124235652189325306L;

	private static final SafeLogger logger = new SafeLogger(FileDropList.class);

	public static long DEF_fileViewLimit = 60L;

	private DefaultListModel<File> listModel = new DefaultListModel<>();
    @SuppressWarnings("unused")
	private DropTarget dropTarget;
    private JScrollPane jScrollPane1;
    private JList<File> list;
    private boolean dropEnabled;
    private FileDropListener fileDropListener;
    private FileDropListenerSingle listDoubleClickAction;
    private FileDropListenerSingle listSingleClickAction;

    private JPopupMenu popUpMenu;

    private long fileViewLimit;
    /**
     * Create the panel.
     */
    public FileDropList() {

    	fileViewLimit = DEF_fileViewLimit;

    	setLayout(new MigLayout(
				"insets 2, hidemode 2",
				"[60px, grow]",
				"[4px:4px:4px][20px, grow]"));

    	setFileDropListener(null);
    	setListDoubleClickAction(null);
    	setListSingleClickAction(null);
    	dropEnabled = true;
        list = new JList<File>();
        dropTarget = new DropTarget(list, this);
        list.setModel(listModel);
        list.setDragEnabled(dropEnabled);
        FileListCellRenderer renderer = new FileListCellRenderer();
        list.setCellRenderer(renderer);
        //list.setTransferHandler(new FileTransferHandler());
        jScrollPane1 = new JScrollPane(list);

        //jScrollPane1.setBounds(10, 150, 635, 330);
        add(jScrollPane1, "cell 0 1 1 1,grow");

        list.addMouseListener(new MouseAdapter() {
            @Override
			public void mouseClicked(MouseEvent evt) {

            	try {
					int index = list.locationToIndex(evt.getPoint());
					File clicked = listModel.get(index);

					if(SwingUtilities.isRightMouseButton(evt) && popUpMenu != null && !popUpMenu.isVisible()) {
						popUpMenu.setVisible(true);


					} else if (evt.getClickCount() == 1) {

						listSingleClickAction.fileDropped(clicked);

					} else if (evt.getClickCount() == 2) {

					    listDoubleClickAction.fileDropped(clicked);
					}
				} catch (Exception e) {
					logger.error("", e);
				}
            }
        });

        list.setFocusable(true);
        list.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				int index = list.getSelectedIndex();
				File clicked = null;
				if(index >= 0)
					clicked = listModel.get(index);
				else
					return;

				if(KeyEvent.VK_UP == e.getKeyCode() || KeyEvent.VK_DOWN == e.getKeyCode()) {
					listSingleClickAction.fileDropped(clicked);

				} else if(KeyEvent.VK_ENTER == e.getKeyCode()) {
					listDoubleClickAction.fileDropped(clicked);
				}
			}
		});

        applyDarkColor();
    }

    public JPopupMenu getPopUpMenu() {
		return popUpMenu;
	}

	public void setPopUpMenu(JPopupMenu popUpMenu) {
		this.popUpMenu = popUpMenu;
		this.popUpMenu.addMouseMotionListener(new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				super.mouseExited(e);
				if(popUpMenu != null)
					popUpMenu.setVisible(false);
			}
		});
	}

	public long getFileViewLimit() {
		return fileViewLimit;
	}
	public void setFileViewLimit(long fileViewLimit) {
		this.fileViewLimit = fileViewLimit;
	}

	public void applyDarkColor() {
    	list.setBackground(Color.DARK_GRAY);
    	this.setBackground(Color.DARK_GRAY);
    	list.setForeground(Color.LIGHT_GRAY);
    	this.setForeground(Color.LIGHT_GRAY);
    }

    // Sample "how to use"
    public static void main(String[] args) throws Exception {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                JPanel pan = new FileDropList();
                pan.setBorder(new LineBorder(Color.BLACK));
                JOptionPane.showMessageDialog(null, pan);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
	public void dragEnter(DropTargetDragEvent arg0) {
        // nothing
    }

    @Override
	public void dragOver(DropTargetDragEvent arg0) {
        // nothing
    }

    @Override
	public void dropActionChanged(DropTargetDragEvent arg0) {
        // nothing
    }

    @Override
	public void dragExit(DropTargetEvent arg0) {
        // nothing
    }

    @Override
	@SuppressWarnings("unchecked")
	public void drop(DropTargetDropEvent evt) {

    	if(!dropEnabled)
    		return;

        int action = evt.getDropAction();
        evt.acceptDrop(action);
        try {
            Transferable data = evt.getTransferable();
            if (data.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> files = (List<File>) data.getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : files) {
                    listModel.addElement(file);
                }

                fileDropListener.filesDropped(files.toArray(new File[files.size()]));
            }
        } catch (UnsupportedFlavorException e) {
        	logger.error("Unsupported flavour while dropping files into list", e);
        } catch (IOException e) {
        	logger.error("IO Exception happened while dropping files into list!", e);
        } finally {
            evt.dropComplete(true);
        }
    }

    public void setFiles(File[] files) {
    	clearList();
    	int i = 0;
    	if(files != null)
    		for(File file : files) {
    			if(i > fileViewLimit)
    				break;

    			listModel.addElement(file);
    			i++;
    		}
    }

    public void setFiles(List<File> files) {
    	clearList();
    	int i = 0;
    	if(files != null)
    		for(File file : files) {
    			if(i > fileViewLimit)
    				break;

    			listModel.addElement(file);
    			i++;
    		}
    }

    public void addFiles(File[] files) {
    	if(files != null)
    		for(File file : files)
    			listModel.addElement(file);
    }
    public void addFile(File file) {
    	if(file != null)
    		listModel.addElement(file);
    }

    public void removeFiles(File[] files) {
    	if(files != null)
    		for(File file : files)
    			listModel.removeElement(file);
    }
    public void removeFile(File file) {
    	if(file != null)
    		listModel.removeElement(file);
    }

    public void clearList() {
    	listModel.removeAllElements();
    }

	public boolean isDropEnabled() {
		return dropEnabled;
	}
	public void setDropEnabled(boolean dropEnabled) {
		this.dropEnabled = dropEnabled;
	}

	public JList<File> getList() {
		return list;
	}

	public FileDropListener getFileDropListener() {
		return fileDropListener;
	}

	public void setFileDropListener(FileDropListener fileDropListener) {
		this.fileDropListener = fileDropListener == null ? (files) -> {} : fileDropListener;
	}

	public FileDropListenerSingle getListDoubleClickAction() {
		return listDoubleClickAction;
	}

	public void setListDoubleClickAction(FileDropListenerSingle listDoubleClickAction) {
		this.listDoubleClickAction = listDoubleClickAction == null ? (file) -> {} : listDoubleClickAction;;
	}

	public FileDropListenerSingle getListSingleClickAction() {
		return listSingleClickAction;
	}

	public void setListSingleClickAction(FileDropListenerSingle listSingleClickAction) {
		this.listSingleClickAction = listSingleClickAction == null ? (file) -> {} : listSingleClickAction;;
	}
}

class FileListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = -7799441088157759804L;
    private FileSystemView fileSystemView;
    private JLabel label;
    private Color textSelectionColor = Color.WHITE;
    private Color backgroundSelectionColor = Color.DARK_GRAY.brighter();
    private Color textNonSelectionColor = Color.WHITE.darker();
    private Color backgroundNonSelectionColor = Color.DARK_GRAY;

    FileListCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    @SuppressWarnings("rawtypes")
	@Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean selected,
            boolean expanded) {

        File file = (File)value;
        label.setIcon(fileSystemView.getSystemIcon(file));
        label.setText(fileSystemView.getSystemDisplayName(file));
        label.setToolTipText(file.getPath());

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}
