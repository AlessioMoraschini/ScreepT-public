package easteregg;

import java.awt.KeyboardFocusManager;
import java.util.ArrayList;

import utility.log.SafeLogger;

public class EasterEggsManager {
	
	private static SafeLogger logger = new SafeLogger(EasterEggsManager.class);

	public ArrayList<EasterEgg> easterEggs;
	public EasterEggShortcutListener shortcutListener;
	
	public EasterEggsManager() {
		easterEggs = new ArrayList<>();
		shortcutListener = new EasterEggShortcutListener(this);
	}

	public EasterEgg getEasterEggByID(String id) {
		logger.debug("Searching easter egg by ID: " + id);
		for(EasterEgg egg : easterEggs) {
			if(egg.getId().equals(id)) {
				logger.debug("Easter egg found: " + egg);
				return egg;
			}
		}
		
		logger.debug("Easter egg not found with ID: " + id);
		return null;
	}
	
	public void applyKeyListener() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shortcutListener);
		logger.debug("Easter eggs key listener activated");
	}
	
	public void removeKeyListener() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(shortcutListener);
		logger.debug("Easter eggs key listener removed");
	}
	
	public ArrayList<PhraseEasterEgg> getPhraseEasterEggs(){
		ArrayList<PhraseEasterEgg> phraseEggs = new ArrayList<>();
		for(EasterEgg genericEgg : easterEggs) {
			if(genericEgg instanceof PhraseEasterEgg) {
				phraseEggs.add((PhraseEasterEgg)genericEgg);
			}
		}
		
		return phraseEggs;
	}
	
	public void addEasterEgg(EasterEgg egg) {
		easterEggs.add(egg);
		logger.debug("Easter egg added to manager: " + egg);
	}

	public void removeEasterEgg(EasterEgg egg) {
		easterEggs.remove(egg);
		logger.debug("Easter egg removed from manager: " + egg);
	}
	
	public ArrayList<EasterEgg> getEasterEggs() {
		return easterEggs != null ? easterEggs : new ArrayList<EasterEgg>();
	}

	public void setEasterEggs(ArrayList<EasterEgg> easterEggs) {
		this.easterEggs = easterEggs;
	}
	
}
