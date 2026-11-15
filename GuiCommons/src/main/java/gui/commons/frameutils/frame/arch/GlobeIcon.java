package gui.commons.frameutils.frame.arch;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

import javax.swing.Icon;

/** World icon drawn in code (no image file): used by the language button of the toolbar */
public class GlobeIcon implements Icon {

	private final int size;
	private final Color color;

	public GlobeIcon(int size, Color color) {
		this.size = size;
		this.color = color;
	}

	@Override
	public void paintIcon(Component c, Graphics g0, int x, int y) {
		Graphics2D g = (Graphics2D) g0.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		float stroke = Math.max(1.2f, size / 12f);
		g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		double pad = stroke / 2 + 1;
		double d = size - 2 * pad;
		double left = x + pad;
		double top = y + pad;
		double cx = left + d / 2;
		double cy = top + d / 2;

		g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
		g.fill(new Ellipse2D.Double(left, top, d, d));
		g.setColor(color);
		g.draw(new Ellipse2D.Double(left, top, d, d));
		// meridians
		g.draw(new Ellipse2D.Double(cx - d * 0.22, top, d * 0.44, d));
		g.drawLine((int) Math.round(cx), (int) Math.round(top), (int) Math.round(cx), (int) Math.round(top + d));
		// equator and the two parallels
		g.drawLine((int) Math.round(left), (int) Math.round(cy), (int) Math.round(left + d), (int) Math.round(cy));
		double inset = d * 0.17;
		double chord = Math.sqrt(Math.max(0, (d / 2) * (d / 2) - (d * 0.30) * (d * 0.30)));
		g.drawLine((int) Math.round(cx - chord), (int) Math.round(cy - d * 0.30), (int) Math.round(cx + chord), (int) Math.round(cy - d * 0.30));
		g.drawLine((int) Math.round(cx - chord), (int) Math.round(cy + d * 0.30), (int) Math.round(cx + chord), (int) Math.round(cy + d * 0.30));
		g.dispose();
	}

	@Override
	public int getIconWidth() {
		return size;
	}

	@Override
	public int getIconHeight() {
		return size;
	}
}
