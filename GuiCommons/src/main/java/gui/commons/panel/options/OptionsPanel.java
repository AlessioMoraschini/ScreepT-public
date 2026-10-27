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
package gui.commons.panel.options;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;
import various.common.light.gui.GuiUtils;

/**
 * Vertical list of option sections, meant to be shown in a JScrollPane, with text filtering.
 * Only presentation: options components keep their own models and listeners, filtering just hides them.
 */
public class OptionsPanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 6178563262913485711L;

	private static final int SCROLL_UNIT = 16;
	/** Room for the vertical scroll bar, in case the window gets smaller than its content */
	private static final int FIT_EXTRA_WIDTH = 24;
	private static final Insets DEFAULT_FRAME_INSETS = new Insets(39, 8, 8, 8);

	private final List<OptionSection> sections = new ArrayList<>();
	private final JLabel noResults = new JLabel("No options match the search", SwingConstants.CENTER);

	public OptionsPanel() {
		super(new MigLayout("wrap 1, fillx, insets 12 18 18 18, gapy 22, hidemode 3", "[grow,fill]"));
		noResults.setForeground(UIManager.getColor("Label.disabledForeground"));
		OptionSection.enlargeFont(noResults);
		noResults.setVisible(false);
		add(noResults, "growx, gaptop 30");
	}

	public OptionSection addSection(String title, String description) {
		OptionSection section = new OptionSection(title, description, 0);
		sections.add(section);
		add(section, "growx");
		return section;
	}

	/**
	 * Shows only the options matching the given text (case insensitive, on labels, texts, tooltips and section titles)
	 */
	public void filter(String text) {
		String query = normalize(text);
		boolean anyVisible = false;
		for (OptionSection section : sections) {
			anyVisible |= section.filter(query);
		}
		noResults.setVisible(!anyVisible);
		revalidate();
		repaint();
	}

	/**
	 * Size of an options window that fits all its content without scrolling, between the minimum size and
	 * the given percentage of the reference frame monitor (maximize stays available)
	 */
	public static <W extends Window & RootPaneContainer> Dimension fitWindowSize(W window, Dimension minimum, int maxScreenPercent, JFrame screenReference) {

		Dimension content = window.getContentPane().getPreferredSize();
		Insets frameInsets = window.isDisplayable() ? window.getInsets() : DEFAULT_FRAME_INSETS;
		int width = content.width + frameInsets.left + frameInsets.right + FIT_EXTRA_WIDTH;
		int height = content.height + frameInsets.top + frameInsets.bottom;

		Dimension maxSize = GuiUtils.getScreenSizePerc(maxScreenPercent, screenReference);
		return new Dimension(Math.max(minimum.width, Math.min(width, maxSize.width)),
				Math.max(minimum.height, Math.min(height, maxSize.height)));
	}

	static String normalize(String text) {
		return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
	}

	// Scrollable: follow the viewport width, so sections reflow when the window is resized

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return SCROLL_UNIT;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return Math.max(SCROLL_UNIT, visibleRect.height - SCROLL_UNIT);
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
