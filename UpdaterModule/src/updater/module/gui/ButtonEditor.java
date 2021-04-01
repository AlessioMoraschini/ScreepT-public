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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;

import updater.module.plugins.PluginDTO;
import updater.module.plugins.PluginManager;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.om.exception.ProgressBarInterruptedException;
import various.common.light.utility.log.SafeLogger;

class ButtonEditor extends DefaultCellEditor {
	private static final long serialVersionUID = 2322648608181904669L;

	private static SafeLogger logger = new SafeLogger(ButtonEditor.class);

	protected JButton button;
    private String label;
    private boolean isPushed;

    private static AtomicBoolean isRunning = new AtomicBoolean(false);

    private IPluginManagerGui pluginManagerInterface;

    public ButtonEditor(IPluginManagerGui pluginManagerInterface) {
        super(new JCheckBox());
        this.button = new JButton();
        this.button.setForeground(Color.BLACK);
        this.button.setBackground(Color.LIGHT_GRAY);
        this.button.setOpaque(false);
        this.pluginManagerInterface = pluginManagerInterface;
        this.button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				 fireEditingStopped();
			}
		});
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {

    	String pluginName = (String)table.getValueAt(row, 0);
    	GuiUtils.launchThreadSafeSwing(getActionOnClick(pluginName));
    	PluginDTO clickedButtonPlugin = pluginManagerInterface.getPluginDTO(pluginName);

    	String installStr = clickedButtonPlugin.installationCompleted ? "Uninstall " : "Install ";
    	label = installStr + pluginName;
        button.setText(label);
        isPushed = true;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        if (isPushed) {

        }
        isPushed = false;
        return label;
    }

    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }

    public Runnable getActionOnClick(String pluginName) {
    	return new Runnable() {
			@Override
			public void run() {

				button.setEnabled(false);
				isRunning.set(true);

				logger.info("Starting plugin installation: " + pluginName);

				if (!pluginManagerInterface.isInstalling()) {
					pluginManagerInterface.setInstalling(true);

					PluginDTO clickedButtonPlugin = pluginManagerInterface.getPluginDTO(pluginName);

					boolean installed = clickedButtonPlugin.installationCompleted;
					PluginManager pluginManager = pluginManagerInterface.getPluginManager();

					try {

						PluginDTO refreshedPlugin = pluginManager.retrieveFromCache(clickedButtonPlugin.getName());

						String dynaMsg = installed ? "Uninstall " : "Install ";
						boolean confirm = PluginManagerGUI.dialogHelper.yesOrNo(dynaMsg + refreshedPlugin.getName() + " ?");

						if (confirm && installed) {


							boolean uninstalled = pluginManager.removePlugin(refreshedPlugin);
							if (uninstalled) {
								String message = "Plugin uninstalled correctly. Files are marked for delete and will be removed at next restart ;)";
								logger.info(message);
								PluginManagerGUI.dialogHelper.info(message, "Plugin uninstalled");
							} else {
								String message = "An error occurred during plugin removal! Delete will be re-tried on exit.";
								logger.warn(message);
								PluginManagerGUI.dialogHelper.warn(message, "An error occurred");
							}

							pluginManagerInterface.setInstalling(false);

						} else if (confirm) {
							try {

								PluginDTO downloaded = pluginManager.downloadPlugin(refreshedPlugin);
								pluginManager.installPlugin(downloaded, true);
								String message = "Installation completed! Plugin will be available after application restart.";
								logger.info(message);
								PluginManagerGUI.dialogHelper.info(message, "Plugin installed");

							} catch (Throwable e) {
								if (e instanceof ProgressBarInterruptedException) {
									String message = "Installation process interrupted by user.";
									logger.info(message);
									PluginManagerGUI.dialogHelper.info(message, "Interrupted by user");

								} else if(e instanceof TimeoutException) {
									String message = "Server is unreachable!";
									logger.error(message, e);
									PluginManagerGUI.dialogHelper.error(message, "An error occurred");

								} else {
									String message = "An error occurred during installation";
									logger.error(message, e);
									PluginManagerGUI.dialogHelper.error(message, "An error occurred");
								}
							} finally {
								pluginManagerInterface.setInstalling(false);
							}

						}
					} catch (Throwable e) {
						logger.error("An error occurred", e);
					}

				} else {
					logger.warn("Exiting, installation already in progress.");
					new JOptionHelper(pluginManagerInterface.getFrame()).warn("Must wait current installation to finish before to start another one!", "Already installing");
				}

				logger.info("Refreshing elements...");
				isRunning.set(false);
				button.setEnabled(true);
				pluginManagerInterface.setInstalling(false);
				logger.info("Process completed, refresh completed!");
			}
		};
    }
}