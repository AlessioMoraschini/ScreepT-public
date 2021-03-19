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
package utility.string;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.swing.JTextArea;

import utility.log.SafeLogger;

public class StringWorker {
	
	public enum EOL{
		CR("\r", "\\r"),
		LF("\n", "\\n"),
		CRLF("\r\n", "\\r\\n");
		
		public static final String CR_EXCLUSIVE_REGEX = "\\r(?!\\n)";
		public static final String LF_EXCLUSIVE_REGEX = "\\n(?!\\r)";
		public static final char UNICODE_CR = 0x00A;
		public static final char UNICODE_LF = 0x00D;
		
		public String eol;
		public String eolRegex;
		
		public static EOL defaultEol = getOsBasedEOL();
		
		public static EOL getOsBasedEOL() {
			String osNameShort = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
			if(osNameShort != null && osNameShort.indexOf("win") > 0) {
				return EOL.CRLF;
			} else {
				return EOL.LF;
			}
		}
	
		private EOL(String eol, String regex) {
			this.eol = eol;
			this.eolRegex = regex;
		}
	}
	
	private static SafeLogger logger = new SafeLogger(StringWorker.class);
	
	public static ArrayList<String> TXT_EDITOR_UPPERCASE_SEPARATORS = new ArrayList<>();
	static {
		TXT_EDITOR_UPPERCASE_SEPARATORS.addAll(Arrays.asList(new String[] {".","!","<",">","?"}));
	}
	
	public static final String UNPRINTABLE_ESCAPES = "[^\\x20-\\x7e]";
	
	public String currentString;
	
	public StringWorker() {
		currentString = new String("");
	}
	
	public StringWorker(String input) {
		currentString = new String(input);
	}
	
    public static boolean isStringUpperCase(String source){
        
    	// Validation
    	if("".equals(trimToEmpty(source))) {
    		return false;
    	}
    	
        //convert String to char array
        char[] charArray = source.toCharArray();
        
        for(int i=0; i < charArray.length; i++){
            
            //if any character is not in upper case, return false
            if(Character.isAlphabetic(charArray[i]) && !Character.isUpperCase(charArray[i])) {
            	return false;
            }
        }
        
        return true;
    }

	public static InputStream stringToInputStream(String src) throws IOException {
	    
		byte[] sourceBytes = src.getBytes(Charset.forName(System.getProperty("file.encoding")));
	    InputStream inputStream = new ByteArrayInputStream(sourceBytes);
	    
	    return inputStream;
	}
	
	public static String removeLastChar(String source) {
		if(source != null && !source.isEmpty()) {
			return source.substring(0, source.length()-1);
		}
		
		return source;
	}
	
	public static String lowerAfterDots(String source, char trigger) {
		
		String result="";
		// first letter lowercase
		boolean flagNewUp = false; 
		boolean firstChanged = false;
		
		// for every char in string if flag is true makeLower case, else add the same original char
		for(int i=0; i<source.length(); i++) {
			char character_i = source.charAt(i);
			if(!firstChanged && Character.isAlphabetic(character_i)) {
				// First alphabetic letter is always changed
				result+=String.valueOf(character_i).toLowerCase();
				firstChanged = true;
				
			}else if (flagNewUp==true) {
				result+=String.valueOf(character_i).toLowerCase();
				// space and still to lowercase handler
				if(!(character_i == ' ') && !(TXT_EDITOR_UPPERCASE_SEPARATORS.contains(String.valueOf(character_i)))) {
					flagNewUp=false;
				}		
			} else {
				result+=String.valueOf(character_i);
			}
				
			// finally update flag according to char value il blacklist
			if (character_i==trigger || TXT_EDITOR_UPPERCASE_SEPARATORS.contains(String.valueOf(character_i))){
				flagNewUp=true;
			}
		}
		return result;
	}
	
