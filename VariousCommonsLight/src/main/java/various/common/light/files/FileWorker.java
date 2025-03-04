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

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import various.common.light.files.om.DeleteFilesByExtensionDTO;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.string.StringWorker.EOL;

public class FileWorker {

	public static SafeLogger logger = new SafeLogger(FileWorker.class);

	private static JOptionHelper dialogHelper = new JOptionHelper(null);

	public static File copyFile(File source, File dest, boolean overwrite) {

		long startTime = System.currentTimeMillis();

		File esit = null;

		// if dir not existing or dest file not existing
		if(!dest.exists()) {
			dest.getParentFile().mkdirs();
			try {
				dest.createNewFile();
			} catch (IOException e) {
				logger.error("Exception happened!", e);
				esit = null;
			}
		}else {
			if(overwrite) {
				logger.debug("File existing: user allowed to overwrite.");
				dest.delete();
				try {
					dest.createNewFile();
				} catch (IOException e) {
					logger.error("Exception happened!", e);
					esit = dest;
				}
			}else {
				// else give it unique name
				dest = FileWorker.renameJavaObjFile(dest);
			}
		}
		try {
		    FileUtils.copyFile(source, dest);
		    logger.debug("File written: "+dest.getAbsolutePath()+"\nSize: " + ConversionUtils.coolFileSize(dest));
		    esit = dest;
		} catch (IOException e) {
			logger.error("Exception happened!", e);
		   esit = null;
		}

		logger.info("copyFile -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
		return esit;
	}

	public static boolean appendFileTxtArea(
			File txtFile,
			JTextArea textArea_1,
			boolean goToStart){
		return appendFileTxtArea(txtFile, textArea_1, goToStart, EOL.defaultEol);
	}

	/**
	 * this method append the content of a txt file (as a string) in the given text area
	 * @param txtFile the source .txt file
	 * @param textArea_1 the text area to be filled with file content
	 * @return
	 */
	public static boolean appendFileTxtArea(
			File txtFile,
			JTextArea textArea_1,
			boolean goToStart,
			EOL eol) {

		long startTime = System.currentTimeMillis();

		int originalPosition = textArea_1.getCaretPosition();

		if(txtFile == null) {
			return false;
		}else {
			logger.info("appendFileTxtArea -> Start...");
			StringBuilder builder = new StringBuilder(100000);
			try {
				FileInputStream readerStream = new FileInputStream(txtFile);
				BufferedReader reader;
				reader = new BufferedReader(new InputStreamReader(readerStream, System.getProperty("file.encoding")));
				try {
					String line;

					while ((line = reader.readLine()) != null) {
						builder.append(line).append(eol.eol);
					}
					reader.close();
					readerStream.close();
				} catch (IOException e) {
					logger.error("", e);
					try {reader.close(); readerStream.close();}
					catch (IOException e1) {}
				}
			} catch (UnsupportedEncodingException | FileNotFoundException e2) {
				logger.error("", e2);
			}finally {
				textArea_1.append(builder.toString());
				logger.info("appendFileTxtArea -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
			}
			textArea_1.requestFocus();
			if (goToStart) {
				textArea_1.setCaretPosition(0);
			}else try{
				if(originalPosition > textArea_1.getText().length()) {
					textArea_1.setCaretPosition(textArea_1.getDocument().getLength()-1);
				}else {
					textArea_1.setCaretPosition(originalPosition);
				}
			}catch (Exception e) {
				// avoided outOfBounds
			}
		}
		return true;
	}

	public static boolean appendStringToFile(String source, File outFile, boolean createIfNotExisting, EOL eol) {

		if(outFile == null || (!outFile.exists() && !createIfNotExisting))
			return false;

		try {
			if(!outFile.exists())
				outFile.createNewFile();

			String outString = eol != null
					? StringWorker.normalizeStringToEol(source, eol)
					: source;

		    Files.write(outFile.toPath(), outString.getBytes(), StandardOpenOption.APPEND);

		}catch (IOException e) {
		    logger.error("Error appending String to file to file", e);
		    return false;
		}

		return true;
	}


	/**
	 * This method writes a string to a file with given EOL (if null default will be used)
	 * @param source the source string to write on file
	 * @param outFile the ouput file to write in
	 * @param overwrite if true overwrite existing file
	 * @return
	 * @throws IOException
	 */
	public static boolean writeStringToFile(String source, File outFile, boolean overwrite, EOL eol) throws IOException {
		String outString = eol != null
				? StringWorker.normalizeStringToEol(source, eol)
				: source;

		return writeStringToFile(outString, outFile, overwrite);
	}

	/**
	 * This method writes a string to a file
	 * @param source the source string to write on file
	 * @param outFile the ouput file to write in
	 * @param overwrite if true overwrite existing file
	 * @return
	 * @throws IOException
	 */
	public static boolean writeStringToFile(String source, File outFile, boolean overwrite) throws IOException {

		long startTime = System.currentTimeMillis();
		logger.info("writeStringToFile -> Start (outFile : " + outFile + ")");

		if(outFile.exists()) {
			boolean deleted = true;
			boolean recreated = false;
			if (overwrite) {
				deleted = outFile.delete();
				recreated = outFile.createNewFile();
			}else {
				outFile = FileWorker.renameJavaObjFile(outFile);
				recreated = outFile.createNewFile();
			}

			if(!recreated || !deleted) {
				return false;
			}

		}else {
			boolean recreated = outFile.createNewFile();
			if(!recreated) {
				return false;
			}
		}

		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outFile), System.getProperty("file.encoding"));
		PrintWriter out = new PrintWriter(outputStreamWriter);
		out.write(source);

		out.close();
		outputStreamWriter.close();

		logger.info("writeStringToFile -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
		return true;
	}

	public static String readFileAsString(
			File toRead,
			String encoding) throws IOException {
		return readFileAsString(toRead, encoding, EOL.defaultEol);
	}

	// returns true if given file contains more than N lines
	public static boolean checkIfHasMoreThanNLines(
			File toRead,
			int limit) {

		if(toRead == null || !toRead.exists()) {
			return false;
		}

		String defaultEncoding = System.getProperty("file.encoding");

		logger.info("readFileAsString -> Start...");

		BufferedReader reader = null;
		try {
			FileInputStream readerStream = new FileInputStream(toRead);
			reader = new BufferedReader(new InputStreamReader(readerStream, defaultEncoding));
		} catch (Exception e1) {
			logger.debug("Cannot read with default encoding: " + defaultEncoding);
			try {
				reader.close();
			} catch(Exception e) {
			}

			return false;
		}

		try {
			String line = reader.readLine();

			int i = 0;
			while ( line != null) {
				if(i > limit)
					return true;

				line = reader.readLine();
				i++;
			}
		} catch (IOException e) {
			return false;
		}finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	public static String readFileAsString(
			File toRead,
			String encoding,
			EOL eol) throws IOException {

		if(toRead == null || !toRead.exists()) {
			throw new IOException();
		}

		String defaultEncoding = System.getProperty("file.encoding");
		encoding = encoding != null ? encoding : defaultEncoding;

		logger.info("readFileAsString -> Start...");
		long startTime = System.currentTimeMillis();

		StringBuilder builder = new StringBuilder(100000);
		FileInputStream readerStream = new FileInputStream(toRead);
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(readerStream, encoding));
		} catch (Exception e1) {
			logger.debug("Cannot read with given encoding: " + encoding + " - Using default: " + defaultEncoding);
			reader = new BufferedReader(new InputStreamReader(readerStream, defaultEncoding));
		}
		try {
			String line = reader.readLine();

			while ( line != null) {
				builder.append(line);
				line = reader.readLine();
				builder.append(eol.eol);
			}
		} catch (IOException e) {
			throw e;
		}finally {
			reader.close();
		}

		logger.info("readFileAsString -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
		return builder.toString();
	}

	/**
	 * This method read from a file and give a String representation of content
	 * @param toRead the source file to read
	 * @return
	 * @throws IOException
	 */
	public static String readFileAsString(File toRead) throws IOException {
		return readFileAsString(toRead, null);
	}

	public static boolean loadFileTxtArea(
			File txtFile,
			JTextArea textArea_1,
			JLabel lblCurrentFile,
			String extensionFilter) {
		return loadFileTxtArea(txtFile, textArea_1, lblCurrentFile, extensionFilter, EOL.defaultEol);
	}


	/**
	 * this method writes the content of a given extension file (as a string) in the given text area, updating also the given label with description
	 * @param txtFile the source .txt file
	 * @param textArea_1 the text area to be filled with file content
	 * @param lblCurrentFile the label that will contain the info about operation, this can be null
	 * @param extension filter can be null, or specified if you want to filter only a type of file admitted
	 * @return
	 */
	public static boolean loadFileTxtArea(
			File txtFile,
			JTextArea textArea_1,
			JLabel lblCurrentFile,
			String extensionFilter,
			EOL eol) {

		if(txtFile == null) {
			return false;
		}

		long startTime = System.currentTimeMillis();
		logger.info("loadFileTxtArea -> Start...");

		if(extensionFilter != null && !FilenameUtils.getExtension(txtFile.getAbsolutePath()).equals(extensionFilter)) {
			dialogHelper.warn("Please select a valid "+ extensionFilter +" file!", null);

		} else {
			BufferedReader reader;
			StringBuilder builder = new StringBuilder();
			textArea_1.setText("");

			try {
				reader = new BufferedReader(new FileReader(txtFile.getAbsolutePath()));

				String line;

				while ((line = reader.readLine()) != null) {
					builder.append(line).append(eol.eol);
				}

				textArea_1.append(builder.toString());
				reader.close();
				if (lblCurrentFile != null) {
					lblCurrentFile.setText(txtFile.getParentFile().getName() + "\\" + txtFile.getName());
				}

			} catch (Exception e1) {
				dialogHelper.error("[Error loading the selected file]", (String)null);
				logger.error("Exception happened!", e1);
				return false;
			}

		}

	    logger.info("loadFileTxtArea -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
		return true;
	}

	/**
	 * this method change name of java File variable until the result is a non existent file
	 * It do that adding (i) numbers at the end of the file name, just before the start of the
	 * extension . THIS DOES NOT ACTUALLY CHANGE PHYSIC FILE'S NAME! (it changes only java object
	 * until it represents a non-existing file in the same parent directory as the given one.
	 *
	 * @param file
	 * @return the new variable representing non existing file, useful to call before create a new file
	 * 	to ensure that it is a not existing one.
	 */
	public static File renameJavaObjFile(File file) {
		String fileName=file.getPath();

		String extension = "";
		String name = "";

		int idxOfDot = fileName.lastIndexOf('.');   //Get the last index of . to separate extension
		if(idxOfDot>=0) {
			extension = fileName.substring(idxOfDot + 1);
			name = fileName.substring(0, idxOfDot);
		}else {
			name = fileName;
		}

		Path path = Paths.get(fileName);
		int counter = 1;
		File f = null;
		while(Files.exists(path)){
			// if already with parentesis number
			if(name.substring(name.length()-1, name.length()).equals(")")) {
				idxOfDot = fileName.lastIndexOf('(');
				name = fileName.substring(0, idxOfDot);
			}
		    fileName = name+"("+counter+")."+extension;
		    path = Paths.get(fileName);
		    counter++;
		}
		f = new File(fileName);

		return f;
	}

	/**
	 * This method is meant to delete all folder content, deleting recursively also the folders inside it.
	 * the given folder won't be deleted, only it's content will be.
	 *
	 * @param folder the source folder to be emptied
	 * @return deleted: false if at least one deletion have incurred in some errors
	 */
	public static boolean deleteDirContentRecursive(File folder) {
		boolean deleted = true;
		long startTime = System.currentTimeMillis();
		out.println("deleteDirContentRecursive -> Start...");

		File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                boolean del = FileWorker.deleteDirContentInclRoot(f);
	                if(del==false) {
	                	deleted = false;
	                }
	            } else {
	                boolean del = f.delete();
	                if(del==false) {
	                	deleted = false;
	                }
	            }
	        }
	    }

