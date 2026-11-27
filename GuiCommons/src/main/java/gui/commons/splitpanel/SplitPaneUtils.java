/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
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
package gui.commons.splitpanel;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import various.common.light.utility.reflection.ReflectionUtils;


public class SplitPaneUtils {

	public static final Color defaultBackgroundDivider = new Color(41,49,52).darker();
	
	public static void resetAllDIviderColors(final Object targetClassInstance, Color target) {
				// Reset all divider colors
				try {
					for (JSplitPane i : ReflectionUtils.retrieveFieldsByType(targetClassInstance, JSplitPane.class)) {
						SplitPaneUtils.setDividerColors(target, Color.BLACK, i);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
	}
	
	public static void setBackground(Color background, JSplitPane panel) {
		try {
			BasicSplitPaneUI ui = (BasicSplitPaneUI) panel.getUI();
	        BasicSplitPaneDivider divider = ui.getDivider();
	        divider.setBackground(background);
		} catch (Exception e) {
		}
	}
	
	public static BasicSplitPaneDivider getDivider(JSplitPane panel) {
		try {
			BasicSplitPaneUI ui = (BasicSplitPaneUI) panel.getUI();
	        return ui.getDivider();
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void setDividerColors(Color back, Color border, JSplitPane panel) {
		panel.setUI(new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
	            return new BasicSplitPaneDivider(this) {
					private static final long serialVersionUID = -4392834503356984099L;

					public void setBorder(Border b) {
						super.setBorder(b);
	                }
	
	                @Override
	                    public void paint(Graphics g) {
		            		if(back == null) {
		            			g.setColor(defaultBackgroundDivider);
		            		}else {
		            			g.setColor(back);
		            		}
		            		g.fillRect(-3, -2, getSize().width+8, getSize().height+8);
	                        super.paint(g);
	                    }
	            };
            }
        });
		
		((BasicSplitPaneUI) panel.getUI()).getDivider().setBorder(new LineBorder(border, 2));
        panel.setBorder(new LineBorder(border));
	}

	public static void hideDivider(JSplitPane panel) {

		panel.setUI(new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
	            return new BasicSplitPaneDivider(this) {
					private static final long serialVersionUID = 1396997974252737249L;

					public void setBorder(Border b) {
	                }
	
	                @Override
	                public void paint(Graphics g) {
	                }
	            };
            }
        });
	}

	public static double getDividerRatio(JSplitPane pane) {
		Double location = (double) pane.getDividerLocation();
		int paneSize = pane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? pane.getWidth() : pane.getHeight();
		Double ratio = location / (paneSize - pane.getDividerSize());
		return ratio > 1.0D 
				? 1.0D 
				: ratio < 0D 
						? 0D 
						: ratio;
	}
	
}
