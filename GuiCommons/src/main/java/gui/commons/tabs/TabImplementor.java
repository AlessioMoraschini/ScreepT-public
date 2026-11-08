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

	/** True if this viewer can really open the file (exists, readable, supported...) */
	boolean canOpen(File file);

	/** Creates the content of a new tab for the file. Called on the Swing thread. */
	TabContent open(File file) throws Exception;
}
