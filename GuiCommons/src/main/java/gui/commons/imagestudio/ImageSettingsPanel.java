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
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import gui.commons.frameutils.frame.arch.ToolStyle;
import net.miginfocom.swing.MigLayout;

/**
 * The right side of the Image Studio and of the batch window: an accordion that edits an {@link ImageSettings}
 * (convert and export, resize, adjustments, filters, transform). Sections and fine options appear according to the
 * {@link StudioMode}. Every change calls the change listener, which re-renders the preview.
 */
public class ImageSettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	/** Filters shown from the Classic mode; the Nerd mode shows all */
	private static final ImageEdit.Filter[] CLASSIC_FILTERS = {
			ImageEdit.Filter.AUTO_CONTRAST, ImageEdit.Filter.GRAYSCALE, ImageEdit.Filter.SEPIA, ImageEdit.Filter.VINTAGE,
			ImageEdit.Filter.WARM, ImageEdit.Filter.COOL, ImageEdit.Filter.SHARPEN, ImageEdit.Filter.BLUR, ImageEdit.Filter.VIGNETTE};

	private final ImageSettings settings;
	private final Runnable onChange;
	private final AccordionPanel accordion = new AccordionPanel();
	private final JScrollPane scroll;
	private boolean updating;

	// controls that follow the settings (refreshed by syncFromSettings)
	private JComboBox<String> comboFormat;
	private JSlider sliderQuality;
	private JLabel lblQuality;
	private JCheckBox chkProgressive;
	private JButton btnBackground;
	private JLabel lblEstimate;
	private JComboBox<ImageSettings.ResizeMode> comboResize;
	private JSpinner spinPercent;
	private JSpinner spinWidth;
	private JSpinner spinHeight;
	private JComboBox<ImageSettings.Resampling> comboResampling;
	private JLabel lblResizeResult;
	private JSlider sBrightness;
	private JSlider sContrast;
	private JSlider sSaturation;
	private JSlider sGamma;
	private JSlider sTemperature;
	private JSlider sHue;
	private final Map<ImageEdit.Filter, JToggleButton> filterButtons = new EnumMap<ImageEdit.Filter, JToggleButton>(ImageEdit.Filter.class);
	private JPanel nerdFilters;
	private int baseWidth = 0;
	private int baseHeight = 0;
	private final List<Runnable> modeRefreshers = new ArrayList<Runnable>();

	/** Studio use: the last session brings back the output choices (format, quality, resize), not the edits of one image */
	public ImageSettingsPanel(ImageSettings settings, Runnable onChange) {
		this(settings, onChange, false);
	}

	/**
	 * @param restoreEdits true (batch): the last used configuration comes back whole, with adjustments and filters
	 */
	public ImageSettingsPanel(ImageSettings settings, Runnable onChange, boolean restoreEdits) {
		super(new MigLayout("insets 0", "[grow,fill]", "[grow,fill]"));
		this.settings = settings;
		this.onChange = onChange;
		setOpaque(false);
		ImageSettingsStore.loadLast(settings, !restoreEdits);

		accordion.addSection("Convert & export", buildConvert(), StudioMode.BASIC, true);
		accordion.addSection("Resize", buildResize(), StudioMode.BASIC, true);
		accordion.addSection("Adjustments", buildAdjustments(), StudioMode.CLASSIC, true);
		accordion.addSection("Filters", buildFilters(), StudioMode.CLASSIC, true);
		accordion.addSection("Rotate & flip", buildTransform(), StudioMode.CLASSIC, false);
		accordion.addSection("Presets", buildPresets(), StudioMode.BASIC, false);

		JPanel holder = new JPanel(new MigLayout("insets 0 0 0 6", "[grow,fill]", ""));
		holder.setOpaque(false);
		holder.add(accordion, "growx");
		scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, "grow");
		applyMode(StudioMode.current());
		syncFromSettings();
		presetFingerprint = ImageSettingsStore.fingerprint(settings);
	}

	// ------------------------------------------------------------------ presets

	private File currentPreset;
	private String presetFingerprint = "";
	private JLabel lblPreset;

	private JPanel buildPresets() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 2, gap 4", "[grow,fill][grow,fill]", ""));
		p.setOpaque(false);
		lblPreset = ToolStyle.mutedLabel("No preset. The last settings are remembered automatically.");
		p.add(lblPreset, "span 2");
		JButton save = ToolStyle.button("Save", null, "Save the settings in the preset file (asks before overwriting)");
		save.addActionListener(e -> savePreset());
		JButton export = ToolStyle.button("Export...", null, "Save the settings to a file, to keep or share");
		export.addActionListener(e -> exportPreset());
		JButton imp = ToolStyle.button("Import...", null, "Load settings from a preset file");
		imp.addActionListener(e -> importPreset());
		JButton reset = ToolStyle.button("Reset all", null, "Every setting goes back to its default");
		reset.addActionListener(e -> resetAll());
		p.add(imp);
		p.add(export);
		p.add(save);
		p.add(reset);
		return p;
	}

	private void updatePresetLabel() {
		if (lblPreset == null) {
			return;
		}
		lblPreset.setText(currentPreset == null ? "No preset. The last settings are remembered automatically."
				: "Preset: " + currentPreset.getName() + (isPresetDirty() ? " (changed)" : ""));
	}

	private boolean isPresetDirty() {
		return currentPreset != null && !presetFingerprint.equals(ImageSettingsStore.fingerprint(settings));
	}

	private File choosePresetFile(boolean forSave) {
		gui.commons.dialogutils.NativeFileChooser chooser = new gui.commons.dialogutils.NativeFileChooser();
		chooser.setCurrentDirectory(currentPreset != null ? currentPreset.getParentFile() : ImageSettingsStore.presetsDir());
		chooser.setDialogTitle(forSave ? "Export Image Studio preset" : "Import Image Studio preset");
		if (forSave) {
			chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), currentPreset != null ? currentPreset.getName() : "my_preset.properties"));
		}
		if (chooser.showDialog(this, forSave ? "Export" : "Import") != gui.commons.dialogutils.NativeFileChooser.APPROVE_OPTION) {
			return null;
		}
		File file = chooser.getSelectedFile();
		if (forSave && !file.getName().contains(".")) {
			file = new File(file.getParentFile(), file.getName() + ".properties");
		}
		return file;
	}

	private void importPreset() {
		File file = choosePresetFile(false);
		if (file == null) {
			return;
		}
		try {
			ImageSettingsStore.load(file, settings, false);
			currentPreset = file;
			syncFromSettings();
			presetFingerprint = ImageSettingsStore.fingerprint(settings);
			updatePresetLabel();
			onChange.run();
		} catch (IOException e) {
			javax.swing.JOptionPane.showMessageDialog(this, "Cannot read the preset:\n" + e.getMessage(), "Import preset", javax.swing.JOptionPane.WARNING_MESSAGE);
		}
	}

	private void exportPreset() {
		File file = choosePresetFile(true);
		if (file == null) {
			return;
		}
		if (file.exists() && javax.swing.JOptionPane.showConfirmDialog(this, "Overwrite " + file.getName() + "?", "File exists", javax.swing.JOptionPane.YES_NO_OPTION) != javax.swing.JOptionPane.YES_OPTION) {
			return;
		}
		writePreset(file);
	}

	/** Saves in the preset in use; without one it works like Export. Asks before overwriting. */
	private void savePreset() {
		if (currentPreset == null) {
			exportPreset();
			return;
		}
		if (javax.swing.JOptionPane.showConfirmDialog(this, "Overwrite the preset " + currentPreset.getName() + " with the current settings?", "Save preset",
				javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
			writePreset(currentPreset);
		}
	}

	private void writePreset(File file) {
		try {
			ImageSettingsStore.write(file, settings);
			currentPreset = file;
			presetFingerprint = ImageSettingsStore.fingerprint(settings);
			updatePresetLabel();
		} catch (IOException e) {
			javax.swing.JOptionPane.showMessageDialog(this, "Cannot write the preset:\n" + e.getMessage(), "Save preset", javax.swing.JOptionPane.WARNING_MESSAGE);
		}
	}

	private void resetAll() {
		ImageSettings defaults = new ImageSettings();
		settings.resetEdits();
		settings.format = defaults.format;
		settings.quality = defaults.quality;
		settings.progressive = defaults.progressive;
		settings.background = defaults.background;
		settings.percent = defaults.percent;
		settings.width = defaults.width;
		settings.height = defaults.height;
		settings.resampling = defaults.resampling;
		syncFromSettings();
		onChange.run();
	}

	/** Keeps the current settings as "the last used": call when the window or tab closes */
	public void persistLast() {
		ImageSettingsStore.saveLast(settings);
	}

	/**
	 * If a preset is in use and the settings changed since, asks whether to overwrite it. The settings stay remembered
	 * as the last used in any case.
	 *
	 * @return false if the user cancelled the closing
	 */
	public boolean confirmPresetOnClose(java.awt.Component parent) {
		persistLast();
		if (!isPresetDirty()) {
			return true;
		}
		Object[] options = {"Overwrite preset", "Keep the preset as it was", "Cancel"};
		int choice = javax.swing.JOptionPane.showOptionDialog(parent,
				"You changed the settings of the preset \"" + currentPreset.getName() + "\".\nOverwrite the preset? (The new settings are remembered as the last used anyway.)",
				"Preset changed", javax.swing.JOptionPane.YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (choice == 0) {
			writePreset(currentPreset);
			return true;
		}
		return choice == 1;
	}

	/** Extra section added by the host (details, histogram, view options...) */
	public void addExtraSection(String title, javax.swing.JComponent content, StudioMode minimumMode, boolean expanded) {
		accordion.addSection(title, content, minimumMode, expanded);
		accordion.applyMode(StudioMode.current());
	}

	public ImageSettings getSettings() {
		return settings;
	}

	public void applyMode(StudioMode mode) {
		accordion.applyMode(mode);
		for (Runnable refresher : modeRefreshers) {
			refresher.run();
		}
	}

	/** Size of the image the settings apply to: shown as the result of the resize */
	public void setBaseSize(int w, int h) {
		baseWidth = w;
		baseHeight = h;
		refreshResizeResult();
	}

	public void setEstimate(String text) {
		lblEstimate.setText(text);
	}

	/** Re-reads every control from the settings (after a reset) without firing change events */
	public void syncFromSettings() {
		updating = true;
		try {
			comboFormat.setSelectedItem(settings.format);
			sliderQuality.setValue(settings.quality);
			chkProgressive.setSelected(settings.progressive);
			comboResize.setSelectedItem(settings.resizeMode);
			spinPercent.setValue(settings.percent);
			spinWidth.setValue(settings.width);
			spinHeight.setValue(settings.height);
			comboResampling.setSelectedItem(settings.resampling);
			sBrightness.setValue(settings.brightness);
			sContrast.setValue(settings.contrast);
			sSaturation.setValue(settings.saturation);
			sGamma.setValue((int) Math.round(settings.gamma * 100));
			sTemperature.setValue(settings.temperature);
			sHue.setValue(settings.hue);
			for (Map.Entry<ImageEdit.Filter, JToggleButton> entry : filterButtons.entrySet()) {
				entry.getValue().setSelected(settings.filters.contains(entry.getKey()));
			}
			btnBackground.setBackground(settings.background);
			updateEnabled();
			refreshResizeResult();
		} finally {
			updating = false;
		}
	}

	private void changed() {
		if (!updating) {
			updateEnabled();
			refreshResizeResult();
			updatePresetLabel();
			onChange.run();
		}
	}

	// ------------------------------------------------------------------ sections

	private JPanel buildConvert() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 2, gapy 6, hidemode 3", "[][grow,fill]", ""));
		p.setOpaque(false);
		p.add(ToolStyle.mutedLabel("Format"));
		comboFormat = new JComboBox<String>(ImageEdit.writableFormats().toArray(new String[0]));
		comboFormat.setSelectedItem(settings.format);
		ToolStyle.hand(comboFormat);
		comboFormat.addActionListener(e -> {
			if (!updating) {
				settings.format = (String) comboFormat.getSelectedItem();
				changed();
			}
		});
		p.add(comboFormat);

		lblQuality = ToolStyle.mutedLabel("Quality: " + settings.quality);
		p.add(lblQuality);
		sliderQuality = new JSlider(1, 100, settings.quality);
		sliderQuality.setToolTipText("JPEG quality: lower is smaller and worse");
		sliderQuality.addChangeListener(e -> {
			lblQuality.setText("Quality: " + sliderQuality.getValue());
			if (!updating) {
				settings.quality = sliderQuality.getValue();
				changed();
			}
		});
		p.add(sliderQuality);

		chkProgressive = new JCheckBox("Progressive JPEG");
		chkProgressive.setToolTipText("Loads blurry first, then sharp: better for the web");
		ToolStyle.hand(chkProgressive);
		chkProgressive.addActionListener(e -> {
			settings.progressive = chkProgressive.isSelected();
			changed();
		});
		JLabel bgLabel = ToolStyle.mutedLabel("Transparency");
		btnBackground = new JButton("  ");
		btnBackground.setBackground(settings.background);
		btnBackground.setToolTipText("Color used instead of transparency in JPEG and BMP");
		btnBackground.setPreferredSize(new Dimension(60, 22));
		btnBackground.addActionListener(e -> {
			Color picked = JColorChooser.showDialog(this, "Color for transparent areas", settings.background);
			if (picked != null) {
				settings.background = picked;
				btnBackground.setBackground(picked);
				changed();
			}
		});
		p.add(chkProgressive, "span 2");
		p.add(bgLabel);
		p.add(btnBackground);

		lblEstimate = ToolStyle.mutedLabel(" ");
		p.add(lblEstimate, "span 2");

		modeRefreshers.add(() -> {
			boolean nerd = StudioMode.current() == StudioMode.NERD;
			chkProgressive.setVisible(nerd);
			bgLabel.setVisible(nerd);
			btnBackground.setVisible(nerd);
		});
		return p;
	}

	private JPanel buildResize() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 2, gapy 6, hidemode 3", "[][grow,fill]", ""));
		p.setOpaque(false);
		p.add(ToolStyle.mutedLabel("Mode"));
		comboResize = new JComboBox<ImageSettings.ResizeMode>(ImageSettings.ResizeMode.values());
		ToolStyle.hand(comboResize);
		comboResize.addActionListener(e -> {
			if (!updating) {
				settings.resizeMode = (ImageSettings.ResizeMode) comboResize.getSelectedItem();
				changed();
			}
		});
		p.add(comboResize);

		p.add(ToolStyle.mutedLabel("Percent"));
		spinPercent = spinner(settings.percent, 1, 800, v -> settings.percent = v);
		p.add(spinPercent);
		p.add(ToolStyle.mutedLabel("Width"));
		spinWidth = spinner(settings.width, 1, 30000, v -> settings.width = v);
		p.add(spinWidth);
		p.add(ToolStyle.mutedLabel("Height"));
		spinHeight = spinner(settings.height, 1, 30000, v -> settings.height = v);
		p.add(spinHeight);

		JLabel resamplingLabel = ToolStyle.mutedLabel("Quality");
		comboResampling = new JComboBox<ImageSettings.Resampling>(ImageSettings.Resampling.values());
		ToolStyle.hand(comboResampling);
		comboResampling.addActionListener(e -> {
			if (!updating) {
				settings.resampling = (ImageSettings.Resampling) comboResampling.getSelectedItem();
				changed();
			}
		});
		p.add(resamplingLabel);
		p.add(comboResampling);

		lblResizeResult = ToolStyle.mutedLabel(" ");
		p.add(lblResizeResult, "span 2");
		modeRefreshers.add(() -> {
			boolean nerd = StudioMode.current() == StudioMode.NERD;
			resamplingLabel.setVisible(nerd);
			comboResampling.setVisible(nerd);
		});
		return p;
	}

	private interface IntSetter {
		void set(int value);
	}

	private JSpinner spinner(int value, int min, int max, final IntSetter setter) {
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
		spinner.addChangeListener(e -> {
			if (!updating) {
				setter.set(((Number) spinner.getValue()).intValue());
				changed();
			}
		});
		return spinner;
	}

	private void updateEnabled() {
		ImageSettings.ResizeMode mode = settings.resizeMode;
		spinPercent.setEnabled(mode == ImageSettings.ResizeMode.PERCENT);
		spinWidth.setEnabled(mode == ImageSettings.ResizeMode.FIT_BOX || mode == ImageSettings.ResizeMode.WIDTH || mode == ImageSettings.ResizeMode.EXACT);
		spinHeight.setEnabled(mode == ImageSettings.ResizeMode.FIT_BOX || mode == ImageSettings.ResizeMode.HEIGHT || mode == ImageSettings.ResizeMode.EXACT);
		comboResampling.setEnabled(mode != ImageSettings.ResizeMode.NONE);
		boolean jpeg = "jpg".equals(settings.format);
		sliderQuality.setEnabled(jpeg);
		lblQuality.setEnabled(jpeg);
		chkProgressive.setEnabled(jpeg);
	}

	private void refreshResizeResult() {
		if (lblResizeResult == null) {
			return;
		}
		if (baseWidth <= 0 || settings.resizeMode == ImageSettings.ResizeMode.NONE) {
			lblResizeResult.setText(baseWidth > 0 ? baseWidth + " x " + baseHeight + " px" : " ");
			return;
		}
		int[] size = ImageEdit.targetSize(baseWidth, baseHeight, settings);
		lblResizeResult.setText(baseWidth + " x " + baseHeight + "  ->  " + size[0] + " x " + size[1] + " px");
	}

	private JPanel buildAdjustments() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 1, gapy 2, hidemode 3", "[grow,fill]", ""));
		p.setOpaque(false);
		sBrightness = slider(p, "Brightness", -100, 100, 0, null, v -> settings.brightness = v);
		sContrast = slider(p, "Contrast", -100, 100, 0, null, v -> settings.contrast = v);
		sSaturation = slider(p, "Saturation", -100, 100, 0, null, v -> settings.saturation = v);
		final List<javax.swing.JComponent> nerdOnly = new ArrayList<javax.swing.JComponent>();
		JPanel nerd = new JPanel(new MigLayout("insets 0, wrap 1, gapy 2, hidemode 3", "[grow,fill]", ""));
		nerd.setOpaque(false);
		sGamma = slider(nerd, "Gamma", 20, 300, 100, "%.2f", v -> settings.gamma = v / 100.0);
		sTemperature = slider(nerd, "Temperature", -100, 100, 0, null, v -> settings.temperature = v);
		sHue = slider(nerd, "Hue", -180, 180, 0, null, v -> settings.hue = v);
		p.add(nerd, "growx");
		nerdOnly.add(nerd);
		JButton reset = ToolStyle.button("Reset adjustments", null, "Brings every slider back to zero");
		reset.addActionListener(e -> {
			settings.brightness = 0;
			settings.contrast = 0;
			settings.saturation = 0;
			settings.gamma = 1.0;
			settings.temperature = 0;
			settings.hue = 0;
			syncFromSettings();
			onChange.run();
		});
		p.add(reset, "gaptop 4");
		modeRefreshers.add(() -> {
			for (javax.swing.JComponent c : nerdOnly) {
				c.setVisible(StudioMode.current() == StudioMode.NERD);
			}
		});
		return p;
	}

	private JSlider slider(JPanel p, final String label, int min, int max, int value, final String valueFormat, final IntSetter setter) {
		final JLabel caption = ToolStyle.mutedLabel(label + ": " + format(value, valueFormat));
		p.add(caption);
		final JSlider slider = new JSlider(min, max, value);
		final int resetValue = value;
		slider.addChangeListener(e -> {
			caption.setText(label + ": " + format(slider.getValue(), valueFormat));
			if (!updating) {
				setter.set(slider.getValue());
				changed();
			}
		});
		slider.setToolTipText("Double click to reset");
		slider.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					slider.setValue(resetValue);
				}
			}
		});
		p.add(slider);
		return slider;
	}

	private static String format(int value, String valueFormat) {
		return valueFormat == null ? String.valueOf(value) : String.format(java.util.Locale.ROOT, valueFormat, value / 100.0);
	}

	private JPanel buildFilters() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 1, gapy 4, hidemode 3", "[grow,fill]", ""));
		p.setOpaque(false);
		p.add(ToolStyle.mutedLabel("Click to turn on or off, they stack in order"));
		JPanel classic = new JPanel(new MigLayout("insets 0, wrap 2, gap 4", "[grow,fill][grow,fill]", ""));
		classic.setOpaque(false);
		for (ImageEdit.Filter filter : CLASSIC_FILTERS) {
			classic.add(filterToggle(filter));
		}
		p.add(classic, "growx");
		nerdFilters = new JPanel(new MigLayout("insets 0, wrap 2, gap 4", "[grow,fill][grow,fill]", ""));
		nerdFilters.setOpaque(false);
		List<ImageEdit.Filter> classicList = java.util.Arrays.asList(CLASSIC_FILTERS);
		for (ImageEdit.Filter filter : ImageEdit.Filter.values()) {
			if (!classicList.contains(filter)) {
				nerdFilters.add(filterToggle(filter));
			}
		}
		p.add(nerdFilters, "growx");
		JButton clear = ToolStyle.button("Clear filters", null, "Turn every filter off");
		clear.addActionListener(e -> {
			settings.filters.clear();
			syncFromSettings();
			onChange.run();
		});
		p.add(clear, "gaptop 2");
		modeRefreshers.add(() -> nerdFilters.setVisible(StudioMode.current() == StudioMode.NERD));
		return p;
	}

	private JToggleButton filterToggle(final ImageEdit.Filter filter) {
		final JToggleButton button = new JToggleButton(filter.label);
		button.setToolTipText(filter.description);
		button.setFont(button.getFont().deriveFont(Font.PLAIN, button.getFont().getSize2D() - 1f));
		button.setMargin(new java.awt.Insets(4, 4, 4, 4));
		button.setSelected(settings.filters.contains(filter));
		ToolStyle.hand(button);
		button.addActionListener(e -> {
			settings.filters.remove(filter);
			if (button.isSelected()) {
				settings.filters.add(filter);
			}
			changed();
		});
		filterButtons.put(filter, button);
		return button;
	}

	private JPanel buildTransform() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 2, gap 4", "[grow,fill][grow,fill]", ""));
		p.setOpaque(false);
		JButton left = ToolStyle.button("Rotate left", null, "Rotate 90 degrees counterclockwise (L)");
		left.addActionListener(e -> {
			settings.rotation = (settings.rotation + 270) % 360;
			changed();
		});
		JButton right = ToolStyle.button("Rotate right", null, "Rotate 90 degrees clockwise (R)");
		right.addActionListener(e -> {
			settings.rotation = (settings.rotation + 90) % 360;
			changed();
		});
		JButton flipH = ToolStyle.button("Flip horizontal", null, "Mirror left to right (H)");
		flipH.addActionListener(e -> {
			settings.flipH = !settings.flipH;
			changed();
		});
		JButton flipV = ToolStyle.button("Flip vertical", null, "Mirror top to bottom (V)");
		flipV.addActionListener(e -> {
			settings.flipV = !settings.flipV;
			changed();
		});
		p.add(left);
		p.add(right);
		p.add(flipH);
		p.add(flipV);
		return p;
	}

	// used by the host shortcuts

	public void rotate(int degreesClockwise) {
		settings.rotation = ((settings.rotation + degreesClockwise) % 360 + 360) % 360;
		changed();
	}

	public void flip(boolean horizontal) {
		if (horizontal) {
			settings.flipH = !settings.flipH;
		} else {
			settings.flipV = !settings.flipV;
		}
		changed();
	}
}
