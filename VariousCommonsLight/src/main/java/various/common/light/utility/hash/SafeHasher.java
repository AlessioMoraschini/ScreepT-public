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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 
 * @author Alessio Moraschini
 * 
 * This class is useful when hashing a secured data to hide the original password, useful to avoid plain-text password backward discovery vulnerability.
 * 
 * This is also useful when you have to store securely sensitive data such as passwords. 
 * Remeber that salt used, iterationsLevel and algorithm must be the same to retrieve the same value when hashing
 * 
 * <b>NB:</>Compatible only for java 1.8 and more if algorithm used is "PBKDF2WithHmacSHA512"
 *
 */
public class SafeHasher {
	
	private static String algorithmSha512 = "PBKDF2WithHmacSHA512";
	private static String algorithmSha1 = "PBKDF2WithHmacSHA1";
	
	private static SecretKeyFactory secretKeyfactorySHA512;
	private static SecretKeyFactory secretKeyfactorySHA1;

	/**
	 * Hash the psw bytes and return the concatenation of salt (16 bytes) + hashed passwords
	 * @param psw the source psw to hash
	 * @param keyLength is the length in bits for the result hashed psw
	 * 
	 * @return the hashed result with same length as the psw length, composed by either the used random hash and the hashed result
	 * 
	 * <b>NB:</> In order to work correctly, you have to use same salt (you'll find it in returned hashDTO), security level and algorithm 
	 *  that were originally used, also when rehashing-back
	 *  
	 * <b>NB:</> PBKDF2SHA512 supported only for java 1.8+
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public static HashDTO hashPsw(char[] psw, IterationsLevel security, HashAlgorithm algorithm)
				throws NoSuchAlgorithmException, InvalidKeySpecException {
		
		byte[] salt = generateSecureRandom(16);
		KeySpec spec = new PBEKeySpec(psw, salt, security.iterations, psw.length * 8);
		byte[] hashed = getKeyFactory(algorithm).generateSecret(spec).getEncoded();
		byte[] result = fillArrayFromOtherArray(hashed, psw.length);
		
		return new HashDTO(salt, result);
	}

	/**
	 * Hash the psw bytes with a given salt and return the hashed result
	 * @param psw the source psw to hash
	 * 
	 * @return the hashed result with same length as the psw length, composed by either the used random hash and the hashed result
	 * 
	 * <b>NB:</> In order to work correctly, you have to use same salt, security level and algorithm originally used also when rehashing-back
	 * <b>NB:</> PBKDF2SHA512 supported only for java 1.8+
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public static byte[] hashPswWithGivenSalt(char[] psw, byte[] salt, IterationsLevel security, HashAlgorithm algorithm)
				throws NoSuchAlgorithmException, InvalidKeySpecException {
		
		KeySpec spec = new PBEKeySpec(psw, salt, security.iterations, psw.length * 8);
		byte[] hashed = getKeyFactory(algorithm).generateSecret(spec).getEncoded();
		byte[] result = fillArrayFromOtherArray(hashed, psw.length);
		
		return result;
	}
	
	private static SecretKeyFactory getKeyFactory(HashAlgorithm algoritm) throws NoSuchAlgorithmException {
		
		if(HashAlgorithm.PBKDF2SHA512.equals(algoritm)) {
			secretKeyfactorySHA512 = secretKeyfactorySHA512 != null ? secretKeyfactorySHA512 : SecretKeyFactory.getInstance(algorithmSha512);
			return secretKeyfactorySHA512;
		} else {
			secretKeyfactorySHA1 = secretKeyfactorySHA1 != null ? secretKeyfactorySHA1 : SecretKeyFactory.getInstance(algorithmSha1);
			return secretKeyfactorySHA1;
		}
	}
	
	/**
	 * Utility method to convert byte array to equivalent char array format. this is useful if you want to apply hashing for a byte array source
	 */
	public static char[] byteArrayToChar(byte[] source) {
		
		char[] result = new char[source.length];
		for(int i = 0; i < source.length; i++) {
			result[i] = (char) source[i]; 
		}
		
		return result;
	}

	/**
	 * Utility method to convert byte array to equivalent char array format. this is useful if you want to apply hashing for a byte array source
	 */
	public static byte[] charArrayToByte(char[] source) {
		
		byte[] result = new byte[source.length];
		for(int i = 0; i < source.length; i++) {
			result[i] = (byte) source[i]; 
		}
		
		return result;
	}
	
	public static byte[] generateSecureRandom(int length) {
		
		SecureRandom sRand = new SecureRandom();
		byte[] iv = new byte[length];
		sRand.nextBytes(iv);
		
		return iv;
	}
	
	/**
	 * Iterative way to fill a given array of outLength positions using a source array
	 * (when elements from it finish filling continues until outLength has been reached, restarting from the 0 position of source to avoid going out of bounds)
	 * @param source
	 * @param outLength
	 * @return
	 */
	public static byte[] fillArrayFromOtherArray(byte[] source, int outLength) {
		
		byte[] destination = new byte[outLength]; 
		for(int i = 0; i < outLength; i++) {
			int j = i % (source.length);
			destination[i] = source[j];
		}
		
		return destination;
	}
	
	
	// ENUMS PARAMS //
	
	public enum IterationsLevel {
		ULTRA(200000),
		VERY_HIGH(80000),
		HIGH(30000),
		MED(10000),
		MED_LOW(4000),
		LOW(1000);
		
		public int iterations;
		
		private IterationsLevel(int iterations) {
			this.iterations = iterations;
		}
	}

	public enum HashAlgorithm {
		PBKDF2SHA512(algorithmSha512),
		PBKDF2SHA1(algorithmSha1);
		
		public String value;
		
		private HashAlgorithm(String value) {
			this.value = value;
		}
	}
	
}
