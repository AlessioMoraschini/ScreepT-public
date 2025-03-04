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
package updater.module.exception;

public class UpdatesUnavailableException extends Exception {
	private static final long serialVersionUID = 949927883559988397L;

	public UpdatesUnavailableException() {
		super();
	}
	
	public UpdatesUnavailableException(String message) {
		super(message);
	}
}
