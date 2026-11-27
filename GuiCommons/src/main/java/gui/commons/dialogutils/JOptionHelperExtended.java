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
package gui.commons.dialogutils;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.dialogs.msg.JOptionHelper;

public class JOptionHelperExtended extends JOptionHelper {

	public static void init() {
		ICON_ERR_REF = new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_ERROR);
		ICON_WARN_REF = new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_WARNING);
		ICON_INFO_REF = new ImageIcon(IconsPathConfigurator.ICON_GEN_INFO);
		ICON_QUESTION_REF = new ImageIcon(IconsPathConfigurator.ICON_GEN_QUESTION);
		DEFAULT_FONT = GeneralConfig.DEFAULT_DIALOGS_FONT;
	}

	public JOptionHelperExtended(Component parentComponent) {
		super(parentComponent);
	}

	public JOptionHelperExtended(Component parentComponent, boolean enableAdvices) {
		this(parentComponent);
		this.enabledAdvices = enableAdvices;
	}

	// PRIVATE METHODS
	@Override
	public Boolean showYNMessageCommon(Object msg, String title, int msgType, boolean allowNull, boolean warningType,
			boolean cancEnabled) {
		Icon icon = warningType ? ICON_WARN_REF : ICON_QUESTION_REF;
		int option = cancEnabled ? YES_OR_NO_CANC : YES_OR_NO;
		Object[] optionsAvailable = cancEnabled ? defaultOptionsExtended : defaultOptions;

		final JOptionPane optionPane = new JOptionPane(msg instanceof String ? getLabelStyledText((String) msg) : msg,
				msgType, option, icon, optionsAvailable,
				allowNull ? optionsAvailable[optionsAvailable.length - 1] : optionsAvailable[0]);

		// *** MONITOR CORRETTO ***
		GraphicsConfiguration gc = getCurrentMonitor(getDialogParent());

		final JDialog dialog = new JDialog(getDialogWindow(), title,
				Dialog.ModalityType.APPLICATION_MODAL, gc);

		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setContentPane(optionPane);
		dialog.pack();

		// Chiudi quando cambia il valore
		optionPane.addPropertyChangeListener(evt -> {
			String prop = evt.getPropertyName();
			if (dialog.isVisible() && evt.getSource() == optionPane
					&& (prop.equals(JOptionPane.VALUE_PROPERTY) || prop.equals(JOptionPane.INPUT_VALUE_PROPERTY))) {
				dialog.setVisible(false);
			}
		});

		// Centra sul monitor corretto
		Rectangle bounds = gc.getBounds();
		dialog.setLocation(bounds.x + (bounds.width - dialog.getWidth()) / 2,
				bounds.y + (bounds.height - dialog.getHeight()) / 2);

		dialog.setAlwaysOnTop(true);
		dialog.toFront();
		dialog.setVisible(true);
		dialog.dispose();

		final Object value = optionPane.getValue();
		// window closed without a choice (X or Esc): the value is UNINITIALIZED or CLOSED_OPTION, it is not an answer
		if (value != null && value != JOptionPane.UNINITIALIZED_VALUE && !Integer.valueOf(JOptionPane.CLOSED_OPTION).equals(value)) {
			return (cancEnabled && value.equals(defaultOptionsExtended[2])) ? null
					: (value.equals(optionsAvailable[0]));
		} else {
			return allowNull ? null : false;
		}
	}

	// Used internally to create other various dialogs without user input
	@Override
	protected void showMessageCommon(Object msg, String title, int msgType, Object[] choiceOptions,
			Object defaultOption) {
		if (enabledAdvices) {
			ImageIcon icon = new ImageIcon();
			ImageIcon iconBig = new ImageIcon();
			if (WARN == msgType) {
				icon = new ImageIcon(IconsPathConfigurator.ICON_GEN_WARNING);
				iconBig = new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_WARNING);
			} else if (ERR == msgType) {
				icon = new ImageIcon(IconsPathConfigurator.ICON_GEN_ERROR);
				iconBig = new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_ERROR);
			} else if (QUESTION == msgType) {
				icon = new ImageIcon(IconsPathConfigurator.ICON_GEN_QUESTION);
				iconBig = new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_QUESTION);
			} else {
				icon = new ImageIcon(IconsPathConfigurator.ICON_GEN_INFO);
				iconBig = new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_INFO);
			}

			final JOptionPane optionPane = new JOptionPane(
					msg instanceof String ? getLabelStyledText((String) msg) : msg, msgType, OK, iconBig, choiceOptions,
					defaultOption);
			
			// *** MONITOR CORRETTO ***
			GraphicsConfiguration gc = getCurrentMonitor(getDialogParent());

			final JDialog dialog = new JDialog(getDialogWindow(), title,
					Dialog.ModalityType.APPLICATION_MODAL, gc);

			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setContentPane(optionPane);
			dialog.pack();
			dialog.setIconImage(icon.getImage());

			// Chiudi quando cambia il valore
			optionPane.addPropertyChangeListener(evt -> {
				String prop = evt.getPropertyName();
				if (dialog.isVisible() && evt.getSource() == optionPane
						&& (prop.equals(JOptionPane.VALUE_PROPERTY) || prop.equals(JOptionPane.INPUT_VALUE_PROPERTY))) {
					dialog.setVisible(false);
				}
			});
			
			// Centra sul monitor corretto
			Rectangle bounds = gc.getBounds();
			dialog.setLocation(bounds.x + (bounds.width - dialog.getWidth()) / 2,
					bounds.y + (bounds.height - dialog.getHeight()) / 2);

			dialog.setAlwaysOnTop(true);
			dialog.toFront();
			dialog.setVisible(true);
			dialog.dispose();
		}
	}

}
