package plugin.external.root.texteditor;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;

import org.jmeld.settings.EditorSettings;
import org.jmeld.settings.JMeldSettings;
import org.jmeld.ui.JMeldPanel;
import org.jmeld.ui.util.FontUtil;

/**
 * Text size (zoom) of the JMeld editors:
 * - Ctrl + mouse wheel (Cmd + wheel on macOS)
 * - Ctrl/Cmd and + / - / 0 (reset)
 * - a compact control in the top toolbar: minus button, slider, plus button, percentage (click = reset)
 *
 * The zoom is a percentage of the default editor font, remembered between sessions.
 */
public final class JMeldZoomSupport {

	public static final int MIN_PERCENT = 50;
	public static final int MAX_PERCENT = 400;
	public static final int STEP_PERCENT = 10;
	public static final int DEFAULT_PERCENT = 100;

	private static final boolean MAC = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");
	private static final String SHORTCUT_NAME = MAC ? "Cmd" : "Ctrl";
	private static final String BAR_PROPERTY = "screept.jmeld.zoombar";

	private static final String PREF_NODE = "am-design-development/screept/codecompare";
	private static final String PREF_KEY = "zoomPercent";

	private static final Set<Window> zoomFrames = Collections.newSetFromMap(new WeakHashMap<Window, Boolean>());
	private static final Set<ZoomBar> zoomBars = Collections.newSetFromMap(new WeakHashMap<ZoomBar, Boolean>());

	private static boolean hasSavedPercent = false;
	private static int percent = loadPercent();
	private static double wheelAccumulator = 0;
	private static boolean wheelHookInstalled = false;

	private JMeldZoomSupport() {
	}

	// ---------------------------------------------------------------- public API

	/**
	 * Adds the zoom behaviour (wheel, keys, toolbar control) to a JMeld frame. Call on the EDT, before the comparison is opened.
	 */
	public static void install(final JFrame frame, final JMeldPanel panel) {
		JMeldThemeSupport.adaptDiffColors();
		zoomFrames.add(frame);
		installWheelHook();
		installKeys(frame);

		injectToolbarControl(panel);
		panel.addContainerListener(new ContainerAdapter() {
			@Override
			public void componentAdded(ContainerEvent e) {
				// JMeld rebuilds its toolbar when the toolbar option is toggled
				if (e.getChild() instanceof JToolBar) {
					injectToolbarControl(panel);
				}
			}
		});

		if (hasSavedPercent) {
			applyToEditors();
		}
		refreshBars();
	}

	public static int getPercent() {
		return percent;
	}

	public static void zoomIn() {
		setPercent(percent + STEP_PERCENT);
	}

	public static void zoomOut() {
		setPercent(percent - STEP_PERCENT);
	}

	public static void reset() {
		setPercent(DEFAULT_PERCENT);
	}

