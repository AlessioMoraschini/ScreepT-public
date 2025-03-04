package gui;

import java.awt.Color;

import org.junit.Ignore;
import org.junit.Test;

import test.util.utils.TestUtils;
import various.common.light.gui.GUIStyelSettings;
import various.common.light.gui.dialogs.color.ColorDialogHelper;
import various.common.light.gui.dialogs.color.ColorChooserGrid.UserChoice;

public class ColorChooserTest {

	@Test
	@Ignore("manual test")
	public void testGridChooser() {
		GUIStyelSettings.setDarkNimbus(1);
		UserChoice userChoice = ColorDialogHelper.askForColorGrid(Color.YELLOW);
		
		TestUtils.printl("Selection: " + userChoice.name() + " : selection color:" + userChoice.getChosenColor());
	}
}
