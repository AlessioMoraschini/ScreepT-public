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

import javax.swing.JFrame;
import javax.swing.JTextArea;

import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;

import frameutils.frame.arch.ParentFrame;
import frameutils.utils.GuiUtilsExtended;

import java.awt.SystemColor;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import utils.StringWorkerExtended;
import various.common.light.om.EscapeType;
import various.common.light.om.SelectionDtoFull;

import javax.swing.JLabel;

import java.awt.Font;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import java.awt.Dimension;

public class EscapeReplacerFrame extends ParentFrame {
	private static final long serialVersionUID = 2122717575417614868L;

	static Logger logger = Logger.getLogger(EscapeReplacerFrame.class);
	
	public static final String tooltipTryEscapeToSpec = "Try conversion from escape sequence to special characters in text field";
	public static final String tooltipTrySpecialToEscape = "Try conversion from special characters to escape sequence in text field";
	public static final String tooltipReplaceEscapesToSpec = "Replace all escape sequences to special characters in textArea selected text(all if nothing selected)";
	public static final String tooltipReplaceSpecToEscapes = "Replace all special characters to escape sequences in textArea selected text(all if nothing selected)";
		
	public static boolean active = false;
	
	private JLabel lblEscapesHeader;
	private JPanel panel;
	private JComboBox<String> comboEscapeType;
	private JButton btnConvertSpecialToEsc;
	private JButton buttonInvoker;
	public JTextArea outputTextArea;
	private JButton btnConvertEscapesTo;
	private JTextField textFieldTry;
	private JButton btnTryEscapeTo;
	private JButton btnTrySpecialTo;
	private JFrame parentFrame;
	Runnable comboClickAction;
	
	// MAIN CONSTRUCTOR //
	public EscapeReplacerFrame(JFrame parent, JTextArea outputText, JButton buttonInvoke, Runnable comboClickAction) {
		
		active = true;
		
		this.comboClickAction = comboClickAction;
		
		GuiUtilsExtended.centerComponent(this);
		
		parentFrame = parent;
		outputTextArea = outputText;
		buttonInvoker = buttonInvoke;
		
		getContentPane().setBackground(Color.DARK_GRAY);
		getContentPane().setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+"", "[250px][220px,grow]", "[30px][30px,grow][20px][20px,grow]"));
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_CONVERT_ESCAPES, this);
		setLocationRelativeTo(parentFrame);
		
		panel = new JPanel();
		panel.setForeground(Color.WHITE);
		panel.setBorder(new LineBorder(Color.GRAY, 2, true));
		panel.setBackground(Color.BLACK);
		getContentPane().add(panel, "cell 0 0 2 1,growx,aligny top");
		panel.setLayout(new MigLayout("", "[][grow][]", "[]"));
		
		lblEscapesHeader = new JLabel("Escape sequence Replacer");
		lblEscapesHeader.setHorizontalAlignment(SwingConstants.CENTER);
		lblEscapesHeader.setForeground(Color.WHITE);
		lblEscapesHeader.setFont(new Font("Lithos Pro Regular", Font.BOLD, 15));
		lblEscapesHeader.setBackground(SystemColor.activeCaptionBorder);
		panel.add(lblEscapesHeader, "cell 1 0,alignx center");
		
		textFieldTry = new JTextField();
		textFieldTry.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		getContentPane().add(textFieldTry, "cell 0 1,growx");
		textFieldTry.setColumns(10);
		
		comboEscapeType = new JComboBox<String>(EscapeType.getTypesString());
