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

import java.awt.Component;
import java.io.File;

import javax.swing.JComponent;

/** Content of an integrated viewer tab, created by a {@link TabImplementor} */
public interface TabContent {

	/** Component shown in the tab */
	JComponent getComponent();

	/** Text of the tab */
	String getTitle();

	/** File shown (null for something without a file, like a pasted image) */
	File getFile();

	/** True if there is something not saved yet */
	boolean isModified();

	/** Saves in place. Returns false if cancelled or failed. */
	boolean save();

	/** Called before closing the tab: asks the user about unsaved work. Returns false to keep the tab open. */
	boolean confirmClose(Component parent);

	/** The tab has been selected: take the focus */
	void onSelected();

	/** The tab is closed: release the resources */
	void dispose();

	/** Notifies the host when title or modified state change (the host refreshes the tab header) */
	void setChangeListener(Runnable listener);

	/**
	 * True if the tab works like the text editor: search, replace, base64, undo, font and the other text functions of the
	 * toolbar apply to it. A viewer that does not use them leaves false: the toolbar disables them while its tab is selected.
	 */
	default boolean supportsTextTools() {
		return false;
	}

	/** True if the tab can be shown full screen (PDF, Markdown...): the toolbar button and F11 work only for these */
	default boolean supportsFullscreen() {
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

	/** Gives the content a way to ask the host to close its tab (for contents that hand the work over to a window) */
	default void setCloseHandler(Runnable closeTab) {
	}
}
