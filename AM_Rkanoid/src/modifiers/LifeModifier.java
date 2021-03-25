package modifiers;

import gui.GameFrame;
import om.Bonus;

public class LifeModifier extends AbstractModifier {
	
	public LifeModifier(GameFrame gameFrame, Bonus bonus) {
		super(gameFrame, bonus);
	}

	@Override
	public void applyModifier() {
		gameFrame.scoreboard.lives.incrementAndGet();
		gameFrame.gameConfig.PLAYER_LIVES += 1d;
	}

	@Override
	public void resetOldBehaviour() {
	}
	
}
