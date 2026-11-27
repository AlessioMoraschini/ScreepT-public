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
package gui.commons.tabs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	public static final String TEXT_NAME = "File editor";

	private static final List<TabImplementor> implementors = new ArrayList<TabImplementor>();
	private static final Map<String, String> overrides = new LinkedHashMap<String, String>();

	private TabImplementors() {
	}

	/** Id prefix of the viewers that come from plugins: they appear at runtime, so an unknown id with this prefix asks the provider */
	public static final String PLUGIN_ID_PREFIX = "plugin:";
	/** Registers the viewers that appear at runtime (plugins). The argument says if the search must be redone (a plugin was installed) or the cached one can be used */
	private static java.util.function.Consumer<Boolean> dynamicProvider;
	private static long lastDynamicRefresh;
	private static boolean refreshingDynamic;

	public static synchronized void setDynamicProvider(java.util.function.Consumer<Boolean> provider) {
		dynamicProvider = provider;
		lastDynamicRefresh = 0;
	}

	/** Asks the provider for the viewers available now (at most every 5 seconds, unless forced) */
	public static synchronized void refreshDynamic(boolean force) {
		if (dynamicProvider == null || refreshingDynamic || (!force && System.currentTimeMillis() - lastDynamicRefresh < 5000)) {
			return;
		}
		refreshingDynamic = true;
		try {
			dynamicProvider.accept(force);
		} catch (RuntimeException e) {
			// a failing plugin search must not break the editor: the viewers already registered stay
		} finally {
			refreshingDynamic = false;
			lastDynamicRefresh = System.currentTimeMillis();
		}
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
		refreshDynamic(false);
		return Collections.unmodifiableList(new ArrayList<TabImplementor>(implementors));
	}

	public static synchronized TabImplementor byId(String id) {
		for (int attempt = 0; attempt < 2; attempt++) {
			for (TabImplementor implementor : implementors) {
				if (implementor.getId().equals(id)) {
					return implementor;
				}
			}
			if (id == null || !id.startsWith(PLUGIN_ID_PREFIX)) {
				break;
			}
			refreshDynamic(false); // a plugin viewer chosen by the user, asked before the plugins were searched
		}
		return null;
	}

	/**
	 * True when the file holds text: no binary bytes in its first 8 KB (UTF-16 with byte order mark counts as text).
	 * Empty files are text. Used to offer the text editor and the text viewers only for files they can show.
	 */
	public static boolean isTextFile(File file) {
		if (file == null || !file.isFile()) {
			return false;
		}
		try (InputStream in = new FileInputStream(file)) {
			byte[] buffer = new byte[8192];
			int read = in.read(buffer);
			if (read >= 2 && ((buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xFE) || (buffer[0] == (byte) 0xFE && buffer[1] == (byte) 0xFF))) {
				return true;
			}
			for (int i = 0; i < read; i++) {
				if (buffer[i] == 0) {
					return false;
				}
			}
			return true;
		} catch (IOException e) {
			return false;
		}
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
		refreshDynamic(false);
		TreeSet<String> set = new TreeSet<String>();
		for (TabImplementor implementor : implementors) {
			set.addAll(implementor.getSupportedExtensions());
		}
		for (String ext : new String[] {"txt", "log", "md", "json", "xml", "html", "htm", "css", "js", "java", "properties", "ini", "csv", "sql", "yml", "yaml", "sh", "bat"}) {
			set.add(ext);
		}
		set.addAll(overrides.keySet());
		return new ArrayList<String>(set);
	}
}
