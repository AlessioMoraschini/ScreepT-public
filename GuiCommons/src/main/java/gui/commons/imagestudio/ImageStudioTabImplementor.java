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
package gui.commons.imagestudio;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

import gui.commons.tabs.TabContent;
import gui.commons.tabs.TabImplementor;

/** Opens images in an editor tab as a read-only viewer (zoom, details, histogram, copy); editing is in the Image Studio window */
public class ImageStudioTabImplementor implements TabImplementor {

	public static final String ID = "image";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Image Viewer";
	}

	@Override
	public Set<String> getDefaultExtensions() {
		Set<String> extensions = new LinkedHashSet<String>();
		for (String suffix : ImageIO.getReaderFileSuffixes()) {
			if (!suffix.isEmpty()) {
				extensions.add(suffix.toLowerCase(Locale.ROOT));
			}
		}
		for (String extra : new String[] {"jpeg", "jfif", "jpe", "dib", "tif", "tiff", "psd", "psb"}) {
			extensions.add(extra);
		}
		return Collections.unmodifiableSet(extensions);
	}

	@Override
	public boolean canOpen(File file) {
		return file != null && file.isFile() && getDefaultExtensions().contains(gui.commons.tabs.TabImplementors.extensionOf(file));
	}

	@Override
	public TabContent open(File file) {
		return new ImageStudioPanel(file, true);
	}
}