//		comboEscapeType = new JComboBox<>();
		comboEscapeType.setPreferredSize(new Dimension(33, 34));
		comboEscapeType.setMinimumSize(new Dimension(33, 28));
		comboEscapeType.setSelectedItem(EscapeType.HTML_4);
		comboEscapeType.setFont(new Font("SansSerif", Font.PLAIN, 14));
		comboEscapeType.setCursor(GuiUtilsExtended.CURSOR_HAND);
		getContentPane().add(comboEscapeType, "cell 1 1,growx");
		
		btnTrySpecialTo = new JButton("Try special To escape");
		btnTrySpecialTo.setMaximumSize(new Dimension(250, 30));
		btnTrySpecialTo.setMinimumSize(new Dimension(148, 25));
		btnTrySpecialTo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		btnTrySpecialTo.setToolTipText(tooltipTrySpecialToEscape);
		btnTrySpecialTo.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnTrySpecialTo.setBackground(new Color(169, 169, 169));
		getContentPane().add(btnTrySpecialTo, "cell 0 2,growx,aligny center");
		
		btnConvertSpecialToEsc = new JButton("Convert Special to escapes");
		btnConvertSpecialToEsc.setMinimumSize(new Dimension(178, 30));
		btnConvertSpecialToEsc.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnConvertSpecialToEsc.setToolTipText(tooltipReplaceSpecToEscapes);
		btnConvertSpecialToEsc.setFont(new Font("Segoe UI", Font.BOLD, 13));
		btnConvertSpecialToEsc.setBackground(Color.GRAY);
		getContentPane().add(btnConvertSpecialToEsc, "cell 1 2,growx");
		
		btnTryEscapeTo = new JButton("Try escape To special");
		btnTryEscapeTo.setMaximumSize(new Dimension(250, 30));
		btnTryEscapeTo.setMinimumSize(new Dimension(148, 25));
		btnTryEscapeTo.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnTryEscapeTo.setToolTipText(tooltipTryEscapeToSpec);
		btnTryEscapeTo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		btnTryEscapeTo.setBackground(new Color(169, 169, 169));
		getContentPane().add(btnTryEscapeTo, "cell 0 3,growx,aligny top");
		
		btnConvertEscapesTo = new JButton("Convert escapes to Special");
		btnConvertEscapesTo.setMinimumSize(new Dimension(178, 30));
		btnConvertEscapesTo.setFont(new Font("Segoe UI", Font.BOLD, 13));
		btnConvertEscapesTo.setToolTipText(tooltipReplaceEscapesToSpec);
		btnConvertEscapesTo.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnConvertEscapesTo.setBackground(Color.GRAY);
		getContentPane().add(btnConvertEscapesTo, "cell 1 3,growx,aligny top");
		
		addHandlers();
		
	}
	
	private void addHandlers() {
		
		// DEFAULT CLOSE ACTIONS 
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		            
	        	active = false;
	        	buttonInvoker.setEnabled(true);
	        	
				logger.debug("Closing "+getClass().getName()+" - Configuration resetted.\n");
	        	dispose();
		    }
		});
		
		// TRY ESCAPE TO SPECIAL CONVERSION
		btnTryEscapeTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String tryString = textFieldTry.getText();
				tryString = StringWorkerExtended.replaceEscapesWithSpecials(tryString, getSelectedEscapeType());
				textFieldTry.setText(tryString);
				GuiUtilsExtended.selectAllText(textFieldTry);
			}
		});
		
		// TRY SPECIAL TO ESCAPE CONVERSION
		btnTrySpecialTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String tryString = textFieldTry.getText();
				// first replace back already escaped sequences to avoid escaping escape text (&grave -> amperstand would be escaped too)
				tryString = StringWorkerExtended.replaceEscapesWithSpecials(tryString, getSelectedEscapeType());
				tryString = StringWorkerExtended.replaceSpecialWithEscapes(tryString, getSelectedEscapeType());
				textFieldTry.setText(tryString);
				GuiUtilsExtended.selectAllText(textFieldTry);
			}
		});
		
		// REPLACE SPECIALS WITH ESCAPED IN TEXT AREA SELECTED/ALL
		btnConvertSpecialToEsc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cursorWaiting();
				// first replace back already escaped sequences to avoid escaping escape text (&grave -> amperstand would be escaped too)
				SelectionDtoFull dto = replaceEscapedWithSpecTextSelected(getSelectedEscapeType(), null, false);
				dto = replaceSpecWithEscapedTextSelected(getSelectedEscapeType(), dto, true);
				dto.applySelectionDTO(outputTextArea);
				cursorFinished();
			}
		});
		
		// REPLACE ESCAPED WITH SPECIALS IN TEXT AREA SELECTED/ALL
		btnConvertEscapesTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cursorWaiting();
				SelectionDtoFull dto = replaceEscapedWithSpecTextSelected(getSelectedEscapeType(), null, true);
				dto.applySelectionDTO(outputTextArea);
				cursorFinished();
			}
		});
		
		// ESCAPE MEMORY IN CONFIGURATION 
		comboEscapeType.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (comboClickAction != null) {
					SwingUtilities.invokeLater(comboClickAction);
				}
			}
		});
	}
	
	public String getCurrentComboLang() {
		try {
			return (String) comboEscapeType.getSelectedItem();
		} catch (Exception e) {
			return "";
		}
	}

	public boolean initComboLang(String language) {
		try {
			comboEscapeType.setSelectedItem(language);
			comboEscapeType.revalidate();
			comboEscapeType.repaint();
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	// USEFUL METHODS //
	
	private void cursorWaiting() {
		outputTextArea.setCursor(GuiUtilsExtended.CURSOR_WAIT);
		textFieldTry.setCursor(GuiUtilsExtended.CURSOR_WAIT);
		panel.setCursor(GuiUtilsExtended.CURSOR_WAIT);
		btnConvertEscapesTo.setCursor(GuiUtilsExtended.CURSOR_WAIT);
		btnConvertSpecialToEsc.setCursor(GuiUtilsExtended.CURSOR_WAIT);
		
		btnConvertEscapesTo.setEnabled(false);
		btnConvertSpecialToEsc.setEnabled(false);
	}
	
	private void cursorFinished() {
		outputTextArea.setCursor(GuiUtilsExtended.CURSOR_TEXT);
		textFieldTry.setCursor(GuiUtilsExtended.CURSOR_TEXT);
		panel.setCursor(GuiUtilsExtended.CURSOR_DEFAULT);
		btnConvertEscapesTo.setCursor(GuiUtilsExtended.CURSOR_HAND);
		btnConvertSpecialToEsc.setCursor(GuiUtilsExtended.CURSOR_HAND);
		
		btnConvertEscapesTo.setEnabled(true);
		btnConvertSpecialToEsc.setEnabled(true);
	}
	
	private EscapeType getSelectedEscapeType() {
		String selectedType = (String)comboEscapeType.getSelectedItem();
		return EscapeType.valueOf(selectedType);
	}
	
	private SelectionDtoFull replaceSpecWithEscapedTextSelected(EscapeType type, SelectionDtoFull selectionTextArea, boolean makeNewSelectedIfNothingSelected) {
		SelectionDtoFull selectionTxtArea = selectionTextArea == null ? new SelectionDtoFull(outputTextArea) : selectionTextArea;
		String escaped = "";
		
		if(selectionTxtArea.selected) {
			escaped = StringWorkerExtended.replaceSpecialWithEscapes(selectionTxtArea.selection, type);
		}else {
			escaped = StringWorkerExtended.replaceSpecialWithEscapes(selectionTxtArea.text, type);
		}

		selectionTxtArea.replaceSelection(escaped, makeNewSelectedIfNothingSelected, false);
		return selectionTxtArea;
		
	}
	
	private SelectionDtoFull replaceEscapedWithSpecTextSelected(EscapeType type, SelectionDtoFull selectionTextArea, boolean makeNewSelectedIfNothingSelected) {
		SelectionDtoFull selectionTxtArea = selectionTextArea == null ? new SelectionDtoFull(outputTextArea) : selectionTextArea;
		String escapedBack = "";
		
		if(selectionTxtArea.selected) {
			escapedBack = StringWorkerExtended.replaceEscapesWithSpecials(selectionTxtArea.selection, type);
		}else {
			escapedBack = StringWorkerExtended.replaceEscapesWithSpecials(selectionTxtArea.text, type);
		}

		selectionTxtArea.replaceSelection(escapedBack, makeNewSelectedIfNothingSelected, false);
		return selectionTxtArea;
	}

	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(700, 200);
	}
}
