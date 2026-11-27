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
package gui.commons.jmenu;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class CustomJmenuItem extends JMenuItem{
	private static final long serialVersionUID = -2555521027493180609L;
	
	public CustomJmenuItem(AbstractAction action) {
		super(action);
	}

	/**
	 * Fake accelerator, use this to only display shortcuts hint text on the right of item
	 */
	@Override public void setAccelerator( KeyStroke keyStroke ) {
	      super.setAccelerator( keyStroke );
	      getInputMap( WHEN_IN_FOCUSED_WINDOW ).put( keyStroke, "none" );
	 }
}
