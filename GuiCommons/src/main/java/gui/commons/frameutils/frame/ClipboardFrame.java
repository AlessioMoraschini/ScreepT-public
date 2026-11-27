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
package gui.commons.frameutils.frame;

import java.awt.Dimension;

import javax.swing.JFrame;

import gui.commons.dialogutils.GenericFileChooserDialog;
import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.commons.frameutils.frame.panels.ClipboardPanelLight;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import initializer.configs.impl.om.TextEditorOption;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;

public class ClipboardFrame extends ParentFrame{
	private static final long serialVersionUID = 7066597918430250412L;
	
	public static ClipboardFrame singletonInstance = null;

	JFrame thisFrame;
	
	ClipboardPanelLight	payloadPanel;
	
	public ClipboardFrame(JFrame parentFrame) {
		this(parentFrame, new TextEditorOption());
	}
	
	public ClipboardFrame(JFrame parentFrame, TextEditorOption options) {
		super(parentFrame, null);
		logger.info(this.getClass().getName() + " - Starting...");
		
		setTitle("Clipboard Frame - " + GeneralConfig.APPLICATION_NAME);
		
		this.setMinimumSize(getDefaultDimension());

		GuiUtilsExtended.centerComponent(this);
		setVisible(true);
		setAlwaysOnTop(true);
		
		isActive.set(true);
		thisFrame = this;
		
		this.fileChooser = new GenericFileChooserDialog(null);

		// SW window icon
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_CLIPBOARD, this);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		

		getContentPane().setLayout(new MigLayout("fill, insets 2", "[100%, grow]", "[100%, grow]"));
		
		payloadPanel = new ClipboardPanelLight(options);
		getContentPane().add(payloadPanel, "cell 0 0 1 1, grow");
		
		addHandlers();
		logger.info(this.getClass().getName() + " - Started and ready!");
	}
	
	private void addHandlers() {
		// DEFAULT CLOSE ACTIONS 
		this.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		            
	        	payloadPanel.flush();
	        	isActive.set(false);
	        	singletonInstance = null;
				logger.debug("Closing "+getClass().getName());
		    }
		});
	}
	
	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(900, 600);
	}
	
	public static ClipboardFrame getInstance(JFrame parentFrame) {
		return singletonInstance == null ? new ClipboardFrame(parentFrame) : singletonInstance;
	}

	public static ClipboardFrame getInstance(JFrame parentFrame, TextEditorOption options) {
		return singletonInstance == null ? new ClipboardFrame(parentFrame, options) : singletonInstance;
	}
}
