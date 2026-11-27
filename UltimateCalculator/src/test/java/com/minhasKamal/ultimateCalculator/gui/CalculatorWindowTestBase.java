/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Ultimate Calculator - Copyright (c) 2016 Minhas Kamal (original project)
 * Modifications Copyright (C) 2019-2026 AM-Design-Development - (Alessio Moraschini)
 *
 * Licensed under the MIT License: see the file license/LICENSE.txt of this module.
 */
package com.minhasKamal.ultimateCalculator.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

import com.minhasKamal.ultimateCalculator.mainFrame.UltimateCalculatorFrame;

/**
 * Base of the calculator window tests: builds a calculator on the interface thread, clicks its buttons with doClick,
 * reads its displays through the private fields, and disposes every window opened during the test.
 */
abstract class CalculatorWindowTestBase {

	private Set<Window> windowsBefore;
	/** Keyboard manager that lets a test make a frame the active window (needed by the keyboard dispatcher) */
	private final ActivatingFocusManager focusManager = new ActivatingFocusManager();
	private KeyboardFocusManager previousFocusManager;

	static final class ActivatingFocusManager extends DefaultKeyboardFocusManager {
		private Window wanted;

		/** The window system may deactivate a shown frame at any moment (another application takes the focus): the test decides, not the desktop. */
		void reassert() {
			activate(wanted);
		}

		void activate(Window w) {
			wanted = w;
			// no focused field of an earlier test may swallow the keys
			setGlobalFocusOwner(null);
			setGlobalActiveWindow(w);
		}
	}

	@Before
	public void prepareWindows() {
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		windowsBefore = new HashSet<Window>(Arrays.asList(Window.getWindows()));
		previousFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		KeyboardFocusManager.setCurrentKeyboardFocusManager(focusManager);
	}

	@After
	public void closeWindows() throws Exception {
		if (windowsBefore == null) {
			return;
		}
		onEdt(new Callable<Void>() {
			public Void call() {
				for (Window w : Window.getWindows()) {
					if (!windowsBefore.contains(w)) {
						w.dispose();
					}
				}
				return null;
			}
		});
		KeyboardFocusManager.setCurrentKeyboardFocusManager(previousFocusManager);
	}

	static <T> T onEdt(final Callable<T> task) throws Exception {
		final AtomicReference<T> result = new AtomicReference<T>();
		final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				try {
					result.set(task.call());
				} catch (Throwable t) {
					failure.set(t);
				}
			}
		});
		if (failure.get() != null) {
			throw new AssertionError("failed on the interface thread: " + failure.get(), failure.get());
		}
		return result.get();
	}

	/** Builds the calculator on the interface thread */
	static <T extends UltimateCalculatorFrame> T build(final Class<T> type) throws Exception {
		return onEdt(new Callable<T>() {
			public T call() throws Exception {
				return type.newInstance();
			}
		});
	}

	/** Value of a (private) field of the owner, looked up through the class hierarchy */
	@SuppressWarnings("unchecked")
	static <T> T field(Object owner, String name) {
		for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return (T) f.get(owner);
			} catch (NoSuchFieldException e) {
				// try the superclass
			} catch (IllegalAccessException e) {
				throw new AssertionError(e);
			}
		}
		throw new AssertionError("no field " + name + " in " + owner.getClass());
	}

	static void setField(Object owner, String name, Object value) throws Exception {
		for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				f.set(owner, value);
				return;
			} catch (NoSuchFieldException e) {
				// try the superclass
			}
		}
		throw new AssertionError("no field " + name);
	}

	static AbstractButton find(Container root, String text) {
		for (Component c : root.getComponents()) {
			if (c instanceof AbstractButton && text.equals(((AbstractButton) c).getText())) {
				return (AbstractButton) c;
			}
			if (c instanceof Container) {
				AbstractButton found = find((Container) c, text);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/** Clicks the buttons with the given texts, in order (as a person would do) */
	static void click(final UltimateCalculatorFrame calc, final String... texts) throws Exception {
		onEdt(new Callable<Void>() {
			public Void call() {
				for (String text : texts) {
					AbstractButton b = find(calc.gui.getContentPane(), text);
					if (b == null) {
						throw new AssertionError("no button " + text);
					}
					b.doClick(0);
				}
				return null;
			}
		});
	}

	static void clickButton(final AbstractButton button) throws Exception {
		onEdt(new Callable<Void>() {
			public Void call() {
				button.doClick(0);
				return null;
			}
		});
	}

	/** Clicks one digit per character: "123" clicks 1, 2, 3 */
	static void type(UltimateCalculatorFrame calc, String digits) throws Exception {
		String[] each = new String[digits.length()];
		for (int i = 0; i < each.length; i++) {
			each[i] = String.valueOf(digits.charAt(i));
		}
		click(calc, each);
	}

	static String text(Object component) throws Exception {
		if (component instanceof JLabel) {
			return ((JLabel) component).getText();
		}
		return ((JTextComponent) component).getText();
	}

	/** Sends the key to the keyboard dispatchers of the calculator, as the keyboard manager would */
	static boolean pressKey(final JFrame frame, final int id, final int code, final char ch) throws Exception {
		return onEdt(new Callable<Boolean>() {
			public Boolean call() {
				focusManager().reassert();
				KeyEvent e = new KeyEvent(frame, id, System.currentTimeMillis(), 0, code, ch);
				for (java.awt.KeyEventDispatcher d : dispatchers()) {
					if (d.dispatchKeyEvent(e)) {
						return true;
					}
				}
				return false;
			}
		});
	}

	static ActivatingFocusManager focusManager() {
		return (ActivatingFocusManager) KeyboardFocusManager.getCurrentKeyboardFocusManager();
	}

	@SuppressWarnings("unchecked")
	static List<java.awt.KeyEventDispatcher> dispatchers() {
		try {
			Field f = KeyboardFocusManager.class.getDeclaredField("keyEventDispatchers");
			f.setAccessible(true);
			return new ArrayList<java.awt.KeyEventDispatcher>((List<java.awt.KeyEventDispatcher>) f.get(KeyboardFocusManager.getCurrentKeyboardFocusManager()));
		} catch (Exception e) {
			throw new AssertionError("cannot reach the key dispatchers: " + e);
		}
	}

	void activate(final JFrame frame) throws Exception {
		onEdt(new Callable<Void>() {
			public Void call() {
				focusManager.activate(frame);
				return null;
			}
		});
	}
}
