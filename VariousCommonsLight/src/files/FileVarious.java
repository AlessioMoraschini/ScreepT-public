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
package files;

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.commons.io.FilenameUtils;

import gui.dialogs.msg.JOptionHelper;
import utility.log.SafeLogger;
import utility.string.StringWorker;
import utility.string.StringWorker.EOL;

/**
 * This class contains a mix of useful methods forked from various classes made by me, to avoid including big dependencies only
 * to use a little of given functions, that would have made the updater module very heavy, so I decided to create this utils class.
 *
 * @author Alessio Moraschini
 */
public class FileVarious {

	private static SafeLogger logger = new SafeLogger(FileVarious.class);

	private static final String END_ARGS_SECTION = "###_END_SECTION";

	/**JFRAME - only directories src
	 * this method select a directory from the user (only directories),if nothing is selected returns null
	 */
	public static File directoRead(JFrame parent, String title, File initialDir) {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(title);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setCurrentDirectory(initialDir);
		fileChooser.setPreferredSize(new Dimension(800, 450));
		File correctFile = null;

		int returnVal = fileChooser.showOpenDialog(parent);
		if (returnVal == JFileChooser.APPROVE_OPTION) {

			File selectedFile = fileChooser.getSelectedFile();
			if(selectedFile==null||!selectedFile.isDirectory()) {
				new JOptionHelper(parent).error("Select a directory!", "Select a directory");
			}else {
				correctFile = selectedFile;
			}
		}
		return correctFile;
	}

	public static File getParent(File currentFile) {
		if(currentFile != null) {
			return currentFile;
		} else {
			return null;
		}
	}

	public static String getParentPath(File currentFile) {
		File parent = getParent(currentFile);
		return parent == null ? null : parent.getAbsolutePath();
	}

	public static String getCanonicalPathSafe(File file) {
		try {
			return file.getCanonicalPath();
		} catch (Exception e) {
			if(file != null)
				return file.getAbsolutePath();
			else
				return null;
		}
	}

	public static File getCanonicalFileSafe(File file) {
		try {
			return file.getCanonicalFile();
		} catch (Exception e) {
			return file;
		}
	}

	public static String getCanonicalPathSafeNormalized(File file, String separatorNormalized) {
		String raw = null;
		try {
			raw = file.getCanonicalPath();

		} catch (Exception e) {
			if(file != null)
				raw = file.getAbsolutePath();
			else
				return null;
		}

		raw = raw.replaceAll("/", "\\\\");
		raw = raw.replaceAll("\\\\", separatorNormalized);

		return raw;
	}

	public static boolean checkIfSameFile(File fileA, File fileB) {
		 String pathA = StringWorker.trimToEmpty(getCanonicalPathSafeNormalized(fileA, "/"));
		 String pathB = StringWorker.trimToEmpty(getCanonicalPathSafeNormalized(fileB, "/"));

		 return pathA.equals(pathB);
	}

	public static boolean validateFileOrDir(File fileOrDir) {
		return fileOrDir != null && fileOrDir.exists();
	}
	public static boolean validateDir(File dir) {
		return validateFileOrDir(dir) && dir.isDirectory();
	}
	public static boolean validateFile(File file) {
		return validateFileOrDir(file) && file.isFile();
	}

	/**
	 * Get relative path from a reference file path:
	 *
	 * first = a/bcd/e/fg
	 * other = a/bcd
	 *
	 * return: ./e/fg
	 *
	 * @param first
	 * @param other
	 * @return
	 * @throws IOException
	 */
	public static String getPathRelativeToOtherFile(File first, File other) throws IOException {

		if(first == null || other == null) {
			return null;
		}

		return other.toURI().relativize(first.toURI()).getPath();
	}

	public static List<File> addWithoutRepetitions(List<File> toAdd, List<File> originalList){
		if(originalList == null)
			originalList = new ArrayList<>();

		if(toAdd != null && !toAdd.isEmpty()) {
			for(File currentNew : toAdd) {
				boolean currentAlreadyPresent = originalList.isEmpty() ? false : true;
				int i = 0;
				for(File file : originalList) {
					try {
						if(file.getCanonicalPath().equals(currentNew.getCanonicalPath()))
							break;

						if(i == originalList.size() - 1)
							currentAlreadyPresent = false;

					} catch (IOException e) {
						logger.error("", e);
					}

					i++;
				}

				if(!currentAlreadyPresent)
					originalList.add(currentNew);
			}
		}

		return originalList;
	}

