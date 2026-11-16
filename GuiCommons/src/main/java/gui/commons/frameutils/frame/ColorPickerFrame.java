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
package gui.commons.frameutils.frame;

import various.common.light.i18n.I18n;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.commons.frameutils.frame.arch.ToolIcons;
import gui.commons.frameutils.frame.arch.ToolStyle;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import gui.commons.handlers.AlwaysOnTopManager;
import gui.commons.mouseutils.MousePointerCaller;
import gui.commons.mouseutils.MousePointerUtils;
import net.miginfocom.swing.MigLayout;
import resources.IconsPathConfigurator;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.dialogs.color.ColorDialogHelper;
import various.common.light.utility.KeyListenerUtils;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.manipulation.ImageWorker;
import various.common.light.utility.reflection.ReflectionUtils;

/**
 * Small always on top color picker: picks the color of any point clicked in the application (or of any point of
 * the screen with Ctrl+Shift), shows it as RGB, HEX, HSB and HSL (all editable, Up/Down change the values), copies
 * each format with one click and keeps the recent colors of the session.
 */
public class ColorPickerFrame extends ParentFrame implements MousePointerCaller {
	private static final long serialVersionUID = -8518015676595593365L;

	static Logger logger = Logger.getLogger(ColorPickerFrame.class);

	public static ColorPickerFrame singletonInstance = null;

	public static ColorDialogHelper colorDialogHelper = new ColorDialogHelper();

	private static final int HISTORY_SIZE = 16;
	/** Recent colors of this session, newest first (shared by the windows opened later) */
	private static final List<Color> HISTORY = new ArrayList<>();

	// GUI ELEMENTS
	private Swatch swatch;
	private JToggleButton pickToggle;
	private JLabel contrastLabel;
	private ToolStyle.Badge statusBadge;
	private JPanel historyPanel;
	private JTextField txtRgb;
	private JTextField txtHex;
	private JTextField txtHsb;
	private JTextField txtHsl;
	private final Timer statusTimer;
	/** Picks with Ctrl+Shift come in bursts while the mouse moves: only the color where it stops goes in the recent ones */
	private final Timer rememberTimer;
	private Color pendingRemember;

	// INSTANCE LOGIC FIELDS
	public boolean active = false;
	public volatile AtomicBoolean hasFocus = new AtomicBoolean(false);
	private MousePointerUtils pointerUtils = null;
	private Color current = Color.GRAY;

	public JFrame caller;

	public ColorPickerFrame(JFrame caller) {

		colorDialogHelper.colorChangedAction = (color) -> showColor(color, null);

		this.caller = caller != null ? caller : new JFrame();
		ApplyCursorSUbElements(GuiUtilsExtended.CURSOR_CROSS, this.caller);

		active = true;
		singletonInstance = this;

		setTitle(I18n.t("gui.color_picker"));
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_COLOR_SAMPLER_BIG, this);

		statusTimer = new Timer(2200, e -> statusBadge.setStatus(" ", null, null));
		statusTimer.setRepeats(false);
		rememberTimer = new Timer(600, e -> remember(pendingRemember));
		rememberTimer.setRepeats(false);

		initializeGui();

		pointerUtils = new MousePointerUtils(getOnClickAction(), this, this);
		setPicking(true);

		addHandlers();

		// on top of every window, also of other applications, whatever the pin of the main window
		AlwaysOnTopManager.keepOnTop(this, true);
		hasFocus.set(false);
		showColor(ImageWorker.randomColorRGB(1.0f), null);
		refreshHistory();

		pack();
		setMinimumSize(getMinimumDimension());
		GuiUtils.centerComponent(this, getDefaultDimension());

