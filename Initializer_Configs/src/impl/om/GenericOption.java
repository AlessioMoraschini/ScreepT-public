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
package impl.om;

import java.util.Locale;

import javax.swing.JPanel;

public class GenericOption {

	// DEFAULT VALUES (usfeul to keep valid object during iniLoad, in case of null read or exception)
	public final static String DEFAULT_USERNAME = "";
	public final static String DEFAULT_CHARSET = "UTF-8";
	public final static float DEFAULT_VOLUME = 0.6F;
	public final static boolean DEF_TxtEditorFileMonitorActive = true;
	public final static boolean DEF_exceptionAdvicesEnabled = false;
	public final static boolean DEF_automaticUpdateOn = true;
	public final static Class<?> DEFAULT_PANEL_CLASS_AT_START = JPanel.class;
	public final static Locale DEF_locale = Locale.getDefault();
	public final static boolean DEF_firstAccess = true;

	// FILEDS
	private String userName;
	private String charset;
	private float generalVolume;
	private boolean TxtEditorFileMonitorActive;
	private boolean exceptionAdvicesEnabled;
	private Class<?> lastPanelOpened;
	private boolean automaticUpdateOn;
	private boolean firstAccess; 
	private Locale locale;
	
	public GenericOption() {
		userName = DEFAULT_USERNAME;
		charset = DEFAULT_CHARSET;
		generalVolume = DEFAULT_VOLUME;
		TxtEditorFileMonitorActive = DEF_TxtEditorFileMonitorActive;
		lastPanelOpened = DEFAULT_PANEL_CLASS_AT_START;
		exceptionAdvicesEnabled = DEF_exceptionAdvicesEnabled;
		automaticUpdateOn = DEF_automaticUpdateOn;
		locale = DEF_locale;
		firstAccess = DEF_firstAccess;
	}
	
	public GenericOption(String user, String chars, boolean ultraSec, float genVolume) {
		this();
		userName = user;
		charset = chars;
		generalVolume = genVolume;
	}
	
	public boolean isFirstAccess() {
		return firstAccess;
	}

	public void setFirstAccess(boolean firstAccess) {
		this.firstAccess = firstAccess;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public boolean isAutomaticUpdateOn() {
		return automaticUpdateOn;
	}

	public void setAutomaticUpdateOn(boolean automaticUpdateOn) {
		this.automaticUpdateOn = automaticUpdateOn;
	}

	public boolean isExceptionAdvicesEnabled() {
		return exceptionAdvicesEnabled;
	}

	public void setExceptionAdvicesEnabled(boolean exceptionAdvicesEnabled) {
		this.exceptionAdvicesEnabled = exceptionAdvicesEnabled;
	}

	public boolean isTxtEditorFileMonitorActive() {
		return TxtEditorFileMonitorActive;
	}

	public void setTxtEditorFileMonitorActive(boolean txtEditorFileMonitorActive) {
		TxtEditorFileMonitorActive = txtEditorFileMonitorActive;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public float getGeneralVolume() {
		return generalVolume;
	}

	public void setGeneralVolume(float generalVolume) {
		this.generalVolume = generalVolume;
	}
	
	public String getVolumeString() {
		return String.format("%.2f", getGeneralVolume()*100)+"%";
	}

	public Class<?> getLastPanelOpened() {
		return lastPanelOpened;
	}

	public void setLastPanelOpened(Class<?> lastPanelOpened) {
		this.lastPanelOpened = lastPanelOpened;
	}

}
