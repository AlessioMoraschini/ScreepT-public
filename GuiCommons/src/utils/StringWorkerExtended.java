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
package utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import resources.GeneralConfig;
import various.common.light.om.EscapeType;
import various.common.light.utility.string.StringWorker;

@SuppressWarnings("deprecation")
public class StringWorkerExtended extends StringWorker {

	public static final String UNPRINTABLE_ESCAPES = "[^\\x20-\\x7e]";
	
	static {
		TXT_EDITOR_UPPERCASE_SEPARATORS = GeneralConfig.TXT_EDITOR_UPPERCASE_SEPARATORS;
	}

	// this array contains all char that trigger uppercase next char
	public StringWorkerExtended(String input) {
		currentString = new String(input);
	}
	
	public static String forceLength(String src, int length) {
		return replaceOccurrencesWithRandom(String.format("%1$"+length+ "s", src), " ").substring(0, length);
	}
	
	public static String forceLength(String src, int length, char[] possibleCharacters) {
		return replaceOccurrencesWithRandom(String.format("%1$"+length+ "s", src), " ", possibleCharacters).substring(0, length);
	}
	
	public static String replaceOccurrencesWithRandom(String source, String match, char[] possibleCharacters) {
		
		while(source.contains(match)){
			source = source.replaceFirst(match, RandomStringUtils.random(1, possibleCharacters));
		}
		
		return source;
	}

	public static String replaceOccurrencesWithRandom(String source, String match) {
		
		while(source.contains(match)){
			source = source.replaceFirst(match, RandomStringUtils.randomAscii(1));
		}
		
		return source;
	}
	
	/**
     * Convert given string to the same string but with replaced special chars with escape sequences.
     * The language used correspond to the type specified in type parameter.
     * @param source the source string into which replace found special chars
     * @param type the type of language to convert from/to
     * @return the source string with replaced special characters
     */
    public static String replaceSpecialWithEscapes(String source, EscapeType type) {
    	if (EscapeType.HTML_3.equals(type)) {
			return StringEscapeUtils.escapeHtml3(source);
		}else if (EscapeType.HTML_4.equals(type)) {
			return StringEscapeUtils.escapeHtml4(source);
		}else if (EscapeType.ECMASCRIPT.equals(type)) {
			return StringEscapeUtils.escapeEcmaScript(source);
		}else if (EscapeType.JSON.equals(type)) {
			return StringEscapeUtils.escapeJson(source);
		}else if (EscapeType.JAVA.equals(type)) {
			return StringEscapeUtils.escapeJava(source);
		}else if (EscapeType.XML.equals(type)) {
			return StringEscapeUtils.escapeXml(source);
		}else if (EscapeType.XML_10.equals(type)) {
			return StringEscapeUtils.escapeXml10(source);
		}else if (EscapeType.XML_11.equals(type)) {
			return StringEscapeUtils.escapeXml11(source);
		}else {
			// if no match found
			return source;
		}
    }
    
    /**
     * Convert given string to the same string but with replaced escape sequences with original special chars.
     * The language used correspond to the type specified in type parameter.
     * @param source the source string into which replace found escape sequences.
     * @param type the type of language to convert from/to
     * @return the source string with replaced escape sequences.
     */
    public static String replaceEscapesWithSpecials(String source, EscapeType type) {
    	if (EscapeType.HTML_3.equals(type)) {
			return StringEscapeUtils.unescapeHtml3(source);
		}else if (EscapeType.HTML_4.equals(type)) {
			return StringEscapeUtils.unescapeHtml4(source);
		}else if (EscapeType.ECMASCRIPT.equals(type)) {
			return StringEscapeUtils.unescapeEcmaScript(source);
		}else if (EscapeType.JSON.equals(type)) {
			return StringEscapeUtils.unescapeJson(source);
		}else if (EscapeType.JAVA.equals(type)) {
			return StringEscapeUtils.unescapeJava(source);
		}else if (EscapeType.XML.equals(type) || EscapeType.XML_10.equals(type) || EscapeType.XML_11.equals(type)) {
			return StringEscapeUtils.unescapeXml(source);
		}else {
			// if no match found
			return source;
		}
    }
}
