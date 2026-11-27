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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** The release tools of updater.jar: zip the folders, generate the hashes of the manifests (see {@link UpdaterToolsScenario}). */
public class UpdaterToolsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void zipFoldersAndGenerateTheHashesOfTheManifests() throws Exception {
		org.junit.Assume.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
		ChildJvm r = ChildJvm.run(UpdaterToolsScenario.class, tmp.getRoot(), 120);
		assertEquals(r.output, 0, r.exitCode);
		assertTrue(r.output, r.output.contains("SCENARIO OK"));
	}
}
