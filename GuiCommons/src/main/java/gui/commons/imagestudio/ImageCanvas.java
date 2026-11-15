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

	/** Crop drawn over the image and editable by hand: rectangle as fractions (x0, y0, x1, y1) of the shown image */
	public interface CropOverlay {
		/** null while the crop is not being edited */
		double[] rect();

		void dragged(double[] rect, boolean finished);
	}

	private static final int HIT_L = 1;
	private static final int HIT_R = 2;
	private static final int HIT_T = 4;
	private static final int HIT_B = 8;
	private static final int HIT_MOVE = 16;
	private static final int HANDLE = 9;
	private static final double MIN_SIZE = 0.10;
	private static final double MAX_MARGIN = 0.45;
	private CropOverlay cropOverlay;
	private int cropHit;
	private double[] cropStartRect;
	private Point cropStartPoint;

	public void setCropOverlay(CropOverlay overlay) {
		this.cropOverlay = overlay;
		repaint();
	}

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
				int hit = hitCrop(e.getPoint());
				if (hit != 0) {
					cropHit = hit;
					cropStartRect = cropOverlay.rect().clone();
					cropStartPoint = e.getPoint();
					return;
				}
				dragStart = e.getPoint();
				dragOffsetX = offsetX;
				dragOffsetY = offsetY;
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (cropHit != 0 && shown != null) {
					cropOverlay.dragged(dragCrop(e.getPoint()), false);
					return;
				}
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
				if (cropHit != 0 && shown != null) {
					cropOverlay.dragged(dragCrop(e.getPoint()), true);
					cropHit = 0;
					setCursor(cropCursor(hitCrop(e.getPoint())));
					return;
				}
				dragStart = null;
				setCursor(Cursor.getDefaultCursor());
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				setCursor(cropCursor(hitCrop(e.getPoint())));
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
		paintCrop(g, img);
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

	/** Screen rectangle of the crop: x, y, width, height */
	private int[] cropScreenRect() {
		double[] r = cropOverlay == null || shown == null ? null : cropOverlay.rect();
		if (r == null) {
			return null;
		}
		int x0 = (int) Math.round(offsetX + r[0] * shown.getWidth() * scale);
		int y0 = (int) Math.round(offsetY + r[1] * shown.getHeight() * scale);
		int x1 = (int) Math.round(offsetX + r[2] * shown.getWidth() * scale);
		int y1 = (int) Math.round(offsetY + r[3] * shown.getHeight() * scale);
		return new int[] {x0, y0, x1 - x0, y1 - y0};
	}

	private void paintCrop(Graphics2D g, BufferedImage img) {
		int[] c = cropScreenRect();
		if (c == null) {
			return;
		}
		int ix = (int) Math.round(offsetX);
		int iy = (int) Math.round(offsetY);
		int iw = (int) Math.round(img.getWidth() * scale);
		int ih = (int) Math.round(img.getHeight() * scale);
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(ix, iy, iw, c[1] - iy);
		g.fillRect(ix, c[1] + c[3], iw, iy + ih - c[1] - c[3]);
		g.fillRect(ix, c[1], c[0] - ix, c[3]);
		g.fillRect(c[0] + c[2], c[1], ix + iw - c[0] - c[2], c[3]);
		g.setColor(new Color(255, 255, 255, 70));
		for (int i = 1; i <= 2; i++) {
			g.drawLine(c[0] + c[2] * i / 3, c[1], c[0] + c[2] * i / 3, c[1] + c[3]);
			g.drawLine(c[0], c[1] + c[3] * i / 3, c[0] + c[2], c[1] + c[3] * i / 3);
		}
		g.setColor(Color.BLACK);
		g.drawRect(c[0] - 1, c[1] - 1, c[2] + 2, c[3] + 2);
		g.setColor(Color.WHITE);
		g.drawRect(c[0], c[1], c[2], c[3]);
		for (int[] h : handles(c)) {
			g.setColor(Color.WHITE);
			g.fillRect(h[0] - HANDLE / 2, h[1] - HANDLE / 2, HANDLE, HANDLE);
			g.setColor(Color.BLACK);
			g.drawRect(h[0] - HANDLE / 2, h[1] - HANDLE / 2, HANDLE, HANDLE);
		}
		// size readout inside the crop
		double[] rect = cropOverlay.rect();
		String text = Math.round((rect[2] - rect[0]) * img.getWidth()) + " x " + Math.round((rect[3] - rect[1]) * img.getHeight());
		java.awt.FontMetrics fm = g.getFontMetrics();
		int tw = fm.stringWidth(text) + 10;
		if (c[2] > tw + 8 && c[3] > fm.getHeight() + 8) {
			g.setColor(new Color(0, 0, 0, 160));
			g.fillRect(c[0] + 4, c[1] + 4, tw, fm.getHeight() + 2);
			g.setColor(Color.WHITE);
			g.drawString(text, c[0] + 9, c[1] + 4 + fm.getAscent() + 1);
		}
	}

	/** Handles: x, y, hit code */
	private int[][] handles(int[] c) {
		int mx = c[0] + c[2] / 2;
		int my = c[1] + c[3] / 2;
		int r = c[0] + c[2];
		int b = c[1] + c[3];
		return new int[][] {
				{c[0], c[1], HIT_L | HIT_T}, {mx, c[1], HIT_T}, {r, c[1], HIT_R | HIT_T}, {r, my, HIT_R},
				{r, b, HIT_R | HIT_B}, {mx, b, HIT_B}, {c[0], b, HIT_L | HIT_B}, {c[0], my, HIT_L}};
	}

	private int hitCrop(Point p) {
		int[] c = cropScreenRect();
		if (c == null) {
			return 0;
		}
		for (int[] h : handles(c)) {
			if (Math.abs(p.x - h[0]) <= HANDLE && Math.abs(p.y - h[1]) <= HANDLE) {
				return h[2];
			}
		}
		int edge = 5;
		boolean insideX = p.x >= c[0] - edge && p.x <= c[0] + c[2] + edge;
		boolean insideY = p.y >= c[1] - edge && p.y <= c[1] + c[3] + edge;
		if (insideY && Math.abs(p.x - c[0]) <= edge) {
			return HIT_L;
		}
		if (insideY && Math.abs(p.x - (c[0] + c[2])) <= edge) {
			return HIT_R;
		}
		if (insideX && Math.abs(p.y - c[1]) <= edge) {
			return HIT_T;
		}
		if (insideX && Math.abs(p.y - (c[1] + c[3])) <= edge) {
			return HIT_B;
		}
		if (p.x > c[0] && p.x < c[0] + c[2] && p.y > c[1] && p.y < c[1] + c[3]) {
			return HIT_MOVE;
		}
		return 0;
	}

	private Cursor cropCursor(int hit) {
		switch (hit) {
			case 0:
				return Cursor.getDefaultCursor();
			case HIT_MOVE:
				return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
			case HIT_L:
				return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
			case HIT_R:
				return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
			case HIT_T:
				return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
			case HIT_B:
				return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
			case HIT_L | HIT_T:
				return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
			case HIT_R | HIT_T:
				return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
			case HIT_L | HIT_B:
				return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
			default:
				return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
		}
	}

	/** New crop rectangle for the drag in progress, kept inside the image and inside the margin limits of the sliders */
	private double[] dragCrop(Point p) {
		double dx = (p.x - cropStartPoint.x) / (scale * shown.getWidth());
		double dy = (p.y - cropStartPoint.y) / (scale * shown.getHeight());
		double[] r = cropStartRect.clone();
		if (cropHit == HIT_MOVE) {
			double w = r[2] - r[0];
			double h = r[3] - r[1];
			double x0 = Math.max(Math.max(0, 1 - MAX_MARGIN - w), Math.min(Math.min(MAX_MARGIN, 1 - w), r[0] + dx));
			double y0 = Math.max(Math.max(0, 1 - MAX_MARGIN - h), Math.min(Math.min(MAX_MARGIN, 1 - h), r[1] + dy));
			return new double[] {x0, y0, x0 + w, y0 + h};
		}
		if ((cropHit & HIT_L) != 0) {
			r[0] = Math.max(0, Math.min(Math.min(MAX_MARGIN, r[2] - MIN_SIZE), r[0] + dx));
		}
		if ((cropHit & HIT_R) != 0) {
			r[2] = Math.min(1, Math.max(Math.max(1 - MAX_MARGIN, r[0] + MIN_SIZE), r[2] + dx));
		}
		if ((cropHit & HIT_T) != 0) {
			r[1] = Math.max(0, Math.min(Math.min(MAX_MARGIN, r[3] - MIN_SIZE), r[1] + dy));
		}
		if ((cropHit & HIT_B) != 0) {
			r[3] = Math.min(1, Math.max(Math.max(1 - MAX_MARGIN, r[1] + MIN_SIZE), r[3] + dy));
		}
		return r;
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
