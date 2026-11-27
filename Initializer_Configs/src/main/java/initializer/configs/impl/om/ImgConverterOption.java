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
package initializer.configs.impl.om;

import various.common.light.om.ScalingAlgorithm;

public class ImgConverterOption {

	// DEFAULT VALUES (usfeul to keep valid object during iniLoad, in case of null read or exception)
	public static final boolean DEFAULT_SAME_SIZE = true;
	public static final boolean DEFAULT_SAME_EXT = true;
	public static final int DEFAULT_QUALITY = 100;
	public static final ScalingAlgorithm DEFAULT_ALGORITHM = ScalingAlgorithm.REPLICATE;
	
	// FIELDS	
	public boolean sameSize;
	public boolean sameExtension;
	public int quality;
	public ScalingAlgorithm algorithm;
	
	// CONSTRUCTOR
	public ImgConverterOption() {
		sameSize = DEFAULT_SAME_SIZE;
		sameExtension = DEFAULT_SAME_EXT;
		quality = DEFAULT_QUALITY;
		algorithm = DEFAULT_ALGORITHM;
	}

	// GETTERS AND SETTERS

	public boolean isSameSize() {
		return sameSize;
	}
	
	public void setSameSize(boolean sameSize) {
		this.sameSize = sameSize;
	}
	
	public boolean isSameExtension() {
		return sameExtension;
	}
	
	public void setSameExtension(boolean sameExtension) {
		this.sameExtension = sameExtension;
	}
	
	public int getQuality() {
		return quality;
	}
	
	public void setQuality(int quality) {
		this.quality = quality;
	}
	
	public ScalingAlgorithm getAlgorithm() {
		return algorithm;
	}
	
	public void setAlgorithm(ScalingAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
}
