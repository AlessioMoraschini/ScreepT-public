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
package gui.commons.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Estimated strength of a password: bits from the length and from the character classes it uses, less the penalties
 * for the weak patterns everybody tries first (repeats, sequences, keyboard walks, famous words).
 * Pure logic, no GUI: the password tools show the level and the tips.
 */
public final class PasswordStrength {

	/** Security levels from the worst to the best: a password reaches the level when its bits are at least minBits */
	public enum Level {
		HACKERS_BIRTHDAY_GIFT("Hacker's birthday gift", 0),
		GRANDMA("Grandma's password", 28),
		SCRIPT_KIDDIE("Script kiddie's first try", 40),
		BORED_TEENAGER("Bored teenager, one evening", 56),
		STUBBORN_HACKER("Stubborn hacker, one weekend", 72),
		MR_ROBOT("Mr. Robot is worried", 88),
		FBI("Now even FBI can't hack you :)", 104);

		private final String title;
		private final int minBits;

		Level(String title, int minBits) {
			this.title = title;
			this.minBits = minBits;
		}

		public String getTitle() {
			return title;
		}

		public int getMinBits() {
			return minBits;
		}
	}

	private static final String[] KEYBOARD_WALKS = {"qwerty", "azerty", "qwertz", "asdf", "zxcv", "1234", "abcd", "4321"};

	private static final String[] FAMOUS_WORDS = {"password", "passw0rd", "admin", "welcome", "letmein", "iloveyou",
			"monkey", "dragon", "master", "login", "sunshine", "princess", "football", "trustno1", "batman", "superman",
			"123456", "111111", "abc123", "hello", "secret"};

	private static final int PENALTY_REPEAT = 6;
	private static final int PENALTY_SEQUENCE = 8;
	private static final int PENALTY_WALK = 15;
	/** A famous word caps the strength: it is the first thing tried by any attacker */
	private static final int FAMOUS_WORD_CAP = 20;

	private PasswordStrength() {
	}

	/** Result of the analysis: the level is null only for an empty password */
	public static final class Result {
		private final Level level;
		private final int length;
		private final int bits;
		private final List<String> tips;

		private Result(Level level, int length, int bits, List<String> tips) {
			this.level = level;
			this.length = length;
			this.bits = bits;
			this.tips = tips;
		}

		public Level getLevel() {
			return level;
		}

		public int getLength() {
			return length;
		}

		/** Estimated bits of entropy, penalties included */
		public int getBits() {
			return bits;
		}

		/** What to do to get stronger, most important first: never empty for a non empty password */
		public List<String> getTips() {
			return tips;
		}
	}

	/** Analysis of the password: the text itself is never stored */
	public static Result analyze(String password) {
		if (password == null || password.isEmpty()) {
			return new Result(null, 0, 0, Collections.<String>emptyList());
		}

		int length = password.length();
		boolean hasLower = false;
		boolean hasUpper = false;
		boolean hasDigit = false;
		boolean hasSymbol = false;
		boolean hasOther = false;
		for (int i = 0; i < length; i++) {
			char c = password.charAt(i);
			if (c >= 'a' && c <= 'z') {
				hasLower = true;
			} else if (c >= 'A' && c <= 'Z') {
				hasUpper = true;
			} else if (c >= '0' && c <= '9') {
				hasDigit = true;
			} else if (c < 128) {
				hasSymbol = true;
			} else {
				hasOther = true;
			}
		}
		int pool = (hasLower ? 26 : 0) + (hasUpper ? 26 : 0) + (hasDigit ? 10 : 0) + (hasSymbol ? 33 : 0) + (hasOther ? 64 : 0);
		double bitsPerChar = Math.log(pool) / Math.log(2);

		int repeats = countRepeats(password);
		int sequences = countSequences(password);
		int walks = countContaining(password.toLowerCase(Locale.ROOT), KEYBOARD_WALKS);
		int famous = countContaining(password.toLowerCase(Locale.ROOT), FAMOUS_WORDS);

		double bits = length * bitsPerChar - repeats * PENALTY_REPEAT - sequences * PENALTY_SEQUENCE - walks * PENALTY_WALK;
		if (famous > 0) {
			bits = Math.min(bits, FAMOUS_WORD_CAP);
		}
		bits = Math.max(0, bits);
		int roundedBits = (int) Math.round(bits);

		Level level = levelFor(roundedBits);
		Level next = level.ordinal() + 1 < Level.values().length ? Level.values()[level.ordinal() + 1] : null;
		boolean penaltyFree = repeats == 0 && sequences == 0 && walks == 0 && famous == 0;

		List<String> tips = new ArrayList<String>();
		if (length < 12) {
			tips.add("Use at least 12 characters (now " + length + ")");
		}
		if (!hasUpper) {
			tips.add("Add uppercase letters");
		}
		if (!hasLower) {
			tips.add("Add lowercase letters");
		}
		if (!hasDigit) {
			tips.add("Add digits");
		}
		if (!hasSymbol) {
			tips.add("Add symbols, like !@#$%");
		}
		if (repeats > 0) {
			tips.add("Avoid 3 identical characters in a row");
		}
		if (sequences > 0) {
			tips.add("Avoid sequences like abc or 321");
		}
		if (walks > 0) {
			tips.add("Avoid keyboard walks like qwerty");
		}
		if (famous > 0) {
			tips.add("Avoid famous passwords (password, admin, letmein...)");
		}
		if (next != null) {
			if (penaltyFree) {
				int moreChars = (int) Math.ceil((next.getMinBits() - bits) / bitsPerChar);
				tips.add("+" + Math.max(1, moreChars) + " characters reach \"" + next.getTitle() + "\"");
			} else {
				tips.add("Fix the points above to reach \"" + next.getTitle() + "\"");
			}
		} else if (tips.isEmpty()) {
			tips.add("Nothing left to fix. Enjoy being unhackable :)");
		}

		return new Result(level, length, roundedBits, tips);
	}

	private static Level levelFor(int bits) {
		Level[] levels = Level.values();
		for (int i = levels.length - 1; i >= 0; i--) {
			if (bits >= levels[i].getMinBits()) {
				return levels[i];
			}
		}
		return levels[0];
	}

	/** Three equal characters in a row, counted once for each run of them */
	private static int countRepeats(String s) {
		int count = 0;
		for (int i = 2; i < s.length(); i++) {
			if (s.charAt(i) == s.charAt(i - 1) && s.charAt(i) == s.charAt(i - 2)) {
				count++;
			}
		}
		return count;
	}

	/** Three letters or digits in ascending or descending order, like abc or 321 */
	private static int countSequences(String s) {
		int count = 0;
		String lower = s.toLowerCase(Locale.ROOT);
		for (int i = 2; i < lower.length(); i++) {
			char a = lower.charAt(i - 2);
			char b = lower.charAt(i - 1);
			char c = lower.charAt(i);
			if (isAlnum(a) && isAlnum(b) && isAlnum(c)
					&& ((b == a + 1 && c == b + 1) || (b == a - 1 && c == b - 1))) {
				count++;
			}
		}
		return count;
	}

	private static boolean isAlnum(char c) {
		return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
	}

	/** How many of the given words are inside the text (lower case) */
	private static int countContaining(String lowerText, String[] words) {
		int count = 0;
		for (String word : words) {
			if (lowerText.contains(word)) {
				count++;
			}
		}
		return count;
	}
}
