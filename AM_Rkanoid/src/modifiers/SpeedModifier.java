package modifiers;

import java.util.concurrent.atomic.AtomicInteger;

import gui.GameFrame;
import om.Bonus;
import om.Paddle;

public class SpeedModifier extends AbstractModifier {
	
	public static final double MODIFIER = 1.6d;
	public static AtomicInteger activeModifiers = new AtomicInteger(0);
	
	public SpeedModifier(GameFrame gameFrame, Bonus bonus) {
		super(gameFrame, bonus);
	}

	@Override
	public void applyModifier() {
		gameFrame.gameConfig.PADDLE_VELOCITY *= MODIFIER;
		gameFrame.paddle.velocity *= MODIFIER;
		applyPaddleColor(bonus.paddleColor);
		activeModifiers.getAndIncrement();
	}

	@Override
	public void resetOldBehaviour() {
		activeModifiers.getAndDecrement();
		gameFrame.gameConfig.PADDLE_VELOCITY /= MODIFIER;
		gameFrame.paddle.velocity /= MODIFIER;
		
		if (activeModifiers.get() < 1) {
			applyPaddleColor(Paddle.DEFAULT_COLOR);
		}
	}
	
	public static void resetModifiersCount() {
		activeModifiers = new AtomicInteger(0);
	}
	
}