	public static boolean isBinaryFile(File fileToCheck) throws IOException {
        String type = Files.probeContentType(fileToCheck.toPath());
        if (type == null) {
            //type couldn't be determined, assume text
            return false;
        } else if (type.startsWith("text")) {
            return false;
        } else {
            //type isn't text
            return true;
        }
    }

	public static ArrayList<String> getPathsRelativeToOtherFile(ArrayList<File> toRelativize, File reference) throws Exception{
		ArrayList<String> relativePaths = new ArrayList<>();

		if(toRelativize == null || toRelativize.isEmpty() || reference == null || !reference.exists()) {
			return relativePaths;
		}

		for(File currToRelativize : toRelativize) {
			if (currToRelativize != null && currToRelativize.exists()) {
				String relative = getPathRelativeToOtherFile(currToRelativize, reference);
				relativePaths.add(relative);
			}
		}

		return relativePaths;
	}

	/**
	 * Get all files contained in a directory, and also in his sublfolders. NB only files will be returned!
	 *
	 * @param ArrayList<File> foundFiles use an initialized or empty or null array list, to which found files will be added
	 * @param directoryName
	 */
	public static ArrayList<File> listFiles(File directory, ArrayList<File> foundFiles) {

		if(foundFiles == null) {
			foundFiles = new ArrayList<>();
		}

	    // Get all files from a directory.
	    File[] thisLevelFiles = directory.listFiles();
	    if(thisLevelFiles != null) {
	        for (File current : thisLevelFiles) {
	            if (current.isFile()) {
	            	foundFiles.add(current);
	            } else if (current.isDirectory()) {
	            	listFiles(current, foundFiles);
	            }
	        }
	    }

		return foundFiles;
	}

	// PRINT UTILS METHODS
	/**
	 * Retrieve a String separator n-times character
	 */
	public static String getSeparator(String character, int n){
		int curr = 0;
		String output = EOL.defaultEol.eol + "  ";
		while (curr < n) {
			output = output.concat(String.valueOf(character));
			curr++;
		}

		return output;
	}

	/**
	 * Create a string of n spaces
	 * @param n
	 */
	public static String getNspacesString(int n) {
		String result = "";
		for(int i = 0; i<n; i++) {
			result = result.concat(" ");
		}

		return result;
	}

	/**
	 * Retrieve a header String specifying character separator (may be null), header width in characters, and title
	 */
	public static String getHeaderString(String character, int n, String title){
		String result = "";

		int newN = (n >= title.length()+4)? n : title.length()+4;
		newN = newN*character.length();
		String leftSpaces = getNspacesString((newN - title.length() -2)/2);
		boolean hasToPad = (leftSpaces.length()*2 + 2 + title.length()) < newN;
		String rightPadding = (hasToPad)? " " : "";
		String rightSpaces = leftSpaces + rightPadding;

		result += getSeparator(character, newN);
		result += EOL.defaultEol.eol + "  " + character + leftSpaces + title + rightSpaces + character ;
		result += getSeparator(character, newN);
		result += EOL.defaultEol.eol;

		return result;
	}

