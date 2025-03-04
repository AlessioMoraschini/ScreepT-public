package various;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.awt.Color;
import java.util.Arrays;

import javax.swing.JFrame;

import org.junit.Ignore;
import org.junit.Test;

import gui.commons.frameutils.frame.ColorPickerFrame;
import various.common.light.utility.manipulation.ConversionUtils;

public class ColorPickerTest {
	
	public static Color testColor = new Color(34, 122, 219);

	@Test
	@Ignore
	public void testColorPickerFrame() throws InterruptedException {
		ColorPickerFrame picker = new ColorPickerFrame(null);
		picker.setVisible(true);
		picker.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		while(true) {
			Thread.sleep(1000);
		}
		
	}
	
	@Test
	public void testConversion() {
		float[] hsb = Color.RGBtoHSB(testColor.getRed(), testColor.getGreen(), testColor.getBlue(), null);
		
		Color reconverted = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
		Color reconvertedFake = Color.getHSBColor(hsb[0], 0.15f, hsb[2]);
		
		assertEquals(testColor, reconverted);
		assertNotEquals(testColor, reconvertedFake);
	}
	
	@Test
	@Ignore
	public void testCOnversionString() {
		String hsbStr = ConversionUtils.getHsbString(testColor, ":");
		float[] hsb = Color.RGBtoHSB(testColor.getRed(), testColor.getGreen(), testColor.getBlue(), null);
		
		System.out.println("HSB Str: " + hsbStr);
		System.out.println("Color method HSB: " + Arrays.toString(hsb));
		
		Color reconvertedHsb = ConversionUtils.getColorFromHsbString(hsbStr, ":", Color.BLACK);
		
		System.out.println("Original RGB: " + testColor);
		System.out.println("RGB from HSB string: " + reconvertedHsb);
		
		
		assertEquals(testColor, reconvertedHsb);
	}
}
