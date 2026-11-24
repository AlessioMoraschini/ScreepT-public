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

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

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

	private static final List<ViewerFullScreen> ACTIVE = new ArrayList<ViewerFullScreen>();
	/** Side of the top right corner where the X shows up when the mouse comes close (it would cover the buttons of the viewer) */
	private static final int HOT_W = 260;
	private static final int HOT_H = 110;

	/**
	 * Full screen on/off for a viewer (tab or window): if the component, or one that contains it or is inside it, is already full
	 * screen it goes back to its place, otherwise it is shown full screen on the screen of its window. Safe to call from many buttons.
	 */
	public static void toggle(JComponent component) {
		toggle(component, null);
	}

	/** As {@link #toggle(JComponent)}; afterBack runs when the component is back in its place */
	public static void toggle(JComponent component, Runnable afterBack) {
		for (ViewerFullScreen view : new ArrayList<ViewerFullScreen>(ACTIVE)) {
			if (view.covers(component)) {
				view.dispose();
				return;
			}
		}
		final Container parent = component.getParent();
		final Window owner = SwingUtilities.getWindowAncestor(component);
		if (parent == null || owner == null) {
			return;
		}
		final JRootPane root = SwingUtilities.getRootPane(component);
		final boolean isContentPane = root != null && root.getContentPane() == component;
		final Object constraints = parent.getLayout() instanceof BorderLayout ? ((BorderLayout) parent.getLayout()).getConstraints(component) : null;
		final int index = parent.getComponentZOrder(component);
		if (isContentPane) {
			root.setContentPane(new JPanel()); // the window keeps an empty content while the real one is full screen
		} else {
			parent.remove(component);
			parent.revalidate();
			parent.repaint();
		}
		ViewerFullScreen opened = open(owner, component, () -> {
			if (isContentPane) {
				root.setContentPane((Container) component);
			} else {
				if (constraints != null) {
					parent.add(component, constraints, index);
				} else {
					parent.add(component, index);
				}
				parent.revalidate();
				parent.repaint();
			}
			SwingUtilities.invokeLater(() -> {
				component.requestFocusInWindow();
				if (afterBack != null) {
					afterBack.run();
				}
			});
		});
		opened.origin = parent;
	}

	/** True if the component is this full screen content, contains it, or is in the place it came from (any button of that viewer closes it) */
	private boolean covers(JComponent c) {
		return SwingUtilities.isDescendingFrom(c, content) || SwingUtilities.isDescendingFrom(content, c)
				|| (origin != null && (SwingUtilities.isDescendingFrom(c, origin) || SwingUtilities.isDescendingFrom(origin, c)));
	}

	/** True if the component (or a part of it) is full screen now */
	public static boolean isActive(JComponent component) {
		for (ViewerFullScreen view : ACTIVE) {
			if (view.covers(component)) {
				return true;
			}
		}
		return false;
	}

	/** Leaves the full screen of the component if it is in it */
	public static void exit(JComponent component) {
		if (isActive(component)) {
			toggle(component);
		}
	}

	/** F11 on the component (valid when its window is the active one) toggles the full screen */
	public static void installKey(final JComponent component) {
		component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "screept.viewerFullScreen");
		component.getActionMap().put("screept.viewerFullScreen", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				toggle(component);
			}
		});
	}

	private final JComponent content;
	private final Runnable onClosed;
	private final CloseButton btnClose = new CloseButton();
	private final KeyEventDispatcher exitKeys;
	private final AWTEventListener cornerWatcher;
	private final Timer hideTimer;
	private Container origin;
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

		// the X covers the top right of the viewer: it shows for a moment, then only when the mouse is in that corner
		hideTimer = new Timer(2500, e -> btnClose.setVisible(false));
		hideTimer.setRepeats(false);
		cornerWatcher = ev -> {
			if (ev instanceof java.awt.event.MouseEvent && ev.getSource() instanceof Component
					&& SwingUtilities.getWindowAncestor((Component) ev.getSource()) == this) {
				java.awt.Point p = SwingUtilities.convertPoint((Component) ev.getSource(), ((java.awt.event.MouseEvent) ev).getPoint(), getContentPane());
				boolean near = p.x > getWidth() - HOT_W && p.y < HOT_H;
				if (near != btnClose.isVisible()) {
					btnClose.setVisible(near);
				}
				if (near) {
					hideTimer.stop();
				}
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(cornerWatcher, AWTEvent.MOUSE_MOTION_EVENT_MASK);
		hideTimer.start();
		ACTIVE.add(this);
	}

	@Override
	public void dispose() {
		if (!closed) {
			closed = true;
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(exitKeys);
			Toolkit.getDefaultToolkit().removeAWTEventListener(cornerWatcher);
			hideTimer.stop();
			ACTIVE.remove(this);
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
