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
package utility.bytes;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ByteWorker {

	/**
	 * DEFAULT CONSTRUCTOR - not yet useful for any method
	 */
	public ByteWorker() {
		
	}
	
	// METHODS 
	
	/**
	 * this method is created to use standard-safe way to convert from string to array of bytes.
	 * @param srcString the source string
	 * @return the byte array equivalent to srcString
	 */
	public static byte[] strToByteArray(String srcString) {
		return srcString.getBytes();
	}
	
	/**
	 * this method is created to use standard-safe way to convert from string to array of bytes.
	 * @param srcString the source string
	 * @param charset the charset to use during conversion
	 * 
	 * @return the byte array equivalent to srcString
	 * 
	 * @throws UnsupportedEncodingException if the charset is not a valid one (use ListCharset class to get a list of available charsets) 
	 */
	public static byte[] strToByteArray(String srcString, String charset) throws UnsupportedEncodingException {
		return srcString.getBytes(charset);
		
	}
	
	/**
	 * this method is created to use standard-safe way to convert from string to arrayList of Byte.
	 * @param srcString the source string
	 * @return the Byte arrayList equivalent to srcString
	 */
	public static ArrayList<Byte> strToByteList(String srcString) {

		byte[] codedBytes = srcString.getBytes();
		ArrayList<Byte> byteList = new ArrayList<Byte>();

		// translate byte[] to ArrayList<Byte>
		for(byte currentByte : codedBytes) {
			byteList.add(currentByte);
		}
		
		return byteList;
		
	}
	
	/**
	 * this method is created to use standard-safe way to convert from string to arrayList of Byte.
	 * @param srcString the source string
	 * @return the Byte arrayList equivalent to srcString
	 * @throws UnsupportedEncodingException 
	 */
	public static ArrayList<Byte> strToByteList(String srcString, String charset) throws UnsupportedEncodingException {

		byte[] codedBytes = srcString.getBytes(charset);
		ArrayList<Byte> byteList = new ArrayList<Byte>();

		// translate byte[] to ArrayList<Byte>
		for(byte currentByte : codedBytes) {
			byteList.add(currentByte);
		}
		
		return byteList;
		
	}
	
	/**
	 * this method is created to use standard-safe way to convert from byte array to arrayList of Byte.
	 * @param srcString the source array of bytes
	 * @return the ArrayList of Byte obj equivalent to src byte array
	 */
	public static ArrayList<Byte> byteArrToByteList(byte[] srcByteArray) {

		ArrayList<Byte> byteList = new ArrayList<Byte>();

		// translate byte[] to ArrayList<Byte>
		for(byte currentByte : srcByteArray) {
			byteList.add(currentByte);
		}
		
		return byteList;
		
	}
	
	/**
	 * this method is created to use standard-safe way to convert from byte array to arrayList of Byte.
	 * @param srcString the source List of Byte obj
	 * @return the byte array equivalent to srcByteList
	 */
	public static byte[] byteListToByteArray(ArrayList<Byte> srcByteList) {

		byte[] decodedBytes = new byte[srcByteList.size()];
		int i = 0;
		for(Byte currentByte : srcByteList) {
			decodedBytes[i] = currentByte;
			i++;
		}
		
		return decodedBytes;
		
	}
	
	/**
	 * this method returns bits composing an array of bytes
	 * 
	 * @param the source array of bytes to analize
	 * @return the String containing 0|1 representing bits in the given parameter byte array
	 */
	public static String byteArrayToBinary(byte[] bytes) {
		String out="";
		
		for (byte b : bytes) {
		    out += Integer.toBinaryString(b & 255 | 256).substring(1)+".";
		}
		
		return out.substring(0, out.length()-1);
	}
	
	/** This method return bitwise XOR of two byte array of the same size. 
	 * 
	 *  @return the result of xor (null if length of param arrays are not equals)
	 */
    public static byte[] xor(byte[] data1, byte[] data2) {

    	byte[] result = null;
    	if (data1.length == data2.length) {

    		result = new byte[data1.length];
    		for (int i = 0; i < data1.length; i++) {
				result[i] = (byte)(0xff & (data2[i] ^ data1[i]));
			} 
			return result;
			
		}else {
			// if different lengths
			return null;
		}
    }
    
	/** This method return bitwise AND of two byte array of the same size. 
	 * 
	 *  @return the result of AND (null if length of param arrays are not equals)
	 */
    public static byte[] and(byte[] data1, byte[] data2) {

    	byte[] result = null;
    	if (data1.length == data2.length) {

    		result = new byte[data1.length];
    		for (int i = 0; i < data1.length; i++) {
				result[i] = (byte)(0xff & (data2[i] & data1[i]));
			} 
			return result;
			
		}else {
			// if different lengths
			return null;
		}
    }
    
    /** This method return bitwise OR of two byte array of the same size. 
	 * 
	 *  @return the result of OR (null if length of param arrays are not equals)
	 */
    public static byte[] or(byte[] data1, byte[] data2) {

    	byte[] result = null;
    	if (data1.length == data2.length) {

    		result = new byte[data1.length];
    		for (int i = 0; i < data1.length; i++) {
				result[i] = (byte)(0xff & (data2[i] | data1[i]));
			} 
			return result;
			
		}else {
			// if different lengths
			return null;
		}
    }

	/**
	 * This method Extracts 4 bytes forming the given integer from left to right. 
	 * In bits it's equivalent to: [00000000 11111111 22222222 33333333], where the number is array index.
	 * @param source the integer to retrieve bytes from
	 * @return array of four bytes representing the integer.
	 */
	public static byte[] retrieveBytesFromInt(Integer source) {
		byte[] result = new byte[4];
	
		result[0] = (byte) ((source & 0xFF000000) >> 24);
		result[1] = (byte) ((source & 0x00FF0000) >> 16);
		result[2] = (byte) ((source & 0x0000FF00) >> 8);
		result[3] = (byte) ((source & 0x000000FF) >> 0);
	
		return result;
	}

	/**
	 * @return the 32 bits integer(4x 8 bits) made by the four bytes in the order: [b0,b1,b2,b3]
	 */
	public static int fourBytesToIntShift(byte b0, byte b1, byte b2, byte b3) {
		int r = (b3 & 0xFF) | ((b2 & 0xFF) << 8) | ((b1 & 0xFF) << 16) | ((b0 & 0xFF) << 24);
		return r;
	}

	/**
	 * @return the 24 bits integer(8 zeros and 24 bits) made by the three bytes in the order: [0,b1,b2,b3]
	 * So, result is wrapped in integer padding left with zeros (useful to avoid overflows in operations with integer retrieved from byte stream)
	 */
	public static int threeBytesToIntShift(byte b1, byte b2, byte b3) {
		int r = (b3 & 0xFF) | ((b2 & 0xFF) << 8) | ((b1 & 0xFF) << 16);
		return r;
	}

	/**
	 * @return the 16 bits equivalent integer(16 zeros and 16 bits) made by the three bytes in the order: [0,0,b1,b2]. 
	 * So, result is wrapped in integer padding left with zeros (useful to avoid overflows in operations with integer retrieved from byte stream)
	 */
	public static int twoBytesToIntShift(byte b1, byte b2) {
		int r = (b2 & 0xFF) | ((b1 & 0xFF) << 8) ;
		return r;
	}

	public static byte[] xorBytes(byte[] first, byte[] second) {
		byte[] xorResult = new byte[first.length];
	
		int i = 0;
		for (byte firstiByte : first) {
			xorResult[i] = (i < second.length) ? (byte) (firstiByte ^ second[i++]) : firstiByte;
		}
		
		return xorResult;
	}
	
	// GETTERS AND SETTERS
	
}
