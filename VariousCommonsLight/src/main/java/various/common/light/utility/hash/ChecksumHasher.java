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
package various.common.light.utility.hash;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.properties.PropertiesManager;

public class ChecksumHasher {

	public static SafeLogger logger = new SafeLogger(ChecksumHasher.class);

	/**
	 * Use only in case of a small file, cause this reads all the bytes from source into memory,
	 * potentially leading to an outOfMemoryException. It's preferrable to use <i>getFileHashBuffered()</i> method.
	 */
	@Deprecated
	public static String getFileHash(File file, HashingFunction algorithm, boolean uppercase) throws Throwable {
		
		if(file == null) return "";
	
		logger.info("Generating hash for file: " + file.getAbsolutePath() + " with algorithm:" + algorithm);
		
		MessageDigest messageDigest = MessageDigest.getInstance(algorithm.algorithmCode);
		messageDigest.update(Files.readAllBytes(file.toPath()));
		byte[] digest = messageDigest.digest();
		
		String result;
		try {
			// NB: hex is not case sensitive!
			result = ConversionUtils.printHexBinary(digest);
			result = (uppercase) ? result.toUpperCase() : result.toLowerCase();
		} catch (Error e) {
			result = "";
		}
		
		logger.info("Generated hash for file: " + file.getAbsolutePath() + ", HASHED:"+result);
		
		return result;
	}
	
	public static String getFileHash(String filePath, HashingFunction algorithm) throws Throwable {
		
		if(filePath == null) return "";
		
		logger.info("Generating hash for file: " + filePath + " with algorithm:"+algorithm);
	         
	    String result = getFileHashBuffered(new File(filePath), algorithm, true);
	    
	    logger.info("Generated hash for file: " + filePath + ", HASHED:"+result);
	    
	    return result;
	}
	
	/**
	 * Use this when reading a big file, cause normal <i>getFileHash()</> can cause a outOfMemoryException 
	 */
	public static String getFileHashBuffered(String file, HashingFunction algorithm) throws NoSuchAlgorithmException {
		
		return getFileHashBuffered(new File(file), algorithm, true);
	}

	/**
	 * Use this when reading a big file, cause normal <i>getFileHash()</> can cause a outOfMemoryException 
	 */
	public static String getFileHashBuffered(String file, HashingFunction algorithm, boolean uppercase) throws NoSuchAlgorithmException {
	
		return getFileHashBuffered(new File(file), algorithm, uppercase);
	}
	
	/**
	 * Use this when reading a big file, cause normal <i>getFileHash()</> can cause a outOfMemoryException 
	 */
	public static String getFileHashBuffered(File file, HashingFunction algorithm, boolean uppercase) throws NoSuchAlgorithmException {
		MessageDigest messageDigest;
	    try {
	    	messageDigest = MessageDigest.getInstance(algorithm.algorithmCode); 
	    } catch (NoSuchAlgorithmException ex) {
	    	logger.error("Unsupported algorithm "+ex);
	        throw ex;
	    }
	    
	    try(FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
	        for(long pos = 0, rem = fileChannel.size(), chunk; rem > pos; pos += chunk) {
	            chunk = Math.min(Integer.MAX_VALUE, rem-pos);
	            messageDigest.update(fileChannel.map(FileChannel.MapMode.READ_ONLY, pos, chunk));
	        }
	    } catch(Exception e){
	        logger.error("Something has gone wrong while hashing file: " + file, e);
	        return "";
	    }
	    
	    String result = ConversionUtils.printHexBinary(messageDigest.digest());
	    result = (uppercase) ? result.toUpperCase() : result.toLowerCase();
	    
	    return result;
	}

	public static String getFolderHash(File directory, HashingFunction algorithm, boolean uppercase) throws Throwable {
		if(directory == null) {
			return "";
		} else if (directory.isFile()) {
			return getFileHashBuffered(directory, algorithm, uppercase);
		} else {
			byte[] hashAccumulator = new byte[0];
			File[] contents =  directory.listFiles();
			
			if(contents != null && contents.length > 0) {
				for (File curr : contents) {
					if (curr.isDirectory()) {
						hashAccumulator = ChecksumHasher.xorBytes(getFolderHash(curr, algorithm, uppercase).getBytes("UTF-8"), hashAccumulator);
					} else {
						hashAccumulator = ChecksumHasher.xorBytes(getFileHashBuffered(curr, algorithm, uppercase).getBytes("UTF-8"), hashAccumulator);
					}
				}
			
			} else {
				hashAccumulator = ChecksumHasher.xorBytes(getFileHashBuffered(directory, algorithm, uppercase).getBytes("UTF-8"), hashAccumulator);
			}
			
			return ConversionUtils.printHexBinary(hashAccumulator);
		}
	}

	public static byte[] xorBytes(byte[] first, byte[] second) {
		
		// Select longest as first
		byte[] A = first.length >= second.length ? first : second;
		byte[] B = A.length == first.length ? second : first;
		byte[] xorResult = new byte[A.length];
	
		int i = 0;
		for (byte firstiByte : A) {
			xorResult[i] = (i < B.length) ? (byte) (firstiByte ^ B[i]) : firstiByte;
			i++;
		}
		
		return xorResult;
	}

	//Add salt
	public static byte[] getSalt() throws NoSuchAlgorithmException
	{
	    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
	    byte[] salt = new byte[16];
	    sr.nextBytes(salt);
	    return salt;
	}
	
	
	public static boolean equalsFilesChecksum(File A, File B, HashingFunction algorithm) throws Throwable {
		
		String hashA = ChecksumHasher.getFileHashBuffered(A, algorithm, true);
		String hashB = ChecksumHasher.getFileHashBuffered(B, algorithm, true);
		
		return hashA.equals(hashB);
	}

	public static boolean checkFileHash(String filePath, String checksum, HashingFunction algorithm) throws Throwable {
		
		if(filePath == null || checksum == null) return false;
		
		String myChecksum = getFileHashBuffered(filePath, algorithm, true);
		
		return myChecksum.equalsIgnoreCase(checksum) || checksum.toUpperCase().contains(myChecksum);
	}

	public static boolean checkFileHash(File file, String checksum, HashingFunction algorithm) throws Throwable {
		
		if(file == null || checksum == null) return false;
		
		String myChecksum = getFileHashBuffered(file, algorithm, true);
		
		return myChecksum.equalsIgnoreCase(checksum) || checksum.toUpperCase().contains(myChecksum);
	}

	public static boolean checkHashesFromProperties(List<File> filesToCheck, PropertiesManager propsManifest, HashingFunction hashingFunction) throws Throwable {
		if(filesToCheck == null || filesToCheck.isEmpty()) {
			return true;
		}
		
		boolean checked = true;
		String checksum = "";
		boolean tempResult = false;
		
		for(File current : filesToCheck) {
			checksum = propsManifest.getProperty(current.getName());
			if (checksum != null && !"".equals(checksum.trim())) {
				tempResult = checkFileHash(current, checksum, hashingFunction);
				checked = tempResult == false ? false : checked;
			}
		}
		
		return checked;
	}

	public static boolean checkHashFromProperties(File toCheck, PropertiesManager propsManifest, HashingFunction hashingFunction) throws Throwable {
		if(propsManifest == null || toCheck == null || !toCheck.exists()) {
			return false;
		} else {
			String checksum = propsManifest.getProperty(toCheck.getName());
			if (checksum != null && !"".equals(checksum.trim())) {
				// If checksum defined then check current file for it
				return checkFileHash(toCheck, checksum, hashingFunction);
			} else {
				// If reference checksum not defined continue
				return true;
			}
		}
	}
	
}
