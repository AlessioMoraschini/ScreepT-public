/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.utility.properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ArrayHelper;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.string.StringWorker;

public class PropertiesManager {

	public static SafeLogger logger = new SafeLogger(PropertiesManager.class);
	private static SimpleDateFormat GENERIC_FORMATTER = new SimpleDateFormat("yyyy/MM/dd - hh:mm:ss");

	public File dynamicFileBindingsDefSource;
	public Properties dynamicFileBindingsDef;
	public CommentedProperties commentedProperties;

	public static void setLoggerEnabled(boolean enabled) {
		logger.setLoggerEnabled(enabled);
	}

	/**
	 * To check if definition had success, check if field <b>dynamicFileBindingsDefSource</b> is null after object's creation:
	 * in that case something has gone wrong
	 *
	 * @param bindingsFileDefinitionSource
	 */
	public PropertiesManager(String bindingsFileDefinitionSource) {
		dynamicFileBindingsDefSource = new File(bindingsFileDefinitionSource);
		dynamicFileBindingsDef = new Properties() {
			private static final long serialVersionUID = 2357857735678865627L;

			@Override
		    public synchronized Enumeration<Object> keys() {
		        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		    }
		};

		if (!readFromFile()) {
			dynamicFileBindingsDefSource = null;
		}

		if (!loadCommentedProperties()) {
			commentedProperties = null;
		}
	}

	private boolean loadCommentedProperties() {
		commentedProperties = new CommentedProperties();
		if (dynamicFileBindingsDefSource != null) {

        	synchronized (dynamicFileBindingsDefSource) {
				try (InputStream input = new FileInputStream(dynamicFileBindingsDefSource)) {

					commentedProperties.load(input);
					return true;

				} catch (IOException ex) {
					logger.error("Cannot prepare commented properties file mapping !!");
					return false;
				}
			}
		}else {
			return false;
		}
	}

	/**
	 * To check if definition had success, check if field <b>dynamicFileBindingsDefSource</b> is null after object's creation:
	 * in that case something has gone wrong
	 *
	 * @param bindingsFileDefinitionSource
	 */
	public PropertiesManager(File bindingsFileDefinitionSource) {
		dynamicFileBindingsDefSource = bindingsFileDefinitionSource;
		dynamicFileBindingsDef = new Properties();

		if (!readFromFile()) {
			dynamicFileBindingsDefSource = null;
		}

		if (!loadCommentedProperties()) {
			commentedProperties = null;
		}
	}

	public PropertiesManager(Properties bindingsFileDefinitionDef) {
		dynamicFileBindingsDef = bindingsFileDefinitionDef;
		dynamicFileBindingsDefSource = new File("");

		if (!loadCommentedProperties()) {
			commentedProperties = null;
		}
	}

	/**
	 * Avoid null values reading from properties
	 * @return
	 */
	public String getPropertyNullable(String key) {
		if(dynamicFileBindingsDef != null) {
			return dynamicFileBindingsDef.getProperty(key);
		}

		return "";
	}

	/**
	 * Avoid null values reading from properties
	 * @return
	 */
	public String getProperty(String key) {
		logger.debug("Loading property: " + key + " From cached properties: " + dynamicFileBindingsDefSource);

		String res = "";
		if(dynamicFileBindingsDef != null) {
			res = dynamicFileBindingsDef.getProperty(key);
			res =  StringWorker.trimToEmpty(res);
		}
		logger.debug("Loaded property: " + res);
		return res;
	}

	public ArrayList<File> getFileList(String key, String separator, boolean excludeNonExisting){
		ArrayList<String> rawList = getStringList(key, separator);
		ArrayList<File> fileList = new ArrayList<File>();

		if(rawList == null || rawList.isEmpty()) {
			return fileList;
		}

		for(String current : rawList) {
			File currentFile = new File(current);
			if(!excludeNonExisting || currentFile.exists()) {
				fileList.add(currentFile);
			}
		}

		return fileList;
	}

	public static String listToString(List<String> stringList, String separator) {
		String concatenation = "";
		// concatenate each element with given separator
		for(String currentString : stringList) {
			concatenation += currentString + separator;
		}
		if(concatenation.length() > 0) {
			concatenation = concatenation.substring(0, concatenation.length() - separator.length());
		}
		return concatenation;
	}