	/**
	 * Inserts a number like file(1).txt from the original file.txt
	 * @param fileName
	 * @return
	 */
	public static String insertNumberBeforeExtension(String fileName, int n) {

		String extension = "";
		String name = "";

		int idxOfDot = fileName.lastIndexOf('.');   //Get the last index of . to separate extension
		if(idxOfDot>=0) {
			extension = fileName.substring(idxOfDot + 1);
			name = fileName.substring(0, idxOfDot);
		}else {
			name = fileName;
		}

		// if already with parentesis number
		if(name.substring(name.length()-1, name.length()).equals(")")) {
			idxOfDot = fileName.lastIndexOf('(');
			name = fileName.substring(0, idxOfDot);
		}
	    fileName = name+"("+n+")."+extension;

		return fileName;
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
	public static File uniqueJavaObjFile(File file) {
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
	 * This method rename single file or directory, making the name starting with an uppercase letter
	 * @param rootDir
	 * @return success the boolean value representing the esit of the operation
	 */
	public static boolean renameFileUpperFirst(File rootDir) {
		String name="";
		String newName="";
		name=rootDir.getName();
		// ottengo la prima lettera e la rendo maiuscola
		String firstLetter = String.valueOf(name.charAt(0)).toUpperCase();
		//tolgo il primo carattere
		name = name.substring(1, name.length());
		newName=firstLetter+name;
		File fileWithNewName = new File(rootDir.getParent(), newName);
		boolean success = rootDir.renameTo(fileWithNewName);
	    if (!success) {
	    	logger.warn("Not renamed! "+newName);
	    }else {
			logger.debug("First letter: "+firstLetter+" - New name: "+newName);
	    }
	    return success;
	}

	/**
	 * This method rename every single file or directory contained in the given one as parameter,
	 * making the name starting with an uppercase letter.
	 * @param rootDir the source directory
	 * @return success the number of the renamed elements
	 */
	public static int upperCaseSubFolder(File rootDir) {
		int total=0;
		boolean esit=false;
		if (rootDir!=null) {
			if (rootDir.isFile()) {
				esit = renameFileUpperFirst(rootDir);
				if (esit == false)
					total++;
			} else {
				renameFileUpperFirst(rootDir);
				for (File file : rootDir.listFiles()) {
					// se � una directory uso la ricorsione
					if (file.isDirectory()) {
						esit = renameFileUpperFirst(file);
						upperCaseSubFolder(file);
					} else if (file.isFile()) {
						esit = renameFileUpperFirst(file);
					}
					if (esit == false)
						total++;
				}
			}
		}
		return total;
	}

	/**
	 * get number of files in a given folder and inside its sub-directories recursively, counting folders if countDirectories is true
	 * @param directory the root directory to search in
	 * @param extensionsToCount these extension will be counted when a match is found
	 * @return
	 */
	public static int countFilesInDirectory(File directory, boolean countDirectories, ArrayList<String> extensionsToCount) {
		int count = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile()){
				String name = file.getName().toLowerCase().trim();
				for(String acceptedExtension : extensionsToCount) {
					if(name.endsWith(acceptedExtension.toLowerCase())) {
						count++;
					}
				}
			}
			if (file.isDirectory()) {
				if(countDirectories) {
					count++;
				}
				count += countFilesInDirectory(file, countDirectories);
			}
		}
		return count;
	}

	/**
	 * get number of files in a given folder and inside its sub-directories recursively, counting folders if countDirectories is true
	 * @param directory the root directory to search in
	 * @return
	 */
	public static int countFilesInDirectory(File directory, boolean countDirectories) {
	  int count = 0;
	  for (File file : directory.listFiles()) {
	      if (file.isFile()) {
	          count++;
	      }
	      if (file.isDirectory()) {
	    	  if(countDirectories) {
	    		  count++;
	    	  }
	          count += countFilesInDirectory(file, countDirectories);
	      }
	  }
	  return count;
	}

	public static int countFilesUntilLimit(File directory, boolean countDirectories, long maxAdmitted) {
		int count = 0;
		for (File file : directory.listFiles()) {

			if (file.isFile()) {
				count++;

			} else if (file.isDirectory()) {
				if(countDirectories) {
					count++;
				}
				count += countFilesUntilLimit(file, countDirectories, maxAdmitted);
				if(count > maxAdmitted)
					return count;
			}
		}
		return count;
	}

	public static boolean checkIfContainsMoreFilesThanMax(File directory, boolean countDirectories, long maxAdmitted) {
		return countFilesUntilLimit(directory, countDirectories, maxAdmitted + 1) > maxAdmitted;
	}

	public static String[] filesToPaths(List<File> files) {
		String[] filePaths = new String[files != null ? files.size() : 0];
		if(files != null) {
			for(int i = 0; i < files.size(); i++) {
				filePaths[i] = FileVarious.getCanonicalPathSafe(files.get(i));
			}
		}

		return filePaths;
	}

	public static List<String> filesToPathList(List<File> files){
		List<String> filePaths = new ArrayList<>();
		if(files != null) {
			for(File file : files) {
				filePaths.add(FileVarious.getCanonicalPathSafe(file));
			}
		}

		return filePaths;
	}

