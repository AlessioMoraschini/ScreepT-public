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
package frameutils.frame.arch;

import java.awt.Dialog;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JDialog;
import javax.swing.JFrame;

import dialogutils.GenericFileChooserDialog;
import dialogutils.JOptionHelperExtended;
import initializer.configs.impl.INItializer;
import various.common.light.gui.GuiUtils;
import various.common.light.utility.log.SafeLogger;

public class ParentDialog extends JDialog {
	private static final long serialVersionUID = 7079644907872152737L;
	
	public static final char DEFAULT_PSW_CHAR = '\u25CF';
	
	public static SafeLogger logger = new SafeLogger(ParentDialog.class);
	
	public AtomicBoolean isActive = new AtomicBoolean(true);

	public JOptionHelperExtended dialogHelper;
	public GenericFileChooserDialog fileChooser;
	
	public JFrame parentFrame;
	public Dialog thisFrame;
	public INItializer configuration;
	
	public ParentDialog() {
		this(null, null);
	}

	public ParentDialog(JFrame parentFrame, INItializer configuration) {
		isActive.set(true);
		
		thisFrame = this;
		
		this.configuration = configuration == null ? new INItializer() : configuration;
		this.parentFrame = parentFrame;
		
		this.dialogHelper = new JOptionHelperExtended(this.parentFrame);
		this.fileChooser = new GenericFileChooserDialog(this.configuration);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		setCloseAction();
	}
	
	public void resizeToDefault(boolean minimum, boolean preferred, boolean maximum) {
		if(minimum) {
			setMinimumSize(getDefaultDimension());
		}
		if(preferred) {
			setPreferredSize(getDefaultDimension());
		}
		if(maximum) {
			setMaximumSize(getMaximumDimension());
		}
	}
	
	protected void setCloseAction() {
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		            
	        	logger.debug("Closing Frame: " + getClass().getName());	        	
	        	isActive.set(false);
	        	
	        	dispose();
		    }
		});
	}
	
	public Dimension getDefaultDimension() {
		return GuiUtils.getScreenSizePerc(80);
	}
	
	public Dimension getMinimumDimension() {
		return getDefaultDimension();
	}
	
	public Dimension getMaximumDimension() {
		return getDefaultDimension();
	}
}
