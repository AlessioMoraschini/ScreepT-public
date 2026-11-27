/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Ultimate Calculator - Copyright (c) 2016 Minhas Kamal (original project)
 * Modifications Copyright (C) 2019-2026 AM-Design-Development - (Alessio Moraschini)
 *
 * Licensed under the MIT License: see the file license/LICENSE.txt of this module.
 */
/**
 * Developer: Minhas Kamal(BSSE-0509, IIT, DU)
 * Date: 02-Jan-2014
**/


package com.minhasKamal.ultimateCalculator.calculators.unitConverter;

public class UnitConverterOperationTemperature {
	/**
	 * Temperature											
	**/
	public double Temperature(int from, int to, double input){
		double output=0.0;
		
		if(from==1){									//Celsius
			if(to==1){				//Celsius
				output = input;
			}else if(to==2){		//Fahrenheit
				output = ((input/5)*9)+32;
			}else if(to==3){		//Kelvin
				output = input+273.15;
			}
		}
		else if(from==2){									//Fahrenheit
			if(to==1){				//Celsius
				output = ((input-32)/9)*5;
			}else if(to==2){		//Fahrenheit
				output = input;
			}else if(to==3){		//Kelvin
				output = (((input-32)/9)*5)+273.15;
			}
		}
		else if(from==3){									//Kelvin
			if(to==1){				//Celsius
				output = input-273.15;
			}else if(to==2){		//Fahrenheit
				output = (((input-273.15)/5)*9)+32;
			}else if(to==3){		//Kelvin
				output = input;
			}
		}
		
		return output;
	}
}
