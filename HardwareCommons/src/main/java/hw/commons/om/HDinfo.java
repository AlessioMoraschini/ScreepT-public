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
