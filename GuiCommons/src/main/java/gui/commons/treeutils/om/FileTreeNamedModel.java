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
package gui.commons.treeutils.om;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import various.common.light.files.om.FileNamed;

/**
 * File tree model. The content of every folder is read from disk once and kept (folders first, then files, both by name):
 * the tree asks for children, count and leaf status on every paint, and reading the folder each time made big folders very
 * slow. The cache is renewed by creating a new model, or by {@link #hasChangedOnDisk()} followed by a new model.
 * It can also show only the entries of a filter ({@link #setVisiblePaths(Set)}): the matching ones and their folders.
 */
public class FileTreeNamedModel implements TreeModel {

	private static final FileNamed[] NONE = new FileNamed[0];

	private static final Comparator<FileNamed> FOLDERS_FIRST = new Comparator<FileNamed>() {
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

	private static final class Entry {
		FileNamed[] all;
		FileNamed[] shown;
		int shownVersion = -1;
		long modified;
	}

	private final FileNamed mFile;
	private final Map<File, Entry> cache = new HashMap<File, Entry>();
	private final List<TreeModelListener> listeners = new CopyOnWriteArrayList<TreeModelListener>();
	private Set<String> visiblePaths;
	private int filterVersion;

	public FileTreeNamedModel(final FileNamed root) {
		mFile = root;
	}

	private synchronized Entry entry(FileNamed parent) {
		File dir = parent.mFile;
		Entry e = cache.get(dir);
		if (e == null) {
			e = new Entry();
			e.modified = dir.lastModified();
			FileNamed[] sons = parent.listFiles();
			if (sons == null || sons.length == 0) {
				e.all = NONE;
			} else {
				Arrays.sort(sons, FOLDERS_FIRST);
				e.all = sons;
			}
			cache.put(dir, e);
		}
		if (e.shownVersion != filterVersion) {
			if (visiblePaths == null) {
				e.shown = e.all;
			} else {
				List<FileNamed> kept = new ArrayList<FileNamed>();
				for (FileNamed f : e.all) {
					if (visiblePaths.contains(f.getFileAbsPath())) {
						kept.add(f);
					}
				}
				e.shown = kept.toArray(new FileNamed[0]);
			}
			e.shownVersion = filterVersion;
		}
		return e;
	}

	private FileNamed[] children(Object parent) {
		if (parent == null) {
			return NONE;
		}
		FileNamed f = (FileNamed) parent;
		if (!f.isDirectory()) {
			return NONE;
		}
		return entry(f).shown;
	}

	/** True when a folder that was read changed on disk (an entry added, removed or renamed) after it was read */
	public synchronized boolean hasChangedOnDisk() {
		for (Map.Entry<File, Entry> e : cache.entrySet()) {
			if (e.getKey().lastModified() != e.getValue().modified) {
				return true;
			}
		}
		return false;
	}

	/** Shows only the entries whose absolute path is in the set (null shows everything) and tells the tree to rebuild */
	public void setVisiblePaths(Set<String> paths) {
		synchronized (this) {
			visiblePaths = paths;
			filterVersion++;
		}
		fireStructureChanged();
	}

	public synchronized boolean isFiltered() {
		return visiblePaths != null;
	}

	private void fireStructureChanged() {
		TreeModelEvent event = new TreeModelEvent(this, new Object[] { mFile });
		for (TreeModelListener l : listeners) {
			l.treeStructureChanged(event);
		}
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listeners.add(l);
	}

	@Override
	public Object getChild(Object parent, int index) {
		FileNamed[] sons = children(parent);
		if (index < 0 || index >= sons.length) {
			return new FileNamed("");
		}
		return sons[index];
	}

	@Override
	public int getChildCount(Object parent) {
		return children(parent).length;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent == null || child == null) {
			return -1;
		}
		FileNamed[] sons = children(parent);
		for (int i = 0; i < sons.length; i++) {
			if (sons[i].equals(child)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Object getRoot() {
		return this.mFile;
	}

	@Override
	public boolean isLeaf(Object node) {
		FileNamed f = (FileNamed) node;
		return node != null && !f.isDirectory();
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listeners.remove(l);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// do nothing
	}

	public static TreePath getPath(FileNamed file) {
		return new TreePath(file.getFile().getName());
	}

}
