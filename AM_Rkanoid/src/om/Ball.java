package om;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

public class Ball extends GameObject {

	volatile public double x, y;
	public double radius;
	public double BALL_VELOCITY;
	public double velocityX;
	public double velocityY;
	
	public volatile Image ballImage;
	
	public static final Color DEFAULT_COLOR = new Color(232, 221, 213);
	public Color ballColor = DEFAULT_COLOR;
	
	public volatile boolean flameBall;
	public volatile boolean ballStopped;

	public Ball(int x, int y, double BALL_VELOCITY, double BALL_RADIUS) {
		this.x = x;
		this.y = y;
		this.radius = BALL_RADIUS;
		this.velocityX = BALL_VELOCITY;
		this.velocityY = BALL_VELOCITY;
		this.BALL_VELOCITY = BALL_VELOCITY;
	}

	public void draw(Graphics g) {
		if (ballImage == null) {
			g.setColor(ballColor);
			g.fillOval((int) left(), (int) top(), (int) radius * 2, (int) radius * 2);
		
		} else {
			g.drawImage(ballImage, (int) left(), (int) top(), null);
		}
	}

	public void update(ScoreBoard scoreBoard, Paddle paddle, double FT_STEP, double SCREEN_WIDTH, double SCREEN_HEIGHT) {
		x += velocityX * FT_STEP;
		y += velocityY * FT_STEP;

		if (left() < 0)
			velocityX = -velocityX;
		
		else if (right() > SCREEN_WIDTH)
			velocityX = -velocityX;
		
		if (top() < 0) {
			velocityY = -velocityY;
		
		} else if (bottom() > SCREEN_HEIGHT) {
			scoreBoard.die();
			stopAlignedToPaddle(paddle);
		}

	}
	
	public void shot() {
		velocityY = - Math.abs(BALL_VELOCITY);
		velocityX = BALL_VELOCITY;
		ballStopped = false;
	}
	
	public void stop() {
		velocityY = 0;
		velocityX = 0;
		ballStopped = true;
	}

	public void stopAlignedToPaddle(Paddle paddle) {
		stop();
		x = paddle.x;
		y = paddle.y - 40;
	}

	public double left() {
		return x - radius;
	}

	public double right() {
		return x + radius;
	}

	public double top() {
		return y - radius;
	}

	public double bottom() {
		return y + radius;
	}
	
}
