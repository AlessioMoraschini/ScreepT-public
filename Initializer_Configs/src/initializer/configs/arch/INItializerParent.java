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
package initializer.configs.arch;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

import various.common.light.files.om.FileNamed;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.reflection.ReflectionUtils;
import various.common.light.utility.string.StringWorker;

public abstract class INItializerParent implements INItializerInterface{

	protected static SafeLogger logger = new SafeLogger(INItializerParent.class);
	public static final String SEPARATOR_DEFAULT = ";";
	public static final String SEPARATOR_INNER_DEFAULT = "::";
	public static final String SEPARATOR_INNER_DEFAULT_LONG = ":;:;";

	public static final String USER_PERSONAL_DIR = System.getProperty("user.home")+"/";

	// FIELDS
	protected Ini iniReadFile;
	protected Wini iniFile;

	public INItializerParent(String iniPath) {
		try {
			iniReadFile = new Wini(new File(iniPath));
			iniFile = new Wini(new File(iniPath));
		} catch (Exception e) {
			logger.error("INItializer() -> Exception happened initializing configuration!", e);
		}
	}

	public INItializerParent(Wini configFile, String defaultIniPath) {
		// initialize both read and write ini variable, to keep them synchronyzed
		if(configFile!=null) {
			iniReadFile = configFile;
			iniFile = configFile;
		} else try {
			iniReadFile = new Wini(new File(defaultIniPath));
			iniFile = new Wini(new File(defaultIniPath));
		} catch (IOException e) {
			logger.error("INItializer(Wini configFile) -> Exception happened initializing configuration!", e);
		}
	}

	@Override
	public abstract void readConfigIniFile();

	@Override
	public abstract void writeCurrentConfigIniFile();


	/**
	 * Use this to store all pending changes to file (options must have been pre-written with <b>writeXxxOptions()</b>)
	 */
	public void store() {
		// WRITE ALL VALUES TO FILE
		try {
			iniFile.store();
		} catch (IOException e) {
			logger.error("Exception happened!", e);
		}
	}

	/**
	 * Read a value from a given INI file in String format : nullSafe
	 * @param iniToReadPath
	 * @param OPT_SECTION
	 * @param OPT_VAR
	 * @return
	 * @throws InvalidFileFormatException
	 * @throws IOException
	 */
	public static String readFromFile(String iniToReadPath, String OPT_SECTION, String OPT_VAR) throws InvalidFileFormatException, IOException {
		String valueRead = "";
		if (iniToReadPath != null && new File(iniToReadPath).exists()) {
			Ini tempIniFile = new Wini(new File(iniToReadPath));
			valueRead = StringWorker.trimToEmpty(tempIniFile.get(OPT_SECTION, OPT_VAR));
		}

		return valueRead;
	}

	/**
	 * Read a value from loaded INI file in String format : nullSafe
	 * @param OPT_SECTION
	 * @param OPT_VAR
	 * @return
	 * @throws InvalidFileFormatException
	 * @throws IOException
	 */
	public String readFromFile(String OPT_SECTION, String OPT_VAR) throws InvalidFileFormatException, IOException {
		return readFromFile(iniReadFile.getFile().getCanonicalPath(), OPT_SECTION, OPT_VAR);
	}

	/**
	 * Writes a value inside a given file, returning true if value is effectively written
	 * @param iniToReadPath
	 * @param OPT_SECTION
	 * @param OPT_VAR
	 * @param OPT_VALUE
	 * @return
	 * @throws InvalidFileFormatException
	 * @throws IOException
	 */
	public static boolean writeToFile(String iniToReadPath, String OPT_SECTION, String OPT_VAR, String OPT_VALUE) throws InvalidFileFormatException, IOException {
		boolean valueWritten = false;
		if (iniToReadPath != null && new File(iniToReadPath).exists()) {
			Wini iniTempFile = new Wini(new File(iniToReadPath));
			iniTempFile.load();

			iniTempFile.put(OPT_SECTION, OPT_VAR, OPT_VALUE);
			iniTempFile.store();
			String result = readFromFile(iniToReadPath, OPT_SECTION, OPT_VAR);
			if(result != null && result.equals(OPT_VALUE)) {
				valueWritten = true;
			}
		}

		return valueWritten;
	}

	/**
	 * This method is used to write on file single opt values in a precise section
	 * @return true if no exception is throwed
	 */
	public synchronized boolean writeSingleValue(String OPT_SECTION, String OPT_VAR, String OPT_VALUE ) {
		boolean OK = false;

		synchronized (iniFile) {
			try {
				iniFile.put(OPT_SECTION, OPT_VAR, OPT_VALUE);
				iniFile.store();
				OK = true;
				logger.info(iniFile.getFile().getCanonicalPath()+" \n\n Single option saved -> "+OPT_SECTION+" - "+OPT_VAR+" : "+OPT_VALUE);
			} catch (IOException e) {
				OK = false;
				logger.error("Exception happened!", e);
			}
		}
		return OK;
	}

