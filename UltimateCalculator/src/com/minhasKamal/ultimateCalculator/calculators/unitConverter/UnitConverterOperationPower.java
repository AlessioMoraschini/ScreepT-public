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

public class UnitConverterOperationPower {
	/**
	 * Power											
	**/
	public double Power(int from, int to, double input){
		double output=0.0;
		
		if(from==1){									//Watt
			if(to==1){				//Watt
				output = input;
			}else if(to==2){		//Kilowatt
				output = input*0.001;
			}else if(to==3){		//Horse Power
				output = input*0.001341022089595;
			}
		}
		else if(from==2){									//Kilowatt
			if(to==1){				//Watt
				output = input*1000;
			}else if(to==2){		//Kilowatt
				output = input;
			}else if(to==3){		//Horse Power
				output = input*1.341022089595028;
			}
		}
		else if(from==3){									//Horse Power
			if(to==1){				//Watt
				output = input*745.6998715822702;
			}else if(to==2){		//Kilowatt
				output = input*0.7456998715822702;
			}else if(to==3){		//Horse Power
				output = input;
			}
		}
		
		return output;
	}
}
