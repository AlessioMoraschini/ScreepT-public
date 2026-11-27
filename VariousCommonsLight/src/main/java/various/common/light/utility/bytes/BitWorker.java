/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package various.common.light.utility.bytes;

import javax.swing.text.BadLocationException;

public class BitWorker {

	/**
	 * Set 1 into the given byte if flag is true, 0 if flag is false. if position is < 0 or > 7 then source byte won't be modified and the original one is returned.
	 * @param toChange
	 * @param position
	 * @return the result after the modification
	 */
	public static byte setBitAtPosition(byte toChange, int position, boolean flag) throws BadLocationException {
		
		if(position < 0 || position > 7)
			throw new BadLocationException("Invalid position!", position);
		
		if (flag) {
			return (byte) (toChange | (1 << position));
		} else {
			return (byte) (toChange & ~(1 << position)); 
		}
	}

	public static boolean getBitAtPosition(byte toCheck, int position) throws BadLocationException {
		if(position < 0 || position > 7)
			throw new BadLocationException("Invalid position!", position);

		return ((toCheck >> position) & 1) == 1;
	}
	
	public static byte bitsToByte(String bits) throws NumberFormatException {
		return (byte)bitsToInt(bits);
	}

	public static int bitsToInt(String bits) throws NumberFormatException {
		return Integer.parseInt(bits, 2);
	}
	
	public static String byteToBits(byte originalToConvert) {
		return String.format("%8s", intToBits(originalToConvert & 0xFF)).replace(' ', '0');
	}
	
	public static String intToBits(Integer originalToConvert) {
		return Integer.toBinaryString(originalToConvert);
	}
	
	/**
	 * Used for subnetMask conversions: 24 -> 255.255.255.0
	 * @param maskLength
	 * @return
	 */
	public static String maskLengthToBytes(int maskLength) {
		String bits = "";
		int maskLengthCopy = maskLength;
		for(int i = 0; i < 32; i++) {
			
			if(maskLengthCopy > 0) {
				bits = bits.concat("1");
			} else {
				bits = bits.concat("0");
			}
			
			maskLengthCopy--;
		}
		
		String byte0 = bits.substring(0, 8);
		String byte1 = bits.substring(8, 16);
		String byte2 = bits.substring(16, 24);
		String byte3 = bits.substring(24, 32);
		
		String output = 
				"" + bitsToInt(byte0)
			+ "." + bitsToInt(byte1) 
			+ "." + bitsToInt(byte2) 
			+ "." + bitsToInt(byte3);
		
		return output;
	}
	
	/**
	 * Used for subnetMask conversions: 255.255.255.0 -> 24
	 * @param maskLength
	 * @return
	 */
	public static int bytesToMaskLength(String bytesDotted) throws NumberFormatException {
		
		if(bytesDotted == null) {
			return 0;
		}
		
		String[] splitted = bytesDotted.split("[.]");
		
		if(splitted.length != 4) {
			return 0;
		}
		
		int incrementer = 0;
		
		for(String block : splitted) {
			String bits = intToBits(Integer.valueOf(block));
			for(char bit : bits.toCharArray()) {
				if(bit == '1') {
					incrementer ++;
				} else if(bit == '0') {
					return incrementer;
				}
			}
		}
		
		return incrementer;
	}
}
