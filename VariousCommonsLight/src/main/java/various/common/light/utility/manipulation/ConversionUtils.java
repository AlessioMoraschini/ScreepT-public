/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.utility.manipulation;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.text.JTextComponent;

import various.common.light.om.HslColor;
import various.common.light.utility.string.StringWorker;

public class ConversionUtils {

	private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

	public static final String baseUppercaseString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static final char[] baseUppercase = baseUppercaseString.toCharArray();

	public static final String baseLowercaseString = "abcdefghijklmnopqrstuvwxyz";
	public static final char[] baseLowercase = baseLowercaseString.toCharArray();

	public static final String baseString = baseUppercaseString.concat(baseLowercaseString);
	public static final char[] baseCharacters = baseString.toCharArray();

	public static final String numberString = "0123456789";
	public static final char[] numberCharacters = numberString.toCharArray();

	public static final String symbolString = "@&#$%";
	public static final char[] symbolCharacters = symbolString.toCharArray();

	public static final String specialString = "`!\\\"'()*+,-./:;<=>?[]^_`{|}~";
	public static final char[] specialCharacters = specialString.toCharArray();

	public static int getPercent(int part, int total) {
		return (int)((float)part/total * 100.0f);
	}

	public static int getPercent(double percentDecimal) {
		return (int)(percentDecimal * 100.0f);
	}

	public static long getPercent(long part, long total) {
		return (long)((float)part/total * 100.0f);
	}

	/**
	 * Convert double given in range 0.0 - 1.0 -> 0-100%
	 * @param percentDecimal
	 * @return
	 */
	public static String getPercentString(double percentDecimal) {
		return String.valueOf((int)(percentDecimal * 100.0f)) + "%";
	}

	public static String getPercentString(int part, int total) {
		return getPercent(part, total) + "%";
	}

	public static String getPercentString(long part, long total) {
		return getPercent(part, total) + "%";
	}

	/**
	 *  return a string containing the frequency in hertx formatted correctly
	 * @param size is the size in bytes to represent in cool way
	 * @return cool formatted string representing the file size with correct measure unit
	 */
	public static String coolFrequency(Long hertz){
		if(hertz <= 0) return "0";
	    final String[] units = new String[] { "Hz", "kHz", "MHz", "GHz", "THz"};
	    int digitGroups = (int) (Math.log10(hertz)/Math.log10(1000));
	    return new DecimalFormat("#,##0.##").format(hertz/Math.pow(1000, digitGroups)) + " " + units[digitGroups];
	}

