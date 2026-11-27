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
