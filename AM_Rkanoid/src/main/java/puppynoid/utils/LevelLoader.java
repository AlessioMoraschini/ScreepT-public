package puppynoid.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import puppynoid.utils.config.GameConfigs;

public class LevelLoader {
	
	private List<String> levels;
	
	private AtomicInteger currentLevel = new AtomicInteger(0);
	
	public LevelLoader() {
		currentLevel = new AtomicInteger(0);
		levels = initLevels();
	}
	
	public List<String> initLevels(){
		
		File levelsFolder = new File(GameConfigs.GAME_CONFIG_FOLDER);
		return Arrays.asList(levelsFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return name.startsWith("config_") && name.endsWith(".properties");
			}
		}));

	}

	public String getCurrentLevelConfig() {
		try {
			return GameConfigs.GAME_CONFIG_FOLDER + levels.get(currentLevel.get());
		}
		catch(Exception e) {
			return null;
		}
	}
	
	public String goToNextLevelConfig() {
		if(!hasNextLevel()) {
			return null;
		} else {
			currentLevel.incrementAndGet();
			return getCurrentLevelConfig();
		}
	}
	
	public void setNextLevel() {
		if(!hasNextLevel()) {
		} else {
			currentLevel.incrementAndGet();
		}
	}
	
	public boolean hasNextLevel() {
		return !levels.isEmpty() && (currentLevel.get() + 1) < levels.size();
	}
	
	/**
	 * Get 1-based current level n�
	 */
	public int getCurrentLevelN() {
		return currentLevel.get() + 1;
	}
	
	public void backToFirstLevel() {
		currentLevel = new AtomicInteger(0);
	}
	
	public boolean goToLevel(int level) {
		if(0 <= level && level < levels.size()) {
			currentLevel = new AtomicInteger(level);
			return true;
		}
		
		return false;
	}

}