	public static String upperAfterDots(String source, char trigger) {
		
		String result="";
		// first letter uppercase
		boolean flagNewUp = false; 
		boolean firstChanged = false;
		
		// for every char in string if flag is true makeUpper case, else add the same original char
		for(int i=0; i<source.length(); i++) {
			char character_i = source.charAt(i);
			
			if(!firstChanged && Character.isAlphabetic(character_i)) {
				// First alphabetic letter is always changed
				result+=String.valueOf(character_i).toUpperCase();
				firstChanged = true;
				
			} else if (flagNewUp==true) {
				result+=String.valueOf(character_i).toUpperCase();
				// space and still to uppercase handler
				if(!(character_i == ' ') && !(TXT_EDITOR_UPPERCASE_SEPARATORS.contains(String.valueOf(character_i)))) {
					flagNewUp=false;
				}		
				
			} else {
				result+=String.valueOf(character_i);
			}
			
			// finally update flag according to char value il blacklist
			if (character_i==trigger || TXT_EDITOR_UPPERCASE_SEPARATORS.contains(String.valueOf(character_i))){
				flagNewUp=true;
			}
		}
		
		
		return result;
	}

	public static String fitToMaxLentgh(String source, int lim) {
		String res = source;
		int length = source.length();
		
		if (length > lim) {
			int half = (int)(float)lim/2;
			res = source.substring(0, half-3)+".."+source.substring(length-half-2, length);
		}
		
		return res;
	}

	public static String forceLengthDeterministic(String src, int length, String padString) {
		return String.format("%1$"+length+ "s", src).substring(0, length).replaceAll(" ", padString);
	}

	/**
	 * this method returns an array of bytes of the same length of given string, even if the source has more bytes for each character (chinese, cyrillic , etc)
	 * @param source the source string
	 * @return null if source is null, array of first source.length bytes else
	 * @throws UnsupportedEncodingException
	 */
	public static byte[] extractSameLengthByteArray(String source) throws UnsupportedEncodingException {
		if(source == null) {
			return null;
		}
		
		int sourceLength = source.length();
		byte[] extractedBytes = new byte[source.length()];
		ArrayList<Byte> extractedList = new ArrayList<Byte>();
		
		// extract bytes from source string
		for(int i=0; i<sourceLength; i++) {
			String currentChar = source.substring(i, i+1);
			byte[] byteChar = currentChar.getBytes("UTF-8");
			
			for(byte b : byteChar) {
				extractedList.add(b);
			}
		}
		
		// create array containing bytes
		int i = 0;
		for(byte by : extractedList) {
			if(i >= sourceLength) {
				break;
			}
			extractedBytes[i] = by;
			i++;
		}
		
		return extractedBytes;
	}
	
	public static int countOccurrencies(String source, String match) {
		int lastIndex = 0;
		int count = 0;
		
		if (isEmpty(match) || isEmpty(source)) {
			return 0;
		}
		
		while(lastIndex != -1){

		    lastIndex = source.indexOf(match, lastIndex);

		    if(lastIndex != -1){
		        count ++;
		        lastIndex += match.length();
		    }
		}
		
		return count;
	}
	
	/**
	 * this method extract from the source string the elements starting from N elements, and return it. 
	 * the elements are calculated as separated by the given separator.
	 * @param source
	 * @param separator
	 * @param start integer representening from which element String elements have to be kept.
	 * @return new string with only elements from start
	 */
	public static String retrieveFromNelements(String source, String separator, int start) {
		try {
			StringTokenizer tokenizer = new StringTokenizer(source, separator);
			String result = "";
			int i= 0;
			while(tokenizer.hasMoreTokens()) {
				if(i>=start) {
					result += tokenizer.nextToken();
				}
				i++;
			}
			return result;
		} catch (NoSuchElementException e) {
			return source;
		} catch(NullPointerException e) {
			return source;
		}
	}

