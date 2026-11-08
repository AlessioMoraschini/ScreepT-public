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
package gui.commons.imagestudio;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

import gui.commons.tabs.TabContent;
import gui.commons.tabs.TabImplementor;

/** Opens images as an Image Studio inside an editor tab */
public class ImageStudioTabImplementor implements TabImplementor {

	public static final String ID = "image";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Image Studio";
	}

	@Override
	public Set<String> getDefaultExtensions() {
		Set<String> extensions = new LinkedHashSet<String>();
		for (String suffix : ImageIO.getReaderFileSuffixes()) {
			if (!suffix.isEmpty()) {
				extensions.add(suffix.toLowerCase(Locale.ROOT));
			}
		}
		return Collections.unmodifiableSet(extensions);
	}

	@Override
	public boolean canOpen(File file) {
		return file != null && file.isFile();
	}

	@Override
	public TabContent open(File file) {
		return new ImageStudioPanel(file);
	}
}
