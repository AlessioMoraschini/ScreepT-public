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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import updater.module.plugins.PluginDTO;

class JTableButtonRenderer extends JButton implements TableCellRenderer {
	private static final long serialVersionUID = 6493027142302342323L;
	
	private IPluginManagerGui pluginManagerInterface;

	public JTableButtonRenderer(IPluginManagerGui pluginManagerInterface) {
		setOpaque(false);
		setForeground(Color.BLACK);
        setBackground(Color.LIGHT_GRAY);
		setHorizontalAlignment( JLabel.CENTER );
		this.pluginManagerInterface = pluginManagerInterface;
	}

	@Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
		
		String pluginName = (String)table.getValueAt(row, 0);
		PluginDTO plugin = pluginManagerInterface.getPluginDTO(pluginName);
		boolean installed = plugin.installationCompleted;
		
        setForeground((installed)? Color.LIGHT_GRAY : Color.BLACK);
        setBackground((installed)? Color.BLACK : Color.LIGHT_GRAY);
        setText((installed)? "Uninstall " + pluginName : "Install " + pluginName);
        
		return this;
    }
}
