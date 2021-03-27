package puppynoid.om;

import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.util.Random;

import various.common.light.sounds.SoundsManager;
import various.common.light.utility.manipulation.ArrayHelper;
import puppynoid.gui.GameFrame;
import puppynoid.modifiers.AbstractModifier;
import puppynoid.modifiers.ExpansionModifier;
import puppynoid.modifiers.FlameModifier;
import puppynoid.modifiers.LifeModifier;
import puppynoid.modifiers.PuppyModifier;
import puppynoid.modifiers.ShotModifier;
import puppynoid.modifiers.SpeedModifier;
import puppynoid.utils.GuiUtils;
import puppynoid.utils.config.GameConfigs;
import puppynoid.utils.config.GeneralConfig;

public enum Bonus {
	SHOT(GameConfigs.IMG_FOLDER + "bonus_shot.png", null, Color.GRAY, 12000, ShotModifier.class, new String[]{GameConfigs.SOUNDS_FOLDER + "Shot.wav"}),
	SPEED(GameConfigs.IMG_FOLDER + "bonus_speed.png", null, Color.GREEN, 12000, SpeedModifier.class, new String[]{GameConfigs.SOUNDS_FOLDER + "Laser.wav"}),
	FLAME_BALL(GameConfigs.IMG_FOLDER + "bonus_flame.png", Color.RED, Color.RED, 12000, FlameModifier.class, new String[]{GameConfigs.SOUNDS_FOLDER + "Fire.wav"}),
	EXPANSION(GameConfigs.IMG_FOLDER + "bonus_expansion.jpg", null, Color.RED, 8000, ExpansionModifier.class, new String[]{GameConfigs.SOUNDS_FOLDER + "Laser.wav"}),
	LIFE(GameConfigs.IMG_FOLDER + "bonus_life.png", null, null, 20, LifeModifier.class, new String[]{GameConfigs.SOUNDS_FOLDER + "Health.wav"}),
	PUPPY(GameConfigs.IMG_FOLDER + "puppy_ball.png", null, null, 10000, PuppyModifier.class, new String[]{GameConfigs.SOUNDS_FOLDER + "Fart01.wav"});
	
	public static final int UNDEFINED_INT = -1;
	public static final Object UNDEFINED_OBJ = null;

	public static final int IMAGE_W = 25;
	public static final int IMAGE_H = 25;
	
	public Image icon;
	public Color ballColor;
	public Color paddleColor;
	public int timeMs;
	public Class<? extends AbstractModifier> modifierClass;
	public AbstractModifier modifierInstance;
	public String[] soundPaths;
	
	public FallingBonus fallingBonus;
	
	private Bonus(
			String imagePath, 
			Color ballColor, 
			Color paddleColor, 
			int timeMs, 
			Class<? extends AbstractModifier> modifier,
			String[] soundPaths) {
		try {
			icon = GuiUtils.getResizedImage(imagePath, IMAGE_W, IMAGE_H);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.ballColor = ballColor;
		this.paddleColor = paddleColor;
		this.timeMs = timeMs;
		this.modifierClass = modifier;
		this.soundPaths = soundPaths;
		
	}
	
	public void playRandomSound(Float volume) {
		String soundToPlay = ArrayHelper.getRandomElement(soundPaths, 100f);
		if(soundToPlay != null) {
			SoundsManager.playSound(soundToPlay, volume);
		}
	}
	
	
	public static Bonus getRandomBonus(float probabilityPerc) {
		
		if(probabilityPerc > 100f)
			probabilityPerc = 100f;

		if(probabilityPerc < 0f)
			probabilityPerc = 0f;
		
		int nValues = values().length;
		int progressive = 0;
			
		
		Random random = new Random();
		int randomInt = random.nextInt(100);
		
		for(int i = 0; i < nValues; i++) {
			progressive += probabilityPerc / (float)nValues;
			if(randomInt < progressive) {
				return values()[i];
			}
		}
		
		return null;
	}
	
	public void applyModifier(GameFrame gameFrame) throws InstantiationException, IllegalAccessException {
		if(this.modifierClass != null) {
			try {
				modifierInstance = this.modifierClass
						.getDeclaredConstructor(GameFrame.class, Bonus.class)
						.newInstance(gameFrame, this);
				
				if (modifierInstance.canAddModifier()) {
					modifierInstance.applyModifier();
					playRandomSound(gameFrame.generalConfig.loadFloat(GeneralConfig.KEY_EFFECTS_VOLUME));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public AbstractModifier getNewModifierInstance(GameFrame gameFrame) {
		if(this.modifierClass != null) {
			try {
				return this.modifierClass
						.getDeclaredConstructor(GameFrame.class, Bonus.class)
						.newInstance(gameFrame, this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public void resetModifier() {
		if(modifierInstance != null) {
			modifierInstance.resetOldBehaviour();
		}
	}
}
