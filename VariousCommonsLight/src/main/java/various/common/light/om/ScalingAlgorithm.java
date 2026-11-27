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
package various.common.light.om;

import java.awt.Image;

public enum ScalingAlgorithm {DEFAULT(Image.SCALE_DEFAULT, "Choose an image-scaling algorithm that gives higher priority to image smoothness than scaling speed."),
							  FAST(Image.SCALE_FAST, "Choose an image-scaling algorithm that gives higher priority to scaling speed than smoothness of the scaled image."),
							  AVERAGING(Image.SCALE_AREA_AVERAGING,"Use the Area Averaging image scaling algorithm.\n The image object is free to substitute a different filter that performs the same algorithm yet integrates more efficiently into the image " ),
							  REPLICATE(Image.SCALE_REPLICATE,"Use the image scaling algorithm embodied in the ReplicateScaleFilter class.\n The Image object is free to substitute a different filter that performs the same algorithm yet integrates more efficiently into the imaging infrastructure "),
							  SMOOTH(Image.SCALE_SMOOTH, "Choose an image-scaling algorithm that gives higher priority to image smoothness than scaling speed.");

	private int algorithm;
	private String description;
	
	private ScalingAlgorithm(int algo, String descr) {
		algorithm = algo;
		description = descr;
	}
	
	public static boolean isValidString(String algoName) {
		for(ScalingAlgorithm algo : ScalingAlgorithm.values()) {
			if(algoName.equals(algo.toString())) {
				return true;
			}
		}
		return false;
	}

	public int getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(int algorithm) {
		this.algorithm = algorithm;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
