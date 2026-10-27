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
package com.minhasKamal.ultimateCalculator.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

/**
 * Calculator backgrounds are light textures: with a dark look and feel the (light) theme text would be unreadable on them.
 * In that case the texture is recolored with the tones of the theme panel, keeping its structure.
 */
public final class CalculatorTheme {

	private static final float BOTTOM_TONE = 0.78f;

	private CalculatorTheme() {
	}

	public static boolean isDarkLaf() {
		Color panel = UIManager.getColor("Panel.background");
		return panel != null && luminance(panel) < 0.4f;
	}

	/** Background texture, darkened when a dark look and feel is active */
	public static ImageIcon background(URL resource) {
		ImageIcon icon = new ImageIcon(resource);
		return isDarkLaf() ? darken(icon) : icon;
	}

	private static ImageIcon darken(ImageIcon icon) {
		int width = icon.getIconWidth();
		int height = icon.getIconHeight();
		BufferedImage source = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = source.createGraphics();
		graphics.drawImage(icon.getImage(), 0, 0, null);
		graphics.dispose();

		Color top = UIManager.getColor("Panel.background");
		int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
		for (int i = 0; i < pixels.length; i++) {
			int argb = pixels[i];
			float light = luminance(new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF));
			// light textures sit in the upper range: stretch it to keep a visible, soft structure
			float tone = Math.max(0f, Math.min(1f, (light - 0.45f) / 0.55f));
			float scale = BOTTOM_TONE + (1f - BOTTOM_TONE) * tone;
			int red = Math.round(top.getRed() * scale);
			int green = Math.round(top.getGreen() * scale);
			int blue = Math.round(top.getBlue() * scale);
			pixels[i] = (argb & 0xFF000000) | (red << 16) | (green << 8) | blue;
		}
		source.setRGB(0, 0, width, height, pixels, 0, width);
		return new ImageIcon(source);
	}

	private static float luminance(Color color) {
		return (0.299f * color.getRed() + 0.587f * color.getGreen() + 0.114f * color.getBlue()) / 255f;
	}
}
