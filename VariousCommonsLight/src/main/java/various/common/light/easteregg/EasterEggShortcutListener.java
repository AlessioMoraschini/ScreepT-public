package various.common.light.easteregg;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

import various.common.light.gui.ShortcutKeys;

import various.common.light.gui.listener.KeyListenerParent;

public class EasterEggShortcutListener extends KeyListenerParent implements KeyEventDispatcher {

	EasterEggsManager manager;
	
	public EasterEggShortcutListener(EasterEggsManager manager) {
		this.manager = manager != null ? manager : new EasterEggsManager();
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {

		// a terminal with the focus owns the keyboard: no shortcut of the other panels
		if(ShortcutKeys.isFocusOwnerCapturingKeys()) {
			return false;
		}

		if (e.getID() == KeyEvent.KEY_RELEASED) {
			super.dispatchKeyEvent(e);
			
			for(EasterEgg egg : manager.getEasterEggs()) {
				
				if(egg.getShortcutFilter().isShorctutEnabled(e)) {
					egg.getAction().run();
				}
			}
		}
		
		return false;
	}

}