	/**
	 * Avoid null values reading from properties
	 * @return
	 */
	public String getProperty(String key, String defaultVal) {
		String res = "";
		if(dynamicFileBindingsDef != null) {
			res = dynamicFileBindingsDef.getProperty(key);
			res = (res != null)? StringWorker.trimToEmpty(res) : defaultVal;
		}

		return res;
	}

	/**
	 * Use this to refresh properties
	 * @return
	 */
	public boolean readFromFile() {

        if (dynamicFileBindingsDefSource != null) {

        	synchronized (dynamicFileBindingsDefSource) {
				try (InputStream input = new FileInputStream(dynamicFileBindingsDefSource)) {

					dynamicFileBindingsDef.load(input);
					return true;

				} catch (IOException ex) {
					logger.error("Cannot prepare properties file mapping !!");
					return false;
				}
			}

		}else {
			return false;
		}
    }

	/**
	 * Set the property value and save current loaded properties to file
	 * This will maintain comments
	 * @param propertyName
	 * @param value
	 */
	public void saveCommentedProperty(String propertyName, String value) {
		saveCommentedProperty(propertyName, value, true);
	}

	public void saveCommentedProperty(String propertyName, String value, boolean saveToFile) {

		java.io.OutputStream out = null;
		try {
			dynamicFileBindingsDef.setProperty(propertyName, value);
        	logger.info("Saving property in " + dynamicFileBindingsDefSource + "\n\n[" + propertyName + " : " + value);
        	commentedProperties.setProperty(propertyName, value);
        	if (saveToFile) {
				out = new FileOutputStream(dynamicFileBindingsDefSource);
				commentedProperties.store(out, "# Updated on " + GENERIC_FORMATTER.format(new Date()));
				logger.info("Saved!");
			}
		} catch (Throwable e) {
			logger.error("Cannot save commented properties", e);
		} finally {
			if (saveToFile && out != null) {
				try {
					out.flush();
					out.close();
				} catch (Exception e1) {
					logger.error("Cannot close streams", e1);
				}
			}
		}
	}

