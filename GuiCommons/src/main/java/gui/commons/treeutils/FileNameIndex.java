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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import various.common.light.files.om.FileNamed;
import various.common.light.utility.string.WildcardPattern;

/**
 * In-memory index of the names under a folder, read once from disk and searched many times: typing in the filter of the file tree
 * costs no disk access, a search is a scan of one char array (milliseconds for hundreds of thousands of names).
 * <p>
 * Compact on purpose: all the names are written one after the other in a single char array (each followed by a NUL), with three
 * primitive arrays (where a name starts, its parent, its kind): about 40 bytes per entry, no object per name. The disk is read with
 * {@link Files#walkFileTree}, which gives the attributes together with the listing (no extra call for each file). Folders
 * excluded by the {@link TreeFilterRules} are not read at all; symbolic links and junctions are listed but never followed.
 * <p>
 * An index is immutable once built: a new one is built when the folder or the rules change, so a search never sees a half
 * updated state.
 */
public final class FileNameIndex {

	/** Entries kept at most (memory about 25 MB): over it the index is cut and {@link #isTruncated()} tells it */
	public static final int MAX_ENTRIES = 600000;

	private static final byte DIR = 1;
	private static final byte LINK = 2;

	private final File root;
	private final long builtAt = System.currentTimeMillis();
	private char[] pool = new char[1 << 16];
	private int poolLength;
	private int[] start = new int[1 << 12];
	private int[] parent = new int[1 << 12];
	private byte[] flags = new byte[1 << 12];
	private int size;
	private boolean truncated;

	private FileNameIndex(File root) {
		this.root = root;
	}

	/** Result of a search: the entries (in index order) and whether the search had to stop at the limit */
	public static final class Matches {
		public final int[] entries;
		public final boolean overLimit;

		Matches(int[] entries, boolean overLimit) {
			this.entries = entries;
			this.overLimit = overLimit;
		}
	}

