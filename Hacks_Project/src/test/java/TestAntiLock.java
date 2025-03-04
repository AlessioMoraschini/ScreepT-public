/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com/
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
