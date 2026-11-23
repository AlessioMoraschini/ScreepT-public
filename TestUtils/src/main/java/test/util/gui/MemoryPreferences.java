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
package test.util.gui;

import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** Preferences kept in memory: a test never reads or writes the registry (or the files) of the person running it */
public final class MemoryPreferences implements PreferencesFactory {

	public static final String FACTORY_PROPERTY = "java.util.prefs.PreferencesFactory";

	private final Preferences user = new Node(null, "");
	private final Preferences system = new Node(null, "");

	/** Must run before the first use of {@link Preferences} in the JVM */
	public static void install() {
		System.setProperty(FACTORY_PROPERTY, MemoryPreferences.class.getName());
	}

	public static boolean installed() {
		return MemoryPreferences.class.getName().equals(System.getProperty(FACTORY_PROPERTY));
	}

	@Override
	public Preferences userRoot() {
		return user;
	}

	@Override
	public Preferences systemRoot() {
		return system;
	}

	private static final class Node extends AbstractPreferences {
		private final Map<String, String> values = new HashMap<String, String>();
		private final Map<String, Node> children = new TreeMap<String, Node>();

		Node(Node parent, String name) {
			super(parent, name);
		}

		@Override
		protected void putSpi(String key, String value) {
			values.put(key, value);
		}

		@Override
		protected String getSpi(String key) {
			return values.get(key);
		}

		@Override
		protected void removeSpi(String key) {
			values.remove(key);
		}

		@Override
		protected void removeNodeSpi() throws BackingStoreException {
			values.clear();
		}

		@Override
		protected String[] keysSpi() throws BackingStoreException {
			return values.keySet().toArray(new String[0]);
		}

		@Override
		protected String[] childrenNamesSpi() throws BackingStoreException {
			return children.keySet().toArray(new String[0]);
		}

		@Override
		protected AbstractPreferences childSpi(String name) {
			Node n = new Node(this, name);
			children.put(name, n);
			return n;
		}

		@Override
		protected void syncSpi() throws BackingStoreException {
			// memory only
		}

		@Override
		protected void flushSpi() throws BackingStoreException {
			// memory only
		}
	}
}
