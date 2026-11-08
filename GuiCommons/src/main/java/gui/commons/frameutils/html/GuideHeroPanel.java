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
package gui.commons.frameutils.html;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Animated header of the user guide: teal pixel rain (the same language as the launcher and the home page),
 * the ScreepT title with a light that sweeps over it, a blinking cursor and a typed tagline.
 * The animation runs only while the panel is on screen.
 */
public class GuideHeroPanel extends JPanel {
	private static final long serialVersionUID = 4817263540912837465L;

	private static final int CELL = 8;
	private static final int HEIGHT = 108;

	private final Timer timer = new Timer(55, e -> tick());
	private final Random random = new Random();
	private final long startedAt = System.currentTimeMillis();

	private double[] drop = new double[0];
	private double[] speed = new double[0];
	private int[] length = new int[0];
	private int rows;

	private final String title;
	private final String tagline;

	public GuideHeroPanel(String title, String tagline) {
		this.title = title;
		this.tagline = tagline;
		setOpaque(true);
		setPreferredSize(new Dimension(10, HEIGHT));
		setMinimumSize(new Dimension(10, HEIGHT));
	}

	@Override
	public void addNotify() {
		super.addNotify();
		timer.start();
	}

	@Override
	public void removeNotify() {
		timer.stop();
		super.removeNotify();
	}

	private void tick() {
		if (!isShowing()) {
			return;
		}
		for (int c = 0; c < drop.length; c++) {
			drop[c] += speed[c];
			if (drop[c] - length[c] > rows) {
				respawn(c, false);
			}
		}
		repaint();
	}

	private void respawn(int c, boolean initial) {
		drop[c] = initial ? -random.nextDouble() * rows : -random.nextDouble() * 10;
		speed[c] = 0.25 + random.nextDouble() * 0.55;
		length[c] = 5 + random.nextInt(9);
	}

	private void ensureGrid() {
		int cols = Math.max(1, getWidth() / CELL + 1);
		rows = getHeight() / CELL + 1;
		if (cols != drop.length) {
			drop = new double[cols];
			speed = new double[cols];
			length = new int[cols];
			for (int c = 0; c < cols; c++) {
				respawn(c, true);
			}
		}
	}

	private static Font titleFont() {
		Font f = new Font("Consolas", Font.BOLD, 46);
		return f.getFamily().equalsIgnoreCase("Consolas") ? f : new Font(Font.MONOSPACED, Font.BOLD, 44);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			boolean dark = HtmlViewerPanel.darkTheme();
			int w = getWidth();
			int h = getHeight();
			ensureGrid();

			// background
			g.setPaint(dark ? new GradientPaint(0, 0, new Color(0x1b2d38), 0, h, new Color(0x151c23))
					: new GradientPaint(0, 0, new Color(0xd7f1ee), 0, h, new Color(0xeef7f6)));
			g.fillRect(0, 0, w, h);

			// pixel rain
			int[] base = dark ? new int[] { 46, 196, 182 } : new int[] { 31, 127, 118 };
			double maxAlpha = dark ? 0.34 : 0.26;
			for (int c = 0; c < drop.length; c++) {
				for (int i = 0; i < length[c]; i++) {
					int row = (int) Math.floor(drop[c]) - i;
					if (row < 0 || row >= rows) {
						continue;
					}
					double a = (1 - (double) i / length[c]) * maxAlpha;
					g.setColor(i == 0 ? new Color(200, 255, 248, (int) (a * 255)) : new Color(base[0], base[1], base[2], (int) (a * 255)));
					g.fillRect(c * CELL, row * CELL, CELL - 2, CELL - 2);
				}
			}

			// title with a sweeping light
			long t = System.currentTimeMillis() - startedAt;
			g.setFont(titleFont());
			FontMetrics fm = g.getFontMetrics();
			int tw = fm.stringWidth(title);
			int cursorW = fm.stringWidth("_");
			int x = (w - tw - cursorW) / 2;
			int y = 56;
			float phase = (t % 4500) / 4500f * tw * 2;
			Color a = dark ? new Color(0x2ec4b6) : new Color(0x1f7f76);
			Color b = dark ? new Color(0xcffff8) : new Color(0x2b6fd6);
			g.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), dark ? 40 : 0));
			g.drawString(title, x + 2, y + 2);
			g.setPaint(new GradientPaint(x - tw + phase, 0, a, x - tw / 2f + phase, 0, b, true));
			g.drawString(title, x, y);
			if ((t / 500) % 2 == 0) {
				g.setPaint(a);
				g.drawString("_", x + tw, y);
			}

			// tagline, typed in then dots
			g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
			fm = g.getFontMetrics();
			String full = "> " + tagline.toUpperCase();
			int typed = (int) Math.min(full.length(), t / 55);
			String text = full.substring(0, typed);
			if (typed == full.length()) {
				text += "...".substring(0, (int) ((t / 450) % 4));
			}
			g.setColor(dark ? new Color(0x7fcfc6) : new Color(0x1f7f76));
			g.drawString(text, (w - fm.stringWidth(full)) / 2, y + 30);

			// bottom border
			g.setColor(dark ? new Color(0x25333f) : new Color(0xcfe3e0));
			g.drawLine(0, h - 1, w, h - 1);
		} finally {
			g.dispose();
		}
	}
}
