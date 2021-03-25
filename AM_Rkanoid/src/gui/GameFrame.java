package gui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import gui.config.MainOptionsFrame;
import modifiers.AbstractModifier;
import modifiers.ExpansionModifier;
import modifiers.FlameModifier;
import modifiers.KeyEventModifier;
import modifiers.PuppyModifier;
import modifiers.ShotModifier;
import modifiers.SpeedModifier;
import om.Ball;
import om.Brick;
import om.Button;
import om.Difficulty;
import om.FallingBonus;
import om.GameObject;
import om.Paddle;
import om.ScoreBoard;
import om.Shot;
import sounds.SoundsManager;
import utils.LevelLoader;
import utils.config.GameConfigs;
import utils.config.GeneralConfig;

public class GameFrame extends JFrame implements KeyListener {

	private static final long serialVersionUID = -5055240957536202375L;

	private static final NumberFormat DOUBLE_FORMATTER = new DecimalFormat("#0.00");
	private AtomicBoolean canReprintFps = new AtomicBoolean(true);
	public LevelLoader levelLoader;

	public SoundsManager soundsManager;

	private List<FallingBonus> activeBonuses;
	public List<KeyEventModifier> keyModifiers;
	public List<Button> buttonList;
	
	/* CONFIGURATION */
	public GameConfigs gameConfig;
	public GeneralConfig generalConfig;

	/* GAME VARIABLES */
	private boolean tryAgain = false;
	private boolean goToNextLevel = false;
	private boolean forceExitLevel = false;
	private boolean paused = false;
	private boolean running = false;

	private double lastFt;
	private double currentSlice;

	public Difficulty difficulty = Difficulty.SUPER_EASY;

	public Runnable returnAction;

	/* GAME OBJECTS */
	public Paddle paddle;
	public Ball ball;
	public ScoreBoard scoreboard;
	private List<Brick> bricks = new ArrayList<Brick>();
	
	private MouseAdapter buttonMouseAdapter;
	private Button muteButton;
	private Button pauseButton;
	private Button optionsButton;
	
