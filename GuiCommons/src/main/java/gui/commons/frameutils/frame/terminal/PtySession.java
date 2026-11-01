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
package gui.commons.frameutils.frame.terminal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

/**
 * A process attached to a pseudo terminal (pty4j): Linux and macOS use the native pty, Windows uses ConPTY or winpty.
 * The process believes it runs in a real terminal (colors, cursor movements, line editing, TUI programs).
 */
final class PtySession {

	/** Sessions still alive, killed when the JVM ends so no shell is left behind */
	private static final CopyOnWriteArrayList<PtySession> ALIVE = new CopyOnWriteArrayList<PtySession>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			for (PtySession session : ALIVE) {
				session.destroy();
			}
		}, "PtySession-shutdown"));
	}

	private final PtyProcess process;
	private final OutputStream output;
	private volatile boolean destroyed;

	/** True when the pseudo terminal library is there and has a native part for this system */
	static boolean isSupported() {
		try {
			Class.forName("com.pty4j.PtyProcess");
			String os = System.getProperty("os.name", "").toLowerCase();
			String arch = System.getProperty("os.arch", "").toLowerCase();
			if (os.contains("win")) {
				return arch.contains("64") || arch.equals("x86") || arch.contains("aarch");
			}
			return os.contains("linux") || os.contains("mac") || os.contains("darwin") || os.contains("freebsd");
		} catch (Throwable e) {
			return false;
		}
	}

	static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	PtySession(String[] command, String directory, Map<String, String> environment, int cols, int rows, boolean useConPty) throws IOException {
		PtyProcessBuilder builder = new PtyProcessBuilder(command).setEnvironment(environment).setInitialColumns(cols).setInitialRows(rows)
				.setConsole(false);
		if (directory != null) {
			builder.setDirectory(directory);
		}
		if (isWindows()) {
			builder.setUseWinConPty(useConPty);
		}
		process = builder.start();
		output = process.getOutputStream();
		ALIVE.add(this);
	}

	/** Blocking read of what the process prints; -1 when it is over */
	int read(byte[] buffer) {
		try {
			return process.getInputStream().read(buffer);
		} catch (IOException e) {
			return -1;
		}
	}

	void write(byte[] data) {
		if (destroyed || data.length == 0) {
			return;
		}
		try {
			synchronized (output) {
				output.write(data);
				output.flush();
			}
		} catch (IOException e) {
			// the process is gone: nothing to type into
		}
	}

	void resize(int cols, int rows) {
		if (!destroyed) {
			try {
				process.setWinSize(new WinSize(cols, rows));
			} catch (RuntimeException e) {
				// a terminal that cannot be resized keeps its size
			}
		}
	}

	boolean isAlive() {
		return !destroyed && process.isAlive();
	}

	/** Blocks until the process ends */
	void waitForExit() {
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	void destroy() {
		synchronized (this) {
			if (destroyed) {
				return;
			}
			destroyed = true;
		}
		ALIVE.remove(this);
		process.destroyForcibly();
	}
}