	/**
	 * this function take a string parameter and a jtextarea to remove from all occurrences of parameter
	 * 
	 * @return the integer new selection end of the string
	 */
	public static int removeCharacterFromTxtArea(String toRemove, JTextArea textSourceArea, int startSel, int endSel) {
		textSourceArea.requestFocus();
		textSourceArea.setSelectionStart(startSel);
		textSourceArea.setSelectionEnd(endSel);
		String selText = textSourceArea.getSelectedText();
		String allText = textSourceArea.getText();
		
		int newEnd = 0;
		
		if (selText == null || selText.equals("")) {
			String text = allText;
			text = text.replaceAll(toRemove, "");
			textSourceArea.setText(text);
			newEnd = text.length();
			
		}else {
			
			String prefix = textSourceArea.getText().substring(0, startSel);
			String postFix = textSourceArea.getText().substring(endSel, allText.length());
			selText = selText.replaceAll(toRemove, "");
			newEnd = startSel + selText.length();
			
			textSourceArea.setText(prefix.concat(selText).concat(postFix));
		}
		return newEnd;
	}

	/**
	 * this method extract from the source string the elements starting from N elements to end element, and return it. 
	 * the elements are calculated as separated by the given separator.
	 * @param source
	 * @param separator
	 * @param start integer representening from which element String elements have to be kept.
	 * @return new string with only elements from start
	 */
	public static String retrieveFromNelements(String source, String separator, int start, int end) {
		try {
			StringTokenizer tokenizer = new StringTokenizer(source, separator);
			String result = "";
			int i= 0;
			while(tokenizer.hasMoreTokens()) {
				if(i>=start && i<=end) {
					result += tokenizer.nextToken();
				}
				i++;
			}
			return result;
		} catch (NoSuchElementException e) {
			return source;
		} catch(NullPointerException e) {
			return source;
		}
	}

	public static String uppercaseEveryFirstLetterNoSpace(String sourceString) {
		String result="";
		String separator=" ";
		StringTokenizer tokenizer = new StringTokenizer(sourceString, separator);
		while(tokenizer.hasMoreTokens()) {
			String word = tokenizer.nextToken();
			int spaceEluder = 0;
			boolean eluded=false;
			while(!eluded && spaceEluder<=word.length()) { // elude spaces
				char first = word.charAt(spaceEluder);
				if(!String.valueOf(first).equals(separator)) {
					eluded=true;
					word=word.substring(0, spaceEluder)+String.valueOf(first).toUpperCase()+word.substring(spaceEluder+1, word.length());
				}
				//fine lettera
				spaceEluder++;
			}
			// fine parola
			if(tokenizer.hasMoreElements()) {
				result+=word+separator; // aggiungo spazio solo se sono alla fine
			}
			else {
				result+=word;
			}
		}
		
		return result;
	}

	public static String lowerCaseEveryFirstLetterNoSpace(String sourceString) {
		String result="";
		String separator=" ";
		StringTokenizer tokenizer = new StringTokenizer(sourceString, separator); 
		while(tokenizer.hasMoreTokens()) {
			String word = tokenizer.nextToken();
			int spaceEluder = 0;
			boolean eluded=false;
			while(!eluded && spaceEluder<=word.length()) { // elude spaces
				char first = word.charAt(spaceEluder);
				if(!String.valueOf(first).equals(separator)) {
					eluded=true;
					word=word.substring(0, spaceEluder)+String.valueOf(first).toLowerCase()+word.substring(spaceEluder+1, word.length());
				}
				//fine lettera
				spaceEluder++;
			}
			// fine parola
			if(tokenizer.hasMoreElements()) {
				result+=word+separator; // aggiungo spazio solo se la parola continua
			}
			else {
				result+=word;
			}
		}
		
		return result;
	}

	public static String removeSpacesAndLines(String source) {
		String result=""; 
		
		// for every char in string if flag is true makeUpper case, else add the same original char
		for(int i=0; i<source.length(); i++) {
			char character_i = source.charAt(i);			
			// finally update flag according to char value il blacklist
			if (!(character_i==' ' || String.valueOf(character_i).equals(EOL.defaultEol.eol))){
				result+=String.valueOf(character_i);
			}
		}
		
		
		return result;
	}

