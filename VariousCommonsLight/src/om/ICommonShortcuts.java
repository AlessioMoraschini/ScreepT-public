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
package om;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import utility.string.StringWorker.EOL;

public interface ICommonShortcuts {

	public boolean isCtrlDown();
	public boolean isShiftDown();
	public boolean isAltDown();
	
	public void revalidateUnsaved();
	
	/**
	 * default line terminator will be used
	 */
	public void saveAsShortcut();
	
	/**
	 * If line terminator is null then default one will be used
	 */
	public void saveAsShortcut(EOL lineTerminator);
	
	public static void triggerKeyEvent(boolean press, boolean release, boolean ctrl, boolean alt, boolean shift, int keyCode) throws AWTException {
		
		Robot robot = new Robot();
		
		pressModifier(robot, true, ctrl, alt, shift);
		
		if(press)
			robot.keyPress(keyCode);
		
		if(release)
			robot.keyRelease(keyCode);
		
		pressModifier(robot, false, ctrl, alt, shift);
	};
	
	public static void pressModifier(Robot robot, boolean press, boolean ctrl, boolean alt, boolean shift) {
		if(press) {
			if(ctrl)
				robot.keyPress(KeyEvent.VK_CONTROL);
			if(alt)
				robot.keyPress(KeyEvent.VK_ALT);
			if(shift)
				robot.keyPress(KeyEvent.VK_SHIFT);
		} else {
			if(ctrl)
				robot.keyRelease(KeyEvent.VK_CONTROL);
			if(alt)
				robot.keyRelease(KeyEvent.VK_ALT);
			if(shift)
				robot.keyRelease(KeyEvent.VK_SHIFT);
		}
	}
}
