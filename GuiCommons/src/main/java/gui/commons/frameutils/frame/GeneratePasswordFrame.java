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
package gui.commons.frameutils.frame;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.RandomStringUtils;

import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.commons.frameutils.frame.panels.ClipboardPanelLight;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import gui.commons.utils.StringWorkerExtended;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.dialogs.msg.JOptionHelper.Choice;
import various.common.light.om.SecurityLevel;
import various.common.light.utility.hash.SafeHasher;
import various.common.light.utility.hash.SafeHasher.HashAlgorithm;
import various.common.light.utility.hash.SafeHasher.IterationsLevel;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.string.StringWorker;

public class GeneratePasswordFrame extends ParentFrame{
	private static final long serialVersionUID = 7066597918430250412L;

	private static final byte[] fixedSalt = new byte[] {'F','0','Z','$','U','_','4','v','p','X','*','h','8','3','X','S'};

	public static GeneratePasswordFrame singletonInstance = null;

	private static boolean symbolsFlag = false;
	private static boolean numbersFlag = true;
	private static boolean specialsFlag = false;

	JFrame thisFrame;

	ClipboardPanelLight	payloadPanel;
	private JPasswordField textField;
	private JLabel lblLength;
	private JSpinner spinnerLength;
	private JButton btnGenerate;
	private JRadioButton rdbtnUseNumbers;
	private JRadioButton rdbtnUseSymbols;
	private JRadioButton rdbtnUseSpecials;
	private JSeparator separator;
	private JButton btnHashed;
	private JCheckBox chckbxShowKey;

	@SuppressWarnings("unused")
	private SecurityLevel securityLevel = SecurityLevel.NO_CHECK;

	public GeneratePasswordFrame(JFrame parentFrame) {
		super(parentFrame, null);
		thisFrame = this;
		singletonInstance = this;

		getContentPane().setBackground(new Color(10,10,10));

		logger.info(this.getClass().getName() + " - Starting...");

		setTitle("Generate Password - " + GeneralConfig.APPLICATION_NAME);

		setAlwaysOnTop(true);

		// SW window icon
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_KEYGEN_PSW, this);
		getContentPane().setLayout(new MigLayout("", "[::5px][:230px:230px,grow][grow][40px:n][100px:n][][70px][60px][]", "[:35px:35px][][35px,grow]"));

