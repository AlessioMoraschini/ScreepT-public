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
