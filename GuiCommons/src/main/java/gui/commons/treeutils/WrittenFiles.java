/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package gui.commons.treeutils;

import java.io.File;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

/** How viewers and plugins tell the application that they wrote a file, so that the file tree can show it */
public final class WrittenFiles {

	private static volatile Consumer<File> listener;

	private WrittenFiles() {
	}

	/** One listener only (the file tree of the application): a new one replaces the previous; null removes it */
	public static void setListener(Consumer<File> newListener) {
		listener = newListener;
	}

	/** May be called from any thread; the listener runs on the event thread */
	public static void written(final File file) {
		final Consumer<File> current = listener;
		if (current != null && file != null) {
			SwingUtilities.invokeLater(() -> current.accept(file));
		}
	}
}
