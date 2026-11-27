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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import updater.module.plugins.PluginDTO;

/** Action button of a plugin row, centered in its cell with the style of PluginActionStyle */
class JTableButtonRenderer extends JPanel implements TableCellRenderer {
	private static final long serialVersionUID = 6493027142302342323L;

	private final JButton button = new JButton();
	private IPluginManagerGui pluginManagerInterface;

	public JTableButtonRenderer(IPluginManagerGui pluginManagerInterface) {
		super(null);
		setOpaque(true);
		add(button);
		this.pluginManagerInterface = pluginManagerInterface;
	}

	@Override
	public void doLayout() {
		Dimension size = button.getPreferredSize();
		button.setBounds(Math.max(0, (getWidth() - size.width) / 2), Math.max(0, (getHeight() - size.height) / 2), size.width, size.height);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		String pluginName = (String) table.getValueAt(row, 0);
		PluginDTO plugin = pluginManagerInterface.getPluginDTO(pluginName);

		PluginActionStyle.apply(button, plugin);
		setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
		// the button is centered by hand: this renderer is painted without a layout pass
		Rectangle cell = table.getCellRect(row, column, false);
		Dimension size = button.getPreferredSize();
		button.setBounds(Math.max(0, (cell.width - size.width) / 2), Math.max(0, (cell.height - size.height) / 2), size.width, size.height);

		return this;
	}
}
