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
/****************************************************************************************************************
* Developer: Minhas Kamal(BSSE-0509, IIT, DU)																	*
* Date: 16-Jan-2014																								*
* Modified: 31-Dec-2014																							*
****************************************************************************************************************/

package com.minhasKamal.ultimateCalculator.mainFrame;

import java.awt.Dimension;
import java.awt.event.KeyListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.UIManager;

import com.minhasKamal.ultimateCalculator.CalculatorMainApp;


/**
 * This is the super class of all calculator frames, which contains all the common features.
 * 
 * @author Minhas Kamal
 */
@SuppressWarnings("serial")
public class UltimateCalculatorFrameGui extends JFrame {
	//**
	// Variable Declaration 																	#*******D*******#
	//**
	JMenuBar jMenuBarMain;
	JMenu jMenuMode, jMenuHelp, jMenuAccessories;
    JCheckBoxMenuItem jCBItemMode[];
    JSeparator separator;
    JMenuItem jMenuItemInstruction;
    
    //other variables
    int modes;
	// End of Variable Declaration 																#_______D_______#

	/***##Constructor##***/
	public UltimateCalculatorFrameGui() {
		modes=7;
		
		initialComponent();
		
		addCloseListener();
	}
	
	private void addCloseListener() {
		// DEFAULT CLOSE OPERATION
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
	            
				CalculatorMainApp.active = false;
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				
				// remove key listeners
				try {
					for (KeyListener key : getKeyListeners()) {
						removeKeyListener(key);
					}
				} catch (Exception e) {
				}
				
	        	dispose();
		    }
		});
	}
	
	/**
	 * Method for Initializing all the GUI variables and placing them all to specific space on 
	 * the frame. It also specifies criteria of the main frame.
	 */
	private void initialComponent() {
		//**
		// Initialization 																		#*******I*******#
		//**
		jMenuBarMain = new JMenuBar();
		jMenuMode = new JMenu(); 
		jMenuHelp = new JMenu(); 
		jMenuAccessories = new JMenu();
		jCBItemMode = new JCheckBoxMenuItem[modes];	//number of modes
		separator = new JSeparator();
		jMenuItemInstruction =  new JMenuItem();
		// End of Initialization																#_______I_______#

		//**
		// Setting Bounds and Attributes of the Elements 										#*******S*******#
		//**
		//Menus
		jMenuMode.setText("Mode  ");
		jMenuHelp.setText("Help  ");
		jMenuAccessories.setText("Accessories");
		
		//Menu Items
		for(int i=0; i<modes; i++) {
			jCBItemMode[i] = new JCheckBoxMenuItem();
		}
		jCBItemMode[0].setText("Simple"); jCBItemMode[1].setText("Advanced"); 
			jCBItemMode[2].setText("Base"); jCBItemMode[3].setText("Equation");
			jCBItemMode[4].setText("Unit Converter"); jCBItemMode[5].setText("Date Calculator");
			jCBItemMode[6].setText("Prime Number");
		
		jMenuItemInstruction.setText("Instruction"); 
		jMenuItemInstruction.setIcon(new ImageIcon(getClass().getResource("/res/imgs/InstructionIcon.png")));
		// End of Setting Bounds and Attributes 												#_______S_______#

		//**
		// Adding Components 																	#*******A*******#
		//**
		//adding menus to the menu bar
		jMenuBarMain.add(jMenuMode);
		jMenuBarMain.add(jMenuHelp);
		
		//adding menu items
		for(int i=0; i<4; i++) {
			jMenuMode.add(jCBItemMode[i]);	//adding modes
		}
		jMenuMode.add(separator);
		jMenuMode.add(jMenuAccessories);
		
		for(int i=4; i<modes; i++) {
			jMenuAccessories.add(jCBItemMode[i]);	//adding modes
		}
		jMenuHelp.add(jMenuItemInstruction);
		// End of Adding Components 															#_______A_______#

		//**Setting Criterion of the Frame**//
		setJMenuBar(jMenuBarMain);
		setTitle("Ultimate Calculator");
		setIconImage(new ImageIcon(getClass().getResource("/res/imgs/UltimateCalculatorIcon.png")).getImage());
//		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		getContentPane().setLayout(null);
    	setResizable(false);
    	setMinimumSize(new Dimension(250, 350));
		setLocationRelativeTo(CalculatorMainApp.parentCallerFrame);
	}

	/********* Main Method *********/
	public static void main(String args[]) {
		/*// Set the NIMBUS look and feel //*/
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception ex) {
			// do nothing if operation is unsuccessful
		}

		/* Create and display the form */
		UltimateCalculatorFrameGui gui = new UltimateCalculatorFrameGui();
		gui.setVisible(true);
	}
}
