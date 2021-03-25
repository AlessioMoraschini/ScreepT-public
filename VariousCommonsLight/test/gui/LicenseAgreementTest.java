package gui;

import java.io.File;

import various.common.light.gui.LightLicenseViewer;

public class LicenseAgreementTest {

	public static void main(String args[]) {
		
		LightLicenseViewer.setMAIN_LIBRARIES_FOLDER(new File("TEST_FILES/licenses/Libraries_licenses"));
		LightLicenseViewer.mandatoryChoice = false;
		LightLicenseViewer.getInstance(new File("TEST_FILES/licenses/License_ScreepT.html"), "Here is the EULA (End User License Agreement)", "ScreepT - License (EULA)");

	}
}
