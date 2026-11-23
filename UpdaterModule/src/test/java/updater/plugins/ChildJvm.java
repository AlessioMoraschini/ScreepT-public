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
package updater.plugins;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs a class with a main method in a JVM of its own (own working directory, never a window), keeping the coverage agent of the test JVM. */
public class ChildJvm {

	public final int exitCode;
	public final String output;

	private ChildJvm(int exitCode, String output) {
		this.exitCode = exitCode;
		this.output = output;
	}

	public static ChildJvm run(Class<?> main, File workingDir, int timeoutSeconds, String... args) throws Exception {
		List<String> cmd = new ArrayList<String>();
		cmd.add(new File(System.getProperty("java.home"), "bin/java").getPath());
		cmd.add("-Djava.awt.headless=" + java.awt.GraphicsEnvironment.isHeadless());
		cmd.add("-Dfile.encoding=UTF-8");
		for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			if (a.startsWith("-javaagent")) {
				cmd.add(a);
			}
		}
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add(main.getName());
		for (String a : args) {
			cmd.add(a);
		}
		File log = File.createTempFile("child", ".log", workingDir);
		Process p = new ProcessBuilder(cmd).directory(workingDir).redirectErrorStream(true).redirectOutput(log).start();
		if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
			p.destroyForcibly();
			p.waitFor(10, TimeUnit.SECONDS);
			return new ChildJvm(-1, "TIMEOUT after " + timeoutSeconds + " s\n" + new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8));
		}
		return new ChildJvm(p.exitValue(), new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8));
	}
}
