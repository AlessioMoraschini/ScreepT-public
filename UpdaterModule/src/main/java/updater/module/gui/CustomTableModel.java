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