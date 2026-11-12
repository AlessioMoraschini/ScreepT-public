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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.JComponent;

import gui.commons.frameutils.frame.arch.ToolStyle;

/** Image area: fit / zoom around the cursor / pan by dragging, transparency checkerboard, pixel readout, pixel grid */
public class ImageCanvas extends JComponent {
	private static final long serialVersionUID = 1L;

	private BufferedImage shown;
	private double scale = 1.0;
	private double offsetX;
	private double offsetY;
	private boolean fitMode = true;
	private Point dragStart;
	private double dragOffsetX;
	private double dragOffsetY;

	private String backgroundStyle = "Checker";
	private int interpolation = 0;
	private boolean grid;
	private boolean pixelReadout = true;
	private Consumer<String> pixelListener;
	private Consumer<String> zoomListener;
	private Consumer<java.io.File> fileDropListener;

	public ImageCanvas() {
		setOpaque(true);
		setPreferredSize(new Dimension(260, 200));
		setMinimumSize(new Dimension(0, 0));
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				dragStart = e.getPoint();
				dragOffsetX = offsetX;
				dragOffsetY = offsetY;
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (dragStart != null && shown != null) {
					fitMode = false;
					offsetX = dragOffsetX + (e.getX() - dragStart.x);
					offsetY = dragOffsetY + (e.getY() - dragStart.y);
					repaint();
				}
				report(e.getPoint());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				dragStart = null;
				setCursor(Cursor.getDefaultCursor());
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				report(e.getPoint());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (pixelListener != null) {
					pixelListener.accept(" ");
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					if (fitMode) {
						actualSize();
					} else {
						fit();
					}
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				zoomBy(e.getWheelRotation() < 0 ? 1.15 : 1 / 1.15, e.getPoint());
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				if (fitMode) {
					fit();
				}
			}
		});
	}

	public void setPixelListener(Consumer<String> listener) {
		this.pixelListener = listener;
	}

	public void setZoomListener(Consumer<String> listener) {
		this.zoomListener = listener;
	}

	public void setPixelReadout(boolean enabled) {
		this.pixelReadout = enabled;
	}

	public void setBackgroundStyle(String style) {
		this.backgroundStyle = style;
		repaint();
	}

	public void setInterpolation(int index) {
		this.interpolation = index;
		repaint();
	}

	public void setGrid(boolean grid) {
		this.grid = grid;
		repaint();
	}

	public BufferedImage getImage() {
		return shown;
	}

	/** @param refit true: fit to the window; false: keep zoom and position (the size may change) */
	private String message;

	/** Text shown instead of an image (why there is none) */
	public void setMessage(String text) {
		message = text;
		repaint();
	}

	public void setImage(BufferedImage img, boolean refit) {
		boolean sameSize = shown != null && img != null && shown.getWidth() == img.getWidth() && shown.getHeight() == img.getHeight();
		this.shown = img;
		if (refit || fitMode || !sameSize) {
			if (refit || fitMode) {
				fit();
			} else {
				center();
				repaint();
			}
		} else {
			repaint();
		}
	}

	public void fit() {
		fitMode = true;
		BufferedImage img = shown;
		if (img == null || getWidth() <= 0 || getHeight() <= 0) {
			repaint();
			return;
		}
		double sx = (getWidth() - 16.0) / img.getWidth();
		double sy = (getHeight() - 16.0) / img.getHeight();
		scale = Math.max(0.01, Math.min(1.0, Math.min(sx, sy)));
		center();
		repaint();
		updateZoom();
	}

	public void actualSize() {
		fitMode = false;
		scale = 1.0;
		center();
		repaint();
		updateZoom();
	}

	private void center() {
		if (shown == null) {
			return;
		}
		offsetX = (getWidth() - shown.getWidth() * scale) / 2.0;
		offsetY = (getHeight() - shown.getHeight() * scale) / 2.0;
	}

	public void zoomBy(double factor, Point anchor) {
		if (shown == null) {
			return;
		}
		Point p = anchor != null ? anchor : new Point(getWidth() / 2, getHeight() / 2);
		double newScale = Math.max(0.02, Math.min(64.0, scale * factor));
		double imgX = (p.x - offsetX) / scale;
		double imgY = (p.y - offsetY) / scale;
		scale = newScale;
		offsetX = p.x - imgX * scale;
		offsetY = p.y - imgY * scale;
		fitMode = false;
		repaint();
		updateZoom();
	}

	private void updateZoom() {
		if (zoomListener != null) {
			zoomListener.accept(Math.round(scale * 100) + "%" + (fitMode ? " (fit)" : ""));
		}
	}

	private void report(Point p) {
		if (!pixelReadout || shown == null || pixelListener == null) {
			return;
		}
		int x = (int) Math.floor((p.x - offsetX) / scale);
		int y = (int) Math.floor((p.y - offsetY) / scale);
		if (x < 0 || y < 0 || x >= shown.getWidth() || y >= shown.getHeight()) {
			pixelListener.accept(" ");
			return;
		}
		int argb = shown.getRGB(x, y);
		pixelListener.accept(String.format(Locale.ROOT, "x %d  y %d    #%06X   rgb(%d, %d, %d)   alpha %d", x, y, argb & 0xFFFFFF,
				(argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff, argb >>> 24));
	}

	@Override
	protected void paintComponent(Graphics g0) {
		Graphics2D g = (Graphics2D) g0.create();
		paintBackground(g);
		BufferedImage img = shown;
		if (img == null) {
			g.setColor(ToolStyle.muted());
			String[] lines = (message != null ? message : "Drop an image here").split("\n");
			java.awt.FontMetrics fm = g.getFontMetrics();
			int y = getHeight() / 2 - (lines.length - 1) * fm.getHeight() / 2;
			for (String line : lines) {
				g.drawString(line, Math.max(4, (getWidth() - fm.stringWidth(line)) / 2), y);
				y += fm.getHeight();
			}
			g.dispose();
			return;
		}
		Object hint = interpolation == 2 ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
				: interpolation == 1 ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		int dw = (int) Math.round(img.getWidth() * scale);
		int dh = (int) Math.round(img.getHeight() * scale);
		BufferedImage source = img;
		if (scale < 0.5 && interpolation != 2) {
			// big reductions: halve first, otherwise the picture aliases
			double s = scale;
			while (s < 0.5 && source.getWidth() > 4 && source.getHeight() > 4) {
				BufferedImage half = new BufferedImage(Math.max(1, source.getWidth() / 2), Math.max(1, source.getHeight() / 2), BufferedImage.TYPE_INT_ARGB);
				Graphics2D hg = half.createGraphics();
				hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				hg.drawImage(source, 0, 0, half.getWidth(), half.getHeight(), null);
				hg.dispose();
				source = half;
				s *= 2;
			}
		}
		g.drawImage(source, (int) Math.round(offsetX), (int) Math.round(offsetY), dw, dh, null);
		if (grid && scale >= 8) {
			g.setColor(new Color(128, 128, 128, 120));
			int x0 = (int) Math.max(0, Math.floor(-offsetX / scale));
			int y0 = (int) Math.max(0, Math.floor(-offsetY / scale));
			int x1 = (int) Math.min(img.getWidth(), Math.ceil((getWidth() - offsetX) / scale));
			int y1 = (int) Math.min(img.getHeight(), Math.ceil((getHeight() - offsetY) / scale));
			for (int x = x0; x <= x1; x++) {
				int sx = (int) Math.round(offsetX + x * scale);
				g.drawLine(sx, (int) Math.round(offsetY + y0 * scale), sx, (int) Math.round(offsetY + y1 * scale));
			}
			for (int y = y0; y <= y1; y++) {
				int sy = (int) Math.round(offsetY + y * scale);
				g.drawLine((int) Math.round(offsetX + x0 * scale), sy, (int) Math.round(offsetX + x1 * scale), sy);
			}
		}
		g.dispose();
	}

	private void paintBackground(Graphics2D g) {
		String bg = backgroundStyle;
		if (bg == null || "Checker".equals(bg)) {
			boolean dark = ToolStyle.isDark();
			g.setColor(dark ? new Color(48, 50, 56) : new Color(238, 238, 238));
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(dark ? new Color(60, 62, 70) : new Color(214, 214, 214));
			int size = 12;
			for (int y = 0; y < getHeight(); y += size) {
				for (int x = ((y / size) % 2) * size; x < getWidth(); x += size * 2) {
					g.fillRect(x, y, size, size);
				}
			}
		} else {
			g.setColor("Dark".equals(bg) ? new Color(32, 33, 38) : "Gray".equals(bg) ? new Color(128, 128, 128)
					: "Light".equals(bg) ? new Color(232, 232, 232) : "Black".equals(bg) ? Color.BLACK : Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}
}
