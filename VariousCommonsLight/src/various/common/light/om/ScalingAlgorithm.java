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
