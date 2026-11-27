/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package gui.commons.utils;

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

import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ImageWorker;

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
