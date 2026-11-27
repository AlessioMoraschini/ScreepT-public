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
package test.util.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Safety net for automatic tests: a test must never wait for a person. Every modal dialog that shows up (a question, a warning, an
 * option dialog like "Open with LogSearch / Load in editor anyway / Cancel") is answered by clicking its default button after a short
 * delay, so the test goes on by itself. A file chooser is cancelled. It runs as a daemon thread for the whole test run
 * (see {@link DialogRunListener}) and records what it closed, so a test can check that nothing unexpected was asked.
 */
public final class AutoDialogCloser implements Runnable {

	private static final long POLL_MS = Long.getLong("test.autodialog.pollMs", 150L);
	/** The dialog is given this time before the click (a test that scripts it, or reads it, is not rushed) */
	private static final long GRACE_MS = Long.getLong("test.autodialog.graceMs", 300L);

	private static Thread thread;
	private static final List<String> CLOSED = new CopyOnWriteArrayList<String>();

	private AutoDialogCloser() {
	}

	/** Starts the watcher (once) */
	public static synchronized void start() {
		if (thread == null) {
			thread = new Thread(new AutoDialogCloser(), "AutoDialogCloser");
			thread.setDaemon(true);
			thread.start();
		}
	}

	/** Titles and messages of the dialogs closed so far */
	public static List<String> closedDialogs() {
		return new ArrayList<String>(CLOSED);
	}

	public static void clearHistory() {
		CLOSED.clear();
	}

	@Override
	public void run() {
		java.util.Map<Window, Long> firstSeen = new java.util.WeakHashMap<Window, Long>();
		while (true) {
			try {
				Thread.sleep(POLL_MS);
				long now = System.currentTimeMillis();
				for (Window w : Window.getWindows()) {
					if (w instanceof Dialog && w.isShowing() && ((Dialog) w).isModal()) {
						Long seen = firstSeen.get(w);
						if (seen == null) {
							firstSeen.put(w, now);
						} else if (now - seen >= GRACE_MS) {
							firstSeen.remove(w);
							answer((Dialog) w);
						}
					}
				}
			} catch (Throwable ignored) {
				// the watcher never dies: a window that vanishes while it looks is normal
			}
		}
	}

	private static void answer(final Dialog dialog) {
		final AbstractButton target = chooseButton(dialog);
		CLOSED.add(dialog.getTitle() + " | " + describe(dialog));
		System.err.println("[AutoDialogCloser] answering '" + dialog.getTitle() + "' with " + (target != null ? "'" + target.getText() + "'" : "dispose"));
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (target != null) {
					target.doClick(0);
				}
				if (dialog.isShowing()) {
					dialog.dispose();
				}
			}
		});
	}

	/** The default button of the dialog; for a file chooser the cancel button; else the first affirmative or any button */
	private static AbstractButton chooseButton(Dialog dialog) {
		List<AbstractButton> buttons = new ArrayList<AbstractButton>();
		boolean chooser = collect(dialog, buttons);
		if (chooser) {
			for (AbstractButton b : buttons) {
				String t = text(b);
				if (t.contains("cancel") || t.contains("annulla")) {
					return b;
				}
			}
		}
		if (dialog instanceof javax.swing.RootPaneContainer) {
			javax.swing.JRootPane root = ((javax.swing.RootPaneContainer) dialog).getRootPane();
			JButton def = root != null ? root.getDefaultButton() : null;
			if (def != null && def.isEnabled()) {
				return def;
			}
		}
		for (AbstractButton b : buttons) {
			String t = text(b);
			if (b.isEnabled() && (t.equals("yes") || t.equals("ok") || t.equals("sì") || t.equals("si") || t.startsWith("open") || t.startsWith("continue"))) {
				return b;
			}
		}
		for (AbstractButton b : buttons) {
			if (b.isEnabled()) {
				return b;
			}
		}
		return null;
	}

	private static String text(AbstractButton b) {
		return b.getText() == null ? "" : b.getText().trim().toLowerCase();
	}

	/** Fills the list with the buttons under the container; true if a JFileChooser is inside */
	private static boolean collect(Container c, List<AbstractButton> out) {
		boolean chooser = c instanceof JFileChooser;
		for (Component child : c.getComponents()) {
			if (child instanceof AbstractButton && child.isVisible()) {
				out.add((AbstractButton) child);
			}
			if (child instanceof Container) {
				chooser |= collect((Container) child, out);
			}
		}
		return chooser;
	}

	private static String describe(Container c) {
		for (Component child : c.getComponents()) {
			if (child instanceof JOptionPane) {
				return String.valueOf(((JOptionPane) child).getMessage()).replace('\n', ' ');
			}
			if (child instanceof Container) {
				String inner = describe((Container) child);
				if (!inner.isEmpty()) {
					return inner;
				}
			}
		}
		return "";
	}
}
