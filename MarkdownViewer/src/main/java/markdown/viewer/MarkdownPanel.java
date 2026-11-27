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
package markdown.viewer;

import java.awt.Component;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import plugin.external.arch.FramePanelAdapter;
import plugin.external.arch.PluginPanel;

/**
 * The Markdown viewer as a tab of the file editor: it is the same {@link MarkdownViewerFrame} (editor, preview, outline, tools),
 * built as usual and moved into a panel, so window and tab share all the code.
 */
public class MarkdownPanel implements PluginPanel {

	private final MarkdownViewerFrame frame;
	private final JComponent component;
	private Runnable changeListener;

	public MarkdownPanel(File markdown) {
		frame = new MarkdownViewerFrame((File) null, markdown);
		component = FramePanelAdapter.wrap(frame);
		frame.attachToHost(component, () -> {
			Runnable listener = changeListener;
			if (listener != null) {
				SwingUtilities.invokeLater(listener);
			}
		});
	}

	@Override
	public JComponent getComponent() {
		return component;
	}

	@Override
	public String getTitle() {
		return frame.getHostTitle();
	}

	@Override
	public File getFile() {
		return frame.getMarkdownFile();
	}

	@Override
	public boolean isModified() {
		return frame.isModified();
	}

	@Override
	public boolean save() {
		return frame.saveMarkdown(true);
	}

	@Override
	public boolean confirmClose(Component parent) {
		return frame.confirmCloseOver(parent);
	}

	@Override
	public void onSelected() {
		component.requestFocusInWindow();
	}

	@Override
	public void dispose() {
		frame.releaseResources();
		frame.dispose();
	}

	@Override
	public void setChangeListener(Runnable listener) {
		this.changeListener = listener;
	}
}
