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
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import updater.ftp.FakeFtpsServer;

/** The FTPS upload of the release tools of updater.jar against a local fake server (see {@link UpdaterFtpUploadScenario}). */
public class UpdaterFtpUploadTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void releaseFilesAreUploadedWithTheManifestLast() throws Exception {
		Assume.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
		Assume.assumeTrue("keytool needed for the test certificate", FakeFtpsServer.isAvailable());
		ChildJvm r = ChildJvm.run(UpdaterFtpUploadScenario.class, tmp.getRoot(), 120);
		assertEquals(r.output, 0, r.exitCode);
		assertTrue(r.output, r.output.contains("SCENARIO OK"));
	}
}
