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
package hw.commons.om;

import oshi.hardware.HWDiskStore;

public class HDinfo {
	
	private HWDiskStore disk;
	
	public HDinfo(HWDiskStore disk) {
		this.disk = disk;
	}
	
	public String toString() {
		int lengthMax = 18;
		String outName = (disk.getModel().length()>lengthMax)? disk.getModel().substring(0,lengthMax-1)+"...": disk.getModel() ;
		return outName;
	}

	public HWDiskStore getDisk() {
		return disk;
	}

	public void setDisk(HWDiskStore disk) {
		this.disk = disk;
	}
	
	
}
