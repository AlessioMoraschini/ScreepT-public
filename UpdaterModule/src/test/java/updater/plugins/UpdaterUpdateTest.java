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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The application update: new files over the old ones, old jar replaced, launcher ini updated, personal files kept.
 * Every scenario is a JVM of its own (the updater always ends with System.exit and takes seconds on its pauses): they
 * all run at the same time, in a folder each, and the tests check what each one left behind.
 */
public class UpdaterUpdateTest {

	@ClassRule
	public static TemporaryFolder tmp = new TemporaryFolder();

	private static final class Run {
		final File root, app;
		final String[] args;
		ChildJvm result;

		Run(String name, String... args) throws Exception {
			root = tmp.newFolder("ScreepT_V0_" + name);
			app = new File(root, "Application");
			assertTrue(app.mkdirs());
			this.args = args;
		}
	}

	private static final Map<String, Run> runs = new HashMap<String, Run>();

	@BeforeClass
	public static void runAllScenarios() throws Exception {
		Assume.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
		runs.put("ok", new Run("ok", "ok"));
		runs.put("badhash", new Run("badhash", "badhash"));
		runs.put("missing", new Run("missing", "missing"));
		runs.put("destRefused", new Run("destRefused", "ok", "backup current=Yes;not valid=No", "cancel"));
		runs.put("askAgain", new Run("askAgain", "ok", "backup current=Cancel;are you sure=No;backup current=No"));
		runs.put("cancelConfirmed", new Run("cancelConfirmed", "ok", "backup current=Cancel;are you sure=Yes"));
		runs.put("noini", new Run("noini", "noini"));
		runs.put("fail", new Run("fail", "fail"));
		ExecutorService pool = Executors.newFixedThreadPool(4);
		try {
			List<Future<?>> done = new ArrayList<Future<?>>();
			for (final Run r : runs.values()) {
				done.add(pool.submit(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						r.result = ChildJvm.run(UpdaterUpdateScenario.class, r.app, 120, r.args);
						return null;
					}
				}));
			}
			for (Future<?> f : done)
				f.get();
		} finally {
			pool.shutdown();
		}
	}

	private static String text(File f) throws Exception {
		return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
	}

	private static ChildJvm ended(String name, int exitCode) {
		ChildJvm r = runs.get(name).result;
		assertEquals(r.output, exitCode, r.exitCode);
		return r;
	}

	@Test
	public void updatesFilesAndLauncherReference() throws Exception {
		ChildJvm r = ended("ok", 0);
		Run run = runs.get("ok");
		assertFalse(r.output, r.output.contains("RETURNED"));
		assertTrue(r.output, r.output.contains("backup current installation")); // asked, answered "No"
		assertEquals("new", text(new File(run.app, "ScreepT_new.jar")));
		assertEquals("dep", text(new File(run.app, "lib/dep.jar")));
		assertFalse("the old jar is deleted", new File(run.app, "ScreepT_old.jar").exists());
		assertEquals("personal file", text(new File(run.app, "keep_me.txt")));
		String ini = text(new File(run.app, "LAUNCHER/Launcher_Configuration.ini")).replace(" ", "");
		assertTrue(ini, ini.contains("jarToLaunch=ScreepT_new.jar"));
		String props = text(new File(run.app, "resourcesFilePathsMappings.properties"));
		assertTrue("new properties from the update: " + props, props.contains("OTHER=new"));
		assertTrue("the proxy setting of the user survives the update: " + props, props.contains("proxy.example"));
		assertEquals("files for the parent folder (licenses, launcher) go one level up", "license", text(new File(run.root, "license.txt")));
	}

	@Test
	public void refusedWhenTheHashDoesNotMatch() throws Exception {
		ChildJvm r = ended("badhash", 0);
		Run run = runs.get("badhash");
		assertTrue(r.output, r.output.contains("DIALOG:"));
		assertTrue("nothing is touched", new File(run.app, "ScreepT_old.jar").exists());
		assertFalse(new File(run.app, "ScreepT_new.jar").exists());
		assertFalse(new File(run.app, "lib/dep.jar").exists());
	}

	@Test
	public void missingUpdatesAreReportedAndNothingChanges() throws Exception {
		ChildJvm r = ended("missing", 0);
		Run run = runs.get("missing");
		assertTrue(r.output, r.output.contains("DIALOG:"));
		assertTrue(new File(run.app, "ScreepT_old.jar").exists());
		assertFalse(new File(run.app, "ScreepT_new.jar").exists());
	}

	@Test
	public void backupDestinationRefusedThenUpdateGoesOn() throws Exception {
		ChildJvm r = ended("destRefused", 0);
		assertTrue(r.output, r.output.contains("not valid"));
		assertEquals("new", text(new File(runs.get("destRefused").app, "ScreepT_new.jar")));
	}

	@Test
	public void cancelledQuestionAsksAgainBeforeUpdating() throws Exception {
		ChildJvm r = ended("askAgain", 0);
		assertTrue(r.output, r.output.contains("Are you sure"));
		assertEquals("new", text(new File(runs.get("askAgain").app, "ScreepT_new.jar")));
	}

	@Test
	public void cancelAndConfirmLeavesTheOldVersion() throws Exception {
		ended("cancelConfirmed", 0);
		Run run = runs.get("cancelConfirmed");
		assertTrue("nothing is updated", new File(run.app, "ScreepT_old.jar").exists());
		assertFalse(new File(run.app, "ScreepT_new.jar").exists());
	}

	@Test
	public void updateWorksWithoutLauncherIni() throws Exception {
		ended("noini", 0);
		Run run = runs.get("noini");
		assertEquals("new", text(new File(run.app, "ScreepT_new.jar")));
		assertTrue("without a reference the old jar cannot be removed", new File(run.app, "ScreepT_old.jar").exists());
	}

	@Test
	public void brokenToolCommandEndsWithFailureCode() throws Exception {
		ChildJvm r = ended("fail", 1);
		assertTrue(r.output, r.output.contains("PROCESS FAILED"));
	}
}