	/**
	 * Reads the folder with the rules. Returns null when cancelled.
	 *
	 * @param cancelled polled at every entry: true stops the reading
	 * @param progress receives the number of entries found so far (every few thousands)
	 */
	public static FileNameIndex build(File root, TreeFilterRules rules, BooleanSupplier cancelled, IntConsumer progress) {
		final FileNameIndex index = new FileNameIndex(root);
		final Path rootPath = root.toPath();
		final int[] stack = new int[4096];
		final int[] depth = { 0 };
		final boolean[] stopped = { false };
		try {
			Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
				private int[] parents = stack;

				private FileVisitResult check() {
					if (cancelled != null && cancelled.getAsBoolean()) {
						stopped[0] = true;
						return FileVisitResult.TERMINATE;
					}
					if (index.size >= MAX_ENTRIES) {
						index.truncated = true;
						return FileVisitResult.TERMINATE;
					}
					if ((index.size & 0x1FFF) == 0 && index.size > 0 && progress != null) {
						progress.accept(index.size);
					}
					return null;
				}

				private void push(int entry) {
					if (depth[0] == parents.length) {
						parents = Arrays.copyOf(parents, parents.length * 2);
					}
					parents[depth[0]++] = entry;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					if (dir.equals(rootPath)) {
						push(-1);
						return FileVisitResult.CONTINUE;
					}
					FileVisitResult stop = check();
					if (stop != null) {
						return stop;
					}
					String name = String.valueOf(dir.getFileName());
					boolean link = attrs.isOther() || attrs.isSymbolicLink();
					if (rules != null && rules.excludes(name, link ? TreeFilterRules.Kind.SYMLINK : TreeFilterRules.Kind.FOLDER)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					int id = index.add(name, parents[depth[0] - 1], link ? LINK : DIR);
					if (link) {
						return FileVisitResult.SKIP_SUBTREE; // junctions and the like: listed, never followed
					}
					push(id);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					FileVisitResult stop = check();
					if (stop != null) {
						return stop;
					}
					String name = String.valueOf(file.getFileName());
					boolean link = attrs.isSymbolicLink();
					if (rules != null && rules.excludes(name, link ? TreeFilterRules.Kind.SYMLINK : TreeFilterRules.Kind.FILE)) {
						return FileVisitResult.CONTINUE;
					}
					index.add(name, parents[depth[0] - 1], link ? LINK : 0);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {
					return FileVisitResult.CONTINUE; // no permission, vanished meanwhile...
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
					depth[0]--;
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException | RuntimeException e) {
			if (index.size == 0) {
				return stopped[0] ? null : index;
			}
		}
		if (stopped[0]) {
			return null;
		}
		index.start = Arrays.copyOf(index.start, index.size + 1);
		index.start[index.size] = index.poolLength;
		index.pool = Arrays.copyOf(index.pool, index.poolLength);
		index.parent = Arrays.copyOf(index.parent, index.size);
		index.flags = Arrays.copyOf(index.flags, index.size);
		return index;
	}

	private int add(String name, int parentId, byte kind) {
		if (size == start.length) {
			int cap = size * 2;
			start = Arrays.copyOf(start, cap);
			parent = Arrays.copyOf(parent, cap);
			flags = Arrays.copyOf(flags, cap);
		}
		int need = poolLength + name.length() + 1;
		if (need > pool.length) {
			pool = Arrays.copyOf(pool, Math.max(need, pool.length * 2));
		}
		start[size] = poolLength;
		parent[size] = parentId;
		flags[size] = kind;
		name.getChars(0, name.length(), pool, poolLength);
		poolLength += name.length();
		pool[poolLength++] = 0;
		return size++;
	}

	public File getRoot() {
		return root;
	}

	public int size() {
		return size;
	}

	public boolean isTruncated() {
		return truncated;
	}

	public long getBuiltAt() {
		return builtAt;
	}

	/** Approximate memory held by the index, in bytes */
	public long memoryBytes() {
		return pool.length * 2L + start.length * 4L + parent.length * 4L + flags.length;
	}

	private static char lower(char c) {
		if (c < 128) {
			return c >= 'A' && c <= 'Z' ? (char) (c + 32) : c;
		}
		return Character.toLowerCase(c);
	}

	/**
	 * Entries whose name contains the text (case insensitive). With '*' or '?' the whole name has to match the pattern instead
	 * (see {@link WildcardPattern}).
	 *
	 * @param within a previous result of a query this one refines (the new text contains the old one): only those are checked;
	 *               null searches everything
	 * @param limit stops when this many entries were found (the result says so)
	 */
	public Matches search(String query, int[] within, int limit) {
		char[] q = query.toCharArray();
		for (int i = 0; i < q.length; i++) {
			q[i] = lower(q[i]);
		}
		if (q.length == 0 || size == 0) {
			return new Matches(new int[0], false);
		}
		if (WildcardPattern.hasWildcard(query)) {
			return searchPattern(q, within, limit);
		}
		int[] found = new int[256];
		int count = 0;
		boolean over = false;
		if (within != null) {
			for (int e : within) {
				if (containsIn(start[e], start[e + 1] - 1, q)) {
					if (count == limit) {
						over = true;
						break;
					}
					if (count == found.length) {
						found = Arrays.copyOf(found, count * 2);
					}
					found[count++] = e;
				}
			}
			return new Matches(Arrays.copyOf(found, count), over);
		}
		char first = q[0];
		int last = poolLength - q.length;
		int entry = 0;
		int i = 0;
		while (i <= last) {
			if (lower(pool[i]) == first && regionMatches(i, q)) {
				while (start[entry + 1] <= i) {
					entry++;
				}
				if (count == limit) {
					over = true;
					break;
				}
				if (count == found.length) {
					found = Arrays.copyOf(found, count * 2);
				}
				found[count++] = entry;
				i = start[entry + 1]; // next entry: one match is enough
			} else {
				i++;
			}
		}
		return new Matches(Arrays.copyOf(found, count), over);
	}

	/** Wildcard search: the pattern has to match the whole name, as a file name does */
	private Matches searchPattern(char[] pattern, int[] within, int limit) {
		int[] found = new int[256];
		int count = 0;
		boolean over = false;
		int n = within != null ? within.length : size;
		for (int k = 0; k < n; k++) {
			int e = within != null ? within[k] : k;
			if (WildcardPattern.matches(pool, start[e], start[e + 1] - 1, pattern, true)) {
				if (count == limit) {
					over = true;
					break;
				}
				if (count == found.length) {
					found = Arrays.copyOf(found, count * 2);
				}
				found[count++] = e;
			}
		}
		return new Matches(Arrays.copyOf(found, count), over);
	}

	private boolean regionMatches(int at, char[] q) {
		for (int k = 1; k < q.length; k++) {
			if (lower(pool[at + k]) != q[k]) {
				return false;
			}
		}
		return true;
	}

	private boolean containsIn(int from, int to, char[] q) {
		for (int i = from; i <= to - q.length; i++) {
			if (lower(pool[i]) == q[0] && regionMatches(i, q)) {
				return true;
			}
		}
		return false;
	}

	/** What the tree shows for a set of matches: for each folder, the children (folders first, then by name) */
	public static final class View {
		/** Children by absolute path of the folder */
		public final Map<String, FileNamed[]> children = new HashMap<String, FileNamed[]>();
		/** Folders to open to see every match (the ones that have children in the view) */
		public final List<File> branches = new ArrayList<File>();
		public int matches;
	}

	/**
	 * Builds the view of the matches: every match with the folders that lead to it. Runs off the interface thread: the objects
	 * of the tree are made here, so showing the result needs no disk access.
	 */
	public View view(int[] matches, BooleanSupplier cancelled) {
		View view = new View();
		view.matches = matches.length;
		File[] files = new File[size];
		boolean[] included = new boolean[size];
		List<Integer> order = new ArrayList<Integer>();
		for (int m : matches) {
			for (int e = m; e >= 0 && !included[e]; e = parent[e]) {
				included[e] = true;
				order.add(e);
			}
			if (cancelled != null && cancelled.getAsBoolean()) {
				return null;
			}
		}
		Map<Integer, List<FileNamed>> byParent = new HashMap<Integer, List<FileNamed>>();
		for (int e : order) {
			File f = fileOf(e, files);
			List<FileNamed> list = byParent.get(parent[e]);
			if (list == null) {
				list = new ArrayList<FileNamed>();
				byParent.put(parent[e], list);
			}
			list.add(new FileNamed(f, (flags[e] & DIR) != 0));
		}
		Comparator<FileNamed> byKind = new Comparator<FileNamed>() {
			@Override
			public int compare(FileNamed a, FileNamed b) {
				boolean da = a.isDirectory();
				boolean db = b.isDirectory();
				if (da != db) {
					return da ? -1 : 1;
				}
				return a.getFileName().compareToIgnoreCase(b.getFileName());
			}
		};
		for (Map.Entry<Integer, List<FileNamed>> en : byParent.entrySet()) {
			FileNamed[] array = en.getValue().toArray(new FileNamed[0]);
			Arrays.sort(array, byKind);
			File dir = en.getKey() < 0 ? root : files[en.getKey()];
			view.children.put(dir.getAbsolutePath(), array);
			if (en.getKey() >= 0) {
				view.branches.add(dir);
			}
		}
		return view;
	}

	private File fileOf(int entry, File[] memo) {
		if (memo[entry] == null) {
			File dir = parent[entry] < 0 ? root : fileOf(parent[entry], memo);
			memo[entry] = new File(dir, nameOf(entry));
		}
		return memo[entry];
	}

	private String nameOf(int entry) {
		return new String(pool, start[entry], start[entry + 1] - 1 - start[entry]);
	}
}
