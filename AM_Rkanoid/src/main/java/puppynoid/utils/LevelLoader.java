package puppynoid.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
		String[] names = levelsFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				// levels are config_1.properties ... config_N.properties (config_0 was a test level of old versions)
				return levelNumber(name) >= 1;
			}
		});
		if (names == null) {
			return new ArrayList<String>();
		}

		// numeric order: config_2 comes before config_10 (the folder listing is alphabetical)
		List<String> sorted = new ArrayList<String>(Arrays.asList(names));
		Collections.sort(sorted, new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				return Integer.compare(levelNumber(a), levelNumber(b));
			}
		});
		return sorted;
	}

	/** N of a file called config_N.properties, -1 for any other file */
	static int levelNumber(String fileName) {
		if (!fileName.startsWith("config_") || !fileName.endsWith(".properties")) {
			return -1;
		}
		try {
			return Integer.parseInt(fileName.substring("config_".length(), fileName.length() - ".properties".length()));
		} catch (NumberFormatException e) {
			return -1;
		}

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

	/** How many levels the game has */
	public int getLevelsCount() {
		return levels.size();
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
