package puppynoid.om;

import java.awt.Color;
import java.awt.Graphics;

import puppynoid.gui.GameFrame;

public class Brick extends Rectangle {

	public boolean destroyed = false;
	public FallingBonus fallingBonus = null;
	public int healthRemaining = 1;
	public GameFrame gameFrame;

	public Brick(double x, double y, GameFrame gameFrame, int healthRemaining) {
		Bonus bonus = Bonus.getRandomBonus(gameFrame.gameConfig.BONUS_PROBABILITY);

//		bonus = Bonus.SHOT; // TEST ONLY
		if (bonus != null) {
			fallingBonus = new FallingBonus(bonus, x, y, gameFrame);
		} else {
			fallingBonus = null;
		}
		this.x = x;
		this.y = y;
		this.sizeX = gameFrame.gameConfig.BLOCK_WIDTH;
		this.sizeY = gameFrame.gameConfig.BLOCK_HEIGHT;
		this.healthRemaining = healthRemaining;
		
	}
	
	public void draw(Graphics g) {
		g.setColor(durabilityToColor());
		g.fillRect((int) left(), (int) top(), (int) sizeX, (int) sizeY);
	}
	
	public boolean hasFallingBonus() {
		return fallingBonus != null;
	}
	
	public Color durabilityToColor() {
		if(healthRemaining >= 3)
			return Color.GRAY;
		
		if(healthRemaining >= 2)
			return Color.RED;
		
		return Color.ORANGE;
	}
}
