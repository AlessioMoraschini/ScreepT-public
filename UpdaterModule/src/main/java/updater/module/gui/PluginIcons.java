package updater.module.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/**
 * Vector icons of the Plugin Manager, drawn with the foreground color of the component (so they follow the look and
 * feel) plus an accent color. No image files needed.
 */
final class PluginIcons implements Icon {

	enum Kind { DOWNLOAD, TRASH, UPDATE, CHECK, SEARCH, REFRESH, PUZZLE, WARNING, INFO }

	private final Kind kind;
	private final int size;
	private final Color fixedColor;

	private PluginIcons(Kind kind, int size, Color fixedColor) {
		this.kind = kind;
		this.size = size;
		this.fixedColor = fixedColor;
	}

	/** Icon drawn with the foreground color of the component it is painted on */
	static Icon of(Kind kind, int size) {
		return new PluginIcons(kind, size, null);
	}

	/** Icon drawn with the given color */
	static Icon of(Kind kind, int size, Color color) {
		return new PluginIcons(kind, size, color);
	}

	/** Rounded square with the initial of the plugin, in a color derived from its name */
	static Icon avatar(final String name, final int size) {
		return new Icon() {
			@Override
			public int getIconWidth() {
				return size;
			}

			@Override
			public int getIconHeight() {
				return size;
			}

			@Override
			public void paintIcon(Component c, Graphics g0, int x, int y) {
				Graphics2D g = (Graphics2D) g0.create();
				try {
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					float hue = (Math.abs((name == null ? "" : name).hashCode()) % 360) / 360f;
					Color top = Color.getHSBColor(hue, 0.55f, 0.85f);
					Color bottom = Color.getHSBColor(hue, 0.70f, 0.60f);
					g.translate(x, y);
					g.setPaint(new GradientPaint(0, 0, top, 0, size, bottom));
					g.fill(new RoundRectangle2D.Float(0, 0, size, size, size * 0.28f, size * 0.28f));
					g.setColor(Color.WHITE);
					g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size * 0.52f)));
					FontMetrics fm = g.getFontMetrics();
					String letter = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
					g.drawString(letter, (size - fm.stringWidth(letter)) / 2f, (size - fm.getHeight()) / 2f + fm.getAscent());
				} finally {
					g.dispose();
				}
			}
		};
	}

	@Override
	public int getIconWidth() {
		return size;
	}

	@Override
	public int getIconHeight() {
		return size;
	}

	@Override
	public void paintIcon(Component c, Graphics g0, int x, int y) {
		Graphics2D g = (Graphics2D) g0.create();
		try {
			g.translate(x, y);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			double scale = size / 18.0;
			g.scale(scale, scale);
			Color color = fixedColor != null ? fixedColor : (c != null && c.getForeground() != null ? c.getForeground() : Color.GRAY);
			if (c != null && !c.isEnabled()) {
				color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
			}
			g.setColor(color);
			g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			draw(g);
		} finally {
			g.dispose();
		}
	}

	private void draw(Graphics2D g) {
		switch (kind) {
		case DOWNLOAD:
			g.drawLine(9, 2, 9, 11);
			g.drawLine(5, 7, 9, 11);
			g.drawLine(13, 7, 9, 11);
			g.drawLine(3, 15, 15, 15);
			g.drawLine(3, 12, 3, 15);
			g.drawLine(15, 12, 15, 15);
			break;
		case TRASH:
			g.drawLine(3, 5, 15, 5);
			g.drawLine(7, 5, 7, 3);
			g.drawLine(7, 3, 11, 3);
			g.drawLine(11, 3, 11, 5);
			Path2D bin = new Path2D.Float();
			bin.moveTo(4.5, 5); bin.lineTo(5.5, 15.5); bin.lineTo(12.5, 15.5); bin.lineTo(13.5, 5);
			g.draw(bin);
			g.drawLine(8, 8, 8, 13);
			g.drawLine(10, 8, 10, 13);
			break;
		case UPDATE:
			g.draw(new Arc2D.Float(3, 3, 12, 12, 30, 250, Arc2D.OPEN));
			Path2D head = new Path2D.Float();
			head.moveTo(9.5, 1.5); head.lineTo(15, 3.2); head.lineTo(11.5, 7.6); head.closePath();
			g.fill(head);
			break;
		case CHECK:
			Path2D check = new Path2D.Float();
			check.moveTo(3.5, 9.5); check.lineTo(7.5, 13.5); check.lineTo(14.5, 4.5);
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(check);
			break;
		case SEARCH:
			g.drawOval(2, 2, 10, 10);
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(11, 11, 16, 16);
			break;
		case REFRESH:
			g.draw(new Arc2D.Float(3, 3, 12, 12, 40, 280, Arc2D.OPEN));
			Path2D arrow = new Path2D.Float();
			arrow.moveTo(10.5, 1.8); arrow.lineTo(15.8, 2.2); arrow.lineTo(14.3, 7); arrow.closePath();
			g.fill(arrow);
			break;
		case PUZZLE:
			// modules: three squares and a free one, rotated
			g.draw(new RoundRectangle2D.Float(2, 2, 6, 6, 2, 2));
			g.draw(new RoundRectangle2D.Float(2, 10, 6, 6, 2, 2));
			g.draw(new RoundRectangle2D.Float(10, 10, 6, 6, 2, 2));
			g.rotate(Math.PI / 4, 13, 5);
			g.draw(new RoundRectangle2D.Float(10.2f, 2.2f, 5.6f, 5.6f, 2, 2));
			break;
		case WARNING:
			Path2D triangle = new Path2D.Float();
			triangle.moveTo(9, 2.5); triangle.lineTo(16.5, 15.5); triangle.lineTo(1.5, 15.5); triangle.closePath();
			g.draw(triangle);
			g.drawLine(9, 7, 9, 11);
			g.fillOval(8, 12, 2, 2);
			break;
		case INFO:
			g.drawOval(2, 2, 14, 14);
			g.drawLine(9, 8, 9, 13);
			g.fillOval(8, 4, 2, 2);
			break;
		default:
			break;
		}
	}
}
