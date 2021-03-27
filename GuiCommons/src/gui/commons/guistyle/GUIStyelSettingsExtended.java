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
package gui.commons.guistyle;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

import org.apache.log4j.Logger;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.theme.LightGray;

import gui.commons.treeutils.TreeUtils;
import gui.commons.utils.ImageWorkerExtended;
import initializer.configs.impl.om.GuiOption;
import resources.GeneralConfig;
import various.common.light.gui.GUIStyelSettings;

public class GUIStyelSettingsExtended extends GUIStyelSettings {

	static Logger logger = Logger.getLogger(GUIStyelSettingsExtended.class);
	
	public static final String jgoodiesPlastic = "com.jgoodies.looks.plastic.PlasticLookAndFeel";
	public static final String jgoodiesPlasticXP = "com.jgoodies.looks.plastic.PlasticXPLookAndFeel";
	public static final String jgoodiesPlastic3d = "com.jgoodies.looks.plastic.Plastic3DLookAndFeel";
	public static final String jgoodiesWindows = "com.jgoodies.looks.windows.WindowsLookAndFeel";
	
	public static PlasticTheme currentTheme = new LightGray();

	static {
		defaultButtonFont = GeneralConfig.DIALOG_BTN_FONT;
	}
	
	public static synchronized boolean setStyle(String style, int nimbusSize) {
    	try {
    		
    		try {
				PlasticLookAndFeel.setCurrentTheme(currentTheme);
			} catch (Exception e) {
				logger.warn("Canot update current selected theme", e);
			}

    		UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
    		UIManager.put("CheckBox.disabledText", Color.LIGHT_GRAY);
    		UIManager.put("RadioButton.disabledText", Color.LIGHT_GRAY);
    		UIManager.put("nimbusOrange", nimbusProgBarColor);
    		UIManager.put("OptionPane.buttonFont", new FontUIResource(defaultButtonFont));
			UIManager.put("MenuBar.foreground", Color.black );
			UIManager.put("ScrollBar.background", new Color(170,170,170));
			UIManager.put("ToolTip.font", new Font("Segoe UI", Font.PLAIN, 18));
			UIManager.put("nimbusLightBackground", new Color( 240, 240, 240) );
			UIManager.put("Tree.selectionBackground", TreeUtils.DEF_BACK_SELECTION);
			UIManager.put("nimbusSelectionForeground", new Color(10, 10, 10));
			UIManager.setLookAndFeel(style);
			Color buttonBackG = (Color)UIManager.get("Button.background");
			if (ImageWorkerExtended.getColorComponentsTotRGBA(buttonBackG) < 400) {
				UIManager.put("Button.foreground", Color.WHITE);
			}else {
				UIManager.put("Button.foreground", Color.BLACK);
			}
			
			GUIStyelSettingsExtended.applySizeToAllWindowsNimbus(nimbusSize, true);
			
    		return true;
		} catch (Throwable e3) {
			logger.error("An error occurred!", e3);
			return false;
		}finally {
		}
    }
    
	public static boolean isDarkNimbus(GuiOption guiOpt) {
		String style = guiOpt.getPreferredStyle();
		
		if(guiOpt.isDarkGuiNimbus() && isNimbus(guiOpt.getPreferredStyle())) {
			return style.toLowerCase().contains("nimbus");
		}
		
		return false;
	}
    
