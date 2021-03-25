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
package general;

import java.io.File;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileView;

import resources.IconsPathConfigurator;

public class MyFileView extends FileView{
	public static ImageIcon dirIcon = new ImageIcon(IconsPathConfigurator.iconFolderPath);

    public MyFileView()
    {
    }
    
    public MyFileView(Hashtable<String,ImageIcon> table,ImageIcon dirIcon)
    {
    }

    /**
     * Retrieve icon using file extension. If file is null txt icon will be returned
     */
    public Icon getIcon(File f){

    	if(f == null) {
    		return new ImageIcon(IconsPathConfigurator.F_ICON_TXT);
    	}
    	
        // If dir
        if(f.isDirectory()) {
            if(dirIcon!=null) 
            	return dirIcon;
        }

        // Get the name
        String name=f.getName();
        int idx=name.lastIndexOf(".");

        if(idx>-1){
        	String ext=name.substring(idx);
            if(IconsPathConfigurator.FILE_TYPE_ICONS_MAP.containsKey(ext.toLowerCase()))
            	return IconsPathConfigurator.FILE_TYPE_ICONS_MAP.get(ext.toLowerCase());
        }
        
        // For other files
	    return new ImageIcon(IconsPathConfigurator.F_ICON_GENERIC);
    }
        
    public static ImageIcon getExtIcon(File f){

    	// null check
    	if(f == null || !f.exists()) {
    		return new ImageIcon();
    	}
    	
    	// If dir
		if (f.isDirectory()) {
			if (dirIcon != null)
				return dirIcon;
		}

		// Get the name
		String name = f.getName();
		int idx = name.lastIndexOf(".");

		if (idx > -1) {
			String ext = name.substring(idx);
			if (IconsPathConfigurator.FILE_TYPE_ICONS_MAP.containsKey(ext.toLowerCase())) {
				return IconsPathConfigurator.FILE_TYPE_ICONS_MAP.get(ext.toLowerCase());
			}
		}

	    // For other files
	    return new ImageIcon(IconsPathConfigurator.F_ICON_GENERIC);
    }
    
    
}