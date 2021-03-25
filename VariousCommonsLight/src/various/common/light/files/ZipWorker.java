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
package various.common.light.files;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import various.common.light.gui.GuiUtils;
import various.common.light.gui.UpdateProgressPanel;
import various.common.light.network.utils.DownloadUtils;
import various.common.light.utility.log.SafeLogger;

public class ZipWorker {
	
	public static SafeLogger logger = new SafeLogger(ZipWorker.class);

	public static File zipFolderToFile(String dir2zip, File outputFile, boolean overwrite, boolean excludeParent) {
		
		if(dir2zip != null){
			File dirSrc = new File(dir2zip);
			if(dirSrc.exists() && dirSrc.isDirectory()) {
				try {
				
					//create a ZipOutputStream to zip the data to 
					if(!overwrite && outputFile.exists()) {
						outputFile = FileWorker.renameJavaObjFile(outputFile);
						outputFile.createNewFile();
					}
					
				    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile)); 
				    zipFolder(zos, dirSrc, null, new UpdateProgressPanel(true), true, excludeParent);
				    
				    //close the stream 
				    zos.close();
				    return outputFile;
				    
				}catch(Exception e){
					return null;
				}
			}
		} 
		
		return null;
	}

	public static File zipFolder(String dir2zip, File outputDir, boolean overwrite, boolean excludeParent) {
		
		
		if(dir2zip != null) {
			
			File dirSrc = new File(dir2zip);
			if (dirSrc.exists() && dirSrc.isDirectory()) {
				File outputFile = new File(outputDir.getAbsolutePath() + File.separator + dirSrc.getName() + ".zip");
				return zipFolderToFile(dir2zip, outputFile, overwrite, excludeParent);
			} 
		}
		
		return null;
	}
	
	/**
	 * Internal usage method.
	 */
	private static void zipFolder(
			ZipOutputStream zos,
			File fileToZip,
			String parentDirectoryName,
			UpdateProgressPanel progressUpdater,
			boolean autocloseProgbar,
			boolean excludeParent) throws Exception {
		
	    if (fileToZip == null || !fileToZip.exists()) {
	        return;
	    }
	
	    progressUpdater = (progressUpdater == null)? new UpdateProgressPanel(true) : progressUpdater;
	    FileInputStream fis = null;
	    
	    try {
	    	
	    	if (parentDirectoryName == null) {
				progressUpdater.getDialog().setTitle("Zipping folder: " + fileToZip.getName());
				progressUpdater.getDialog().setMinimumSize(new Dimension(650, 120));
				progressUpdater.getDialog().setPreferredSize(new Dimension(650, 120));
				progressUpdater.getGeneralBar().setIndeterminate(true);
				progressUpdater.getGeneralBar().setValue(50);
				progressUpdater.getGeneralBar().setStringPainted(false);
			}
	    	
			String zipEntryName = fileToZip.getName();
			if (parentDirectoryName!=null && !parentDirectoryName.isEmpty() && !excludeParent) {
			    zipEntryName = parentDirectoryName + "/" + fileToZip.getName();
			}
	
			if (fileToZip.isDirectory()) {
			    logger.info("+" + zipEntryName);
			    for (File file : fileToZip.listFiles()) {
			    	boolean dontUseParentForSons = excludeParent && parentDirectoryName == null;
			    	zipFolder(zos, file, zipEntryName, progressUpdater, false, dontUseParentForSons);
			    }
			} else {
			    logger.info("   " + zipEntryName);
			    byte[] buffer = new byte[1024];
			    fis = new FileInputStream(fileToZip);
			    zos.putNextEntry(new ZipEntry(zipEntryName));
			    
			    int fileSize = (int)fileToZip.length();
			    int length;
			    
			    int i = 0;
			    while ((length = fis.read(buffer)) > 0) {
			    	
			    	int percGenZipped = (int)((float)i*100*buffer.length/(float)fileSize);
			    	percGenZipped = percGenZipped >= 100 ? 99 : percGenZipped;
			    	UpdateProgressPanel.updateProgress("Zipping " + fileToZip, percGenZipped, progressUpdater, false);
			        zos.write(buffer, 0, length);
			        
			        i++;
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (autocloseProgbar) {
				progressUpdater.forceClose();
			} else {
				progressUpdater.getDialog().setVisible(true);
			}
			zos.closeEntry();
			if (fis != null) {
				fis.close();
			}
		}
	}

	/**
	 * Zip a file or folder in a new .zip archive, into destination folder. If overwrite is true, then no new unique file will be created, 
	 * but the existing one with same name will be overridden!
	 * 
	 * @param sourcePath
	 * @param destFolderPath
	 * @param ovewrwrite
	 * @return
	 * @throws Exception
	 * 
	 * <b>Fix this before to use it</b>
	 */
	@Deprecated
	public static File zipFile(String sourcePath, String destFolderPath, boolean ovewrwrite, UpdateProgressPanel progressUpdater) throws Exception {
		
		long startTime = System.currentTimeMillis();
		logger.info("FileStuff.zipFile() -> Starting zip:"
				+ "\n source: " + sourcePath
				+ "\n destPath: " + destFolderPath
				+ "\n overwrite: " + ovewrwrite);
		
		if(sourcePath == null || destFolderPath == null) {
			throw new NullPointerException();
		}
		
		if(progressUpdater != null) {
			progressUpdater.getDialog().setTitle("Zipping file: " + new File(sourcePath).getName());
			progressUpdater.getDialog().setMinimumSize(new Dimension(650, 120));
			progressUpdater.getDialog().setPreferredSize(new Dimension(650, 120));
			GuiUtils.centerComponent(progressUpdater.getDialog());
		}
		
		File destFolderOut = new File(destFolderPath);
		if(!destFolderOut.exists()) {
			destFolderOut.mkdirs();
		}
		
		File fileToZip = new File(sourcePath);
		destFolderPath += File.separator + fileToZip.getName();
		File out = (ovewrwrite)? new File(destFolderPath) : FileWorker.renameJavaObjFile(new File(destFolderPath));
		
		FileOutputStream fos = new FileOutputStream(out);
		ZipOutputStream zipOut = new ZipOutputStream(fos);
		FileInputStream fis = new FileInputStream(fileToZip);
		ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		int cycles = 1;
		while((length = fis.read(bytes)) >= 0) {
			int percDownloaded = DownloadUtils.getPerc(1024*cycles, zipEntry.getSize());
	    	UpdateProgressPanel.updateProgress("Zipping " + zipEntry.getName(), percDownloaded, progressUpdater, false);
			zipOut.write(bytes, 0, length);
			cycles ++;
		}
		zipOut.close();
		fis.close();
		fos.close();
		
		logger.info("FileStuff.zipFile() -> COMPLETED!"
				+ "\n source: " + sourcePath
				+ "\n zipped File: " + out
				+ "\n overwrite: " + ovewrwrite);
		
		logger.debug("zipFile -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
		
		return out;
	}

	/**
	 * Unzip an archive into destination folder. If overwrite is true, then no new unique file will be created, 
	 * but the existing one with same name will be overridden!
	 * 
	 * Use version From downloadUtils
	 * 
	 * @param sourcePath
	 * @param destFolderPath
	 * @param ovewrwrite
	 * @return
	 * @throws Exception
	 */
	public static File unzipFile(String sourcePath, String destFolderPath, boolean ovewrwrite) throws Exception {
		
		long startTime = System.currentTimeMillis();
		logger.info("FileStuff.unzipFile() -> Starting unzip:"
				+ "\n sourcePath: " + sourcePath
				+ "\n destFolderPath: " + destFolderPath
				+ "\n overwrite: " + ovewrwrite);

		File destFolderOut = new File(destFolderPath);
		UpdateProgressPanel progressUpdater = new UpdateProgressPanel(true);
		ZipInputStream zipInputStream = null;

		try {
			GuiUtils.centerComponent(progressUpdater.getDialog());
			progressUpdater.setClosable(true);
			progressUpdater.getDialog().setTitle("Unzipping file: " + new File(sourcePath).getName());
			progressUpdater.getDialog().setMinimumSize(new Dimension(650, 120));
			progressUpdater.getDialog().setPreferredSize(new Dimension(650, 120));
			progressUpdater.getDialog().setVisible(true);
			progressUpdater.getGeneralBar().setIndeterminate(true);
			
			if(sourcePath == null || destFolderPath == null || !new File(sourcePath).exists()) {
				DownloadUtils.logger.error("Null input data, cannot proceed.");
				throw new Exception();
			}
			
			// Adapt name in case of !overwrite
			File fileToUnZip = new File(sourcePath);
			int lastDotIndex = fileToUnZip.getName().lastIndexOf('.');
			String destPathFolderName = fileToUnZip.getName().substring(0,lastDotIndex);
			String separator = destFolderPath.endsWith("/") || destFolderPath.endsWith(File.separator) ? "" : File.separator;
			destFolderPath += separator + destPathFolderName;
			
			destFolderOut = new File(destFolderPath);
			DownloadUtils.logger.info(sourcePath + " will be extracted to " + destFolderOut);
			if(!destFolderOut.exists() || destFolderOut.isFile()) {
				destFolderOut.mkdirs();
				DownloadUtils.logger.info(destFolderOut + " Directory unexisting -> created");
			}
			
			byte[] buffer = new byte[1024];
			int zipEntries = zipEntriesCount(sourcePath);
			int currentEntryIterator = 1;
			zipInputStream = new ZipInputStream(new FileInputStream(sourcePath));
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			
			while (zipEntry != null) {
				DownloadUtils.logger.debug("Unzipping current element: " + zipEntry.getName());
				
				String newOutCurrentPath = destFolderPath + File.separator + zipEntry.getName();
				File outputFile = (ovewrwrite)? new File(newOutCurrentPath) : FileWorker.renameJavaObjFile(new File(newOutCurrentPath));
				
				// Existence check
				if(outputFile.exists()) {
					DownloadUtils.logger.debug("Current output existing: will be deleted! " + outputFile);
					boolean deleted = outputFile.delete();
					DownloadUtils.logger.debug(outputFile + " delete result: " + deleted);
				} else {
					DownloadUtils.logger.debug("Current output does not exist: creating all parent directories needed! " + outputFile.getParentFile());
					outputFile.getParentFile().mkdirs();
				}
				
				// In case of folder no need to unzip, just go to next
				if(zipEntry.isDirectory()) {
					DownloadUtils.logger.debug("Current element is directory, creating and continuing with next! " + zipEntry.getName());
					outputFile.mkdir();
					zipEntry = zipInputStream.getNextEntry();
					continue;
				}
				
				int percTot = currentEntryIterator / zipEntries;
				UpdateProgressPanel.updateProgress(zipEntry.getName(), percTot > 99 ? 99 : percTot, progressUpdater, true);
				
				// For every entry in archive
				DownloadUtils.logger.debug("Creating output stream for current output node: " + outputFile);
			    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			    DownloadUtils.logger.debug("Output stream created! starting unzip...");
			    
			    int len;
			    int cycles = 1;
			    while ((len = zipInputStream.read(buffer)) > 0) {
			    	int percDownloaded = Math.abs(DownloadUtils.getPerc(1024*cycles, zipEntry.getSize()));
			    	percDownloaded = (percDownloaded>99) ? 99 : percDownloaded;
			    	
			    	// In case of size unavailable | negative value | near to finish : set progressbar undefined
			    	progressUpdater.getPb().setIndeterminate(percDownloaded == 99);
			    	DownloadUtils.ifStoppedByUserThrowException(progressUpdater);
			    	UpdateProgressPanel.updateProgress(zipEntry.getName(), percDownloaded, progressUpdater, false);
			    	
			    	// For each byte of current zipped file in archive
			        fileOutputStream.write(buffer, 0, len);
			        
			        cycles++;
			    }
			    
			    fileOutputStream.close();
			    DownloadUtils.logger.debug("Current element unzipped! " + zipEntry.getName());
			    zipEntry = zipInputStream.getNextEntry();
			    
			    currentEntryIterator ++;
			}
			
			DownloadUtils.logger.info("unzip file: elapsed" + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
			DownloadUtils.logger.info("FileStuff.unzipFile() -> COMPLETED!"
					+ "\n source: " + sourcePath
					+ "\n unzipped File: " + destFolderPath
					+ "\n overwrite: " + ovewrwrite);
		
		} catch (Throwable e) {
			DownloadUtils.logger.error("An error occurred while unzipping data", e);
			throw e;
			
		} finally {
			try {
				zipInputStream.closeEntry();
				zipInputStream.close();
			}catch(Exception e) {}

			if (progressUpdater != null) {
				progressUpdater.update("Completed", 100);
				progressUpdater.forceClose();
			}
		}
	    
	    return destFolderOut;
	}
	
	/**
	 * Retrieve number of entries within a zip file
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static int zipEntriesCount(String path) throws IOException {

	     ZipFile zf = new ZipFile(path);
	     try {
			return zf.size();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			zf.close();
		}
	}

}
