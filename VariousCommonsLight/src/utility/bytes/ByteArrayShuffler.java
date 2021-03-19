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

public class ByteArrayShuffler {

	// FIELDS
	
	private static byte[] srcArray;

	private final static int BLOCK_SIZE = 16;
	private final static int N_BLOCKS = 16;
	private final static int interval = BLOCK_SIZE * N_BLOCKS;
	
	
	// METHODS
	
	public static byte[] entroPieForward(byte[] srcArr) throws IndexOutOfBoundsException{
		
		srcArray = null;
		// first make value copy to rollback if something goes wrong
		srcArray = srcArr.clone();
		
		if(srcArray.length==0 || srcArray==null) {
			return null;
		}
		
		int LENGTH = srcArray.length;
		int iterator = 0;
		
		// first check that next elements are defined
		while(LENGTH >= iterator+interval) {
			
			// now scan all blocks
			for(int i = 1; i <= N_BLOCKS; i++) {
				
				// now scan every elements of current block
				for(int k = 1; k <= BLOCK_SIZE; k++) {
					
					if(k>i) {
						// if elements to switch then change with a distant element in another block
						switchElem(iterator, (BLOCK_SIZE*k)-i, srcArray);
					}
					iterator++;
					
				}//END CURRENT BLOCK
			
			}//END BLOCKS SCAN
			
		}//END WHILE
		
		return srcArray;
	}// END METHOD
	
	public static byte[] entroPieBack(byte[] srcArr) throws IndexOutOfBoundsException{
		
		srcArray = null;
		// first make value copy to rollback if something goes wrong
		srcArray = srcArr.clone();
		
		if(srcArray.length==0 || srcArray==null) {
			return null;
		}
		
		int LENGTH = srcArray.length;
		int residual = LENGTH%interval;
		// this time start from the last coded
		int iterator = LENGTH-residual-1;
		
		// first check that next elements are defined
		while(0 <= iterator-interval+1) {
			
			// now scan all blocks starting from last
			for(int i = N_BLOCKS; i > 0; i--) {
				
				// now scan every elements of current block starting from last
				for(int k = BLOCK_SIZE; k > 0; k--) {
					
					if(k>i) {
						// if elements to switch then change with a distant element in another block
						switchElem(iterator, (BLOCK_SIZE*k)-i, srcArray);
					}
					iterator--;
					
				}//END CURRENT BLOCK
			
			}//END BLOCKS SCAN
			
		}//END WHILE
		
		return srcArray;
	}
	
	private static void switchElem(int srcPos, int destPos, byte[] array) throws IndexOutOfBoundsException{

		byte temp = array[srcPos];
		array[srcPos] = array [destPos];
		array[destPos] = temp;

	}
	
}
