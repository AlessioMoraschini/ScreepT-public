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
package gui.commons.guistyle;

import javax.swing.UIManager.LookAndFeelInfo;

public class LookAndFeelInfoCustom extends LookAndFeelInfo{

	public LookAndFeelInfoCustom(String name, String className) {
		super(name, className);
	}
	
	@Override
	public String toString() {
		return super.getName();
	}
	
	public boolean isJGoodiesThemed() {
		return this.getName().toLowerCase().contains("plastic");
	}
	
	public boolean isMetal() {
		return this.getName().toLowerCase().contains("metal");
	}
	
	public boolean isNimbus() {
		return this.getName().toLowerCase().contains("nimbus");
	}
}
