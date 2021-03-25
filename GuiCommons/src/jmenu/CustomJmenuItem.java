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
package jmenu;

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
