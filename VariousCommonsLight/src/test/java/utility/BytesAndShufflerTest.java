package utility;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import various.common.light.utility.bytes.ByteArrayShuffler;
import various.common.light.utility.bytes.ByteWorker;

/** Byte level helpers: conversions must round-trip, bit operations must obey their algebra. */
public class BytesAndShufflerTest {

	@Test
	public void textToBytesAndBackKeepsNonAsciiCharacters() throws UnsupportedEncodingException {
		String text = "Città – naïve ☕ 日本";
		byte[] utf8 = ByteWorker.strToByteArray(text, "UTF-8");
		assertEquals(text, new String(utf8, "UTF-8"));
		assertTrue("a multi-byte text is longer in bytes than in characters", utf8.length > text.length());

		ArrayList<Byte> asList = ByteWorker.strToByteList(text, "UTF-8");
		assertEquals(utf8.length, asList.size());
		assertArrayEquals(utf8, ByteWorker.byteListToByteArray(asList));
		assertEquals(asList, ByteWorker.byteArrToByteList(utf8));
		assertArrayEquals(text.getBytes(), ByteWorker.strToByteArray(text));
		assertEquals(ByteWorker.strToByteList(text).size(), text.getBytes().length);
	}

	@Test
	public void binaryStringShowsEveryByteOnEightBitsSeparatedByDots() {
		assertEquals("01000001", ByteWorker.byteArrayToBinary("A".getBytes()));
		assertEquals("00000001.11111111.10000000.00000000", ByteWorker.byteArrayToBinary(new byte[] { 1, -1, -128, 0 }));
	}

	@Test
	public void xorAndOrAreByteWiseAndNeedEqualLengths() {
		byte[] a = { 0x0F, (byte) 0xF0, 0x55, (byte) 0xFF };
		byte[] b = { 0x3C, (byte) 0xCC, (byte) 0xAA, 0x00 };
		assertArrayEquals(new byte[] { 0x33, 0x3C, (byte) 0xFF, (byte) 0xFF }, ByteWorker.xor(a, b));
		assertArrayEquals(new byte[] { 0x0C, (byte) 0xC0, 0x00, 0x00 }, ByteWorker.and(a, b));
		assertArrayEquals(new byte[] { 0x3F, (byte) 0xFC, (byte) 0xFF, (byte) 0xFF }, ByteWorker.or(a, b));
		// xor is its own inverse: the base of every simple stream mask
		assertArrayEquals(a, ByteWorker.xor(ByteWorker.xor(a, b), b));
		assertNull(ByteWorker.xor(a, new byte[3]));
		assertNull(ByteWorker.and(a, new byte[5]));
		assertNull(ByteWorker.or(new byte[0], a));
	}

	@Test
	public void xorBytesKeepsTheTailOfTheFirstArrayWhenTheSecondIsShorter() {
		assertArrayEquals(new byte[] { 3, 3, 3, 4 }, ByteWorker.xorBytes(new byte[] { 1, 2, 3, 4 }, new byte[] { 2, 1, 0 }));
		assertArrayEquals(new byte[] { 3, 3, 9, 8 }, ByteWorker.xorBytes(new byte[] { 1, 2, 9, 8 }, new byte[] { 2, 1 }));
		assertArrayEquals(new byte[] { 1, 2 }, ByteWorker.xorBytes(new byte[] { 1, 2 }, new byte[0]));
	}

	@Test
	public void integersSurviveTheTripToFourBytesAndBack() {
		int[] samples = { 0, 1, -1, 255, 256, 65535, 65536, 123456789, Integer.MAX_VALUE, Integer.MIN_VALUE, -123456789 };
		for (int v : samples) {
			byte[] b = ByteWorker.retrieveBytesFromInt(v);
			assertEquals(4, b.length);
			assertEquals("value " + v, v, ByteWorker.fourBytesToIntShift(b[0], b[1], b[2], b[3]));
		}
		// big endian
		assertArrayEquals(new byte[] { 0x01, 0x02, 0x03, 0x04 }, ByteWorker.retrieveBytesFromInt(0x01020304));
		assertEquals(0x010203, ByteWorker.threeBytesToIntShift((byte) 1, (byte) 2, (byte) 3));
		assertEquals(0xFFFFFF, ByteWorker.threeBytesToIntShift((byte) -1, (byte) -1, (byte) -1));
		assertEquals(0x0102, ByteWorker.twoBytesToIntShift((byte) 1, (byte) 2));
		assertEquals("negative bytes are unsigned", 0xFF80, ByteWorker.twoBytesToIntShift((byte) -1, (byte) -128));
	}

	// ---------------------------------------------------------------- shuffler

	private static byte[] randomBytes(int n, long seed) {
		byte[] data = new byte[n];
		new Random(seed).nextBytes(data);
		return data;
	}

	@Test
	public void shufflingIsReversibleForAnyLength() {
		int[] lengths = { 1, 15, 16, 255, 256, 257, 511, 512, 1000, 4096, 5000 };
		for (int n : lengths) {
			byte[] original = randomBytes(n, n);
			byte[] backup = original.clone();
			byte[] shuffled = ByteArrayShuffler.entroPieForward(original);
			assertNotNull(shuffled);
			assertEquals(n, shuffled.length);
			assertArrayEquals("the input is never modified (length " + n + ")", backup, original);
			assertArrayEquals("round trip with length " + n, original, ByteArrayShuffler.entroPieBack(shuffled));
		}
	}

	@Test
	public void shufflingReallyMovesBytesOfFullBlocksAndKeepsTheirMultiset() {
		byte[] data = new byte[512];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (i * 7 + i / 256);
		}
		byte[] shuffled = ByteArrayShuffler.entroPieForward(data);
		assertFalse("a full 256 byte window is permuted", Arrays.equals(data, shuffled));
		byte[] a = data.clone();
		byte[] b = shuffled.clone();
		Arrays.sort(a);
		Arrays.sort(b);
		assertArrayEquals("only a permutation: no byte lost or invented", a, b);
	}

	@Test
	public void dataShorterThanOneWindowIsLeftAsItIs() {
		byte[] data = randomBytes(200, 3);
		assertArrayEquals(data, ByteArrayShuffler.entroPieForward(data));
		assertArrayEquals(data, ByteArrayShuffler.entroPieBack(data));
	}

	@Test
	public void emptyInputGivesNull() {
		assertNull(ByteArrayShuffler.entroPieForward(new byte[0]));
		assertNull(ByteArrayShuffler.entroPieBack(new byte[0]));
	}
}
