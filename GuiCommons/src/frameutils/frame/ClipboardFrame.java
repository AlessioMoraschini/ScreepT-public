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
package frameutils.frame;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;

import dialogutils.GenericFileChooserDialog;
import dialogutils.JOptionHelperExtended;
import frameutils.frame.arch.ParentFrame;
import frameutils.frame.panels.ClipboardPanelLight;
import frameutils.utils.GuiUtilsExtended;
import impl.om.TextEditorOption;
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
		logger.info(this.getClass().getName() + " - Starting...");
		
		setTitle("Clipboard Frame - " + GeneralConfig.APPLICATION_NAME);
		
		this.setMinimumSize(getDefaultDimension());

		GuiUtilsExtended.centerComponent(this);
		setVisible(true);
		setAlwaysOnTop(true);
		
		isActive.set(true);
		thisFrame = this;
		
		this.dialogHelper = new JOptionHelperExtended(this);
		this.fileChooser = new GenericFileChooserDialog(null);

		// SW window icon
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_CLIPBOARD, this);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		getContentPane().setBackground(Color.DARK_GRAY);
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
