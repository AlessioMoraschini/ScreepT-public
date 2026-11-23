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
package test.util.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Builds every window and panel of a module the way the application would (never shown on screen), lays it out, paints it
 * into an image and disposes it. It exercises the interface code of the module (constructors, layout, renderers, painting) with
 * no person involved. While it runs a security manager refuses what a test must never do: leave the JVM, start a program,
 * reach the network, write outside the temp folder and the module folder, delete outside them.
 * A class that cannot be built without the real application state is skipped: only what builds is counted.
 */
public final class UiSweep {

	private static final Pattern SKIP = Pattern.compile("(?i).*(\\.Main[A-Za-z]*$|Starter|Launcher$|StandAlone|Exit|Splash|Wizard$|Installer|ColorChooserGrid|LightLicenseViewer|FileChooserOnTop|NativeFileChooser).*");
	private static final long PER_CLASS_MS = 10_000;
	/** Class being built right now (named in the blocked-action log) */
	private static volatile String current = "";

	public static final class Result {
		public int tried;
		public int built;
		public final List<String> builtNames = new ArrayList<String>();
		public final List<String> failedNames = new ArrayList<String>();
		public final List<String> hung = new ArrayList<String>();

		@Override
		public String toString() {
			return "UiSweep: " + built + " built of " + tried + " tried, hung " + hung;
		}
	}

	private UiSweep() {
	}

	/** Sweeps the classes of the module that contains the anchor class (only classes whose name matches the optional regex) */
	public static Result run(Class<?> anchor, String nameRegex) throws Exception {
		Result result = new Result();
		if (GraphicsEnvironment.isHeadless()) {
			System.err.println("UiSweep: headless, nothing swept");
			return result;
		}
		File root = classesDir(anchor);
		List<String> names = new ArrayList<String>();
		collect(root, root, names);
		Collections.sort(names);
		System.setProperty("screept.nativeFileDialog", "false");
		MemoryPreferences.install();
		AutoDialogCloser.start();
		SecurityManager old = System.getSecurityManager();
		System.setSecurityManager(new Guard());
		try {
			for (String name : names) {
				if (SKIP.matcher(name).matches() || (nameRegex != null && !name.matches(nameRegex))) {
					continue;
				}
				Class<?> c;
				try {
					c = Class.forName(name, false, anchor.getClassLoader());
				} catch (Throwable t) {
					continue;
				}
				if (c.isInterface() || Modifier.isAbstract(c.getModifiers()) || c.isEnum() || c.isAnonymousClass()
						|| !(Window.class.isAssignableFrom(c) || JComponent.class.isAssignableFrom(c))) {
					continue;
				}
				result.tried++;
				current = name;
				String outcome = buildOne(c);
				if (outcome == null) {
					result.built++;
					result.builtNames.add(name);
				} else if (outcome.equals("HUNG")) {
					if (!interfaceThreadAlive()) {
						result.hung.add(name);
						break; // the interface thread is stuck for good: the rest would only wait
					}
					result.failedNames.add(name + " <too slow: " + PER_CLASS_MS + " ms>"); // slow, not stuck: only a note
				} else {
					result.failedNames.add(name + " <" + outcome + ">");
				}
			}
		} finally {
			System.setSecurityManager(old);
			disposeAll();
		}
		System.err.println(result);
		return result;
	}

	// ------------------------------------------------------------------ methods

	private static final Pattern SKIP_METHOD = Pattern.compile("(?i).*(exit|halt|shutdown|restart|kill|reboot|logoff|hibernate|sleep|wait|robot|lock|^main$|^run$).*");
	private static final Pattern SKIP_METHOD_CLASS = Pattern.compile("(?i).*(Robot|Locker|Keyboard|Hook|Terminal|ConPty).*");

