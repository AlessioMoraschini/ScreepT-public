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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.UIManager;

import com.minhasKamal.ultimateCalculator.calculators.advancedCalculator.AdvancedCalculator;
import com.minhasKamal.ultimateCalculator.calculators.baseCalculator.BaseCalculator;
import com.minhasKamal.ultimateCalculator.calculators.dateCalculator.DateCalculator;
import com.minhasKamal.ultimateCalculator.calculators.equationCalculator.EquationCalculator;
import com.minhasKamal.ultimateCalculator.calculators.primeNumberHunter.PrimeNumberHunter;
import com.minhasKamal.ultimateCalculator.calculators.simpleCalculator.SimpleCalculator;
import com.minhasKamal.ultimateCalculator.calculators.unitConverter.UnitConverter;
import com.minhasKamal.ultimateCalculator.help.instruction.Instruction;


/**
 * This is the super class of all calculator frames, which contains all the common features.
 * 
 * @author Minhas Kamal
 */
public class UltimateCalculatorFrame{
	// GUI Declaration
	public UltimateCalculatorFrameGui gui;
	
	//**
	// Variable Declaration 																	#*******D*******#
	//**
    protected JCheckBoxMenuItem jCBItemMode[];
    private JMenuItem jMenuItemInstruction;
    
    
    //other variables
    private int modes;
    protected String instruction;
	// End of Variable Declaration 																#_______D_______#

	/***##Constructor##***/
	public UltimateCalculatorFrame() {
		instruction="";
		
		initialComponent();
		gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	
	/**
	 * Method for Initializing all the GUI variables and placing them all to specific space on 
	 * the frame. It also specifies criteria of the main frame.
	 */
	private void initialComponent() {
		// GUI Initialization
		gui = new UltimateCalculatorFrameGui();
		gui.setVisible(true);
		
		//**
		// Assignation 																			#*******A*******#
		//**
		jCBItemMode = gui.jCBItemMode;	//number of modes
		jMenuItemInstruction =  gui.jMenuItemInstruction;
		
		modes = gui.modes;
		// End of Assignation																	#_______A_______#

		//**
		// Adding Action Events & Other Attributes												#*******AA*******#
		//**
		for(int i=0; i<modes; i++) {
			jCBItemMode[i].addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) {
	            	jCBItemModeActionPerformed(evt);
	            }
	        });
		}
		
		jMenuItemInstruction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            	jMenuItemInstructionActionPerformed(evt);
            }
        });
		
		// End of Adding Action Events & Other Attributes										#_______AA_______#
	}

	//**
	// Action Events 																			#*******AE*******#
	//**
	//menu items
  	private void jCBItemModeActionPerformed(ActionEvent evt){
  		if(evt.getActionCommand()=="Simple"){
  			new SimpleCalculator();
  		}else if(evt.getActionCommand()=="Advanced"){
  			new AdvancedCalculator();
  		}else if(evt.getActionCommand()=="Base"){
  			new BaseCalculator();
  		}else if(evt.getActionCommand()=="Equation"){
  			new EquationCalculator();
  		}else if(evt.getActionCommand()=="Unit Converter"){
  			new UnitConverter();
  		}else if(evt.getActionCommand()=="Date Calculator"){
  			new DateCalculator();
  		}else if(evt.getActionCommand()=="Prime Number"){
  			new PrimeNumberHunter();
  		}
  		
  		gui.dispose();
  	}
  	private void jMenuItemInstructionActionPerformed(ActionEvent evt){
  		Instruction instructionsGui = new Instruction(instruction);
  		instructionsGui.setVisible(true);
  		instructionsGui.setLocationRelativeTo(gui);
  	}
	// End of Action Events 																	#_______AE_______#

	//**
	// Auxiliary Methods 																		#*******AM*******#
	//**
	
	// End of Auxiliary Methods 																#_______AM_______#
	
	//**
	// Unimplemented Methods 																	#*******UM*******#
	//**
	
	// End of Unimplemented Methods 															#_______UM_______#
	
	
	/********* Main Method *********/
	public static void main(String args[]) {
		/*// Set the NIMBUS look and feel //*/
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception ex) {
			// do nothing if operation is unsuccessful
		}

		/* Create */
		new UltimateCalculatorFrame();
	}
}
