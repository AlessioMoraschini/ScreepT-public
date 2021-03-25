package utils.config;

import java.io.IOException;

import various.common.light.utility.properties.PropertiesManager;

public class AbstractConfig {
	
	public static String GAME_CONFIG_FOLDER = "resources/config/";
	public static String IMG_FOLDER = "resources/images/";
	public static String SOUNDS_FOLDER = "resources/sounds/";
	
	protected PropertiesManager properties;
	protected String propertiesPath;

	public AbstractConfig(String propertiesPath) throws IOException {
		this.propertiesPath = propertiesPath;
		
		properties = new PropertiesManager(propertiesPath);  
	}
	
	public double loadDouble(String propName) {
		return Double.parseDouble(properties.getProperty(propName));
	}

	public float loadFloat(String propName) {
		return Float.parseFloat(properties.getProperty(propName));
	}

	public int loadInt(String propName) {
		return Integer.parseInt(properties.getProperty(propName));
	}

	public String loadString(String propName) {
		return properties.getProperty(propName);
	}
	
	public void reload() {
		properties.readFromFile();
	}
	
	public boolean saveAll() {
		return properties.saveAllProperties();
	}

	public void setProperty(String propName, int value, boolean save) {
		if (save) {
			properties.saveCommentedProperty(propName, String.valueOf(value));
		} else {
			properties.commentedProperties.setProperty(propName, String.valueOf(value));
		}
	}

	public void setProperty(String propName, boolean value, boolean save) {
		if (save) {
			properties.saveCommentedProperty(propName, String.valueOf(value));
		} else {
			properties.commentedProperties.setProperty(propName, String.valueOf(value));
		}
	}
	
	public void setProperty(String propName, double value, boolean save) {
		if (save) {
			properties.saveCommentedProperty(propName, String.valueOf(value));
		} else {
			properties.commentedProperties.setProperty(propName, String.valueOf(value));
		}
	}

	public void setProperty(String propName, float value, boolean save) {
		if (save) {
			properties.saveCommentedProperty(propName, String.valueOf(value));
		} else {
			properties.commentedProperties.setProperty(propName, String.valueOf(value));
		}
	}

	public void setProperty(String propName, String value, boolean save) {
		if (save) {
			properties.saveCommentedProperty(propName, value);
		} else {
			properties.commentedProperties.setProperty(propName, value);
		}
	}

	public PropertiesManager getProperties() {
		return properties;
	}

	public String getPropertiesPath() {
		return propertiesPath;
	}

}
