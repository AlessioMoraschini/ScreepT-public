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
package multimedia.conversion;

import java.io.File;

import org.apache.log4j.Logger;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import resources.GeneralConfig;

public class ImageToTextExtractor {
	
	// ImageToTextExtractor logger creation
	static Logger logger = Logger.getLogger(ImageToTextExtractor.class);
				
	private ITesseract instanceOfExporter;
	private String cachedText;
	
	/**
	 * Uses default tessdata path = GeneralConfig.RESOURCES_DIR  + "lib/tessdata"
	 */
	public ImageToTextExtractor() {
		instanceOfExporter = new Tesseract();
		instanceOfExporter.setDatapath(GeneralConfig.RESOURCES_DIR  + "lib/tessdata");
		cachedText = "";
	}
	
	/**
	 * Use custom tessDataFolderPath where you put the folder for configuration
	 * @param tessDataFolderPath
	 */
	public ImageToTextExtractor(String tessDataFolderPath) {
		instanceOfExporter = new Tesseract();
		instanceOfExporter.setDatapath(tessDataFolderPath);
		cachedText = "";
	}

	public String readTextFromImage(File imageFile) throws TesseractException {
		
		String result = "";
		
		result = instanceOfExporter.doOCR(imageFile);
		cachedText = result;
		
		return result;
	}

	public ITesseract getInstanceOfExporter() {
		return instanceOfExporter;
	}

	public void setInstanceOfExporter(Tesseract instanceOfExporter) {
		this.instanceOfExporter = instanceOfExporter;
	}

	public String getCachedText() {
		return cachedText;
	}
	
}
