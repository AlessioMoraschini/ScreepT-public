package puppynoid.om;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;

import puppynoid.gui.GameFrame;
import puppynoid.utils.GuiUtils;
import puppynoid.utils.config.GameConfigs;

public class Shot extends Rectangle {

	// Signal if shot has hit something or gone out of view
	public volatile boolean used = false;
	private double velocity;
	
	private static final String imagePath = GameConfigs.IMG_FOLDER + "projectile.png";
	private static Image image;
	
	public GameFrame gameFrame;

	public Shot(double x, double y, GameFrame gameFrame) {
		if(image == null) try {
			image = GuiUtils.getResizedImage(imagePath, 20, 80);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.x = x;
		this.y = y;
		this.gameFrame = gameFrame;
		this.sizeX = image.getWidth(null);
		this.sizeY = image.getHeight(null);
		this.velocity = gameFrame.gameConfig.PROJECTILE_VELOCITY;
	} 

	public void draw(Graphics g) {
		if (!used) {
			g.drawImage(image, (int) left(), (int) top(), null);
		}
	}
	
	public void update(double FT_STEP) {
		y -= velocity * FT_STEP;
	}
	
}
