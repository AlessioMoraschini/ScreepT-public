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
package gui.commons.frameutils.frame.panels;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;

import javax.swing.JTextArea;
import javax.swing.Scrollable;

import gui.commons.frameutils.frame.panels.arch.IParentPanel;
import gui.commons.frameutils.frame.panels.arch.ParentPanel;
import gui.commons.panel.clipboard.ClipboardView;
import initializer.configs.impl.om.TextEditorOption;
import net.miginfocom.swing.MigLayout;
import resources.GeneralConfig;

/**
 * Clipboard of the Clipboard frame: the shared {@link ClipboardView} with a plain text area
 */
public class ClipboardPanelLight extends ParentPanel implements IParentPanel, Scrollable {
	private static final long serialVersionUID = 1L;

	public static ClipboardPanelLight singletonInstance;

	public JTextArea txtAreaClipboard;
	private ClipboardView view;
	private TextEditorOption options;

	public ClipboardPanelLight() {
		this(new TextEditorOption());
	}

	public ClipboardPanelLight(TextEditorOption options) {
		this.options = options != null ? options : new TextEditorOption();
		super.init();
		singletonInstance = this;
	}

	@Override
	protected void initGui() {
		txtAreaClipboard = new JTextArea();
		txtAreaClipboard.setLineWrap(true);
		txtAreaClipboard.setWrapStyleWord(true);
		view = new ClipboardView(txtAreaClipboard, new Runnable() {
			@Override
			public void run() {
				styleArea();
			}
		}, options, null);
		styleArea();
		setLayout(new MigLayout("fill, insets 0", "[grow,fill]", "[grow,fill]"));
		add(view, "grow");
	}

	private void styleArea() {
		if (GeneralConfig.TXT_EDITOR_CLIPBOARD_FONT != null) {
			txtAreaClipboard.setFont(GeneralConfig.TXT_EDITOR_CLIPBOARD_FONT);
		}
	}

	@Override
	protected void addHandlers() {
	}

	@Override
	protected void addKeyListeners() {
	}

	/** Writes the unsaved text (window closing) */
	public void flush() {
		view.flush();
	}

	@Override
	public void sync() {
		view.restyle();
		view.refreshFromDisk();
	}

	@Override
	public void reload(String target) throws Exception {
		view.select(target);
	}

	@Override
	public void doAction(String target) throws IOException {
		view.save();
	}

	public static ClipboardPanelLight getSingletonLazy() {
		singletonInstance = (singletonInstance == null) ? new ClipboardPanelLight() : singletonInstance;
		return singletonInstance;
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 16;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 64;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return true;
	}
}
