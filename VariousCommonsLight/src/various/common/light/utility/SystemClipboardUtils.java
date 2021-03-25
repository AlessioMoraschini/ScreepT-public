package various.common.light.utility;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.string.StringWorker.EOL;

public class SystemClipboardUtils {
	
	private static SafeLogger logger = new SafeLogger(SystemClipboardUtils.class);
	
	@SuppressWarnings("unchecked")
	public static List<String> readAsFileList(Clipboard clipboard) {
		clipboard = clipboard == null ? Toolkit.getDefaultToolkit().getSystemClipboard() : clipboard;
	    try {
	    	return (List<String>)clipboard.getData(DataFlavor.javaFileListFlavor);
	    } catch(Exception e) {
	    	logger.error("", e);
	        return new ArrayList<String>();
	    }
	}
	
	@SuppressWarnings("unchecked")
	public static List<File> getClipboardFiles(Clipboard clipboard) {
		clipboard = clipboard == null ? Toolkit.getDefaultToolkit().getSystemClipboard() : clipboard;
	    try {
	    	return (List<File>)clipboard.getData(DataFlavor.javaFileListFlavor);
	    } catch(Exception e) {
	    	logger.error("", e);
	        return new ArrayList<File>();
	    }
	}
	
	public static boolean pasteFilesFromClipboard(File destination, Clipboard clipboard, JOptionHelper dialogHelper) throws IOException {
		List<File> clipboardFiles = getClipboardFiles(clipboard);
		if(!clipboardFiles.isEmpty() && dialogHelper.yesOrNo("Paste " + clipboardFiles.size() + " files into " + destination.getName() + " ?")) {
			// TODO subfolders
			for(File file : clipboardFiles) {
				boolean copy = true;
				if(destination.listFiles((dir, name) -> {
					boolean sameType = (dir.isFile() && file.isFile()) || (dir.isDirectory() && file.isDirectory());
					return sameType && file.getName().equals(name);
				}).length > 0) {
					copy = dialogHelper.yesOrNo(destination + " already contains " + file.getName() + ", are you sure to overwrite?", "Overwrite?");
				}
				if (copy) {
					FileUtils.copyToDirectory(file, destination);
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	public static void setClipobardFiles(final List<File> filesToAdd) {
		
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
			
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return DataFlavor.javaFileListFlavor.equals(flavor);
			}
			
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] {DataFlavor.javaFileListFlavor};
			}
			
			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				return filesToAdd;
			}
		}, new ClipboardOwner() {
			
			@Override
			public void lostOwnership(Clipboard clipboard, Transferable contents) {
				logger.debug("Clipboard paste manager owns no more loaded files.");
			}
		});
	}
	
	public static void setClipobardString(final List<File> filesToAdd, EOL eol) {
		StringSelection ss = new StringSelection(StringWorker.concatFilePaths(filesToAdd, eol));
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
	}

	public static void setClipobardString(String string) {
		StringSelection ss = new StringSelection(string);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
	}

	public static String readAsString(Clipboard clipboard) {
		clipboard = clipboard == null ? Toolkit.getDefaultToolkit().getSystemClipboard() : clipboard;
	    try {
	        return (String)clipboard.getData(DataFlavor.stringFlavor);
	    } catch(Exception e) {
	    	logger.error("", e);
	        return null;
	    }
	}

}
