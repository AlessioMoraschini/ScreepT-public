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
package textarea;

import javax.swing.JTextArea;

public interface ITextAreaDialogs {

	public JTextArea getTargetTextArea();
	public void loadThemeIntoTarget(JTextArea txtArea);
	public String getCurrentClipboard();
}
