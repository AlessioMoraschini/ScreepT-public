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
package network.progress;

public interface UploadAction {
	public default void doAfterUploadProgress(int percent, String currentFile) {
		System.out.println("Uploading File: " + currentFile + "Current progress is " + percent);
	};
}
