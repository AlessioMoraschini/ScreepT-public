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
package various.common.light.utility.manipulation;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class EntrophyzerUtils {

	/**
	 * This method sort a map according to keys contained
	 */
	public static TreeMap<Integer, Integer> sortMapByKey(Map<Integer, Integer> map){
        TreeMap<Integer, Integer> treeMap = new TreeMap<Integer, Integer>(map);

		return treeMap;
	}

	public static <E extends Comparable<E>, T> TreeMap<E, T> sortMapByKeyGeneric(Map<E, T> map){
		TreeMap<E, T> treeMap = new TreeMap<E, T>(map);
		
		return treeMap;
	}
	
	/**
	 * This method sort a map according to values
	 * @param map
	 * @return
	 */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
    
	/**
	 * This method creates an hashmap containing indexes as key, and corresponding int values from keyArray 
	 * @param keyArray
	 * @return
	 */
	public static Map<Integer, Integer> arrayToIndexedMap(int[] valuesArray){
		Map<Integer, Integer> map = new LinkedHashMap<Integer, Integer>();
		
		for(int i=0 ; i<valuesArray.length; i++){
			map.put(i, valuesArray[i]);
		}
		
		return map;
	}
	
	/**
	 * This method creates an hashmap containing indexes as key, and corresponding generic Type values from keyArray 
	 * @param keyArray
	 * @return
	 */
	public static<E> Map<Integer, E> arrayToIndexedMap(E[] valuesArray){
		Map<Integer, E> map = new LinkedHashMap<Integer, E>();
		
		for(int i=0 ; i<valuesArray.length; i++){
			map.put(i, valuesArray[i]);
		}
		
		return map;
	}
	
	/**
	 * Integer keys map-array mapper
	 * @param keyArray
	 * @return
	 */
	public Map<Integer, Integer> arrayToIndexedIntMap(int[] keyArray){
		Map<Integer, Integer> map = new LinkedHashMap<Integer, Integer>();
		
		for(int i=0 ; i<keyArray.length; i++){
			map.put(i, keyArray[i]);
		}
		
		return map;
	}
	
	/////////////////////////////////////////////////////////////////////////////// CORE METHODS

	/**
	 * This method sorts the given array of bytes using the order of the given keys
	 * @param source
	 * @param keys
	 * @return
	 */
	public static byte[] orderArrayByKey(byte[] source, int[] keys ) {
    	int srcLength = source.length;
    	int N = keys.length;
		byte[] result = new byte[srcLength];
		Map<Integer, Integer> keysMap = arrayToIndexedMap(keys);
		Map<Integer, Integer> valSortedMap = sortMapByValue(keysMap);
		Integer[] indexSortedArray = new Integer[N];
		indexSortedArray = valSortedMap.keySet().toArray(indexSortedArray);
		int progr = 0;
		
		boolean end = false;
		
		while(!end) {
			
			if(progr+N <= srcLength) {
				// if there is space in result to use full keys array
				
				// update current portion of source bytes
				byte[] sourcePortion = new byte[N];
				System.arraycopy(source, progr, sourcePortion, 0, N);
				// reorder portion using indexes calculated from keys and copy in new section of result
				sourcePortion = orderFromIndexes(sourcePortion, indexSortedArray);
				System.arraycopy(sourcePortion, 0, result, progr, sourcePortion.length);
				progr += N;
				
			}else {
				int subLength = srcLength-progr;
				// update current portion of source bytes
				byte[] sourcePortion = new byte[subLength];
				System.arraycopy(source, progr, sourcePortion, 0, subLength);
				// extract residual elements number from indexes array
				indexSortedArray = new Integer[subLength];
				keysMap = arrayToIndexedMap(Arrays.copyOfRange(keys, 0, subLength));
				valSortedMap = sortMapByValue(keysMap);
				indexSortedArray = valSortedMap.keySet().toArray(indexSortedArray);
				// reorder portion using indexes calculated from keys and copy in new section of result
				sourcePortion = orderFromIndexes(sourcePortion, indexSortedArray);
				System.arraycopy(sourcePortion, 0, result, progr, subLength);

				end = true;
			}
		}
		
    	return result;
    }
    
    public static byte[] reorderBackArrayByKey(byte[] ordered, int[] keys) {
    	int srcLength = ordered.length;
    	int N = keys.length;
		byte[] result = new byte[srcLength];
		Map<Integer, Integer> keysMap = arrayToIndexedMap(keys);
		Map<Integer, Integer> valSortedMap = sortMapByValue(keysMap);
		Integer[] indexSortedArray = new Integer[N];
		indexSortedArray = valSortedMap.keySet().toArray(indexSortedArray);
		int progr = 0;
		
		boolean end = false;
		
		while(!end) {
			
			if(progr+N <= srcLength) {
				// if there is space in result to use full keys array
				
				// update current portion of source bytes
				byte[] sourcePortion = new byte[N];
				System.arraycopy(ordered, progr, sourcePortion, 0, N);
				// reorder portion using indexes calculated from keys and copy in new section of result
				sourcePortion = reorderBackFromIndexes(sourcePortion, indexSortedArray);
				System.arraycopy(sourcePortion, 0, result, progr, sourcePortion.length);
				progr += N;
				
			}else {
				int subLength = srcLength-progr;
				// update current portion of source bytes
				byte[] sourcePortion = new byte[subLength];
				System.arraycopy(ordered, progr, sourcePortion, 0, subLength);
				// extract residual elements number from indexes array
				indexSortedArray = new Integer[subLength];
				keysMap = arrayToIndexedMap(Arrays.copyOfRange(keys, 0, subLength));
				valSortedMap = sortMapByValue(keysMap);
				indexSortedArray = valSortedMap.keySet().toArray(indexSortedArray);
				// reorder portion using indexes calculated from keys and copy in new section of result
				sourcePortion = reorderBackFromIndexes(sourcePortion, indexSortedArray);
				System.arraycopy(sourcePortion, 0, result, progr, subLength);

				end = true;
			}
		}
		
    	return result;
    }
    ////////////////////////////////////////////////////////////////////////////// END CORE METHODS
	
	/**
	 * This method order the byte source according to unique indexes defined in <strong>indexes</strong>.
	 * @param source
	 * @param indexes
	 * @return null if length of source differ from indexes length
	 */
	private static byte[] orderFromIndexes(byte[] source, Integer[] indexes) {
    	if(source.length!=indexes.length) {
    		return null;
    	}
    	
    	byte[] result = new byte[indexes.length];

    	for(int i=0; i<indexes.length; i++) {
    		result[indexes[i]] = source[i];
    	}
    	
    	return result;
    }
    
	/**
	 * This method order the byte source back to original according to unique indexes defined in <strong>indexes</strong>.
	 * @param source
	 * @param indexes
	 * @return null if length of source differ from indexes length
	 */
    private static byte[] reorderBackFromIndexes(byte[] ordered, Integer[] indexes) {
    	if(ordered.length!=indexes.length) {
    		return null;
    	}
    	
    	byte[] result = new byte[indexes.length];

    	for(int i=0; i<indexes.length; i++) {
    		result[i] = ordered[indexes[i]];
    	}
    	
    	return result;
    }
    
    /**
	 * This method order the Generic type source array according to unique indexes defined in <strong>indexes</strong>.
	 * @param source
	 * @param indexes
	 * @return null if length of source differ from indexes length
	 */
    public static<E> E[] orderFromIndexes(E[] source, Integer[] indexes) {
    	if(source.length!=indexes.length) {
    		return null;
    	}
    	
    	E[] result = source.clone();

    	for(int i=0; i<indexes.length; i++) {
    		result[indexes[i]] = source[i];
    	}
    	
    	return result;
    }
    
    /**
	 * This method order back to original the generic type source according to unique indexes defined in <strong>indexes</strong>.
	 * @param source
	 * @param indexes
	 * @return null if length of source differ from indexes length
	 */
    public static<E> E[] reorderBackFromIndexes(E[] ordered, Integer[] indexes) {
    	if(ordered.length!=indexes.length) {
    		return null;
    	}
    	
    	E[] result = ordered.clone();

    	for(int i=0; i<indexes.length; i++) {
    		result[i] = ordered[indexes[i]];
    	}
    	
    	return result;
    }
    
    public static Integer[] convertCharsToInteger(char[] source) throws InputMismatchException {
    	if(source == null) {
    		throw new InputMismatchException("Source characters array is null!");
    	}
    	
    	Integer[] output = new Integer[source.length];
    	for(int i = 0; i < source.length; i++) {
    		output[i] = (int)source[i]; 
    	}
    	
    	return output;
    }

    public static int[] convertCharsToInt(char[] source) throws InputMismatchException {
    	if(source == null) {
    		throw new InputMismatchException("Source characters array is null!");
    	}
    	
    	int[] output = new int[source.length];
    	for(int i = 0; i < source.length; i++) {
    		output[i] = (int)source[i]; 
    	}
    	
    	return output;
    }
    
    public static int[] byteArrayToInt(byte[] source) {
    	if(source == null) {
    		throw new InputMismatchException("Source byte array is null!");
    	}
    	
    	int[] output = new int[source.length];
    	for(int i = 0; i < source.length; i++) {
    		output[i] = (int)source[i]; 
    	}
    	
    	return output;
    }
    
	public static byte[] convertCharsToBytes(char[] sourceChar, int lengthLimit) {
		CharBuffer chBuff = CharBuffer.wrap(sourceChar);
		ByteBuffer byBuff = Charset.forName("UTF-8").encode(chBuff);
		byte[] bytes = Arrays.copyOfRange(byBuff.array(), 0, lengthLimit);
		Arrays.fill(byBuff.array(), (byte)0); // clear array data
		
		return bytes;
	}
    
}
