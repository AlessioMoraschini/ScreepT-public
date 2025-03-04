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
package gui.commons.frameutils.html;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import gui.commons.ButtonUtils.ButtonDefaults;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import resources.SoundsConfigurator;
import resources.SoundsManagerExtended;
import various.common.light.gui.dialogs.msg.JOptionHelper;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Font;

public class HeaderPanelSearch extends JPanel{
	private static final long serialVersionUID = 7734630242175430411L;

	public static String defaultJsBackColorSelection = "yellow";
	
	private JButton btnGoBack;
	private JButton btnGoNext;
	private JButton btnReload;
	private JButton btnOpenInBrowser;
	
	public HeaderPanelSearch headerPaneThis;
	private JavaFxPanelHtml panelFxHtml;
	private JButton btnIndex;
	private JButton btnFind;
	private JTextField textFieldSearch;
	
	public HeaderPanelSearch(JavaFxPanelHtml panel) {
		
		panelFxHtml = panel;
		headerPaneThis = this;
		
		setBorder(new LineBorder(Color.white, 2));
		setBackground(Color.BLACK);
		setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+"", "[50px][50px][50px][50px][50px][grow][50px]", "[30px,grow]"));
		
		btnGoBack = new JButton("Back");
		btnGoBack.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnGoBack.setIgnoreRepaint(true);
		btnGoBack.setBackground(new Color(169, 169, 169));
		btnGoBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnGoBack.setIcon(new ImageIcon(IconsPathConfigurator.ICON_UNDO));
		add(btnGoBack, "cell 0 0,grow");
		
		btnGoNext = new JButton("Forward");
		btnGoNext.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnGoNext.setIgnoreRepaint(true);
		btnGoNext.setBackground(new Color(169, 169, 169));
		btnGoNext.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnGoNext.setIcon(new ImageIcon(IconsPathConfigurator.ICON_REDO));
		add(btnGoNext, "cell 1 0,grow");
		
		btnReload = new JButton("Reload");
		btnReload.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnReload.setIgnoreRepaint(true);
		btnReload.setBackground(new Color(119, 136, 153));
		btnReload.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnReload.setIcon(new ImageIcon(IconsPathConfigurator.ICON_RELOAD_CONF));
		add(btnReload, "cell 2 0,grow");
		
		btnOpenInBrowser = new JButton("View in Web Browser");
		btnOpenInBrowser.setForeground(Color.WHITE);
		btnOpenInBrowser.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnOpenInBrowser.setIgnoreRepaint(true);
		btnOpenInBrowser.setBackground(Color.DARK_GRAY);
		btnOpenInBrowser.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		add(btnOpenInBrowser, "cell 4 0,grow");
		
		btnIndex = new JButton("Index");
		btnIndex.setForeground(Color.WHITE);
		btnIndex.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnIndex.setIgnoreRepaint(true);
		btnIndex.setBackground(Color.DARK_GRAY);
		btnIndex.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		add(btnIndex, "cell 3 0,grow");
		
		textFieldSearch = new JTextField();
		textFieldSearch.setForeground(Color.DARK_GRAY);
		textFieldSearch.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 17));
		textFieldSearch.setHorizontalAlignment(SwingConstants.CENTER);
		add(textFieldSearch, "cell 5 0,grow");
		textFieldSearch.setColumns(10);
		
		btnFind = new JButton("Find");
		btnFind.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnFind.setBackground(new Color(169, 169, 169));
		btnFind.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnFind.setIcon(new ImageIcon(IconsPathConfigurator.ICON_SEARCH));
		add(btnFind, "cell 6 0,grow");
		
		addHandlersFx();
	}
	
	private void addHandlersFx(){
		
		// BACK IN HISTORY
		btnGoBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panelFxHtml.goBack();
			}
		});
		
		// FORWARD IN HISTORY
		btnGoNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panelFxHtml.goNext();
			}
		});
		
		// RELOAD PAGE
		btnReload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panelFxHtml.reload();
			}
		});
		
		// GOTO INDEX
		btnIndex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panelFxHtml.goToIndex();
			}
		});
		
		// OPEN IN BROWSER
		btnOpenInBrowser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!panelFxHtml.openInBrowser()) {
					new JOptionHelper(panelFxHtml).error("Cannot open in default browser, some error occurred :(", "Error opening in browser");
				}
			}
		});
		
		// SEARCH BUTTON
		btnFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!textFieldSearch.getText().equals("")) {
					panelFxHtml.findTextNHighLight(textFieldSearch.getText());
				}else {
					SoundsManagerExtended.playSound(SoundsConfigurator.CLICK_1, SoundsConfigurator.DEFAULT_EFFECTS_VOLUME);
				}
			}
		});
		
		textFieldSearch.getDocument().addDocumentListener(new DocumentListener() {
			
			public void removeUpdate(DocumentEvent e) {
				panelFxHtml.deselectAll();
			}
			public void insertUpdate(DocumentEvent e) {
				panelFxHtml.deselectAll();
			}
			public void changedUpdate(DocumentEvent e) {
				panelFxHtml.deselectAll();
			}
		});
		
		ButtonDefaults.setDefaultOnFocus(btnFind, headerPaneThis);
		ButtonDefaults.setDefaultOnFocus(btnGoBack, headerPaneThis);
		ButtonDefaults.setDefaultOnFocus(btnGoNext, headerPaneThis);
		ButtonDefaults.setDefaultOnFocus(btnOpenInBrowser, headerPaneThis);
		ButtonDefaults.setDefaultOnFocus(btnIndex, headerPaneThis);
		ButtonDefaults.setDefaultOnFocus(btnReload, headerPaneThis);
	}
	
}
