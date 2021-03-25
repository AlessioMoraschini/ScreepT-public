package JUnit;

import java.io.File;

import org.junit.Test;

import conversion.ImageToTextExtractor;
import net.sourceforge.tess4j.TesseractException;
import testUtils.TestUtils;

public class ImageToTextReaderTest {
	
	public static final String SOURCE_IMAGE_PATH = "TestFiles/mediaFileTest/img.png";
	
	 @Test
	 public void testExtractor() throws TesseractException {
		 TestUtils.printSeparator(80);
		 TestUtils.print("\n\n");
		 ImageToTextExtractor extractor = new ImageToTextExtractor();
		 TestUtils.printl(extractor.readTextFromImage(new File(SOURCE_IMAGE_PATH)));
	 }
}
