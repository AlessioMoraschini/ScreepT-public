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
package gui.commons.imagestudio;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JComponent;

import gui.commons.frameutils.frame.arch.ToolStyle;

/** Luma / red / green / blue histogram (square root scale so small peaks stay visible) */
public class HistogramView extends JComponent {
	private static final long serialVersionUID = 1L;

	private final int[][] bins = new int[4][256];
	private int max = 1;

	public HistogramView() {
		setPreferredSize(new Dimension(200, 90));
	}

	public void setImage(BufferedImage img) {
		for (int[] bin : bins) {
			Arrays.fill(bin, 0);
		}
		if (img != null) {
			int w = img.getWidth();
			int h = img.getHeight();
			int step = Math.max(1, (int) Math.sqrt((w * (double) h) / 1_000_000d));
			for (int y = 0; y < h; y += step) {
				for (int x = 0; x < w; x += step) {
					int p = img.getRGB(x, y);
					int r = (p >> 16) & 0xff;
					int g = (p >> 8) & 0xff;
					int b = p & 0xff;
					bins[0][(int) (0.299 * r + 0.587 * g + 0.114 * b)]++;
					bins[1][r]++;
					bins[2][g]++;
					bins[3][b]++;
				}
			}
		}
		max = 1;
		for (int[] bin : bins) {
			for (int v : bin) {
				max = Math.max(max, v);
			}
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g0) {
		Graphics2D g = (Graphics2D) g0.create();
		g.setColor(ToolStyle.isDark() ? new Color(30, 31, 36) : new Color(246, 246, 246));
		g.fillRect(0, 0, getWidth(), getHeight());
		Color[] colors = {new Color(150, 150, 150, 170), new Color(220, 70, 70, 140), new Color(70, 190, 90, 140), new Color(70, 120, 230, 140)};
		double sx = getWidth() / 256.0;
		for (int c = 0; c < 4; c++) {
			g.setColor(colors[c]);
			for (int i = 0; i < 256; i++) {
				int hgt = (int) Math.round(Math.sqrt(bins[c][i] / (double) max) * (getHeight() - 4));
				g.fillRect((int) Math.round(i * sx), getHeight() - hgt, Math.max(1, (int) Math.ceil(sx)), hgt);
			}
		}
		g.dispose();
	}
}
