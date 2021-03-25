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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.GuiUtils;
import various.common.light.utility.hash.ChecksumHasher;
import various.common.light.utility.hash.HashingFunction;
import various.common.light.utility.manipulation.ConversionUtils;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.border.LineBorder;

import frameutils.frame.panels.arch.ParentPanel;
import frameutils.utils.GuiUtilsExtended;
import general.MyFileView;

import java.awt.Font;

import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JButton;

public class FilePropertiesFrame extends JFrame{
	private static final long serialVersionUID = -1506126616913194676L;

	public static final Color defaultRed = Color.ORANGE;
	public static final Color defaultBlue = Color.CYAN;
	public static final Color defaultGreen = Color.GREEN;
	
	public static final int DEF_width = 850;
	public static final int DEF_height = 240;
	
	private JTextField dynLblFileName;
	private JTextField dynLblPath;
	private JLabel lblSize;
	private JLabel lblPath;
	private JLabel lblFileName;
	private JLabel dynLblSize;
	private JLabel lblLastModified;
	private JLabel dynLblLastChange;
	private JPanel footerPanel;
	private JLabel dynLblCanRead;
	private JLabel dynLblCanWrite;
	private JLabel dynLblCanExecute;
	private JLabel dynLblIsHidden;

	public File fileUnderAnalysis;
	private JLabel lblCreatedOn;
	private JLabel lblLastAccess;
	private JLabel dynLblLastAccess;
	private JLabel dynLblCreationDate;
	private JLabel hashMd5Lbl;
	private JTextField hashMd5DynLbl;
	private JButton viewInFileSystemButton;
	
