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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Which shell the interactive terminal runs, on every system. Linux and macOS: the user's shell ($SHELL) as a login shell.
 * Windows: Git Bash when installed (a Linux-like shell, as everywhere else), else PowerShell, else cmd.
 * To force another one set the environment variable SCREEPT_SHELL (or the system property screept.shell), for example
 * "wsl.exe" or "C:\Program Files\PowerShell\7\pwsh.exe -NoLogo".
 */
public final class ShellLauncher {

	private ShellLauncher() {
	}

	public static String[] command() {
		String forced = System.getProperty("screept.shell");
		if (forced == null || forced.trim().isEmpty()) {
			forced = System.getenv("SCREEPT_SHELL");
		}
		if (forced != null && !forced.trim().isEmpty()) {
			return forced.trim().split("\\s+");
		}

		if (PtySession.isWindows()) {
			File gitBash = findGitBash();
			if (gitBash != null) {
				return new String[] { gitBash.getAbsolutePath(), "--login", "-i" };
			}
			File powerShell = new File(System.getenv("SystemRoot") + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe");
			return powerShell.isFile() ? new String[] { powerShell.getAbsolutePath(), "-NoLogo" } : new String[] { "cmd.exe" };
		}

		String shell = System.getenv("SHELL");
		if (shell == null || !new File(shell).canExecute()) {
			shell = new File("/bin/bash").canExecute() ? "/bin/bash" : "/bin/sh";
		}
		// a login shell, so the PATH of the user is complete also when ScreepT is started from the desktop (macOS especially)
		return shell.endsWith("sh") && !shell.endsWith("/sh") ? new String[] { shell, "-l" } : new String[] { shell };
	}

	/** Environment of the shell: the one of ScreepT plus what a terminal program expects */
	public static Map<String, String> environment() {
		Map<String, String> env = new HashMap<String, String>(System.getenv());
		// markers of the Claude Code session that may have started ScreepT: a Claude run inside the terminal must start as a new one
		env.remove("CLAUDECODE");
		env.remove("CLAUDE_CODE_CHILD_SESSION");
		env.put("TERM", "xterm-256color");
		env.put("COLORTERM", "truecolor");
		if (PtySession.isWindows()) {
			// Git Bash: stay in the directory it is started in
			env.put("CHERE_INVOKING", "1");
		} else if (!env.containsKey("LANG") || !env.get("LANG").toUpperCase().contains("UTF")) {
			env.put("LANG", "en_US.UTF-8");
		}
		return env;
	}

	private static File findGitBash() {
		String[] roots = { System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"), System.getenv("ProgramW6432"),
				System.getenv("LOCALAPPDATA") == null ? null : System.getenv("LOCALAPPDATA") + "\\Programs" };
		for (String root : roots) {
			if (root != null) {
				File candidate = new File(root, "Git\\bin\\bash.exe");
				if (candidate.isFile()) {
					return candidate;
				}
			}
		}
		return null;
	}
}
