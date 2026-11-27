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
package gui.commons.tabbedpaneutils;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;

public class CustomClosableTab extends JPanel{
	private static final long serialVersionUID = -5436072440304717027L;

	JPanel pnlTab;
	JLabel lblTitle;
	JLabel imageLbl;
	JButton btnClose;
	
	public CustomClosableTab(String title, String tooltip, String iconPath, ActionListener tabCloseListener) {
		
		setToolTipText(tooltip);
		setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+"", "[25.00px,grow]", "[16.00px, grow]"));

		// tab title
		lblTitle = new JLabel();
		lblTitle.setText(title);
		
		// image label
		imageLbl = new JLabel();
		imageLbl.setIcon(new ImageIcon(iconPath));
		imageLbl.setPreferredSize(new Dimension(15, 20));
		imageLbl.setMaximumSize(new Dimension(15, 20));
		
		// button close
		setLayout(new MigLayout(GeneralConfig.DEBUG_GRAPHICS+"", "[16.00px][25.00px:35.00px,grow][15.00px]", "[16.00px,grow]"));

		// add elements to pnlTab : order must be same as "int TAB_XXX_ORDER" defined at start of this class
		add(imageLbl, "cell 0 0 1 1,alignx left,aligny top"); // 1 - ICON
		add(lblTitle, "cell 1 0 1 1,alignx center,aligny top"); // 2 - TEXT TITLE
		btnClose = new JButton();
		
		btnClose = TabUtils.applyStyleToTabCloseBtn(btnClose);
		Dimension btnDimension = new Dimension(20, 20) ;
		btnClose.setMinimumSize(btnDimension);
		btnClose.setPreferredSize(btnDimension);
		btnClose.setMaximumSize(btnDimension);
		btnClose.setText("X");
		btnClose.addActionListener(tabCloseListener);

		add(btnClose, "cell 2 0 1 1,alignx right,aligny top"); // 3 - CLOSE BUTTON
		
	}
}