	/**
	 * Calls every method of every class of the module (not only the interface ones) with neutral arguments, twice (empty/zero and
	 * "1"/1/true), under the same guard of {@link #run}: it reaches the logic that the interface sweep never calls.
	 * A call that throws is part of the exercise; one that does not return in time is left behind.
	 */
	public static int runMethods(Class<?> anchor, String nameRegex) throws Exception {
		int calls = 0;
		boolean edtDead = false;
		long moduleDeadline = System.currentTimeMillis() + 10_000;
		if (GraphicsEnvironment.isHeadless()) {
			return calls;
		}
		File root = classesDir(anchor);
		List<String> names = new ArrayList<String>();
		collect(root, root, names);
		Collections.sort(names);
		System.setProperty("screept.nativeFileDialog", "false");
		MemoryPreferences.install();
		AutoDialogCloser.start();
		SecurityManager old = System.getSecurityManager();
		System.setSecurityManager(new Guard());
		strict = true;
		try {
			for (String name : names) {
				if (SKIP.matcher(name).matches() || SKIP_METHOD_CLASS.matcher(name).matches() || (nameRegex != null && !name.matches(nameRegex))) {
					continue;
				}
				if (System.currentTimeMillis() > moduleDeadline) {
					break; // time budget of the module is over: what is left would only slow the build
				}
				current = name;
				progress(name);
				Class<?> c;
				try {
					c = Class.forName(name, false, anchor.getClassLoader());
				} catch (Throwable t) {
					continue;
				}
				if (c.isInterface() || c.isEnum() || c.isAnonymousClass() || c.getName().contains("$Lambda")) {
					continue;
				}
				boolean swing = java.awt.Component.class.isAssignableFrom(c);
				if (swing && edtDead) {
					continue;
				}
				calls += sweepClass(c);
				disposeAll();
				if (swing && !interfaceThreadAlive()) {
					edtDead = true; // the interface thread is stuck for good: no component can be exercised any more
				}
			}
		} finally {
			strict = false;
			System.setSecurityManager(old);
			disposeAll();
		}
		System.err.println("UiSweep.runMethods: " + calls + " calls in " + anchor.getName());
		return calls;
	}

