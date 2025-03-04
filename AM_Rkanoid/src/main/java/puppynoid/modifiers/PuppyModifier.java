package puppynoid.modifiers;

import java.awt.Image;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import puppynoid.gui.GameFrame;
import puppynoid.om.Bonus;
import puppynoid.utils.GuiUtils;
import puppynoid.utils.config.GameConfigs;

public class PuppyModifier extends AbstractModifier {
	
	public static AtomicInteger activeModifiers = new AtomicInteger(0);
	
	public static Image puppyBallImage;
	
	public PuppyModifier(GameFrame gameFrame, Bonus bonus) {
		super(gameFrame, bonus);
	}

	@Override
	public void applyModifier() {
		gameFrame.ball.radius = gameFrame.gameConfig.BALL_RADIUS * 3d;
		gameFrame.ball.flameBall = true;
		if(puppyBallImage == null) {
			try {
				puppyBallImage = GuiUtils.getResizedImage(
						GameConfigs.IMG_FOLDER + "puppy_ball.png",
						(int)(gameFrame.ball.radius * 0.3d), 
						(int)(gameFrame.ball.radius * 0.3d));
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		applyBallImage(puppyBallImage);
		activeModifiers.getAndIncrement();
	}

	@Override
	public void resetOldBehaviour() {
		activeModifiers.decrementAndGet();
		if (activeModifiers.get() < 1 && FlameModifier.activeModifiers.get() < 1) {
			gameFrame.ball.flameBall = false;
			gameFrame.ball.radius = gameFrame.gameConfig.BALL_RADIUS;
			removeBallImage();
		}
	}
	
	public static void resetModifiersCount() {
		activeModifiers = new AtomicInteger(0);
	}
	
}