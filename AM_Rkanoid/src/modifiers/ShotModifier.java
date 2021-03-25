package modifiers;

import java.awt.event.KeyEvent;

import gui.GameFrame;
import om.Bonus;
import om.Shot;
import sounds.SoundsManager;
import utils.config.GameConfigs;
import utils.config.GeneralConfig;

public class ShotModifier extends AbstractModifier implements KeyEventModifier {
	
	public static volatile boolean alreadyActive = false;
	
	public volatile boolean canBeEnabled = true;
	
	public ShotModifier(GameFrame gameFrame, Bonus bonus) {
		super(gameFrame, bonus);
		canBeEnabled = true;
	}

	@Override
	public void applyModifier() {
		if (!alreadyActive) {
			gameFrame.paddle.canShot = true;
			gameFrame.keyModifiers.add(this);
			alreadyActive = true;
		} else {
			canBeEnabled = false;
		}
	}

	@Override
	public void resetOldBehaviour() {
		if (canBeEnabled) {
			gameFrame.paddle.canShot = false;
			if (gameFrame.keyModifiers.contains(this)) {
				gameFrame.keyModifiers.remove(this);
			}
			alreadyActive = false;
		}
	}

	@Override
	public void applyModifier(KeyEvent event) {
		if(KeyEvent.VK_S == event.getKeyCode()) {
			synchronized (gameFrame.paddle.shotList) {
				if (gameFrame.paddle.canShot && alreadyActive && canBeEnabled) {
					gameFrame.paddle.shotList.add(new Shot(gameFrame.paddle.x, gameFrame.paddle.y, gameFrame));
					SoundsManager.playSound(
							GameConfigs.SOUNDS_FOLDER + "Shot.wav",
							gameFrame.generalConfig.loadFloat(GeneralConfig.KEY_EFFECTS_VOLUME));
				}
			}
		}
	}
	
	public static void resetModifiers() {
		alreadyActive = false;
	}

	@Override
	public boolean canAddModifier() {
		return !alreadyActive;
	}
	
}