		SwingUtilities.invokeLater(() -> {
			setVisible(true);
			resizeToDefault(false, true, false);
		});
	}

	/**
	 * This is the main action to trigger when listener is added and active, and user clicks with mouse
	 */
	public Runnable getOnClickAction() {
		return () -> {
			if (pointerUtils != null) {
				Color picked = pointerUtils.getCurrentLoadedPXColor();
				if (picked != null) {
					SwingUtilities.invokeLater(() -> {
						showColor(picked, null);
						pendingRemember = picked;
						rememberTimer.restart();
					});
				}
			}
		};
	}

	// ---------------------------------------------------------------------------------------------------------------
	// GUI
	// ---------------------------------------------------------------------------------------------------------------

	public void initializeGui() {

		JPanel content = new JPanel(new MigLayout("insets 12 14 12 14, gap 12 8, hidemode 3", "[][grow,fill]", "[][][]"));

		// COLOR SAMPLE
		swatch = new Swatch();
		content.add(swatch, "spany 2, w 112!, h 84!, aligny top");

		// TOP BAR: picking switch and hint
		JPanel topBar = new JPanel(new MigLayout("insets 0, gap 10 4, fillx", "[][grow,fill]", "[][]"));
		topBar.setOpaque(false);
		pickToggle = new JToggleButton();
		pickToggle.setIcon(ToolIcons.of(ToolIcons.Kind.EYEDROPPER, 16, Color.WHITE));
		pickToggle.setIconTextGap(6);
		pickToggle.setFocusable(false);
		pickToggle.setToolTipText(I18n.t("gui.turn_color_picking_on_or_off"));
		ToolStyle.accent(pickToggle, ToolStyle.BLUE);
		JLabel hint = ToolStyle.mutedLabel(I18n.t("gui.click_anywhere_in_screept_or_hold"));
		statusBadge = ToolStyle.badge();
		statusBadge.setStatus(" ", null, null);
		contrastLabel = ToolStyle.mutedLabel(" ");
		contrastLabel.setToolTipText(I18n.t("gui.contrast_ratio_of_white_and_black"));
		topBar.add(pickToggle);
		topBar.add(hint, "wmin 0, wrap");
		topBar.add(contrastLabel, "span 2, split 2, wmin 0");
		topBar.add(statusBadge, "gapleft push");
		content.add(topBar, "growx, wrap");

		// FORMATS
		JPanel formats = new JPanel(new MigLayout("insets 0, gap 8, fillx", "[grow,fill,sg chip][grow,fill,sg chip][grow,fill,sg chip][grow,fill,sg chip]"));
		formats.setOpaque(false);
		txtRgb = field("Red, green, blue (0-255): type a value or use the Up/Down arrows");
		txtHex = field("Hexadecimal #rrggbb: type a value or use the Up/Down arrows");
		txtHsb = field("Hue, saturation, brightness (0-1): type a value or use the Up/Down arrows");
		txtHsl = field("Hue (0-360), saturation and lightness (0-100): type a value or use the Up/Down arrows");
		formats.add(chip("RGB", txtRgb));
		formats.add(chip("HEX", txtHex));
		formats.add(chip("HSB / HSV", txtHsb));
		formats.add(chip("HSL", txtHsl));
		content.add(formats, "newline, span 2, growx");

		// RECENT COLORS
		JPanel recent = new JPanel(new MigLayout("insets 0, gap 8", "[][grow,fill][]"));
		recent.setOpaque(false);
		historyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		historyPanel.setOpaque(false);
		JButton clearHistory = ToolStyle.iconButton(ToolIcons.Kind.CLEAR, I18n.t("gui.clear_the_recent_colors"));
		clearHistory.addActionListener(e -> {
			synchronized (HISTORY) {
				HISTORY.clear();
			}
			refreshHistory();
		});
		recent.add(ToolStyle.caption("Recent"));
		recent.add(historyPanel, "wmin 0");
		recent.add(clearHistory);
		content.add(recent, "newline, span 2, growx");

		// the window fits its content; scroll bars only when it is made smaller than that
		JScrollPane scroll = new JScrollPane(content);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getHorizontalScrollBar().setUnitIncrement(16);
		setContentPane(scroll);
	}

	private static JTextField field(String tooltip) {
		JTextField field = new JTextField(9);
		field.setFont(ToolStyle.mono(2f));
		field.setHorizontalAlignment(SwingConstants.CENTER);
		field.putClientProperty("JComponent.roundRect", Boolean.TRUE);
		field.setToolTipText(tooltip);
		return field;
	}

	/** Rounded card with the format name, its copy button and the value */
	private JPanel chip(String name, JTextField field) {
		JPanel chip = ToolStyle.card(new MigLayout("insets 0, gap 4 4, fillx", "[grow][]", "[][]"));
		JLabel caption = ToolStyle.caption(name);
		caption.setForeground(ToolStyle.muted());
		JButton copy = ToolStyle.iconButton(ToolIcons.Kind.COPY, I18n.t("common.copy"));
		copy.setMargin(new Insets(1, 1, 1, 1));
		copy.addActionListener(e -> copy(field.getText().trim()));
		chip.add(caption, "aligny center");
		chip.add(copy, "wrap");
		chip.add(field, "span 2, growx, wmin 70");
		return chip;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// LOGIC
	// ---------------------------------------------------------------------------------------------------------------

	/** Shows the color everywhere, except in the field the user is typing in */
	private void showColor(Color color, JTextField skip) {
		if (color == null) {
			return;
		}
		current = new Color(color.getRGB() & 0xFFFFFF);
		setIfNot(txtRgb, skip, ConversionUtils.getRgbString(current, ","));
		setIfNot(txtHex, skip, ConversionUtils.getHexString(current));
		setIfNot(txtHsb, skip, ConversionUtils.getHsbString(current, ":"));
		setIfNot(txtHsl, skip, ConversionUtils.getHslString(current, ":"));
		swatch.setBackground(current);
		swatch.repaint();
		contrastLabel.setText(String.format(Locale.ROOT, "White text %.1f  ·  Black text %.1f",
				ToolStyle.contrast(current, Color.WHITE), ToolStyle.contrast(current, Color.BLACK)));
	}

	private static void setIfNot(JTextField field, JTextField skip, String text) {
		if (field != skip && !text.equals(field.getText())) {
			field.setText(text);
			field.putClientProperty("JComponent.outline", null);
		}
	}

	/** Parses the typed value: a valid one updates the color, an invalid one only marks the field */
	private void typed(JTextField field, Function<String, Color> parser) {
		if (!field.hasFocus()) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			Color parsed = null;
			try {
				parsed = parser.apply(field.getText().replaceAll("\\s", ""));
			} catch (RuntimeException e) {
				// incomplete or wrong value while typing
			}
			field.putClientProperty("JComponent.outline", parsed == null ? "error" : null);
			if (parsed != null) {
				showColor(parsed, field);
			}
		});
	}

	private void copy(String text) {
		if (text.isEmpty()) {
			return;
		}
		ToolStyle.copyToClipboard(text);
		remember(current);
		statusBadge.setStatus("Copied: " + text, ToolIcons.of(ToolIcons.Kind.CHECK, 14, Color.WHITE), ToolStyle.GREEN);
		statusTimer.restart();
	}

	private void setPicking(boolean on) {
		pointerUtils.enabled.set(on);
		pickToggle.setSelected(on);
		pickToggle.setText(on ? "Picking ON" : "Picking OFF");
		if (on) {
			ToolStyle.accent(pickToggle, ToolStyle.BLUE);
			pickToggle.setIcon(ToolIcons.of(ToolIcons.Kind.EYEDROPPER, 16, Color.WHITE));
		} else {
			pickToggle.setBackground(UIManager.getColor("Button.background"));
			pickToggle.setForeground(UIManager.getColor("Button.foreground"));
			pickToggle.setIcon(ToolIcons.of(ToolIcons.Kind.EYEDROPPER, 16));
		}
		ApplyCursorSUbElements(on ? GuiUtilsExtended.CURSOR_CROSS : GuiUtilsExtended.CURSOR_DEFAULT, caller);
	}

	/** Adds the color on top of the recent ones (no duplicates) */
	private void remember(Color color) {
		if (color == null) {
			return;
		}
		Color solid = new Color(color.getRGB() & 0xFFFFFF);
		synchronized (HISTORY) {
			if (!HISTORY.isEmpty() && HISTORY.get(0).equals(solid)) {
				return;
			}
			HISTORY.remove(solid);
			HISTORY.add(0, solid);
			while (HISTORY.size() > HISTORY_SIZE) {
				HISTORY.remove(HISTORY.size() - 1);
			}
		}
		refreshHistory();
	}

	private void refreshHistory() {
		historyPanel.removeAll();
		List<Color> colors;
		synchronized (HISTORY) {
			colors = new ArrayList<>(HISTORY);
		}
		if (colors.isEmpty()) {
			historyPanel.add(ToolStyle.mutedLabel(I18n.t("gui.picked_colors_appear_here")));
		}
		for (Color color : colors) {
			historyPanel.add(new HistoryDot(color));
		}
		historyPanel.revalidate();
		historyPanel.repaint();
	}

	public void addHandlers() {

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				active = false;
				logger.debug("Closing " + getClass().getName());
				statusTimer.stop();
				rememberTimer.stop();
				pointerUtils.removeMouseListener();
				ApplyCursorSUbElements(GuiUtilsExtended.CURSOR_DEFAULT, caller);
				singletonInstance = null;
				dispose();
			}
		});

		pickToggle.addActionListener(e -> setPicking(pickToggle.isSelected()));

		listen(txtRgb, text -> ConversionUtils.getColorFromRgbString(text, ","));
		txtRgb.addKeyListener(KeyListenerUtils.getKeyListenerIntTxtField(txtRgb, ",", 1, 0, 255));

		listen(txtHex, text -> {
			if (!text.matches("#?[0-9a-fA-F]{6}")) {
				return null;
			}
			return ConversionUtils.getColorFromHexString(text, null);
		});
		txtHex.addKeyListener(KeyListenerUtils.getKeyListenerHexTxtField(txtHex, ",", 1000, 0, 0xFFFFFF));

		listen(txtHsb, text -> ConversionUtils.getColorFromHsbString(text, ":", null));
		txtHsb.addKeyListener(KeyListenerUtils.getKeyListenerFloatTxtField(txtHsb, ":", 0.01f, 0, 1.0f));

		listen(txtHsl, text -> ConversionUtils.getColorFromHslString(text, ":", null));
		txtHsl.addKeyListener(KeyListenerUtils.getKeyListenerIntTxtField(txtHsl, ":", 1, 0, new int[] {360, 100, 100}));

		swatch.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				boolean oldEnabled = pointerUtils.enabled.get();
				pointerUtils.enabled.set(false);

				Color selected = colorDialogHelper.askForColor(swatch, false);
				if (selected != null) {
					showColor(selected, null);
					remember(selected);
				} else {
					showColor(current, null);
				}

				GuiUtils.launchThreadSafeSwing(() -> {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						Thread.currentThread().interrupt();
					}
					pointerUtils.enabled.set(oldEnabled && pickToggle.isSelected());
				});
			}
		});
	}

	private void listen(JTextField field, Function<String, Color> parser) {
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				typed(field, parser);
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				typed(field, parser);
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				typed(field, parser);
			}
		});
	}

	public void ApplyCursorSUbElements(Cursor cursor, Component instance) {
		instance.setCursor(cursor);
		try {
			List<JScrollPane> resultSet2 = ReflectionUtils.retrieveFieldsByType(instance, JScrollPane.class);
			for (JScrollPane curr : resultSet2) {
				curr.setCursor(cursor);
			}
			List<JTextArea> resultSet = ReflectionUtils.retrieveFieldsByType(instance, JTextArea.class);
			for (JTextArea curr : resultSet) {
				curr.setCursor(cursor);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.warn("Error trying to update cursors with reflection");
		}
	}

	/** Content size plus the window borders, never wider/taller than the screen */
	@Override
	public Dimension getDefaultDimension() {
		Dimension content = getContentPane() != null ? getContentPane().getPreferredSize() : new Dimension(680, 220);
		Insets insets = isDisplayable() ? getInsets() : new Insets(32, 8, 8, 8);
		Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		return new Dimension(
				Math.min(screen.width - 40, Math.max(640, content.width + insets.left + insets.right + 4)),
				Math.min(screen.height - 80, content.height + insets.top + insets.bottom + 4));
	}

	@Override
	public Dimension getMinimumDimension() {
		return new Dimension(360, 160);
	}

	public static ColorPickerFrame getInstance(JFrame parentFrame) {
		return singletonInstance == null ? new ColorPickerFrame(parentFrame) : singletonInstance;
	}

	@Override
	public List<Component> getDisabledElements() {
		return Arrays.asList(this.getComponents());
	}

	// ---------------------------------------------------------------------------------------------------------------

	/** Big rounded sample of the current color (its background) with the hex code in a readable color */
	private static final class Swatch extends JComponent {
		private static final long serialVersionUID = 1L;

		Swatch() {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(I18n.t("gui.click_to_choose_the_color_with"));
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				Color color = getBackground() != null ? getBackground() : Color.GRAY;
				int w = getWidth() - 1;
				int h = getHeight() - 1;
				int arc = 22;
				g.setColor(color);
				g.fillRoundRect(0, 0, w, h, arc, arc);
				// soft light on top: a bit of depth, not a flat square
				g.setPaint(new java.awt.GradientPaint(0, 0, new Color(255, 255, 255, 46), 0, h / 2f, new Color(255, 255, 255, 0)));
				g.fillRoundRect(0, 0, w, h, arc, arc);
				Color border = UIManager.getColor("Component.borderColor");
				g.setColor(border != null ? border : Color.GRAY);
				g.setStroke(new BasicStroke(1f));
				g.drawRoundRect(0, 0, w, h, arc, arc);

				Color text = ToolStyle.contrast(color, Color.WHITE) >= ToolStyle.contrast(color, Color.BLACK) ? Color.WHITE : Color.BLACK;
				g.setColor(text);
				g.setFont(ToolStyle.mono(1f).deriveFont(Font.BOLD));
				String hex = ConversionUtils.getHexString(color).toUpperCase(Locale.ROOT);
				FontMetrics metrics = g.getFontMetrics();
				g.drawString(hex, (getWidth() - metrics.stringWidth(hex)) / 2, getHeight() - 10 - metrics.getDescent());
			} finally {
				g.dispose();
			}
		}
	}

	/** Round recent color: click to use it again */
	private final class HistoryDot extends JComponent {
		private static final long serialVersionUID = 1L;
		private static final int SIZE = 22;
		private final Color color;
		private boolean hover;

		HistoryDot(Color color) {
			this.color = color;
			setPreferredSize(new Dimension(SIZE + 2, SIZE + 2));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(ConversionUtils.getHexString(color).toUpperCase(Locale.ROOT) + "  (" + ConversionUtils.getRgbString(color, ",") + ")");
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					showColor(HistoryDot.this.color, null);
				}
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
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(color);
				g.fillOval(1, 1, SIZE - 1, SIZE - 1);
				Color ring = hover ? ToolStyle.BLUE : UIManager.getColor("Component.borderColor");
				g.setColor(ring != null ? ring : Color.GRAY);
				g.setStroke(new BasicStroke(hover ? 2f : 1f));
				g.drawOval(1, 1, SIZE - 1, SIZE - 1);
			} finally {
				g.dispose();
			}
		}
	}
}
