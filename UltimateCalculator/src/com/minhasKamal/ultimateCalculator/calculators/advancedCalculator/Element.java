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
 * Developer: Minhas Kamal (BSSE-0509, IIT, DU)
**/

package com.minhasKamal.ultimateCalculator.calculators.advancedCalculator;

public class Element {
	private String string;     //string property
    private boolean operator;      //operator-true, operand-false
    
    public Element(String string, boolean operator){
    	setString(string);
    	setOperator(operator);
    }

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public boolean isOperator() {
		return operator;
	}

	public void setOperator(boolean operator) {
		this.operator = operator;
	}
}