	/**
	 * This method is used to write on file single opt values in a precise section
	 * @return true if no exception is throwed
	 */
	public synchronized boolean writeSingleValue(String OPT_SECTION, String OPT_VAR, float OPT_VALUE ) {
		boolean OK = false;

		synchronized (iniFile) {
			try {
				iniFile.put(OPT_SECTION, OPT_VAR, OPT_VALUE);
				iniFile.store();
				OK = true;
				logger.info(iniFile.getFile().getCanonicalPath()+" \n\n Single option saved -> "+OPT_SECTION+" - "+OPT_VAR+" : "+OPT_VALUE);
			} catch (IOException e) {
				OK = false;
				logger.error("Exception happened!", e);
			}
		}
		return OK;
	}

	/**
	 * This method is used to write on file single opt values in a precise section (INT value)
	 * @return true if no exception is throwed
	 */
	public synchronized boolean writeSingleValue(String OPT_SECTION, String OPT_VAR, int OPT_VALUE ) {
		boolean OK = false;

		synchronized (iniFile) {
			try {
				iniFile.put(OPT_SECTION, OPT_VAR, OPT_VALUE);
				iniFile.store();
				OK = true;
				logger.info(iniFile.getFile().getCanonicalPath()+" \n\n Single option saved -> "+OPT_SECTION+" - "+OPT_VAR+" : "+OPT_VALUE);
			} catch (IOException e) {
				OK = false;
				logger.error("Exception happened!", e);
			}
		}
		return OK;
	}

	/**
	 * This method is used to write on file single opt values in a precise section (INT value)
	 * @return true if no exception is throwed
	 */
	public synchronized boolean writeSingleValue(String OPT_SECTION, String OPT_VAR, boolean OPT_VALUE ) {
		boolean OK = false;

		synchronized (iniFile) {
			try {
				iniFile.put(OPT_SECTION, OPT_VAR, OPT_VALUE);
				iniFile.store();
				OK = true;
				logger.info(iniFile.getFile().getCanonicalPath()+" \n\n Single option saved -> "+OPT_SECTION+" - "+OPT_VAR+" : "+OPT_VALUE);
			} catch (IOException e) {
				OK = false;
				logger.error("Exception happened!", e);
			}
		}
		return OK;
	}

	public String getSafeStringConf(String section, String variable, String defIfBlank) {
		String result = iniReadFile.get(section, variable);
		result = (StringWorker.isEmpty(result))? defIfBlank : result;

		return result;
	}

	public File getSafeFileConf(String section, String variable, String defPathIfBlank) {
		File result = new File(getSafeStringConf(section, variable, defPathIfBlank));
		result = (result == null || !result.exists() || !result.isFile()) ? new File(defPathIfBlank) : result;

		return result;
	}

	public boolean getBooleanFromString(String value, boolean defIfBlank) {
		boolean result = false;
		if(value == null || "".equals(value)) {
			result = defIfBlank;
		}else {
			result = Boolean.valueOf(value);
		}

		return result;
	}

	public static int getIntVarFromString(String val, int defaultVal) {
		try {
			return Integer.valueOf(val);
		}catch(Exception e){
			logger.warn("Catched exception, default value [" + defaultVal + "] will be used", e);
			return defaultVal;
		}
	}

	public static Float getFloatVarFromString(String val, Float defaultVal) {
		try {
			return Float.valueOf(val);
		}catch(Exception e){
			logger.warn("Catched exception, default value [" + defaultVal + "] will be used", e);
			return defaultVal;
		}
	}

	public static Double getDoubleVarFromString(String val, Double defaultVal) {
		try {
			return Double.valueOf(val);
		}catch(Exception e){
			logger.warn("Catched exception, default value [" + defaultVal + "] will be used", e);
			return defaultVal;
		}
	}

	public static File getFileFromString(String val, File defaultVal) {
		try {
			if(val != null)
				return new File(val);
			else
				return defaultVal;

		}catch(Exception e){
			logger.warn("Catched exception, default value [" + defaultVal + "] will be used", e);
			return defaultVal;
		}
	}

	public static Vector<FileNamed> getFileNamedListFromString(String val, String separator, Vector<FileNamed> defaultList) {
		val = StringWorker.trimToEmpty(val);
		separator = StringWorker.isEmptyNoTrim(separator) ? ";" : separator;

		Vector<FileNamed> list = new Vector<>();
		try {
			String[] parts = val.split(separator);
			for(int i = 0; i < parts.length; i++) {
				list.add(new FileNamed(parts[i]));
			}

		} catch (Exception e1) {
			logger.warn("Catched exception, default value [" + defaultList + "] will be used", e1);
			return defaultList;
		}

		return list;
	}
	public static Vector<String> getStringListFromString(String val, String separator, Vector<String> defaultList) {
		Vector<String> list = new Vector<>();
		try {
			String[] parts = val.split(separator);
			for(int i = 0; i < parts.length; i++) {
				list.add(parts[i]);
			}

		} catch (Exception e1) {
			logger.warn("Catched exception, default value [" + defaultList + "] will be used", e1);
			return defaultList;
		}

		return list;
	}

