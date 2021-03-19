package utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

/**
 * @author A.Moraschini - 2019
 */
public class TestUtils {
	
	/**
	 *  lazy printer - print a word and go to next line
	 * @param S
	 */
	public static void printl(String source){
		source = (source != null) ? source : "";
		System.out.println(source);
	}
	
	/**
	 *  lazy printer - prints a new line
	 */
	public static void printl(){
		System.out.println("");
	}

	/**
	 *  lazy printer - prints a word
	 * @param S
	 */
	public static void print(String S){
		System.out.print(S);
	}

	/**
	 *  lazy printer - Prints a space
	 */
	public static void print(){
		System.out.print(" ");
	}
	
   /** pretty print a map
    * 
    * @param map
    */
    public static <K, V> void printMap(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println("Key : " + entry.getKey() 
				+ " <=> Value : " + entry.getValue());
        }
    }
    
    /** pretty print an array
     * 
     * @param array
     */
    public static <K extends Object> void printArray(K[] array){
    	int i = 0;
    	for(K element : array){
    		printl(element.getClass().getTypeName() + "["+i+"]" + element.toString());
    		i++;
    	}
    }

    /** pretty print a byte array
     * 
     * @param array
     */
    public static void printByteArray(byte[] array){
    	int i = 0;
    	for(byte element : array){
    		printl("["+i+"]" + element);
    		i++;
    	}
    }

    /** pretty print a char array
     * 
     * @param array
     */
    public static void printCharArray(char[] array){
    	int i = 0;
    	for(char element : array){
    		printl("["+i+"]" + element);
    		i++;
    	}
    }
	
    /**
     * 
     * Print an array with n elements for each line till the end of array's content
     */
	public static <T> void printArrayAsLines(int nForLine, T[] array) {
		
		print(getArrayAsLines(nForLine, array));
	}

	/**
	 * 
	 * Print an array with n elements for each line till the end of array's content
	 */
	public static <T> String getArrayAsLines(int nForLine, T[] array) {
		
		String result = "";
		
		if(array == null || array.length == 0) {
			return result;
		}
		
		result += "\n";
		
		int progr = 0;
		nForLine = (nForLine < array.length)? nForLine : array.length;
		
		while(progr+nForLine < array.length) {
			T[] portionToPrint = Arrays.copyOfRange(array, progr, progr+nForLine);
			result += Arrays.toString(portionToPrint) + "\n";
			
			progr += nForLine;
		}
		
		while(progr < array.length) {
			T[] portionToPrint = Arrays.copyOfRange(array, progr, progr+1);
			result += Arrays.toString(portionToPrint) + "\n";
			
			++ progr;
		}
		
		return result;
	}
	
	/**
	 * Prints a separator n-times character
	 */
	public static void printSeparator(int n){
		print( getSeparator("#", n));
	}
	
	/**
	 * Prints a separator n-times character
	 */
	public static void printSeparator(String character, int n){
		print( getSeparator(character, n));
	}
	
	/**
	 * Retrieve a String separator n-times character
	 */
	public static String getSeparator(String character, int n){
		int curr = 0;
		String output = "\n  ";
		while (curr < n) {
			output = output.concat(String.valueOf(character));
			curr++;
		}
		
		return output;
	}
	
	/**
	 * Prints a header String specifying character separator (may be null), header width in characters, and title
	 */
	public static void printHeader(String character, int n, String title){
		
	    print(getHeaderString(character, n, title));
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
		result += "\n  " + character + leftSpaces + title + rightSpaces + character ;
		result += getSeparator(character, newN);
		result += "\n";
		
		return result;
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
	 * disable or enable 
	**/
	public boolean printEnabled(boolean DEBUG) {
        if (!DEBUG) {
            System.setOut(new PrintStream(new OutputStream() {
                public void close() {}
                public void flush() {}
                public void write(byte[] b) {}
                public void write(byte[] b, int off, int len) {}
                public void write(int b) {
                }
            }));
        }
        return DEBUG;
	}
	
	/**
	 * Reflection-based to string to get superclass fields
	 */
	public static String reflectionToString(Object object, boolean recursive) {
        if (object == null) return "null";

        Class<?> clazz = object.getClass();
        StringBuilder sb = new StringBuilder(clazz.getSimpleName()).append(" {");

        while (clazz != null && !clazz.equals(Object.class)) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        sb.append(f.getName()).append(" = ").append(f.get(object)).append(",");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!recursive) {
                break;
            }
            clazz = clazz.getSuperclass();
        }

        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.append("}").toString();
    }
	
	public static void printExceptionStackString(Throwable error) {
		printl(getArrayAsLines(1, error.getStackTrace()));
	}
	
	public static String getExceptionStackString(Throwable error) {
		return getArrayAsLines(1, error.getStackTrace());
	}
	
}
