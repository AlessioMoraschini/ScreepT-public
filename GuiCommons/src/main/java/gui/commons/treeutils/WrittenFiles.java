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