	public static Vector<String[]> getMatrixOfStringsFromString(String val, String separator, String innerSeparator, Vector<String[]> defaultList) {
		Vector<String> lastFiltersRows = getStringListFromString(val, separator, new Vector<String>());
		Vector<String[]> lastFilters = new Vector<>();
		for(String row : lastFiltersRows) {
			Vector<String> currRowFilters = getStringListFromString(StringWorker.trimToEmpty(row), innerSeparator, new Vector<String>());
			if(!currRowFilters.isEmpty()) {
				lastFilters.add(currRowFilters.toArray(new String[currRowFilters.size()]));
			}
		}
		return lastFilters;
	}



	public static <T> Map<String, T> getMapFromKeyList(List<String> keys, List<T> values, T defaultNullVal) {
		Map<String, T> map = new HashMap<>();

		int i = 0;
		for(String key : keys) {
			T value = null;
			try {
				value = values.get(i) != null ? values.get(i) : defaultNullVal;
			} catch(Exception e) {
				value = defaultNullVal;
			}

			map.put(key, value);
			i++;
		}

		return map;
	}

	public static Map<String, String> getStringMapFromString(String val, String separator, String innerSeparator, Map<String, String> defaultMap) {
		Map<String, String> map = new HashMap<>();
		try {
			StringTokenizer tokenizerOpenedFiles = new StringTokenizer(val, separator);
			while(tokenizerOpenedFiles.hasMoreTokens()) {
				String token = tokenizerOpenedFiles.nextToken();
				String[] splittedKeyVal = StringWorker.isEmpty(token) ? new String[0] : token.split(StringWorker.trimToEmpty(innerSeparator));

				if(splittedKeyVal.length == 1) {
					map.put(StringWorker.trimToEmpty(splittedKeyVal[0]), "");

				} else if(splittedKeyVal.length == 2) {
					map.put(StringWorker.trimToEmpty(splittedKeyVal[0]), StringWorker.trimToEmpty(splittedKeyVal[1]));
				}
			}

		} catch (Exception e1) {
			logger.error(e1);
			return map;
		}

		return map;
	}

	public static ArrayList<String> getStringListFromString(String val, String separator, ArrayList<String> defaultList) {
		ArrayList<String> list = new ArrayList<>();
		try {
			StringTokenizer tokenizerOpenedFiles = new StringTokenizer(val, separator);
			while(tokenizerOpenedFiles.hasMoreTokens()) {
				list.add(tokenizerOpenedFiles.nextToken());
			}

		} catch (Exception e1) {
			logger.error(e1);
			return defaultList;
		}

		return list;
	}

	public String listToString(List<String> loadedFiles) {
		String openedFiles = "";
		// concateno gli elementi della lista e, tranne l'ultimo, aggiungo una virgola
		for(String currentPath : loadedFiles) {
			openedFiles += currentPath+",";
		}
		if(openedFiles.length() > 0) {
			openedFiles = openedFiles.substring(0, openedFiles.length()-1);
		}
		return openedFiles;
	}

	public static String mapToString(Map<String, String> map) {
		String stringMap = "";

		if(map == null || map.isEmpty())
			return stringMap;

		String[] keys = map.keySet().toArray(new String[map.size()]);

		for(int i = 0; i < keys.length; i++) {

			stringMap += keys[i] + SEPARATOR_INNER_DEFAULT + map.get(keys[i]);
			if(i < keys.length - 1)
				stringMap += SEPARATOR_DEFAULT;
		}

		return stringMap;
	}

	@Override
	public String toStringa() {
		List<Field> list = ReflectionUtils.retrieveAllFields(getClass());
		StringBuilder builder = new StringBuilder();
		for (Field i : list) {
			builder.append(i.toGenericString());
		}

		return builder.toString();
	};

	public Wini getIniFile() {
		return iniFile;
	}

	public Ini getIniReadFile() {
		return iniReadFile;
	}

	public void setIniReadFile(Ini iniReadFile) {
		this.iniReadFile = iniReadFile;
	}

	public void setIniFile(Wini iniFile) {
		this.iniFile = iniFile;
	}

	// Utility methods //

	 /**
     * parse font from a representing string
     */
    public static Font getFontFromString(String stringFont) {
    	return ConversionUtils.getFontFromString(stringFont);
    }

    /**
     * parse font from a representing string
     */
    public static Font getFontFromString(String stringFont, Font defaultFont) {
    	return ConversionUtils.getFontFromString(stringFont, defaultFont);
    }

    /**
     * return font in string format
     * @return
     */
    public static String getFontString(Font srcFont) {
    	return ConversionUtils.getFontString(srcFont);
    }

    /**
     * create color from String RGB separed with "-" representation (red-green-blue)
     */
    public static Color getRgbColorFromString(String color) {
    	return ConversionUtils.getColorFromRgbString(color);
    }

    /**
     * Convert color to string RGB
     */
    public static String getRgbString(Color sourceColor) {
    	return ConversionUtils.getRgbString(sourceColor, "-");
    }
}
