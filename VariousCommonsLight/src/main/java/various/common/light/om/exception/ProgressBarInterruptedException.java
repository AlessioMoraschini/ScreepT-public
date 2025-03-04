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
package various.common.light.om.exception;

public class ProgressBarInterruptedException extends Exception{
	private static final long serialVersionUID = -6742493294534645837L;

	public ProgressBarInterruptedException(String msg){
		super(msg);
	}

	public ProgressBarInterruptedException(String msg, Exception exc){
		super(msg);
		this.initCause(exc);
	}

}
