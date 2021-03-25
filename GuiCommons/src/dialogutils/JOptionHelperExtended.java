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
package dialogutils;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import gui.dialogs.msg.JOptionHelper;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;

public class JOptionHelperExtended extends JOptionHelper {
	
	public static void init() {
		ICON_ERR_REF = new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_ERROR);    
		ICON_WARN_REF = new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_WARNING); 
		ICON_INFO_REF = new ImageIcon(IconsPathConfigurator.ICON_GEN_INFO);        
		ICON_QUESTION_REF = new ImageIcon(IconsPathConfigurator.ICON_GEN_QUESTION);
		DEFAULT_FONT = GeneralConfig.DEFAULT_DIALOGS_FONT;
	}
	
	public JOptionHelperExtended (Component parentComponent) {
		super(parentComponent);
		this.enabledAdvices = true;
	}
	
	public JOptionHelperExtended (Component parentComponent, boolean enableAdvices) {
		this(parentComponent);
		this.enabledAdvices = enableAdvices;
	}
	
	// PRIVATE METHODS
	@Override
	protected Boolean showYNMessageCommon(Object msg, String title, int msgType, boolean allowNull, boolean warningType, boolean cancEnabled) {
		
		Icon icon = warningType? new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_WARNING) : new ImageIcon(IconsPathConfigurator.ICON_BIG_GEN_QUESTION);
		int options = cancEnabled ? YES_OR_NO_CANC : YES_OR_NO;
		Object[] optionsAvailable = cancEnabled ? defaultOptionsExtended : defaultOptions;
		
		final JOptionPane optionPane = new JOptionPane(
				msg instanceof String ? getLabelStyledText((String)msg) : msg, 
        		msgType,
        		options, 
        		icon,
        		optionsAvailable,
        		optionsAvailable[0]);
        
		JDialog dialog = optionPane.createDialog(title);
		dialog.setAlwaysOnTop(true);
		dialog.setLocationRelativeTo(parentComponent);
		dialog.setIconImage(new ImageIcon(IconsPathConfigurator.ICON_GEN_QUESTION).getImage());
		dialog.enableInputMethods(true);
		dialog.toFront();
		
		try {
			dialog.setVisible(true);
			dialog.dispose();
		} catch (Exception e) {
		}
		
		final Object value = optionPane.getValue();
		if (value != null) {
			return (cancEnabled && value.equals(defaultOptionsExtended[2])) ? null : (value.equals(optionsAvailable[0]));
		} else {
			return (allowNull) ? null : false;
		}
	}
	
	// Used internally to create other various dialogs without user input
	@Override
	protected void showMessageCommon(Object msg, String title, int msgType, Object[] choiceOptions, Object defaultOption) {
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
					msg instanceof String ? getLabelStyledText((String) msg) : msg, 
					msgType,
					OK,
					iconBig,
					choiceOptions,
					defaultOption);
			
			JDialog dialog = optionPane.createDialog(title);
			dialog.setAlwaysOnTop(true);
			dialog.setLocationRelativeTo(parentComponent);
			dialog.setIconImage(icon.getImage());
			dialog.toFront();
			try {
				dialog.setVisible(true);
				dialog.dispose();
			} catch (Exception e) {
			} 
		}
	}
	
}
