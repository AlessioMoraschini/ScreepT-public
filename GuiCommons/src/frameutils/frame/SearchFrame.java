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
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Cursor;

import net.miginfocom.swing.MigLayout;
import om.SelectionDtoFull;
import resources.SoundsConfigurator;
import resources.SoundsManagerExtended;

import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import mouseutils.MovePanelMouseListener;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;
import java.awt.Font;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.border.LineBorder;
import javax.swing.JLabel;

public class SearchFrame extends JFrame{
	private static final long serialVersionUID = -7775810414279893815L;

	// TextFileWriter logger creation
	static Logger logger = Logger.getLogger(SearchFrame.class);
	
	public static final String SEARCH_PLACEHOLDER = "Search word...";
	public static final String REPLACE_PLACEHOLDER = "Replace selected";
		
	public JTextArea textSearchArea;
	public JTextField searchTextField;
	private JTextField txtReplaceSelection;
	private JButton btnChangeText;
	private JButton btnNext;
	private JButton btnPrev;
	private JFrame thisFrame;
	private JCheckBox chckbxCaseSensitive;
	private JPanel tabSearch;
	
	private String textTemp;
	private boolean textChanged = true;
	private boolean end = false;
	private int indexStart = 0;
	
	public volatile boolean active = false;
	private JPanel headerPanel;
	private JLabel lblTextSearch;
	private JButton btnClose;
	
	public SearchFrame(boolean alwaysOnTop, JTextArea targetSearchArea) {
		
		logger.info(this.getClass().getName() + " Starting...");
		
		thisFrame = this;
		active = true;
		
		this.setUndecorated(true);
		this.setAlwaysOnTop(alwaysOnTop);
		this.setTitle("Text Search");
		MovePanelMouseListener.applyMoveListener(this);
		
		getContentPane().setBackground(Color.GRAY);
		
		tabSearch = new JPanel();
		tabSearch.setBorder(new LineBorder(Color.LIGHT_GRAY, 2));
		tabSearch.setLayout(new MigLayout("", "[100px,grow][100px,grow][100px,grow][100px][100px][120px,grow]", "[:35px:40px][50px,grow][50px]"));
		tabSearch.setBackground(Color.DARK_GRAY);
		getContentPane().add(tabSearch);
		
		textSearchArea = targetSearchArea;
		textSearchArea.requestFocus();
		textSearchArea.setCaret(new DefaultCaret() {
			private static final long serialVersionUID = 717425793296609783L;

			@Override
			public void setSelectionVisible(boolean visible) {
				super.setSelectionVisible(true);
			}
		});
		
		headerPanel = new JPanel();
		headerPanel.setForeground(Color.BLACK);
		headerPanel.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		headerPanel.setBackground(new Color(112, 128, 144));
		tabSearch.add(headerPanel, "cell 0 0 6 1,grow");
		headerPanel.setLayout(new MigLayout("", "[][grow][]", "[::25px,fill]"));
		
		lblTextSearch = new JLabel("Text Search");
		lblTextSearch.setHorizontalAlignment(SwingConstants.RIGHT);
		lblTextSearch.setForeground(new Color(255, 255, 255));
		lblTextSearch.setFont(new Font("Lithos Pro Regular", Font.BOLD, 20));
		lblTextSearch.setBackground(Color.LIGHT_GRAY);
		headerPanel.add(lblTextSearch, "cell 1 0,alignx center,aligny top");
		
		btnNext = new JButton("Next");
		btnNext.setMinimumSize(new Dimension(42, 35));
		btnNext.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnNext.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnNext.setBackground(new Color(119, 136, 153));
		tabSearch.add(btnNext, "cell 5 1,growx,aligny center");
		
		btnClose = new JButton("X");
		btnClose.setPreferredSize(new Dimension(35, 35));
		btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnClose.setMaximumSize(new Dimension(35, 35));
		btnClose.setMinimumSize(new Dimension(22, 25));
		btnClose.setBackground(new Color(255, 51, 51));
		btnClose.setFont(new Font("SansSerif", Font.BOLD, 11));
		headerPanel.add(btnClose, "cell 2 0,alignx right,aligny top");

		searchTextField = new JTextField();
		searchTextField.setMinimumSize(new Dimension(6, 35));
		searchTextField.setHorizontalAlignment(SwingConstants.CENTER);
		searchTextField.setText(SEARCH_PLACEHOLDER);
		searchTextField.setToolTipText("Search term");
		tabSearch.add(searchTextField, "cell 0 1 3 1,growx,aligny center");
		searchTextField.setColumns(10);
		
		btnChangeText = new JButton("Change text");
		btnChangeText.setToolTipText("Replace selected text");
		btnChangeText.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnChangeText.setMinimumSize(new Dimension(42, 35));
		btnChangeText.setFont(new Font("Segoe UI", Font.BOLD, 15));
		btnChangeText.setBackground(new Color(128, 128, 128));
		tabSearch.add(btnChangeText, "cell 3 1 2 1,growx,aligny center");
		
		txtReplaceSelection = new JTextField();
		txtReplaceSelection.setToolTipText("Replace Selected text");
		txtReplaceSelection.setText(REPLACE_PLACEHOLDER);
		txtReplaceSelection.setMinimumSize(new Dimension(6, 35));
		txtReplaceSelection.setHorizontalAlignment(SwingConstants.CENTER);
		txtReplaceSelection.setColumns(10);
		tabSearch.add(txtReplaceSelection, "cell 0 2 3 1,growx");

		chckbxCaseSensitive = new JCheckBox("Case Sensitive");
		chckbxCaseSensitive.setOpaque(false);;
		chckbxCaseSensitive.setFont(new Font("SansSerif", Font.PLAIN, 14));
		chckbxCaseSensitive.setForeground(Color.WHITE);
		tabSearch.add(chckbxCaseSensitive, "cell 3 2 2 1,alignx center,aligny center");
		
		btnPrev = new JButton("Prev");
		btnPrev.setMinimumSize(new Dimension(42, 35));
		btnPrev.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnPrev.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnPrev.setBackground(new Color(119, 136, 153));
		tabSearch.add(btnPrev, "cell 5 2,growx,aligny center");

		adHandlersTextAreaSimple();
		
		addWindowListener(new WindowAdapter() {
		    public void windowGainedFocus(WindowEvent e) {
				btnNext.requestFocusInWindow();
		    }
		});
		
		logger.info(this.getClass().getName() + " Running");
	}
	
