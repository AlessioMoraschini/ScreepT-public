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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import gui.commons.dialogutils.JOptionHelperExtended;
import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import gui.commons.textarea.om.TxtFieldFileDragNDropManager;
import initializer.configs.impl.om.TextEditorOption;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.UpdateProgressPanel;
import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;

public class HashCheckerFrame extends ParentFrame{
	private static final long serialVersionUID = -5018329398511796700L;

	public static HashCheckerFrame singletonInstance = null;

	public static String HINT_VERIFY = "Write here the checksum to verify, and press \"Verify Checksum\" to compare with current file's hash!";

	private static Color lightGrayDefault = new Color(211, 211, 211);
	private static Color yellowDefault = new Color(250, 250, 210);

	private File currentFile;
	public JFrame parentFrame;
	private TxtFieldFileDragNDropManager txtFieldLoadedFile;
	private JTextField txtFieldChecksumResult;
	private JTextField txtFieldChecksumMatcher;
	private JComboBox<HashingFunction> comboBoxHashingAlgos;
	private JButton btnVerifyChecksum;
	private JButton btnCalculateHash;
	private JButton btnSelectFile;
	private TextEditorOption txtOptions;

	private JFrame thisFrame;

	private JRadioButton rdbtnUseAlgorithmAsPrefix;
	private JRadioButton rdbtnUppercase;

	public HashCheckerFrame(File initialFile, JFrame parentFrame, TextEditorOption txtOptions) {
		super(parentFrame, null);

		this.txtOptions = txtOptions == null ? new TextEditorOption() : txtOptions;

		logger.info(this.getClass().getName() + " - Starting...");

		setTitle("File Hash Checker");

		this.parentFrame = parentFrame;
		thisFrame = this;
		singletonInstance = this;

		getContentPane().setBackground(Color.DARK_GRAY);
		getContentPane().setLayout(new MigLayout("fill, insets 2", "[5px][150px:150px][220px,grow][50px:150px:150px][190px:190px:200px][5px]", "[5px,center][30px,center][][35px][][20px,grow][20px,grow][5px,center]"));

		btnSelectFile = new JButton("Select File");
		btnSelectFile.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnSelectFile.setFont(new Font("SansSerif", Font.BOLD, 12));
		btnSelectFile.setBackground(new Color(119, 136, 153));
		getContentPane().add(btnSelectFile, "cell 1 1,grow");

		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_HASHING, this);

		txtFieldLoadedFile = new TxtFieldFileDragNDropManager(new Runnable() {
			@Override
			public void run() {
				if (txtFieldLoadedFile != null && txtFieldLoadedFile.loadedFilesList != null && !txtFieldLoadedFile.loadedFilesList.isEmpty()) {
					loadFile(txtFieldLoadedFile.loadedFilesList.get(0), true);
					txtFieldLoadedFile.loadedFilesList.clear();
				}
			}
		}, txtFieldLoadedFile);

		txtFieldLoadedFile.setAllowDirectoriesDrop(true);
		txtFieldLoadedFile.setEditable(false);

		txtFieldLoadedFile.setBackground(lightGrayDefault);
		txtFieldLoadedFile.setPreferredSize(new Dimension(18, 25));
		txtFieldLoadedFile.setForeground(Color.DARK_GRAY);
		getContentPane().add(txtFieldLoadedFile, "cell 2 1 3 1,grow");

		JSeparator separator_1 = new JSeparator();
		getContentPane().add(separator_1, "cell 1 2 4 1,growx");

		JLabel lblHashingAlgorithm = new JLabel("Hashing algorithm:");
		lblHashingAlgorithm.setForeground(Color.LIGHT_GRAY);
		lblHashingAlgorithm.setFont(new Font("Tahoma", Font.BOLD, 14));
		getContentPane().add(lblHashingAlgorithm, "cell 1 3,alignx center");

//		comboBoxHashingAlgos = new JComboBox<HashingFunction>();
		comboBoxHashingAlgos = new JComboBox<HashingFunction>(HashingFunction.values());
		comboBoxHashingAlgos.setCursor(GuiUtilsExtended.CURSOR_HAND);
		getContentPane().add(comboBoxHashingAlgos, "cell 2 3,growx");

		rdbtnUppercase = new JRadioButton("Uppercase");
		rdbtnUppercase.setCursor(GuiUtilsExtended.CURSOR_HAND);
		rdbtnUppercase.setOpaque(false);
		rdbtnUppercase.setForeground(Color.WHITE);
		rdbtnUppercase.setFont(new Font("SansSerif", Font.PLAIN, 14));
		rdbtnUppercase.setSelected(txtOptions.uppercase_hash);
		getContentPane().add(rdbtnUppercase, "cell 3 3,alignx center");