	    out.println("deleteDirContentRecursive -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
	    return deleted;
	}

	/**
	 * This method is meant to delete all folder content, excluding sub-directories and their content.
	 * the given folder won't be deleted, only it's content files will be.
	 *
	 * @param folder the source folder, which content files have to be deleted
	 * @return deleted: false if at least one deletion have incurred in some errors
	 */
	public static boolean deleteDirContent(File folder) {
		boolean deleted = false;
		long startTime = System.currentTimeMillis();
		logger.info("deleteDirContent -> Start...");

		File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isFile()) {
	            	deleted = f.delete();
	            }
	        }
	    }

	    logger.info("deleteDirContent -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
	    return deleted;
	}

	/**
	 * same as deleteDirContentRecursive but this one deletes also the root folder
	 * @param folder
	 * @return
	 */
	public static boolean deleteDirContentInclRoot(File folder) {
		boolean deleted = true;
		long startTime = System.currentTimeMillis();
		out.println("deleteDirContentInclRoot -> Start...");

		File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	    	for(File f: files) {
		        try {
					    if(f.isDirectory()) {
					        boolean del = deleteDirContentInclRoot(f);
					        if(del==false) {
					        	deleted = false;
					        }
					    } else {
					        boolean del = f.delete();
					        if(del==false) {
					        	deleted = false;
					        }
					    }
				} catch (Exception e) {
					logger.error("Cannot delete file: " + f, e);
					deleted = false;
				}
	    	}
	    }

	    // at the end delete given container folder too
	    folder.delete();

	    logger.info("deleteDirContentInclRoot -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
	    return deleted;
	}

	public static void moveDirectory(File directoryToMove, File destination, StandardCopyOption copyOption) throws IOException {
		Files.move(directoryToMove.toPath(), destination.toPath(), copyOption);
	}

	/**
	 * this method is used to physically copy the content of a folder inside a destination folder
	 * (works also for a source file instead of a source folder).
	 * @param source Directory or file
	 * @param destination Directory container / target file, to replace if exists
	 * @param extFilter is the extension to filter and mantain // ex: "png", "txt"
	 * if it's "" then every extension will be kept
	 * @throws IOException
	 */
	public static void copyDirectoryContents(File source, File destination) throws IOException{

		long startTime = System.currentTimeMillis();
		logger.info("copyDirectoryContents -> Start...");

		for(File file : FileWorker.getAllContent(destination, CustomFileFilters.ALL_FOUND)) {
			try {
				file.setWritable(true);
			} catch(Exception e) {
				logger.warn("Cannot set writable file: " + file, e);
			}
		}

		if(source.isDirectory()) {
			for (File curr : source.listFiles()) {
				logger.info("Copying "+curr+" \n  => into "+destination);
				try {
					FileUtils.copyToDirectory(curr, destination);
				} catch (Exception e) {
					logger.error("An error occurred while copying \"" + curr + "\" into " + destination, e);
				}
			}
		}

		out.println("copyDirectoryContents -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
	}

	public static boolean isTextAreaEqualToFileContent(JTextArea textArea, File fileToCheck) throws IOException {
		if(textArea == null || fileToCheck == null || !fileToCheck.exists()) {
			return false;
		}

		String fileRead = readFileAsString(fileToCheck);
		if(fileRead != null && fileRead.equals(textArea.getText())) {
			return true;
		}

		return false;
	}

	/**
	 * this method is used to physically copy the content of a folder inside a distination folder
	 * (works also for a source file instead of a source folder).
	 * @param source Directory or file
	 * @param destination Directory container / target file, to replace if exists
	 * @param extFilter is the extension to filter and mantain // ex: "png", "txt"
	 * if it's "" then every extension will be kept
	 * @throws IOException
	 */
	public static void copyDirectoryContents(File source, File destination, boolean overwrite, String extFilter) throws IOException{

		long startTime = System.currentTimeMillis();
		logger.info("copyDirectoryContents -> Start...");

		if(source.isDirectory()) {
			File[] srcFiles = source.listFiles();
			if (srcFiles!=null) {
				for (File file : srcFiles) {

					if (extFilter.equals("") || FilenameUtils.getExtension(file.getName()).equals(extFilter)) {
						String destinationPathString = destination.getPath() + "\\" + file.getName();
						File newFile = new File(destinationPathString);
						if (newFile.exists() && !overwrite) {
							// if file i exists and overwrite specified as false, then use the function to
							// correct the name until it's not unique, and then update path string
							destinationPathString = destination.getPath() + "\\" + FileVarious.uniqueJavaObjFile(newFile).getName();
						}
						Path destinationPath = Paths.get(destinationPathString);
						Files.copy(file.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}

		logger.info("copyDirectoryContents -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
	}

	/**
	 * This method copy the structure of the source dir, copying any found folder inside it (comprising source dir),
	 * and creates the same structure in the given destination dir. If inside dest dir there is a folder with same name
	 * as source dir, it creates a unique name.
	 * @param sourceDir
	 * @param destinationRootDir
	 * @return true if every folder is created, false in the other case (some folder may be missing)
	 * @throws IOException
	 */
	public static boolean copyDirStructure(File sourceDir, File destinationRootDir, boolean recreateRoot) throws IOException {

		if(!sourceDir.exists() || FileVarious.checkIfContainedInDir(destinationRootDir, sourceDir)) {
			throw new IOException();
		}

		long startTime = System.currentTimeMillis();
		logger.info("copyDirStructure -> Start...");

		boolean esit = true;
		String destRootPath = destinationRootDir.getAbsolutePath();

		// only first round recreates the source dir - if all the time cycle never ends
		if(recreateRoot) {
			destRootPath = destRootPath+"/"+sourceDir.getName();
			destRootPath = FileVarious.getUniqueNameFile(new File(destRootPath));
			new File(destRootPath).mkdir();
		}

		File[] foundDirectories = sourceDir.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.DIR_ONLY));
		for(File current : foundDirectories) {
			File destCurrent = new File(destRootPath + "/" + current.getName());

			// recursive repeat for current source folder inside root
			if(!copyDirStructure(current, destCurrent, false) && !destCurrent.exists()) {
				esit = false;
			};

			// copy current directory
			if(!destCurrent.mkdirs() && !destCurrent.exists()) {
				esit = false;
			}

		}

		logger.info("copyDirStructure -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
		return esit;
	}

	/**
	 * this method delete recursively the given directory's content, deleting only files that match the given extensions.
	 * @param rootDir
	 * @param extListCommaSeparated list of extension to delete separated by a comma (spaces will be removed)
	 * @return true if all is deleted, false if something is still present
	 */
	public static DeleteFilesByExtensionDTO emptyDirContentByExt(File rootDir, String extListCommaSeparated) {
		DeleteFilesByExtensionDTO response = new DeleteFilesByExtensionDTO();

		// validate input before start
		if(rootDir== null || !rootDir.exists()) {
			response.setEsit(false);
			return response;
		}

		boolean esit = true;
		extListCommaSeparated = StringWorker.trimToEmpty(extListCommaSeparated);
		String[] extensionsList = extListCommaSeparated.split(",");
		for(String currExtension : extensionsList) {
			currExtension = currExtension.trim(); // remove whitespaces at start and at end
			currExtension = FileVarious.removeIllegalCharsFromExtension(currExtension);

			// delete content of current cleaned extension
			esit = true;
			File[] foundCurrExtContent = rootDir.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.FILE_ONLY, currExtension, true));
			if(foundCurrExtContent.length != 0) {
				response.setNothingFound(false);
			}

			for(File current : foundCurrExtContent) {
				// delete current if file
				if(!FileUtils.deleteQuietly(current)) {
					esit = false;
				}
			}

			// if current extension has 0 results
			if(foundCurrExtContent.length == 0) {
				response.getNotFoundExtensionResult().add(currExtension);
			}

			// recursion for found folders inside current
			File[] foundCurrDirs = rootDir.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.DIR_ONLY));
			for(File currentFoundDir : foundCurrDirs) {
				DeleteFilesByExtensionDTO subResponse = emptyDirContentByExt(currentFoundDir, currExtension);
				esit = (subResponse.isEsit() == false)? false : esit;
				response.setNothingFound(response.isNothingFound() && subResponse.isNothingFound());
				for (String subExt : subResponse.getNotFoundExtensionResult()) {
					if (!response.getNotFoundExtensionResult().contains(subExt)) {
						response.getNotFoundExtensionResult().add(subExt);
					}
				}
			}
		}

		response.setEsit(esit);

		return response;
	}

	/**
	 * this method delete recursively the given directory's content, without deleting the folders in it.
	 * @param dirToEmpy
	 * @return true if all is deleted, false if something is still present
	 */
	public static boolean emptyDirContentFolderOnly(File dirToEmpy) {
		if(!dirToEmpy.exists()) {
			return false;
		}

		boolean esit = true;

		File[] foundContent = dirToEmpy.listFiles(CustomFileFilters.customTypeFilter(CustomFileFilters.ALL_FOUND));
		for(File current : foundContent) {

			// delete current if file
			if(current.isFile() && !FileUtils.deleteQuietly(current)) {
				esit = false;
			}

			// recursive delete content if folder
			if(current.isDirectory() && !emptyDirContentFolderOnly(current)) {
				esit = false;
			};
		}

		return esit;
	}

	/**
	 * List all files in directory and his subdirectories.
	 * Use CustomFileFilters class and his parameters to select type of search.
	 * @param sourceDir
	 * @param filterType
	 * @return
	 */
	public static List<File> getAllContent(File sourceDir, String filterType) {
		List<File> result = new ArrayList<>();
		for(File found : sourceDir.listFiles()) {
			if(found.isDirectory()) {
				result.addAll(getAllContent(found, filterType));
			}else if(CustomFileFilters.fileAccepted(filterType)){
				result.add(found);
			}
		}
		return result;
	}

	public static File copyFileIntoDir(File source, File destdir, boolean overwrite, String tempDir) {

		if(destdir == null || source == null || !source.exists()) {
			return null;
		}

		long startTime = System.currentTimeMillis();
		File targetNewFile = new File(destdir.getAbsolutePath() + File.separator + source.getName());
		logger.info("copyFileIntoDir -> Start...");

		boolean esit = false;

		// if dir not existing or dest file not existing
		if(!destdir.exists()) {
			destdir.getParentFile().mkdirs();
			destdir.mkdir();
		}

		if(!overwrite) {
			logger.debug("File exists: "+targetNewFile.getAbsolutePath()+" -> starting rename");
			// Create unique file name and write a copy into temp folder, using it as source
			targetNewFile = FileVarious.uniqueJavaObjFile(targetNewFile);
			copyFile(source, new File(tempDir + targetNewFile.getName()), true);
			source = new File(tempDir + targetNewFile.getName());
			logger.debug("File renamed and -> " + targetNewFile.getAbsolutePath()+" -> copied into temp directory: " + tempDir);
		}

		try {
		    FileUtils.copyFileToDirectory(source, destdir, true);
		    logger.debug("File written: "+destdir.getAbsolutePath()+"\nSize: " + ConversionUtils.coolFileSize(destdir));
		    esit = true;
		} catch (IOException e) {
			logger.error("Exception happened!", e);
		   esit = false;
		}

		logger.info("copyFileIntoDir -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");

		return (esit)? new File(destdir.getAbsolutePath()+File.separator + source.getName()) : null;
	}

	public static final void mkDirs(String... mkdirList) {
		if(mkdirList != null) {
			for(String dir : mkdirList) {
				try {
					new File(dir).mkdirs();
				} catch (Exception e) {
					logger.warn("Cannot apply mkDirs to given source: " + dir);
				}
			}
		}
	}

	public static final void cleanupDirs(String... mkdirList) {
		if(mkdirList != null) {
			for(String dir : mkdirList) {
				try {
					deleteDirContentRecursive(new File(dir));
				} catch (Exception e) {
					logger.warn("Cannot cleanup given source: " + dir);
				}
			}
		}
	}
}
