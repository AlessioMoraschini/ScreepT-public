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
package various.common.light.gui.dialogs.msg;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.io.File;
import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import various.common.light.gui.GuiUtils;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.string.StringWorker;

public class JOptionHelper {

	public enum DefaultSysIcons {
		ERR("OptionPane.errorIcon"),
		WARN("OptionPane.warningIcon"),
		INFO("OptionPane.informationIcon"),
		QUESTION("OptionPane.questionIcon");

		public String key;

		private DefaultSysIcons(String key) {
			this.key = key;
		}

		public Icon getIcon() {
			return UIManager.getIcon(key);
		}
	}

	// MESSAGE TYPES
	public static final int ERR = JOptionPane.ERROR_MESSAGE;
	public static final int WARN = JOptionPane.WARNING_MESSAGE;
	public static final int INFO = JOptionPane.INFORMATION_MESSAGE;
	public static final int QUESTION = JOptionPane.QUESTION_MESSAGE;
	public static final int PLAIN = JOptionPane.PLAIN_MESSAGE;

	public static final int OK = JOptionPane.OK_OPTION;
	public static final int YES = JOptionPane.YES_OPTION;
	public static final int NO = JOptionPane.NO_OPTION;
	public static final int CANC = JOptionPane.CANCEL_OPTION;

	// QUESTION UTILS
	public static final int YES_OR_NO = JOptionPane.YES_NO_OPTION;
	public static final int YES_OR_NO_CANC = JOptionPane.YES_NO_CANCEL_OPTION;

	protected static final String DEF_ERR_MSG = "An error occurred!";
	protected static final String DEF_WARN_MSG = "Warning!";
	protected static final String DEF_INFO_MSG = "Info";

	public static final Object[] defaultOptionsExtended = new Object[]{"Yes", "No", "Cancel"};
	public static final Object[] defaultOptions = new Object[]{"Yes", "No"};
	public static final Object[] okOnlyOptions = new Object[]{"OK"};

	// LAZY ICON INITIALIZERS
	public static Icon ICON_ERR_REF;
	public static Icon ICON_WARN_REF;
	public static Icon ICON_INFO_REF;
	public static Icon ICON_QUESTION_REF;

	public Dimension IF_SCROLLABLE_DEF_DIMENSION = new Dimension( 500, 500 );

