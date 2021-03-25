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
package dialogutils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.HeadlessException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import files.FileVarious;
import general.MyFileView;
import impl.INItializer;
import resources.GeneralConfig;

public class GenericFileChooserDialog {

	public static String writerNames[] = ImageIO.getWriterFileSuffixes();
	public static MyFileView fileView = new MyFileView();
	
	public static JOptionHelperExtended dialogHelper = new JOptionHelperExtended(null);
	
	public INItializer currentConf;
	public FileFilter fileFilter;
	
	public File defaultDirectory;
	
	public static AtomicBoolean blockEventsModal = new AtomicBoolean();
	
	/**
	 * Use null currentconfig if you don't need to automatically update last dst/src paths
	 * @param currentConf
	 */
	public GenericFileChooserDialog(INItializer currentConf) {
		this.currentConf = (currentConf == null)? new INItializer() : currentConf;
		this.fileFilter = null;
	}

	/**
	 * Use null currentconfig if you don't need to automatically update last dst/src paths
	 * @param currentConf
	 */
	public GenericFileChooserDialog(INItializer currentConf, String typeDescription, String... fileFilter) {
		this.currentConf = (currentConf == null)? new INItializer() : currentConf;
		this.fileFilter = new FileNameExtensionFilter(typeDescription, fileFilter);
	}
	
	private void updateDefaultDir(File dir, JFileChooser chooser) {
		if (dir != null && dir.exists()) {
			chooser.setCurrentDirectory(dir);
		}
	}
	
