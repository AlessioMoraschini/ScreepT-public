package gui;

import java.io.File;

import various.common.light.gui.LightLicenseViewer;

public class LicenseAgreementTest {

	public static void main(String args[]) {

		LightLicenseViewer.mandatoryChoice = false;
		LightLicenseViewer.setMAIN_LIBRARIES_FOLDER(new File("./TEST_FILES/licenses/Libraries_licenses"));
		LightLicenseViewer.setMAIN_LIBRARIES_FOLDER_DETAIL(new File("./TEST_FILES/licenses/Libraries_licenses/detailedReport"));
		LightLicenseViewer.getInstance(new File("TEST_FILES/licenses/License_ScreepT.html"), "Here is the EULA (End User License Agreement)", "ScreepT - License (EULA)",
				new boolean[] {true, true, true});

	}
}
