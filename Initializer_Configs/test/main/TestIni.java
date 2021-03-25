package main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.ini4j.InvalidFileFormatException;
import org.junit.BeforeClass;
import org.junit.Test;

import arch.INItializerParent;
import utility.manipulation.ConversionUtils;

public class TestIni {

	private static final String iniFilePath = "TestFiles/test.ini";
	private static final String TEST_SECTION = "Test_Section";
	private static final String TEST_VAR = "Test_Value";
	
	private static String randomString = "";
	
	
	@BeforeClass
	public static void init() throws InvalidFileFormatException, IOException {
		
		new File(iniFilePath).createNewFile();
		
		byte[] randomBytes = new byte[20];
		new Random().nextBytes(randomBytes);
		randomString = ConversionUtils.printHexBinary(randomBytes);
		
		assertTrue(INItializerParent.writeToFile(iniFilePath, TEST_SECTION, TEST_VAR, randomString));
	}
	
	@Test
	public void testRead() throws InvalidFileFormatException, IOException {
		String read = INItializerParent.readFromFile(iniFilePath, TEST_SECTION, TEST_VAR);
		assertEquals(randomString, read);
	}
}
