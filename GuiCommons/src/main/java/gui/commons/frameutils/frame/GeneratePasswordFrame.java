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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.commons.frameutils.frame.arch.ToolIcons;
import gui.commons.frameutils.frame.arch.ToolStyle;
import gui.commons.imagestudio.DialogFit;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import gui.commons.utils.PasswordStrength;
import gui.commons.utils.StringWorkerExtended;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.dialogs.msg.JOptionHelper.Choice;
import various.common.light.utility.hash.SafeHasher;
import various.common.light.utility.hash.SafeHasher.HashAlgorithm;
import various.common.light.utility.hash.SafeHasher.IterationsLevel;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.string.StringWorker;
import various.common.light.i18n.I18n;

/**
 * Random password from the chosen character sets, or a deterministic one derived from a word (PBKDF2-SHA512).
 * Every password is judged with the same strength check, which also works on any password typed by the user.
 * Same look of the other tool windows (hash checker, color picker): colors and fonts come from the look and feel.
 */
public class GeneratePasswordFrame extends ParentFrame {
	private static final long serialVersionUID = 7066597918430250412L;

	private static final byte[] fixedSalt = new byte[] {'F','0','Z','$','U','_','4','v','p','X','*','h','8','3','X','S'};

	public static GeneratePasswordFrame singletonInstance = null;

	/** Random source for the passwords: cryptographically strong, unlike java.util.Random */
	private static final SecureRandom RANDOM = new SecureRandom();

	private static final int DEFAULT_LENGTH = 16;

	/** Orange with white text readable (the tool color is too light for white text) */
	private static final Color ORANGE_DARK = new Color(176, 98, 16);

	private static boolean symbolsFlag = false;
	private static boolean numbersFlag = true;
	private static boolean specialsFlag = false;

	JFrame thisFrame;

	private JPasswordField textField;
	private JCheckBox chckbxShowKey;
	private JButton btnCopy;
	private JSpinner spinnerLength;
	private JButton btnGenerate;
	private JCheckBox rdbtnUseNumbers;
	private JCheckBox rdbtnUseSymbols;
	private JCheckBox rdbtnUseSpecials;
	private JButton btnHashed;
	private ToolStyle.Badge strengthBadge;

	private JPasswordField checkField;
	private JCheckBox chckbxShowCheck;
	private ToolStyle.Badge checkBadge;
	private StrengthMeter strengthMeter;
	private JLabel tipsLabel;

	/** Size of the content, computed once the window is packed: the window fits its content */
	private Dimension fitDimension;

	public GeneratePasswordFrame(JFrame parentFrame) {
		super(parentFrame, null);
		thisFrame = this;
		singletonInstance = this;

		logger.info(this.getClass().getName() + " - Starting...");

		setTitle("Generate Password - " + GeneralConfig.APPLICATION_NAME);

		setAlwaysOnTop(true);

		// SW window icon
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_KEYGEN_PSW, this);

		buildGui();
		addHandlers();
		updateCheck();

