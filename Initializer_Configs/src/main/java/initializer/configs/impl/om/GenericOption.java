/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package initializer.configs.impl.om;

import java.util.Locale;

import javax.swing.JPanel;

public class GenericOption {

	// DEFAULT VALUES (usfeul to keep valid object during iniLoad, in case of null read or exception)
	public final static String DEFAULT_USERNAME = "";
	public final static String DEFAULT_CHARSET = "UTF-8";
	public final static float DEFAULT_VOLUME = 0.6F;
	public final static Integer DEFAULT_POS_X = 1;
	public final static Integer DEFAULT_POS_Y = 1;
	public final static boolean DEF_TxtEditorFileMonitorActive = true;
	public final static boolean DEF_exceptionAdvicesEnabled = false;
	public final static boolean DEF_automaticUpdateOn = true;
	public final static boolean DEF_automaticPluginUpdateOn = true;
	public final static Class<?> DEFAULT_PANEL_CLASS_AT_START = JPanel.class;
	public final static Locale DEF_locale = Locale.getDefault();
	public final static boolean DEF_firstAccess = true;
	/** Language of the interface chosen by the user: English until he chooses (also for the installations that were updated) */
	public final static String DEF_language = "en";

	// FILEDS
	private String userName;
	private String charset;
	private Integer lastKnownPositionX;
	private Integer lastKnownPositionY;
	private float generalVolume;
	private boolean TxtEditorFileMonitorActive;
	private boolean exceptionAdvicesEnabled;
	private Class<?> lastPanelOpened;
	private boolean automaticUpdateOn;
	private boolean automaticPluginUpdateOn;
	private boolean firstAccess; 
	private Locale locale;
	private String language = DEF_language;
	
	public GenericOption() {
		userName = DEFAULT_USERNAME;
		charset = DEFAULT_CHARSET;
		lastKnownPositionX = DEFAULT_POS_X;
		lastKnownPositionY = DEFAULT_POS_Y;
		generalVolume = DEFAULT_VOLUME;
		TxtEditorFileMonitorActive = DEF_TxtEditorFileMonitorActive;
		lastPanelOpened = DEFAULT_PANEL_CLASS_AT_START;
		exceptionAdvicesEnabled = DEF_exceptionAdvicesEnabled;
		automaticUpdateOn = DEF_automaticUpdateOn;
		automaticPluginUpdateOn = DEF_automaticPluginUpdateOn;
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

	public String getLanguage() {
		return language == null || language.trim().isEmpty() ? DEF_language : language;
	}

	public void setLanguage(String language) {
		this.language = language == null || language.trim().isEmpty() ? DEF_language : language.trim().toLowerCase();
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

	/** At start ScreepT looks for updates of the installed plugins (like automaticUpdateOn does for the software) */
	public boolean isAutomaticPluginUpdateOn() {
		return automaticPluginUpdateOn;
	}

	public void setAutomaticPluginUpdateOn(boolean automaticPluginUpdateOn) {
		this.automaticPluginUpdateOn = automaticPluginUpdateOn;
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
	
	public Integer getLastKnownPositionX() {
		return lastKnownPositionX;
	}

	public void setLastKnownPositionX(Integer lastKnownPositionX) {
		this.lastKnownPositionX = lastKnownPositionX;
	}

	public Integer getLastKnownPositionY() {
		return lastKnownPositionY;
	}

	public void setLastKnownPositionY(Integer lastKnownPositionY) {
		this.lastKnownPositionY = lastKnownPositionY;
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
