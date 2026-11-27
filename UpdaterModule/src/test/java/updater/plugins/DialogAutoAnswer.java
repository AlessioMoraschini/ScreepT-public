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
package updater.plugins;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * For child JVMs of the scenarios: answers the Swing dialogs the code under test opens (it would wait for a person forever).
 * Questions get "No", every other message the first button. Each message is printed as "DIALOG: text".
 */
public class DialogAutoAnswer {

	private static final List<String[]> rules = new ArrayList<String[]>();
	private static String chooser = null;

	/**
	 * Scripted answers: "text of the message=button;text of another=button" (each rule is used once, in order of match).
	 * chooser = folder the file chooser approves, "cancel" to dismiss it, null to leave it alone.
	 */
	public static void configure(String ruleList, String chooserFolder) {
		rules.clear();
		if (ruleList != null && !ruleList.isEmpty()) {
			for (String r : ruleList.split(";"))
				rules.add(r.split("=", 2));
		}
		chooser = chooserFolder;
	}

	public static void start() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						for (Window w : Window.getWindows()) {
							if (w.isShowing())
								answer(w);
						}
						Thread.sleep(100);
					} catch (Throwable ignored) {
					}
				}
			}
		}, "dialog-auto-answer");
		t.setDaemon(true);
		t.start();
	}

	private static void answer(Window w) {
		JFileChooser fc = findChooser(w);
		if (fc != null && chooser != null) {
			System.out.println("CHOOSER: " + fc.getDialogTitle());
			if ("cancel".equals(chooser)) {
				fc.cancelSelection();
			} else {
				fc.setSelectedFile(new java.io.File(chooser));
				fc.approveSelection();
			}
			return;
		}
		JOptionPane pane = find(w);
		if (pane == null)
			return;
		String message = String.valueOf(pane.getMessage());
		System.out.println("DIALOG: " + message);
		String wanted = null;
		synchronized (rules) {
			for (int i = 0; i < rules.size(); i++) {
				if (message.toLowerCase().contains(rules.get(i)[0].toLowerCase())) {
					wanted = rules.remove(i)[1];
					break;
				}
			}
		}
		List<AbstractButton> buttons = new ArrayList<AbstractButton>();
		collect(pane, buttons);
		AbstractButton pick = null;
		for (AbstractButton b : buttons) {
			String t = b.getText() == null ? "" : b.getText();
			if (wanted != null ? t.toLowerCase().startsWith(wanted.toLowerCase()) : "No".equalsIgnoreCase(t))
				pick = b;
		}
		if (pick == null && !buttons.isEmpty())
			pick = buttons.get(0);
		if (pick != null)
			pick.doClick();
	}

	private static JOptionPane find(Container c) {
		for (Component k : c.getComponents()) {
			if (k instanceof JOptionPane)
				return (JOptionPane) k;
			if (k instanceof Container) {
				JOptionPane r = find((Container) k);
				if (r != null)
					return r;
			}
		}
		return null;
	}

	private static JFileChooser findChooser(Container c) {
		for (Component k : c.getComponents()) {
			if (k instanceof JFileChooser)
				return (JFileChooser) k;
			if (k instanceof Container) {
				JFileChooser r = findChooser((Container) k);
				if (r != null)
					return r;
			}
		}
		return null;
	}

	private static void collect(Container c, List<AbstractButton> out) {
		for (Component k : c.getComponents()) {
			if (k instanceof AbstractButton)
				out.add((AbstractButton) k);
			else if (k instanceof Container)
				collect((Container) k, out);
		}
	}
}
