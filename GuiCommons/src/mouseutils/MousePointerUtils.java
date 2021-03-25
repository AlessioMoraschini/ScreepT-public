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
package mouseutils;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import various.common.light.utility.log.SafeLogger;

public class MousePointerUtils {
	
	// mainGUI logger
	static SafeLogger logger = new SafeLogger(MousePointerUtils.class);

	public Robot robot;
	public AWTEventListener mouseListener;
	public volatile Color pixelCurrColor;
	Runnable onClickAction;
	Thread actionClickThread;
	MousePointerCaller owner;
	public Component disabledTarget;
	
	public AtomicBoolean enabled = new AtomicBoolean(false);
	
	public MousePointerUtils(Runnable onClickAction, MousePointerCaller owner, Component disabledTarget) {
		this.onClickAction = (onClickAction != null)? onClickAction : () -> {};
		this.owner = owner;
		this.disabledTarget = disabledTarget;
		
		try {
			mouseListener = addMouseListener();
			robot = new Robot();
		} catch (AWTException e) {
			logger.error(e);
		}
	}
	
	/**
	 * Retrieve clicked () and after disable listener (to re enable just call addMouseListener())
	 * @return
	 */
	public synchronized Color getCurrentLoadedPXColor() {
		return pixelCurrColor;
	}

	public synchronized AWTEventListener addMouseListener() {
		enabled.set(true);
		
		AWTEventListener listener = new AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				
				if(!enabled.get()) {return;}
				
				Point currentPos = MouseInfo.getPointerInfo().getLocation();
				Point absTargPos = disabledTarget.getBounds().getLocation();

				try {
					absTargPos = disabledTarget.getLocationOnScreen();
				}catch(Exception e) {}
				
				Rectangle absoluteBounds = new Rectangle(
						absTargPos.x,
						absTargPos.y,
						disabledTarget.getWidth(),
						disabledTarget.getHeight()
					);
				boolean hoverExcludedComponent = absoluteBounds.contains(currentPos);
				
				
				if (event instanceof KeyEvent) {
					KeyEvent keyEvent = (KeyEvent) event;
					
					if (keyEvent.isControlDown() && keyEvent.isShiftDown()) {
						if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
							logger.debug(MouseInfo.getPointerInfo().getLocation() + " | ");
							logger.debug(event);
							pixelCurrColor = robot.getPixelColor(currentPos.x, currentPos.y);
							onClickAction.run();
						}
					}
				}

				if(hoverExcludedComponent) {return;}
				
				if (event instanceof MouseEvent) {
					MouseEvent me = (MouseEvent)event;
					
					if(me.getID() == MouseEvent.MOUSE_PRESSED && me.getButton() == MouseEvent.BUTTON1) {
						logger.debug(MouseInfo.getPointerInfo().getLocation() + " | ");
						logger.debug(event);
						
						pixelCurrColor = robot.getPixelColor(currentPos.x, currentPos.y);
						onClickAction.run();

					}
				} else if (event instanceof FocusEvent) {
					if(!MousePointerCaller.hasFocus.get()) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								if(owner instanceof Component) {
									((Component)owner).requestFocus();
									if(owner instanceof JFrame) {
										((JFrame)owner).toFront();
									}
								}
							}
						});
					}
					
					logger.debug(MouseInfo.getPointerInfo().getLocation() + " | ");
					logger.debug(event);
					
					pixelCurrColor = robot.getPixelColor(currentPos.x, currentPos.y);
					onClickAction.run();
					
				} 
			}
		};
		
		Toolkit.getDefaultToolkit().addAWTEventListener(listener, 
				AWTEvent.KEY_EVENT_MASK |
				AWTEvent.MOUSE_EVENT_MASK | 
				AWTEvent.FOCUS_EVENT_MASK);
		
		return listener;
	}
	
	public synchronized void removeMouseListener() {
		Toolkit.getDefaultToolkit().removeAWTEventListener(mouseListener);
		enabled.set(false);
	}

	public Runnable removeMouseListenerRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				removeMouseListener();
			}
		};
	}
	
}
