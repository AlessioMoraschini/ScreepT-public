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
package various.common.light.utility.string;

/**
 * Wildcards for searches typed by the user: '*' is any text (also none), '?' is exactly one character. Case insensitive.
 * A text with no wildcard is not a pattern: callers keep their plain "contains" search for it.
 * <p>
 * Works on char arrays, so the file name index can use it on its own memory without creating strings.
 */
public final class WildcardPattern {

	private WildcardPattern() {
	}

	/** True when the text has a '*' or a '?' (so it has to be matched as a pattern) */
	public static boolean hasWildcard(String text) {
		return text != null && (text.indexOf('*') >= 0 || text.indexOf('?') >= 0);
	}

	/** The pattern in the form {@link #matches} expects: lower case, the wildcards unchanged */
	public static char[] lowerCase(String pattern) {
		char[] p = pattern.toCharArray();
		for (int i = 0; i < p.length; i++) {
			p[i] = Character.toLowerCase(p[i]);
		}
		return p;
	}

	/**
	 * Matches the pattern against the characters from {@code from} (inclusive) to {@code to} (exclusive).
	 *
	 * @param text the text (its letters are compared in lower case)
	 * @param pattern the pattern, from {@link #lowerCase}
	 * @param whole true: the pattern must match the whole region (file names); false: it may match any part of it (free text)
	 */
	public static boolean matches(char[] text, int from, int to, char[] pattern, boolean whole) {
		int p = 0;
		int t = from;
		int starP = -1;
		int starT = 0;
		while (true) {
			if (p == pattern.length) {
				if (!whole || t == to) {
					return true;
				}
			} else if (pattern[p] == '*') {
				starP = p++;
				starT = t;
				continue;
			} else if (t < to && (pattern[p] == '?' || pattern[p] == Character.toLowerCase(text[t]))) {
				p++;
				t++;
				continue;
			}
			// mismatch: the last '*' takes one more character
			if (starP < 0 || starT >= to) {
				return false;
			}
			p = starP + 1;
			t = ++starT;
		}
	}

	/** Matches the pattern against the whole text (case insensitive): a file name */
	public static boolean matchesWhole(String text, String pattern) {
		char[] t = text.toCharArray();
		return matches(t, 0, t.length, lowerCase(pattern), true);
	}

	/** Matches the pattern against any part of the text (case insensitive): free text, like a plain search */
	public static boolean matchesAnyPart(String text, String pattern) {
		// a leading '*' lets the match start anywhere: one pass over the text
		char[] lower = lowerCase(pattern);
		char[] p = new char[lower.length + 1];
		p[0] = '*';
		System.arraycopy(lower, 0, p, 1, lower.length);
		char[] t = text.toCharArray();
		return matches(t, 0, t.length, p, false);
	}
}
