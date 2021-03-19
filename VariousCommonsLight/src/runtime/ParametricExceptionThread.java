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
package runtime;

public class ParametricExceptionThread extends Thread implements IParametricRunnable{
	
	
	public ParametricExceptionThread(Runnable runnable) {
		super(runnable);
	}
	
	/**
	 * Run runnable only if this is not a critical exception (an error)
	 */
	@Override
	public void parametricRun(Throwable throwable) {
		this.start();
	}
	
	public static boolean isCriticalError(Throwable t) {
		boolean exception = t instanceof Exception;
		return (!exception);
	}
}
