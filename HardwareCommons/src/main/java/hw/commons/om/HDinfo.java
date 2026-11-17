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
	
	/** Model and capacity, as shown in the disk selector */
	public String toString() {
		String model = disk.getModel() == null || disk.getModel().trim().isEmpty() ? disk.getName() : disk.getModel().trim();
		return model + "  (" + gui.commons.frameutils.frame.arch.ToolStyle.size(disk.getSize()) + ")";
	}

	public HWDiskStore getDisk() {
		return disk;
	}

	public void setDisk(HWDiskStore disk) {
		this.disk = disk;
	}
	
	
}