	/** Last class being swept is written to target/methodsweep.txt: where a stuck run stopped */
	private static void progress(String name) {
		try {
			java.nio.file.Files.write(java.nio.file.Paths.get("target", "methodsweep.txt"), (name + System.lineSeparator()).getBytes("UTF-8"),
					java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (Exception ignored) {
			// only a help
		}
	}

	private static int sweepClass(final Class<?> c) {
		final boolean swing = Window.class.isAssignableFrom(c) || java.awt.Component.class.isAssignableFrom(c);
		final int[] count = { 0 };
		final long deadline = System.currentTimeMillis() + 2_000;
		final Object[] holder = new Object[1];
		for (int pass = 0; pass < 2; pass++) {
			final boolean second = pass == 1;
			Method[] methods;
			try {
				methods = c.getDeclaredMethods();
			} catch (Throwable t) {
				return count[0];
			}
			if (!Modifier.isAbstract(c.getModifiers()) && holder[0] == null) {
				holder[0] = timed(swing, new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						return instance(c);
					}
				}, 3_000);
			}
			for (final Method m : methods) {
				if (System.currentTimeMillis() > deadline) {
					return count[0];
				}
				if (m.isSynthetic() || m.isBridge() || SKIP_METHOD.matcher(m.getName()).matches()) {
					continue;
				}
				final boolean isStatic = Modifier.isStatic(m.getModifiers());
				final Object target = holder[0];
				if (!isStatic && target == null) {
					continue;
				}
				count[0]++;
				timed(swing, new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						m.setAccessible(true);
						return m.invoke(isStatic ? null : target, argsFor(m.getParameterTypes(), second));
					}
				}, 1_500);
			}
		}
		return count[0];
	}

	private static Object instance(Class<?> c) {
		List<Constructor<?>> ctors = new ArrayList<Constructor<?>>(Arrays.asList(c.getDeclaredConstructors()));
		Collections.sort(ctors, new Comparator<Constructor<?>>() {
			@Override
			public int compare(Constructor<?> a, Constructor<?> b) {
				return Integer.compare(a.getParameterTypes().length, b.getParameterTypes().length);
			}
		});
		int attempts = 0;
		for (Constructor<?> k : ctors) {
			if (attempts++ >= 4) {
				break;
			}
			try {
				k.setAccessible(true);
				return k.newInstance(argsFor(k.getParameterTypes(), false));
			} catch (Throwable ignored) {
				// try the next constructor
			}
		}
		return null;
	}

	/** Runs on the interface thread (components) or on a helper thread, never longer than the limit; null if it failed or was left behind */
	private static Object timed(boolean onInterfaceThread, Callable<Object> job, long limitMs) {
		final Callable<Object> inner = job;
		FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
			@Override
			public Object call() {
				try {
					return inner.call();
				} catch (Throwable t) {
					return null;
				}
			}
		});
		Thread helper = null;
		if (onInterfaceThread) {
			SwingUtilities.invokeLater(task);
		} else {
			helper = new Thread(task, "UiSweep-method");
			helper.setDaemon(true);
			helper.start();
		}
		try {
			return task.get(limitMs, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			task.cancel(true);
			if (helper != null) {
				helper.interrupt();
				try {
					helper.stop(); // a loop that never ends would burn the processor for the rest of the build
				} catch (Throwable ignored) {
					// already gone
				}
			}
			return null;
		}
	}

	// ------------------------------------------------------------------ building

	private static String buildOne(final Class<?> c) {
		final String[] outcome = { "no constructor worked" };
		FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
			@Override
			public Void call() {
				List<Constructor<?>> ctors = new ArrayList<Constructor<?>>(Arrays.asList(c.getDeclaredConstructors()));
				Collections.sort(ctors, new Comparator<Constructor<?>>() {
					@Override
					public int compare(Constructor<?> a, Constructor<?> b) {
						return Integer.compare(a.getParameterTypes().length, b.getParameterTypes().length);
					}
				});
				int attempts = 0;
				for (Constructor<?> k : ctors) {
					if (attempts++ >= 5) {
						break;
					}
					try {
						k.setAccessible(true);
						Object o = k.newInstance(argsFor(k.getParameterTypes()));
						render(o);
						outcome[0] = null;
						return null;
					} catch (Throwable t) {
						outcome[0] = brief(t.getCause() != null ? t.getCause() : t);
					} finally {
						disposeAll();
					}
				}
				return null;
			}
		});
		SwingUtilities.invokeLater(task);
		try {
			task.get(PER_CLASS_MS, TimeUnit.MILLISECONDS);
		} catch (java.util.concurrent.TimeoutException e) {
			return "HUNG";
		} catch (Exception e) {
			return String.valueOf(e);
		}
		return outcome[0];
	}

	// ------------------------------------------------------------------ interaction

	private static final long INTERACT_MS = 1_000;
	private static final int MAX_CLICKS = 40;
	/** While true the guard is stricter: nothing is written outside the temp and target folders, no preferences are touched */
	private static volatile boolean strict;

	/** Uses what the person would use: tabs, lists, fields, buttons, menus (modal dialogs are answered by AutoDialogCloser) */
	private static void interact(Component root) {
		long deadline = System.currentTimeMillis() + INTERACT_MS;
		strict = true;
		try {
			java.util.Set<Component> done = new java.util.HashSet<Component>();
			java.util.Set<Window> seenWindows = new java.util.HashSet<Window>(Arrays.asList(Window.getWindows()));
			int[] clicks = { 0 };
			List<Component> roots = new ArrayList<Component>();
			roots.add(root);
			for (int round = 0; round < 3 && !roots.isEmpty(); round++) {
				for (Component r : roots) {
					List<Component> all = new ArrayList<Component>();
					gather(r, all, 0);
					for (Component c : all) {
						if (System.currentTimeMillis() > deadline) {
							return;
						}
						if (done.add(c)) {
							poke(c, clicks);
						}
					}
				}
				roots = new ArrayList<Component>();
				for (Window w : Window.getWindows()) {
					if (seenWindows.add(w) && w.isDisplayable()) {
						try {
							w.pack();
							w.validate();
						} catch (Throwable ignored) {
							// not laid out
						}
						roots.add(w);
					}
				}
			}
		} finally {
			strict = false;
		}
	}

	private static void gather(Component c, List<Component> out, int depth) {
		if (c == null || depth > 40 || out.size() > 2000) {
			return;
		}
		out.add(c);
		if (c instanceof javax.swing.JMenu) {
			for (Component m : ((javax.swing.JMenu) c).getMenuComponents()) {
				gather(m, out, depth + 1);
			}
		}
		if (c instanceof java.awt.Container) {
			for (Component k : ((java.awt.Container) c).getComponents()) {
				gather(k, out, depth + 1);
			}
		}
		if (c instanceof JComponent && ((JComponent) c).getComponentPopupMenu() != null) {
			gather(((JComponent) c).getComponentPopupMenu(), out, depth + 1);
		}
	}

	private static void poke(Component c, int[] clicks) {
		try {
			if (c instanceof javax.swing.JTabbedPane) {
				javax.swing.JTabbedPane tabs = (javax.swing.JTabbedPane) c;
				for (int i = 0; i < Math.min(tabs.getTabCount(), 30); i++) {
					tabs.setSelectedIndex(i);
				}
			} else if (c instanceof javax.swing.JComboBox) {
				javax.swing.JComboBox<?> box = (javax.swing.JComboBox<?>) c;
				for (int i = 0; i < Math.min(box.getItemCount(), 20); i++) {
					box.setSelectedIndex(i);
				}
			} else if (c instanceof javax.swing.JSpinner) {
				javax.swing.JSpinner s = (javax.swing.JSpinner) c;
				Object next = s.getNextValue();
				if (next != null) {
					s.setValue(next);
				}
			} else if (c instanceof javax.swing.JSlider) {
				javax.swing.JSlider s = (javax.swing.JSlider) c;
				s.setValue((s.getMinimum() + s.getMaximum()) / 2);
			} else if (c instanceof javax.swing.JTree) {
				javax.swing.JTree t = (javax.swing.JTree) c;
				for (int i = 0; i < Math.min(t.getRowCount(), 200); i++) {
					t.expandRow(i);
				}
				if (t.getRowCount() > 0) {
					t.setSelectionRow(0);
				}
			} else if (c instanceof javax.swing.JTable) {
				javax.swing.JTable t = (javax.swing.JTable) c;
				if (t.getRowCount() > 0 && t.getColumnCount() > 0) {
					t.setRowSelectionInterval(0, 0);
				}
			} else if (c instanceof javax.swing.JList) {
				javax.swing.JList<?> l = (javax.swing.JList<?>) c;
				if (l.getModel().getSize() > 0) {
					l.setSelectedIndex(0);
				}
			} else if (c instanceof javax.swing.text.JTextComponent) {
				javax.swing.text.JTextComponent t = (javax.swing.text.JTextComponent) c;
				if (t.isEditable() && t.isEnabled() && t.getDocument().getLength() == 0) {
					t.setText("test 123");
					t.setCaretPosition(0);
				}
			} else if (c instanceof javax.swing.AbstractButton) {
				javax.swing.AbstractButton b = (javax.swing.AbstractButton) c;
				if (b instanceof javax.swing.JMenu) {
					if (b.isEnabled()) {
						((javax.swing.JMenu) b).setPopupMenuVisible(true);
						((javax.swing.JMenu) b).setPopupMenuVisible(false);
					}
				} else if (b.isEnabled() && clicks[0]++ < MAX_CLICKS) {
					b.doClick(0);
				}
			}
		} catch (Throwable ignored) {
			// a failing reaction is part of what the person may meet: the sweep goes on
		}
	}

	private static boolean interfaceThreadAlive() {
		FutureTask<Void> probe = new FutureTask<Void>(new Callable<Void>() {
			@Override
			public Void call() {
				return null;
			}
		});
		SwingUtilities.invokeLater(probe);
		try {
			probe.get(15, TimeUnit.SECONDS);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** The exception with its first frames: enough to see what the class needed from the running application */
	private static String brief(Throwable t) {
		StringBuilder s = new StringBuilder(String.valueOf(t));
		StackTraceElement[] st = t.getStackTrace();
		for (int i = 0; i < Math.min(st.length, 6); i++) {
			s.append(" @").append(st[i].getClassName().substring(st[i].getClassName().lastIndexOf('.') + 1)).append('.').append(st[i].getMethodName()).append(':').append(st[i].getLineNumber());
		}
		return s.toString();
	}

	private static void render(Object o) {
		BufferedImage img = new BufferedImage(900, 650, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try {
			if (o instanceof Window) {
				Window w = (Window) o;
				w.pack();
				if (w.getWidth() < 50 || w.getHeight() < 50) {
					w.setSize(800, 600);
				}
				w.validate();
				w.printAll(g);
				interact(w);
			} else if (o instanceof JComponent) {
				JFrame host = new JFrame();
				host.add((JComponent) o);
				host.setSize(900, 650);
				host.validate();
				((JComponent) o).printAll(g);
				interact((JComponent) o);
				host.dispose();
			}
		} finally {
			g.dispose();
		}
	}

	/**
	 * For code that drives a real application (not this sweep): same protection as the sweep (no exit, no program started,
	 * no network but localhost, no registry, writes only in temp or target folders). Returns the previous manager, to give
	 * back to {@link #removeGuard(SecurityManager)} when the real exit is wanted.
	 */
	public static SecurityManager installGuard() {
		MemoryPreferences.install();
		SecurityManager old = System.getSecurityManager();
		System.setSecurityManager(new Guard());
		return old;
	}

	public static void removeGuard(SecurityManager previous) {
		System.setSecurityManager(previous);
	}

	/** Closes every window; never blocks the caller for long (a dead or busy interface thread would otherwise hold it forever) */
	private static void disposeAll() {
		if (SwingUtilities.isEventDispatchThread()) {
			disposeNow();
			return;
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				disposeNow();
			}
		}, "UiSweep-dispose");
		t.setDaemon(true);
		t.start();
		try {
			t.join(5_000);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
	}

	private static void disposeNow() {
		for (Window w : Window.getWindows()) {
			try {
				w.dispose();
			} catch (Throwable ignored) {
				// already gone
			}
		}
	}

	private static Object[] argsFor(Class<?>[] types) throws Exception {
		return argsFor(types, false);
	}

	private static Object[] argsFor(Class<?>[] types, boolean alternative) throws Exception {
		Object[] args = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			args[i] = alternative ? alternativeFor(types[i]) : valueFor(types[i]);
		}
		return args;
	}

	/** Second set of neutral values: not empty, not zero */
	private static Object alternativeFor(Class<?> t) throws Exception {
		if (t == boolean.class) {
			return true;
		} else if (t == int.class) {
			return 1;
		} else if (t == long.class) {
			return 1L;
		} else if (t == short.class) {
			return (short) 1;
		} else if (t == byte.class) {
			return (byte) 1;
		} else if (t == float.class) {
			return 1f;
		} else if (t == double.class) {
			return 1d;
		} else if (t == String.class || t == CharSequence.class) {
			return "1";
		} else if (t == List.class || t == java.util.Collection.class) {
			List<Object> l = new ArrayList<Object>();
			l.add("1");
			return l;
		}
		return valueFor(t);
	}

	private static Object valueFor(final Class<?> t) throws Exception {
		if (t == boolean.class) {
			return false;
		} else if (t == int.class) {
			return 0;
		} else if (t == long.class) {
			return 0L;
		} else if (t == short.class) {
			return (short) 0;
		} else if (t == byte.class) {
			return (byte) 0;
		} else if (t == float.class) {
			return 0f;
		} else if (t == double.class) {
			return 0d;
		} else if (t == char.class) {
			return ' ';
		} else if (t == String.class || t == CharSequence.class) {
			return "";
		} else if (t == File.class) {
			return File.createTempFile("sweep", ".txt");
		} else if (t == JFrame.class || t == java.awt.Frame.class || t == Window.class) {
			return new JFrame();
		} else if (t == JDialog.class || t == java.awt.Dialog.class) {
			return new JDialog();
		} else if (t == Component.class || t == java.awt.Container.class || t == JComponent.class || t == JPanel.class) {
			return new JPanel();
		} else if (t == Font.class) {
			return new Font("SansSerif", Font.PLAIN, 12);
		} else if (t == Color.class) {
			return Color.BLACK;
		} else if (t == Runnable.class) {
			return new Runnable() {
				@Override
				public void run() {
				}
			};
		} else if (t == List.class || t == java.util.Collection.class) {
			return new ArrayList<Object>();
		} else if (t == java.util.Map.class) {
			return new HashMap<Object, Object>();
		} else if (t.isArray()) {
			return java.lang.reflect.Array.newInstance(t.getComponentType(), 0);
		} else if (t.isInterface()) {
			return Proxy.newProxyInstance(t.getClassLoader(), new Class<?>[] { t }, new java.lang.reflect.InvocationHandler() {
				@Override
				public Object invoke(Object proxy, java.lang.reflect.Method m, Object[] a) {
					Class<?> r = m.getReturnType();
					if (r == boolean.class) {
						return false;
					} else if (r == int.class) {
						return 0;
					} else if (r == long.class) {
						return 0L;
					}
					return null;
				}
			});
		}
		return null;
	}

	// ------------------------------------------------------------------ class scan

	private static File classesDir(Class<?> anchor) throws URISyntaxException {
		return new File(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	private static void collect(File root, File dir, List<String> out) {
		File[] list = dir.listFiles();
		if (list == null) {
			return;
		}
		for (File f : list) {
			if (f.isDirectory()) {
				collect(root, f, out);
			} else if (f.getName().endsWith(".class") && !f.getName().matches(".*\\$\\d.*")) {
				String rel = root.toURI().relativize(f.toURI()).getPath();
				out.add(rel.substring(0, rel.length() - 6).replace('/', '.'));
			}
		}
	}

	// ------------------------------------------------------------------ safety

	/** Refuses to leave the JVM, start programs, reach the network, write or delete outside the temp and module folders */
	private static final class Guard extends SecurityManager {
		private final String tmp = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath().toLowerCase();
		private final String work = new File(System.getProperty("user.dir")).getAbsolutePath().toLowerCase();

		@Override
		public void checkPermission(Permission perm) {
			// executing a file (Desktop.open/edit/browse, shell association) starts programs without passing through checkExec
			if (perm instanceof java.io.FilePermission && perm.getActions().contains("execute")) {
				deny("execute file " + perm.getName());
			}
			if (perm instanceof RuntimePermission && strict && perm.getName().equals("preferences") && !MemoryPreferences.installed()) {
				deny("preferences (registry)");
			}
			// everything else is allowed (including setting the manager back)
		}

		private void deny(String what) {
			try {
				java.nio.file.Files.write(java.nio.file.Paths.get("target", "uisweep-blocked.txt"),
						(current + " -> " + what + System.lineSeparator()).getBytes("UTF-8"), java.nio.file.StandardOpenOption.CREATE,
						java.nio.file.StandardOpenOption.APPEND);
			} catch (Exception ignored) {
				// the log is only a help
			}
			throw new SecurityException("blocked in tests: " + what);
		}

		@Override
		public void checkPermission(Permission perm, Object context) {
		}

		@Override
		public void checkExit(int status) {
			deny("exit " + status);
		}

		@Override
		public void checkExec(String cmd) {
			deny("start program " + cmd);
		}

		@Override
		public void checkConnect(String host, int port) {
			if (!"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host)) {
				throw new SecurityException("no network in tests: " + host);
			}
		}

		@Override
		public void checkConnect(String host, int port, Object context) {
			checkConnect(host, port);
		}

		@Override
		public void checkWrite(String file) {
			if (!allowed(file)) {
				throw new SecurityException("write outside the test folders: " + file);
			}
		}

		@Override
		public void checkDelete(String file) {
			if (!allowed(file)) {
				throw new SecurityException("delete outside the test folders: " + file);
			}
		}

		private boolean allowed(String file) {
			String p = new File(file).getAbsolutePath().toLowerCase();
			return p.startsWith(tmp) || (!strict && p.startsWith(work)) || p.contains(File.separator + "target" + File.separator);
		}
	}
}
