package frameutils.shortcuts;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

import frameutils.frame.panels.ShellReadWritePanel;

public class ShellShortcutsListener implements KeyEventDispatcher {

	ShellReadWritePanel panelRef;
	
	public ShellShortcutsListener(ShellReadWritePanel panel) {
		panelRef = panel;
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		
		if(!panelRef.commandTextArea.hasFocus())
			return false;

		if (e.getID() == KeyEvent.KEY_PRESSED && panelRef.getTopLevelAncestor().hasFocus()) {
			
		} else if(e.getID() == KeyEvent.KEY_RELEASED && SwingUtilities.getWindowAncestor(panelRef).isFocusableWindow()) {
			
			if(e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown() && !e.isAltDown() && !e.isShiftDown()) {
				panelRef.playStopPausePanel.getPlayButton().doClick();
			}
			
		}
		
		
		
		
		return false;
	}

}