    public static synchronized boolean setDarkNimbus(int nimbusSize) {
    	
    	UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
		UIManager.put("CheckBox.disabledText", Color.LIGHT_GRAY);
		UIManager.put("RadioButton.disabledText", Color.LIGHT_GRAY);
		UIManager.put("nimbusOrange", nimbusProgBarColor);
		UIManager.put("nimbusBase", new Color(18, 30, 49));
		UIManager.put("OptionPane.buttonFont", new FontUIResource(defaultButtonFont));
		UIManager.put("ScrollBar.background", new Color(70, 70, 70));
		UIManager.put("MenuBar.foreground", Color.white);
		UIManager.put("nimbusLightBackground", new Color(250, 250, 250));
		UIManager.put("Button.focus", new ColorUIResource(new Color(focusOpacity, 20,20,20)));
		UIManager.put("Tree.selectionBackground", TreeUtils.DEF_BACK_SELECTION);
		UIManager.put("nimbusSelectionForeground", new Color(10, 10, 10));
		
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					return true;
				}
			}
			GUIStyelSettingsExtended.applySizeToAllWindowsNimbus(nimbusSize, false);
		} catch (Throwable e) {
			logger.error("Exception happened!", e);
		}

		updateAllWindowsLaf();

		return false;
    }
    
	public static synchronized boolean setLightNimbus(int nimbusSize) {
		
		UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
		UIManager.put("CheckBox.disabledText", Color.LIGHT_GRAY);
		UIManager.put("RadioButton.disabledText", Color.LIGHT_GRAY);
		UIManager.put("nimbusOrange", nimbusProgBarColor);
		UIManager.put("OptionPane.buttonFont", new FontUIResource(GeneralConfig.DIALOG_BTN_FONT));
		UIManager.put("ScrollBar.background", new Color(170,170,170));
		UIManager.put("nimbusBase", new Color(51, 98, 140));
		UIManager.put("MenuBar.foreground", Color.black);
		UIManager.put("nimbusLightBackground", new Color(240, 240, 240));
		UIManager.put("MenuItem.textIconGap", 18);
		UIManager.put("Tree.selectionBackground", TreeUtils.DEF_BACK_SELECTION);
		UIManager.put("nimbusSelectionForeground", new Color(10, 10, 10));
		UIManager.put("Button.focus", new ColorUIResource(new Color(focusOpacity, 20, 20, 20)));
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					return true;
				}
			}
			GUIStyelSettingsExtended.applySizeToAllWindowsNimbus(nimbusSize, true);
		} catch (Throwable e) {
			logger.error("Exception happened!", e);
		} 

		updateAllWindowsLaf();
		
		return false;
	}
	
	public static LookAndFeelInfoCustom[] getLookAndFeelList() {
		LookAndFeelInfoCustom[] jtatooInfo = getJTatooLAF();
		LookAndFeelInfoCustom[] jgoodiesInfo = getJGoodiesLAF();
		
		List<LookAndFeelInfo> lookNFeelArrayList = new ArrayList<>();
		for(LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
			String lowerCaseName = laf.getName().toLowerCase();
			if(!lowerCaseName.contains("windows classic") && !lowerCaseName.contains("cde/motif")) {
				lookNFeelArrayList.add(laf);
			}
		}
		
		LookAndFeelInfoCustom[] outList = new LookAndFeelInfoCustom
				[lookNFeelArrayList.size() + jtatooInfo.length + jgoodiesInfo.length]; // modify equals to custom's total
		
		int i = 0;
		
		for(LookAndFeelInfo current : lookNFeelArrayList) {
			outList[i] = new LookAndFeelInfoCustom(current.getName(), current.getClassName());
			i++;
		}
		
		for(LookAndFeelInfoCustom currentJtatoo : jtatooInfo) {
			outList[i] = currentJtatoo;
			i++;
		}

		for(LookAndFeelInfoCustom currentJgoodies : jgoodiesInfo) {
			outList[i] = currentJgoodies;
			i++;
		}

		return outList;
	}
	
	public static LookAndFeelInfoCustom[] getJTatooLAF() {
		LookAndFeelInfoCustom[] lookNFeelJtatoos = 
			{
				/**new LookAndFeelInfo("jtatooNoireLNF", jtatooNoireLNF),
				new LookAndFeelInfo("jtatooLookAndFeel", jtatooLookAndFeel),
				new LookAndFeelInfo("jtatooGraphiteLNF", jtatooGraphiteLNF),
				new LookAndFeelInfo("jtatooLunaLNF", jtatooLunaLNF),
				new LookAndFeelInfo("jtatooMintLNF", jtatooMintLNF)**/
			};
		
		return lookNFeelJtatoos;
	}

	public static LookAndFeelInfoCustom[] getJGoodiesLAF() {
		LookAndFeelInfoCustom[] jGoodiesLafs = 
			{
				new LookAndFeelInfoCustom("jgoodiesWindows", jgoodiesWindows),
				new LookAndFeelInfoCustom("jgoodiesPlasticXP", jgoodiesPlasticXP),
				new LookAndFeelInfoCustom("jgoodiesPlastic3d", jgoodiesPlastic3d),
				new LookAndFeelInfoCustom("jgoodiesPlastic", jgoodiesPlastic)
			};
		
		return jGoodiesLafs;
	}
	
	/**
	 * PUBLIC METHOD THAT USES PRIVATE ONE
	 * 
	 * Apply general nimbus LAF size for all components in the parentFrame Container, according to the int value given as param:
	 * 0=mini; 1=small; 2= large.  Other values use small as default.
	 */
	public static void applySizeToAllWindowsNimbus(int size, boolean updateUI) {
		
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				applyNimbusSize(size);
//				if(updateUI) {
//					updateAllWindowsLaf();
//				}
//			}
//		});
		applyNimbusSize(null, size);
		if(updateUI) {
			updateAllWindowsLaf();
		}
	}
	
	
	/**
	 * Get all components that are sons of the given container
	 * @param c
	 * @return
	 */
	public static List<Component> getAllComponents(final Container c) {
		Component[] comps = c.getComponents();
		List<Component> compList = new ArrayList<Component>();
		for (Component comp : comps) {
			compList.add(comp);
			if (comp instanceof Container)
				compList.addAll(getAllComponents((Container) comp));
		}
		return compList;
	}
	
	public static ArrayList<PlasticTheme> getPlasticThemes() {
		ArrayList<PlasticTheme> themes = new ArrayList<>();
		
		for(Object theme : Plastic3DLookAndFeel.getInstalledThemes()) {
			if(theme instanceof PlasticTheme) {
				themes.add((PlasticTheme) theme);
			}
		}
		
		return themes;
	}
	
	public static PlasticTheme getPlasticThemeByName(String name, ArrayList<PlasticTheme> themes) {
		if(themes == null) {
			themes = getPlasticThemes();
		}
		
		for(PlasticTheme curr : themes) {
			if(curr.getName().equalsIgnoreCase(name)) {
				return curr;
			}
		}
		
		return null;
	}
	
	public static void updateThemeByName(String name) {
		ArrayList<PlasticTheme> themes = getPlasticThemes();
		PlasticTheme theme = getPlasticThemeByName(name, themes);
		if(theme != null) {
			currentTheme = theme;
		}
	}
	
}
