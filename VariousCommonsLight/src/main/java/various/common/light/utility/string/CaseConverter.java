/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
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
package various.common.light.utility.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Case conversions of the text editor (camelCase, snake_case...). Pure functions: every line is converted on its own,
 * indentation, trailing spaces and line terminators (LF or CRLF) are kept as they are.
 */
public final class CaseConverter {

	private CaseConverter() {
	}

	public static String camel(String text) {
		return perLine(text, words -> join(words, "", false));
	}

	public static String pascal(String text) {
		return perLine(text, words -> join(words, "", true));
	}

	public static String snake(String text) {
		return perLine(text, words -> join(words, "_", false));
	}

	public static String kebab(String text) {
		return perLine(text, words -> join(words, "-", false));
	}

	public static String constant(String text) {
		String snake = snake(text);
		return snake == null ? null : snake.toUpperCase(Locale.ROOT);
	}

	/** First letter of every sentence in capitals (after . ! ? and at the start), the rest in small letters */
	public static String sentence(String text) {
		if (text == null) {
			return null;
		}
		StringBuilder out = new StringBuilder(text.length());
		boolean start = true;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (Character.isLetter(ch)) {
				out.append(start ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
				start = false;
			} else {
				out.append(ch);
				if (ch == '.' || ch == '!' || ch == '?' || ch == '\n') {
					start = true;
				}
			}
		}
		return out.toString();
	}

	/** Every upper case letter becomes lower case and the other way round */
	public static String invert(String text) {
		if (text == null) {
			return null;
		}
		StringBuilder out = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			out.append(Character.isUpperCase(ch) ? Character.toLowerCase(ch) : Character.toUpperCase(ch));
		}
		return out.toString();
	}

	/** Words of a line: split on anything that is not a letter or digit, and on lower->Upper and XMLHttp -> XML, Http */
	static List<String> words(String line) {
		List<String> words = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (!Character.isLetterOrDigit(ch)) {
				flush(words, current);
				continue;
			}
			if (current.length() > 0 && Character.isUpperCase(ch)) {
				char prev = line.charAt(i - 1);
				boolean nextLower = i + 1 < line.length() && Character.isLowerCase(line.charAt(i + 1));
				if (Character.isLowerCase(prev) || Character.isDigit(prev) || (Character.isUpperCase(prev) && nextLower)) {
					flush(words, current);
				}
			}
			current.append(ch);
		}
		flush(words, current);
		return words;
	}

	private static void flush(List<String> words, StringBuilder current) {
		if (current.length() > 0) {
			words.add(current.toString().toLowerCase(Locale.ROOT));
			current.setLength(0);
		}
	}

	private static String join(List<String> words, String separator, boolean capitalizeFirst) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < words.size(); i++) {
			String w = words.get(i);
			if (i > 0) {
				out.append(separator);
			}
			boolean capital = i > 0 ? separator.isEmpty() : capitalizeFirst;
			out.append(capital ? Character.toUpperCase(w.charAt(0)) + w.substring(1) : w);
		}
		return out.toString();
	}

	private interface WordsJoiner {
		String apply(List<String> words);
	}

	private static String perLine(String text, WordsJoiner joiner) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		StringBuilder out = new StringBuilder(text.length());
		int pos = 0;
		while (pos < text.length()) {
			int nl = text.indexOf('\n', pos);
			int end = nl < 0 ? text.length() : nl;
			int eolStart = end > pos && text.charAt(end - 1) == '\r' ? end - 1 : end;
			String line = text.substring(pos, eolStart);
			int a = 0;
			while (a < line.length() && Character.isWhitespace(line.charAt(a))) {
				a++;
			}
			int b = line.length();
			while (b > a && Character.isWhitespace(line.charAt(b - 1))) {
				b--;
			}
			List<String> words = words(line.substring(a, b));
			out.append(line, 0, a).append(words.isEmpty() ? line.substring(a, b) : joiner.apply(words)).append(line, b, line.length());
			out.append(text, eolStart, nl < 0 ? text.length() : nl + 1);
			pos = nl < 0 ? text.length() : nl + 1;
		}
		return out.toString();
	}
}
