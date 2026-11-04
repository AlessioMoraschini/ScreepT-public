package markdown.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Window icon of the Markdown viewer: the "M" and the down arrow of the Markdown logo on a blue rounded square.
 * It is drawn at every size (no image file), so it stays sharp in the title bar, taskbar and Alt+Tab.
 */
final class AppIcon {

	private static final int[] SIZES = {16, 20, 24, 32, 48, 64, 128};

	private AppIcon() {
	}

	static List<Image> images() {
		List<Image> images = new ArrayList<Image>();
		for(int size : SIZES) {
			images.add(draw(size));
		}
		return images;
	}

	static Image draw(int s) {
		BufferedImage image = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			float radius = s * 0.22f;
			g.setPaint(new GradientPaint(0, 0, new Color(0x5B9DFF), 0, s, new Color(0x2756C9)));
			g.fill(new RoundRectangle2D.Float(0.5f, 0.5f, s - 1f, s - 1f, radius * 2, radius * 2));

			// soft light edge on the top
			g.setColor(new Color(255, 255, 255, 60));
			g.setStroke(new BasicStroke(Math.max(1f, s / 40f)));
			g.draw(new RoundRectangle2D.Float(1f, 1f, s - 2f, s - 2f, radius * 2 - 1, radius * 2 - 1));

			// "M"
			Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.round(s * 0.52f));
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics();
			String m = "M";
			float mWidth = fm.stringWidth(m);
			float arrowWidth = s * 0.20f;
			float gap = s * 0.06f;
			float x0 = (s - (mWidth + gap + arrowWidth)) / 2f;
			float baseline = (s - fm.getHeight()) / 2f + fm.getAscent() - s * 0.02f;
			g.setColor(Color.WHITE);
			g.drawString(m, x0, baseline);

			// down arrow
			float ax = x0 + mWidth + gap + arrowWidth / 2f;
			float top = s * 0.32f;
			float bottom = s * 0.66f;
			g.setStroke(new BasicStroke(Math.max(1.2f, s * 0.075f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new Line2D.Float(ax, top, ax, bottom - s * 0.02f));
			Path2D head = new Path2D.Float();
			head.moveTo(ax - arrowWidth * 0.5f, bottom - arrowWidth * 0.62f);
			head.lineTo(ax, bottom);
			head.lineTo(ax + arrowWidth * 0.5f, bottom - arrowWidth * 0.62f);
			g.draw(head);
		} finally {
			g.dispose();
		}
		return image;
	}
}
