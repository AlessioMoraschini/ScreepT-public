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
package updater.module.gui;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.table.AbstractTableModel;

public class CustomTableModel extends AbstractTableModel {
   
	private static final long serialVersionUID = 1L;
	// String name <-> string description <-> boolean already installed <-> checkBox select <-> jbutton uninstall (if installed)
	private static final String[] COLUMN_NAMES = {"Plugin name", "DeScreepTion", "Already installed", "selected for installation", "Single Plugin actions"};
	
    private static final Class<?>[] COLUMN_TYPES = new Class<?>[] {String.class, String.class, String.class, JCheckBox.class,  JButton.class};

    @Override public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override public int getRowCount() {
        return 4;
    }

    @Override public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_TYPES[columnIndex];
    }

    @Override public Object getValueAt(final int rowIndex, final int columnIndex) {
            /*Adding components*/
        switch (columnIndex) {
            case 0: return rowIndex;
            case 1: return rowIndex;
            case 2: // fall through
            case 3: final JButton button = new JButton(COLUMN_NAMES[columnIndex]);
                    return button;
                    
            default: return "Error";
        }
    }   
}