		DialogFit.apply(this, parentFrame, 0, 0);
		fitDimension = getSize();

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				generate();
				logger.info(this.getClass().getName() + " - Started and ready!");
			}
		});

	}

	private void buildGui() {

		JPanel content = new JPanel(new MigLayout("wrap 1, fill, insets 14 18 14 18, gapy 10", "[grow,fill]", "[][][][]"));
		setContentPane(content);

		content.add(ToolStyle.header("Password Generator", "Random passwords from the characters you choose, or derived from a word"));

		// OUTPUT
		JPanel output = ToolStyle.card(new MigLayout("insets 0, fillx, gap 8 6", "[grow,fill][][]", "[][]"));

		textField = new JPasswordField();
		textField.setFont(ToolStyle.mono(4f));
		textField.setHorizontalAlignment(SwingConstants.CENTER);
		textField.setEchoChar((char) 0);
		textField.setColumns(22);
		textField.putClientProperty("JPasswordField.cutCopyAllowed", true);
		textField.setToolTipText("Click to select all");

		chckbxShowKey = ToolStyle.hand(new JCheckBox("Show"));
		chckbxShowKey.setToolTipText("Show or hide the password");

		btnCopy = ToolStyle.button("Copy", ToolIcons.Kind.COPY, "Copy the password to the clipboard");

		strengthBadge = ToolStyle.badge();
		strengthBadge.setToolTipText("Hover for the tips to make it stronger");

		btnGenerate = ToolStyle.accent(ToolStyle.button("Generate", ToolIcons.Kind.DICE, "Generate a new random password"), ToolStyle.BLUE);

		output.add(textField, "growx, h 40!");
		output.add(chckbxShowKey);
		output.add(btnCopy, "wrap");
		output.add(strengthBadge);
		output.add(btnGenerate, "span 2, alignx right");
		content.add(output, "growx");

		// OPTIONS
		JPanel options = ToolStyle.card(new MigLayout("insets 0, fillx, gap 10 8", "[][grow,fill]", "[][][]"));

		spinnerLength = ToolStyle.hand(new JSpinner());
		spinnerLength.setToolTipText("Best match to use in ScreepT is 16 characters");
		spinnerLength.setModel(new SpinnerNumberModel(DEFAULT_LENGTH, 1, 64, 1));

		JPanel lengthRow = new JPanel(new MigLayout("insets 0, gap 10", "[]10[]", "[]"));
		lengthRow.setOpaque(false);
		lengthRow.add(spinnerLength);
		lengthRow.add(ToolStyle.mutedLabel("Best match in ScreepT: 16 characters"));

		rdbtnUseNumbers = ToolStyle.hand(new JCheckBox("Use numbers [" + ConversionUtils.numberString + "]"));
		rdbtnUseNumbers.setSelected(numbersFlag);
		rdbtnUseSymbols = ToolStyle.hand(new JCheckBox("Use Symbols [" + ConversionUtils.symbolString + "]"));
		rdbtnUseSymbols.setSelected(symbolsFlag);
		rdbtnUseSpecials = ToolStyle.hand(new JCheckBox("Use Specials [" + ConversionUtils.specialString + "]"));
		rdbtnUseSpecials.setSelected(specialsFlag);

		JPanel charsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
		charsRow.setOpaque(false);
		charsRow.add(rdbtnUseNumbers);
		charsRow.add(rdbtnUseSymbols);
		charsRow.add(rdbtnUseSpecials);

		btnHashed = ToolStyle.button("From Word", ToolIcons.Kind.PASTE, "Derive a deterministic password from a word (PBKDF2-SHA512). The salt is fixed: the same word and length give the same password on every ScreepT");
		JPanel sourceRow = new JPanel(new MigLayout("insets 0, gap 10", "[]10[]", "[]"));
		sourceRow.setOpaque(false);
		sourceRow.add(btnHashed);
		sourceRow.add(ToolStyle.mutedLabel("Same word and length always give the same password"));

		options.add(ToolStyle.caption("Length"), "aligny center");
		options.add(lengthRow, "growx, wrap");
		options.add(ToolStyle.caption("Characters"), "aligny center");
		options.add(charsRow, "growx, wrap");
		options.add(ToolStyle.caption("Source"), "aligny center");
		options.add(sourceRow, "growx");
		content.add(options, "growx");

		// STRENGTH CHECK: any password typed here, the level and the tips follow every key
		JPanel check = ToolStyle.card(new MigLayout("insets 0, fillx, gap 8 6", "[grow,fill][]", "[][][][][]"));

		checkField = new JPasswordField();
		checkField.setColumns(22);
		checkField.putClientProperty("JTextField.placeholderText", "Type any password to check it");
		checkField.putClientProperty("JPasswordField.cutCopyAllowed", true);

		chckbxShowCheck = ToolStyle.hand(new JCheckBox("Show"));
		checkBadge = ToolStyle.badge();
		strengthMeter = new StrengthMeter();
		tipsLabel = new JLabel(" ");
		tipsLabel.setFont(ToolStyle.font(0f, java.awt.Font.PLAIN));

		check.add(ToolStyle.caption("Password strength"), "span 2, wrap");
		check.add(checkField, "growx, h 34!");
		check.add(chckbxShowCheck, "wrap");
		check.add(strengthMeter, "span 2, growx, h 12!, wrap");
		check.add(checkBadge, "span 2, wrap");
		JScrollPane tipsScroll = new JScrollPane(tipsLabel);
		tipsScroll.setBorder(null);
		tipsScroll.setOpaque(false);
		tipsScroll.getViewport().setOpaque(false);
		// fixed room for the tips: the window is sized once, a longer list scrolls instead of resizing it
		tipsScroll.setPreferredSize(new Dimension(440, 96));
		check.add(tipsScroll, "span 2, growx");
		content.add(check, "growx");
	}

	private void addHandlers() {

		// DEFAULT CLOSE ACTIONS
		addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {

		    	singletonInstance = null;

		    	dispose();
		    }
	    });

		// SHOW/HIDE PASSWORD
		chckbxShowKey.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				textField.setEchoChar(chckbxShowKey.isSelected() ? DEFAULT_PSW_CHAR : (char) 0);
			}
		});

		// SHOW/HIDE THE PASSWORD TYPED FOR THE CHECK
		chckbxShowCheck.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				checkField.setEchoChar(chckbxShowCheck.isSelected() ? (char) 0 : DEFAULT_PSW_CHAR);
			}
		});

		// every change of the typed password updates the level and the tips
		checkField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateCheck();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateCheck();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateCheck();
			}
		});

		btnCopy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ToolStyle.copyToClipboard(new String(textField.getPassword()));
			}
		});

		rdbtnUseNumbers.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				numbersFlag = rdbtnUseNumbers.isSelected();
			}
		});
		rdbtnUseSymbols.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				symbolsFlag = rdbtnUseSymbols.isSelected();
			}
		});
		rdbtnUseSpecials.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				specialsFlag = rdbtnUseSpecials.isSelected();
			}
		});

		btnGenerate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				generate();
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
						// the derived password is judged like any other
						showStrength(converted);

					} catch (NoSuchAlgorithmException | InvalidKeySpecException e1) {
						dialogHelper.error(I18n.t("common.an_error_occurred"), "An error occurred");
						logger.error("Cannot hash user source password", e1);
					} catch(Exception e2) {
						logger.error("Generic exception: Cannot hash user source password", e2);
					}
				}

				thisFrame.setCursor(GuiUtilsExtended.CURSOR_DEFAULT);
			}
		});

		// a click selects all the password, to copy it at once
		textField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String selection = textField.getSelectedText();
				if (selection == null) {
					textField.selectAll();
				}
			}
		});
	}

	/** New random password from the selected character sets, judged for its strength */
	private void generate() {
		char[] alphabet = getPossibleCharacters();
		int length = (int) spinnerLength.getValue();
		char[] password = new char[length];
		for (int i = 0; i < length; i++) {
			password[i] = alphabet[RANDOM.nextInt(alphabet.length)];
		}
		String generated = new String(password);
		Arrays.fill(password, '\0');
		textField.setText(generated);
		showStrength(generated);
	}

	/** Level and bits of the password in the output, the tips in its tooltip */
	private void showStrength(String password) {
		PasswordStrength.Result result = PasswordStrength.analyze(password);
		if (result.getLevel() == null) {
			strengthBadge.setStatus("Empty password", null, null);
			strengthBadge.setToolTipText(null);
			return;
		}
		strengthBadge.setStatus(result.getLevel().getTitle() + " - " + result.getBits() + " bits", null, levelColor(result.getLevel()));
		strengthBadge.setToolTipText(tipsHtml(result.getTips()));
	}

	/** Level, meter and tips of the password typed in the check field */
	private void updateCheck() {
		char[] typed = checkField.getPassword();
		PasswordStrength.Result result = PasswordStrength.analyze(new String(typed));
		Arrays.fill(typed, '\0');
		if (result.getLevel() == null) {
			strengthMeter.setLevel(null);
			checkBadge.setStatus("Type a password to check it", null, null);
			tipsLabel.setText(" ");
			return;
		}
		strengthMeter.setLevel(result.getLevel());
		checkBadge.setStatus(result.getLevel().getTitle() + " - " + result.getBits() + " bits", null, levelColor(result.getLevel()));
		tipsLabel.setText(tipsHtml(result.getTips()));
	}

	/** Tips as a list, wrapped to a readable width */
	private static String tipsHtml(List<String> tips) {
		StringBuilder html = new StringBuilder("<html><body style='width: 440px'>");
		for (String tip : tips) {
			html.append("&bull; ").append(tip).append("<br>");
		}
		return html.append("</body></html>").toString();
	}

	/** Color of a level: red for the weak ones, green when the password is really hard */
	private static Color levelColor(PasswordStrength.Level level) {
		switch (level) {
			case HACKERS_BIRTHDAY_GIFT:
			case GRANDMA:
				return ToolStyle.RED;
			case SCRIPT_KIDDIE:
			case BORED_TEENAGER:
				return ORANGE_DARK;
			case STUBBORN_HACKER:
				return ToolStyle.BLUE;
			default:
				return ToolStyle.GREEN;
		}
	}

	/** One segment for each security level: the ones reached are colored, the others are muted */
	private static final class StrengthMeter extends JComponent {
		private static final long serialVersionUID = 1L;

		private PasswordStrength.Level level;

		StrengthMeter() {
			setOpaque(false);
		}

		void setLevel(PasswordStrength.Level level) {
			this.level = level;
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(240, 12);
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				PasswordStrength.Level[] levels = PasswordStrength.Level.values();
				int reached = level == null ? 0 : level.ordinal() + 1;
				int gap = 4;
				int segment = (getWidth() - gap * (levels.length - 1)) / levels.length;
				for (int i = 0; i < levels.length; i++) {
					g.setColor(i < reached ? levelColor(levels[i]) : ToolStyle.withAlpha(ToolStyle.muted(), 70));
					g.fillRoundRect(i * (segment + gap), 0, segment, getHeight(), 6, 6);
				}
			} finally {
				g.dispose();
			}
		}
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
		return fitDimension != null ? new Dimension(fitDimension) : new Dimension(600, 300);
	}

	@Override
	public Dimension getMinimumDimension() {
		return getDefaultDimension();
	}

	public static GeneratePasswordFrame getInstance(JFrame parentFrame) {
		return singletonInstance == null ? new GeneratePasswordFrame(parentFrame) : singletonInstance;
	}
}
