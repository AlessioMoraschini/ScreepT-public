import org.junit.Test;

import antilocker.AntiLocker;
import utils.TestUtils;

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