	/**
	 * Set the property value and save current loaded properties to file, returning boolean value representing the result of operation
	 * This will not maintain comments
	 * @param propertyName
	 * @param value
	 */
	public boolean savePropertyChecked(String key, String value){
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(dynamicFileBindingsDefSource);
			dynamicFileBindingsDef.setProperty(key, value);
			dynamicFileBindingsDef.store(out, null);
		} catch (Exception e) {
			return false;
		} finally {
			try {
				out.close();
			} catch (Exception e2) {
			}
		}
		return true;
	}

	/**
	 * Set the property value and save current loaded properties to file
	 * This will not maintain comments
	 * @param propertyName
	 * @param value
	 *
	 * @Deprecated -> use save Commented property, it's preferrable to this one
	 */
	@Deprecated
	public void saveProperty(String propertyName, String value) {
		dynamicFileBindingsDef.setProperty(propertyName, value);
		dynamicFileBindingsDef.put(propertyName, value);
		java.io.OutputStream out = null;
		try {
			logger.info("Saving property in " + dynamicFileBindingsDefSource + "\n\n[" + propertyName + " : " + value);
			out = new FileOutputStream(dynamicFileBindingsDefSource);
			dynamicFileBindingsDef.store(out, "# Updated on " + GENERIC_FORMATTER.format(new Date()));
        	logger.info("Saved!");
		} catch (IOException e1) {
			logger.error("", e1);
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e1) {
				logger.error("", e1);
			}
		}
	}

	public boolean saveAllProperties() {
		java.io.OutputStream out = null;
		try {
			logger.info("Saving properties in " + dynamicFileBindingsDefSource + "\n\n");
			out = new FileOutputStream(dynamicFileBindingsDefSource);
			dynamicFileBindingsDef.store(out, "# Updated on " + GENERIC_FORMATTER.format(new Date()));
        	logger.info("Saved!");
        	return true;
		} catch (IOException e1) {
			logger.error("An error occurred while saving all properties", e1);
			return false;
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e1) {
				logger.error("Cannot close streams", e1);
			}
		}
	}

	public boolean removeProperty(String key){
		return removeProperty(key, true);
	}

	public boolean removeProperty(String key, boolean writeFile){
		FileOutputStream out = null;

		logger.debug("Removing property: " + key);

		try {
			dynamicFileBindingsDef.remove(key);
			if(writeFile) {
				out = new FileOutputStream(dynamicFileBindingsDefSource);
				dynamicFileBindingsDef.store(out, null);
			}
		} catch (IOException e) {
			logger.error("Cannot remove property: " + key);
			return false;
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (Exception e2) {
				logger.error("Cannot close properties file stream: " + dynamicFileBindingsDef);
			}
		}

		logger.debug("Property removed succesfully: " + key);
		return true;
	}

	public int getIntVarFromProps(String key, int defaultVal) {
		String val = dynamicFileBindingsDef.getProperty(key);
		try {
			return Integer.valueOf(val);
		}catch(Exception e){
			return defaultVal;
		}
	}

	public ArrayList<String> getStringList(String key, String separator){
		ArrayList<String> list = new ArrayList<>();

		String rawList = dynamicFileBindingsDef.getProperty(key);

		if(rawList == null || "".equals(rawList)) {
			return list;
		}

		String[] splitted = rawList.split(separator);
		splitted = ArrayHelper.removeEmpties(splitted);

		list.addAll(Arrays.asList(splitted));

		return list;
	}

	public boolean getBooleanVarFromProps(String key, boolean defaultVal) {
		String val = dynamicFileBindingsDef.getProperty(key);
		try {
			return Boolean.valueOf(val);
		}catch(Exception e){
			return defaultVal;
		}
	}

	public float getFloatVarFromProps(String key, float defaultVal) {
		String val = dynamicFileBindingsDef.getProperty(key);
		try {
			return Float.valueOf(val);
		}catch(Exception e){
			return defaultVal;
		}
	}

	/**
	 * Read color from property string in form "R-G-B" with integer values
	 **/
	public Color getColorVarFromProps(String key, Color defaultVal) {
		String val = dynamicFileBindingsDef.getProperty(key);
		try {
			String[] rgb = val.split("-");
	    	if(rgb.length != 3) {
	    		return new Color(0,0,0);
	    	}
	    	int red = Integer.parseInt(rgb[0]);
	    	int green = Integer.parseInt(rgb[1]);
	    	int blue = Integer.parseInt(rgb[2]);

	    	return new Color(red, green, blue);
		}catch(Exception e){
			return defaultVal;
		}
	}

	/**
	 * Parse dimension from property string in the form of "int-int", identified by his String key in properties
	 */
	public Dimension getDimensionFromString(String separator, String key, Dimension defaultDim) {
		int width;
		int height;
		String source = StringWorker.trimToEmpty(dynamicFileBindingsDef.getProperty(key));

		if (source.equals("")) {return defaultDim;};

		try {
			String[] splitted = source.split(separator);
			width = Integer.parseInt(splitted[0]);
			height = Integer.parseInt(splitted[1]);
			return new Dimension(width, height);
		} catch (Exception e) {
			return defaultDim;
		}

	}

    /**
     * parse font from a representing string
     */
    public Font getFontFromString(String key, Font defaultFont) {
    	String stringFont = StringWorker.trimToEmpty(dynamicFileBindingsDef.getProperty(key));
    	if("".equals(StringWorker.trimToEmpty(stringFont))) return defaultFont;

    	return ConversionUtils.getFontFromString(stringFont, defaultFont);
    }

	// GETTERS AND SETTERS //

	public File getDynamicFileBindingsDefSource() {
		return dynamicFileBindingsDefSource;
	}

	public void setDynamicFileBindingsDefSource(File dynamicFileBindingsDefSource) {
		this.dynamicFileBindingsDefSource = dynamicFileBindingsDefSource;
	}

	public Properties getDynamicFileBindingsDef() {
		return dynamicFileBindingsDef;
	}

	public void setDynamicFileBindingsDef(Properties dynamicFileBindingsDef) {
		this.dynamicFileBindingsDef = dynamicFileBindingsDef;
	}


}
