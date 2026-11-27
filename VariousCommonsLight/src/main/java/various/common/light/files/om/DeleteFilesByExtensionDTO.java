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
package various.common.light.files.om;

import java.util.ArrayList;

public class DeleteFilesByExtensionDTO {
	private boolean esit;
	private boolean nothingFound;
	private ArrayList<String> notFoundExtensionResult;
	
	public DeleteFilesByExtensionDTO() {
		esit = false;
		notFoundExtensionResult = new ArrayList<String>();
		nothingFound = true;
	}

	
	public boolean isNothingFound() {
		return nothingFound;
	}

	public void setNothingFound(boolean nothingFound) {
		this.nothingFound = nothingFound;
	}

	public boolean isEsit() {
		return esit;
	}

	public void setEsit(boolean esit) {
		this.esit = esit;
	}

	public ArrayList<String> getNotFoundExtensionResult() {
		return notFoundExtensionResult;
	}

	public void setNotFoundExtensionResult(ArrayList<String> notFoundExtensionResult) {
		this.notFoundExtensionResult = notFoundExtensionResult;
	}
}
