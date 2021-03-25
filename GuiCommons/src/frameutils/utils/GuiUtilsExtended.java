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
package frameutils.utils;


import java.awt.Cursor;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import dialogutils.JOptionHelperExtended;
import gui.GuiUtils;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import resources.SoundsConfigurator;
import resources.SoundsManagerExtended;
import utility.string.CustomCodeFormatter;
import utility.string.StringWorker;

public class GuiUtilsExtended extends GuiUtils {
	
	// INItializer logger creation		
	public static Logger logger = Logger.getLogger(GuiUtilsExtended.class); 
	
	public static final Cursor CURSOR_WAIT = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	public static final Cursor CURSOR_DEFAULT = Cursor.getDefaultCursor();
	public static final Cursor CURSOR_TEXT = new Cursor(Cursor.TEXT_CURSOR);
	public static final Cursor CURSOR_HAND = new Cursor(Cursor.HAND_CURSOR);
	public static final Cursor CURSOR_CROSS = new Cursor(Cursor.CROSSHAIR_CURSOR);
	
	public static final Dimension DEF_CUSTOM_CURSOR_SIZE = new Dimension(8,8);
	public static Cursor CURSOR_COLOR_PICK;
	
	public static JOptionHelperExtended dialogHelper = new JOptionHelperExtended(null);
	public static CustomCodeFormatter codeFormatter;
	
	public static final String XML = "xml";
	public static final String JS = "js";
	public static final String JSON = "json";
	public static final String HTML = "html";
	public static final String CSS = "css";
	public static final String JAVA = "java";
	public static final String[] supportedTypes = new String[]{
			XML, JS, JSON, HTML, CSS, JAVA
	};
	public static final List<String> supportedTypesList = new ArrayList<>(Arrays.asList(supportedTypes));
	
	public static void init() {
		codeFormatter = new CustomCodeFormatter(GeneralConfig.JS_LIB_FOLDER);
		CURSOR_COLOR_PICK = GuiUtils.getCustomCursor(IconsPathConfigurator.ICON_COLOR_SAMPLER, DEF_CUSTOM_CURSOR_SIZE);
	}
	
	public static void formatTextLater(JTextArea sourceArea, String extension, int tabSize) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				sourceArea.setCursor(GuiUtilsExtended.CURSOR_WAIT);
				formatText(sourceArea, extension, tabSize);
				sourceArea.setCursor(GuiUtilsExtended.CURSOR_TEXT);
			}
		});
	}
	
	public static void formatText(JTextArea sourceArea, String extension, int tabSize) {
		Dimension originalDialogDim = dialogHelper.IF_SCROLLABLE_DEF_DIMENSION;
		dialogHelper.setIF_SCROLLABLE_DEF_DIMENSION(new Dimension(500,230));
		boolean error = false;
		int selStart = sourceArea.getSelectionStart(); 
		int selEnd = sourceArea.getSelectionEnd();
		String source = GuiUtilsExtended.getSelectedOrAllText(sourceArea);
		String old = new String(source);
		String formatted = "";
		boolean suppported = false;

		try {
			// XML format handler (if xml formatter works then is a xml recognized file)
			if (extension.toLowerCase().endsWith(XML)) {
				suppported = true;
				formatted = CustomCodeFormatter.xmlPrettyFormat(source, tabSize);
				source = CustomCodeFormatter.xmlPrettyFormat(source, tabSize);
				if (source.equals(old)) {
					// already formatted
					source = StringWorker.removeLines(source);
				}
			// JS/JSON -> use javascript function call with custom library to prettify JS string
			}else if (extension.toLowerCase().endsWith(JS) || extension.toLowerCase().endsWith(JSON)) {
				suppported = true;
				formatted = codeFormatter.jsFormatJSLIB(source, 5);
				source = StringWorker.replaceSpacesWithTabs(formatted, tabSize);
				
			// HTML -> use javascript function call with custom library to prettify html string
			}else if (extension.toLowerCase().endsWith(HTML)) {
				suppported = true;
				formatted = codeFormatter.htmlFormatJSLIB(source, 5);
				source = StringWorker.replaceSpacesWithTabs(formatted, tabSize);
			
			// CSS -> use javascript function call with custom library to prettify css string
			}else if (extension.toLowerCase().endsWith(CSS)) {
				suppported = true;
				formatted = codeFormatter.cssFormatJSLIB(source, 5);
				source = StringWorker.replaceSpacesWithTabs(formatted, tabSize);
				
			// JAVA -> use google library to prettify java string
			} else if (extension.toLowerCase().endsWith(JAVA)) {
			suppported = false;
//			formatted = CustomCodeFormatter.googleJavaCodeFormat(source);
//			source = formatted;
		}
		} catch (Exception e) {
			GuiUtils.manageFormatterException(e, extension);
			error = true;
		}

		// apply formatted to textArea
		if (!error && !source.equals(old)) {
			if (selStart == selEnd) {
				sourceArea.setText(source);
			}else {
				sourceArea.replaceSelection(source);
			}
		}else {
			String unsupportedAdvice = "";
			if(!suppported) {
				unsupportedAdvice = extension + " is not yet supported... maybe coming soon? ;)";
			}
			
			if (!suppported || error) {
				SoundsManagerExtended.playSound(SoundsConfigurator.ERROR, null);
				logger.warn("Format not supported or an error occurred :-> skipping text replace in text area. " + unsupportedAdvice);
				dialogHelper.warn(GuiUtils.encapsulateInHtml("<b>Cannot format with current settings</b></br>" + unsupportedAdvice), "Cannot Format text");
			}
		}
		
		dialogHelper.setIF_SCROLLABLE_DEF_DIMENSION(originalDialogDim);
	}
	
	public static boolean isFileTypeSupportedFormatter(File file) {
		if(file != null && file.exists() && file.isFile()) {
			for(String ext : supportedTypes) {
				if(file.getName().toLowerCase().endsWith("." + ext)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
}
