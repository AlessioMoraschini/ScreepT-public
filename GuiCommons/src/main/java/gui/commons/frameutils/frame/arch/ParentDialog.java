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
package gui.commons.frameutils.frame.arch;

import java.awt.Dialog;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JDialog;
import javax.swing.JFrame;

import gui.commons.dialogutils.GenericFileChooserDialog;
import gui.commons.dialogutils.JOptionHelperExtended;
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
