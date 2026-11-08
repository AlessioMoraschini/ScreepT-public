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
package gui.commons.tabs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Registry of the viewers and of the extension to viewer associations. Defaults come from the implementors; the user
 * overrides (only the ones that differ from the default) are kept here and saved by the preferences.
 */
public final class TabImplementors {

	/** Id of the standard text editor: it is not an implementor, it is what opens everything else */
	public static final String TEXT_ID = "text";
	public static final String TEXT_NAME = "Text editor";

	private static final List<TabImplementor> implementors = new ArrayList<TabImplementor>();
	private static final Map<String, String> overrides = new LinkedHashMap<String, String>();

	private TabImplementors() {
	}

	public static synchronized void register(TabImplementor implementor) {
		for (int i = 0; i < implementors.size(); i++) {
			if (implementors.get(i).getId().equals(implementor.getId())) {
				implementors.set(i, implementor);
				return;
			}
		}
		implementors.add(implementor);
	}

	public static synchronized List<TabImplementor> all() {
		return Collections.unmodifiableList(new ArrayList<TabImplementor>(implementors));
	}

	public static synchronized TabImplementor byId(String id) {
		for (TabImplementor implementor : implementors) {
			if (implementor.getId().equals(id)) {
				return implementor;
			}
		}
		return null;
	}

	public static String extensionOf(File file) {
		return file == null ? "" : extensionOf(file.getName());
	}

	public static String extensionOf(String name) {
		int dot = name.lastIndexOf('.');
		return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	/** Viewer id by default for an extension: the first registered implementor that declares it, else the text editor */
	public static synchronized String defaultIdFor(String extension) {
		String ext = extension.toLowerCase(Locale.ROOT);
		for (TabImplementor implementor : implementors) {
			if (implementor.getDefaultExtensions().contains(ext)) {
				return implementor.getId();
			}
		}
		return TEXT_ID;
	}

	/** Viewer id in use for an extension: the user choice, or the default */
	public static synchronized String effectiveIdFor(String extension) {
		String chosen = overrides.get(extension.toLowerCase(Locale.ROOT));
		return chosen != null ? chosen : defaultIdFor(extension);
	}

	/** The integrated viewer to open the file with, or null when it goes to the text editor */
	public static synchronized TabImplementor resolve(File file) {
		String id = effectiveIdFor(extensionOf(file));
		if (TEXT_ID.equals(id)) {
			return null;
		}
		TabImplementor implementor = byId(id);
		return implementor != null && implementor.canOpen(file) ? implementor : null;
	}

	/** Sets the viewer for an extension; a choice equal to the default is not kept (the ini holds only the differences) */
	public static synchronized void associate(String extension, String id) {
		String ext = extension.toLowerCase(Locale.ROOT);
		if (id == null || id.equals(defaultIdFor(ext))) {
			overrides.remove(ext);
		} else {
			overrides.put(ext, id);
		}
	}

	/** Copy of the user choices that differ from the defaults */
	public static synchronized Map<String, String> getOverrides() {
		return new LinkedHashMap<String, String>(overrides);
	}

	public static synchronized void setOverrides(Map<String, String> newOverrides) {
		overrides.clear();
		if (newOverrides != null) {
			for (Map.Entry<String, String> entry : newOverrides.entrySet()) {
				associate(entry.getKey(), entry.getValue());
			}
		}
	}

	/** "ext=id;ext=id", the form saved in the ini file */
	public static synchronized String serializeOverrides() {
		StringBuilder out = new StringBuilder();
		for (Map.Entry<String, String> entry : overrides.entrySet()) {
			if (out.length() > 0) {
				out.append(';');
			}
			out.append(entry.getKey()).append('=').append(entry.getValue());
		}
		return out.toString();
	}

	public static synchronized void loadOverrides(String serialized) {
		Map<String, String> loaded = new LinkedHashMap<String, String>();
		if (serialized != null) {
			for (String pair : serialized.split(";")) {
				String[] parts = pair.split("=");
				if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
					loaded.put(parts[0].trim().toLowerCase(Locale.ROOT), parts[1].trim());
				}
			}
		}
		setOverrides(loaded);
	}

	/** Every extension worth listing in the preferences: those of the viewers, common text ones and the user's own */
	public static synchronized List<String> knownExtensions() {
		TreeSet<String> set = new TreeSet<String>();
		for (TabImplementor implementor : implementors) {
			set.addAll(implementor.getDefaultExtensions());
		}
		for (String ext : new String[] {"txt", "log", "md", "json", "xml", "html", "htm", "css", "js", "java", "properties", "ini", "csv", "sql", "yml", "yaml", "sh", "bat"}) {
			set.add(ext);
		}
		set.addAll(overrides.keySet());
		return new ArrayList<String>(set);
	}
}
