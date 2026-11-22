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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Small drawn icons (16x16) that tell which viewer a tab uses: a colored badge with a white glyph, readable on
 * dark and light themes. Drawn in code, so there are no image files to ship.
 */
public final class ViewerIcons {

	private static final int SIZE = 16;
	private static final Map<String, Icon> CACHE = new HashMap<String, Icon>();

	private ViewerIcons() {
	}

	/** Icon of the viewer with the given id (see {@link TabImplementor#getId()}), null if the id is unknown or empty */
	public static synchronized Icon forViewer(String viewerId) {
		if (viewerId == null || viewerId.isEmpty()) {
			return null;
		}
		String key = viewerId.toLowerCase(Locale.ROOT);
		Icon icon = CACHE.get(key);
		if (icon == null) {
			icon = new ImageIcon(draw(key));
			CACHE.put(key, icon);
		}
		return icon;
	}

	private static BufferedImage draw(String id) {
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			Color badge;
			if (id.equals("image")) {
				badge = new Color(52, 132, 200);
			} else if (id.equals("logsearch")) {
				badge = new Color(214, 140, 40);
			} else if (id.equals("html")) {
				badge = new Color(222, 96, 56);
			} else if (id.contains("markdown")) {
				badge = new Color(84, 104, 124);
			} else if (id.contains("pdf")) {
				badge = new Color(196, 52, 44);
			} else {
				badge = new Color(122, 92, 196);
			}
			g.setColor(badge);
			g.fill(new RoundRectangle2D.Double(0.5, 0.5, SIZE - 1, SIZE - 1, 5, 5));

			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			if (id.equals("image")) {
				// sun and mountains
				g.fill(new Ellipse2D.Double(10, 3.5, 2.6, 2.6));
				Path2D mountains = new Path2D.Double();
				mountains.moveTo(3, 12.5);
				mountains.lineTo(6.5, 7.5);
				mountains.lineTo(9, 10.5);
				mountains.lineTo(10.5, 9);
				mountains.lineTo(13, 12.5);
				mountains.closePath();
				g.fill(mountains);
			} else if (id.equals("logsearch")) {
				// lines of a log and a magnifier
				g.drawLine(3, 4, 8, 4);
				g.drawLine(3, 7, 6, 7);
				g.drawLine(3, 10, 6, 10);
				g.draw(new Ellipse2D.Double(7.2, 6.2, 4.4, 4.4));
				g.drawLine(11, 10, 13, 12);
			} else if (id.equals("html")) {
				// </>
				g.drawPolyline(new int[] { 6, 3, 6 }, new int[] { 5, 8, 11 }, 3);
				g.drawPolyline(new int[] { 10, 13, 10 }, new int[] { 5, 8, 11 }, 3);
				g.drawLine(9, 4, 7, 12);
			} else if (id.contains("markdown")) {
				// M with a down arrow
				g.drawPolyline(new int[] { 2, 2, 5, 7, 7 }, new int[] { 11, 5, 8, 5, 11 }, 5);
				g.drawLine(11, 5, 11, 11);
				g.drawPolyline(new int[] { 9, 11, 13 }, new int[] { 9, 11, 9 }, 3);
			} else {
				// PDF and any other plugin: the first letters of the viewer
				String text = id.contains("pdf") ? "PDF" : letter(id);
				g.setFont(new Font("SansSerif", Font.BOLD, text.length() > 1 ? 7 : 11));
				int width = g.getFontMetrics().stringWidth(text);
				g.drawString(text, (SIZE - width) / 2f, text.length() > 1 ? 10.5f : 12f);
			}
		} finally {
			g.dispose();
		}
		return image;
	}

	/** First letter of a plugin viewer id ("plugin:myviewer" gives "M") */
	private static String letter(String id) {
		String name = id.startsWith(TabImplementors.PLUGIN_ID_PREFIX) ? id.substring(TabImplementors.PLUGIN_ID_PREFIX.length()) : id;
		return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
	}
}
