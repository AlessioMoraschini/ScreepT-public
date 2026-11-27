/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
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
import org.junit.Ignore;
import org.junit.Test;

import antilocker.AntiLocker;
import test.util.utils.TestUtils;

@Ignore
public class TestAntiLock {

	@Test
	public void testAntiLock() throws InterruptedException {
		AntiLocker antiLock = new AntiLocker(1000, 20, 20);
		antiLock.start();
		TestUtils.printHeader("#", 60, "Antilock started!");
		Thread.sleep(5000);
		
		antiLock.pause();
		TestUtils.printHeader("#", 60, "Antilock paused!");
		Thread.sleep(5000);

		antiLock.resume();
		TestUtils.printHeader("#", 60, "Antilock resumed!");
		Thread.sleep(5000);
		
		antiLock.abort();
		TestUtils.printHeader("#", 60, "Antilock aborted!");
	}
}
