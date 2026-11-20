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
