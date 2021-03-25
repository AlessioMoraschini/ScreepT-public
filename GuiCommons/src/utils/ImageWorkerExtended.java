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
package utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.googlecode.pngtastic.core.PngChunk;
import com.googlecode.pngtastic.core.PngImage;
import com.googlecode.pngtastic.core.PngOptimizer;

import utility.log.SafeLogger;
import utility.manipulation.ImageWorker;

public class ImageWorkerExtended extends ImageWorker {

	// ImageWorker logger creation
	public static SafeLogger logger = new SafeLogger(ImageWorkerExtended.class);

	//////////////////////
	// CONSTRUCTOR // NB il file sorgente deve esistere, altrimenti lancia
	////////////////////// eccezione!
	//////////////////////

	public ImageWorkerExtended(String imgPath) throws IOException {
		super(imgPath);
	}

	public ImageWorkerExtended(File imgFile) throws IOException {
		super(imgFile);
	}

	public ImageWorkerExtended(BufferedImage img) {
		super(img);
	}

	//////////////////////
	// METHODS //
	//////////////////////

	
	/**
	 * 
	 * @param source source image file (could be every kind of source)
	 * @param compFactor the higher and more compressed is the result : must be a %
	 * @param removeGamma flag=true : gamma will be removed
	 * 
	 * @return byte array containing compressed byte result as array
	 * @throws IOException
	 */
	public static byte[] compressIMGpng(BufferedImage source, int compFactor, boolean removeGamma) throws IOException {

		// write temp png byte array
	    ByteArrayOutputStream tmpBytes = new ByteArrayOutputStream();
	    ImageIO.write(source, "png", tmpBytes);
	    tmpBytes.close();
	    
	    // compress
	    PngImage pngImage = new PngOptimizer().optimize(new PngImage(new ByteArrayInputStream(tmpBytes.toByteArray())), removeGamma, compFactor);
	    
	    // extract compressed data as png
	    ByteArrayOutputStream compressedPngData = new ByteArrayOutputStream();
	    DataOutputStream outputStreamWrapper = new DataOutputStream(compressedPngData);
	    outputStreamWrapper.writeLong(PngImage.SIGNATURE);
	    for (PngChunk chunk : pngImage.getChunks()) {
	        outputStreamWrapper.writeInt(chunk.getLength());
	        outputStreamWrapper.write(chunk.getType());
	        outputStreamWrapper.write(chunk.getData());
	        int i = (int) chunk.getCRC();
	        outputStreamWrapper.writeInt(i);
	    }
	    outputStreamWrapper.close();
	    compressedPngData.close();
	    
	    // convert to png byte array
	    return compressedPngData.toByteArray();
	}
	
	/**
	 * 
	 * @param source source image file (could be every kind of source)
	 * @param compFactor the higher and more compressed is the result : must be a %
	 * @param removeGamma flag=true : gamma will be removed
	 * 
	 * @return byte array containing compressed byte result as array
	 * @throws IOException
	 */
	public static byte[] compressIMGpngLossy(BufferedImage source, float compressFactor, boolean removeGamma) throws IOException {
		// TODO
		return null;
	}

}
