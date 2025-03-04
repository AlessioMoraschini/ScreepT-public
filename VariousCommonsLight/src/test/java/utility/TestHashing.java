package utility;

import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;

import test.util.configuration.RepeatTest;
import test.util.configuration.RepeatedTestRule;
import test.util.utils.TestUtils;
import various.common.light.utility.hash.HashDTO;
import various.common.light.utility.hash.SafeHasher;
import various.common.light.utility.hash.SafeHasher.HashAlgorithm;
import various.common.light.utility.hash.SafeHasher.IterationsLevel;

public class TestHashing {

	@Rule
	public RepeatedTestRule repeatRule = new RepeatedTestRule();
	private static final int repeatNTimes = 4;
	
	private static final IterationsLevel iter = IterationsLevel.MED;
	private static final HashAlgorithm algo = HashAlgorithm.PBKDF2SHA512;
	char[] psw;
	
	@Test
	@RepeatTest(times = repeatNTimes)
	public void testPKBHashCharSource() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String S = "C";
		TestUtils.printHeader(S, 50, "CHAR ARRAY SRC");
		
		psw = new char[]{'a', 'b', 'a', 'b', 'a', 'b', 'a', 'b', 'a', 'b', 'a', 'b', 'a', 'b', 'a', 'b'};
		HashDTO hashResult = SafeHasher.hashPsw(psw, iter, algo);
		
		TestUtils.printHeader(S, 40, "SOURCE");
		TestUtils.printCharArray(psw);
		
		TestUtils.printHeader(S, 40, "SALT");
		TestUtils.printByteArray(hashResult.getSalt());

		TestUtils.printHeader(S, 40, "HASHED");
		TestUtils.printByteArray(hashResult.getHashed());
		
		byte[] hashBackResult = SafeHasher.hashPswWithGivenSalt(psw, hashResult.getSalt(), iter, algo);
		
		TestUtils.printHeader(S, 40, "HASHED-BACK with previous salt");
		TestUtils.printByteArray(hashBackResult);
		
		assertTrue(Arrays.equals(hashBackResult, hashResult.getHashed()));
	}

	@Test
	@RepeatTest(times = repeatNTimes)
	public void testPKBHashBytesSource() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String S = "B";
		TestUtils.printHeader(S, 50, "BYTES ARRAY SRC");
		
		byte[] srcBytes = SafeHasher.generateSecureRandom(16);
		psw = SafeHasher.byteArrayToChar(srcBytes);
		HashDTO hashResult = SafeHasher.hashPsw(psw, iter, algo);
		
		TestUtils.printHeader(S, 40, "SOURCE_BYTES");
		TestUtils.printByteArray(srcBytes);
		
		TestUtils.printHeader(S, 40, "SOURCE_AS_CHAR");
		TestUtils.printCharArray(psw);
		
		TestUtils.printHeader(S, 40, "SALT");
		TestUtils.printByteArray(hashResult.getSalt());
		
		TestUtils.printHeader(S, 40, "HASHED");
		TestUtils.printByteArray(hashResult.getHashed());
		
		byte[] hashBackResult = SafeHasher.hashPswWithGivenSalt(psw, hashResult.getSalt(), iter, algo);
		
		TestUtils.printHeader(S, 40, "HASHED-BACK with previous salt");
		TestUtils.printByteArray(hashBackResult);
		
		assertTrue(Arrays.equals(hashBackResult, hashResult.getHashed()));
	}
	
}
