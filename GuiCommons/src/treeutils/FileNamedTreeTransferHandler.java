package treeutils;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import various.common.light.files.om.FileNamed;
import various.common.light.gui.dialogs.msg.JOptionHelper;

public class FileNamedTreeTransferHandler extends TransferHandler {

    private static final long serialVersionUID = 1L;
    
    public JOptionHelper optionHelper;
    public Runnable postMoveAction;
    
    public FileNamedTreeTransferHandler(JOptionHelper optionHelper, Runnable postMoveAction) {
		this.optionHelper = optionHelper;
		this.postMoveAction = postMoveAction != null ? postMoveAction : () -> {};
	}

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree list = (JTree) c;
        List<File> files = new ArrayList<File>();
        
        
        for (TreePath path : list.getSelectionPaths()) {
        	
        	FileNamed node = (FileNamed)path.getLastPathComponent();
			 if (node != null) {
				try {
					files.add(node.mFile.getCanonicalFile());
				} catch (IOException e) {
					System.err.println("Cannot retrieve file " + node);
					e.printStackTrace();
				}
			}
        }
        
        return new FileTransferable(files) {
			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				SwingUtilities.invokeLater(postMoveAction);
				return super.getTransferData(flavor);
			}
        	
        };
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }
    
    @Override
	public boolean importData(TransferSupport support) {
    	if(super.importData(support)) {
			postMoveAction.run();			
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean importData(JComponent comp, Transferable t) {
		if(super.importData(comp, t)) {
			postMoveAction.run();			
			return true;
		} else {
			return false;
		}
	}


	// NESTED CLASS FILE TRANSFERABLE
    public class FileTransferable implements Transferable {
    	
    	private List<File> files;
    	
    	public FileTransferable(List<File> files) {
    		this.files = files;
    	}
    	
    	public DataFlavor[] getTransferDataFlavors() {
    		return new DataFlavor[] { DataFlavor.javaFileListFlavor };
    	}
    	
    	public boolean isDataFlavorSupported(DataFlavor flavor) {
    		return flavor.equals(DataFlavor.javaFileListFlavor);
    	}
    	
    	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    		if (!isDataFlavorSupported(flavor)) {
    			throw new UnsupportedFlavorException(flavor);
    		}
    		return files;
    	}
    }
}
