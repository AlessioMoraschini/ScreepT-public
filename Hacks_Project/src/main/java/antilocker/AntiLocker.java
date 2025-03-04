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
package antilocker;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class AntiLocker {
	
	private volatile boolean paused = false;
	private volatile boolean running = false;
	private volatile boolean debugEnabled = false;
	private int period = -1;
	private int xIncrement = 0;
	private int yIncrement = 0;
	private AmTimer timer;
	private Robot robot;
	
	public AntiLocker(int periodMs, int xIncrement, int yIncrement) {
		running = false;
		this.xIncrement = xIncrement;
		this.yIncrement = yIncrement;
		this.period = periodMs;
		try {
			this.robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}

		init(false);
	}
	
	private void init(boolean inverseDirection){
		this.timer = new AmTimer(()->{
				pressAndReleaseKey(KeyEvent.VK_CONTROL);
				if(inverseDirection)
					moveMouse(-xIncrement, -yIncrement);
				else
					moveMouse(xIncrement, yIncrement);
			}, period, debugEnabled);
	}
	
	public void setEnabledDebug(boolean enabled) {
		debugEnabled = enabled;
	}
	
	public void moveMouse(int xIncrement, int yIncrement) {
		if(robot != null) {
			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			robot.mouseMove(mouseLocation.x + xIncrement, mouseLocation.y + yIncrement);
		}
	}
	
	public void pressKey(int keyCode) {
		if(robot != null) {
			robot.keyPress(keyCode);
		}
	}

	public void releaseKey(int keyCode) {
		if(robot != null) {
			robot.keyRelease(keyCode);
		}
	}
	public void pressAndReleaseKey(int keyCode) {
		pressKey(keyCode);
		releaseKey(keyCode);
	}
	
	public Point getMouseLocation() {
		return MouseInfo.getPointerInfo().getLocation();
	}
	
	public boolean abort() {
		if(running) {
			timer.abort();
			running = false;
			paused = false;
			init(false);
			return true;
		}
		return false;
	}
	
	public boolean pause() {
		if(running && !paused) {
			timer.setPaused(true);
			paused = true;
			return true;
		}
		
		return false;
	}
	
	public boolean resume() {
		if(running && paused) {
			timer.setPaused(false);
			paused = false;
			return true;
		}
		
		return false;
	}
	
	public boolean start() {
		if(running || timer.isRunning() || timer.isPaused())
			return false;

		new Thread(() -> {
			running = true;
			int i = 0;
			while(running) {
				init( i%2 == 0 );
				timer.executeDelayedAsync();
				while(running && (paused || (!timer.isExecuted()))) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				i++;
			}
			
			running = false;
		}).start();

		return true;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isRunning() {
		return running;
	}

	public void setxIncrement(int xIncrement) {
		this.xIncrement = xIncrement;
	}

	public void setyIncrement(int yIncrement) {
		this.yIncrement = yIncrement;
	}
	
}