	public static void setPercent(int newPercent) {
		int clamped = Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, newPercent));
		percent = clamped;
		hasSavedPercent = true;
		savePercent();
		applyKeepingPosition();
		refreshBars();
	}

	// ---------------------------------------------------------------- font handling

	/**
	 * JMeld jumps to the first difference whenever the font changes (and it replaces its editors):
	 * the text offset at the top of every editor is remembered and brought back afterwards, matching
	 * the editors by their order in the window, so zooming never loses the reading position.
	 */
	private static void applyKeepingPosition() {
		final java.util.List<Integer> offsets = new java.util.ArrayList<Integer>();
		try {
			for (Window window : zoomFrames.toArray(new Window[0])) {
				for (Object[] entry : findEditors(window)) {
					javax.swing.JViewport viewport = (javax.swing.JViewport) entry[0];
					javax.swing.text.JTextComponent text = (javax.swing.text.JTextComponent) entry[1];
					offsets.add(Integer.valueOf(text.viewToModel(new java.awt.Point(0, viewport.getViewPosition().y))));
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			offsets.clear();
		}

		applyToEditors();

		if (offsets.isEmpty()) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				restoreOffsets(offsets);
			}
		});
		// JMeld may scroll again a moment after the font change
		javax.swing.Timer later = new javax.swing.Timer(80, new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				restoreOffsets(offsets);
			}
		});
		later.setRepeats(false);
		later.start();
	}

	/** Visible text editors inside a scroll viewport: {viewport, text component}, in hierarchy order. */
	private static java.util.List<Object[]> findEditors(Component root) {
		java.util.List<Object[]> found = new java.util.ArrayList<Object[]>();
		collectEditors(root, found);
		return found;
	}

	private static void collectEditors(Component component, java.util.List<Object[]> found) {
		if (component instanceof javax.swing.text.JTextComponent && component.isShowing()) {
			javax.swing.JViewport viewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(javax.swing.JViewport.class, component);
			if (viewport != null) {
				found.add(new Object[] { viewport, component });
			}
		}
		if (component instanceof java.awt.Container) {
			for (Component child : ((java.awt.Container) component).getComponents()) {
				collectEditors(child, found);
			}
		}
	}

	private static void restoreOffsets(java.util.List<Integer> offsets) {
		try {
			int index = 0;
			for (Window window : zoomFrames.toArray(new Window[0])) {
				for (Object[] entry : findEditors(window)) {
					if (index >= offsets.size()) {
						return;
					}
					javax.swing.JViewport viewport = (javax.swing.JViewport) entry[0];
					javax.swing.text.JTextComponent text = (javax.swing.text.JTextComponent) entry[1];
					int offset = Math.min(offsets.get(index++).intValue(), text.getDocument().getLength());
					java.awt.Rectangle line = text.modelToView(offset);
					java.awt.Point current = viewport.getViewPosition();
					if (line != null && current.y != line.y) {
						viewport.setViewPosition(new java.awt.Point(current.x, line.y));
					}
				}
			}
		} catch (Throwable t) {
			// position not restored, nothing else to do
		}
	}

	private static void applyToEditors() {
		try {
			EditorSettings editor = JMeldSettings.getInstance().getEditor();
			Font base = FontUtil.defaultTextAreaFont;
			boolean custom = editor.isCustomFontEnabled() && editor.getFont() != null;
			Font current = custom ? editor.getFont() : base;

			float size = Math.max(6f, Math.round(base.getSize2D() * percent / 100f));
			if (custom && Math.abs(current.getSize2D() - size) < 0.01f) {
				return;
			}
			if (!custom && percent == DEFAULT_PERCENT) {
				return;
			}

			// the settings fire a change: every open editor re-reads its font
			editor.enableCustomFont(true);
			editor.setFont(current.deriveFont(size));
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	// ---------------------------------------------------------------- persistence

	private static int loadPercent() {
		try {
			String stored = Preferences.userRoot().node(PREF_NODE).get(PREF_KEY, null);
			if (stored != null) {
				hasSavedPercent = true;
				return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, Integer.parseInt(stored.trim())));
			}
		} catch (Throwable t) {
			// preferences not available: default zoom
		}
		return DEFAULT_PERCENT;
	}

	private static void savePercent() {
		try {
			Preferences.userRoot().node(PREF_NODE).put(PREF_KEY, String.valueOf(percent));
		} catch (Throwable t) {
			// not persisted, still works for this session
		}
	}

	// ---------------------------------------------------------------- mouse wheel

	private static void installWheelHook() {
		if (wheelHookInstalled) {
			return;
		}
		wheelHookInstalled = true;

		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				if (!(event instanceof MouseWheelEvent)) {
					return;
				}
				MouseWheelEvent wheel = (MouseWheelEvent) event;
				if (wheel.isConsumed() || !isZoomModifierDown(wheel)) {
					return;
				}

				Component source = wheel.getComponent();
				Window window = source instanceof Window ? (Window) source : SwingUtilities.getWindowAncestor(source);
				if (window == null || !zoomFrames.contains(window)) {
					return;
				}

				wheelAccumulator += wheel.getPreciseWheelRotation();
				int steps = (int) wheelAccumulator;
				if (steps != 0) {
					wheelAccumulator -= steps;
					setPercent(percent - steps * STEP_PERCENT);
				}

				// the wheel must not scroll the text while zooming
				final JScrollPane scrollPane = source instanceof JScrollPane ? (JScrollPane) source
						: (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, source);
				if (scrollPane != null && scrollPane.isWheelScrollingEnabled()) {
					scrollPane.setWheelScrollingEnabled(false);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							scrollPane.setWheelScrollingEnabled(true);
						}
					});
				}
				wheel.consume();
			}
		}, AWTEvent.MOUSE_WHEEL_EVENT_MASK);
	}

	private static boolean isZoomModifierDown(InputEvent e) {
		return e.isControlDown() || (MAC && e.isMetaDown());
	}

	// ---------------------------------------------------------------- keyboard

	private static void installKeys(JFrame frame) {
		JComponent root = frame.getRootPane();
		int[] masks = MAC ? new int[] { InputEvent.META_DOWN_MASK, InputEvent.CTRL_DOWN_MASK } : new int[] { InputEvent.CTRL_DOWN_MASK };

		for (int mask : masks) {
			bind(root, "jmeld-zoom-in", mask, new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					zoomIn();
				}
			}, KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS, KeyEvent.VK_ADD);

			bind(root, "jmeld-zoom-out", mask, new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					zoomOut();
				}
			}, KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT);

			bind(root, "jmeld-zoom-reset", mask, new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					reset();
				}
			}, KeyEvent.VK_0, KeyEvent.VK_NUMPAD0);
		}
	}

	private static void bind(JComponent root, String actionKey, int mask, AbstractAction action, int... keyCodes) {
		for (int keyCode : keyCodes) {
			root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, mask), actionKey);
		}
		root.getActionMap().put(actionKey, action);
	}

	// ---------------------------------------------------------------- toolbar control

	private static void injectToolbarControl(JMeldPanel panel) {
		for (Component child : panel.getComponents()) {
			if (child instanceof JToolBar) {
				JToolBar toolBar = (JToolBar) child;
				if (toolBar.getClientProperty(BAR_PROPERTY) != null) {
					return;
				}
				try {
					ZoomBar bar = new ZoomBar();

					if (toolBar.getLayout() instanceof FormLayout) {
						// "Blank" compare button right after "New" (column 1)
						((FormLayout) toolBar.getLayout()).insertColumn(2, new ColumnSpec("pref:none"));
						toolBar.add(JMeldBlankCompare.createButton(panel), new CellConstraints(2, 1));
					}
					int count = toolBar.getComponentCount();

					if (toolBar.getLayout() instanceof FormLayout) {
						// the JMeld toolbar is a FormLayout with one column per component: a new column goes
						// right before the last three buttons (settings, help, about), after the flexible space
						int column = Math.max(1, count - 2);
						FormLayout formLayout = (FormLayout) toolBar.getLayout();
						formLayout.insertColumn(column, new ColumnSpec("pref:none"));
						toolBar.add(bar.component, new CellConstraints(column, 1));
					} else {
						toolBar.add(bar.component, Math.max(0, count - 3));
					}

					zoomBars.add(bar);
					toolBar.putClientProperty(BAR_PROPERTY, bar);
					toolBar.revalidate();
					toolBar.repaint();
				} catch (Throwable t) {
					// the zoom control is optional: JMeld must keep working without it
					t.printStackTrace();
				}
				return;
			}
		}
	}

	private static void refreshBars() {
		if (SwingUtilities.isEventDispatchThread()) {
			for (ZoomBar bar : zoomBars.toArray(new ZoomBar[0])) {
				bar.refresh();
			}
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					refreshBars();
				}
			});
		}
	}

	private static final class ZoomBar {

		final JComponent component;
		private final JSlider slider;
		private final JLabel valueLabel;
		private boolean updating = false;

		ZoomBar() {
			String hint = "Text size: " + SHORTCUT_NAME + " + mouse wheel, " + SHORTCUT_NAME + " and + / -, " + SHORTCUT_NAME + " 0 to reset";

			JLabel caption = new JLabel("Text size");
			caption.setForeground(UIManager.getColor("Label.disabledForeground"));
			caption.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 6));

			JButton minus = createStepButton("−", "Smaller text", new Runnable() {
				@Override
				public void run() {
					zoomOut();
				}
			});
			JButton plus = createStepButton("+", "Larger text", new Runnable() {
				@Override
				public void run() {
					zoomIn();
				}
			});

			slider = new JSlider(MIN_PERCENT, MAX_PERCENT, percent);
			slider.setFocusable(false);
			slider.setOpaque(false);
			slider.setToolTipText(hint);
			Dimension sliderSize = new Dimension(120, slider.getPreferredSize().height);
			slider.setPreferredSize(sliderSize);
			slider.setMaximumSize(sliderSize);
			slider.addChangeListener(new javax.swing.event.ChangeListener() {
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					if (!updating) {
						setPercent(slider.getValue());
					}
				}
			});

			valueLabel = new JLabel(percent + "%", SwingConstants.RIGHT);
			valueLabel.setToolTipText("Click to reset to 100%");
			valueLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			Dimension valueSize = new Dimension(valueLabel.getFontMetrics(valueLabel.getFont()).stringWidth("400%") + 8,
					valueLabel.getPreferredSize().height);
			valueLabel.setPreferredSize(valueSize);
			valueLabel.setMaximumSize(valueSize);
			valueLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					reset();
				}
			});

			Box box = Box.createHorizontalBox();
			box.setToolTipText(hint);
			JSeparator divider = new JSeparator(SwingConstants.VERTICAL);
			Dimension dividerSize = new Dimension(8, 20);
			divider.setPreferredSize(dividerSize);
			divider.setMaximumSize(dividerSize);
			box.add(divider);
			box.add(Box.createHorizontalStrut(6));
			box.add(caption);
			box.add(minus);
			box.add(slider);
			box.add(plus);
			box.add(Box.createHorizontalStrut(4));
			box.add(valueLabel);
			box.add(Box.createHorizontalStrut(8));
			box.setMaximumSize(box.getPreferredSize());
			component = box;
		}

		private JButton createStepButton(String text, String tooltip, final Runnable action) {
			JButton button = new JButton(text);
			button.setToolTipText(tooltip);
			button.setFocusable(false);
			button.putClientProperty("JButton.buttonType", "toolBarButton");
			button.setMargin(new java.awt.Insets(0, 6, 0, 6));
			button.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					action.run();
				}
			});
			return button;
		}

		void refresh() {
			updating = true;
			try {
				slider.setValue(percent);
				valueLabel.setText(percent + "%");
			} finally {
				updating = false;
			}
		}
	}
}
