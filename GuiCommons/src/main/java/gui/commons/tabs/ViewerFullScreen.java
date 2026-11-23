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
package gui.commons.tabs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import various.common.light.i18n.I18n;

/**
 * Full screen window for the component of a viewer tab (PDF, Markdown...). The very same component is moved here (page, zoom and
 * scroll position stay) and given back to the tab on exit. A round X at the top right, Esc and F11 leave the full screen.
 */
public final class ViewerFullScreen extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final int MARGIN = 16;
	private static final int BUTTON = 36;
	/** Space kept free at the right so the X does not cover the scrollbar of the content */
	private static final int SCROLLBAR_GAP = 18;

	private final JComponent content;
	private final Runnable onClosed;
	private final CloseButton btnClose = new CloseButton();
	private final KeyEventDispatcher exitKeys;
	private boolean closed;

	/** Shows the component full screen on the screen of the owner window; onClosed gives it back to its place (called once) */
	public static ViewerFullScreen open(Window owner, JComponent content, Runnable onClosed) {
		ViewerFullScreen view = new ViewerFullScreen(owner, content, onClosed);
		view.setVisible(true);
		view.toFront();
		content.requestFocusInWindow();
		return view;
	}

	private ViewerFullScreen(Window owner, JComponent content, Runnable onClosed) {
		super(owner);
		this.content = content;
		this.onClosed = onClosed;
		setUndecorated(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		GraphicsConfiguration gc = owner != null && owner.getGraphicsConfiguration() != null ? owner.getGraphicsConfiguration()
				: GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		setBounds(gc.getBounds());

		JLayeredPane layers = new JLayeredPane() {
			private static final long serialVersionUID = 1L;

			@Override
			public void doLayout() {
				ViewerFullScreen.this.content.setBounds(0, 0, getWidth(), getHeight());
				btnClose.setBounds(getWidth() - MARGIN - SCROLLBAR_GAP - BUTTON, MARGIN, BUTTON, BUTTON);
			}
		};
		layers.add(content, JLayeredPane.DEFAULT_LAYER);
		layers.add(btnClose, JLayeredPane.MODAL_LAYER);
		setContentPane(layers);

		btnClose.setToolTipText(I18n.t("gui.fs_exit"));
		btnClose.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				dispose();
			}
		});

		// the viewers may use Esc themselves: the keys are caught before they reach the focused component
		exitKeys = ev -> {
			if (ev.getID() != KeyEvent.KEY_PRESSED || ev.getModifiersEx() != 0
					|| (ev.getKeyCode() != KeyEvent.VK_ESCAPE && ev.getKeyCode() != KeyEvent.VK_F11)) {
				return false;
			}
			boolean mine = ev.getSource() == this
					|| (ev.getSource() instanceof java.awt.Component && SwingUtilities.getWindowAncestor((java.awt.Component) ev.getSource()) == this);
			if (!mine || MenuSelectionManager.defaultManager().getSelectedPath().length > 0) {
				return false; // another window, or an open menu/popup that uses Esc to close itself
			}
			ev.consume();
			SwingUtilities.invokeLater(this::dispose);
			return true;
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(exitKeys);
	}

	@Override
	public void dispose() {
		if (!closed) {
			closed = true;
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(exitKeys);
			getContentPane().remove(content);
			try {
				if (onClosed != null) {
					onClosed.run();
				}
			} finally {
				super.dispose();
			}
		}
	}

	/** Round X drawn in code (no image files) */
	private static final class CloseButton extends JComponent {
		private static final long serialVersionUID = 1L;

		private boolean hover;

		CloseButton() {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setPreferredSize(new Dimension(BUTTON, BUTTON));
			addMouseListener(new MouseAdapter() {
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
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			int w = getWidth();
			int h = getHeight();
			g2.setColor(hover ? new Color(200, 60, 60, 235) : new Color(24, 26, 30, 215));
			g2.fillOval(0, 0, w - 1, h - 1);
			g2.setColor(new Color(255, 255, 255, 70));
			g2.drawOval(0, 0, w - 1, h - 1);
			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int pad = (int) (w * 0.33);
			g2.drawLine(pad, pad, w - 1 - pad, h - 1 - pad);
			g2.drawLine(w - 1 - pad, pad, pad, h - 1 - pad);
			g2.dispose();
		}
	}
}