	/**
	 *  return a string containing the size of the file formatted correctly according to his size
	 *
	 * @return cool formatted string representing the file size with correct measure unit
	 */
	public static String coolFileSize(File file){
		long size = file.length();
		if(size <= 0) return "0";
	    final String[] units = new String[] { "B", "kB", "MB", "GB", "TB", "ExaB" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.##").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	/**
	 *  return a string containing the size formatted correctly according to his size
	 * @param size is the size in bytes to represent in cool way
	 * @return cool formatted string representing the file size with correct measure unit
	 */
	public static String coolFileSize(Long size){
		if(size <= 0) return "0";
	    final String[] units = new String[] { "B", "kB", "MB", "GB", "TB", "ExaB" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.##").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	/**
	 *  return a string containing the size formatted correctly in bits according to his size
	 * @param size is the size in bytes to represent in cool way
	 * @return cool formatted string representing the file size in bits with correct measure unit
	 */
	public static String coolFileSizeBits(File file){
		long size = file.length();
		if(size <= 0) return "0";
	    final String[] units = new String[] { "b", "kb", "Mb", "Gb", "Tb", "Exab" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.##").format(size*8/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	/**
	 *  return a string containing the size formatted correctly in bits according to his size
	 * @param size is the size in bytes to represent in cool way
	 * @return cool formatted string representing the file size in bits with correct measure unit
	 */
	public static String coolFileSizeBits(Long size){
		if(size <= 0) return "0";
	    final String[] units = new String[] { "b", "kb", "Mb", "Gb", "Tb", "Exab" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.##").format(size*8/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	public static String coolTimeFromMsOrNs(Long timeElapsed, boolean nanoseconds){
		Duration ms = nanoseconds ? Duration.ofNanos(timeElapsed) : Duration.ofMillis(timeElapsed);
		return format(ms);
	}

	public static String format(Duration duration) {
	    long days = duration.toDays();
	    duration = duration.minusDays(days);
	    long hours = duration.toHours();
	    duration = duration.minusHours(hours);
	    long minutes = duration.toMinutes();
	    duration = duration.minusMinutes(minutes);
	    long seconds = duration.getSeconds() ;
	    duration = duration.minusSeconds(seconds);
	    long mseconds = duration.toMillis() ;
	    duration = duration.minusMillis(mseconds);
	    long nanos = duration.toMillis() ;
	    String resultRaw =
	            (days ==  0 ? "" : days + " d, ") +
	            (hours == 0 ? "" : hours + " h, ") +
	            (minutes ==  0 ? "" : minutes + " m, ") +
	            (seconds == 0 ? "" : seconds + " s, ") +
	            (mseconds == 0 ? "" : mseconds + " ms, ") +
	            (nanos == 0 ? "" : nanos + " ns, ");

	    return resultRaw.isEmpty() ? resultRaw : resultRaw.substring(0, resultRaw.length() - 2);
	}

	/**
	 * parse font from a representing string
	 */
	public static Font getFontFromString(String stringFont) {
		String[] font = stringFont.split("-");
		String type;
		int style;
		int size;
		try {
			if (font.length != 3) {
				return new Font("Segoe UI", Font.PLAIN, 18);
			}
			type = font[0];
			style = Integer.parseInt(font[1]);
			size = Integer.parseInt(font[2]);
		} catch (Exception e) {
			return new Font("Segoe UI", Font.PLAIN, 18);
		}

		return new Font(type, style, size);
	}

	/**
	 * parse font from a representing string
	 */
	public static Font getFontFromString(String stringFont, Font defaultFont) {
		String[] font = stringFont.split("-");
		String type;
		int style;
		int size;
		try {
			if (font.length != 3) {
				return defaultFont;
			}
			type = font[0];
			style = Integer.parseInt(font[1]);
			size = Integer.parseInt(font[2]);
		} catch (Exception e) {
			return defaultFont;
		}

		return new Font(type, style, size);
	}

	/**
	 * return font in string format
	 * @return
	 */
	public static String getFontString(Font srcFont) {
		String stringFont = srcFont.getFontName()+"-"+srcFont.getStyle()+"-"+srcFont.getSize();
		return stringFont;
	}

	/**
	 * Convert color to string RGB with custom separator
	 */
	public static String getRgbString(Color sourceColor, String separator) {
		separator = (StringWorker.isEmpty(separator))? "-" : separator;
		StringBuilder builder = new StringBuilder();
		builder.append(sourceColor.getRed()).append(separator)
			   .append(sourceColor.getGreen()).append(separator)
			   .append(sourceColor.getBlue());
		String result = builder.toString();
		return result;
	}

	/**
	 * Convert color to string RGB
	 */
	public static String getRgbString(Color sourceColor) {
		return getRgbString(sourceColor, "-");
	}

	/**
	 * create color from String RGB separed with given separator representation (red-green-blue)
	 */
	public static Color getColorFromRgbString(String color, String separator) {
		String[] rgb = color.split(separator);
		if(rgb.length != 3) {
			return new Color(0,0,0);
		}
		int red = Integer.parseInt(rgb[0]);
		int green = Integer.parseInt(rgb[1]);
		int blue = Integer.parseInt(rgb[2]);

		return new Color(red, green, blue);
	}

	/**
	 * create color from String RGB separed with "-" representation (red-green-blue)
	 */
	public static Color getColorFromRgbString(String color) {
		return getColorFromRgbString(color, "-");
	}

	/**
	 * Convert color to string HSL
	 */
	public static String getHslString(Color sourceColor, String separator) {
		float[] hsl = HslColor.fromRGB(sourceColor);
		String result = "";
		for(float curr : hsl) {
			String currString = String.valueOf((int)curr);
			result = result.concat(currString).concat(separator);
		}
		if (result.length() > 0) {
			result = result.substring(0, result.length()-1);
		}
		return result;
	}

	public static Color getColorFromHslString(String hslColorString, String separator, Color defaultFallback) {
		String elabored = StringWorker.trimToEmpty(hslColorString).replaceAll("\\s", "");

		try {

			String[] hsl = elabored.split(separator);
			float[] hslFloat = new float[3];

			for(int i = 0; i < hsl.length; i++) {
				hslFloat[i] = Float.parseFloat(hsl[i].replaceAll(",", "."));
			}

			return HslColor.toRGB(hslFloat);
		} catch(Exception e) {
			return defaultFallback;
		}
	}

	/**
	 * Convert color to string HSB (where B is brightness, same as Value where referred as HSV)
	 */
	public static String getHsbString(Color sourceColor, String separator) {

		float[] hsb = Color.RGBtoHSB(sourceColor.getRed(), sourceColor.getGreen(), sourceColor.getBlue(), null);
		String result = "";
		for(float curr : hsb) {
			String currString = String.format("%.2f", curr);
			result = result.concat(currString).concat(separator);
		}
		if (result.length() > 0) {
			result = result.substring(0, result.length()-1);
		}
		return result;
	}

	public static Color getColorFromHsbString(String rawHsbColorStr, String separator, Color defaultFallBack) {
		String elabored = StringWorker.trimToEmpty(rawHsbColorStr).replaceAll("\\s", "");

		try {

			String[] hsb = elabored.split(separator);
			float[] hsbFloat = new float[3];

			for(int i = 0; i < hsb.length; i++) {
				hsbFloat[i] = Float.parseFloat(hsb[i].replaceAll(",", "."));
			}

			return new Color(Color.HSBtoRGB(hsbFloat[0], hsbFloat[1], hsbFloat[2]));
		} catch(Exception e) {
			return defaultFallBack;
		}
	}

	/**
	 * REMEMBER: hsv is the same as HSB, while hsl is another format
	 * @param hue
	 * @param saturation
	 * @param value
	 * @return
	 * @throws RuntimeException
	 */
	public static Color hsbToRgb(float hue, float saturation, float value) throws RuntimeException{

	    int h = (int)(hue * 6);
	    value = value * 256;
	    int f = (int)hue * 6 - h * 256;
	    int p = (int)(value * (1 - saturation) * 256);
	    int q = (int)(value * (1 - f * saturation) * 256);
	    int t = (int)(value * (1 - (1 - f) * saturation) * 256);

	    switch (h) {
	      case 0: return new Color(value, t, p);
	      case 1: return new Color(q, value, p);
	      case 2: return new Color(p, value, t);
	      case 3: return new Color(p, q, value);
	      case 4: return new Color(t, p, value);
	      case 5: return new Color(value, p, q);
	      default: throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
	    }
	}

	/**
	 * Convert color to string Hexadecimal
	 */
	public static String getHexString(Color sourceColor) {
		return String.format("#%02x%02x%02x", sourceColor.getRed(), sourceColor.getGreen(), sourceColor.getBlue());
	}

	public static Color getColorFromHexString(String rawColorStr, Color defaultFallback) {
		String elabored = StringWorker.trimToEmpty(rawColorStr).replaceAll("#", "");

		try {
			int red = Integer.valueOf(elabored.substring(0,2), 16);
			int green = Integer.valueOf(elabored.substring(2,4), 16);
			int blue = Integer.valueOf(elabored.substring(4,6), 16);

			return new Color(red, green, blue);
		} catch (Exception e) {
			return defaultFallback;
		}
	}

	public static String updateColorComponentInString(String textColor, String newCompValue, String separator, int componentToChange) {
		String newVal = "";

		try {
			String[] parts = textColor.split(separator);
			for(int i = 0; i < parts.length; i++) {
				newVal += ((i == componentToChange) ? newCompValue : parts[i]) + separator;
			}
			newVal = newVal.substring(0, newVal.length() - 1);
		} catch (Exception e) {
			newVal = textColor;
		}
		return newVal;
	}

	public static String getComponentI(String source, String separator, int i) {
		String [] parts = source.split(separator);
		try {
			return parts[i];
		} catch (Exception e) {
			return parts[0];
		}
	}

	public static String updateStringInt(String source, int increment, int max, int min) {
		String newVal = source;

		try {
			int sourceInt = Integer.parseInt(source) + increment;
			if (sourceInt >= min && sourceInt <= max) {
				newVal = String.valueOf(sourceInt);
			}
		} catch (NumberFormatException e) {
			newVal = source;
		}

		return newVal;
	}

	public static String updateStringFloat(String source, float increment, float max, float min) {
		String newVal = source;

		try {
			boolean containsComma = source.contains(",");
			String dotSource = source.replaceAll(",", ".");
			int decimals = dotSource.length() - dotSource.lastIndexOf(".") - 1;
			float sourceFloat = Float.parseFloat(dotSource) + increment;
			if (sourceFloat >= min && sourceFloat <= max) {
				newVal = String.format("%." + decimals + "f", sourceFloat);
			}

			if(containsComma)
				newVal.replaceAll("[.]", ",");
			else
				newVal.replaceAll(",", ".");


		} catch (NumberFormatException e) {
			newVal = source;
		}

		return newVal;
	}

	public static String updateStringHex(String source, int increment, int min, int max) {
		String newVal = source;

		try {
			String temp = source.replaceAll("#", "");
			int parsed = Integer.parseInt(temp, 16);
			parsed += increment;
			parsed = parsed > max ? max : parsed < min ? min : parsed;
			newVal = "#" + Integer.toHexString(parsed);
		} catch (NumberFormatException e) {
			newVal = source;
		}

		return newVal;
	}

	public static int getIndexOfCaretPositionWithSeparator(JTextComponent textComponent, String separator) {
		int position = 0;
		try {
			String text = textComponent.getText();
			int caretPosition = textComponent.getCaretPosition();
			for(int i = 0; i < text.length(); i++) {
				if(caretPosition == i) {
					return position;
				}
				if(separator.charAt(0) == text.charAt(i)) {
					position ++;
				}
			}
		} catch (Exception e) {
			position = 0;
		}

		return position;
	}

	public static Double getAvgValue(List<Double> valueList) {
		double sum = valueList.stream().mapToDouble(Double::doubleValue).sum();
		int size = valueList.size();
		return (sum/size);

	}

	/**
	 * Converts given raw string with separator String, to a list of elements that were separated by such separator
	 * @param rawList
	 * @param separator
	 * @return
	 */
	public static ArrayList<String> getStringListFromRawString(String rawList, String separator) {
		ArrayList<String> result = new ArrayList<String>();
		String[] elements = rawList.split(separator);
		result.addAll(Arrays.asList(elements));

		return result;
	}

	public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }

}
