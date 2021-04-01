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
package gui.commons.frameutils.frame.panels.arch;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import gui.commons.dialogutils.GenericFileChooserDialog;
import gui.commons.dialogutils.JOptionHelperExtended;
import gui.commons.textarea.om.SelectiveLineEnabledTextArea;
import resources.IconsPathConfigurator;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.ScreepTGuiFactory;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.reflection.ReflectionUtils;

public abstract class ParentPanel extends JPanel implements IParentPanel{
	private static final long serialVersionUID = -5704690014209186123L;

	// ParentPanel logger creation
	public static SafeLogger logger = new SafeLogger(ParentPanel.class);

	protected JOptionHelperExtended dialogHelper;
	public GenericFileChooserDialog fileChooser;

	public TreeMap<String, File> dataMap;

	public ParentPanel() {
//		init();
	}

	protected final void init() {
		dataMap = new TreeMap<>();
		dialogHelper = new JOptionHelperExtended(this);
		fileChooser = new GenericFileChooserDialog(null);
		initGui();
		addHandlers();
		addKeyListeners();
	}

	public void setFileChooser(GenericFileChooserDialog fileChooser) {
		this.fileChooser = fileChooser;
	}

	abstract protected void addHandlers();

	abstract protected void initGui();

	abstract protected void addKeyListeners();

	public static JButton getPinToTopButton(JFrame targetFrame) {
		return ScreepTGuiFactory.getPinToTopButton(targetFrame, IconsPathConfigurator.ICON_PIN_GREEN);
	}

	protected void styleAllButtons(Color background, Color foreground, boolean makeTransparent) {
		try {
			for(JButton button : ReflectionUtils.retrieveFieldsByType(this, JButton.class)) {
				button.setCursor(GuiUtils.CURSOR_HAND);
				button.setFont(ScreepTGuiFactory.DEFAULT_BTN_FONT);
				if(makeTransparent)
					GuiUtils.makeTransparentJButton(button);

				if(background != null)
					button.setBackground(background);

				if(foreground != null)
					button.setForeground(foreground);
			}
		} catch (Exception e) {
		}
	}

	public static SelectiveLineEnabledTextArea getSpecialTextArea(SelectiveLineEnabledTextArea toEnrich, Color background, Color foreground, Color caretColor,
			Font font) {
		SelectiveLineEnabledTextArea textArea = toEnrich != null ? toEnrich : new SelectiveLineEnabledTextArea();
		textArea.setMargin(new Insets(4, 4, 4, 4));

		if (font != null)
			textArea.setFont(font);

		if (foreground != null)
			textArea.setForeground(foreground);

		if (background != null)
			textArea.setBackground(background);

		if (caretColor != null)
			textArea.setCaretColor(caretColor);

		return textArea;
	}


	/**
	 * This method syncronize current secondary panel textarea with the main one retrieved with
	 * @throws IOException
	 */
	@Override
	public void sync() {
		// DEFAULT warning message if not overridden
		logger.warn("Sync method is not yet implemented");
	}

	/**
	 * This method reinitialize the panel based on data contained in file identified by target key.
	 * You can refer to generalConfig to know which kind of panel you can call and their relative files
	 * @throws Exception
	 */
	@Override
	public void reload(String target) throws Exception{
		// DEFAULT warning message if not overridden
		logger.warn("Reload method is not yet implemented");
	}

	/**
	 * This method reinitialize the panel based on data contained in file identified by target key.
	 * You can refer to generalConfig to know which kind of panel you can call and their relative files
	 *
	 * @throws IOException
	 */
	@Override
	public void doAction(String target) throws IOException{
		// DEFAULT warning message if not overridden
		logger.warn("doAction method is not yet implemented");
	}

}
