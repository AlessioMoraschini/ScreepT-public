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
package gui.commons.dialogutils;

import java.awt.Component;
import java.awt.HeadlessException;

import javax.swing.JFileChooser;

import org.apache.log4j.Logger;

/**
 * Drop-in replacement of {@link JFileChooser}: on Windows the show*Dialog methods open the native
 * Explorer dialog (editable address bar, search box, copy/paste, favourites), elsewhere or on any
 * native failure the classic Swing dialog is used.
 * Configure it and read results exactly as a JFileChooser (title, current directory, selected file,
 * selection mode, multi selection, file filters).
 */
public class NativeFileChooser extends JFileChooser {

	private static final long serialVersionUID = 4630257781943362471L;

	static Logger logger = Logger.getLogger(NativeFileChooser.class);

	/** System property: set to "false" to always use the Swing chooser (automatic tests) */
	public static final String NATIVE_DIALOG_PROPERTY = "screept.nativeFileDialog";

	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");

	@Override
	public int showDialog(Component parent, String approveButtonText) throws HeadlessException {

		if (approveButtonText != null) {
			setApproveButtonText(approveButtonText);
			setDialogType(CUSTOM_DIALOG);
		}

		// tests set this to false: a native dialog cannot be answered by a test and would wait for a person
		if (WINDOWS && !"false".equals(System.getProperty(NATIVE_DIALOG_PROPERTY))) {
			try {
				return WindowsNativeFileDialog.show(this, parent);
			} catch (WindowsNativeFileDialog.UnavailableException | LinkageError e) {
				logger.warn("Native file dialog not available, using Swing file chooser", e);
			}
		}

		return super.showDialog(parent, approveButtonText);
	}
}
