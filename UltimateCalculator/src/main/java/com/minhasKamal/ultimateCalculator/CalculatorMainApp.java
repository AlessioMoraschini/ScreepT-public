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
/**
 * Developer: Minhas Kamal (IIT, DU, BSSE-0509)
 * Date: 17-Jan-2014
 * Comment: I want to make a calculator that contains all the criterion that a calculator can have.
 * 	I am working real hard to make this complex system.
**/


package com.minhasKamal.ultimateCalculator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.minhasKamal.ultimateCalculator.calculators.simpleCalculator.SimpleCalculator;


public class CalculatorMainApp extends Thread{
	
	public static volatile boolean active = false;
	
	public static SimpleCalculator calculatorGuiSingleton;
	private static CalculatorMainApp singletonInstance;
	
	public static boolean keyListenerSimpleAdded = false;
	public static boolean keyListenerBaseAdded = false;
	public static boolean keyListenerAdvancedAdded = false;
	
	public static JFrame parentCallerFrame;
	
	// constructor - for calling from others softwares
	public CalculatorMainApp(JFrame parentCallerFrame) {
		CalculatorMainApp.parentCallerFrame = parentCallerFrame;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				start();
			}
		});
	}
	
	// main - for direct launch
	public static void main(String args[]) {
		
		active = true;
		calculatorGuiSingleton = new SimpleCalculator();
		
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		active = true;
		calculatorGuiSingleton = new SimpleCalculator();
		calculatorGuiSingleton.gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	

	public static SimpleCalculator getCalculatorGuiSingleton() {
		return calculatorGuiSingleton;
	}

	public static boolean isKeyListenerSimpleAdded() {
		return keyListenerSimpleAdded;
	}

	public static boolean isKeyListenerBaseAdded() {
		return keyListenerBaseAdded;
	}

	public static boolean isKeyListenerAdvancedAdded() {
		return keyListenerAdvancedAdded;
	}
	
	public static CalculatorMainApp getInstance(JFrame parentCaller) {
		if(singletonInstance == null || !CalculatorMainApp.active)
			singletonInstance = new CalculatorMainApp(parentCaller);
		
		return singletonInstance;
	}

}
