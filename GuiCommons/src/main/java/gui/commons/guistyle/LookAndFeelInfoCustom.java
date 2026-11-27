/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
