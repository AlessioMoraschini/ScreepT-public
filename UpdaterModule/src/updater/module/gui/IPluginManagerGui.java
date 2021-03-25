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

import javax.swing.JFrame;

import updater.module.plugins.PluginDTO;
import updater.module.plugins.PluginManager;

public interface IPluginManagerGui {
	
	public PluginManager getPluginManager();
	public PluginDTO getPluginDTO(String nameWithoutExtension);
	public boolean isInstalling();
	public void setInstalling(boolean installing);
	public JFrame getFrame();
	public JFrame getParentFrame();
	public void refreshTable(boolean useCache) throws Throwable;
	public void refreshGuiLibsAfterChange();
}
