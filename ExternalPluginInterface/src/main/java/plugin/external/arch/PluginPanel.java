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
package plugin.external.arch;

import java.awt.Component;
import java.io.File;

import javax.swing.JComponent;

/** Content of a tab hosting a plugin (see {@link IPluginPanelMode}). Only Swing and the JDK: no dependency on the application. */
public interface PluginPanel {

	/** Component shown in the tab */
	JComponent getComponent();

	/** Text of the tab */
	String getTitle();

	/** File shown (null for something without a file) */
	File getFile();

	/** True if there is something not saved yet */
	boolean isModified();

	/** Saves in place. Returns false if cancelled or failed. */
	boolean save();

	/** Called before closing the tab: asks the user about unsaved work. Returns false to keep the tab open. */
	boolean confirmClose(Component parent);

	/** The tab has been selected: take the focus */
	default void onSelected() {
	}

	/** The tab is closed: release the resources (timers, global listeners, windows) */
	void dispose();

	/** The host gives a callback to call when the title or the modified state change (it refreshes the tab header) */
	default void setChangeListener(Runnable listener) {
	}

	/** The host gives a way to close its tab, for panels that hand the work over to a window */
	default void setCloseHandler(Runnable closeTab) {
	}

	/**
	 * True if the search, replace, Base64, undo, font... functions of the file editor toolbar apply to this panel.
	 * Almost always false: the plugin has its own tools, and the toolbar functions are disabled while the panel is selected.
	 */
	default boolean supportsTextTools() {
		return false;
	}

	/** Saves in another file chosen by the user, if the content can; by default it saves in place */
	default boolean saveAs() {
		return save();
	}

	/** True if the content has its own history: undo and redo of the toolbar and of the keyboard apply to it */
	default boolean supportsEditHistory() {
		return false;
	}

	default void undo() {
	}

	default void redo() {
	}
}