	public FilePropertiesFrame(File targetFile, boolean alwaysOnTop) {
		
		super();
		
		fileUnderAnalysis = targetFile;
		
		GuiUtilsExtended.setFrameIcon((targetFile != null && targetFile.isDirectory()) ? new ImageIcon(IconsPathConfigurator.iconFolderPath): MyFileView.getExtIcon(targetFile), this);
		this.setAlwaysOnTop(alwaysOnTop);
		
		GuiUtilsExtended.centerComponent(this, DEF_width, DEF_height);
		
		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		panel.setBackground(Color.DARK_GRAY);
		panel.setLayout(new MigLayout("", "[][grow][80px:n][grow][][85px:90px][][grow]", "[][10px:n][][10px:n][grow][30px:n:40px]"));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(panel);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		lblFileName = new JLabel("File Name:");
		lblFileName.setForeground(Color.LIGHT_GRAY);
		lblFileName.setFont(new Font("Tahoma", Font.BOLD, 14));
		panel.add(lblFileName, "cell 0 0,alignx center");
		
		dynLblFileName = ParentPanel.getTextField(null, Color.DARK_GRAY, null, null, true);
		panel.add(dynLblFileName, "cell 1 0 3 1,growx");
		
		viewInFileSystemButton = new JButton("");
		viewInFileSystemButton.setToolTipText("View in File System explorer");
		viewInFileSystemButton.setCursor(GuiUtils.CURSOR_HAND);
		viewInFileSystemButton.setIcon(new ImageIcon(IconsPathConfigurator.ICON_COPY_DIR_STRUCTURE));
		GuiUtilsExtended.makeTransparentJButton(viewInFileSystemButton);
		panel.add(viewInFileSystemButton, "cell 4 0,grow");
		
		lblSize = new JLabel("File Size:");
		lblSize.setForeground(Color.LIGHT_GRAY);
		lblSize.setFont(new Font("Tahoma", Font.BOLD, 14));
		panel.add(lblSize, "cell 5 0,alignx right");
		
		dynLblSize = new JLabel("");
		dynLblSize.setForeground(new Color(176, 196, 222));
		panel.add(dynLblSize, "cell 6 0 2 1,alignx left");
		
		JSeparator separator = new JSeparator();
		separator.setBackground(Color.GRAY);
		panel.add(separator, "cell 0 1 8 1,growx");
		
		lblPath = new JLabel("File Path:");
		lblPath.setForeground(Color.LIGHT_GRAY);
		lblPath.setFont(new Font("Tahoma", Font.BOLD, 14));
		panel.add(lblPath, "cell 0 2,alignx center");
		
		dynLblPath = ParentPanel.getTextField(null, Color.DARK_GRAY, null, null, true);
		panel.add(dynLblPath, "cell 1 2 4 1,growx");
		
		hashMd5Lbl = new JLabel("Hash MD5:");
		hashMd5Lbl.setForeground(Color.LIGHT_GRAY);
		hashMd5Lbl.setFont(new Font("Tahoma", Font.BOLD, 14));
		panel.add(hashMd5Lbl, "cell 5 2,alignx right");
		
		hashMd5DynLbl = new JTextField((String) null);
		hashMd5DynLbl.setPreferredSize(new Dimension(18, 25));
		hashMd5DynLbl.setForeground(Color.DARK_GRAY);
		panel.add(hashMd5DynLbl, "cell 7 2,growx");
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setBackground(Color.GRAY);
		panel.add(separator_1, "cell 0 3 8 1,growx");
		
		lblLastAccess = new JLabel("Last Access:");
		lblLastAccess.setForeground(Color.LIGHT_GRAY);
		lblLastAccess.setFont(new Font("Tahoma", Font.BOLD, 14));
		panel.add(lblLastAccess, "cell 0 4,alignx right,aligny center");
		
		dynLblLastAccess = new JLabel(" 1970/01/01 - 01:00:00");
		dynLblLastAccess.setForeground(new Color(176, 196, 222));
		panel.add(dynLblLastAccess, "cell 1 4,alignx left,aligny center");
		
		lblCreatedOn = new JLabel("Created on:");
		lblCreatedOn.setForeground(Color.LIGHT_GRAY);
		lblCreatedOn.setFont(new Font("Tahoma", Font.BOLD, 14));
		panel.add(lblCreatedOn, "cell 2 4,alignx right,aligny center");
		
		dynLblCreationDate = new JLabel(" 1970/01/01 - 01:00:00");
		dynLblCreationDate.setForeground(new Color(176, 196, 222));
		panel.add(dynLblCreationDate, "cell 3 4 2 1,alignx left,aligny center");
		
		lblLastModified = new JLabel("Last Change:");
		lblLastModified.setForeground(Color.LIGHT_GRAY);
		lblLastModified.setFont(new Font("Tahoma", Font.BOLD, 14));
		panel.add(lblLastModified, "cell 5 4,alignx right,aligny center");
		
		dynLblLastChange = new JLabel((String) null);
		dynLblLastChange.setForeground(new Color(176, 196, 222));
		panel.add(dynLblLastChange, "cell 6 4 2 1,alignx left,aligny center");
		
		footerPanel = new JPanel();
		footerPanel.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		footerPanel.setForeground(Color.WHITE);
		footerPanel.setBackground(Color.GRAY);
		panel.add(footerPanel, "cell 0 5 8 1,grow");
		footerPanel.setLayout(new MigLayout("", "[1px,grow][1px,grow][1px,grow][1px,grow]", "[1px,grow]"));
		
		dynLblCanRead = new JLabel((String) null);
		dynLblCanRead.setForeground(Color.WHITE);
		dynLblCanRead.setFont(new Font("Segoe-UI", Font.BOLD, 14));
		footerPanel.add(dynLblCanRead, "cell 0 0,alignx center,growy");
		
		dynLblCanWrite = new JLabel((String) null);
		dynLblCanWrite.setForeground(Color.WHITE);
		dynLblCanWrite.setFont(new Font("Segoe-UI", Font.BOLD, 14));
		footerPanel.add(dynLblCanWrite, "cell 1 0,alignx center");
		
		dynLblCanExecute = new JLabel((String) null);
		dynLblCanExecute.setForeground(Color.WHITE);
		dynLblCanExecute.setFont(new Font("Segoe-UI", Font.BOLD, 14));
		footerPanel.add(dynLblCanExecute, "cell 2 0,alignx center");
		
		dynLblIsHidden = new JLabel((String) null);
		dynLblIsHidden.setForeground(Color.WHITE);
		dynLblIsHidden.setFont(new Font("Segoe-UI", Font.BOLD, 14));
		footerPanel.add(dynLblIsHidden, "cell 3 0,alignx center");
		
		initializeValues();
		
		adjustLabelsLength();
		
		addHandlers();
	}
	
