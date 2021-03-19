package utility;

import org.junit.Test;

import utility.bytes.BitWorker;
import utils.TestUtils;

public class BitWorkerTest {
	
	@Test
	public void testConversionMask() {
		int prefix = 24;
		String maskFromPrefix = BitWorker.maskLengthToBytes(prefix);
		
		TestUtils.printl("Conversion from " + prefix + " to subnetMask -> " + maskFromPrefix);
		
		String mask = "255.255.255.0";
		int prefixFromMask = BitWorker.bytesToMaskLength(mask);
		TestUtils.printl("Conversion from " + mask + " to prefix -> " + prefixFromMask);
	}

}
