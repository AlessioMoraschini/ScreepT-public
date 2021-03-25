package om;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Paddle extends Rectangle {

	public double velocity = 0.0;
	
	public volatile boolean canShot = false;
	public List<Shot> shotList;
	
	public static final Color DEFAULT_COLOR = new Color(217, 94, 46);
	public Color paddleColor = DEFAULT_COLOR;

	public Paddle(double x, double y, double PADDLE_WIDTH, double PADDLE_HEIGHT) {
		this.shotList = new ArrayList<Shot>();
		this.x = x;
		this.y = y;
		this.sizeX = PADDLE_WIDTH;
		this.sizeY = PADDLE_HEIGHT;
	}

	public void update(double FT_STEP) {
		x += velocity * FT_STEP;
		Iterator<Shot> shotIterator = shotList.iterator();
		while(shotIterator.hasNext()) {
			shotIterator.next().update(FT_STEP);
		}
	}

	public void stopMove(double SCREEN_WIDTH) {
		velocity = 0.0;
		
		if (left() < 0.0) {
			this.x = 0.0 + sizeX/2.0;
		}
		
		if (right() > SCREEN_WIDTH) {
			this.x = SCREEN_WIDTH - sizeX/2.0;
		}
	}

	public void moveLeft(double PADDLE_VELOCITY) {
		if (left() > 0.0) {
			velocity = -PADDLE_VELOCITY;
		} else {
			this.x = 0.0 + sizeX/2.0;
			velocity = 0.0;
		}
	}

	public void moveRight(double SCREEN_WIDTH, double PADDLE_VELOCITY) {
		if (right() < SCREEN_WIDTH) {
			velocity = PADDLE_VELOCITY;
		} else {
			this.x = SCREEN_WIDTH - sizeX/2.0;
			velocity = 0.0;
		}
	}

	public void draw(Graphics g) {
		g.setColor(paddleColor);
		g.fillRect((int) (left()), (int) (top()), (int) sizeX, (int) sizeY);
		for(Shot shot : shotList) {
			shot.draw(g);
		}
	}

}