/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
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
package various.common.light.gui;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.swing.ImageIcon;

/**
 * Safety net for the icon of the windows: every frame and dialog that opens without an icon of its own (and whose owner has none to
 * lend) gets the icon registered for its class, or else the default one (the icon of the application). Windows that set their own
 * icon are not touched. Install it once at start: {@code WindowIcons.install(() -> pathOfTheAppIcon)}.
 */
public final class WindowIcons {

	private static final Map<String, Supplier<String>> BY_CLASS = new ConcurrentHashMap<String, Supplier<String>>();
	private static final Map<String, Image> CACHE = new ConcurrentHashMap<String, Image>();
	private static volatile Supplier<String> defaultIcon;
	private static boolean installed;

	private WindowIcons() {
	}

	/** Starts watching the windows; {@code defaultIconPath} gives the path of the icon used when no class icon is registered */
	public static synchronized void install(Supplier<String> defaultIconPath) {
		defaultIcon = defaultIconPath;
		if (installed) {
			return;
		}
		installed = true;
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof Window) {
				apply((Window) event.getSource());
			}
		}, AWTEvent.WINDOW_EVENT_MASK);
	}

	/** Icon for the windows of a class (simple class name), instead of the default one */
	public static void register(String simpleClassName, Supplier<String> iconPath) {
		BY_CLASS.put(simpleClassName, iconPath);
	}

	private static void apply(Window window) {
		if (!(window instanceof Frame) && !(window instanceof Dialog)) {
			return;
		}
		if (!window.getIconImages().isEmpty()) {
			return; // its own icon, or the one of its owner
		}
		Supplier<String> path = BY_CLASS.get(window.getClass().getSimpleName());
		Image image = load(path != null ? path : defaultIcon);
		if (image == null && path != null) {
			image = load(defaultIcon);
		}
		if (image != null) {
			window.setIconImages(Collections.singletonList(image));
		}
	}

	private static Image load(Supplier<String> supplier) {
		try {
			String path = supplier == null ? null : supplier.get();
			if (path == null || !new File(path).isFile()) {
				return null;
			}
			Image cached = CACHE.get(path);
			if (cached == null) {
				cached = new ImageIcon(path).getImage();
				CACHE.put(path, cached);
			}
			return cached;
		} catch (RuntimeException e) {
			return null; // an icon that cannot be loaded is not worth an error
		}
	}
}
