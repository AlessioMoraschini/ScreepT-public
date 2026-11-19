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
package gui.commons.treeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exclusion rules of the file tree filter: what is skipped when the names of a folder are indexed (heavy folders such as
 * node_modules or .git, build outputs, archives...). Every rule has a name pattern with the wildcards * and ? (case insensitive),
 * the kind of entry it applies to (folder, file, symbolic link, or any) and can be switched off without deleting it.
 * A folder that matches is not read at all, and what is inside it is never found by the filter.
 * The rules are saved as one string (see {@link #serialize()}).
 */
public final class TreeFilterRules {

	/** The kind of entry a rule applies to */
	public enum Kind {
		FOLDER('D'), FILE('F'), SYMLINK('L'), ANY('A');

		final char code;

		Kind(char code) {
			this.code = code;
		}

		static Kind of(char code) {
			for (Kind k : values()) {
				if (k.code == code) {
					return k;
				}
			}
			return ANY;
		}
	}

	/** One rule: pattern, kind and whether it is active */
	public static final class Rule {
		public boolean enabled;
		public String pattern;
		public Kind kind;
		private String lower;

		public Rule(boolean enabled, String pattern, Kind kind) {
			this.enabled = enabled;
			this.pattern = pattern == null ? "" : pattern.trim();
			this.kind = kind == null ? Kind.ANY : kind;
		}

		boolean applies(String name, Kind entryKind) {
			if (!enabled || pattern.isEmpty() || (kind != Kind.ANY && kind != entryKind)) {
				return false;
			}
			if (lower == null || !lower.equals(pattern.toLowerCase(Locale.ROOT))) {
				lower = pattern.toLowerCase(Locale.ROOT);
			}
			return globMatches(lower, name.toLowerCase(Locale.ROOT));
		}

		public Rule copy() {
			return new Rule(enabled, pattern, kind);
		}
	}

	private final List<Rule> rules = new ArrayList<Rule>();
	/** Global switch: off = nothing is excluded, but every rule keeps its own on/off state */
	private boolean masterOn = true;
	/** Copy of the active rules, rebuilt when needed: the indexing thread reads it while the dialog may change the list */
	private volatile Rule[] active;

	public TreeFilterRules() {
	}

	/** Folders that are big and rarely interesting to search in */
	public static TreeFilterRules defaults() {
		TreeFilterRules r = new TreeFilterRules();
		for (String folder : new String[] { ".git", ".svn", ".hg", "node_modules", ".idea", ".gradle", "__pycache__", "target" }) {
			r.rules.add(new Rule(true, folder, Kind.FOLDER));
		}
		return r;
	}

	/** Rules from the saved string; empty or null gives the defaults */
	public static TreeFilterRules parse(String saved) {
		String text = saved == null ? "" : saved.trim();
		boolean marked = text.length() >= 3 && (text.startsWith("M0:") || text.startsWith("M1:"));
		if (!marked && text.isEmpty()) {
			return defaults();
		}
		TreeFilterRules r = new TreeFilterRules();
		if (marked) {
			r.masterOn = text.charAt(1) == '1';
			text = text.substring(3).trim();
		}
		if (text.isEmpty() || text.equals("-")) {
			return r; // the user removed every rule
		}
		for (String item : text.split(";")) {
			String[] parts = item.split("\\|", -1);
			if (parts.length != 3 || parts[2].isEmpty()) {
				continue;
			}
			r.rules.add(new Rule(parts[0].equals("1"), decode(parts[2]), Kind.of(parts[1].isEmpty() ? 'A' : parts[1].charAt(0))));
		}
		return r;
	}

	/** Saved form: the global switch (M1 on, M0 off), then the rules separated by ';', or "-" when there are none */
	public String serialize() {
		StringBuilder sb = new StringBuilder(masterOn ? "M1:" : "M0:");
		if (rules.isEmpty()) {
			return sb.append('-').toString();
		}
		boolean first = true;
		for (Rule rule : rules) {
			if (!first) {
				sb.append(';');
			}
			first = false;
			sb.append(rule.enabled ? '1' : '0').append('|').append(rule.kind.code).append('|').append(encode(rule.pattern));
		}
		return sb.toString();
	}

	public List<Rule> getRules() {
		return rules;
	}

	public boolean isMasterOn() {
		return masterOn;
	}

	public void setMasterOn(boolean masterOn) {
		this.masterOn = masterOn;
	}

	public TreeFilterRules copy() {
		TreeFilterRules c = new TreeFilterRules();
		c.masterOn = masterOn;
		for (Rule r : rules) {
			c.rules.add(r.copy());
		}
		return c;
	}

	/** True when the entry has to be left out of the index */
	public boolean excludes(String name, Kind entryKind) {
		if (!masterOn) {
			return false; // switched off globally: nothing is excluded
		}
		Rule[] current = active;
		if (current == null) {
			List<Rule> on = new ArrayList<Rule>();
			for (Rule r : rules) {
				if (r.enabled && !r.pattern.isEmpty()) {
					on.add(r.copy());
				}
			}
			current = on.toArray(new Rule[0]);
			active = current;
		}
		for (Rule r : current) {
			if (r.applies(name, entryKind)) {
				return true;
			}
		}
		return false;
	}

	/** Same rules as the other (in the same order, enabled or not)? Used to know whether an index must be rebuilt */
	public boolean sameAs(TreeFilterRules other) {
		return other != null && serialize().equals(other.serialize());
	}

	/** Wildcard match: * = any run of characters (even none), ? = exactly one; both texts already in lower case */
	static boolean globMatches(String pattern, String text) {
		int p = 0;
		int t = 0;
		int star = -1;
		int mark = 0;
		while (t < text.length()) {
			if (p < pattern.length() && (pattern.charAt(p) == '?' || pattern.charAt(p) == text.charAt(t))) {
				p++;
				t++;
			} else if (p < pattern.length() && pattern.charAt(p) == '*') {
				star = p++;
				mark = t;
			} else if (star >= 0) {
				p = star + 1;
				t = ++mark;
			} else {
				return false;
			}
		}
		while (p < pattern.length() && pattern.charAt(p) == '*') {
			p++;
		}
		return p == pattern.length();
	}

	private static String encode(String s) {
		return s.replace("%", "%25").replace(";", "%3B").replace("|", "%7C");
	}

	private static String decode(String s) {
		return s.replace("%7C", "|").replace("%3B", ";").replace("%25", "%");
	}
}
