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
package gui.commons.frameutils.frame.arch;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import gui.commons.dialogutils.ExtensionFileFilter;
import gui.commons.dialogutils.GenericFileChooserDialog;
import gui.commons.dialogutils.JOptionHelperExtended;
import initializer.configs.impl.INItializer;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.GuiUtils;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.primitive.DynaBoolean;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.string.StringWorker.EOL;

public class ParentFrame extends JFrame{
	private static final long serialVersionUID = 7079644907872152737L;

	public static final char DEFAULT_PSW_CHAR = '\u25CF';

	public static SafeLogger logger = new SafeLogger(ParentFrame.class);

	public AtomicBoolean isActive = new AtomicBoolean(true);

	public JOptionHelperExtended dialogHelper;
	public GenericFileChooserDialog fileChooser;

	public int defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE;

	public DynaBoolean closeAction;

	public JFrame parentFrame;
	public JFrame thisFrame;
	public INItializer configuration;

	protected boolean alreadySaving;

	public ParentFrame() {
		this(null, null);
	}

	public ParentFrame(JFrame parentFrame, INItializer configuration) {
		this(parentFrame, configuration, true);
	}

	public ParentFrame(JFrame parentFrame, INItializer configuration, boolean addEscAutoClose) {

		setCloseAction(null);

		isActive.set(true);

		thisFrame = this;

		this.configuration = configuration == null ? new INItializer() : configuration;
		this.parentFrame = parentFrame;

		this.dialogHelper = new JOptionHelperExtended(this.parentFrame);
		this.fileChooser = new GenericFileChooserDialog(this.configuration);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		setCloseAction();

//		if(addEscAutoClose)
//			GuiUtils.addEscAdapter(this);
	}

	public DynaBoolean getCloseAction() {
		return closeAction;
	}

	public void setCloseAction(DynaBoolean closeAction) {
		this.closeAction = closeAction == null ? ()->{return true;} : closeAction;
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

	        	if(!closeAction.isTrue()) {
	        		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	        		return;
	        	} else {
	        		setDefaultCloseOperation(defaultCloseOperation);
	        	}

	        	dispose();
		    }
		});
	}

	public Dimension getDefaultDimension() {
		return GuiUtils.getScreenSizePerc(80, this);
	}

	public Dimension getMinimumDimension() {
		return getDefaultDimension();
	}

	public Dimension getMaximumDimension() {
		return getDefaultDimension();
	}


	public File saveCurrentFileAs(File current, JTextArea source, EOL lineSeparator, String extension) {

		File selectedFile = null;

		if (!alreadySaving) {

			alreadySaving = true;

			// update signal value
			boolean done = false;
			String exportNamePreset = (current != null && !current.getName().equals(""))?
					current.getName() : "NewFile" + extension;
			exportNamePreset = FileVarious.ensureValidExtension(exportNamePreset, extension);

			try {
				File currentParent = FileVarious.getParent(current);
				selectedFile = (current == null || !current.exists() || !current.isFile())
						? fileChooser.fileWrite(this, exportNamePreset, currentParent, new ExtensionFileFilter[] {new ExtensionFileFilter(new String[] {extension}, extension)})
						: current;

				if (selectedFile != null) {

					String outString = StringWorker.normalizeStringToEol(source.getText(), lineSeparator == null ? EOL.defaultEol : lineSeparator);

					if (selectedFile.exists()) {
						if (dialogHelper.yesOrNo("File " + selectedFile.getName()
								+ " exists, overwrite it?", "Overwrite?")) {
							done = FileWorker.writeStringToFile(outString, selectedFile, true);
						}
					} else {
						done = selectedFile.createNewFile();
						PrintWriter out = new PrintWriter(selectedFile.getAbsolutePath());
						out.write(outString);
						out.close();
					}
					if (done) {
						String msgOk = "File " + selectedFile.getName() + " succesfully saved!";
						dialogHelper.info(msgOk, "File Saved!");

						if (current == null) {
							current = selectedFile;
						}
						fileChooser.currentConf.getFileOpt().setLastDstFolderPath(FileVarious.getParentPath(current));
					} else {

					}
				}

			} catch (IOException e1) {
				dialogHelper.error("error Saving the selected file :(");
				logger.error("Exception happened!", e1);

			} finally {
				alreadySaving = false;
			}
		}

		return selectedFile;
	}
}
