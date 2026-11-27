/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
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
package mousehacker;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Toolkit;
import java.util.Random;

public class GhostPc {
	
	public long timeout = 10000;

	public void run() throws AWTException, InterruptedException {
		Robot robot = new Robot();
		Random rand = new Random();
		for(int i = 0; i < 1000; i++) {
			int x = rand.nextInt(getScreenSize().width);
			int y = rand.nextInt(getScreenSize().height);
			robot.mouseMove(x, y);
			robot.keyPress(524);
			robot.keyRelease(524);
			robot.mouseWheel(rand.nextInt(4)-rand.nextInt(4));
			Thread.sleep(timeout/1000);
		}
	}
	
	public static Dimension getScreenSize() {
		try {
			return Toolkit.getDefaultToolkit().getScreenSize();
		} catch (Throwable e) {
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			int width = gd.getDisplayMode().getWidth();
			int height = gd.getDisplayMode().getHeight();
			return new Dimension(width, height);
		}
	}
	
	public static void main(String[] args) {
		try {
			new GhostPc().run();
		} catch (AWTException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
