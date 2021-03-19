package utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class Generator {

	// SECURE INTEGER GENERATOR
	public static int secRand(boolean verySecure) throws NoSuchAlgorithmException{
		SecureRandom secureGen;
		secureGen = (verySecure)? SecureRandom.getInstanceStrong() : new SecureRandom();
		return secureGen.nextInt();
	}
	
	// INTEGER ARRAY GENERATOR
	public static int[] randomIntArray (int dim){
		Random rand = new Random();
		int[]res = new int[dim];
		for(int i=0 ; i<res.length-1; i++){
			res[i] = rand.nextInt(Integer.MAX_VALUE)-rand.nextInt(Integer.MAX_VALUE) ;
		}
		return res; 
	}
	
	public static String getRandomString(int length) {
		byte[] bytes = getRandomBytes(length);
		bytes = Base64.getEncoder().encode(bytes);
		String randomString = new String(bytes);
		
		if(randomString.length() > length) {
			return randomString.substring(0,length);
		} else {
			return String.format("%" + length + "s", randomString);
		}
	}
	
	// HASHMAP GENERATOR
	public static Map<Integer, Integer> getRandIntMap(int dim){
		Map<Integer, Integer> map = new LinkedHashMap<Integer, Integer>();
		Random rand = new Random();
		
		for(int i=dim-1 ; i>=0; i--){
			Integer val = rand.nextInt(Integer.MAX_VALUE)-rand.nextInt(Integer.MAX_VALUE) ;
			map.put(i, val);
		}
		
		return map;
	}
	
    // byte array source seq generator
    public static byte[] getRandomBytes(int dim) {
		byte[] srcBytes = new byte[dim];
    	for(int i = 0; i<srcBytes.length; i++) {
    		srcBytes[i] = (byte)i;
		}
    	return srcBytes;
	}
}
