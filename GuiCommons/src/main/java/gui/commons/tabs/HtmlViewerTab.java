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
package gui.commons.tabs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;

import gui.commons.frameutils.frame.arch.ToolStyle;
import net.miginfocom.swing.MigLayout;

/**
 * Simple HTML viewer for an editor tab, based on the HTML engine of Swing (HTML 3.2 and basic CSS: pages with modern
 * scripts or layouts look better in the browser, one click away). Links open in the default browser.
 */
public class HtmlViewerTab extends JPanel implements TabContent {
	private static final long serialVersionUID = 1L;

	/** The implementor that registers this viewer */
	public static class Implementor implements TabImplementor {
		public static final String ID = "html";

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public String getDisplayName() {
			return "HTML viewer";
		}

		@Override
		public Set<String> getDefaultExtensions() {
			return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("html", "htm")));
		}

		@Override
		public boolean canOpen(File file) {
			return file != null && file.isFile();
		}

		@Override
		public TabContent open(File file) {
			return new HtmlViewerTab(file);
		}
	}

	private final File file;
	private final JEditorPane pane = new JEditorPane();
	private final JLabel status = ToolStyle.mutedLabel(" ");

	public HtmlViewerTab(File file) {
		super(new BorderLayout(0, 6));
		this.file = file;
		pane.setEditable(false);
		pane.addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
				browse(e.getURL().toString());
			} else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED && e.getURL() != null) {
				status.setText(e.getURL().toString());
			} else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
				status.setText(" ");
			}
		});

		JPanel bar = new JPanel(new MigLayout("insets 6 8 0 8, gapx 6", "[][][grow,fill]", "[]"));
		bar.setOpaque(false);
		JButton reload = ToolStyle.button("Reload", null, "Read the file again");
		reload.addActionListener(e -> load());
		JButton browser = ToolStyle.button("Open in browser", null, "Show the page with the default browser");
		browser.addActionListener(e -> browse(file.toURI().toString()));
		bar.add(reload);
		bar.add(browser);
		bar.add(status);
		add(bar, BorderLayout.NORTH);
		add(new JScrollPane(pane), BorderLayout.CENTER);
		load();
	}

	private void load() {
		try {
			pane.setPage(file.toURI().toURL());
		} catch (IOException e) {
			pane.setContentType("text/plain");
			pane.setText("Cannot read " + file + ": " + e.getMessage());
		}
	}

	private void browse(String uri) {
		try {
			Desktop.getDesktop().browse(new java.net.URI(uri));
		} catch (Exception e) {
			status.setText("Cannot open the browser: " + e.getMessage());
		}
	}

	@Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public String getTitle() {
		return file.getName();
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
	public void onSelected() {
		pane.requestFocusInWindow();
	}

	@Override
	public void dispose() {
		pane.setText("");
	}

	@Override
	public void setChangeListener(Runnable listener) {
		// nothing changes in this viewer
	}
}
