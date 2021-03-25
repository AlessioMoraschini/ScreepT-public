package om;

import java.awt.Graphics;

import gui.GameFrame;
import various.common.light.utility.time.AmAsyncAction;

public class FallingBonus extends Rectangle {

	// Signal if paddle has collided this bonus
	public volatile boolean catched = false;
	
	// Signal if this bonus has already been used
	public volatile boolean consumed = false;
	
	// Signal if this bonus is being used
	public volatile boolean inAction = false;
	
	public AmAsyncAction timer;
	
	private double velocity;
	
	public GameFrame gameFrame;
	
	public Bonus bonus;
	
	public FallingBonus(Bonus bonus, double x, double y, GameFrame gameFrame) {
		this.bonus = bonus;
		this.bonus.fallingBonus = this;
		this.x = x;
		this.y = y;
		this.gameFrame = gameFrame;
		this.sizeX = gameFrame.gameConfig.BONUS_WIDTH;
		this.sizeY = gameFrame.gameConfig.BONUS_HEIGHT;
		this.velocity = gameFrame.gameConfig.BONUS_VELOCITY;
		
		timer = new AmAsyncAction(() -> {
				inAction = false;
				consumed = true;
				bonus.resetModifier();
			},
			bonus.timeMs);
	} 

	public void draw(Graphics g) {
		if (!catched) {
			g.drawImage(bonus.icon, (int) left(), (int) top(), null);
		}
	}
	
	public void setCatched(boolean catched) {
		this.catched = catched;
	}
	
	public void fireBonus() throws InstantiationException, IllegalAccessException {
		if (catched && !consumed) {
			inAction = true;
			timer.executeDelayedAsync();
			bonus.applyModifier(gameFrame);
		}
	}
	
	public void update(double FT_STEP) {
		y += velocity * FT_STEP;
	}
	
}
