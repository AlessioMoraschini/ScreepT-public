package dialogutils;

import java.io.File;

/**
 * Inherited FileFilter class to facilitate reuse when
 * multiple file filter selections are required. For example
 * purposes, I used a static nested class, which is defined
 * as below as a member of our original FileChooserExample
 * class.
 */
public class ExtensionFileFilter extends javax.swing.filechooser.FileFilter {

    private java.util.List<String> extensions;
    private String description;
    
    public static String DEFAULT_EXTENSION = "txt";

    public ExtensionFileFilter(String[] exts, String desc) {
    	extensions = new java.util.ArrayList<String>();
        if (exts != null) {

            for (String ext : exts) {

                // Clean array of extensions to remove "."
                // and transform to lowercase.
                extensions.add(
                    ext.replace(".", "").trim().toLowerCase()
                );
            }
        } else {
        	extensions.add("");
        }

        // Using inline if syntax, use input from desc or use
        // a default value.
        // Wrap with an if statement to default as well as
        // avoid NullPointerException when using trim().
        description = (desc != null) ? desc.trim() : "All Files";
    }

    // Handles which files are allowed by filter.
    @Override
    public boolean accept(File f) {
    
        // Allow directories to be seen.
        if (f.isDirectory()) return true;

        // exit if no extensions exist.
        if (extensions == null) return false;
		
        // Allows files with extensions specified to be seen.
        for (String ext : extensions) {
            if (f.getName().toLowerCase().endsWith("." + ext))
                return true;
        }

        // Otherwise file is not shown.
        return false;
    }

    // 'Files of Type' description
    @Override
    public String getDescription() {
        return description;
    }
    
    public String getExtension(int i) {
    	if(extensions != null && !extensions.isEmpty()) {
    		try {
				return extensions.get(i);
			} catch (Exception e) {
				return extensions.get(0);
			} 
    	} else {
    		return DEFAULT_EXTENSION;
    	}
    }
}
