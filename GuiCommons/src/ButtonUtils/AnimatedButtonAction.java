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
package ButtonUtils;

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
