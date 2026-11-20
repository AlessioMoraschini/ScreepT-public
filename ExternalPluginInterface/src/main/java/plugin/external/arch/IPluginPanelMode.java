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