	private void adHandlersTextAreaSimple() {
		
		btnClose.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				textSearchArea.requestFocus();
				textSearchArea.setSelectionStart(textSearchArea.getSelectionStart());
				textSearchArea.setSelectionEnd(textSearchArea.getSelectionStart());
				dispatchEvent(new WindowEvent(thisFrame, WindowEvent.WINDOW_CLOSING));
			}
		});
		
		// window on close operations
		this.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		            active = false;
		        	logger.debug("Closing "+getClass().getName());
		        	dispose();
		    }
		});
		
		this.addFocusListener(new FocusListener() {
			
			public void focusLost(FocusEvent e) {
			}
			public void focusGained(FocusEvent e) {
				textChanged = true;
				textSearchArea.revalidate();
				loadText();
			}
		});
		
		// FIND PREV OCCURRENCE
		btnPrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchTxtArea(searchTextField.getText(), false, chckbxCaseSensitive.isSelected());
			}
		});
		
		btnPrev.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				getRootPane().setDefaultButton(btnPrev);
			}
		});
		
		// FIND NEXT OCCURRENCE
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchTxtArea(searchTextField.getText(), true, chckbxCaseSensitive.isSelected());
			}
		});
		
		btnNext.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				getRootPane().setDefaultButton(btnNext);
			}
		});
		
		btnNext.requestFocus();
		
		// REPLACE TEXT SELECTED
		btnChangeText.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				replaceTextSelected();
			}
		});
		
		btnChangeText.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				getRootPane().setDefaultButton(btnChangeText);
			}
		});
		
		textSearchArea.getDocument().addDocumentListener(new DocumentListener() {
			
			public void removeUpdate(DocumentEvent e) {
				mainTextAreaChanged();
			}
			
			public void insertUpdate(DocumentEvent e) {
				mainTextAreaChanged();
			}
			
			public void changedUpdate(DocumentEvent e) {
				mainTextAreaChanged();
			}
		});
		
		thisFrame.addFocusListener(new FocusListener() {
			
			public void focusLost(FocusEvent e) {
				searchTextField.setSelectionStart(0);
			}
			
			public void focusGained(FocusEvent e) {
				String selected = textSearchArea.getSelectedText();
				if(!searchTextField.getText().equals(selected) && thisFrame.hasFocus()) {
					searchTextField.setText(selected);
				}
			}
		});
		
		addPlacehoderActions(searchTextField, SEARCH_PLACEHOLDER);
		
		addPlacehoderActions(txtReplaceSelection, REPLACE_PLACEHOLDER);
		
	}
	
	private void mainTextAreaChanged() {
		textChanged = true;
		indexStart = textSearchArea.getSelectionStart();
	}
	
	private void replaceTextSelected() {
		SelectionDtoFull selectionTxtArea = new SelectionDtoFull(textSearchArea);
		int selStart = selectionTxtArea.first;
		String output = "";
		String center = "";
		
		if(textSearchArea.getSelectedText() != null) {
			if (chckbxCaseSensitive.isSelected()) {
				center = textSearchArea.getSelectedText().replaceAll(searchTextField.getText(),
						txtReplaceSelection.getText());
			}else {
				center = textSearchArea.getSelectedText().replaceAll("(?i)"+Pattern.quote(searchTextField.getText()),
						txtReplaceSelection.getText());
			}
			output = selectionTxtArea.text.concat(center).concat(selectionTxtArea.end);
			textSearchArea.setText(output);
			textSearchArea.requestFocus();
			textSearchArea.setSelectionStart(selStart);
			textSearchArea.setSelectionEnd(selStart+center.length());
		}else {
			
			textSearchArea.requestFocus();
			textSearchArea.setSelectionStart(selStart);
			textSearchArea.setSelectionEnd(selectionTxtArea.last);
			btnNext.doClick();
		}
		
		if(!center.equals("")) {
			textSearchArea.setSelectionEnd(selStart + center.length());
		}else {
			textSearchArea.setSelectionEnd(selectionTxtArea.last);
		}
		
	}
	
	public static void addPlacehoderActions(JTextField field, String placeholder) {
		// Search text
		field.addMouseListener(new MouseListener() {
			
			public void mouseReleased(MouseEvent e) {
			}
			public void mousePressed(MouseEvent e) {
				if(field.getText().equals(placeholder)) {
					field.setText("");
				}else {
					field.setSelectionStart(0);
					field.setSelectionEnd(field.getText().length());
				}
			}
			public void mouseExited(MouseEvent e) {
			}
			public void mouseEntered(MouseEvent e) {
			}
			public void mouseClicked(MouseEvent e) {
			}
		});
		
		field.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if(field.getText().equals(placeholder)) {
					field.setText("");
				}
			}
		});
		
		
	}
	
	// SEARCH FUNCTIONS //
	
	private void searchTxtArea(String match, boolean forward, boolean caseSensitive) {
		loadText();
		if(!caseSensitive) {
			textTemp = textTemp.toLowerCase();
			match = match.toLowerCase();
			textChanged = true;
		}
		
		int tempIndex = indexStart;
		if(forward) {
			tempIndex = textTemp.indexOf(match, indexStart);
		}else {
			tempIndex = textTemp.lastIndexOf(match, indexStart);
		}
		// if found update reference, else use 0
		indexStart = (tempIndex >= 0)? tempIndex : textSearchArea.getSelectionStart();
		
		if (tempIndex >= 0) {
			textSearchArea.setSelectionStart(indexStart);
			textSearchArea.setSelectionEnd(indexStart + match.length());
			end = false;
			SoundsManagerExtended.playSound(SoundsConfigurator.CLICK_2, null);
		}else {
			end = true;
			SoundsManagerExtended.playSound(SoundsConfigurator.CLICK_1, null);
		}
		
		ifEndGoToStart(match, forward);
	}
	
	private void ifEndGoToStart(String match, boolean forward) {
		int totTxtAreaLength = textSearchArea.getText().length();
		if(forward && end) {
			indexStart = 0;
		}else if(!forward && end) {
			indexStart = totTxtAreaLength;
		}else {
			if(forward) {
				indexStart++;
			}else if(!forward) {
				indexStart--;
			}
		}
	}
	
	private void loadText() {
		if (textChanged) {
			textTemp = textSearchArea.getText();
			textChanged = false;
		}
	}
	
}
