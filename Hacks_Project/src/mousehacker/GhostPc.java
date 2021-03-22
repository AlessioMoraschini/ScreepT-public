/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com/
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
