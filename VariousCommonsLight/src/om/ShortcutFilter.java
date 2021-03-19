package om;

import java.awt.event.KeyEvent;

@FunctionalInterface
public interface ShortcutFilter {
	
	public boolean isShorctutEnabled(KeyEvent e);

}
