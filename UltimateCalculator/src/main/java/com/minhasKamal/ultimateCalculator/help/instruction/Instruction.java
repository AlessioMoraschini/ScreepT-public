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
/****************************************************************************************************************************
* Developer: Minhas Kamal(BSSE-0509, IIT, DU)																				*
* Date: 21-Dec-2013; Modified: 14-Jul-2014																					*
* Comment: This will make a window that will show a manual of how the software should be used.							 	*									
****************************************************************************************************************************/

package com.minhasKamal.ultimateCalculator.help.instruction;


import java.awt.event.ActionEvent;
import javax.swing.*;

import com.minhasKamal.ultimateCalculator.CalculatorMainApp;

import java.awt.SystemColor;
import java.awt.Dimension;
import java.awt.Font;

@SuppressWarnings("serial")
public class Instruction extends JFrame {
	//**
	// Variable Declaration 																				#*******D*******#
	//**
	private JPanel jPanelMain;
	private JScrollPane jScrollPane;
	private JTextArea jTextAreaNotification;
	private JButton jButtonOK;
	
	//operational variables
	private String instruction;
	// End of Variable Declaration 																			#_______D_______#

	/***##Constructor##***/
	public Instruction() {
		//write your message here
		this.instruction = "Instruction";
		
		initialComponent();
	}
	
	public Instruction(String instruction) {
		this.instruction = instruction;
		
		initialComponent();
		
		setAlwaysOnTop(true);
		setUndecorated(false);
	}

	
	/**
	 * Method for Initializing all the GUI variables, placing them all to specific space on the frame and adding action
	 * listener to them. Also specifies criteria of the main frame.
	 */
	private void initialComponent() {
		//**
		// Initialization 																					#*******I*******#
		//**
		jPanelMain=new JPanel();
		jScrollPane=new JScrollPane();
		jTextAreaNotification=new JTextArea();
		jButtonOK = new JButton();
		jButtonOK.setForeground(SystemColor.text);
		jButtonOK.setFont(new Font("SansSerif", Font.BOLD, 14));
		// End of Initialization																			#_______I_______#

		//**
		// Setting Bounds and Attributes of the Elements 													#*******S*******#
		//**
		jPanelMain.setBackground(SystemColor.windowBorder);
        jPanelMain.setBounds(0, 0, 350, 350);
        jPanelMain.setLayout(null);
		
		jScrollPane.setViewportView(jTextAreaNotification);
		jScrollPane.setBounds(10, 10, 330, 330);
		jScrollPane.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
		
		jTextAreaNotification.setText(instruction);
		jTextAreaNotification.setForeground(new java.awt.Color(102, 53, 0));
		jTextAreaNotification.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		jTextAreaNotification.setEditable(false);
		jTextAreaNotification.setLineWrap(true);
		jTextAreaNotification.setWrapStyleWord(true);
		jTextAreaNotification.setCaretPosition(0);
        
        jButtonOK.setText("OK");
        jButtonOK.setBounds(10, 360, 330, 30);
        jButtonOK.setBackground(SystemColor.inactiveCaptionText);
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });
        jButtonOK.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).
	    	put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER,0), "ENTER_pressed");
	    jButtonOK.getActionMap().put("ENTER_pressed", new AbstractAction() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	        	jButtonOKActionPerformed(evt);
	        }
	    });
		// End of Setting Bounds and Attributes 															#_______S_______#

		//**
		// Adding Components 																				#*******A*******#
		//**
        jPanelMain.add(jScrollPane); 
		// End of Adding Components 																		#_______A_______#

        
		//**Setting Criterion of the Frame**//
		setIconImage(new ImageIcon(getClass().getResource("/res/imgs/InstructionIcon.png")).getImage());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Instruction");
		setMinimumSize(new Dimension(355, 435));
		setLocationRelativeTo(CalculatorMainApp.parentCallerFrame);
		getContentPane().setLayout(null);
		setResizable(false);
		getContentPane().add(jPanelMain); getContentPane().add(jButtonOK);
		setFocusable(true);
	}

	//**
	// Action Events 																						#********AE*******#
	//**
	private void jButtonOKActionPerformed(ActionEvent evt){	
		dispose();
	}
	// End of Action Events 																				#________AE_______#

	/********* Main Method *********/
	public static void main(String args[]) {
		/*// Set the NIMBUS look and feel //*/
		try {
			javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception ex) {
			// do nothing if operation is unsuccessful
		}

		/* Create and display the form */
		Instruction gui = new Instruction();
		gui.setVisible(true);
	}

	//**
	// Auxiliary Methods 																					#********AM*******#
	//**

	// End of Auxiliary Methods 																			#________AM_______#
}
