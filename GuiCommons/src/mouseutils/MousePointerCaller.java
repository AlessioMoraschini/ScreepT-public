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

import java.awt.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface MousePointerCaller {

	public AtomicBoolean hasFocus = new AtomicBoolean(false);
	
	public List<Component> getDisabledElements();
	
}
