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
package multimedia.conversion.gui;

import java.awt.Component;
import java.io.File;

import javax.swing.JComponent;

/**
 * The editing of one PDF, seen by the viewer. No type of the PDF editing library appears here: the viewer works also with an
 * old PDF plugin that does not ship it, as long as {@link #isAvailable()} is checked before creating the editor.
 */
public interface PdfEditing {

	static boolean isAvailable() {
		try {
			Class.forName("org.apache.pdfbox.pdmodel.PDDocument", false, PdfEditing.class.getClassLoader());
			Class.forName("org.apache.fontbox.ttf.TTFParser", false, PdfEditing.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}

	JComponent getComponent();

	/** The file showing the document with the changes made so far */
	File getCurrentVersion();

	boolean isModified();

	/** Saves over the file; false if cancelled or failed */
	boolean save();

	/** Saves in another file, which becomes the edited one; false if cancelled or failed */
	boolean saveAs();

	/** The file written by {@link #save()} */
	File getTarget();

	void undo();

	void redo();

	/** Shows the page (0 based) */
	void showPage(int page);

	/** Asks which pages and writes them in a new file */
	void extractPages(Component parent, int currentPage);

	/** Closes the document and deletes the temporary files */
	void release();
}
