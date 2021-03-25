package utility;

import org.junit.Test;

import test.util.utils.TestUtils;
import various.common.light.utility.bytes.BitWorker;

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
