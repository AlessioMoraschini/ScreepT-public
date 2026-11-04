package markdown.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

import javax.swing.Icon;

/**
 * Small vector icons of the Markdown viewer toolbar. They are drawn with the foreground color of the button,
 * so they follow the look and feel (dark or light) and the enabled state, and need no image files.
 */
final class ToolIcons implements Icon {

	enum Kind {
		OPEN, SAVE, SAVE_AS, EXPORT, EDITOR, SPLIT, PREVIEW, REFRESH, COPY, BROWSER,
		CODE_BLOCK, LINK, IMAGE, LIST, NUMBERED, TASK, QUOTE, TABLE, RULE,
		OUTLINE, WRAP, SYNC, INVERT, ZOOM_IN, ZOOM_OUT
	}

	private static final int SIZE = 18;
	private final Kind kind;

	private ToolIcons(Kind kind) {
		this.kind = kind;
	}

	static Icon of(Kind kind) {
		return new ToolIcons(kind);
	}

	@Override
	public int getIconWidth() {
		return SIZE;
	}

	@Override
	public int getIconHeight() {
		return SIZE;
	}

	@Override
	public void paintIcon(Component c, Graphics g0, int x, int y) {
		Graphics2D g = (Graphics2D) g0.create();
		try {
			g.translate(x, y);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			Color fg = c != null && c.getForeground() != null ? c.getForeground() : Color.GRAY;
			boolean lightForeground = (0.299 * fg.getRed() + 0.587 * fg.getGreen() + 0.114 * fg.getBlue()) > 128;
			Color accent = lightForeground ? new Color(110, 168, 255) : new Color(30, 100, 220);
			if(c != null && !c.isEnabled()) {
				fg = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 90);
				accent = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90);
			}
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(fg);
			draw(g, fg, accent);
		} finally {
			g.dispose();
		}
	}

	private void draw(Graphics2D g, Color fg, Color accent) {
		switch(kind) {
		case OPEN: {
			Path2D p = new Path2D.Float();
			p.moveTo(2, 4.5); p.lineTo(7, 4.5); p.lineTo(9, 6.5); p.lineTo(15.5, 6.5); p.lineTo(15.5, 14.5); p.lineTo(2, 14.5); p.closePath();
			g.draw(p);
			g.setColor(accent);
			g.drawLine(2, 9, 15, 9);
			break;
		}
		case SAVE: {
			floppy(g, accent, 0);
			break;
		}
		case SAVE_AS: {
			floppy(g, accent, -1);
			g.setColor(accent);
			g.drawLine(13, 11, 13, 16);
			g.drawLine(10, 14, 16, 14);
			break;
		}
		case EXPORT: {
			Path2D p = new Path2D.Float();
			p.moveTo(3.5, 2); p.lineTo(9.5, 2); p.lineTo(12.5, 5); p.lineTo(12.5, 16); p.lineTo(3.5, 16); p.closePath();
			g.draw(p);
			g.drawLine(9, 2, 9, 5);
			g.drawLine(9, 5, 12, 5);
			g.setColor(accent);
			g.drawLine(7, 10, 16, 10);
			g.drawLine(13, 7, 16, 10);
			g.drawLine(13, 13, 16, 10);
			break;
		}
		case EDITOR: {
			g.drawRoundRect(2, 3, 14, 12, 3, 3);
			g.drawLine(5, 7, 11, 7);
			g.drawLine(5, 10, 13, 10);
			g.drawLine(5, 13, 9, 13);
			break;
		}
		case SPLIT: {
			g.drawRoundRect(2, 3, 14, 12, 3, 3);
			g.drawLine(9, 3, 9, 15);
			g.drawLine(4, 7, 7, 7);
			g.drawLine(4, 10, 7, 10);
			g.setColor(accent);
			g.fillRect(11, 6, 3, 4);
			break;
		}
		case PREVIEW: {
			Path2D eye = new Path2D.Float();
			eye.moveTo(1, 9); eye.quadTo(9, 1.5, 17, 9); eye.quadTo(9, 16.5, 1, 9);
			g.draw(eye);
			g.setColor(accent);
			g.fillOval(6, 6, 6, 6);
			break;
		}
		case REFRESH: {
			g.draw(new Arc2D.Float(3, 3, 12, 12, 40, 280, Arc2D.OPEN));
			g.setColor(accent);
			Path2D head = new Path2D.Float();
			head.moveTo(10.5, 2.2); head.lineTo(15.8, 2.4); head.lineTo(14.3, 7); head.closePath();
			g.fill(head);
			break;
		}
		case COPY: {
			g.drawLine(3, 11, 3, 3);
			g.drawLine(3, 3, 11, 3);
			g.drawRoundRect(6, 6, 9, 9, 2, 2);
			g.setColor(accent);
			g.drawLine(8, 10, 13, 10);
			g.drawLine(8, 13, 11, 13);
			break;
		}
		case BROWSER: {
			g.drawOval(2, 2, 14, 14);
			g.drawOval(6, 2, 6, 14);
			g.drawLine(2, 9, 16, 9);
			break;
		}
		case CODE_BLOCK: {
			g.drawRoundRect(2, 3, 14, 12, 3, 3);
			g.drawLine(5, 7, 8, 7);
			g.setColor(accent);
			g.drawLine(7, 10, 13, 10);
			g.setColor(fg);
			g.drawLine(5, 13, 9, 13);
			break;
		}
		case LINK: {
			java.awt.geom.AffineTransform old = g.getTransform();
			g.rotate(-Math.PI / 4, 9, 9);
			g.drawRoundRect(1, 6, 9, 6, 6, 6);
			g.setColor(accent);
			g.drawRoundRect(8, 6, 9, 6, 6, 6);
			g.setTransform(old);
			break;
		}
		case IMAGE: {
			g.drawRoundRect(2, 3, 14, 12, 3, 3);
			g.setColor(accent);
			g.fillOval(11, 5, 3, 3);
			g.setColor(fg);
			Path2D hills = new Path2D.Float();
			hills.moveTo(3.5, 13.5); hills.lineTo(7, 9); hills.lineTo(10, 12); hills.lineTo(12, 10); hills.lineTo(14.5, 13.5);
			g.draw(hills);
			break;
		}
		case LIST: {
			for(int i = 0; i < 3; i++) {
				int y = 4 + i * 5;
				g.setColor(accent);
				g.fillOval(2, y - 1, 3, 3);
				g.setColor(fg);
				g.drawLine(7, y, 16, y);
			}
			break;
		}
		case NUMBERED: {
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7));
			for(int i = 0; i < 3; i++) {
				int y = 4 + i * 5;
				g.setColor(accent);
				g.drawString(String.valueOf(i + 1), 1, y + 3);
				g.setColor(fg);
				g.drawLine(7, y, 16, y);
			}
			break;
		}
		case TASK: {
			g.drawRoundRect(2, 2, 6, 6, 2, 2);
			g.setColor(accent);
			Path2D check = new Path2D.Float();
			check.moveTo(3.5, 5); check.lineTo(5, 6.6); check.lineTo(7.4, 3.2);
			g.draw(check);
			g.setColor(fg);
			g.drawLine(10, 5, 16, 5);
			g.drawRoundRect(2, 10, 6, 6, 2, 2);
			g.drawLine(10, 13, 16, 13);
			break;
		}
		case QUOTE: {
			g.setColor(accent);
			for(int i = 0; i < 2; i++) {
				int x = 2 + i * 8;
				g.fillOval(x, 5, 5, 5);
				g.drawLine(x + 4, 8, x + 2, 13);
			}
			break;
		}
		case TABLE: {
			g.setColor(accent);
			g.fillRect(3, 4, 12, 3);
			g.setColor(fg);
			g.drawRoundRect(2, 3, 14, 12, 2, 2);
			g.drawLine(2, 7, 16, 7);
			g.drawLine(2, 11, 16, 11);
			g.drawLine(7, 3, 7, 15);
			g.drawLine(12, 3, 12, 15);
			break;
		}
		case RULE: {
			g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 110));
			g.drawLine(4, 4, 14, 4);
			g.drawLine(4, 14, 14, 14);
			g.setColor(accent);
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(2, 9, 16, 9);
			break;
		}
		case OUTLINE: {
			g.setColor(accent);
			g.fillOval(2, 3, 3, 3);
			g.setColor(fg);
			g.drawLine(7, 5, 16, 5);
			g.drawLine(8, 9, 16, 9);
			g.drawLine(11, 13, 16, 13);
			g.setColor(accent);
			g.drawLine(4, 7, 4, 9);
			g.drawLine(4, 9, 6, 9);
			break;
		}
		case WRAP: {
			g.drawLine(2, 5, 15, 5);
			g.drawLine(15, 5, 15, 12);
			g.drawLine(15, 12, 7, 12);
			g.setColor(accent);
			Path2D head = new Path2D.Float();
			head.moveTo(9.5, 8.5); head.lineTo(5, 12); head.lineTo(9.5, 15.5); head.closePath();
			g.fill(head);
			break;
		}
		case SYNC: {
			g.drawLine(6, 15, 6, 4);
			g.drawLine(3, 7, 6, 4);
			g.drawLine(9, 7, 6, 4);
			g.setColor(accent);
			g.drawLine(12, 3, 12, 14);
			g.drawLine(9, 11, 12, 14);
			g.drawLine(15, 11, 12, 14);
			break;
		}
		case INVERT: {
			g.drawOval(2, 2, 14, 14);
			g.setColor(accent);
			g.fill(new Arc2D.Float(2, 2, 14, 14, 90, 180, Arc2D.PIE));
			break;
		}
		case ZOOM_IN:
		case ZOOM_OUT: {
			g.drawOval(2, 2, 10, 10);
			g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(11, 11, 16, 16);
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(accent);
			g.drawLine(5, 7, 9, 7);
			if(kind == Kind.ZOOM_IN) {
				g.drawLine(7, 5, 7, 9);
			}
			break;
		}
		default:
			break;
		}
	}

	private void floppy(Graphics2D g, Color accent, int offset) {
		Path2D body = new Path2D.Float();
		body.moveTo(3, 3); body.lineTo(12, 3); body.lineTo(15, 6); body.lineTo(15, 15); body.lineTo(3, 15); body.closePath();
		g.draw(body);
		g.drawRect(6, 3, 5, 4);
		g.setColor(accent);
		g.drawRect(5, 10, 8, 5);
	}
}
