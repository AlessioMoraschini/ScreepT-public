/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
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
