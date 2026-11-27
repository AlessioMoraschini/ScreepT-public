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
		frame.setPanelMode();
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
		return getFile() != null ? getFile().getName() : "PDF";
	}

	@Override
	public File getFile() {
		return frame.getCurrentFile() != null ? frame.getCurrentFile() : file;
	}

	@Override
	public boolean isModified() {
		return frame.isModified();
	}

	@Override
	public boolean save() {
		return frame.save();
	}

	@Override
	public boolean confirmClose(Component parent) {
		return frame.confirmDiscard(parent);
	}

	@Override
	public void setChangeListener(Runnable listener) {
		frame.setEditListener(listener);
	}

	@Override
	public boolean saveAs() {
		return frame.saveAs();
	}

	@Override
	public boolean supportsEditHistory() {
		return true;
	}

	@Override
	public void undo() {
		frame.undo();
	}

	@Override
	public void redo() {
		frame.redo();
	}

	@Override
	public void dispose() {
		frame.releaseResources();
		frame.dispose();
	}
}
