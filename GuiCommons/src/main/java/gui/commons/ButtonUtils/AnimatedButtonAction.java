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
package gui.commons.ButtonUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.Timer;

public class AnimatedButtonAction extends AbstractAction{
	private static final long serialVersionUID = -2206917300166814804L;

	JButton button;
	Runnable action;
	int timer;
	
	public AnimatedButtonAction(int timer, JButton button, Runnable action) {
		this.timer = timer;
		this.button = button;
		this.action = action;
	}
	
	/**
	 * Override this method calling 
	 */
    @Override
    public void actionPerformed(ActionEvent e) {
        button.getModel().setArmed(true);
        button.getModel().setPressed(true);
        Timer t = new Timer(timer, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                button.getModel().setArmed(false);
                button.getModel().setPressed(false);
            }
        });
        t.setRepeats(false);
        t.start();
        
        new Thread(action).start();
    }

	public JButton getButton() {
		return button;
	}

	public void setButton(JButton button) {
		this.button = button;
	}

	public int getTimer() {
		return timer;
	}

	public void setTimer(int timer) {
		this.timer = timer;
	}
    
}