	public static String removeLines(String source) {
		String result=""; 
		
		// for every char in string if flag is true makeUpper case, else add the same original char
		for(int i=0; i<source.length(); i++) {
			char character_i = source.charAt(i);			
			// finally update flag according to char value il blacklist
			if (!(String.valueOf(character_i).equals(EOL.defaultEol.eol))){
				result+=String.valueOf(character_i);
			}
		}
		
		
		return result;
	}

	public static String[] getUpperCaseArray(String[] sourceStringArr) {
		String[] uppercaseNames = new String[sourceStringArr.length];
		int i = 0;
		for(String s : sourceStringArr) {
			uppercaseNames[i] = s.toUpperCase();
			i++;
		}
		return uppercaseNames;
	}

	public static String[] getLowerCaseArray(String[] sourceStringArr) {
		String[] lowercaseNames = new String[sourceStringArr.length];
		int i = 0;
		for(String s : sourceStringArr) {
			lowercaseNames[i] = s.toLowerCase();
			i++;
		}
		return lowercaseNames;
	}
	
	public static String getNTimesString(String source, int n) {
		if(n < 0 || isEmptyNoTrim(source)) {
			return source;
		}
		
		String result = "";
		
		String original = new String(source);
		for(int i = 0; i < n; i++) {
			result += original;
		}
		
		return result;
	}
	
	public static String replaceSpacesWithTabs(String source, int tabSize) {
		String[] lines = source.split(EOL.defaultEol.eol);
		StringBuilder builder = new StringBuilder();
		for (String line : lines) {
			
			int lineLength = line.length();
			String convertedBlock = "";
			String buffer = "";
			for(int i = 0; i < lineLength; i++) {
				Character current = line.charAt(i);
				buffer += current;

				if(!current.equals(' ')) {
					convertedBlock += buffer;
					buffer = "";
				}
				
				if (buffer.length() == tabSize ) {
					convertedBlock += "\t";
					buffer = "";
				}
			}
			
			builder.append(convertedBlock).append(buffer).append(EOL.defaultEol.eol);
		}
		
		return builder.toString();
	}
	
	public static String replaceTabsWithSpaces(String source, int tabSize) {
		String spaces = getNTimesString(" ", tabSize);
		return source.replace("\t", spaces);
	}
	
	public static ArrayList<String> getStringWhileNotNull(Character[] buffer) {
		ArrayList<String> notNulls = new ArrayList<>();
		for(Character current : buffer) {
			if(current != null) {
				notNulls.add(String.valueOf(current));
			} else {
				return notNulls;
			}
		}
		return notNulls;
	}

	/**
	 * This method changes the characters from start to end in the string with the given String
	 * 
	 * @param int start : 0<start<end<src.lenght
	 * @param int end : 0<start<end<src.lenght
	 */
	public static String injectStringInSelection(String src, String insert, int start, int end) {
		// check start to valid invalid case values
		if(start<0) {
			start=0;
		}
		if(start>end) {
			start=end;
		}
		if(start>src.length()) {
			start=src.length();
		}
		// check end to valid invalid case values
		if(end<0) {
			end=0;
		}
		if(end<start) {
			start=end;
		}
		if(end>src.length()) {
			end=src.length();
		}
		// initialize result with first segment that has to remain the same
		String result=src.substring(0, start);
		String second=src.substring(end, src.length());
		result+=insert+second;
		return result;
		
	}

	/**
	 * This method prints key and values from map
	 * @param map
	 */
	public static <K, V> void printMap(Map<K, V> map) {
	    for (Map.Entry<K, V> entry : map.entrySet()) {
	        System.out.println("Key : " + entry.getKey() 
				+ " <=> Value : " + entry.getValue());
	    }
	}