	public void addHandlers() {
		viewInFileSystemButton.addActionListener((e) -> {
			new Thread(new Runnable() {
				public void run() {
//					File loadedFileParent = FileVarious.getParent(fileUnderAnalysis);
					GuiUtilsExtended.openInFileSystem(fileUnderAnalysis);
				}
			}).start();
		});
	}
	
	public void adjustLabelsLength() {
		dynLblFileName.setPreferredSize(new Dimension(18, 25));
		dynLblPath.setPreferredSize(new Dimension(18, 25));
	}
	
	public void initializeValues() {
		
		BasicFileAttributes attr;
		try {
			attr = Files.readAttributes(fileUnderAnalysis.toPath(), BasicFileAttributes.class);
			dynLblCreationDate.setText(GeneralConfig.GENERIC_FORMATTER.format(attr.creationTime().toMillis()));
			dynLblLastAccess.setText(GeneralConfig.GENERIC_FORMATTER.format(attr.lastAccessTime().toMillis()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		dynLblFileName.setText(fileUnderAnalysis.getName());
		dynLblPath.setText(fileUnderAnalysis.getAbsolutePath());
		dynLblSize.setText(ConversionUtils.coolFileSize(fileUnderAnalysis));
		dynLblLastChange.setText(" " + GeneralConfig.GENERIC_FORMATTER.format(new Date(fileUnderAnalysis.lastModified())));
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.getIconByFile(fileUnderAnalysis), this);
		updateFooter();
		try {
			if (fileUnderAnalysis != null) {
				if (fileUnderAnalysis.isFile()) {
					hashMd5DynLbl.setText(ChecksumHasher.getFileHashBuffered(fileUnderAnalysis.getAbsolutePath(), HashingFunction.MD5));
				} else {
					hashMd5DynLbl.setText("Not a File... cannot hash!");
				}
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update labels with informations about the given file
	 * @param fileToAnalyze
	 */
	public void refreshValues(File fileToAnalyze) {
		if(fileToAnalyze == null || !fileToAnalyze.exists()) {
			return;
		}
		this.fileUnderAnalysis = fileToAnalyze;
		GuiUtilsExtended.setFrameIcon(MyFileView.getExtIcon(fileUnderAnalysis), this);
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.getIconByFile(fileUnderAnalysis), this);
		setTitle(fileUnderAnalysis.getName());
		adjustLabelsLength();
		this.initializeValues();
		this.repaint();
	}
	
	public void updateFooter() {
		
		// CANREAD
		if (fileUnderAnalysis.canRead()) {
			dynLblCanRead.setText("Can Read");
			dynLblCanRead.setForeground(defaultGreen);
		}else {
			dynLblCanRead.setText("Cannot Read");
			dynLblCanRead.setForeground(defaultRed);
		}
		
		// CANWRITE
		if (fileUnderAnalysis.canWrite()) {
			dynLblCanWrite.setText("Can Write");
			dynLblCanWrite.setForeground(defaultGreen);
		}else {
			dynLblCanWrite.setText("Cannot Write");
			dynLblCanWrite.setForeground(defaultRed);
		}
		
		// CAN EXECUTE
		if (fileUnderAnalysis.canExecute()) {
			dynLblCanExecute.setText("Can Execute");
			dynLblCanExecute.setForeground(defaultGreen);
		}else {
			dynLblCanExecute.setText("Cannot Execute");
			dynLblCanExecute.setForeground(defaultRed);
		}
		
		// HIDDEN FILE
		if (fileUnderAnalysis.isHidden()) {
			dynLblIsHidden.setText("Hidden File");
			dynLblIsHidden.setForeground(defaultBlue);
		}else {
			dynLblIsHidden.setText("Visible File");
			dynLblIsHidden.setForeground(defaultGreen);
		}
		
	}
}