	public GameFrame(GeneralConfig generalConfig, Difficulty difficulty, Runnable returnAction, LevelLoader levelLoader) throws IOException {

		this.soundsManager = new SoundsManager();
		
		this.activeBonuses = new ArrayList<FallingBonus>();
		this.keyModifiers = new ArrayList<KeyEventModifier>();
		this.buttonList = new ArrayList<Button>();
		
		this.difficulty = difficulty;
		this.levelLoader = levelLoader != null ? levelLoader : new LevelLoader();
		this.generalConfig = generalConfig;
		this.returnAction = returnAction != null ? returnAction : () -> {
		};

		initializeLevel(levelLoader.getCurrentLevelConfig());
		initializeButtons();

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				if (!goToNextLevel) {
					if (running && !askExitGame()) {
						setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
						return;
					}
					
					System.out.println("returnAction.run called from windowListener");
					returnAction.run();
				}

				running = false;
				
				SoundsManager.MUTED = false;
			}
		});
		
	}

	///////////////// INITIALIZATION METHODS /////////////////////

	public void initializeLevel(String configFile) throws IOException {

		gameConfig = new GameConfigs(configFile);

		applyDifficultyMultiplier(difficulty);

		paddle = new Paddle(gameConfig.SCREEN_WIDTH / 2, gameConfig.SCREEN_HEIGHT - 50, gameConfig.PADDLE_WIDTH, gameConfig.PADDLE_HEIGHT);

		ball = new Ball(gameConfig.SCREEN_WIDTH / 2, gameConfig.SCREEN_HEIGHT / 2, gameConfig.BALL_VELOCITY, gameConfig.BALL_RADIUS);
		ball.stopAlignedToPaddle(paddle);

		scoreboard = new ScoreBoard(GameConfigs.GAME_WELCOME, gameConfig.PLAYER_LIVES, gameConfig.FONT, gameConfig.SCREEN_HEIGHT, gameConfig.SCREEN_WIDTH,
				levelLoader);

		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setUndecorated(false);
		this.setResizable(false);
		this.setSize(gameConfig.SCREEN_WIDTH, gameConfig.SCREEN_HEIGHT);
		this.setVisible(true);
		this.addKeyListener(this);
		this.setLocationRelativeTo(null);
		this.setIconImage(new ImageIcon(GameConfigs.IMG_FOLDER + "mainIcon.png").getImage());

		this.createBufferStrategy(2);

		initializeBricks(bricks);
	}
	
	void initializeBricks(List<Brick> bricks) {
		// deallocate old bricks
		bricks.clear();

		double marginLeft = ((double) gameConfig.SCREEN_WIDTH - (gameConfig.COUNT_BLOCKS_X * (gameConfig.BLOCK_WIDTH + 11.0d))) / 2.0d;
		double marginTop = 50d;

		for (int iX = 0; iX < gameConfig.COUNT_BLOCKS_X; ++iX) {
			for (int iY = 0; iY < gameConfig.COUNT_BLOCKS_Y; ++iY) {
				Point currentPoint = new Point(iX, iY);
				if (!gameConfig.HIDDEN_BRICKS_MAP.containsKey(currentPoint)) {
					bricks.add(new Brick(marginLeft + (iX + 1) * (gameConfig.BLOCK_WIDTH + 3) + 22, marginTop + (iY + 2) * (gameConfig.BLOCK_HEIGHT + 3) + 20,
							this, gameConfig.HEALTH_BRICKS_MAP.get(currentPoint)));
				}
			}
		}
	}
	
	public void initializeButtons() {
		muteButton = new Button(gameConfig.SCREEN_WIDTH - 40, 60, 30, 30, this.getContentPane());
		try {
			muteButton.setSelectedImage(GameConfigs.IMG_FOLDER + "muted.png");
			muteButton.setDeselectedImage(GameConfigs.IMG_FOLDER + "unmuted.png");
			muteButton.setOnClickAction(() -> {
				if(!muteButton.isSelected) {
					soundsManager.pause();
					SoundsManager.MUTED = true;
					generalConfig.setProperty(GeneralConfig.KEY_BASE_MUTED, true, true);
					generalConfig.setProperty(GeneralConfig.KEY_EFFECTS_MUTED, true, true);
				} else {
					soundsManager.resumeLoop();
					SoundsManager.MUTED = false;
					generalConfig.setProperty(GeneralConfig.KEY_BASE_MUTED, false, true);
					generalConfig.setProperty(GeneralConfig.KEY_EFFECTS_MUTED, false, true);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		muteButton.isSelected = 
				generalConfig.getProperties().getBooleanVarFromProps(GeneralConfig.KEY_BASE_MUTED, true)
				&& generalConfig.getProperties().getBooleanVarFromProps(GeneralConfig.KEY_EFFECTS_MUTED, true);
		muteButton.getOnClickAction().run();
		
		SoundsManager.MUTED = muteButton.isSelected;
		buttonList.add(muteButton);
		
		pauseButton = new Button(gameConfig.SCREEN_WIDTH - 80, 60, 30, 30, this.getContentPane());
		try {
			pauseButton.setSelectedImage(GameConfigs.IMG_FOLDER + "play.png");
			pauseButton.setDeselectedImage(GameConfigs.IMG_FOLDER + "pause.png");
			pauseButton.setOnClickAction(() -> {
				if(!pauseButton.isSelected) {
					paused = true;
				} else {
					paused = false;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		buttonList.add(pauseButton);
		
		optionsButton = new Button(gameConfig.SCREEN_WIDTH - 120, 60, 30, 30, this.getContentPane());
		try {
			optionsButton.setSelectedImage(GameConfigs.IMG_FOLDER + "options.png");
			optionsButton.setDeselectedImage(GameConfigs.IMG_FOLDER + "options.png");
			optionsButton.setOnClickAction(() -> {
				// Open options panel
				boolean paused = this.paused;
				MainOptionsFrame optFrame = new MainOptionsFrame(this, "Options", generalConfig, gameConfig);
				optFrame.returnAction = () -> {
					this.paused = paused;
				};
				
				this.paused = true;
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		buttonList.add(optionsButton);

		startButtonsMouseListener();
	}
	
	public void startButtonsMouseListener() {
		if(buttonMouseAdapter != null) {
			this.removeMouseListener(buttonMouseAdapter);
			this.removeMouseMotionListener(buttonMouseAdapter);
		}
		
		buttonMouseAdapter = new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				for(Button button : buttonList) {
					button.onClick(e.getX(), e.getY());
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				for(Button button : buttonList) {
					button.checkIfHover(e.getX(), e.getY());
				}
			}
		};
		
		this.addMouseMotionListener(buttonMouseAdapter);
		this.addMouseListener(buttonMouseAdapter);
	}
	
	private void applyDifficultyMultiplier(Difficulty difficulty) {
		gameConfig.BALL_VELOCITY *= difficulty.multiplierD;
		gameConfig.PADDLE_VELOCITY *= (1.1d / difficulty.multiplierD);
		gameConfig.PADDLE_WIDTH *= (1.0d / difficulty.multiplierD);
		gameConfig.PLAYER_LIVES -= (difficulty.multiplierI);
		gameConfig.BONUS_PROBABILITY /= difficulty.multiplierD;
	}

	boolean isIntersecting(GameObject mA, GameObject mB) {
		return mA.right() >= mB.left() && mA.left() <= mB.right() && mA.bottom() >= mB.top() && mA.top() <= mB.bottom();
	}

	/////////// COLLISION CHECK METHODS ///////////////

	void testCollision(Paddle mPaddle, FallingBonus fallingBonus, Iterator<FallingBonus> it) {
		if(fallingBonus.consumed) {
			it.remove();
		}
		
		if (fallingBonus.catched || !isIntersecting(mPaddle, fallingBonus))
			return;

		// Collision logic
		try {
			fallingBonus.setCatched(true);
			fallingBonus.fireBonus();
			
		} catch (Exception e) {
			System.out.println("Error while running catched fallingBonus logics");
			e.printStackTrace();
		}
	}

	void testCollision(Paddle mPaddle, Ball mBall) {
		if (!isIntersecting(mPaddle, mBall))
			return;

		double centersDistance = mBall.x - mPaddle.x;

		double percFromCenter = Math.abs((centersDistance) / (gameConfig.PADDLE_WIDTH / 2d));

		mBall.velocityY = -gameConfig.BALL_VELOCITY;
		if (mBall.x < mPaddle.x)
			mBall.velocityX = -gameConfig.BALL_VELOCITY;
		else
			mBall.velocityX = gameConfig.BALL_VELOCITY;

		mBall.velocityX = mBall.velocityX * percFromCenter;
	}

	void testCollision(Brick mBrick, Shot shot, ScoreBoard scoreboard, Iterator<Brick> it, Iterator<Shot> shotIterator) {
		if (!isIntersecting(mBrick, shot) || mBrick.destroyed)
			return;

		mBrick.healthRemaining--;
		shot.used = true;
		shotIterator.remove();

		if (mBrick.healthRemaining < 1) {
			mBrick.destroyed = true;
			if (mBrick.hasFallingBonus()) {
				activeBonuses.add(mBrick.fallingBonus);
			}
		}
	}

	boolean testCollision(Brick mBrick, Ball mBall, ScoreBoard scoreboard, Iterator<Brick> it) {
		if (!isIntersecting(mBrick, mBall))
			return false;

		if (ball.flameBall) {
			mBrick.healthRemaining = 0;
		} else {
			mBrick.healthRemaining--;
		}

		if (mBrick.healthRemaining < 1) {
			mBrick.destroyed = true;
			AbstractModifier modifierSample = mBrick.hasFallingBonus() ? mBrick.fallingBonus.bonus.getNewModifierInstance(this) : null;
			boolean canAdd = modifierSample != null ? modifierSample.canAddModifier() : false;
			if (mBrick.hasFallingBonus() && canAdd) {
				activeBonuses.add(mBrick.fallingBonus);
			}
		}

		double overlapLeft = mBall.right() - mBrick.left();
		double overlapRight = mBrick.right() - mBall.left();
		double overlapTop = mBall.bottom() - mBrick.top();
		double overlapBottom = mBrick.bottom() - mBall.top();

		boolean ballFromLeft = overlapLeft < overlapRight;
		boolean ballFromTop = overlapTop < overlapBottom;

		double minOverlapX = ballFromLeft ? overlapLeft : overlapRight;
		double minOverlapY = ballFromTop ? overlapTop : overlapBottom;

		if (!ball.flameBall) {
			if (minOverlapX < minOverlapY) {
				mBall.velocityX = ballFromLeft ? -gameConfig.BALL_VELOCITY : gameConfig.BALL_VELOCITY;
			} else {
				mBall.velocityY = ballFromTop ? -gameConfig.BALL_VELOCITY : gameConfig.BALL_VELOCITY;
			}
		}

		return true;
	}

	////////////////// DRAWING METHODS //////////////////

	public void drawBackground(Graphics g) {
		if (gameConfig.background == null) {
			g.setColor(Color.black);
			g.fillRect(0, 0, getWidth(), getHeight());
		} else {
			g.drawImage(gameConfig.background, 0, 0, null);
		}
	}
	

	private void drawButtons(Graphics g) {
		for(Button button : buttonList) {
			button.draw(g);
		}
	};	

	private void drawScene(List<Brick> bricks, ScoreBoard scoreboard) {

		BufferStrategy bf = this.getBufferStrategy();
		Graphics g = null;

		try {

			g = bf.getDrawGraphics();

			drawBackground(g);
			
			// Draw game objects for current frame
			ball.draw(g);
			paddle.draw(g);
			
			for (Brick brick : bricks) {
				brick.draw(g);
			}

			for (FallingBonus bonus : activeBonuses) {
				bonus.draw(g);
			}

			scoreboard.draw(g);
			
			drawButtons(g);

			// If paused draw string
			if (paused) {
				FontMetrics fontMetrics = g.getFontMetrics(scoreboard.font.deriveFont(50f));
				String msg = "PAUSED (press SPACEBAR to resume)";
				int titleLen = fontMetrics.stringWidth(msg);
				int titleHeight = fontMetrics.getHeight();
				g.setColor(Color.WHITE);
				g.setFont(scoreboard.font.deriveFont(50f));
				g.drawString(msg, (int) (gameConfig.SCREEN_WIDTH / 2.0d) - (int) (titleLen / 2.0d), (gameConfig.SCREEN_HEIGHT / 2) + (titleHeight));
			}

		} catch (Exception e) {

		} finally {
			if (g != null) {
				g.dispose();
			}
		}

		bf.show();
		Toolkit.getDefaultToolkit().sync();
	}

	//////////////// SOUND METHODS /////////////////////
	public void playBackTheme() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				float lastGeneralVolume = generalConfig.loadFloat(GeneralConfig.KEY_BASE_VOLUME);
				float currentGeneralVolume = lastGeneralVolume;
						
				if (gameConfig.BACKGROUND_SOUND_THEME != null) {
					soundsManager.loop(
							gameConfig.BACKGROUND_SOUND_THEME,
							lastGeneralVolume);
				}

				while (running) {
					try {
						
						currentGeneralVolume = generalConfig.loadFloat(GeneralConfig.KEY_BASE_VOLUME);
						
						boolean mutedBase = generalConfig.getProperties().getBooleanVarFromProps(
								GeneralConfig.KEY_BASE_MUTED, true);
						boolean mutedEffects = generalConfig.getProperties().getBooleanVarFromProps(
								GeneralConfig.KEY_EFFECTS_MUTED, true);
						
						muteButton.isSelected = mutedEffects && mutedBase;

						if (paused || mutedBase) {
							soundsManager.pause();
						} else if (!paused || !mutedBase){
							soundsManager.resumeLoop();
						}
						
						if(currentGeneralVolume != lastGeneralVolume) {
							soundsManager.setVolume(currentGeneralVolume);
							soundsManager.applyVolume();
						}
						
						lastGeneralVolume = generalConfig.loadFloat(GeneralConfig.KEY_BASE_VOLUME);

						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				soundsManager.stop();
			}
		}).start();
	}

	///////// MAIN RUNNER //////////

	public void run() throws IOException {

		// SET BACKGROUND
		BufferStrategy bf = this.getBufferStrategy();
		Graphics g = bf.getDrawGraphics();
		drawBackground(g);

		playBackTheme();

		// START RUN LOGICS
		running = true;

		startEnabledFPSRefresher();

		while (running) {
			
			long time1 = System.currentTimeMillis();

			checkPausedBonuses();

			if (!scoreboard.gameOver && !scoreboard.win) {
				tryAgain = false;
				update();
				drawScene(bricks, scoreboard);

				// to simulate low FPS
				try {
					Thread.sleep(gameConfig.FPS_SLEEP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			} else if (tryAgain) {
				running = false;
				relaunchcurrentLevel();
			}

			long time2 = System.currentTimeMillis();
			double elapsedTime = time2 - time1;

			lastFt = elapsedTime;

			double seconds = elapsedTime / 1000.0;
			if (seconds > 0.0 && canReprintFps.get()) {
				double fps = 1.0 / seconds;
				this.setTitle("FPS: " + DOUBLE_FORMATTER.format(fps));
				canReprintFps.set(false);
			}

		}

		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		resetModifiersCount();

		if (scoreboard.win) {
			if (!launchNewLevel()) {
				// game finished!!!
				returnAction.run();

				if (scoreboard.finished) {
					System.out.println("GAME FINISHED :D");
				}

			} else {
				System.out.println("Going to next level: " + levelLoader.getCurrentLevelN());
			}

		} else {
			System.out.println("Returning to main menu");
			returnAction.run();
		}
	}
	
	private void checkPausedBonuses() {
		if (paused) {
			for(FallingBonus bonus : activeBonuses) {
				if(bonus.catched && !bonus.consumed) {
					bonus.timer.setPaused(true);
				}
			}
			return;
		
		} else {
			for(FallingBonus bonus : activeBonuses) {
				if(bonus.catched && !bonus.consumed) {
					bonus.timer.setPaused(false);
				}
			}
		}
	}
	

	private void update() {
		
		scoreboard.updateScoreboard();

		if(paused)
			return;

		currentSlice += lastFt;

		for (; currentSlice >= gameConfig.FT_SLICE; currentSlice -= gameConfig.FT_SLICE) {

			ball.update(scoreboard, paddle, gameConfig.FT_STEP, gameConfig.SCREEN_WIDTH, gameConfig.SCREEN_HEIGHT);
			paddle.update(gameConfig.FT_STEP);
			testCollision(paddle, ball);

			Iterator<Brick> it = bricks.iterator();
			while (it.hasNext()) {
				Brick brick = it.next();
				if (!testCollision(brick, ball, scoreboard, it)) {

					synchronized (paddle.shotList) {
						Iterator<Shot> shotIterator = paddle.shotList.iterator();
						while (shotIterator.hasNext()) {
							Shot shot = shotIterator.next();
							if (shot.y < 0) {
								shot.used = true;
								shotIterator.remove();
								continue;
							}
							testCollision(brick, shot, scoreboard, it, shotIterator);
						}
					}
				}

				if (brick.destroyed) {
					it.remove();
					scoreboard.increaseScore(difficulty, gameConfig.COUNT_BLOCKS_X, gameConfig.COUNT_BLOCKS_Y, bricks.isEmpty());
				}
			}

			Iterator<FallingBonus> itBonus = activeBonuses.iterator();
			while (itBonus.hasNext()) {
				FallingBonus fallingBonus = itBonus.next();
				fallingBonus.update(gameConfig.FT_STEP);
				testCollision(paddle, fallingBonus, itBonus);
			}

		}
	}

	private boolean launchNewLevel() throws IOException {
		String config = levelLoader.goToNextLevelConfig();
		if (config != null) {
			return launchLevel(config);
		} else {
			return false;
		}
	}

	private boolean relaunchcurrentLevel() throws IOException {
		scoreboard.score = scoreboard.initialScore;
		return launchLevel(levelLoader.getCurrentLevelConfig());
	}
	
	private boolean launchLevel(String levelConfig) throws IOException {
		if (!forceExitLevel) {
			
			int lives = scoreboard.lives.get();
			if(lives <= 0)
				lives = 1;
			
			int score = scoreboard.score;
			GameFrame gamePanel = new GameFrame(
					generalConfig,
					Difficulty.valueOf(generalConfig.loadString(GeneralConfig.KEY_DIFFICULTY)), 
					returnAction, 
					levelLoader);
			gamePanel.setLocationRelativeTo(this);
			gamePanel.scoreboard.lives = new AtomicInteger(lives);
			gamePanel.scoreboard.score = score;
			scoreboard.initialScore = score;

			gamePanel.run();
			
			return true;
		} else {
			forceExitLevel = false;
			return false;
		}
	}

	private void startEnabledFPSRefresher() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (running) {
					try {
						Thread.sleep(gameConfig.FPS_STRING_REFRESH_RATE);
						canReprintFps.set(true);

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	private void resetModifiersCount() {
		ExpansionModifier.resetModifiersCount();
		SpeedModifier.resetModifiersCount();
		FlameModifier.resetModifiersCount();
		ShotModifier.resetModifiers();
		PuppyModifier.resetModifiersCount();
	}
	
	public boolean askExitGame() {
		boolean pause = paused;
		paused = true;
		int choice = JOptionPane.showConfirmDialog(this, "Are you sure to exit this game? (Your progress will be lost)", "Exit Game?",
				JOptionPane.YES_NO_OPTION);

		paused = pause;
		
		boolean outcome = choice == JOptionPane.YES_OPTION;
		
		return outcome;
	}


	// KEY EVENT METHODS //
	
	@Override
	public void keyPressed(KeyEvent event) {

		switch (event.getKeyCode()) {

		case KeyEvent.VK_ESCAPE:
			if (askExitGame()) {
				forceExitLevel = true;
				running = false;
			}
			break;
		case KeyEvent.VK_ENTER:
			tryAgain = !scoreboard.finished;
			break;
		case KeyEvent.VK_N:
			if (scoreboard.win && levelLoader.hasNextLevel()) {
				goToNextLevel = true;
				running = false;
			}
			break;
		case KeyEvent.VK_SPACE:
			paused = !paused;
			pauseButton.isSelected = paused;
			break;
		case KeyEvent.VK_LEFT:
			paddle.moveLeft(gameConfig.PADDLE_VELOCITY);
			alignBallToPaddle();
			break;
		case KeyEvent.VK_RIGHT:
			paddle.moveRight(gameConfig.SCREEN_WIDTH, gameConfig.PADDLE_VELOCITY);
			alignBallToPaddle();
			break;
		case KeyEvent.VK_CONTROL:
			if (ball.ballStopped) {
				ball.shot();
			}
			break;
		default:
			break;
		}
	}

	private void alignBallToPaddle() {
		if (ball.ballStopped) {
			ball.velocityX = paddle.velocity;
			ball.x = paddle.x;
		}
	}

	@Override
	public void keyReleased(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			paddle.stopMove(gameConfig.SCREEN_WIDTH);
			alignBallToPaddle();
			break;
		case KeyEvent.VK_RIGHT:
			paddle.stopMove(gameConfig.SCREEN_WIDTH);
			alignBallToPaddle();
			break;
		default:
			break;
		}

		for (KeyEventModifier keyModifier : keyModifiers) {
			keyModifier.applyModifier(event);
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0) {

	}

}