	/**
	 * This method prints key and values from map
	 * @param map
	 */
	public static <K, V> String getMapToString(Map<K, V> map) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			builder.append("Key : " + entry.getKey() + " <=> Value : " + entry.getValue()).append(EOL.defaultEol.eol);
		}
		
		return builder.toString();
	}

	/**
	 * This method prints values from generic array using toString
	 * @param array
	 */
	public static<E> void printArray(E[] array) {
		int i=0;
	    for (E curr : array) {
	        System.out.println("Val["+i+"] : " + curr);
	        i++;
	    }
	}
	public static<E> String strArray(E[] array) {
		int i=0;
		StringBuilder builder = new StringBuilder();
		if(array != null)
			for (E curr : array) {
				builder.append("Val["+i+"] : " + curr);
				i++;
			}
		
		return builder.toString();
	}

	/**
	 * This method prints string representation of int array using toString
	 * @param array
	 */
	public static void printArray(int[] array) {
		int i=0;
	    for (int curr : array) {
	    	System.out.println("Val["+i+"] : " + curr);
	        i++;
	    }
	}

	/**
	 * This method returns string representation from key and values from map
	 * @param map
	 */
	public static <K, V> String stringVal(Map<K, V> map) {
		String out="";
	    for (Map.Entry<K, V> entry : map.entrySet()) {
	        out+= "Key : " + entry.getKey() + " <=> Value : " + entry.getValue()+EOL.defaultEol.eol;
	    }
	    return out;
	}

	/**
	 * This method returns string representation from generic array using toString
	 * @param array
	 */
	public static<E> String stringVal(E[] array) {
		int i=0;
		String out="";
	    for (E curr : array) {
	        out+="Val["+i+"] : " + curr + EOL.defaultEol.eol;
	        i++;
	    }
	    return out;
	}

	/**
	 * This method returns string representation from int array using toString
	 * @param array
	 */
	public static String stringVal(int[] array) {
		int i=0;
		String out="";
	    for (int curr : array) {
	        out += "Val["+i+"] : " + curr + EOL.defaultEol.eol;
	        i++;
	    }
	    return out;
	}

	public static boolean containsIgnoreCase(String first, String second) {
		if(first == null || second == null) {
			return false;
		}
		
		return first.toLowerCase().contains(second.toLowerCase());
	}

	public static boolean contains(String first, String second) {
		if(first == null || second == null) {
			return false;
		}
		
		return first.contains(second);
	}
	
	public static String concat(List<? extends Object> list,  EOL separator) {
		StringBuilder builder = new StringBuilder();
		if(list != null && !list.isEmpty()) {
			int i = 0;
			for(Object s : list) {
				if(s != null)
					builder.append(s);
				if(s != null && i < list.size() - 1)
					builder.append(separator.eol);
				
				i++;
			}
		}
		
		return builder.toString();
	}
	
	public static String concatFilePaths(List<File> list,  EOL separator) {
		separator = separator == null ? EOL.defaultEol : separator;
		StringBuilder builder = new StringBuilder();
		if(list != null && !list.isEmpty()) {
			int i = 0;
			for(File file : list) {
				if(file != null)
					try {
						builder.append(file.getCanonicalPath());
					} catch (IOException e) {
						logger.error("Error getting canonical path of: " + file, e);
					}
				if(file != null && i < list.size() - 1)
					builder.append(separator.eol);
				
				i++;
			}
		}
		
		return builder.toString();
	}

	public static boolean isEmpty(String source) {
		return source == null || (source != null && "".equals(source.trim()));
	}
	public static boolean isEmptyNoTrim(String source) {
		return source == null || (source != null && "".equals(source));
	}

	public static String nullToEmpty(String source) {
		if (source != null) {
			return source;
		} else {
			return "";
		}
	}

	public static String trimToEmpty(String source) {
		return nullToEmpty(source).trim();
	}
	
	public static boolean notEmpty(String source) {
		return !isEmpty(source);
	}

	public static boolean containsOneOfMatchesChar(char[] password, char[] targetMatches) {
		for(char iterator : password) {
			for (char match : targetMatches) {
				if (iterator == match) {
					return true;
				} 
			}
		}
		
		return false;
	}

	public static boolean containsMatch(char[] password, char targetMatch) {
		for(char iterator : password) {
			if(iterator == targetMatch) {
				return true;
			}
		}
		
		return false;
	}
	
	public static String removeBlankLines(String text, String lineSeparator) {
		if(text == null || lineSeparator == null)
			return text;
		
		StringBuilder builder = new StringBuilder();
		
		String[] lines = text.split(lineSeparator);
		for(int i = 0; i < lines.length; i++) {
			if(!StringWorker.isEmpty(lines[i]))
				builder.append(lines[i]);
			
			if(i < lines.length - 1)
				builder.append(lineSeparator);
		}
		
		return builder.toString();
	}
	
	public static boolean checkWildcardMatch(String toCheck, String wildcardExpression) {
		return checkWildcardMatch(toCheck, wildcardExpression, false);
	}

	public static boolean checkWildcardMatch(String toCheck, String wildcardExpression, boolean ignoreCase) {
		if(isEmpty(toCheck) || isEmpty(wildcardExpression))
			return false;
		
		if(ignoreCase) {
			wildcardExpression = wildcardExpression.toLowerCase();
			toCheck = toCheck.toLowerCase();
		}
		
		return trimToEmpty(toCheck).matches(trimToEmpty(wildcardExpression).replace("?", ".?").replace("*", ".*?"));
	}

	/**
	 * Replace all EOL(EndOfLine) characters with the wanted type
	 * @param source
	 * @return
	 */
	public static String normalizeStringToEol(String source, EOL outputEOL) {
		
		String output = normalizeStringToLF(source).replaceAll(EOL.LF.eol, outputEOL.eol);
		
		return output;
	}

	/**
	 * Replace all EOL(EndOfLine) characters with the wanted type
	 * @param source
	 * @return
	 */
	public static String normalizeStringToEol(String source, String outputEOL) {
		
		String output = normalizeStringToLF(source).replaceAll(EOL.LF.eol, outputEOL);
		
		return output;
	}

	/**
	 * Replace all EOL(EndOfLine) characters with the LF type
	 * @param source
	 * @return
	 */
	public static String normalizeStringToLF(String source) {
		String output = trimToEmpty(source);
		
		output = source.replaceAll(EOL.CRLF.eolRegex, EOL.LF.eol);
		output = output.replaceAll(EOL.CR.eolRegex, EOL.LF.eol);
		
		return output;
	}

	/**
	 * Make visible characters \r and \n using ASCII
	 */
	public static String makeCRLFvisible(String source) {
		if(isEmpty(source)) {
			return "";
		}
		
		byte[] random = new byte[32];
		new Random().nextBytes(random);
		String rand = UUID.nameUUIDFromBytes(random).toString();
		
		return source.replace("\r\n", "[\\r\\n]"+rand).replace("\n", "[\\n]"+rand).replace("\r", "[\\r]"+rand).replace(rand, System.getProperty("line.separator"));
	}

	public static String makeCRLFvisible(File source) throws IOException {
		if(source == null || !source.exists()) {
			throw new IOException();
		}
	
		logger.info("readFileAsString -> Start...");
		long startTime = System.currentTimeMillis();
	
		StringBuilder builder = new StringBuilder(100000);
		FileInputStream readerStream = new FileInputStream(source);
		BufferedReader reader = new BufferedReader(new InputStreamReader(readerStream, Charset.forName("UTF-8")));
		int charInt;
		char charEl;
		try {
			boolean CR = false;
			while (( charInt = reader.read()) != -1) {
				charEl = (char) charInt;
				if(charEl == '\r') {
					builder.append("[\\r]");
					CR = true;
				} else {
					if (CR) {
						CR = false;
						if (charEl == '\n'){
							builder.append("[\\n]\r\n");
						} else {
							builder.append("\r");
						}
					} else {
						if(charEl == '\n') {
							builder.append("[\\n]\n");
						} else {
							builder.append(charEl);
						}
					}
				}
			}
		} catch (IOException e) {
			throw e;
		}finally {
			reader.close();			
		}
		
		logger.info("readFileAsString -> Finished in " + String.valueOf(System.currentTimeMillis()-startTime) + " ms");
		return builder.toString();
	}
	
	public static boolean isStringSupportedByCharset(String source, Charset charset) {
		
		try {
			if("".equals(trimToEmpty(source)) || !validateCharset(charset)) {
				return false;
			}
			
			return charset.newEncoder().canEncode(source);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isStringSupportedByCharset(String source, String charsetName) {
		
		try {
			if("".equals(trimToEmpty(source)) || !validateCharset(charsetName)) {
				return false;
			}
			
			return Charset.forName(charsetName).newEncoder().canEncode(source);
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean validateCharset(Charset charset) throws Exception {
		return charset != null && Charset.isSupported(charset.name());
	}
	public static boolean validateCharset(String charsetName) throws Exception {
		return charsetName != null && Charset.isSupported(charsetName);
	}

	/**
	 * Use this method to remove every character from given string, except "." and
	 * digits 0-9 Note that eventual undescores will become dots.
	 * 
	 * @param source
	 * @return
	 */
	public static String keepOnlyDotsAndDigits(String source) {
		String dotsOnly = source.replaceAll("_", ".");
		dotsOnly = dotsOnly.replaceAll("-", "");
		// remove non dots chars
		return dotsOnly.replaceAll("[a-zA-Z!#$%&'*+=?^`{|}+]|[_]\\-", "");
	}

	/**
	 * Dotted version format compare - if version not matches format
	 * <Strong>IllegalArgumentException</Strong> is thrown
	 * 
	 * @param version_A
	 * @param version_B
	 * @return -1 if A less than B, 1 if A more than B, 0 if equals
	 */
	public static int compareVersions(String version_A, String version_B) throws IllegalArgumentException {
	
		if (version_A == null || version_B == null) {
			throw new IllegalArgumentException("Version can not be null");
		}
		
		String cleanVersA = keepOnlyDotsAndDigits(version_A);
		String cleanVersB = keepOnlyDotsAndDigits(version_B);
		
		if (!cleanVersA.matches("[0-9]+(\\.[0-9]+)*") || !cleanVersB.matches("[0-9]+(\\.[0-9]+)*")) {
			throw new IllegalArgumentException("Invalid version format");
		}
	
		String[] AParts = cleanVersA.split("\\.");
		String[] BParts = cleanVersB.split("\\.");
		int length = Math.max(AParts.length, BParts.length);
		for (int i = 0; i < length; i++) {
			int ACurrPart = i < AParts.length ? Integer.parseInt(AParts[i]) : 0;
			int BCurrPart = i < BParts.length ? Integer.parseInt(BParts[i]) : 0;
			if (ACurrPart < BCurrPart) {
				return -1;
			}
			if (ACurrPart > BCurrPart) {
				return 1;
			}
		}
		return 0;
	}
	
	public static String repeatStringNTimes(String string, int n, boolean newLineEach) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < n; i++) {
			builder.append(string).append((newLineEach && i < n - 1) ? EOL.defaultEol.eol : "");
		}
		
		return builder.toString();
	}
	
	public static DecimalFormat getNDecimalsFormat(int n) {
		String decimalPattern = repeatStringNTimes("0", n, false);
		return new DecimalFormat("#0." + decimalPattern); 
	}
	
	public static String getStringDouble(Double value, int nDecimals) {
		return getNDecimalsFormat(nDecimals).format(value);
	}

}
