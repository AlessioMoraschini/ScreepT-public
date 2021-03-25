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
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import dialogutils.GenericFileChooserDialog;
import dialogutils.JOptionHelperExtended;
import frameutils.frame.arch.ParentFrame;
import frameutils.frame.panels.ShellReadPanel;
import frameutils.frame.panels.ShellReadWritePanel;
import frameutils.frame.panels.arch.ParentPanel;
import frameutils.utils.GuiUtilsExtended;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;

public class ShellFrame extends ParentFrame {
	private static final long serialVersionUID = 7066597918430250412L;
	
	public static ShellFrame singletonInstance = null;

	JFrame thisFrame;
	public ParentPanel payloadPanel;
	public boolean writeMode = false;
	private Runnable closeAction;
	
	public ShellFrame(JFrame parentFrame) {
		this(parentFrame, false);
	}
	
	public ShellFrame(JFrame parentFrame, boolean writeMode) {
		this(parentFrame, writeMode, null);
	}
	
	public ShellFrame(JFrame parentFrame, boolean writeMode, File preloadFile) {
		this(null, parentFrame, writeMode, preloadFile);
	}
	
	public ShellFrame(JTextArea cmdAreaToEnrich, JFrame parentFrame, boolean writeMode, File preloadFile) {

		this.writeMode = writeMode;
		this.closeAction = () -> {};
		
		logger.info(this.getClass().getName() + " - Starting...");
		
		setTitle("Shell Frame - " + GeneralConfig.APPLICATION_NAME);
		
		super.resizeToDefault(true, true, false);

		GuiUtilsExtended.centerComponent(this);
		setVisible(true);
		setAlwaysOnTop(false);
		
		isActive.set(true);
		thisFrame = this;
		
		this.dialogHelper = new JOptionHelperExtended(this);
		this.fileChooser = new GenericFileChooserDialog(null);

		// SW window icon
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_CONSOLE, this);
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		getContentPane().setBackground(Color.DARK_GRAY);
		getContentPane().setLayout(new MigLayout("fill, insets 2", "[100%, grow]", "[100%, grow]"));
		
		if(writeMode) {
			ShellReadWritePanel panel = new ShellReadWritePanel(80000, this, cmdAreaToEnrich);
			if(preloadFile != null && preloadFile.isFile() && preloadFile.exists())
				panel.loadAction(preloadFile).run();
			
			payloadPanel = panel;
			
		} else {
			payloadPanel = new ShellReadPanel(20000);
		}
		getContentPane().add(payloadPanel, "cell 0 0 1 1, grow");
		
		addHandlers();
		logger.info(this.getClass().getName() + " - Started and ready!");
		
		if (payloadPanel instanceof ShellReadWritePanel) {
			SwingUtilities.invokeLater(() -> {
				((ShellReadWritePanel)payloadPanel).commandTextArea.requestFocusInWindow();
			});
		}
	}
	
	public File getLoadedFile() {
		if(payloadPanel instanceof ShellReadWritePanel) {
			return ((ShellReadWritePanel)payloadPanel).loadedFile;
		}
		
		return null;
	}
	
	private void addHandlers() {
		// DEFAULT CLOSE ACTIONS 
		this.addWindowListener(new java.awt.event.WindowAdapter() {
		    
			@Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		            
				if(payloadPanel instanceof ShellReadWritePanel) {
					setVisible(true);
					if(((ShellReadWritePanel)payloadPanel).askSaveIfUnsaved() == null) {
						// CANC pressed
						setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
						return;
					}
					setVisible(false);
				}
				setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				logger.debug("Closing "+getClass().getName());
				closeAction.run();
				isActive.set(false);
				singletonInstance = null;
		    }
		});
	}
	
	public Runnable getCloseAction() {
		return closeAction;
	}

	public void setCloseAction(Runnable closeAction) {
		this.closeAction = closeAction == null ? () -> {} : closeAction;
	}

	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(1100, 600);
	}
	
	@Override
	public Dimension getMinimumDimension() {
		return new Dimension(1000, 400);
	}

	public static ShellFrame getInstance(JFrame parentFrame) {
		if(singletonInstance == null)
			singletonInstance = new ShellFrame(parentFrame);
		
		return singletonInstance;
	}

	public static ShellFrame getInstance(JFrame parentFrame, boolean writeMode) {
		if(singletonInstance == null || singletonInstance.writeMode != writeMode)
			singletonInstance = new ShellFrame(parentFrame, writeMode);

		return  singletonInstance;
	}
	public static ShellFrame getInstance(JFrame parentFrame, boolean writeMode, File preloadFile) {
		if(singletonInstance == null || singletonInstance.writeMode != writeMode)
			singletonInstance = new ShellFrame(parentFrame, writeMode, preloadFile);

		return  singletonInstance;
	}
	public static ShellFrame getInstance(JTextArea cmdTxtAreaToEnrich, JFrame parentFrame, boolean writeMode, File preloadFile) {
		if(singletonInstance == null || singletonInstance.writeMode != writeMode)
			singletonInstance = new ShellFrame(cmdTxtAreaToEnrich, parentFrame, writeMode, preloadFile);
		
		return  singletonInstance;
	}
}
