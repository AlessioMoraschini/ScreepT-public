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

import java.io.File;

/**
 * Optional capability of a plugin: it can also work as a <b>panel</b>, shown inside a tab of the file editor, besides its own window.
 * A plugin that does not implement it keeps working as always (its window only): the capability is added, nothing changes for old plugins.
 * <p>
 * The application discovers the implementations at runtime and offers them in Graphic preferences > File editor > Viewers, where the
 * user can choose the plugin as the default viewer of an extension, and in the "Open with" menu of the file tree.
 * Use {@link FramePanelAdapter} to reuse the content of the window of the plugin as panel.
 * <p>
 * <b>A plugin using this interface needs ScreepT 0.9.87 or newer.</b>
 */
public interface IPluginPanelMode extends IPlugin {

	/** Extensions (lowercase, without dot) this panel is meant for: they are listed in the viewer preferences. Not a default association. */
	default String[] getPanelExtensions() {
		return new String[0];
	}

	/** Name shown to the user in the preferences and in the "Open with" menu */
	default String getPanelName() {
		return getPluginName();
	}

	/** True if the panel can really open the file (exists, supported...). Called before offering the panel for a file. */
	default boolean canOpenInPanel(File file) {
		return file != null && file.isFile();
	}

	/**
	 * Creates the content of a tab for the file. Called on the Swing thread, once per tab: the returned panel owns its own state.
	 * @param file the file to show (may be null for an empty panel)
	 */
	PluginPanel createPanel(File file) throws Exception;
}
