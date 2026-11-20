/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package plugin.external.arch;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * Reuses a window of a plugin as panel: the window is built as usual (never shown) and its content, menu bar and window wide key
 * bindings are moved into a panel. The code of the window stays the only one: panel and window show the same thing.
 * <p>
 * The window must not be shown or used after {@link #wrap(JFrame)}; dispose it when the panel is closed.
 */
public final class FramePanelAdapter {

	private FramePanelAdapter() {
	}

	public static JComponent wrap(JFrame frame) {
		JPanel host = new JPanel(new BorderLayout());

		JMenuBar menuBar = frame.getJMenuBar();
		if (menuBar != null) {
			frame.setJMenuBar(null);
			host.add(menuBar, BorderLayout.NORTH);
		}

		Container content = frame.getContentPane();
		frame.setContentPane(new JPanel());
		host.add(content, BorderLayout.CENTER);

		// the key bindings of the root pane (valid in the whole window) are valid in the whole panel
		copyBindings(frame.getRootPane(), host, JComponent.WHEN_IN_FOCUSED_WINDOW);
		copyBindings(frame.getRootPane(), host, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		return host;
	}

	private static void copyBindings(JComponent from, JComponent to, int condition) {
		InputMap source = from.getInputMap(condition);
		KeyStroke[] keys = source.allKeys();
		if (keys == null) {
			return;
		}
		InputMap targetInput = to.getInputMap(condition);
		ActionMap sourceActions = from.getActionMap();
		ActionMap targetActions = to.getActionMap();
		for (KeyStroke key : keys) {
			Object name = source.get(key);
			if (name == null) {
				continue;
			}
			targetInput.put(key, name);
			if (sourceActions.get(name) != null) {
				targetActions.put(name, sourceActions.get(name));
			}
		}
	}
}