	public static Font DEFAULT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);

	public boolean enabledAdvices = true;

	// Parent component to center and refer to
	protected Component parentComponent;

	public static void init() {
		ICON_ERR_REF = UIManager.getIcon("OptionPane.errorIcon");
		ICON_WARN_REF = UIManager.getIcon("OptionPane.warningIcon");
		ICON_INFO_REF = UIManager.getIcon("OptionPane.informationIcon");
		ICON_QUESTION_REF = UIManager.getIcon("OptionPane.questionIcon");
		DEFAULT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
	}

	public JOptionHelper (Component parentComponent) {
		setParentComponent(parentComponent);
		this.enabledAdvices = true;
	}

	public JOptionHelper (Component parentComponent, boolean enableAdvices) {
		this(parentComponent);
		this.enabledAdvices = enableAdvices;
	}

	public void setParentComponent(Component parent) {
		this.parentComponent = (parentComponent != null) ? parentComponent : new JFrame();
	}

	/**
	 * Shows a customizable message dialog that remains alwaysOnTop. Returns true if ok is selected, false in other cases
	 * msgType can be error, info, warn, exc...
	 */
	public boolean showYNDialog(String msg, String title, int msgType) {

		return showYNMessageCommon(msg, title, msgType, false, false, false);
	}

	/**
	 * Shows a warning customizable message dialog that remains alwaysOnTop. Returns true if ok is selected, false in other cases
	 * msgType can be error, info, warn, exc...
	 */
	public boolean showYNDialogWarn(String msg, String title, int msgType) {

		return showYNMessageCommon(msg, title, msgType, false, true, false);
	}

	/**
	 * Shows a customizable message dialog that remains alwaysOnTop. Returns true if ok is selected, false in other cases
	 * msgType can be error, info, warn, exc...
	 */
	public Boolean showYNOrNullDialog(String msg, String title, int msgType) {

		return showYNMessageCommon(msg, title, msgType, true, false, false);
	}

	/**
	 * Shows a customizable message dialog that remains alwaysOnTop. Returns true if ok is selected, false in other cases
	 * msgType can be error, info, warn, exc...
	 */
	public Boolean showYNOrNullDialogWarn(String msg, String title) {

		return showYNMessageCommon(msg, title, JOptionPane.WARNING_MESSAGE, true, true, false);
	}

	/**
	 * Shows a customizable message dialog that remains alwaysOnTop. Returns true if ok is selected, false in other cases
	 * msgType can be error, info, warn, exc...
	 */
	public void showTypedOKDialog(String msg, String title, int msgType) {

		showMessageCommon(msg, title, msgType, okOnlyOptions, okOnlyOptions[0]);
	}

	/**
	 * Shows an error message
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void error(String msg) {
		if (enabledAdvices) {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_ERR_MSG;
			showMessageCommon(msg, msg, ERR, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	public void error(String msg, SafeLogger logger, Throwable e) {
		error(msg);
		logger.error("Cannot import theme.", e);
	}

	public void error(String msg, SafeLogger logger) {
		error(msg);
		logger.error("Cannot import theme.");
	}

	/**
	 * Shows an error message
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void error(String msg, String title) {
		if (enabledAdvices) {
			title = (!StringWorker.isEmpty(title)) ? title : DEF_ERR_MSG;
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_ERR_MSG;
			showMessageCommon(msg, title, ERR, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows an error message within a scrollable panel, useful to show long messages
	 *
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public boolean errorScroll(String msg, String title) {
		msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_ERR_MSG;
		JScrollPane scroller = getScrollableDialogPanel(msg);
		GuiUtils.scrollJScrollPane(scroller, false);
		return showYNMessageCommon(scroller, title, ERR, false, false, false);
	}

	/**
	 * Shows an error message within a scrollable panel, useful to show long messages
	 * This version pretty prints an exception/error (throwable)
	 *
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void errorScroll( String msg, String title, Throwable e) {
		if (enabledAdvices) {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_ERR_MSG;
			String exception = Arrays.deepToString(e.getStackTrace());
			String newMsg = msg+"\n\n"+exception;
			JScrollPane scroller = getScrollableDialogPanel(newMsg);
			GuiUtils.scrollJScrollPane(scroller, false);
			showMessageCommon(scroller, title, ERR, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows an error message within a scrollable panel, useful to show long messages. The default options are YES/NO
	 * This version pretty prints an exception/error (throwable)
	 *
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 *
	 * @return the selected object between options, or null if nothing is selected
	 */
	public Object errorScroll(String msg, String title, Throwable e, Object[] options, int msgOption) {
		Object res = null ;
		try {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_ERR_MSG;
			String exception = Arrays.deepToString(e.getStackTrace());
			String newMsg = msg + "\n\n" + exception;
			JScrollPane scroller = getScrollableDialogPanel(newMsg);
			GuiUtils.scrollJScrollPane(scroller, false);
			res = JEnhancedOptionPane.showInputDialog(title, false, parentComponent, scroller, msgOption, options, ERR, ICON_ERR_REF, null);
		} catch (Exception e2) {
		}

		return res;
	}

	/**
	 * Shows a warning message
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void warn(String msg, String title) {
		if (enabledAdvices) {
			title = (!StringWorker.isEmpty(title)) ? title : DEF_WARN_MSG;
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_WARN_MSG;
			showMessageCommon(msg, title, WARN, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows a warning message
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void warn(String msg) {
		if (enabledAdvices) {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_WARN_MSG;
			showMessageCommon(msg, msg, WARN, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows a warning message within a scrollable panel, useful to show long messages
	 *
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void warnScroll(String msg, String title) {
		if (enabledAdvices) {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_WARN_MSG;
			JScrollPane scroller = getScrollableDialogPanel(msg);
			GuiUtils.scrollJScrollPane(scroller, false);
			showMessageCommon(scroller, title, WARN, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows an error message within a scrollable panel, useful to show long messages. The default options are YES/NO
	 * This version pretty prints an exception/error (throwable)
	 *
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 *
	 * @return the selected object between options, or null if nothing is selected
	 */
	public Object warnScroll(String msg, String title, Throwable e, Object[] options, int msgOption) {
		Object res = null ;
		try {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_WARN_MSG;
			String exception = Arrays.deepToString(e.getStackTrace());
			String newMsg = msg + "\n\n" + exception;
			JScrollPane scroller = getScrollableDialogPanel(newMsg);
			GuiUtils.scrollJScrollPane(scroller, false);
			res = JEnhancedOptionPane.showInputDialog(title, false, parentComponent, scroller, msgOption, options, ERR, ICON_WARN_REF, null);
		} catch (Exception e2) {
		}

		return res;
	}

	/**
	 * Shows a INFO message
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void info(String msg) {
		if (enabledAdvices) {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_INFO_MSG;
			showMessageCommon(msg, msg, INFO, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows a INFO message
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void info(String msg, String title) {
		if (enabledAdvices) {
			title = (!StringWorker.isEmpty(title)) ? title : DEF_INFO_MSG;
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_INFO_MSG;
			showMessageCommon(msg, title, INFO, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	public void info(String msg, SafeLogger logger) {
		error(msg);
		logger.info(msg);
	}


	/**
	 * Shows a information message within a scrollable panel, useful to show long messages
	 *
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void infoScroll(String msg, String title) {
		if (enabledAdvices) {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_INFO_MSG;
			JScrollPane scroller = getScrollableDialogPanel(msg);
			showMessageCommon(scroller, title, INFO, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows a information message within a scrollable panel, useful to show long messages
	 *
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void infoScroll(String msg, String title, Dimension dimension) {
		if (enabledAdvices) {
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_INFO_MSG;
			JScrollPane scroller = getScrollableDialogPanel(msg);
			scroller.setMinimumSize(dimension);
			scroller.setPreferredSize(dimension);
			showMessageCommon(scroller, title, INFO, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows a Question message
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void question(String msg, String title) {
		if (enabledAdvices) {
			title = (!StringWorker.isEmpty(title)) ? title : DEF_INFO_MSG;
			msg = (!StringWorker.isEmpty(msg)) ? msg : DEF_INFO_MSG;
			showMessageCommon(msg, title, QUESTION, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows a plain message
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public void plainMsg(String msg, String title) {
		if (enabledAdvices) {
			title = (!StringWorker.isEmpty(title)) ? title : "Message";
			msg = (!StringWorker.isEmpty(msg)) ? msg : "Information message not specified";
			showMessageCommon(msg, title, PLAIN, okOnlyOptions, okOnlyOptions[0]);
		}
	}

	/**
	 * Shows a question and return boolean choice
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public boolean yesOrNo(String msg, String title) {

		title = (!StringWorker.isEmpty(title)) ? title : "Choose an option";
		msg = (!StringWorker.isEmpty(msg)) ? msg : "Choose an Option";

		return showYNMessageCommon(msg, title, QUESTION, false, false, false);
	}

	/**
	 * Shows a question and return boolean choice
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public boolean yesOrNo(String msg) {

		msg = (!StringWorker.isEmpty(msg)) ? msg : "Choose an Option";

		return showYNMessageCommon(msg, msg, QUESTION, false, false, false);
	}

	public boolean askOverwriteFile(File file) {
		String msg = GuiUtils.encapsulateInHtml("<b>File already exists</b>, do you want to overwrite it?<br>" + file);
		return yesOrNo(msg, "Overwrite?");
	}

	/**
	 * Shows a question and return int choice (OK|NO|CANC)
	 * @param msg nullable, but should be not null and not empty to be clear to user
	 * @param title nullable
	 */
	public int yesNoOrCanc(String msg, String title) {

		title = (!StringWorker.isEmpty(title)) ? title : "Plain";
		msg = (!StringWorker.isEmpty(msg)) ? msg : "Information message not specified";
		Boolean boolRes = showYNMessageCommon(msg, title, QUESTION, true, false, true);
		int result = boolRes == null ? CANC : boolRes ? OK : NO;

		// TODO check for bug when pressing X|ESC -> return no but should return canc

		return result;
	}

	public String askForString(String msg, String title) {

		title = (!StringWorker.isEmpty(title)) ? title : "Specify a String";
		msg = (!StringWorker.isEmpty(msg)) ? msg : "Insert a String";

		return askForString(msg, title, "New");
	}

	public String askForString(String msg) {

		msg = (!StringWorker.isEmpty(msg)) ? msg : "Insert a String";

		return askForString(msg, msg, "New");
	}

	public String askForString(String msg, String title, String initialValue) {
		String unsafe = askForStringNullable(msg, title, initialValue, new Object[]{"Confirm"});
		unsafe = unsafe == null ? "" : unsafe;

		return unsafe;
	}

	/**
	 * Returns null if cancel is pressed
	 * @param msg
	 * @param title
	 * @return selected string, or null if dialog is closed
	 */
	public String askForStringNullable(String msg, String title) {

		return askForStringNullable(msg, title, "", new Object[]{"Confirm"});
	}

	/**
	 * Returns null if cancel is pressed
	 * @param msg
	 * @param title
	 * @param initialVal
	 * @return selected string, or null if dialog is closed
	 */
	public String askForStringNullable(String msg, String title, String initialVal) {

		return askForStringNullable(msg, title, initialVal, new Object[]{"Confirm"});
	}

	/**
	 * Shows a customizable message dialog that remains alwaysOnTop. Returns true if ok is selected, false in other cases
	 * msgType can be error, info, warn, exc...
	 */
	public String askForStringNullable(Object msg, String title, String initialValue, Object options[]) {

		final JOptionPane pane = new JOptionPane(
				msg instanceof String ? getLabelStyledText((String)msg) : msg, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION, null,
				options, initialValue);
		pane.setWantsInput(true);
		pane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		pane.selectInitialValue();
		pane.setInitialSelectionValue(initialValue);
		final JDialog dialog = parentComponent != null ? pane.createDialog(parentComponent, title) : pane.createDialog(title);
		if(parentComponent != null)
			dialog.setLocationRelativeTo(parentComponent);
		dialog.setAutoRequestFocus(true);
		dialog.setModal(true);

		GuiUtils.launchThreadSafeSwing(new Runnable() {
			@Override
			public void run() {
				try {
					Point center = GuiUtils.getCenter(dialog);
					GuiUtils.mouseMove(center);
					Thread.sleep(100);
					GuiUtils.mouseClick(center.x, center.y);
				} catch (InterruptedException e) {}
			}
		});

		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);

		final Object value = pane.getInputValue();
		return (value == JOptionPane.UNINITIALIZED_VALUE) ? null : (String) value;
	}

	public char[] askForPasswordNullable(String msg, String title, int maxLength, String confirmBtnText) {

		Object[] options = new Object[] {confirmBtnText};
		Choice<char[]> result = askForPasswordNullable(msg, title, maxLength, options);

		return result != null ? result.value : null;
	}

	public Choice<char[]> askForPasswordNullable(String msg, String title, int maxLength, Object[] options) {

		JPanel panel = new JPanel();
		JLabel label = new JLabel(msg);
		JPasswordField pass = new JPasswordField(10);
		panel.add(label);
		panel.add(pass);

		int option = JOptionPane.showOptionDialog(parentComponent, panel, title,
				JOptionPane.NO_OPTION, QUESTION,
				null, options, options[0]);

		if(option != JOptionPane.DEFAULT_OPTION) {
			return new Choice<char[]>(option, pass.getPassword());

		} else {
			return null;
		}
	}

	public void setAdvicesEnabled(boolean enabled) {
		enabledAdvices = enabled;
	}
	public Dimension getIF_SCROLLABLE_DEF_DIMENSION() {
		return IF_SCROLLABLE_DEF_DIMENSION;
	}

	public void setIF_SCROLLABLE_DEF_DIMENSION(Dimension iF_SCROLLABLE_DEF_DIMENSION) {
		IF_SCROLLABLE_DEF_DIMENSION = iF_SCROLLABLE_DEF_DIMENSION;
	}

	// PRIVATE METHODS

	protected JScrollPane getScrollableDialogPanel(String preloadedMsg) {
		preloadedMsg = (preloadedMsg == null)? "" : preloadedMsg;

		JTextArea textArea = new JTextArea("Insert your Text here");
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(textArea);
		scrollPane.setVisible(true);
		scrollPane.setBackground(Color.DARK_GRAY);
		scrollPane.setForeground(Color.white);
		textArea.setBackground(Color.DARK_GRAY);
		textArea.setForeground(Color.white);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setMargin(new Insets(6,6,6,6));
		textArea.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		scrollPane.setPreferredSize(IF_SCROLLABLE_DEF_DIMENSION);

		textArea.setText(preloadedMsg);
		GuiUtils.scrollJScrollPane(scrollPane, false);
		textArea.requestFocus();
		textArea.setCaretPosition(0);

		return scrollPane;
	}

	public Boolean showYNMessageCommon(Object msg, String title, int msgType, boolean allowNull, boolean warningType, boolean cancEnabled) {

		Icon icon = warningType? ICON_WARN_REF : ICON_QUESTION_REF;
		int option = cancEnabled ? YES_OR_NO_CANC : YES_OR_NO;
		Object[] optionsAvailable = cancEnabled ? defaultOptionsExtended : defaultOptions;

		final JOptionPane optionPane = new JOptionPane(
				msg instanceof String ? getLabelStyledText((String)msg) : msg,
        		msgType,
        		option,
        		icon,
        		optionsAvailable,
        		allowNull ? optionsAvailable[optionsAvailable.length - 1] : optionsAvailable[0]);

		final JDialog dialog = optionPane.createDialog(title);
		try {
			dialog.setAlwaysOnTop(true);
			dialog.enableInputMethods(true);
			dialog.setLocationRelativeTo(parentComponent);
//			GuiUtils.moveRelativeTo(dialog, parentComponent);
			dialog.toFront();
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

	public String showYNMessageCommon(Object msg, String title, int msgType, boolean warningType, String[] optionsAvailable, int initialOptionIndex) {

		Icon icon = warningType? ICON_WARN_REF : ICON_QUESTION_REF;

		final JOptionPane optionPane = new JOptionPane(
				msg instanceof String ? getLabelStyledText((String)msg) : msg,
						msgType,
						YES_OR_NO_CANC,
						icon,
						optionsAvailable,
						optionsAvailable[initialOptionIndex]);

		final JDialog dialog = optionPane.createDialog(title);
		try {
			dialog.setAlwaysOnTop(true);
			dialog.enableInputMethods(true);
			dialog.setLocationRelativeTo(parentComponent);
			dialog.toFront();
			dialog.setVisible(true);
			dialog.dispose();
		} catch (Exception e) {
		}

		return (String) optionPane.getValue();
	}

	// Used internally to create other various dialogs without user input
	protected void showMessageCommon(Object msg, String title, int msgType, Object[] choiceOptions, Object defaultOption) {
		if (enabledAdvices) {

			Icon icon = ICON_INFO_REF;
			if (WARN == msgType) {
				icon = ICON_WARN_REF;
			} else if (ERR == msgType) {
				icon = ICON_ERR_REF;
			} else if (QUESTION == msgType) {
				icon = ICON_QUESTION_REF;
			}

			final JOptionPane optionPane = new JOptionPane(
					msg instanceof String ? getLabelStyledText((String) msg) : msg,
					msgType,
					JOptionPane.OK_OPTION,
					icon,
					choiceOptions,
					defaultOption);

			JDialog dialog = optionPane.createDialog(title);
			dialog.setAlwaysOnTop(true);
			dialog.setLocationRelativeTo(parentComponent);
			dialog.toFront();

			try {
				dialog.setIconImage(GuiUtils.iconToImage(icon));
			} catch (Exception e1) {}

			try {
				dialog.setVisible(true);
				dialog.dispose();
			} catch (Exception e) {
			}
		}
	}

	public JLabel getLabelStyledText(String msg) {
		JLabel label = new JLabel(msg, SwingConstants.CENTER);
		label.setOpaque(false);
		label.setFont(DEFAULT_FONT);
		label.setVisible(true);
		label.revalidate();
		label.repaint();

		return label;
	}

	/**
	 * open dialog in parent JComponent, displaying msg
	 * Check first precondition, if true ask to user else return false.
	 * After, if OK executes actionOK.
	 *
	 * @return if OK return true, NO return false, else return null if cancel is pressed or window closed
	 */
	public Boolean askYorNPrecond(String title, String msg, Component parent, boolean precondition) {
			int askSave = CANC;

			if (precondition) {
				askSave = yesNoOrCanc(msg, title);
			}else {
				return false;
			}

			if(askSave == OK){
				return true;

			}else if(askSave == NO) {
				return false;
			}

			return null;
	}

	public class Choice<T>{
		public int option = -1;
		public T value;

		public Choice() {
		}

		public Choice(int option, T value) {
			this.option = option;
			this.value = value;
		}
	}
}
