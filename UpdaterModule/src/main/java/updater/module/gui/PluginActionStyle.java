package updater.module.gui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;

import updater.module.plugins.PluginDTO;

/**
 * Look of the action button of a plugin row: Install (green, download), Uninstall (red, trash) or
 * Update / Uninstall (blue, update arrows). The behavior of the buttons is not touched: it stays in ButtonEditor.
 */
final class PluginActionStyle {

	static final Color GREEN = new Color(61, 155, 92);
	static final Color RED = new Color(201, 72, 66);
	static final Color BLUE = new Color(58, 124, 214);
	static final Color ORANGE = new Color(214, 140, 38);
	static final Color MUTED = new Color(139, 148, 158);

	static final Dimension BUTTON_SIZE = new Dimension(172, 34);

	enum Action { INSTALL, UNINSTALL, UPDATE }

	private PluginActionStyle() {
	}

	static Action of(PluginDTO plugin) {
		if (plugin == null || !plugin.installationCompleted) {
			return Action.INSTALL;
		}
		return plugin.isUpToDate() ? Action.UNINSTALL : Action.UPDATE;
	}

	/** Text, icon, colors and tooltip of the button for the given plugin */
	static void apply(JButton button, PluginDTO plugin) {
		Action action = of(plugin);
		String name = plugin != null ? plugin.getName() : "";

		button.putClientProperty("JButton.buttonType", "roundRect");
		button.setOpaque(true);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setPreferredSize(BUTTON_SIZE);

		switch (action) {
		case INSTALL:
			button.setText("Install");
			button.setBackground(GREEN);
			button.setIcon(PluginIcons.of(PluginIcons.Kind.DOWNLOAD, 16, Color.WHITE));
			button.setToolTipText("Download and install " + name);
			break;
		case UPDATE:
			button.setText("Update / Uninstall");
			button.setBackground(BLUE);
			button.setIcon(PluginIcons.of(PluginIcons.Kind.UPDATE, 16, Color.WHITE));
			button.setToolTipText("A newer version of " + name + " is available: update it, or uninstall it");
			break;
		default:
			button.setText("Uninstall");
			button.setBackground(RED);
			button.setIcon(PluginIcons.of(PluginIcons.Kind.TRASH, 16, Color.WHITE));
			button.setToolTipText("Remove " + name + " (takes effect at the next restart if it is in use)");
			break;
		}
		button.setIconTextGap(8);
	}
}
