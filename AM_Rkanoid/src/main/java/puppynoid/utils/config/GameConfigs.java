package puppynoid.utils.config;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import puppynoid.utils.GuiUtils;

public class GameConfigs extends AbstractConfig {
	
	public static final String GAME_TITLE = "Puppy-noid";
	public static final String GAME_WELCOME = "Welcome to " + GAME_TITLE;
	
	public static final Color DEFAULT_BUTTON_BACKGROUND = Color.DARK_GRAY;
	public static final Color DEFAULT_BUTTON_FOREGROUND = Color.WHITE;

	public int FPS_SLEEP = 10;
	public int FPS_STRING_REFRESH_RATE = 300;

	public int SCREEN_WIDTH = 1600;
	public int SCREEN_HEIGHT = 1000;
	
	public String BACKGROUND_IMAGE;
	public String BACKGROUND_SOUND_THEME;
	
	public int COUNT_BLOCKS_X = 11;
	public int COUNT_BLOCKS_Y = 4;

	public int PLAYER_LIVES = 5;

	public double BONUS_VELOCITY = 0.5;

	public double BALL_RADIUS = 10.0;
	public double BALL_VELOCITY = 0.5;

	public double PROJECTILE_VELOCITY = 0.6;

	public double PADDLE_WIDTH = 150.0;
	public double PADDLE_HEIGHT = 15.0;
	public double PADDLE_VELOCITY = 0.6;

	public double BLOCK_WIDTH = 60.0;
	public double BLOCK_HEIGHT = 20.0;

	public float BONUS_PROBABILITY = 10.0f;
	public double BONUS_SPEED = 0.7;
	public double BONUS_WIDTH = 15.0;
	public double BONUS_HEIGHT = 15.0;
	
	public Map<Point, Integer> HEALTH_BRICKS_MAP = new HashMap<Point, Integer>();
	public Map<Point, Boolean> HIDDEN_BRICKS_MAP = new HashMap<Point, Boolean>();

	public double FT_SLICE = 1.0;
	public double FT_STEP = 1.0;

	public String FONT = "Courier New";
	
	public Image background;
	
	public GameConfigs(String propertiesPath) throws IOException {
		
		super(propertiesPath); 
		
		initConfigs();
		
		try {
			background = GuiUtils.getResizedImageOpacity(
					BACKGROUND_IMAGE,
					0.4f,
					SCREEN_WIDTH,
					SCREEN_HEIGHT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void initConfigs() {
		FPS_SLEEP = loadInt("FPS_SLEEP");
		FPS_STRING_REFRESH_RATE = loadInt("FPS_STRING_REFRESH_RATE");
		SCREEN_WIDTH = loadInt("SCREEN_WIDTH");
		SCREEN_HEIGHT = loadInt("SCREEN_HEIGHT");
		COUNT_BLOCKS_X = loadInt("COUNT_BLOCKS_X");
		COUNT_BLOCKS_Y = loadInt("COUNT_BLOCKS_Y");
		PLAYER_LIVES = loadInt("PLAYER_LIVES");
		
		BONUS_VELOCITY = loadDouble("BONUS_VELOCITY");
		PROJECTILE_VELOCITY = loadDouble("PROJECTILE_VELOCITY");
		
		BALL_RADIUS = loadDouble("BALL_RADIUS");
		BALL_VELOCITY = loadDouble("BALL_VELOCITY");
		PADDLE_WIDTH = loadDouble("PADDLE_WIDTH");
		PADDLE_HEIGHT = loadDouble("PADDLE_HEIGHT");
		PADDLE_VELOCITY = loadDouble("PADDLE_VELOCITY");
		BLOCK_WIDTH = loadDouble("BLOCK_WIDTH");
		BLOCK_HEIGHT = loadDouble("BLOCK_HEIGHT");
		FT_SLICE = loadDouble("FT_SLICE");
		FT_STEP = loadDouble("FT_STEP");
		
		BONUS_PROBABILITY = loadFloat("BONUS_PROBABILITY");
		BONUS_SPEED = loadDouble("BONUS_SPEED");     
		BONUS_WIDTH = loadDouble("BONUS_WIDTH");     
		BONUS_HEIGHT = loadDouble("BONUS_HEIGHT");  
		
		HEALTH_BRICKS_MAP = loadHealthBricksMap();
		HIDDEN_BRICKS_MAP = loadHiddenBricksMap();
		
		FONT = properties.getProperty("FONT");

		if(properties.getProperty("BACKGROUND_IMAGE") == null)
			BACKGROUND_IMAGE = IMG_FOLDER + "backgrounds/back_1.jpg";
		else
			BACKGROUND_IMAGE = IMG_FOLDER + properties.getProperty("BACKGROUND_IMAGE");
		
		if(properties.getProperty("BACKGROUND_SOUND_THEME") == null)
			BACKGROUND_SOUND_THEME = "LevelThemes/Theme_1.wav";
		else
			BACKGROUND_SOUND_THEME = SOUNDS_FOLDER + properties.getProperty("BACKGROUND_SOUND_THEME");
			
	}
	
	private Map<Point,Integer> loadHealthBricksMap(){
		String rawHealthString = properties.getProperty("BRICK_HEALTH_LIST");
		String[] singleConfigs = rawHealthString.split(";");
		
		Map<Point,Integer> healthBricksMap = new HashMap<Point, Integer>();

		// Preload modified values
		for(String singleConf : singleConfigs) {
			try {
				String[] subConf = singleConf.split("-");
				Point point = new Point(Integer.parseInt(subConf[0]), Integer.parseInt(subConf[1]));
				Integer health = Integer.parseInt(subConf[2]);
				
				healthBricksMap.put(point, health);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		
		// Fill other values with health defaulted to 1
		for(int i = 0; i < COUNT_BLOCKS_X; i++) {
			for(int j = 0; j < COUNT_BLOCKS_Y; j++) {
				Point currentBrick = new Point(i,j);
				if(!healthBricksMap.containsKey(currentBrick)) {
					healthBricksMap.put(currentBrick, 1);
				}
			}
		}
		
		return healthBricksMap;
	}
	
	private Map<Point,Boolean> loadHiddenBricksMap(){
		String rawHealthString = properties.getProperty("HIDDEN_BRICKS_LIST");
		String[] singleConfigs = rawHealthString.split(";");
		
		Map<Point,Boolean> hiddenBricksMap = new HashMap<Point, Boolean>();

		// Preload modified values
		for(String singleConf : singleConfigs) {
			try {
				String[] subConf = singleConf.split("-");
				Point point = new Point(Integer.parseInt(subConf[0]), Integer.parseInt(subConf[1]));
				
				hiddenBricksMap.put(point, true);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		
		return hiddenBricksMap;
	}

}