		textField = new JPasswordField();
		getContentPane().add(textField, "cell 1 0 2 1,grow");
		textField.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 17));
		textField.setColumns(10);
		textField.setEchoChar((char) 0);
		textField.setBackground(Color.DARK_GRAY);
		textField.setForeground(Color.WHITE);
		textField.setHorizontalAlignment(SwingConstants.CENTER);
		textField.putClientProperty("JPasswordField.cutCopyAllowed",true);

		chckbxShowKey = new JCheckBox("");
		chckbxShowKey.setIcon(new ImageIcon(IconsPathConfigurator.ICON_VISIBLE));
		chckbxShowKey.setSelectedIcon(new ImageIcon(IconsPathConfigurator.ICON_INVISIBLE));
		chckbxShowKey.setRolloverIcon(chckbxShowKey.getIcon());
		chckbxShowKey.setOpaque(false);
		chckbxShowKey.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		chckbxShowKey.setForeground(Color.WHITE);
		getContentPane().add(chckbxShowKey, "cell 3 0,alignx center,aligny center");

		lblLength = new JLabel("Length");
		lblLength.setForeground(Color.WHITE);
		lblLength.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
		getContentPane().add(lblLength, "cell 4 0,alignx center");

		spinnerLength = new JSpinner();
		spinnerLength.setMaximumSize(new Dimension(32767, 30));
		spinnerLength.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		spinnerLength.setToolTipText("Best match to use in ScreepT is 16 characters");
		spinnerLength.setModel(new SpinnerNumberModel(16, 1, 64, 1));
		GuiUtils.setJspinnerColors(spinnerLength, Color.GRAY, new Color(10,10,10));
		spinnerLength.setAlignmentY(Component.TOP_ALIGNMENT);
		spinnerLength.setAlignmentX(Component.LEFT_ALIGNMENT);
		spinnerLength.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
		getContentPane().add(spinnerLength, "cell 5 0,grow");

		btnGenerate = new JButton("Generate");
		btnGenerate.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnGenerate.setBackground(Color.GRAY);
		btnGenerate.setFont(new Font("Segoe UI", Font.BOLD, 15));
		getContentPane().add(btnGenerate, "cell 6 0,grow");

		btnHashed = new JButton("From Word");
		btnHashed.setFont(new Font("Segoe UI", Font.BOLD, 15));
		btnHashed.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnHashed.setBackground(Color.GRAY);
		getContentPane().add(btnHashed, "cell 7 0 2 1,grow");

		separator = new JSeparator();
		getContentPane().add(separator, "cell 0 1 9 1,growx");

		rdbtnUseNumbers = new JRadioButton("Use numbers ["+ConversionUtils.numberString+"]");
		rdbtnUseNumbers.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		rdbtnUseNumbers.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		rdbtnUseNumbers.setOpaque(false);
		rdbtnUseNumbers.setForeground(Color.WHITE);
		getContentPane().add(rdbtnUseNumbers, "cell 0 2 2 1,alignx center");

		rdbtnUseSpecials = new JRadioButton("Use Specials ["+ConversionUtils.specialString+"]");
		rdbtnUseSpecials.setOpaque(false);
		rdbtnUseSpecials.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		rdbtnUseSpecials.setForeground(Color.WHITE);
		rdbtnUseSpecials.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		getContentPane().add(rdbtnUseSpecials, "cell 2 2 4 1,alignx center");

		rdbtnUseSymbols = new JRadioButton("Use Symbols  ["+ConversionUtils.symbolString+"]");
		rdbtnUseSymbols.setOpaque(false);
		rdbtnUseSymbols.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		rdbtnUseSymbols.setForeground(Color.WHITE);
		rdbtnUseSymbols.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		getContentPane().add(rdbtnUseSymbols, "cell 6 2 3 1,alignx center");

		addHandlers();

		setLocationRelativeTo(parentFrame);

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				rdbtnUseNumbers.setSelected(numbersFlag);
				rdbtnUseSpecials.setSelected(specialsFlag);
				rdbtnUseSymbols.setSelected(symbolsFlag);
				adaptSecurityLevel();
				btnGenerate.doClick();
				logger.info(this.getClass().getName() + " - Started and ready!");
			}
		});

	}

	private void adaptSecurityLevel() {
		if(numbersFlag && specialsFlag && symbolsFlag) {
			securityLevel = SecurityLevel.MAX;
		} else if(numbersFlag) {
			securityLevel = SecurityLevel.MID;
		} else {
			securityLevel = SecurityLevel.NO_CHECK;
		}
	}

	private void addHandlers() {

		// DEFAULT CLOSE ACTIONS
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {

		    	singletonInstance = null;

		    	dispose();
		    }
	    });

		// SHOW/HIDE PASSWORD
		chckbxShowKey.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (chckbxShowKey.isSelected()) {
					textField.setEchoChar(DEFAULT_PSW_CHAR);
				}else {
					textField.setEchoChar((char) 0);
				}
			}
		});

		rdbtnUseNumbers.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				numbersFlag = rdbtnUseNumbers.isSelected();
				adaptSecurityLevel();
			}
		});
		rdbtnUseSymbols.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				symbolsFlag = rdbtnUseSymbols.isSelected();
				adaptSecurityLevel();
			}
		});
		rdbtnUseSpecials.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				specialsFlag = rdbtnUseSpecials.isSelected();
				adaptSecurityLevel();
			}
		});

		btnGenerate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String randomString = RandomStringUtils.random((int)spinnerLength.getValue(), getPossibleCharacters());
				textField.setText(randomString);
			}
		});

		btnHashed.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				Choice<char[]> source = dialogHelper.askForPasswordNullable(
						"Insert source word to create a password using PBKDF2SHA512",
						"Specify source",
						64,
						new Object[] {"Generate random", "Generate deterministic"});

				thisFrame.setCursor(GuiUtilsExtended.CURSOR_HAND);

				if(source != null && source.value.length == 0){
					dialogHelper.warn("You have to specify a password in order to get an hashed value!", "Invalid input");

				} else if(source != null && source.value != null) {
					try {
						char[] expanded;
						if (source.option == 0) {
							expanded = source.value;
						} else {
							expanded = SafeHasher.byteArrayToChar(SafeHasher.fillArrayFromOtherArray(SafeHasher.charArrayToByte(source.value), (int) spinnerLength.getValue()));
						}
						final String raw = ConversionUtils.printHexBinary(SafeHasher.hashPswWithGivenSalt(expanded, fixedSalt, IterationsLevel.HIGH, HashAlgorithm.PBKDF2SHA512));
						final String converted = source.option == 0 ?
								StringWorkerExtended.forceLength(raw, (int)spinnerLength.getValue(), getPossibleCharacters()) :
									StringWorker.forceLengthDeterministic(raw, (int)spinnerLength.getValue(), "O");
						textField.setText(converted);

					} catch (NoSuchAlgorithmException | InvalidKeySpecException e1) {
						dialogHelper.error("An error occurred!", "An error occurred");
						logger.error("Cannot hash user source password", e1);
					} catch(Exception e2) {
						logger.error("Generic exception: Cannot hash user source password", e2);
					}
				}

				thisFrame.setCursor(GuiUtilsExtended.CURSOR_DEFAULT);
			}
		});

		textField.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				String selection = textField.getSelectedText();
				if (selection == null) {
					textField.selectAll();
				}
			}
		});
	}

	private char[] getPossibleCharacters() {

		char[] symbolChars = rdbtnUseSymbols.isSelected() ? ConversionUtils.symbolCharacters : new char[0];
		char[] numberChars = rdbtnUseNumbers.isSelected() ? ConversionUtils.numberCharacters : new char[0];
		char[] specialChars = rdbtnUseSpecials.isSelected() ? ConversionUtils.specialCharacters : new char[0];

		char[] possibleCharacters = new char[ConversionUtils.baseCharacters.length + symbolChars.length + numberChars.length + specialChars.length];

		int i = 0;

		for(char base : ConversionUtils.baseCharacters) {
			possibleCharacters[i] = base;
			i++;
		}

		for(char symbol : symbolChars) {
			possibleCharacters[i] = symbol;
			i++;
		}

		for(char number : numberChars) {
			possibleCharacters[i] = number;
			i++;
		}

		for(char special : specialChars) {
			possibleCharacters[i] = special;
			i++;
		}

		return possibleCharacters;
	}

	public void forceLentgh(int length) {
		spinnerLength.setValue(length);
		spinnerLength.setEnabled(false);
	}

	public void unforceLentgh(int length) {
		spinnerLength.setValue(length);
		spinnerLength.setEnabled(true);
	}

	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(880, 150);
	}

	public static GeneratePasswordFrame getInstance(JFrame parentFrame) {
		return singletonInstance == null ? new GeneratePasswordFrame(parentFrame) : singletonInstance;
	}
}