		rdbtnUseAlgorithmAsPrefix = new JRadioButton("Use Algorithm as Prefix");
		rdbtnUseAlgorithmAsPrefix.setFont(new Font("SansSerif", Font.PLAIN, 14));
		rdbtnUseAlgorithmAsPrefix.setCursor(GuiUtilsExtended.CURSOR_HAND);
		rdbtnUseAlgorithmAsPrefix.setForeground(Color.WHITE);
		rdbtnUseAlgorithmAsPrefix.setOpaque(false);
		rdbtnUseAlgorithmAsPrefix.setSelected(txtOptions.prefix_hash);
		getContentPane().add(rdbtnUseAlgorithmAsPrefix, "cell 4 3,alignx center");

		JSeparator separator = new JSeparator();
		getContentPane().add(separator, "cell 1 4 4 1,growx");

		btnVerifyChecksum = new JButton("Verify Checksum");
		btnVerifyChecksum.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnVerifyChecksum.setFont(new Font("SansSerif", Font.BOLD, 12));
		btnVerifyChecksum.setBackground(new Color(169, 169, 169));
		getContentPane().add(btnVerifyChecksum, "cell 1 5,grow");

		txtFieldChecksumMatcher = new JTextField(HINT_VERIFY);
		txtFieldChecksumMatcher.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		txtFieldChecksumMatcher.setBackground(yellowDefault);
		txtFieldChecksumMatcher.setPreferredSize(new Dimension(18, 25));
		txtFieldChecksumMatcher.setForeground(Color.DARK_GRAY);
		getContentPane().add(txtFieldChecksumMatcher, "cell 2 5 3 1,grow");

		btnCalculateHash = new JButton("Calculate Hash");
		btnCalculateHash.setFont(new Font("SansSerif", Font.BOLD, 12));
		btnCalculateHash.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnCalculateHash.setBackground(new Color(169, 169, 169));
		getContentPane().add(btnCalculateHash, "cell 1 6,grow");

		txtFieldChecksumResult = new JTextField("Click \"Calculate Hash\" to calculate Hash using selected algoritm");
		txtFieldChecksumResult.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		txtFieldChecksumResult.setBackground(yellowDefault);
		txtFieldChecksumResult.setPreferredSize(new Dimension(18, 25));
		txtFieldChecksumResult.setForeground(Color.DARK_GRAY);
		getContentPane().add(txtFieldChecksumResult, "cell 2 6 3 1,grow");

		loadFile(initialFile, false);

		addHandlers();

		resizeToDefault(true, true, false);
		setVisible(true);
		setAlwaysOnTop(true);
		setLocationRelativeTo(parentFrame);