	public static File[] getFileArrayFromStrings(String[] filePaths){
		logger.debug("ARGUMENT FILES:" + Arrays.toString(filePaths));

		File[] argumentsFiles = new File[filePaths.length];
		int j = 0;
		for(String s : filePaths) {
			if (!END_ARGS_SECTION.equals(s)) {
				argumentsFiles[j] = new File(s);
			}else {
				break;
			}
			j++;
		}
		return argumentsFiles;
	}

	public static List<String> getStringListFromFiles(List<File> filePaths){

		List<String> argumentsFiles = new ArrayList<>();
		for(File s : filePaths) {
			if (!END_ARGS_SECTION.equals(s)) {
				argumentsFiles.add(getCanonicalPathSafe(s));
			}else {
				break;
			}
		}
		return argumentsFiles;
	}

	public static String getStringFromFiles(List<File> filePaths, String separator){

		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < filePaths.size(); i++) {
			File s = filePaths.get(i);
			builder.append(FileVarious.getCanonicalPathSafe(s));
			if (i < filePaths.size() - 1)
				builder.append(separator);
		}

		return builder.toString();
	}

	/**
	 * This method delete all the content from the file located at given String path, without deleting the file.
	 * @param filePath this is the path - relative or absolute - used to locate target file
	 * @return true if file is successfully emptied, false in the other cases
	 */
	public static boolean clearFileContent(String filePath) {
		PrintWriter writer;
		boolean esit = false;
		try {
			writer = new PrintWriter(filePath);
			writer.print("");
			esit = true;
			writer.close();

		} catch (FileNotFoundException e) {
			esit = false;
			logger.error("FILE NOT CLEARED!", e);
		}
		return esit;
	}

	/**
	 * This method ensures that the given String name is not null or made by only dots, and then checks if it has an extension.
	 *
	 * If no extension is found, then the fallback one will be used creating a valid fileName.
	 *
	 * If an invalid extension is provided, "txt" will be used as default one.
	 *
	 */
	public static String ensureValidExtension(String name, String extensionFallback) {
		if(name == null) {
			return "";
		} else {

			if (extensionFallback != null) {
				extensionFallback.replaceAll("[.]", "");
				if("".equals(extensionFallback.trim())) {
					extensionFallback = "txt";
				}
			} else {
				extensionFallback = "txt";
			}
		}

		String newNameClearedDots = name.replaceAll("[.]", "").trim();
		String correctFallbackExtension = extensionFallback.toLowerCase();
		name = (!newNameClearedDots.equals(""))? name : "NewFile." + correctFallbackExtension;
		String ext = FilenameUtils.getExtension(name);
		name = (ext != null && !"".equals(ext.trim())) ? name : name.endsWith(".") ? name + correctFallbackExtension : name + "." + correctFallbackExtension;

		return name;
	}

	/**
	 * Change extension to the given one and return the new file reference. Be careful, the returned file is not physically created!
	 * NB the extension must be preceeded by a dot
	 */
	public static File changeExtension(File file, String newExtension) {
		int extensionLenght = (FilenameUtils.getExtension(file.getName())).length();
		String noExt="";
		if(extensionLenght==0) {
			noExt = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-(extensionLenght));
		}else {
			noExt = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-(extensionLenght+1));
		}

		file= new File(noExt + newExtension);
		logger.debug("NEW NAME: "+file.getAbsolutePath());
		return file;
	}

	/**
	 * Get a renamed file in same directory, without creating real file but creating only a java object
	 * @throws Exception
	 */
	public static File getRenamed(File toRename, String newName, boolean unique) throws Exception {
		File parent = toRename.getParentFile();
		File renamed = new File(parent.getCanonicalPath() + File.separator + newName);

		return unique ? uniqueJavaObjFile(renamed) : renamed;
	}

	public File replaceDirOccurrencesInPath(File toCheck, File dirToReplace, String newDirName) throws Exception {

		if (toCheck != null && dirToReplace != null && dirToReplace.isDirectory()) {
			File parent = toCheck;
			String endTemp = File.separator + toCheck.getName();
			while ((parent = parent.getParentFile()) != null) {
				if (parent.getAbsoluteFile().equals(dirToReplace.getAbsoluteFile())) {
					String newDir = dirToReplace.getParentFile().getCanonicalPath() + File.separator + newDirName;
					return new File(newDir + endTemp);
				}

				endTemp = parent.getName() + File.separator + endTemp;
			}
		}

		return toCheck;
	}

	/**
	 * Change extension to the given one and return the new file reference. Be careful, the returned file is not physically created!
	 * NB newExtension must not be preceeded by a dot
	 * NB defaultExtension not must be preceeded by a dot
	 */
	public static File changeExtensionForceDot(File file, String newExtension) {

		if(newExtension == null || file == null) {
			return file;
		}

		String adapted = newExtension.replaceAll("[.]", "");

		return changeExtension(file, "." + adapted);
	}

	public static String getNAncestorsPath(File file, int nLevels) {
		String result = "";
		if (file != null) {
			result = result.concat(file.getName());
			File clone = new File(file.getAbsolutePath());
			while ((nLevels -1) >= 0 && clone.exists()) {
				String father = clone.getParentFile().getName();
				int separatorPos = clone.getAbsolutePath().length() - clone.getName().length();
				String separator = clone.getAbsolutePath().substring(separatorPos-1, separatorPos);
				result = father.concat(separator).concat(result);
				clone = clone.getParentFile();
				nLevels--;
			}
		}
		return result;
	}

	/**
	 * check if a file/folder is contained in a given suspect parent dir.
	 * @param fileToCheck
	 * @param suspectParentFolder
	 * @return true if fileToCheck is contained suspectParentFolder, false if not
	 */
	public static boolean checkIfContainedInDir(File fileToCheck, File suspectParentFolder) {
		boolean areRelated = true;
		try {
			areRelated = fileToCheck.getCanonicalPath().contains(suspectParentFolder.getCanonicalPath() + File.separator);
		} catch (IOException e) {
			logger.error("", e);
		}
	    return areRelated;
	}

	public static boolean containedInList(List<File> files, File match) {
		if(files != null) {
			for(File file : files) {
				try {
					if(getCanonicalPathSafe(file).equals(match))
						return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return false;
	}

	public static File searchUntilAncestorMatch(File son, String wantedParentMatch, boolean exactNameMatch) {
		File scanner = son;
		while(scanner != null && scanner.getParentFile() != null) {
			scanner = scanner.getParentFile();
			boolean found =
					(exactNameMatch && scanner.getName().equals(wantedParentMatch)) ||
					(!exactNameMatch && scanner.getName().contains(wantedParentMatch));
			if(found) {
				return scanner;
			}
		}

		return son;
	}

	public static boolean hasParent(File son, String parentName, boolean exactNameMatch) {
		File scanner = son;
		while(scanner != null && scanner.getParentFile() != null) {
			scanner = scanner.getParentFile();
			boolean found =
					(exactNameMatch && scanner.getName().equals(parentName)) ||
					(!exactNameMatch && scanner.getName().contains(parentName));
			if(found) {
				return true;
			}
		}

		return false;
	}

	public static File getNlevelsAncestor(File son, int levelsUp) {

		if(son ==  null || son.getParentFile() == null) {
			logger.warn("\n\n=> Parent file search result FAILED! [son:"+son);
			return son;
		}

		int originalLevels = levelsUp;
		try {
			File result = null;
			if (son != null) {
				result = son.getParentFile().getAbsoluteFile();
				if (levelsUp > 0) {
					if (levelsUp == 0) {
						levelsUp = 1;
					}

					while (levelsUp > 1) {
						if (result.getParentFile() == null || !result.getParentFile().getAbsoluteFile().exists()) {
							break;
						}
						result = result.getParentFile().getAbsoluteFile();
						levelsUp--;
					}
				}

				logger.debug("\n\n=> Parent file search result [son:"+son.getCanonicalPath()
					+", levelsUp:"+originalLevels+"]: "+result.getCanonicalPath()+"\n");

			}

			return result;
		} catch (Exception e) {
			logger.error("Error finding parent file, returning given son", e);
			return son;
		}
	}

	public static boolean doesAncestorNlevelsExist(File currentFile, int ancestorLevel) {
		if(currentFile == null || !currentFile.exists()) {return false;}

		File ancestor = getNlevelsAncestor(currentFile, ancestorLevel);
		return (ancestor != null && ancestor.exists() && !ancestor.equals(currentFile));
	}

	public static String getNlevelsPathString(File son, int levelsUp) {
		int originalLevels = levelsUp;
		String result = "";
		try {
			if (son != null && son.exists()) {
				File temp = son.getCanonicalFile();
				if (levelsUp > 0) {
					if (levelsUp == 0) {
						levelsUp = 1;
					}

					while (levelsUp > 0) {
						result = temp.getName() + File.separator + result;
						if (temp.getParentFile() == null || !temp.getParentFile().exists()) {
							break;
						}
						temp = temp.getParentFile();
						levelsUp--;
					}
					if (result.length() > 0) {
						result = result.substring(0, result.length() - 1);
					}
				}
				logger.debug("\n\n=> Parent file toStringPath search result [son:" + son.getCanonicalPath() + ", levelsUp:" + originalLevels + "]: " + result + "\n");
			}
			return result;
		} catch (Exception e) {
			logger.error("Error finding parent file, returning given son", e);
			return result;
		}
	}

	/**
	 * this method change name from the given file absolute path, until the result is a non existent file
	 * It do that adding (i) numbers at the end of the file name, just before the start of the
	 * extension .
	 * <Strong>Note that the given File variable won't change</Strong>
	 *
	 * @param file
	 * @return the new unique path name in String format
	 */
	public static String getUniqueNameFile(File file) {
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
		return fileName;
	}

	/**
	 * this function rename a file until it has a unique name not existing yet, putting incrementally
	 * an integer value starting to 0 (even if the file does not exist but have no index at the beginning )
	 * at the start of the fileName, placing a string separator between it and the original fileName
	 *
	 * @param file the source file to check for unique name
	 * @param separator the string separator to put after file incremental number
	 * @return the file renamed - NB the original File parameter will not be renamed !
	 */
	public static File renameJavaObjFileAtStart(File file, String separator) {

		String fileName=file.getPath();

		String first = fileName.split(separator)[0];
		try {
			Integer.parseInt(first);
		}catch(NumberFormatException e) {
			first = "0";
			FileWorker.logger.error("Exception happened!", e);
		}

		int counter = 1;

		File f = null;

		while(file.exists()){

			counter++;
		}

		f = new File(counter+separator+fileName);

		return f;
	}

	/**
	 * Get a string without unsupported characters for files extensions. This actually means only a-z or 0-9 digits are admitted (ignoring case)
	 * extension can both contain part of the end of file's name, method will recognize to mantain dot or not based on last dot index
	 * @param extension the given extension to filter out removing illegal chars
	 * @return the cleaned string
	 */
	public static String removeIllegalCharsFromExtension(String extension) {
		String extensionIncipit = "";
		String dot = ".";
		String extensionAfterDot = "";
		int dotIndex = extension.lastIndexOf(".");
		if(dotIndex < 0) {
			dotIndex = 0;
			dot = "";
		}
		extensionIncipit = extension.substring(0, dotIndex);
		extensionAfterDot = extension.substring(dotIndex).replaceAll("[^a-zA-Z0-9]", "");

		return extensionIncipit.concat(dot).concat(extensionAfterDot);
	}

	/**
	 * Checks if the root of the given path is defined in current file system
	 * @param path
	 * @return
	 */
	public static boolean isPathRootValid(String path) {
		// Root path validation
		boolean rootOK = false;
		for (File curr : File.listRoots()) {
			// check for every existing root until one matches
			if (path.startsWith(curr.getAbsolutePath())) {
				rootOK = true;
				break;
			}
		}

		return rootOK;
	}

	public static boolean isFileSonOfDir(File file, File dir) {

		if(file == null || dir == null) return false;

		File tempUpperDir;
		try {
			tempUpperDir = file.getCanonicalFile().getParentFile();
			dir = dir.getCanonicalFile();
		} catch (IOException e) {
			tempUpperDir = file.getParentFile();
		}

		while(tempUpperDir != null) {

			if(dir.equals(tempUpperDir)) {
		    	return true;
		    }

		    tempUpperDir = tempUpperDir.getParentFile();
		}

		return false;
	}
}