	/**JFRAME - only img supported extension
	 * this method select a file from the user (only supported images files),if nothing is selected returns null
	 */
	public File fileImgRead(JFrame parent) {
		
		blockEventsModal.set(true);
		
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 

		JFileChooser fileChooser = new CustomFileChooserOnTop();	
		updateFileFilters(fileChooser, fileFilter);
		fileChooser.setDialogTitle("Select input Image File");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));	
		fileChooser.setFileView(fileView);
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		File correctFile = null;
		
		int returnVal = fileChooser.showOpenDialog(parent);
		File selectedFile = fileChooser.getSelectedFile();
        if (returnVal == JFileChooser.APPROVE_OPTION && selectedFile!=null) {
            
        	String givenExtension = FilenameUtils.getExtension(selectedFile.getName());
        	boolean jpeg = givenExtension.toLowerCase().contains("jpeg");
        	boolean jpg = givenExtension.toLowerCase().contains("jpg");
        	boolean png = givenExtension.toLowerCase().contains("png");
        	boolean others = Arrays.asList(writerNames).contains(givenExtension);
        	if(!jpeg && !jpg && !png && !others){
        		dialogHelper.error("Please select a valid file between "+Arrays.toString(writerNames), "Wrong File format");
        	}else {
        		correctFile = selectedFile;
        		currentConf.getFileOpt().setLastSrcFolderPath(correctFile.getAbsolutePath());
        	}
        }
        
        blockEventsModal.set(false);
        resetLaf(oldLaf);
        
        return correctFile;
	}
	
	/**JFRAME - only img supported extension
	 * this method select a file from the user (only supported images files),if nothing is selected returns null
	 * @throws UnsupportedDataTypeException if image type is not supported 
	 */
	public File fileImgRead(JPanel parent, String ext) throws Exception {
		
		blockEventsModal.set(true);
		
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 

		JFileChooser fileChooser = new CustomFileChooserOnTop();
		fileChooser.setFileFilter(getImageFileFilter(ext));
		fileChooser.setDialogTitle("Select input Image File");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileView(fileView);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));	
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		fileChooser.setFileFilter(new FileFilter() {
			
			@Override
			public String getDescription() {
				return "Only images of "+ext+" format";
			}
			
			@Override
			public boolean accept(File file) {
				if(file.getName().toLowerCase().endsWith(ext) || file.isDirectory()) {
					return true;
				}else return false;
			}
		});
		File correctFile = null;		
		
		int returnVal = fileChooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            
        	File selectedFile = fileChooser.getSelectedFile();
        	String givenExtension = FilenameUtils.getExtension(selectedFile.getName());
        	if(selectedFile==null || !ext.equalsIgnoreCase(givenExtension)) {
        		throw new Exception("File extension not supported! Please select a valid file between "+Arrays.toString(writerNames));
        	}else {
        		correctFile = selectedFile;
        		currentConf.getFileOpt().setLastSrcFolderPath(correctFile.getAbsolutePath());
        	}
        }
        
        blockEventsModal.set(false);
        resetLaf(oldLaf);
        
        return correctFile;
	}
	
	/**
	 * JPANEL - every extension
	 * this method select a source file from the user,if nothing is selected returns null
	 */
	public File fileRead(Component parent) {
		
        return fileRead(parent, fileFilter);
	}
	
	/**
	 * JPANEL - every extension
	 * this method select a source file from the user,if nothing is selected returns null
	 */
	public File fileRead (Container parent, FileFilter fileFilter) {
		
		blockEventsModal.set(true);
		LookAndFeel oldLaf = backupLafNDefaultSystem();
		
		
		JFileChooser fileChooser = getFileChooser(fileFilter);
		int returnVal = fileChooser.showOpenDialog(parent);
		File correctFile = checkSelectedFile(returnVal, fileChooser);
		
		resetLaf(oldLaf);
		blockEventsModal.set(false);
		
		return correctFile;
	}

	/**
	 * JPANEL - every extension
	 * this method select a source file from the user,if nothing is selected returns null
	 */
	public File fileRead(Component parent, FileFilter fileFilter) {
		
		blockEventsModal.set(true);
		LookAndFeel oldLaf = backupLafNDefaultSystem();
		
		
		JFileChooser fileChooser = getFileChooser(fileFilter);
		int returnVal = fileChooser.showOpenDialog(parent);
		File correctFile = checkSelectedFile(returnVal, fileChooser);
		
		resetLaf(oldLaf);
		blockEventsModal.set(false);
		
		return correctFile;
	}
	
	private File checkSelectedFile(int returnVal, JFileChooser fileChooser) {
		File correctFile = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			
			File selectedFile = fileChooser.getSelectedFile();
			if(selectedFile==null) {
				dialogHelper.error("Please select a file!", "File not selected");
			}else {
				correctFile = selectedFile;
				currentConf.getFileOpt().setLastSrcFolderPath(correctFile.getAbsolutePath());
			}
		}
		
		return correctFile;
	}
	
	private JFileChooser getFileChooser(FileFilter fileFilter) {
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();	
		updateFileFilters(fileChooser, fileFilter);
		fileChooser.setDialogTitle("Select input File");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));	
		fileChooser.setFileView(fileView);
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		
		return fileChooser;
	}

	/**
	 * JPANEL - every extension
	 * this method select a source file from the user,if nothing is selected returns null
	 */
	public File fileRead(Component parent, ExtensionFileFilter[] filters) {
		
		blockEventsModal.set(true);
		
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		addMultipleFileFilter(fileChooser, filters);
		fileChooser.setDialogTitle("Select input File");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setFileView(fileView);
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		File correctFile = null;
		
		int returnVal = fileChooser.showOpenDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			
			File selectedFile = fileChooser.getSelectedFile();
//        	String givenExtension = FilenameUtils.getExtension(selectedFile.getName());
			if(selectedFile==null) {
				dialogHelper.error("Please select a file!", "File not selected");
			}else {
				correctFile = selectedFile;
				currentConf.getFileOpt().setLastSrcFolderPath(correctFile.getAbsolutePath());
			}
		}
		
		resetLaf(oldLaf);
		
		blockEventsModal.set(false);
		
		return correctFile;
	}
	
	/**
	 * JCOMPONENT - every extension and every typer of source
	 * this method select a source file from the user,if nothing is selected returns null
	 */
	public File fileAndDirRead(JComponent parent) {
		
		blockEventsModal.set(true);
		
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		updateFileFilters(fileChooser, fileFilter);
		fileChooser.setDialogTitle("Select input File or Directory");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setFileView(fileView);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		
		File correctFile = null;
		
		int returnVal = fileChooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            
        	File selectedFile = fileChooser.getSelectedFile();
//        	String givenExtension = FilenameUtils.getExtension(selectedFile.getName());
        	if(selectedFile==null) {
        		dialogHelper.error("Please select a file!", "File not selected");
        	}else {
        		correctFile = selectedFile;
        		currentConf.getFileOpt().setLastSrcFolderPath(correctFile.getParentFile().getAbsolutePath());
        	}
        }
        
        blockEventsModal.set(false);
        resetLaf(oldLaf);
        
        return correctFile;
	}

	/**
	 * JCOMPONENT - every extension and every typer of source
	 * this method select a source file from the user,if nothing is selected returns null
	 */
	public File fileAndDirRead(JFrame parent) {
		
		blockEventsModal.set(true);
		
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		updateFileFilters(fileChooser, fileFilter);
		fileChooser.setDialogTitle("Select input File or Directory");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setFileView(fileView);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		
		File correctFile = null;
		
		int returnVal = fileChooser.showOpenDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			
			File selectedFile = fileChooser.getSelectedFile();
//        	String givenExtension = FilenameUtils.getExtension(selectedFile.getName());
			if(selectedFile==null) {
				dialogHelper.error("Please select a file!", "File not selected");
			}else {
				correctFile = selectedFile;
				currentConf.getFileOpt().setLastSrcFolderPath(correctFile.getParentFile().getAbsolutePath());
			}
		}
		
		blockEventsModal.set(false);
		resetLaf(oldLaf);
		
		return correctFile;
	}

	/**
	 * JCOMPONENT - every extension and every typer of source
	 * this method select a source file from the user,if nothing is selected returns null
	 */
	public List<File> fileAndDirReadMulti(Component parent) {
		
		blockEventsModal.set(true);
		
		List<File> results = new ArrayList<>();
		
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		updateFileFilters(fileChooser, fileFilter);
		updateDefaultDir(defaultDirectory, fileChooser);

		fileChooser.setDialogTitle("Select input File or Directory");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setFileView(fileView);
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		
		int returnVal = fileChooser.showOpenDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			
			results = Arrays.asList(fileChooser.getSelectedFiles());
			if(results == null) {
				dialogHelper.error("Please select a file!", "File not selected");
				results = new ArrayList<>();
			}
		}
		
		blockEventsModal.set(false);
		resetLaf(oldLaf);
		
		return results;
	}
	
	/**JFRAME - only img supported extension
	 * this method select a file from the user (only supported Image files),if nothing is selected returns null
	 */
	public File fileImgWrite(JFrame parent, String defaultName) {
		
		blockEventsModal.set(true);
		
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 

		JFileChooser fileChooser = new CustomFileChooserOnTop();
		updateFileFilters(fileChooser, fileFilter);
		fileChooser.setDialogTitle("Save Image File");
		fileChooser.setFileView(fileView);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastDstFolderPath()));	
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setSelectedFile(new File(defaultName));
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		File correctFile = null;
		
		int returnVal = fileChooser.showSaveDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            
        	File selectedFile = fileChooser.getSelectedFile();
        	String givenExtension = FilenameUtils.getExtension(selectedFile.getName());
        	if(selectedFile==null||!Arrays.asList(writerNames).contains(givenExtension)) {
        		dialogHelper.error("Please select a valid file between "+Arrays.toString(writerNames), "Wrong File format");
        	}else {
        		correctFile = selectedFile;
        		currentConf.getFileOpt().setLastDstFolderPath(correctFile.getParentFile().getAbsolutePath());
        	}
        }else {
        	dialogHelper.info("Skipped after user request.", "Skipped");
        }
        
        blockEventsModal.set(false);
        resetLaf(oldLaf);
        
        return correctFile;
	}
	
	/**JFRAME - only img supported extension
	 * this method select a file from the user (only supported Image files),if nothing is selected returns null
	 */
	public File fileWrite(JFrame parent, String defaultName) {
		
		blockEventsModal.set(true);

		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		updateFileFilters(fileChooser, fileFilter);
		fileChooser.setDialogTitle("Save File");
		fileChooser.setFileView(fileView);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastDstFolderPath()));	
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setSelectedFile(new File(defaultName));
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		File correctFile = null;
		
		int returnVal = fileChooser.showSaveDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            
        	File selectedFile = fileChooser.getSelectedFile();
    		correctFile = selectedFile;
    		currentConf.getFileOpt().setLastDstFolderPath(correctFile.getParentFile().getAbsolutePath());
        }else {
        	dialogHelper.info("Skipped after user request.", "Skipped");
        }
        
        blockEventsModal.set(false);
        resetLaf(oldLaf);
        
        return correctFile;
	}

	/**JFRAME - only img supported extension
	 * this method select a file from the user (only supported Image files),if nothing is selected returns null
	 */
	public File fileWrite(JFrame parent, String defaultName, File defaultDir, ExtensionFileFilter[] filters) {
		
		blockEventsModal.set(true);
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		addMultipleFileFilter(fileChooser, filters);
		fileChooser.setDialogTitle("Save File");
		fileChooser.setFileView(fileView);
		fileChooser.setCurrentDirectory(defaultDir != null ? defaultDir : new File(currentConf.getFileOpt().getLastDstFolderPath()));	
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setSelectedFile(new File(defaultName));
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		File correctFile = null;
		
		int returnVal = fileChooser.showSaveDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			
			correctFile = fileChooser.getSelectedFile();
			String selectedExtension = getSafeFilterExtension(fileChooser.getFileFilter());
			if (!selectedExtension.isEmpty()) {
				correctFile = FileVarious.changeExtensionForceDot(correctFile, selectedExtension);
			}
			
			currentConf.getFileOpt().setLastDstFolderPath(correctFile.getParentFile().getAbsolutePath());
		}else {
			dialogHelper.info("Skipped after user request.", "Skipped");
		}
		
		blockEventsModal.set(false);
		resetLaf(oldLaf);
		
		return correctFile;
	}
	
	/**JFRAME - only directories src
	 * this method select a directory from the user (only directories),if nothing is selected returns null
	 */
	public File directoRead(JFrame parent) {
		
		blockEventsModal.set(true);
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		updateFileFilters(fileChooser, null);
		fileChooser.setDialogTitle("Choose Input Directory");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));		
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setFileView(fileView);
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		File correctFile = null;
		
		int returnVal = fileChooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            
        	File selectedFile = fileChooser.getSelectedFile();
        	if(selectedFile==null||!selectedFile.isDirectory()) {
        		dialogHelper.error("Please select a directory!", "Select a directory");
        	}else {
        		correctFile = selectedFile;
        		currentConf.getFileOpt().setLastSrcFolderPath(correctFile.getParentFile().getAbsolutePath());
        	}
        }
        
        blockEventsModal.set(false);
        resetLaf(oldLaf);
        
        return correctFile;
	}

	/**JFRAME - only directories src
	 * this method select a directory from the user (only directories),if nothing is selected returns null
	 */
	public File directoRead(JFrame parent, String title) {
		
		blockEventsModal.set(true);
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		updateFileFilters(fileChooser, null);
		fileChooser.setDialogTitle(title);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastSrcFolderPath()));		
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setFileView(fileView);
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		File correctFile = null;
		
		int returnVal = fileChooser.showOpenDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			
			File selectedFile = fileChooser.getSelectedFile();
			if(selectedFile==null||!selectedFile.isDirectory()) {
				dialogHelper.error("Select a directory!", "Select a directory");
			}else {
				correctFile = selectedFile;
				currentConf.getFileOpt().setLastSrcFolderPath(correctFile.getParentFile().getAbsolutePath());
			}
		}
		
		blockEventsModal.set(false);
		resetLaf(oldLaf);
		
		return correctFile;
	}
	
	/**JFRAME - only directories dest
	 * this method select a directory from the user (only directories)to write a copy of current
	 * folder, if nothing is selected returns null
	 */
	public File directoWrite(JFrame parent) {
		
		blockEventsModal.set(true);
		LookAndFeel oldLaf = backupLafNDefaultSystem(); 
		
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		updateFileFilters(fileChooser, null);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle("Choose output Directory");
		fileChooser.setCurrentDirectory(new File(currentConf.getFileOpt().getLastDstFolderPath()));	
		updateDefaultDir(defaultDirectory, fileChooser);
		fileChooser.setFileView(fileView);
		fileChooser.setPreferredSize(GeneralConfig.FILE_CHOOSER_SIZE);
		File correctFile = null;
		
		int returnVal = fileChooser.showSaveDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            
        	File selectedFile = fileChooser.getSelectedFile();
        	if(selectedFile==null||!selectedFile.isDirectory()) {
        		dialogHelper.error("Select a directory!", "Select a directory");
        	}else {
        		correctFile = selectedFile;
        		currentConf.getFileOpt().setLastDstFolderPath(correctFile.getAbsolutePath());
        	}
        }
        
        blockEventsModal.set(false);
        resetLaf(oldLaf);
        
        return correctFile;
	}
	
	private class CustomFileChooserOnTop extends JFileChooser{
		private static final long serialVersionUID = -1504918820583535391L;

		@Override
		protected JDialog createDialog(Component parent) throws HeadlessException {
			// intercept the dialog created by JFileChooser
			JDialog dialog = super.createDialog(parent);
			dialog.setAlwaysOnTop(true);
			dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
//			dialog.setModal(true); // set modality (or setModalityType)
			return dialog;
		}
	}
	
	private FileNameExtensionFilter getImageFileFilter(String ext) {
		return new FileNameExtensionFilter("Images (*." + ext + ")", ext);
	}

	private void updateFileFilters(JFileChooser fileChooser, FileFilter filter) {
		if (filter != null) {
			fileChooser.setFileFilter(filter);
		}
	}
	
	private static void addMultipleFileFilter(JFileChooser fileChooser, ExtensionFileFilter[] fileFilters) {
		
		if(fileFilters == null)
			return;
		
		fileChooser.setAcceptAllFileFilterUsed(true);

		for(int i = 0; i < fileFilters.length; i++) {
			fileChooser.addChoosableFileFilter(fileFilters[i]);
		}
	}
	
	private LookAndFeel backupLafNDefaultSystem() {
		LookAndFeel oldUI = UIManager.getLookAndFeel();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return oldUI;
	}
	
	private void resetLaf(LookAndFeel oldLaf) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(oldLaf);
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		});
	}
	
	/**
	 * If allFilesFilter an empty string will be returned
	 * @param filter
	 * @return
	 */
	private static String getSafeFilterExtension(FileFilter filter) {
		if(filter != null && filter instanceof ExtensionFileFilter) {
			return ((ExtensionFileFilter)filter).getExtension(0);
		} else {
			return "";
		}
	}
}