		logger.info(this.getClass().getName() + " - Started!");
	}

	public void updateHash() {
		SwingUtilities.invokeLater(()->{
			btnCalculateHash.doClick();
		});
	}

	public void loadFile(File toLoadFile, boolean calculateHash) {
		if (toLoadFile != null && toLoadFile.exists()) {
			currentFile = toLoadFile;
			txtFieldLoadedFile.setText(currentFile.getAbsolutePath());
			txtFieldLoadedFile.setBackground(lightGrayDefault);
			txtFieldChecksumResult.setText("Click on 'calculate checksum' to get hash Cheksum for current file");

			if(!calculateHash)
				return;

			if ("".equals(txtFieldChecksumMatcher.getText()) || txtFieldChecksumMatcher.getText().equals(HINT_VERIFY)) {
				btnCalculateHash.doClick();
			} else {
				btnVerifyChecksum.doClick();
			}
		}
	}

	private void addHandlers() {
		// DEFAULT CLOSE ACTIONS
		this.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {

		    	singletonInstance = null;
	        	isActive.set(false);
				logger.debug("Closing "+getClass().getName());
		    }
		});

		txtFieldLoadedFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				loadFile(new File(txtFieldLoadedFile.getText()), true);
			}
		});

		btnSelectFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File readFile = fileChooser.fileAndDirRead(thisFrame);
				if(readFile != null && readFile.exists()) {
					loadFile(readFile, true);
				}
			}
		});

		btnCalculateHash.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final String prefix = getPrefix();
				GuiUtilsExtended.launchThreadSafeSwing(new Runnable() {
					@Override
					public void run() {
						UpdateProgressPanel updateProgress = null;
						try {

							if (currentFile != null && currentFile.exists()) {
								if (currentFile.isFile()) {
									txtFieldChecksumResult.setText(prefix + ChecksumHasher.getFileHashBuffered(currentFile, getSelectedFunction(), rdbtnUppercase.isSelected()));
								} else if (dialogHelper.showYNDialog("Hashing a folder can be a long task, really want to start hashing?",
										"Hash folder?", JOptionHelperExtended.YES_OR_NO)) {
									updateProgress = new UpdateProgressPanel(false);
									updateProgress.setClosable(true);
									updateProgress.setUndefinedDuration();
									txtFieldChecksumResult.setText(prefix + ChecksumHasher.getFolderHash(currentFile, getSelectedFunction(), rdbtnUppercase.isSelected()));
								}
								txtFieldChecksumResult.setBackground(GeneralConfig.ADVICE_FOOTER_OK_COLOR);
							} else {
								txtFieldLoadedFile.setBackground(yellowDefault);
								dialogHelper.warn("File not existing or not selected!", "File not found");
							}

						} catch (Throwable e1) {
							logger.error("An error occurred while calculating hash for current file: " + currentFile, e1);
							dialogHelper.error("An error occurred while calculating hash!", "An error occurred");
							txtFieldChecksumResult.setBackground(GeneralConfig.ADVICE_FOOTER_KO_COLOR);
						} finally {
							if (updateProgress != null) {
								updateProgress.forceClose();
							}
						}
					}
				});
			}
		});

		btnVerifyChecksum.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final String prefix = getPrefix();
				GuiUtilsExtended.launchThreadSafeSwing(new Runnable() {
					@Override
					public void run() {
						UpdateProgressPanel updateProgress = null;
						try {
							if (currentFile != null && currentFile.exists()) {
								String hash = "";
								if (rootPaneCheckingEnabled) {
									if (currentFile.isFile()) {
										hash = prefix + ChecksumHasher.getFileHashBuffered(currentFile, getSelectedFunction(), rdbtnUppercase.isSelected());
									} else {
										if (dialogHelper.showYNDialog("Hashing a folder can be a long task, are you sure to start hashing?",
												"Hash folder?", JOptionHelperExtended.YES_OR_NO)) {
											updateProgress = new UpdateProgressPanel(false);
											updateProgress.setClosable(true);
											updateProgress.setUndefinedDuration();
											hash = prefix + ChecksumHasher.getFolderHash(currentFile, getSelectedFunction(), rdbtnUppercase.isSelected());
										} else {
											return;
										}
									}
								}

								txtFieldChecksumResult.setText(hash);
								txtFieldChecksumResult.setBackground(GeneralConfig.ADVICE_FOOTER_OK_COLOR);
								if (txtFieldChecksumMatcher.getText().trim().equalsIgnoreCase(hash)) {
									txtFieldChecksumMatcher.setBackground(GeneralConfig.ADVICE_FOOTER_OK_COLOR);
									dialogHelper.info("File matches the given checksum!", "File matched!");
								} else {
									txtFieldChecksumMatcher.setBackground(GeneralConfig.ADVICE_FOOTER_KO_COLOR);
									dialogHelper.error("File does not match the given checksum!", "File does not match!");
								}
							} else {
								txtFieldLoadedFile.setBackground(yellowDefault);
								dialogHelper.warn("File not existing or not selected!", "File not found");
							}

						} catch (Throwable e1) {
							logger.error("An error occurred while calculating hash for current file: " + currentFile, e1);
							dialogHelper.error("An error occurred while calculating hash!", "An error occurred");
							txtFieldChecksumMatcher.setBackground(GeneralConfig.ADVICE_FOOTER_KO_COLOR);
							txtFieldChecksumResult.setBackground(GeneralConfig.ADVICE_FOOTER_KO_COLOR);
						} finally {
							if (updateProgress != null) {
								updateProgress.forceClose();
							}
						}
					}
				});
			}
		});

		rdbtnUppercase.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						txtOptions.setUppercase_hash(rdbtnUppercase.isSelected());
					}
				});
			}
		});

		rdbtnUseAlgorithmAsPrefix.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						txtOptions.setPrefix_hash(rdbtnUseAlgorithmAsPrefix.isSelected());
					}
				});
			}
		});

		txtFieldChecksumMatcher.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {
				if(HINT_VERIFY.equals(txtFieldChecksumMatcher.getText())) {
					txtFieldChecksumMatcher.setText("");
				} else {
					txtFieldChecksumMatcher.selectAll();
				}
			}
		});

		txtFieldChecksumResult.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {
				if (txtFieldChecksumResult.getText() != null) {
					txtFieldChecksumResult.selectAll();
				}
			}
		});
	}

	private HashingFunction getSelectedFunction() {
		HashingFunction selected = (HashingFunction) comboBoxHashingAlgos.getSelectedItem();
		return selected;
	}

	private String getPrefix() {
		return (rdbtnUseAlgorithmAsPrefix.isSelected() ? comboBoxHashingAlgos.getSelectedItem().toString() + "=" : "");
	}

	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(940, 225);
	}

	public static HashCheckerFrame getInstance(File initialFile, JFrame parentFrame, TextEditorOption txtOptions) {
		return singletonInstance == null ? new HashCheckerFrame(initialFile, parentFrame, txtOptions) : singletonInstance;
	}
}
