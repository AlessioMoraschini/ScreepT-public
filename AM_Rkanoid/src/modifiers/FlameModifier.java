package modifiers;

import java.awt.Image;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import gui.GameFrame;
import om.Bonus;
import utils.GuiUtils;
import utils.config.GameConfigs;

public class FlameModifier extends AbstractModifier {
	
	public static AtomicInteger activeModifiers = new AtomicInteger(0);
	
	public Image fireBallImage;
	
	public FlameModifier(GameFrame gameFrame, Bonus bonus) {
		super(gameFrame, bonus);
	}

	@Override
	public void applyModifier() {
		gameFrame.ball.flameBall = true;
		gameFrame.ball.radius = gameFrame.gameConfig.BALL_RADIUS * 1.2d;
		try {
			fireBallImage = GuiUtils.getResizedImage(
					GameConfigs.IMG_FOLDER + "flame_ball.png",
					(int)gameFrame.ball.radius * 2, 
					(int)gameFrame.ball.radius * 2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		applyBallImage(fireBallImage);
		activeModifiers.getAndIncrement();
	}

	@Override
	public void resetOldBehaviour() {
		activeModifiers.decrementAndGet();
		if (activeModifiers.get() < 1 && PuppyModifier.activeModifiers.get() < 1) {
			gameFrame.ball.radius = gameFrame.gameConfig.BALL_RADIUS;
			gameFrame.ball.flameBall = false;
			removeBallImage();
		}
	}
	
	public static void resetModifiersCount() {
		activeModifiers = new AtomicInteger(0);
	}
}
