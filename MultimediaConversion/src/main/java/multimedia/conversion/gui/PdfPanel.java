/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package multimedia.conversion.gui;

import java.awt.Component;
import java.io.File;

import javax.swing.JComponent;

import plugin.external.arch.FramePanelAdapter;
import plugin.external.arch.PluginPanel;

/** The PDF reader as a tab of the file editor: the same {@link PdfSelector2TextFrame} (viewer, text extraction), moved into a panel */
public class PdfPanel implements PluginPanel {

	private final PdfSelector2TextFrame frame;
	private final JComponent component;
	private final File file;

	public PdfPanel(File file) {
		this.file = file;
		frame = new PdfSelector2TextFrame(null, null, "Pdf viewer");
		component = FramePanelAdapter.wrap(frame);
		if (file != null) {
			frame.loadFile(file);
		}
	}

	@Override
	public JComponent getComponent() {
		return component;
	}

	@Override
	public String getTitle() {
		return file != null ? file.getName() : "PDF";
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public boolean isModified() {
		return false;
	}

	@Override
	public boolean save() {
		return true;
	}

	@Override
	public boolean confirmClose(Component parent) {
		return true;
	}

	@Override
	public void dispose() {
		frame.releaseResources();
		frame.dispose();
	}
}
