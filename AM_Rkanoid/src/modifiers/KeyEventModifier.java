package modifiers;

import java.awt.event.KeyEvent;

@FunctionalInterface
public interface KeyEventModifier {

	public void applyModifier(KeyEvent event);
}
