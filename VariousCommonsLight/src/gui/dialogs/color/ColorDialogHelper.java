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
package gui.dialogs.color;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gui.dialogs.color.ColorChooserGrid.UserChoice;
import gui.dialogs.color.om.ColorChanged;

public class ColorDialogHelper extends JColorChooser{
	private static final long serialVersionUID = 7495639155315531462L;
	
	private boolean colorEsit;
	private JColorChooser thisChooser;
	
	public ColorChanged colorChangedAction = (e) -> {};
	
	public ColorDialogHelper() {
		colorChangedAction = (e) -> {};
	}
	
	public ColorDialogHelper(ColorChanged colorChangedAction) {
		if(colorChangedAction != null) {
			this.colorChangedAction = colorChangedAction;
		}
	}
	
	/**
	 * Create a colorChooser dialog updating in real time the color as the selected one.
	 * If OK color is maintained, else the original is restored
	 * @param targetComponent the component to change the color of
	 * @param foreground true if wanna change foreground, false if the background is to change
	 * @return the new color if user pressed ok, null in the other case
	 */
	public Color askForColor(JComponent targetComponent, boolean foreground) {
		
		thisChooser = this;
		colorEsit = false;
		String title = (foreground)? "Choose Text color" : "Choose Background color";
		Color backupColor;
		backupColor = (foreground)? targetComponent.getForeground() : targetComponent.getBackground();
		
		thisChooser.getSelectionModel().setSelectedColor(backupColor);
		thisChooser.getSelectionModel().addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (foreground) {
					targetComponent.setForeground(thisChooser.getColor());
				}else {
					targetComponent.setBackground(thisChooser.getColor());
				}
				
				colorChangedAction.colorChanged(thisChooser.getColor());
			}
		});
		
	    JDialog dialog = JColorChooser.createDialog(targetComponent, title, true, thisChooser, new ActionListener() {
			
			@Override // OK
			public void actionPerformed(ActionEvent e) {
				Color color = thisChooser.getColor();
				if (color != null) {
					if (foreground) {
						targetComponent.setForeground(color);
					}else {
						targetComponent.setBackground(color);
					}
					colorChangedAction.colorChanged(thisChooser.getColor());
					colorEsit = true;
		      }			
			}
		}, new ActionListener() {
			@Override // NO
			public void actionPerformed(ActionEvent e) {
				if (foreground) {
					targetComponent.setForeground(backupColor);
				}else {
					targetComponent.setBackground(backupColor);
				}
				colorChangedAction.colorChanged(backupColor);
				colorEsit = false;
			}
		});
		dialog.setVisible(true);
		
		if(colorEsit) {
			return thisChooser.getColor();
		}else {
			return null;
		}
	}
	
	public static UserChoice askForColorGrid(Color selectedColor) {
		ColorChooserGrid colorChooser = new ColorChooserGrid(selectedColor);
		
		return colorChooser.userChoice;
	}

	public static UserChoice askForColorGrid(Color selectedColor, String title) {
		ColorChooserGrid colorChooser = new ColorChooserGrid(selectedColor, title);
		
		return colorChooser.userChoice;
	}
	
}
