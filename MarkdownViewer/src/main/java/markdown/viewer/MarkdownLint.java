package markdown.viewer;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Document check (offline): broken relative links, images and "#anchors", undefined reference links, heading structure,
 * unclosed code fences and tables with uneven columns. Web links are not visited.
 */
public final class MarkdownLint {

	public enum Level { ERROR, WARNING, INFO }

	/** One finding: 1-based line, what and how serious. */
	public static final class Issue {
		public final int line;
		public final Level level;
		public final String message;

		Issue(int line, Level level, String message) {
			this.line = line;
			this.level = level;
			this.message = message;
		}

		@Override
		public String toString() {
			return level + " line " + line + ": " + message;
		}
	}

	private static final Pattern INLINE_LINK = Pattern.compile("(!?)\\[([^\\]]*)\\]\\(\\s*(<[^>]*>|[^)\\s]*)(?:\\s+(?:\"[^\"]*\"|'[^']*'))?\\s*\\)");
	private static final Pattern REF_LINK = Pattern.compile("(!?)\\[([^\\]]+)\\]\\[([^\\]]*)\\]");
	private static final Pattern REF_DEF = Pattern.compile("^ {0,3}\\[([^\\]]+)\\]:\\s*\\S+");
	private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})");
	private static final Pattern EXPLICIT_ANCHOR = Pattern.compile("(?:id|name)=[\"']([^\"']+)[\"']");
	private static final Pattern SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");

	private MarkdownLint() {
	}

	/** @param baseDir folder of the file (relative links are checked from it), null when it was never saved: then only "#anchors" are checked */
	public static List<Issue> check(String markdown, File baseDir) {
		List<Issue> issues = new ArrayList<Issue>();
		String[] lines = MarkdownText.lines(markdown);
		int start = MarkdownText.frontMatterEnd(lines);

		Set<String> anchors = new HashSet<String>();
		for(MarkdownText.HeadingRef h : MarkdownText.headings(markdown))
			anchors.add(h.slug);
		Matcher explicit = EXPLICIT_ANCHOR.matcher(markdown);
		while(explicit.find())
			anchors.add(explicit.group(1));

		Set<String> definitions = new HashSet<String>();
		for(int i = start; i < lines.length; i++) {
			Matcher d = REF_DEF.matcher(MarkdownText.stripCr(lines[i]));
			if(d.find())
				definitions.add(d.group(1).trim().toLowerCase(Locale.ROOT));
		}

		String fence = null;
		int fenceLine = -1;
		int previousLevel = 0, h1 = 0;
		Map<String, Integer> seenHeadings = new HashMap<String, Integer>();
		Map<String, List<String>> otherFiles = new HashMap<String, List<String>>();

		for(int i = start; i < lines.length; i++) {
			String line = MarkdownText.stripCr(lines[i]);
			Matcher f = FENCE.matcher(line);
			if(fence != null) {
				if(f.find() && f.group(1).charAt(0) == fence.charAt(0) && f.group(1).length() >= fence.length() && line.trim().equals(f.group(1)))
					fence = null;
				continue;
			}
			if(f.find()) {
				fence = f.group(1);
				fenceLine = i;
				continue;
			}
			String code = blankCodeSpans(line);

			Matcher heading = Pattern.compile("^ {0,3}(#{1,6})[ \\t]+(.*?)[ \\t]*$").matcher(code);
			if(heading.matches()) {
				int level = heading.group(1).length();
				String text = MarkdownText.plainText(heading.group(2).replaceAll("[ \\t]+#+[ \\t]*$", "")).trim().toLowerCase(Locale.ROOT);
				if(level == 1 && ++h1 == 2)
					issues.add(new Issue(i + 1, Level.INFO, "Second level 1 heading (one title per document is the usual rule)"));
				if(previousLevel > 0 && level > previousLevel + 1)
					issues.add(new Issue(i + 1, Level.WARNING, "Heading level jumps from " + previousLevel + " to " + level));
				previousLevel = level;
				if(!text.isEmpty()) {
					Integer first = seenHeadings.get(level + ":" + text);
					if(first != null)
						issues.add(new Issue(i + 1, Level.INFO, "Duplicate heading (same as line " + first + "): its anchor gets a -1 suffix"));
					else
						seenHeadings.put(level + ":" + text, i + 1);
				}
			}

			Matcher m = INLINE_LINK.matcher(code);
			while(m.find()) {
				boolean image = !m.group(1).isEmpty();
				String target = m.group(3);
				if(target.startsWith("<") && target.endsWith(">"))
					target = target.substring(1, target.length() - 1);
				if(image && m.group(2).trim().isEmpty())
					issues.add(new Issue(i + 1, Level.INFO, "Image without alternative text: " + shorten(target)));
				if(target.isEmpty()) {
					issues.add(new Issue(i + 1, Level.ERROR, (image ? "Image" : "Link") + " with an empty target: [" + shorten(m.group(2)) + "]()"));
					continue;
				}
				checkTarget(issues, i + 1, target, anchors, baseDir, otherFiles);
			}

			Matcher r = REF_LINK.matcher(code);
			while(r.find()) {
				String id = (r.group(3).isEmpty() ? r.group(2) : r.group(3)).trim().toLowerCase(Locale.ROOT);
				if(!definitions.contains(id))
					issues.add(new Issue(i + 1, Level.WARNING, "Reference [" + shorten(id) + "] has no definition ([" + shorten(id) + "]: url)"));
			}
		}
		if(fence != null)
			issues.add(new Issue(fenceLine + 1, Level.ERROR, "Code fence " + fence + " is never closed: everything after it is code"));

		checkTables(issues, lines, start);
		java.util.Collections.sort(issues, new java.util.Comparator<Issue>() {
			@Override
			public int compare(Issue a, Issue b) {
				return a.line != b.line ? Integer.compare(a.line, b.line) : a.level.compareTo(b.level);
			}
		});
		return issues;
	}

	private static void checkTarget(List<Issue> issues, int line, String target, Set<String> anchors, File baseDir, Map<String, List<String>> otherFiles) {
		if(SCHEME.matcher(target).matches() || target.startsWith("//") || target.startsWith("/"))
			return; // web, mailto, data... : not visited
		if(target.startsWith("#")) {
			String id = decode(target.substring(1));
			if(!id.isEmpty() && !anchors.contains(id) && !anchors.contains(id.toLowerCase(Locale.ROOT)))
				issues.add(new Issue(line, Level.ERROR, "No heading for the anchor " + target + " in this document"));
			return;
		}
		if(baseDir == null)
			return;
		String path = target, fragment = null;
		int hash = path.indexOf('#');
		if(hash >= 0) {
			fragment = path.substring(hash + 1);
			path = path.substring(0, hash);
		}
		int query = path.indexOf('?');
		if(query >= 0)
			path = path.substring(0, query);
		if(path.isEmpty())
			return;
		File file = new File(baseDir, decode(path));
		if(!file.exists()) {
			issues.add(new Issue(line, Level.ERROR, "File not found: " + shorten(path)));
			return;
		}
		String name = file.getName().toLowerCase(Locale.ROOT);
		if(fragment != null && !fragment.isEmpty() && file.isFile() && (name.endsWith(".md") || name.endsWith(".markdown"))) {
			String key = file.getAbsolutePath();
			List<String> slugs = otherFiles.get(key);
			if(slugs == null) {
				slugs = new ArrayList<String>();
				try {
					String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
					for(MarkdownText.HeadingRef h : MarkdownText.headings(text))
						slugs.add(h.slug);
				} catch (IOException e) {
					slugs = null;
				}
				otherFiles.put(key, slugs);
			}
			String id = decode(fragment);
			if(slugs != null && !slugs.contains(id) && !slugs.contains(id.toLowerCase(Locale.ROOT)))
				issues.add(new Issue(line, Level.ERROR, "No heading #" + shorten(fragment) + " in " + shorten(path)));
		}
	}

	private static void checkTables(List<Issue> issues, String[] lines, int start) {
		String fence = null;
		for(int i = start; i < lines.length - 1; i++) {
			String line = MarkdownText.stripCr(lines[i]);
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
			if(!MarkdownText.isTableLine(line) || !MarkdownText.isTableLine(lines[i + 1]) || !MarkdownText.isDelimiterRow(lines[i + 1]) || MarkdownText.isDelimiterRow(line))
				continue;
			int columns = MarkdownText.splitRow(line).size();
			int j = i + 2;
			while(j < lines.length && MarkdownText.isTableLine(lines[j])) {
				int cells = MarkdownText.splitRow(lines[j]).size();
				if(cells != columns)
					issues.add(new Issue(j + 1, Level.WARNING, "Table row has " + cells + " cells, the header has " + columns));
				j++;
			}
			if(MarkdownText.splitRow(lines[i + 1]).size() != columns)
				issues.add(new Issue(i + 2, Level.WARNING, "The |---| row has " + MarkdownText.splitRow(lines[i + 1]).size() + " cells, the header has " + columns));
			i = j - 1;
		}
	}

	/** Code spans replaced by spaces of the same length, so that links inside them are not checked. */
	static String blankCodeSpans(String line) {
		if(line.indexOf('`') < 0)
			return line;
		StringBuilder sb = new StringBuilder(line);
		int i = 0;
		while(i < line.length()) {
			if(line.charAt(i) != '`') {
				i++;
				continue;
			}
			int run = 0;
			while(i + run < line.length() && line.charAt(i + run) == '`')
				run++;
			int close = -1;
			for(int j = i + run; j < line.length(); j++) {
				if(line.charAt(j) != '`')
					continue;
				int r = 0;
				while(j + r < line.length() && line.charAt(j + r) == '`')
					r++;
				if(r == run) {
					close = j;
					break;
				}
				j += r - 1;
			}
			if(close < 0) {
				i += run;
				continue;
			}
			for(int k = i; k < close + run; k++)
				sb.setCharAt(k, ' ');
			i = close + run;
		}
		return sb.toString();
	}

	private static String decode(String s) {
		try {
			return URLDecoder.decode(s.replace("+", "%2B"), "UTF-8");
		} catch (Exception e) {
			return s;
		}
	}

	private static String shorten(String s) {
		return s.length() > 60 ? s.substring(0, 57) + "..." : s;
	}
}
