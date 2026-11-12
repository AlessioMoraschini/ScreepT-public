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
package gui.commons.imagestudio;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * How much of the Image Studio is shown. One choice for the whole application, remembered between runs: every open
 * studio (window tabs and editor tabs) follows it.
 */
public enum StudioMode {
	BASIC("Basic", "Convert and resize"),
	CLASSIC("Classic", "+ adjustments, filters, rotate, flip, copy"),
	NERD("Nerd", "+ every option: histogram, gamma, hue, resampling, grid");

	private static final String PREF_KEY = "imageStudioMode2";
	private static final List<Runnable> listeners = new ArrayList<Runnable>();
	private static StudioMode current = load();

	public final String label;
	public final String description;

	StudioMode(String label, String description) {
		this.label = label;
		this.description = description;
	}

	@Override
	public String toString() {
		return label;
	}

	public boolean atLeast(StudioMode minimum) {
		return ordinal() >= minimum.ordinal();
	}

	public static synchronized StudioMode current() {
		return current;
	}

	public static void set(StudioMode mode) {
		List<Runnable> toNotify;
		synchronized (StudioMode.class) {
			if (mode == null || mode == current) {
				return;
			}
			current = mode;
			try {
				Preferences.userNodeForPackage(StudioMode.class).put(PREF_KEY, mode.name());
			} catch (RuntimeException e) {
				// not remembered: still applied for this run
			}
			toNotify = new ArrayList<Runnable>(listeners);
		}
		for (Runnable listener : toNotify) {
			listener.run();
		}
	}

	public static synchronized void addListener(Runnable listener) {
		listeners.add(listener);
	}

	public static synchronized void removeListener(Runnable listener) {
		listeners.remove(listener);
	}

	private static StudioMode load() {
		try {
			return valueOf(Preferences.userNodeForPackage(StudioMode.class).get(PREF_KEY, NERD.name()));
		} catch (RuntimeException e) {
			return NERD;
		}
	}
}
