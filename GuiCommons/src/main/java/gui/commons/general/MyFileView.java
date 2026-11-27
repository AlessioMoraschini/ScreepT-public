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
package gui.commons.general;

import java.io.File;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileView;

import resources.IconsPathConfigurator;

public class MyFileView extends FileView{
	public static ImageIcon dirIcon = new ImageIcon(IconsPathConfigurator.iconFolderPath);

	/** Loaded once: a new ImageIcon reads the image from disk, and a tree asks the icon of every row on every paint */
	private static ImageIcon genericIcon;
	private static ImageIcon txtIcon;

	private static synchronized ImageIcon generic() {
		if (genericIcon == null) {
			genericIcon = new ImageIcon(IconsPathConfigurator.F_ICON_GENERIC);
		}
		return genericIcon;
	}

	private static synchronized ImageIcon txt() {
		if (txtIcon == null) {
			txtIcon = new ImageIcon(IconsPathConfigurator.F_ICON_TXT);
		}
		return txtIcon;
	}

	/** Icon of a file or folder whose kind is already known: no access to the disk */
	public Icon getIcon(File f, boolean directory) {
		if (f == null) {
			return txt();
		}
		if (directory && dirIcon != null) {
			return dirIcon;
		}
		String name = f.getName();
		int idx = name.lastIndexOf(".");
		if (idx > -1) {
			ImageIcon byExt = IconsPathConfigurator.FILE_TYPE_ICONS_MAP.get(name.substring(idx).toLowerCase());
			if (byExt != null) {
				return byExt;
			}
		}
		return generic();
	}

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
    		return txt();
    	}
    	return getIcon(f, f.isDirectory());
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