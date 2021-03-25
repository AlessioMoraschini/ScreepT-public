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
				output = (((input-32)/9)*5)-273.16;
			}
		}
		else if(from==3){									//Kelvin
			if(to==1){				//Celsius
				output = input-273.16;
			}else if(to==2){		//Fahrenheit
				output = (((input-273.16)/5)*9)+32;
			}else if(to==3){		//Kelvin
				output = input;
			}
		}
		
		return output;
	}
}
