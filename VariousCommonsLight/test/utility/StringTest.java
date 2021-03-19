package utility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import files.FileWorker;
import gui.GuiUtils;
import testconfig.TestConstants;
import utility.string.StringWorker;
import utility.string.StringWorker.EOL;
import utils.TestUtils;

public class StringTest {
	
	private static File LFTEST = new File("C:\\Users\\xx\\Desktop\\Documenti&Utili\\Informatica varie\\CryptoSteg\\Export\\ScreepT_V0.9.17\\Application\\Resources_ScreepT\\Readme\\ReadMe.html");
	private static File testEOLFile = new File(TestConstants.destFolderPath + "EOL_TEST.txt");
	private static File testEOLFileDest = new File(TestConstants.destFolderPath + "EOL_TEST_CONV.txt");

	private static String sourceStringCR = "\" testString \r asd \n\"";
	private static String sourceStringLF = "\" testString \n \n\"";
	private static String sourceStringCRLF = "\" testString \r\n \r\n \"" ;
	
	private static ArrayList<String> inputList = new ArrayList<>();
	
	@BeforeClass
	public static void init() {
		inputList.add(sourceStringCR);
		inputList.add(sourceStringLF);
		inputList.add(sourceStringCRLF);
	}
	
	@Test
	public void testFileRead() throws IOException {
		TestUtils.print(StringWorker.makeCRLFvisible(testEOLFile));
		FileWorker.writeStringToFile(StringWorker.makeCRLFvisible(LFTEST), testEOLFileDest, true);
	}
	
	@Test
	@Ignore
	public void testTextAreaRMLF() throws InterruptedException {
		JFrame frame = new JFrame("Test shortcuts");
		JTextArea textAreaS = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(textAreaS);
		scrollPane.setViewportView(textAreaS);
		frame.add(scrollPane);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
		GuiUtils.centerComponent(frame, 50);
		textAreaS.setText( StringWorker.makeCRLFvisible(sourceStringCRLF));
		
		while(true) {
			Thread.sleep(400);
		}
	}
	
	@Test
	@Ignore
	public void testCrLfReplace() throws IOException {
		
		ArrayList<String> total = new ArrayList<>();
		
		TestUtils.printHeader("#", 60, "TRANSFORM TO - TEST");
		for (EOL eolA : EOL.values()) {
			total.addAll(transformTo(eolA));
		}
		
		writeAllToFile(total);
		
	}
	
	private ArrayList<String> transformTo(EOL output) throws IOException {
		ArrayList<String> outputList = new ArrayList<>();
		
		for(String inputString : inputList) {
			String inputStr = StringWorker.makeCRLFvisible(inputString);
			String transformed = StringWorker.normalizeStringToEol(inputString, output);
			outputList.add(" ## Input string [" + inputStr + "] transform to :: " + output.name()
			+ " = " + transformed + " ## ");
		}
		
		return outputList;
	}
	
	private void writeAllToFile(ArrayList<String> sources) throws IOException {
		StringBuilder builder = new StringBuilder();
		for(String src : sources) {
			builder.append(src);
		}
		
		FileWorker.writeStringToFile(builder.toString(), testEOLFile, true);
	}
}
