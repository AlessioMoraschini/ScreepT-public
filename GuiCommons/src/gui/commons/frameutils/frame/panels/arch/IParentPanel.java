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
package gui.commons.frameutils.frame.panels.arch;

import java.io.IOException;

public interface IParentPanel{

	public void sync();
	public void reload(String target) throws Exception;
	public void doAction(String target) throws IOException;
}
