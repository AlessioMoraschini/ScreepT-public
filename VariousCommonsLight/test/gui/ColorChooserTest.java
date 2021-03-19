package gui;

import java.awt.Color;

import org.junit.Test;

import gui.dialogs.color.ColorDialogHelper;
import gui.dialogs.color.ColorChooserGrid.UserChoice;
import utils.TestUtils;

public class ColorChooserTest {

	@Test
	public void testGridChooser() {
		GUIStyelSettings.setDarkNimbus(1);
		UserChoice userChoice = ColorDialogHelper.askForColorGrid(Color.YELLOW);
		
		TestUtils.printl("Selection: " + userChoice.name() + " : selection color:" + userChoice.getChosenColor());
	}
}
