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
import java.util.Set;

/**
 * A viewer that can live inside a tab of the editor (like the editors of an IDE): the image studio, LogSearch, the
 * HTML viewer... The editor asks the {@link TabImplementors} registry which implementor opens a file, then calls
 * {@link #open} and puts the returned {@link TabContent} in a new tab.
 */
public interface TabImplementor {

	/** Stable identifier saved in the ini file for the user associations (lowercase, no spaces) */
	String getId();

	/** Name shown to the user (preferences, menus) */
	String getDisplayName();

	/** Extensions (lowercase, no dot) this viewer handles by default */
	Set<String> getDefaultExtensions();

	/**
	 * Extensions worth listing in the preferences for this viewer (it can open them, without being their default).
	 * By default the same as {@link #getDefaultExtensions()}.
	 */
	default Set<String> getSupportedExtensions() {
		return getDefaultExtensions();
	}

	/** True if this viewer can really open the file (exists, readable, supported...) */
	boolean canOpen(File file);

	/** Creates the content of a new tab for the file. Called on the Swing thread. */
	TabContent open(File file) throws Exception;
}
