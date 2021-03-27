package puppynoid.om;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.concurrent.atomic.AtomicInteger;

import puppynoid.utils.LevelLoader;

public class ScoreBoard {
	
	int SCREEN_WIDTH;
	int SCREEN_HEIGHT;
	
	public static final int INITIAL_MESSAGE_DURATION = 2000;

	public int score = 0;
	public AtomicInteger lives = new AtomicInteger(5);
	public boolean win = false;
	public boolean finished = false;
	public boolean gameOver = false;
	String text = "";

	public Font font;
	
	public LevelLoader levelLoader;
	
	public int initialScore;

	public ScoreBoard(String text, int PLAYER_LIVES, String FONT, int SCREEN_HEIGHT, int SCREEN_WIDTH, LevelLoader levelLoader) {
		this.font = new Font(FONT, Font.PLAIN, 12);
		this.text = text;
		this.lives = new AtomicInteger(PLAYER_LIVES);
		this.SCREEN_HEIGHT = SCREEN_HEIGHT;
		this.SCREEN_WIDTH = SCREEN_WIDTH;
		this.levelLoader = levelLoader;
		
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(INITIAL_MESSAGE_DURATION);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				updateScoreboard();
			}
		}).start();
	}

	public void increaseScore(Difficulty difficulty, int COUNT_BLOCKS_X, int COUNT_BLOCKS_Y, boolean noMoreBricks) {
		score++;
		if (noMoreBricks) {
			win = true;
			int bonus = calculateBonus(difficulty);
			if (!levelLoader.hasNextLevel()) {
				text = "You have completed PuppyNoid :D \nYour score was: " + score +
						" - Bonus Difficulty("+difficulty+"): " + bonus + "\n\nPress Esc to exit";
				finished = true;
			} else {
				text = "You have won! \nYour score is: " + score + 
						" - Bonus Difficulty("+difficulty+"): " + bonus + "\n\nPress N to go to Next Level,\n\n (Enter to restart, Esc to exit)";
			}
			score += bonus;
		} else {
			updateScoreboard();
		}
	}
	
	public int calculateBonus(Difficulty difficulty) {
		return (int)((score - initialScore) * difficulty.multiplierD * 0.25d);
	}

	public void die() {
		lives.decrementAndGet();
		if (lives.get() == 0) {
			gameOver = true;
			text = "You have lost! \nYour score was: " + score
					+ "\n\nPress Enter to restart, Esc to exit";
		} else {
			updateScoreboard();
		}
	}

	public void updateScoreboard() {
		text = "Level: " + levelLoader.getCurrentLevelN() + "  Score: " + score + "  Lives: " + lives;
	}

	public void draw(Graphics g) {
		if (win || gameOver) {
			font = font.deriveFont(50f);
			FontMetrics fontMetrics = g.getFontMetrics(font);
			g.setColor(Color.WHITE);
			g.setFont(font);
			int titleHeight = fontMetrics.getHeight();
			int lineNumber = 1;
			for (String line : text.split("\n")) {
				int titleLen = fontMetrics.stringWidth(line);
				g.drawString(
						line, 
						(SCREEN_WIDTH / 2) - (titleLen / 2),
						(SCREEN_HEIGHT / 4) + (titleHeight * lineNumber));
				lineNumber++;

			}
		} else {
			font = font.deriveFont(34f);
			FontMetrics fontMetrics = g.getFontMetrics(font);
			g.setColor(Color.WHITE);
			g.setFont(font);
			int titleLen = fontMetrics.stringWidth(text);
			int titleHeight = fontMetrics.getHeight();
			g.drawString(text, (SCREEN_WIDTH / 2) - (titleLen / 2),	titleHeight + 25);

		}
	}

}
