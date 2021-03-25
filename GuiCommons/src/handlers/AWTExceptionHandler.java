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
package handlers;

import javax.swing.SwingUtilities;

import arch.InitOwner;
import dialogutils.JOptionHelperExtended;
import resources.GeneralConfig;
import resources.SoundsConfigurator;
import resources.SoundsManagerExtended;
import runtime.IParametricRunnable;
import runtime.ParametricExceptionThread;
import utility.log.SafeLogger;

public class AWTExceptionHandler implements Thread.UncaughtExceptionHandler{
	
	static SafeLogger logger = new SafeLogger(AWTExceptionHandler.class);
		
	public volatile static int N_OF_DIALOG_OPENED = 0;

	public static final Object[] options = new Object[] { "Kill Application", "Continue" };
	
	public static final String ERROR_FILE_REASONS = " reasons could be that file does not exist or contains errors ";
	public static final String CRITICAL_ERROR_MSG = "Critical error spotted: it's possible that Application won't behave normally, until next restart.\n ";
	public static String DIALOG_USER_ERR_INFO;
	
	IParametricRunnable doAfterError;
	Thread doAfterErrorThread;
	InitOwner initOwner;
	
	public static void init() {
		DIALOG_USER_ERR_INFO = "You can export Logs going under help menu -> export logs: export them on your pc and send them to the mail ("+GeneralConfig.MAIL_BUGS
				+") if you want to signal this problem. You can find more infos on: " + GeneralConfig.WEBSITE + "\n\n";
	}
	
	/**
	 * When an exception will be catched, if exception is critical then parametric runnable will be launched (parametricRun method invoked)
	 * @param doAfterError
	 */
	public AWTExceptionHandler(IParametricRunnable doAfterError, InitOwner initOwner) {
		this.doAfterError = doAfterError;
		this.initOwner = initOwner;
		
		registerExceptionHandler();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				while (true) {
					try {
						// Ensure that will never be impossible to notify cause of stuck number over maximum
						// every K seconds decrease current N_OF_DIALOGS_OPENED
						decreaseCount();
						Thread.sleep(GeneralConfig.AWT_EVENT_CHECKER_DECREASE_LIMIT_PERIOD);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} 
				}
			}
		});
	}
	
	public boolean checkAllowedAndUpdCount() {
		boolean isOk = N_OF_DIALOG_OPENED < GeneralConfig.MAX_N_OF_ERR_DIALOG_OPENED;
		N_OF_DIALOG_OPENED = (isOk)? N_OF_DIALOG_OPENED + 1 : N_OF_DIALOG_OPENED; 
		// avoid N times overflow... max is one more than max value
		N_OF_DIALOG_OPENED = (N_OF_DIALOG_OPENED >= GeneralConfig.MAX_N_OF_ERR_DIALOG_OPENED)? GeneralConfig.MAX_N_OF_ERR_DIALOG_OPENED+1 : N_OF_DIALOG_OPENED;
		return isOk;
	}
	
	public synchronized void decreaseCount() {
		// avoid negative values
		if(N_OF_DIALOG_OPENED >= 0) {
			N_OF_DIALOG_OPENED -= 1;
		}
	}
	
	public void handle(Throwable t) {

		logger.error("AWT UNHANDLED EXCEPTION DETECTED: ", t);
		
		if(checkAllowedAndUpdCount()) {
			
			
			try {
			
				// Default: continue
				Object res = options[1];
				
				if (ParametricExceptionThread.isCriticalError(t)) {
					SoundsManagerExtended.playSound(SoundsConfigurator.ERROR, null);

					String outOfMemoryPreamble = t instanceof OutOfMemoryError ? 
							"Not Enough RAM memory allocated to this process, to solve this you can increase the max memory option in launcher panel.\n\n" : "";
					res = new JOptionHelperExtended(null).errorScroll(
							outOfMemoryPreamble + CRITICAL_ERROR_MSG + DIALOG_USER_ERR_INFO + t.getClass().getCanonicalName(), 
							"Critical Error",
							t, 
							options, 
							JOptionHelperExtended.YES_OR_NO);
					
				}else{
					if (initOwner.getInitializer().getGenOpt().isExceptionAdvicesEnabled()) {
						SoundsManagerExtended.playSound(SoundsConfigurator.SOUND_PLING, null);
						res = new JOptionHelperExtended(null).warnScroll(
								"An Exception occurred, but I'm still here ;) "
										+ "\n\n(if you don't want to see this message you can disable it going to preferences->general options and deselecting the related checkbox)\n\n => PRESS \"Kill Application\" IF YOU WANT TO CLOSE, TRYING TO SAVE CURRENT WORK\n\n =>"
										+ DIALOG_USER_ERR_INFO + t.getClass().getCanonicalName(),
								"An Exception Occurred", t, options, JOptionHelperExtended.YES_OR_NO);
					}else {
						// default continue on exception
						res = options[1];
					}
					
				}
				
				// IF not kill then return back, Else call the parametricRun with current exception
				if (options[0].equals(res)) {
					logger.error("AWT UNHANDLED EXCEPTION DETECTED: ", t);
					doAfterError.parametricRun(t);
				}
	
				
			} catch (Throwable t1) {
				// DON'T let the exception go out of here... it could cause loops
				
			}finally {
				decreaseCount();
			}

		} else {
			System.out.println("MAX NUMBER OF AWT ERROR MANAGERS REACHED: Cannot show more advices until current is closed");
		}
	}

	public void registerExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler(this);
	    System.setProperty("sun.awt.exception.handler", AWTExceptionHandler.class.getName());
	  }

	@Override
	public void uncaughtException(Thread t, final Throwable e) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				handle(e);
			}
		});
	}
}
