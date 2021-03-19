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
package om;

import java.awt.image.BufferedImage;

public enum BufferedImageType {THREE_BYTE_BGR("3BYTE RGB", BufferedImage.TYPE_3BYTE_BGR,3, "Represents an image with 8-bit RGB color components, corresponding to a Windows-style BGR color model) with the colors Blue, Green, and Red stored in 3 bytes. There is no alpha. When data with non-opaque alpha is stored in an image of this type"), 
							   FOUR_BYTE_ABGR_PRE("4BYTE ABGR (Pre-multiplied)", BufferedImage.TYPE_4BYTE_ABGR_PRE,4, "Represents an image with 8-bit RGBA color components with the colors Blue, Green, and Red stored in 3 bytes and 1 byte of alpha. The image has a ComponentColorModel with alpha."), 
							   FOUR_BYTE_ABGR("4 BYTE ABGR", BufferedImage.TYPE_4BYTE_ABGR,4, "Represents an image with 8-bit RGBA color components with the colors Blue, Green, and Red stored in 3 bytes and 1 byte of alpha. The image has a ComponentColorModel with alpha."),
							   INT_ABGR("INT ARGB", BufferedImage.TYPE_INT_ARGB,4, "Represents an image with 8-bit RGBA color components packed into integer pixels. The image has a DirectColorModel with alpha."),
							   INT_ARGB_PRE("INT ARGB (Pre-multiplied)", BufferedImage.TYPE_INT_ARGB_PRE,4, "Represents an image with 8-bit RGBA color components packed into integer pixels. The image has a DirectColorModel with alpha. "),
							   INT_RGB("INT RGB", BufferedImage.TYPE_INT_RGB,3,"Represents an image with 8-bit RGB color components packed into integer pixels. The image has a DirectColorModel without alpha. " ),
							   INT_BGR("INT BGR", BufferedImage.TYPE_INT_BGR,3, "Represents an image with 8-bit RGB color components, corresponding to a Windows- or Solaris- style BGR color model, with the colors Blue, Green, and Red packed into integer pixels. There is no alpha"),
							   USHORT_GRAY("USHORT_GRAY", BufferedImage.TYPE_USHORT_GRAY,2, "Represents an unsigned short grayscale image, non-indexed)."),
							   USHORT_565_RGB("USHORT_565_RGB", BufferedImage.TYPE_USHORT_565_RGB,2, "Represents an image with 5-6-5 RGB color components (5-bits red, 6-bits green, 5-bits blue) with no alpha. This image has a DirectColorModel. ");
							   
	private int codice;
	private int multiplicator;
	private String nomeStringa;
	private String description;
	
	BufferedImageType(String nome, int type, int mult, String descr) {
		this.codice = type;
		this.nomeStringa = nome;
		multiplicator=mult;
		description = descr;
	}

	/**
	 * @return the type of image described in bufferedImage
	 */
	public int getType() {
		return codice;
	}


	public int getMultiplicator() {
		return multiplicator;
	}

	public String getNomeStringa() {
		return nomeStringa;
	}

	public String getDescription() {
		return description;
	}
	
	/**
	 * this method check if the given type is supported for code/decode with ImageCodeDecode class
	 * @param imageType the given type to check
	 * @return true if supported, false if not.
	 */
	public static boolean checkIfAdmittedType(int imageType) {
		for(BufferedImageType current : values()) {
			if(current.getType() == imageType) {
				return true;
			}
		}
		return false;
	}
}
