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
package various.common.light.gui.dialogs.msg;

import java.awt.Component;
import java.awt.HeadlessException;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class JEnhancedOptionPane extends JOptionPane {
	private static final long serialVersionUID = -271340139932360755L;

	/**
	 * Use values selectable into Buttons as options
	 * @param message can be a GUI component or a string
	 * @param options the custom values to use as options
	 * @param option the type of the choices (YES_NO_CANC ...)
	 * @param msgType JOptoinPane.ERROR_MSG or similar
	 * @param icon the icon to use
	 * @param initialValue Eventual initial selection
	 * @return the choice clicked by user, or null if nothing is selected
	 * @throws HeadlessException
	 */
	public static Object showInputDialog(String title, boolean wantsInput, Component parent, final Object message,int option, final Object[] options, final int msgType, Icon icon, Object initialValue)
            throws HeadlessException {
        final JOptionPane pane = new JOptionPane(message, 
        										 msgType,
                                                 option, 
                                                 icon,
                                                 options,
                                                 initialValue);
        pane.setWantsInput(true);
        pane.setComponentOrientation((getRootFrame()).getComponentOrientation());
        pane.setMessageType(msgType);
        
        pane.setWantsInput(wantsInput);
        final JDialog dialog = pane.createDialog(parent, title);
        dialog.setAlwaysOnTop(true);
		try {
			dialog.setVisible(true);
			dialog.dispose();
		} catch (Exception e) {
		}
        
		final Object value = pane.getValue();
        return value;
    }
	
	public static String showInputDialog(final Object message, final Object[] options)
            throws HeadlessException {
        final JOptionPane pane = new JOptionPane(message, QUESTION_MESSAGE,
                                                 OK_CANCEL_OPTION, null,
                                                 options, null);
        pane.setWantsInput(true);
        pane.setComponentOrientation((getRootFrame()).getComponentOrientation());
        pane.setMessageType(QUESTION_MESSAGE);
        pane.selectInitialValue();
        final String title = UIManager.getString("OptionPane.inputDialogTitle", null);
        final JDialog dialog = pane.createDialog(null, title);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        dialog.dispose();
        final Object value = pane.getInputValue();
        return (value == UNINITIALIZED_VALUE) ? null : (String) value;
    }
}
