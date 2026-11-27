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
package various.common.light.om;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import various.common.light.utility.string.StringWorker.EOL;

public interface ICommonShortcuts {

	public boolean isCtrlDown();
	public boolean isShiftDown();
	public boolean isAltDown();

	public void revalidateUnsaved();

	/**
	 * default line terminator will be used
	 */
	public void saveAsShortcut();

	public default boolean areShortcutsEnabled(ShortcutsEnabler enabler) {
		return enabler == null || enabler.shortcutsEnabled();
	};

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

	@FunctionalInterface
	public interface ShortcutsEnabler {
		public boolean shortcutsEnabled();
	}
}
