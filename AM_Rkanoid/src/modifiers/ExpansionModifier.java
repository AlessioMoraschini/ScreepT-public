package modifiers;

import java.util.concurrent.atomic.AtomicInteger;

import gui.GameFrame;
import om.Bonus;
import om.Paddle;

public class ExpansionModifier extends AbstractModifier {
	
	public static final double MODIFIER = 1.6d;
	public static AtomicInteger activeModifiers = new AtomicInteger(0);
	
	private boolean applied = false;
	
	public ExpansionModifier(GameFrame gameFrame, Bonus bonus) {
		super(gameFrame, bonus);
	}

	@Override
	public void applyModifier() {
		if (gameFrame.paddle.sizeX < (gameFrame.gameConfig.SCREEN_WIDTH / 2d)) {
			gameFrame.gameConfig.PADDLE_WIDTH *= MODIFIER;
			gameFrame.paddle.sizeX *= MODIFIER;
			applied = true;
			applyPaddleColor(bonus.paddleColor);
			activeModifiers.getAndIncrement();
		}
	}

	@Override
	public void resetOldBehaviour() {
		activeModifiers.decrementAndGet();
		if (applied) {
			gameFrame.gameConfig.PADDLE_WIDTH /= MODIFIER;
			gameFrame.paddle.sizeX /= MODIFIER;
			if (activeModifiers.get() < 1) {
				applyPaddleColor(Paddle.DEFAULT_COLOR);
			}
		}
	}
	
	public static void resetModifiersCount() {
		activeModifiers = new AtomicInteger(0);
	}
	
}
