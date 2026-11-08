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
package gui.commons.frameutils.frame.arch;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/**
 * Vector icons of the small tool windows (hash checker, color picker, password generator...), drawn on an 18x18 grid
 * with the foreground of the component they are painted on, so they follow the look and feel (dark and light) and
 * the interface scale. No image files needed.
 */
public final class ToolIcons implements Icon {

	public enum Kind {
		COPY, FOLDER, REFRESH, CHECK, CROSS, EYEDROPPER, STAR, STAR_FILLED, PASTE, CLEAR, STOP, DICE, EYE, BROWSER, SEARCH, PALETTE
	}

	private final Kind kind;
	private final int size;
	private final Color fixedColor;

	private ToolIcons(Kind kind, int size, Color fixedColor) {
		this.kind = kind;
		this.size = size;
		this.fixedColor = fixedColor;
	}

	/** Icon drawn with the foreground color of the component it is painted on */
	public static Icon of(Kind kind, int size) {
		return new ToolIcons(kind, size, null);
	}

	/** Icon drawn with the given color */
	public static Icon of(Kind kind, int size, Color color) {
		return new ToolIcons(kind, size, color);
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
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			draw(g);
		} finally {
			g.dispose();
		}
	}

	private void draw(Graphics2D g) {
		switch (kind) {
		case COPY:
			g.draw(new RoundRectangle2D.Float(6, 6, 9.5f, 10, 2.5f, 2.5f));
			Path2D back = new Path2D.Float();
			back.moveTo(12, 3.5); back.lineTo(12, 2.8); back.quadTo(12, 2, 11, 2); back.lineTo(4, 2);
			back.quadTo(2.5, 2, 2.5, 3.5); back.lineTo(2.5, 11); back.quadTo(2.5, 12, 3.5, 12);
			g.draw(back);
			break;
		case FOLDER:
			Path2D folder = new Path2D.Float();
			folder.moveTo(2, 5); folder.lineTo(2, 14.5); folder.lineTo(16, 14.5); folder.lineTo(16, 6.5);
			folder.lineTo(9, 6.5); folder.lineTo(7.5, 4); folder.lineTo(2.8, 4); folder.closePath();
			g.draw(folder);
			break;
		case REFRESH:
			g.draw(new Arc2D.Float(3, 3, 12, 12, 40, 280, Arc2D.OPEN));
			Path2D arrow = new Path2D.Float();
			arrow.moveTo(10.5, 1.8); arrow.lineTo(15.8, 2.2); arrow.lineTo(14.3, 7); arrow.closePath();
			g.fill(arrow);
			break;
		case CHECK:
			Path2D check = new Path2D.Float();
			check.moveTo(3.5, 9.5); check.lineTo(7.5, 13.5); check.lineTo(14.5, 4.5);
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(check);
			break;
		case CROSS:
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(4, 4, 14, 14);
			g.drawLine(14, 4, 4, 14);
			break;
		case EYEDROPPER:
			// bulb on the top right, glass tube down to the tip on the bottom left
			g.fill(new RoundRectangle2D.Float(10.5f, 1.5f, 6, 6, 6, 6));
			g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(9, 6, 12, 9);
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			Path2D tube = new Path2D.Float();
			tube.moveTo(10.5, 7.5); tube.lineTo(4, 14); tube.lineTo(2.5, 15.5); tube.moveTo(4, 14);
			g.draw(tube);
			break;
		case STAR:
		case STAR_FILLED:
			Path2D star = new Path2D.Float();
			for (int i = 0; i < 10; i++) {
				double radius = i % 2 == 0 ? 7.5 : 3.2;
				double angle = -Math.PI / 2 + i * Math.PI / 5;
				double px = 9 + radius * Math.cos(angle);
				double py = 9.6 + radius * Math.sin(angle);
				if (i == 0) {
					star.moveTo(px, py);
				} else {
					star.lineTo(px, py);
				}
			}
			star.closePath();
			if (kind == Kind.STAR_FILLED) {
				g.fill(star);
			}
			g.draw(star);
			break;
		case PASTE:
			g.draw(new RoundRectangle2D.Float(3, 3.5f, 12, 13, 2.5f, 2.5f));
			g.fill(new RoundRectangle2D.Float(6, 1.5f, 6, 3.5f, 1.5f, 1.5f));
			g.drawLine(6, 9, 12, 9);
			g.drawLine(6, 12, 10, 12);
			break;
		case CLEAR:
			g.drawOval(2, 2, 14, 14);
			g.drawLine(6, 6, 12, 12);
			g.drawLine(12, 6, 6, 12);
			break;
		case STOP:
			g.fill(new RoundRectangle2D.Float(4, 4, 10, 10, 2.5f, 2.5f));
			break;
		case DICE:
			g.draw(new RoundRectangle2D.Float(2.5f, 2.5f, 13, 13, 3.5f, 3.5f));
			g.fillOval(5, 5, 2, 2);
			g.fillOval(8, 8, 2, 2);
			g.fillOval(11, 11, 2, 2);
			break;
		case EYE:
			Path2D eye = new Path2D.Float();
			eye.moveTo(1.5, 9); eye.quadTo(9, 1, 16.5, 9); eye.quadTo(9, 17, 1.5, 9); eye.closePath();
			g.draw(eye);
			g.fillOval(7, 7, 4, 4);
			break;
		case BROWSER:
			g.drawOval(2, 2, 14, 14);
			g.drawOval(6, 2, 6, 14);
			g.drawLine(2, 9, 16, 9);
			break;
		case SEARCH:
			g.drawOval(2, 2, 10, 10);
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(11, 11, 16, 16);
			break;
		case PALETTE:
			g.drawOval(2, 2, 14, 14);
			g.fillOval(5, 5, 3, 3);
			g.fillOval(10, 4, 3, 3);
			g.fillOval(4, 10, 3, 3);
			break;
		default:
			break;
		}
	}
}
