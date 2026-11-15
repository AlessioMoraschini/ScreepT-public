/*
 * Copyright (c) AM-Design-Development. All rights reserved.
 */
package markdown.viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure text functions behind the smart editing of the markdown viewer (no Swing here, every method is testable):
 * GitHub style heading slugs, table of contents between {@code <!-- TOC -->} markers, table formatting,
 * list items (continuation, indentation, numbering), heading levels and the "$...$" / "$$...$$" math.
 */
public final class MarkdownText {

	public static final String TOC_START = "<!-- TOC -->";
	public static final String TOC_END = "<!-- /TOC -->";
	public static final String OMIT_IN_TOC = "<!-- omit in toc -->";

	private static final Pattern HEADING = Pattern.compile("^ {0,3}(#{1,6})(?:[ \\t]+(.*?))?[ \\t]*$");
	private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})");
	private static final Pattern LIST_ITEM = Pattern.compile("^([ \\t]*)(?:([-*+])|(\\d{1,9})([.)]))([ \\t]+)(\\[[ xX]\\][ \\t]+)?");
	private static final Pattern QUOTE = Pattern.compile("^([ \\t]*(?:>[ \\t]?)+)");
	private static final Pattern TABLE_DELIMITER_CELL = Pattern.compile("^\\s*:?-+:?\\s*$");

	private MarkdownText() {
	}

	// ------------------------------------------------------------------ headings and slugs

	/** One ATX heading of the document. */
	public static final class HeadingRef {
		public final int level;
		public final String text;
		public final String slug;
		public final int line;
		public final boolean omitted;

		HeadingRef(int level, String text, String slug, int line, boolean omitted) {
			this.level = level;
			this.text = text;
			this.slug = slug;
			this.line = line;
			this.omitted = omitted;
		}

		@Override
		public String toString() {
			return "#" + slug;
		}
	}

	/** Lines split on \n (a trailing \r is kept out of the text). */
	public static String[] lines(String text) {
		return (text == null ? "" : text).split("\n", -1);
	}

	/** ATX headings outside fenced code blocks and front matter, with unique GitHub style slugs (-1, -2... for duplicates). */
	public static List<HeadingRef> headings(String markdown) {
		List<HeadingRef> result = new ArrayList<HeadingRef>();
		Map<String, Integer> used = new HashMap<String, Integer>();
		String[] lines = lines(markdown);
		String fence = null;
		int start = frontMatterEnd(lines);
		for(int i = start; i < lines.length; i++) {
			String line = stripCr(lines[i]);
			Matcher f = FENCE.matcher(line);
			if(fence != null) {
				if(f.find() && f.group(1).charAt(0) == fence.charAt(0) && f.group(1).length() >= fence.length() && line.trim().equals(f.group(1)))
					fence = null;
				continue;
			}
			if(f.find()) {
				fence = f.group(1);
				continue;
			}
			Matcher h = HEADING.matcher(line);
			if(!h.matches())
				continue;
			String raw = h.group(2) == null ? "" : h.group(2);
			boolean omitted = raw.contains(OMIT_IN_TOC);
			raw = raw.replace(OMIT_IN_TOC, "");
			raw = raw.replaceAll("[ \\t]+#+[ \\t]*$", "").replaceAll("^#+$", "").trim();
			String base = slug(plainText(raw));
			String unique = base;
			Integer count = used.get(base);
			if(count != null) {
				int n = count;
				do {
					n++;
					unique = base + "-" + n;
				} while(used.containsKey(unique));
				used.put(base, n);
			}
			used.put(unique, used.containsKey(unique) ? used.get(unique) : 0);
			result.add(new HeadingRef(h.group(1).length(), raw, unique, i, omitted));
		}
		return result;
	}

	/** True when the line is inside a fenced code block (or is one of its fences). */
	public static boolean isInsideFence(String[] lines, int lineIndex) {
		String fence = null;
		for(int i = 0; i <= lineIndex && i < lines.length; i++) {
			String line = stripCr(lines[i]);
			Matcher f = FENCE.matcher(line);
			if(fence != null) {
				if(f.find() && f.group(1).charAt(0) == fence.charAt(0) && f.group(1).length() >= fence.length() && line.trim().equals(f.group(1))) {
					if(i == lineIndex)
						return true;
					fence = null;
				}
			} else if(f.find()) {
				fence = f.group(1);
			}
		}
		return fence != null;
	}

	/** Index of the first line after a "---" front matter, 0 when there is none. */
	private static int frontMatterEnd(String[] lines) {
		if(lines.length == 0 || !stripCr(lines[0]).equals("---"))
			return 0;
		for(int i = 1; i < lines.length; i++) {
			String l = stripCr(lines[i]);
			if(l.equals("---") || l.equals("..."))
				return i + 1;
		}
		return 0;
	}

	/** Heading text without markdown syntax: links and images become their text, emphasis and code marks go away. */
	public static String plainText(String inline) {
		String s = inline;
		s = s.replaceAll("!\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
		s = s.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
		s = s.replaceAll("\\[([^\\]]*)\\]\\[[^\\]]*\\]", "$1");
		s = s.replaceAll("<[^>]+>", "");
		s = s.replaceAll("(\\*\\*|__|~~)(.+?)\\1", "$2");
		s = s.replaceAll("(?<![\\w*])[*_](\\S(?:.*?\\S)?)[*_](?![\\w*])", "$1");
		s = s.replace("`", "");
		return s.trim();
	}

	/**
	 * GitHub style anchor: lower case, letters / digits / '_' kept, spaces and '-' become '-', everything else dropped.
	 * The preview engine is configured with the same rules, so "#slug" links work in the preview too.
	 */
	public static String slug(String text) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(Character.isLetterOrDigit(c) || c == '_') {
				sb.append(Character.toLowerCase(c));
			} else if(c == ' ' || c == '-') {
				sb.append('-');
			}
		}
		return sb.toString();
	}

	/** Markdown nested list of links to the headings (the ones marked {@code <!-- omit in toc -->} are left out). */
	public static String buildToc(String markdown, String eol) {
		List<HeadingRef> all = headings(markdown);
		int minLevel = 7;
		for(HeadingRef h : all) {
			if(!h.omitted)
				minLevel = Math.min(minLevel, h.level);
		}
		StringBuilder sb = new StringBuilder();
		for(HeadingRef h : all) {
			if(h.omitted)
				continue;
			sb.append(repeat(' ', (h.level - minLevel) * 2)).append("- [").append(linkText(h.text)).append("](#").append(h.slug).append(')').append(eol);
		}
		return sb.toString();
	}

	private static String linkText(String headingText) {
		String s = headingText.replaceAll("!\\[([^\\]]*)\\]\\([^)]*\\)", "$1").replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
		return s.replace("[", "\\[").replace("]", "\\]");
	}

	/** Offsets [start, end) of the text between the TOC markers (after the start marker line, before the end marker), or null. */
	public static int[] tocRange(String markdown) {
		int s = markdown.indexOf(TOC_START);
		if(s < 0)
			return null;
		int e = markdown.indexOf(TOC_END, s + TOC_START.length());
		if(e < 0)
			return null;
		int contentStart = markdown.indexOf('\n', s);
		if(contentStart < 0 || contentStart > e)
			return null;
		int contentEnd = e;
		while(contentEnd > contentStart + 1 && markdown.charAt(contentEnd - 1) == ' ')
			contentEnd--;
		return new int[] {contentStart + 1, contentEnd};
	}

	/** End of line used by the text ("\r\n" when its first line ends with it). */
	public static String eol(String text) {
		int n = text.indexOf('\n');
		return n > 0 && text.charAt(n - 1) == '\r' ? "\r\n" : "\n";
	}

	// ------------------------------------------------------------------ heading level

	/** The line with its heading level changed by delta (0 = plain paragraph, max 6). */
	public static String changeHeadingLevel(String line, int delta) {
		Matcher h = Pattern.compile("^( {0,3})(#{1,6})(?:[ \\t]+|$)").matcher(line);
		int level = 0;
		String rest = line;
		String indent = "";
		if(h.find()) {
			indent = h.group(1);
			level = h.group(2).length();
			rest = line.substring(h.end());
		}
		int next = Math.max(0, Math.min(6, level + delta));
		return next == 0 ? indent + rest : indent + repeat('#', next) + " " + rest;
	}

	// ------------------------------------------------------------------ lists

	/** Prefix of a list item / quote line. */
	public static final class ListPrefix {
		/** Leading spaces / tabs. */
		public final String indent;
		/** "-", "*", "+" or the number with its "." / ")". Empty for a quote. */
		public final String marker;
		/** Number of an ordered item, -1 for bullets. */
		public final int number;
		/** "." or ")" for ordered items. */
		public final String delimiter;
		/** Spaces after the marker. */
		public final String gap;
		/** "[ ] " / "[x] " of a task, else empty. */
		public final String task;
		/** Quote prefix ("> ") when this is a quote line. */
		public final String quote;
		/** Length of the whole prefix. */
		public final int length;

		ListPrefix(String indent, String marker, int number, String delimiter, String gap, String task, String quote, int length) {
			this.indent = indent;
			this.marker = marker;
			this.number = number;
			this.delimiter = delimiter;
			this.gap = gap;
			this.task = task;
			this.quote = quote;
			this.length = length;
		}

		public boolean isQuote() {
			return quote != null;
		}

		public boolean isOrdered() {
			return number >= 0;
		}

		/** Columns used by the indentation (a tab counts 4). */
		public int indentWidth() {
			return width(indent);
		}

		/** Width of the marker plus its gap: the indentation a child item needs. */
		public int contentOffset() {
			return marker.length() + gap.length();
		}

		/** Prefix of the next item of the same list: number + 1, empty task box. */
		public String next() {
			if(isQuote())
				return quote;
			String m = isOrdered() ? (number + 1) + delimiter : marker;
			return indent + m + gap + (task.isEmpty() ? "" : "[ ] ");
		}
	}

	/** List item (or quote) prefix of the line, null when the line is neither. */
	public static ListPrefix listPrefix(String line) {
		Matcher m = LIST_ITEM.matcher(line);
		if(m.find()) {
			boolean ordered = m.group(3) != null;
			String task = m.group(6) == null ? "" : m.group(6);
			return new ListPrefix(m.group(1), ordered ? m.group(3) + m.group(4) : m.group(2), ordered ? parseInt(m.group(3)) : -1,
					ordered ? m.group(4) : "", m.group(5), task, null, m.end());
		}
		// a marker alone at the end of the line ("-" / "1." just typed) is an empty item too
		Matcher bare = Pattern.compile("^([ \\t]*)(?:([-*+])|(\\d{1,9})([.)]))$").matcher(line);
		if(bare.find()) {
			boolean ordered = bare.group(3) != null;
			return new ListPrefix(bare.group(1), ordered ? bare.group(3) + bare.group(4) : bare.group(2), ordered ? parseInt(bare.group(3)) : -1,
					ordered ? bare.group(4) : "", " ", "", null, bare.end());
		}
		Matcher q = QUOTE.matcher(line);
		if(q.find()) {
			String quote = q.group(1);
			if(!quote.endsWith(" "))
				quote = quote + " ";
			return new ListPrefix("", "", -1, "", "", "", quote, q.end());
		}
		return null;
	}

	/**
	 * Numbers again the ordered lists of the block: each run of items at the same indentation keeps the number of its
	 * first item and counts up from there. Returns the new lines (same count, same order).
	 */
	public static String[] renumber(String[] lines) {
		String[] out = lines.clone();
		// runs currently open: {indentation, next number, 1 = ordered}
		List<int[]> open = new ArrayList<int[]>();
		for(int i = 0; i < out.length; i++) {
			String line = stripCr(out[i]);
			String cr = out[i].endsWith("\r") ? "\r" : "";
			if(line.trim().isEmpty())
				continue;
			ListPrefix p = listPrefix(line);
			int indent = leadingWidth(line);
			// a line less indented closes the deeper runs
			while(!open.isEmpty() && open.get(open.size() - 1)[0] > indent)
				open.remove(open.size() - 1);
			if(p == null || p.isQuote()) {
				if(indent == 0)
					open.clear();
				continue;
			}
			int[] top = open.isEmpty() ? null : open.get(open.size() - 1);
			if(top != null && top[0] == indent && (top[2] == 1) == p.isOrdered()) {
				if(p.isOrdered()) {
					int number = top[1];
					top[1] = number + 1;
					if(number != p.number)
						out[i] = p.indent + number + p.delimiter + line.substring(p.indent.length() + p.marker.length()) + cr;
				}
				continue;
			}
			if(top != null && top[0] == indent)
				open.remove(open.size() - 1); // bullets where the numbers were (or the other way round): a new list
			open.add(new int[] {indent, p.isOrdered() ? p.number + 1 : 0, p.isOrdered() ? 1 : 0});
		}
		return out;
	}

	// ------------------------------------------------------------------ tables

	/** True when the line can be a row of a pipe table. */
	public static boolean isTableLine(String line) {
		String t = stripCr(line).trim();
		return t.length() > 1 && t.indexOf('|') >= 0 && !t.startsWith("    ");
	}

	/** True when the line is the "|---|:---:|" delimiter row. */
	public static boolean isDelimiterRow(String line) {
		List<String> cells = splitRow(line);
		if(cells.isEmpty())
			return false;
		for(String c : cells) {
			if(!TABLE_DELIMITER_CELL.matcher(c).matches())
				return false;
		}
		return true;
	}

	/** Cells of a table row (outer pipes removed, escaped "\|" kept inside the cell, pipes inside `code` too). */
	public static List<String> splitRow(String line) {
		String t = stripCr(line).trim();
		List<String> cells = new ArrayList<String>();
		StringBuilder cell = new StringBuilder();
		boolean code = false;
		for(int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if(c == '\\' && i + 1 < t.length()) {
				cell.append(c).append(t.charAt(++i));
				continue;
			}
			if(c == '`')
				code = !code;
			if(c == '|' && !code) {
				cells.add(cell.toString());
				cell.setLength(0);
			} else {
				cell.append(c);
			}
		}
		cells.add(cell.toString());
		if(t.startsWith("|") && !cells.isEmpty())
			cells.remove(0);
		if(t.endsWith("|") && !t.endsWith("\\|") && !cells.isEmpty())
			cells.remove(cells.size() - 1);
		List<String> trimmed = new ArrayList<String>();
		for(String c : cells)
			trimmed.add(c.trim());
		return trimmed;
	}

	/**
	 * Formats a pipe table: every column as wide as its widest cell, cells padded following the alignment of the
	 * delimiter row (":---" left, ":---:" center, "---:" right). Rows with fewer cells get empty ones.
	 * Returns null when the lines are not a table (no delimiter row as second line).
	 */
	public static String[] formatTable(String[] rows) {
		if(rows.length < 2 || !isDelimiterRow(rows[1]))
			return null;
		String indent = stripCr(rows[0]).replaceAll("^([ \\t]*).*$", "$1");
		List<List<String>> table = new ArrayList<List<String>>();
		int columns = 0;
		for(String r : rows) {
			List<String> cells = splitRow(r);
			table.add(cells);
			columns = Math.max(columns, cells.size());
		}
		char[] align = new char[columns];
		List<String> delimiter = table.get(1);
		for(int c = 0; c < columns; c++) {
			String d = c < delimiter.size() ? delimiter.get(c) : "---";
			boolean left = d.startsWith(":"), right = d.endsWith(":");
			align[c] = left && right ? 'c' : right ? 'r' : left ? 'l' : 'n';
		}
		int[] widths = new int[columns];
		for(int r = 0; r < table.size(); r++) {
			if(r == 1)
				continue;
			List<String> cells = table.get(r);
			for(int c = 0; c < cells.size(); c++)
				widths[c] = Math.max(widths[c], displayWidth(cells.get(c)));
		}
		for(int c = 0; c < columns; c++)
			widths[c] = Math.max(widths[c], align[c] == 'c' ? 5 : align[c] == 'n' ? 3 : 4);

		String[] out = new String[rows.length];
		for(int r = 0; r < table.size(); r++) {
			List<String> cells = table.get(r);
			StringBuilder sb = new StringBuilder(indent).append('|');
			for(int c = 0; c < columns; c++) {
				String cell = c < cells.size() ? cells.get(c) : "";
				sb.append(' ');
				if(r == 1) {
					int dashes = widths[c] - (align[c] == 'c' ? 2 : align[c] == 'n' ? 0 : 1);
					sb.append(align[c] == 'l' || align[c] == 'c' ? ":" : "").append(repeat('-', dashes)).append(align[c] == 'r' || align[c] == 'c' ? ":" : "");
				} else {
					int pad = widths[c] - displayWidth(cell);
					int before = align[c] == 'r' ? pad : align[c] == 'c' ? pad / 2 : 0;
					sb.append(repeat(' ', before)).append(cell).append(repeat(' ', pad - before));
				}
				sb.append(" |");
			}
			out[r] = sb.toString() + (rows[r].endsWith("\r") ? "\r" : "");
		}
		return out;
	}

	/** Columns a text takes in a monospaced font (wide East Asian characters count 2). */
	public static int displayWidth(String s) {
		int w = 0;
		for(int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			i += Character.charCount(cp);
			if(Character.getType(cp) == Character.NON_SPACING_MARK || Character.getType(cp) == Character.FORMAT)
				continue;
			boolean wide = (cp >= 0x1100 && cp <= 0x115F) || (cp >= 0x2E80 && cp <= 0xA4CF) || (cp >= 0xAC00 && cp <= 0xD7A3)
					|| (cp >= 0xF900 && cp <= 0xFAFF) || (cp >= 0xFE30 && cp <= 0xFE4F) || (cp >= 0xFF00 && cp <= 0xFF60)
					|| (cp >= 0xFFE0 && cp <= 0xFFE6) || (cp >= 0x1F300 && cp <= 0x1FAFF) || (cp >= 0x20000 && cp <= 0x3FFFD);
			w += wide ? 2 : 1;
		}
		return w;
	}

	// ------------------------------------------------------------------ math

	/** Markdown with the formulas replaced by placeholders, and the formulas themselves. */
	public static final class MathExtraction {
		public final String markdown;
		public final List<String> formulas;
		public final List<Boolean> display;

		MathExtraction(String markdown, List<String> formulas, List<Boolean> display) {
			this.markdown = markdown;
			this.formulas = formulas;
			this.display = display;
		}

		public boolean isEmpty() {
			return formulas.isEmpty();
		}
	}

	private static final String MATH_TOKEN = "MDVMATHX";

	/**
	 * Takes the "$$...$$" blocks and "$...$" inline formulas out of the markdown (not inside code), so that the markdown
	 * engine does not read "_" or "*" in them as emphasis. Inline rules as pandoc: no space after the opening "$", none
	 * before the closing one, no digit right after it ("$5 and $10" stays text). "\$" is a literal dollar.
	 */
	public static MathExtraction extractMath(String markdown) {
		List<String> formulas = new ArrayList<String>();
		List<Boolean> display = new ArrayList<Boolean>();
		if(markdown == null || markdown.indexOf('$') < 0)
			return new MathExtraction(markdown == null ? "" : markdown, formulas, display);

		StringBuilder out = new StringBuilder(markdown.length());
		String[] lines = lines(markdown);
		String fence = null;
		StringBuilder block = null;
		String blockIndent = "";
		for(int i = 0; i < lines.length; i++) {
			String raw = lines[i];
			String line = stripCr(raw);
			String nl = i < lines.length - 1 ? "\n" : "";
			Matcher f = FENCE.matcher(line);
			if(block == null && fence != null) {
				if(f.find() && f.group(1).charAt(0) == fence.charAt(0) && f.group(1).length() >= fence.length() && line.trim().equals(f.group(1)))
					fence = null;
				out.append(raw).append(nl);
				continue;
			}
			if(block == null && f.find()) {
				fence = f.group(1);
				out.append(raw).append(nl);
				continue;
			}
			String trimmed = line.trim();
			if(block != null) {
				int close = trimmed.indexOf("$$");
				if(close >= 0 && trimmed.endsWith("$$")) {
					block.append(trimmed, 0, trimmed.length() - 2);
					out.append(blockIndent).append(placeholder(formulas.size(), true)).append(nl);
					formulas.add(block.toString().trim());
					display.add(Boolean.TRUE);
					block = null;
				} else {
					block.append(line).append('\n');
				}
				continue;
			}
			if(trimmed.startsWith("$$") && !line.startsWith("    ")) {
				String rest = trimmed.substring(2);
				blockIndent = line.substring(0, line.indexOf("$$"));
				if(rest.length() >= 2 && rest.endsWith("$$")) {
					formulas.add(rest.substring(0, rest.length() - 2).trim());
					display.add(Boolean.TRUE);
					out.append(blockIndent).append(placeholder(formulas.size() - 1, true)).append(nl);
				} else {
					block = new StringBuilder(rest).append(rest.isEmpty() ? "" : "\n");
				}
				continue;
			}
			out.append(extractInline(raw, formulas, display)).append(nl);
		}
		if(block != null) {
			// "$$" never closed: the text stays as it was
			out.append(blockIndent).append("$$").append(block);
		}
		return new MathExtraction(out.toString(), formulas, display);
	}

	private static String extractInline(String line, List<String> formulas, List<Boolean> display) {
		if(line.indexOf('$') < 0)
			return line;
		StringBuilder sb = new StringBuilder(line.length());
		int i = 0;
		int ticks = 0;
		while(i < line.length()) {
			char c = line.charAt(i);
			if(c == '`') {
				int run = 0;
				while(i + run < line.length() && line.charAt(i + run) == '`')
					run++;
				ticks = ticks == 0 ? run : (ticks == run ? 0 : ticks);
				sb.append(line, i, i + run);
				i += run;
				continue;
			}
			if(ticks == 0 && c == '\\' && i + 1 < line.length()) {
				sb.append(c).append(line.charAt(i + 1));
				i += 2;
				continue;
			}
			if(ticks == 0 && c == '$') {
				boolean isDisplay = i + 1 < line.length() && line.charAt(i + 1) == '$';
				int open = isDisplay ? 2 : 1;
				int end = findClosingDollar(line, i + open, isDisplay);
				if(end > 0) {
					String tex = line.substring(i + open, end);
					sb.append(placeholder(formulas.size(), false));
					formulas.add(tex);
					display.add(isDisplay);
					i = end + open;
					continue;
				}
			}
			sb.append(c);
			i++;
		}
		return sb.toString();
	}

	private static int findClosingDollar(String line, int from, boolean isDisplay) {
		if(from >= line.length() || (!isDisplay && Character.isWhitespace(line.charAt(from))))
			return -1;
		for(int j = from; j < line.length(); j++) {
			char c = line.charAt(j);
			if(c == '\\') {
				j++;
				continue;
			}
			if(c != '$')
				continue;
			if(isDisplay) {
				if(j + 1 < line.length() && line.charAt(j + 1) == '$' && j > from)
					return j;
				continue;
			}
			// an inline formula has no other "$": one that cannot close it means this was not a formula ("$5 and $10")
			if(j == from || Character.isWhitespace(line.charAt(j - 1)))
				return -1;
			if(j + 1 < line.length() && Character.isDigit(line.charAt(j + 1)))
				return -1;
			return j;
		}
		return -1;
	}

	private static String placeholder(int index, boolean block) {
		return MATH_TOKEN + (block ? "B" : "I") + index + "Z";
	}

	/** How the formulas are written back into the html. */
	public interface MathRenderer {
		String render(String tex, boolean display);
	}

	/** Puts the formulas back into the html made from the extracted markdown. */
	public static String restoreMath(String html, MathExtraction math, MathRenderer renderer) {
		if(math.isEmpty())
			return html;
		Matcher m = Pattern.compile("(<p>)?" + MATH_TOKEN + "([BI])(\\d+)Z(</p>)?").matcher(html);
		StringBuffer sb = new StringBuffer();
		while(m.find()) {
			int index = Integer.parseInt(m.group(3));
			boolean block = "B".equals(m.group(2));
			String tex = index < math.formulas.size() ? math.formulas.get(index) : "";
			boolean disp = block || (index < math.display.size() && math.display.get(index));
			String rendered = renderer.render(tex, disp);
			String keepP = block ? "" : (m.group(1) == null ? "" : m.group(1));
			String keepEndP = block ? "" : (m.group(4) == null ? "" : m.group(4));
			m.appendReplacement(sb, Matcher.quoteReplacement(keepP + rendered + keepEndP));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	// ------------------------------------------------------------------ utilities

	public static String stripCr(String s) {
		return s.endsWith("\r") ? s.substring(0, s.length() - 1) : s;
	}

	public static String repeat(char c, int times) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < times; i++)
			sb.append(c);
		return sb.toString();
	}

	public static String htmlEscape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/** Columns of the leading whitespace (tab = 4). */
	public static int leadingWidth(String line) {
		int w = 0;
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(c == ' ')
				w++;
			else if(c == '\t')
				w += 4 - (w % 4);
			else
				break;
		}
		return w;
	}

	static int width(String whitespace) {
		return leadingWidth(whitespace + "x");
	}

	private static int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	static String lowerAscii(String s) {
		return s.toLowerCase(Locale.ROOT);
	}
}
