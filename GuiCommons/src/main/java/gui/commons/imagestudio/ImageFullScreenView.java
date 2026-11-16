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

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import various.common.light.i18n.I18n;

/**
 * Full screen preview of an image, for the Image Studio (window or tab). The picture fills the screen (zoom with the wheel, drag to
 * move, double click for fit / actual size) and a small menu floats at the top center with zoom and rotation (view only: the image
 * is not changed). The menu disappears 5 seconds after the mouse left it and comes back as soon as the mouse passes over its place;
 * the pin keeps it always visible. The close button at the top right is always visible (Esc closes too).
 */
public final class ImageFullScreenView extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final int HIDE_DELAY_MS = 5000;
	private static final int MARGIN = 16;

	/** The pin is kept while the application is open */
	private static boolean pinnedByUser = false;

	private final ImageCanvas canvas = new ImageCanvas();
	private final BufferedImage source;
	private int quarterTurns = 0;

	private final JPanel menu;
	private final RoundButton btnPin;
	private final JLabel lblZoom = new JLabel(" ");
	private final RoundButton btnClose;
	private final JLayeredPane layers;
	private final Timer hideTimer;
	private boolean pinned = pinnedByUser;
	private final java.awt.event.AWTEventListener mouseWatcher;

	/** Shows the image full screen, on the screen of the owner window; returns when it is shown (the view is not modal) */
	public static void show(Window owner, BufferedImage image) {
		if (image == null) {
			return;
		}
		ImageFullScreenView view = new ImageFullScreenView(owner, image);
		view.setVisible(true);
		view.toFront();
		view.requestFocus();
	}

	private ImageFullScreenView(Window owner, BufferedImage image) {
		super(owner);
		this.source = image;
		setUndecorated(true);
		setAlwaysOnTop(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		GraphicsConfiguration gc = owner != null && owner.getGraphicsConfiguration() != null ? owner.getGraphicsConfiguration()
				: GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		Rectangle screen = gc.getBounds();
		setBounds(screen);

		canvas.setBackgroundStyle("Black");
		canvas.setPixelReadout(false);
		canvas.setZoomListener(text -> lblZoom.setText(text));
		canvas.setImage(image, true);

		// ---- menu at the top center
		menu = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(24, 26, 30, 225));
				g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
				g2.setColor(new Color(255, 255, 255, 40));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
				g2.dispose();
			}
		};
		menu.setOpaque(false);
		menu.add(new RoundButton(Kind.ZOOM_OUT, I18n.t("gui.fs_zoom_out"), () -> canvas.zoomBy(1 / 1.25, null)));
		menu.add(new RoundButton(Kind.ZOOM_IN, I18n.t("gui.fs_zoom_in"), () -> canvas.zoomBy(1.25, null)));
		menu.add(new RoundButton(Kind.FIT, I18n.t("gui.fs_fit"), () -> canvas.fit()));
		menu.add(new RoundButton(Kind.ACTUAL, I18n.t("gui.fs_actual_size"), () -> canvas.actualSize()));
		menu.add(new RoundButton(Kind.ROTATE_LEFT, I18n.t("gui.fs_rotate_left"), () -> rotate(-1)));
		menu.add(new RoundButton(Kind.ROTATE_RIGHT, I18n.t("gui.fs_rotate_right"), () -> rotate(1)));
		lblZoom.setForeground(new Color(220, 224, 230));
		lblZoom.setFont(lblZoom.getFont().deriveFont(Font.BOLD, 12f));
		lblZoom.setPreferredSize(new Dimension(lblZoom.getFontMetrics(lblZoom.getFont()).stringWidth("6400% (fit)") + 10, 28));
		lblZoom.setHorizontalAlignment(JLabel.CENTER);
		menu.add(lblZoom);
		btnPin = new RoundButton(Kind.PIN, I18n.t("gui.fs_pin"), this::togglePin);
		btnPin.setSelected(pinned);
		menu.add(btnPin);

		// ---- close button: always visible
		btnClose = new RoundButton(Kind.CLOSE, I18n.t("gui.fs_exit"), this::dispose);
		btnClose.setPreferredSize(new Dimension(40, 40));

		layers = new JLayeredPane() {
			private static final long serialVersionUID = 1L;

			@Override
			public void doLayout() {
				canvas.setBounds(0, 0, getWidth(), getHeight());
				menu.setBounds(menuZone());
				btnClose.setBounds(getWidth() - MARGIN - 40, MARGIN, 40, 40);
			}
		};
		layers.add(canvas, JLayeredPane.DEFAULT_LAYER);
		layers.add(menu, JLayeredPane.PALETTE_LAYER);
		layers.add(btnClose, JLayeredPane.MODAL_LAYER);
		setContentPane(layers);
		menu.setSize(menu.getPreferredSize());

		bindKeys();

		hideTimer = new Timer(HIDE_DELAY_MS, e -> hideMenuIfIdle());
		hideTimer.setRepeats(false);

		// the mouse over the canvas must be seen wherever it is: the menu place is a hot zone even when the menu is hidden
		mouseWatcher = event -> {
			if (event instanceof MouseEvent && ((MouseEvent) event).getID() == MouseEvent.MOUSE_MOVED || event instanceof MouseEvent && ((MouseEvent) event).getID() == MouseEvent.MOUSE_DRAGGED) {
				MouseEvent me = (MouseEvent) event;
				if (me.getSource() instanceof java.awt.Component && SwingUtilities.getWindowAncestor((java.awt.Component) me.getSource()) == ImageFullScreenView.this) {
					Point p = SwingUtilities.convertPoint((java.awt.Component) me.getSource(), me.getPoint(), layers);
					if (menuZone().contains(p)) {
						showMenu();
					}
				}
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(mouseWatcher, AWTEvent.MOUSE_MOTION_EVENT_MASK);

		showMenu();
	}

	@Override
	public void dispose() {
		hideTimer.stop();
		Toolkit.getDefaultToolkit().removeAWTEventListener(mouseWatcher);
		super.dispose();
	}

	/** Place of the menu (also its hot zone while hidden): top center */
	private Rectangle menuZone() {
		Dimension size = menu.getPreferredSize();
		return new Rectangle((layers.getWidth() - size.width) / 2, MARGIN, size.width, size.height);
	}

	/** The menu is shown (when it was hidden) and the 5 seconds start again */
	private void showMenu() {
		if (!menu.isVisible()) {
			menu.setVisible(true);
		}
		if (!pinned) {
			hideTimer.restart();
		}
	}

	/** After the delay the menu goes away, unless it is pinned or the mouse is on it (then the delay starts again) */
	private void hideMenuIfIdle() {
		if (pinned) {
			return;
		}
		Point mouse = java.awt.MouseInfo.getPointerInfo() != null ? java.awt.MouseInfo.getPointerInfo().getLocation() : null;
		if (mouse != null) {
			SwingUtilities.convertPointFromScreen(mouse, layers);
			if (menuZone().contains(mouse)) {
				hideTimer.restart();
				return;
			}
		}
		menu.setVisible(false);
	}

	private void togglePin() {
		pinned = !pinned;
		pinnedByUser = pinned;
		btnPin.setSelected(pinned);
		btnPin.setToolTipText(pinned ? I18n.t("gui.fs_unpin") : I18n.t("gui.fs_pin"));
		if (pinned) {
			hideTimer.stop();
			menu.setVisible(true);
		} else {
			hideTimer.restart();
		}
	}

	/** View rotation only: the original image is never touched */
	private void rotate(int turns) {
		quarterTurns = ((quarterTurns + turns) % 4 + 4) % 4;
		canvas.setImage(rotated(source, quarterTurns), true);
	}

	private static BufferedImage rotated(BufferedImage src, int turns) {
		if (turns == 0) {
			return src;
		}
		int w = src.getWidth();
		int h = src.getHeight();
		boolean swap = turns % 2 == 1;
		BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_ARGB);
		AffineTransform transform = new AffineTransform();
		if (turns == 1) {
			transform.translate(h, 0);
			transform.rotate(Math.PI / 2);
		} else if (turns == 2) {
			transform.translate(w, h);
			transform.rotate(Math.PI);
		} else {
			transform.translate(0, w);
			transform.rotate(-Math.PI / 2);
		}
		Graphics2D g = out.createGraphics();
		g.drawImage(src, transform, null);
		g.dispose();
		return out;
	}

	private void bindKeys() {
		JComponent root = getRootPane();
		bind(root, java.awt.event.KeyEvent.VK_ESCAPE, 0, "close", this::dispose);
		bind(root, java.awt.event.KeyEvent.VK_F11, 0, "close11", this::dispose);
		bind(root, java.awt.event.KeyEvent.VK_EQUALS, 0, "zin", () -> canvas.zoomBy(1.25, null));
		bind(root, java.awt.event.KeyEvent.VK_ADD, 0, "zin2", () -> canvas.zoomBy(1.25, null));
		bind(root, java.awt.event.KeyEvent.VK_MINUS, 0, "zout", () -> canvas.zoomBy(1 / 1.25, null));
		bind(root, java.awt.event.KeyEvent.VK_SUBTRACT, 0, "zout2", () -> canvas.zoomBy(1 / 1.25, null));
		bind(root, java.awt.event.KeyEvent.VK_F, 0, "fit", () -> canvas.fit());
		bind(root, java.awt.event.KeyEvent.VK_1, 0, "actual", () -> canvas.actualSize());
		bind(root, java.awt.event.KeyEvent.VK_R, 0, "rotR", () -> rotate(1));
		bind(root, java.awt.event.KeyEvent.VK_L, 0, "rotL", () -> rotate(-1));
		bind(root, java.awt.event.KeyEvent.VK_P, 0, "pin", this::togglePin);
	}

	private static void bind(JComponent root, int code, int modifiers, String name, Runnable action) {
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(code, modifiers), name);
		root.getActionMap().put(name, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	// ------------------------------------------------------------------ buttons

	private enum Kind {
		ZOOM_OUT, ZOOM_IN, FIT, ACTUAL, ROTATE_LEFT, ROTATE_RIGHT, PIN, CLOSE
	}

	/** Round button with a drawn icon (no image files); PIN is a toggle, the others run their action */
	private static final class RoundButton extends JComponent {
		private static final long serialVersionUID = 1L;

		private final Kind kind;
		private final Runnable action;
		private boolean hover;
		private boolean selected;

		RoundButton(Kind kind, String tooltip, Runnable action) {
			this.kind = kind;
			this.action = action;
			setToolTipText(tooltip);
			setPreferredSize(new Dimension(kind == Kind.FIT || kind == Kind.ACTUAL ? 44 : 32, 32));
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					hover = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					hover = false;
					repaint();
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						action.run();
					}
				}
			});
		}

		void setSelected(boolean selected) {
			this.selected = selected;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			int w = getWidth();
			int h = getHeight();
			boolean close = kind == Kind.CLOSE;
			if (close) {
				g2.setColor(new Color(24, 26, 30, hover ? 255 : 215));
				g2.fill(new Ellipse2D.Double(0, 0, w - 1, h - 1));
				g2.setColor(new Color(255, 255, 255, 60));
				g2.draw(new Ellipse2D.Double(0, 0, w - 1, h - 1));
			} else if (hover || selected) {
				g2.setColor(new Color(255, 255, 255, selected ? 70 : 45));
				g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
			}
			g2.setColor(new Color(236, 239, 244));
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			double cx = w / 2.0;
			double cy = h / 2.0;
			switch (kind) {
			case ZOOM_OUT:
				g2.drawLine((int) cx - 6, (int) cy, (int) cx + 6, (int) cy);
				break;
			case ZOOM_IN:
				g2.drawLine((int) cx - 6, (int) cy, (int) cx + 6, (int) cy);
				g2.drawLine((int) cx, (int) cy - 6, (int) cx, (int) cy + 6);
				break;
			case FIT:
				text(g2, "Fit", cx, cy);
				break;
			case ACTUAL:
				text(g2, "100%", cx, cy);
				break;
			case ROTATE_LEFT:
				arrow(g2, cx, cy, true);
				break;
			case ROTATE_RIGHT:
				arrow(g2, cx, cy, false);
				break;
			case PIN:
				pin(g2, cx, cy, selected);
				break;
			case CLOSE:
				g2.drawLine((int) cx - 6, (int) cy - 6, (int) cx + 6, (int) cy + 6);
				g2.drawLine((int) cx - 6, (int) cy + 6, (int) cx + 6, (int) cy - 6);
				break;
			default:
				break;
			}
			g2.dispose();
		}

		private static void text(Graphics2D g2, String text, double cx, double cy) {
			g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
			java.awt.FontMetrics fm = g2.getFontMetrics();
			g2.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) (cy + fm.getAscent() / 2.0 - 1));
		}

		/** Circular arrow: open circle with an arrow head, mirrored for the left rotation */
		private static void arrow(Graphics2D g2, double cx, double cy, boolean left) {
			double r = 8;
			double start = left ? 120 : 60;
			double extent = left ? 270 : -270;
			g2.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, start, extent, Arc2D.OPEN));
			double angle = Math.toRadians(start);
			double tx = cx + r * Math.cos(angle);
			double ty = cy - r * Math.sin(angle);
			int dir = left ? 1 : -1;
			Path2D head = new Path2D.Double();
			head.moveTo(tx, ty);
			head.lineTo(tx + dir * 6, ty - 1);
			head.moveTo(tx, ty);
			head.lineTo(tx + dir * 1, ty + 6);
			g2.draw(head);
		}

		/** Push pin: a head, a body and the needle; filled when it is on */
		private static void pin(Graphics2D g2, double cx, double cy, boolean on) {
			Path2D body = new Path2D.Double();
			body.moveTo(cx - 4, cy - 8);
			body.lineTo(cx + 4, cy - 8);
			body.lineTo(cx + 3, cy - 1);
			body.lineTo(cx + 7, cy + 2);
			body.lineTo(cx - 7, cy + 2);
			body.lineTo(cx - 3, cy - 1);
			body.closePath();
			if (on) {
				g2.fill(body);
			} else {
				g2.draw(body);
			}
			g2.drawLine((int) cx, (int) cy + 2, (int) cx, (int) cy + 9);
		}
	}
}
