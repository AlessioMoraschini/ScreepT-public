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
import javax.swing.ButtonGroup;
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
			ImageEdit.Filter.GRAYSCALE, ImageEdit.Filter.NOIR, ImageEdit.Filter.SEPIA, ImageEdit.Filter.VINTAGE, ImageEdit.Filter.WARM,
			ImageEdit.Filter.COOL, ImageEdit.Filter.VIVID, ImageEdit.Filter.AUTO_CONTRAST, ImageEdit.Filter.VIGNETTE,
			ImageEdit.Filter.SHARPEN, ImageEdit.Filter.BLUR, ImageEdit.Filter.LOMO, ImageEdit.Filter.GOLDEN, ImageEdit.Filter.CLARITY,
			ImageEdit.Filter.RAINBOW, ImageEdit.Filter.NEON, ImageEdit.Filter.POP_ART, ImageEdit.Filter.KALEIDOSCOPE};

	private static final String SEC_CONVERT = "Convert & export";
	private static final String SEC_RESIZE = "Resize";
	private static final String SEC_CROP = "Crop";
	private static final String SEC_EFFECTS = "Effects";
	private static final String SEC_DECORATE = "Border & text";
	private static final String SEC_ADJUST = "Adjustments";
	private static final String SEC_FILTERS = "Filters";
	private static final String SEC_TRANSFORM = "Rotate & flip";
	private static final String SEC_PRESETS = "Presets";
	/** Longest side of the small image the filter cards are drawn from */
	private static final int THUMB_SIZE = 144;

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
	private final Map<ImageEdit.Filter, FilterCard> filterCards = new EnumMap<ImageEdit.Filter, FilterCard>(ImageEdit.Filter.class);
	private final Map<ImageEdit.Category, JToggleButton> categoryChips = new EnumMap<ImageEdit.Category, JToggleButton>(ImageEdit.Category.class);
	private JPanel filterGrid;
	private ImageEdit.Category filterCategory = ImageEdit.Category.COLOR;
	private JSlider sFilterStrength;
	private JLabel lblCropResult;
	/** Controls added by the sections that follow the settings by themselves (reset, presets) */
	private final List<Runnable> syncers = new ArrayList<Runnable>();
	private int thumbGeneration;
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

		accordion.addSection(SEC_CONVERT, buildConvert(), StudioMode.BASIC, true);
		accordion.addSection(SEC_RESIZE, buildResize(), StudioMode.BASIC, false);
		accordion.addSection(SEC_CROP, buildCrop(), StudioMode.CLASSIC, false);
		accordion.addSection(SEC_ADJUST, buildAdjustments(), StudioMode.CLASSIC, false);
		accordion.addSection(SEC_EFFECTS, buildEffects(), StudioMode.CLASSIC, false);
		accordion.addSection(SEC_FILTERS, buildFilters(), StudioMode.CLASSIC, false);
		accordion.addSection(SEC_TRANSFORM, buildTransform(), StudioMode.CLASSIC, false);
		accordion.addSection(SEC_DECORATE, buildDecorate(), StudioMode.NERD, false);
		accordion.addSection(SEC_PRESETS, buildPresets(), StudioMode.BASIC, false);

		JPanel holder = new ScrollableHolder(new MigLayout("insets 0 0 0 6, aligny top", "[grow,fill]", ""));
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
		p.add(lblPreset, "span 2, wmin 0");
		JLabel hint = ToolStyle.mutedLabel("Import: .properties, .xmp (Camera Raw, Lightroom), .lrtemplate");
		hint.setToolTipText(PhotoshopPresets.SUPPORTED_HINT);
		p.add(hint, "span 2, wmin 0");
		JButton save = ToolStyle.button("Save", null, "Save the settings in the preset file (asks before overwriting)");
		save.addActionListener(e -> savePreset());
		JButton export = ToolStyle.button("Export...", null, "Save the settings to a file, to keep or share");
		export.addActionListener(e -> exportPreset());
		JButton imp = ToolStyle.button("Import...", null, PhotoshopPresets.SUPPORTED_HINT);
		imp.addActionListener(e -> importPresetFromUser());
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
		chooser.setDialogTitle(forSave ? "Export Image Studio preset" : "Import preset (.properties, .xmp, .lrtemplate)");
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

	/** Asks for a preset file (Image Studio, Camera Raw / Lightroom) and applies it */
	public void importPresetFromUser() {
		File file = choosePresetFile(false);
		if (file == null) {
			return;
		}
		if (PhotoshopPresets.isAdobePreset(file)) {
			try {
				String name = file.getName();
				applyAdobeSettings(PhotoshopPresets.read(file), name.substring(0, name.lastIndexOf('.')));
			} catch (IOException e) {
				javax.swing.JOptionPane.showMessageDialog(this, "Cannot read the preset:\n" + e.getMessage(), "Import preset", javax.swing.JOptionPane.WARNING_MESSAGE);
			}
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

	/** Applies Adobe develop settings (already read) and shows what was translated and what was not */
	public void applyAdobeSettings(java.util.Map<String, String> crs, String name) {
		PhotoshopPresets.Report report = PhotoshopPresets.apply(crs, settings, name);
		currentPreset = null;
		syncFromSettings();
		presetFingerprint = ImageSettingsStore.fingerprint(settings);
		updatePresetLabel();
		onChange.run();
		javax.swing.JOptionPane.showMessageDialog(this, new JLabel(report.toHtml()), "Imported settings", javax.swing.JOptionPane.INFORMATION_MESSAGE);
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
		refreshCropResult();
		refreshBadges();
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
			for (Map.Entry<ImageEdit.Filter, FilterCard> entry : filterCards.entrySet()) {
				entry.getValue().setSelected(settings.filters.contains(entry.getKey()));
			}
			sFilterStrength.setValue(settings.filterStrength);
			for (Runnable syncer : syncers) {
				syncer.run();
			}
			refreshCropResult();
			btnBackground.setBackground(settings.background);
			updateEnabled();
			refreshResizeResult();
			refreshBadges();
			refreshFilterChips();
		} finally {
			updating = false;
		}
	}

	private void changed() {
		if (!updating) {
			updateEnabled();
			refreshResizeResult();
			refreshBadges();
			refreshFilterChips();
			refreshCropResult();
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
		p.add(lblEstimate, "span 2, wmin 0");

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

		p.add(ToolStyle.mutedLabel("Quick size"));
		final String[] quickNames = {"Pick a common size...", "Full HD  1920 x 1080", "HD  1280 x 720", "4K  3840 x 2160", "Square  1080 x 1080",
				"Story  1080 x 1920", "Facebook cover  820 x 312", "Icon  512 x 512", "Icon  256 x 256", "Thumbnail  320 x 240"};
		final int[][] quickSizes = {null, {1920, 1080}, {1280, 720}, {3840, 2160}, {1080, 1080}, {1080, 1920}, {820, 312}, {512, 512}, {256, 256}, {320, 240}};
		final JComboBox<String> quick = new JComboBox<String>(quickNames);
		ToolStyle.hand(quick);
		quick.setToolTipText("Fits the image in the box of a common format");
		quick.addActionListener(e -> {
			int index = quick.getSelectedIndex();
			if (updating || index <= 0) {
				return;
			}
			settings.resizeMode = ImageSettings.ResizeMode.FIT_BOX;
			settings.width = quickSizes[index][0];
			settings.height = quickSizes[index][1];
			syncFromSettings();
			updating = true;
			quick.setSelectedIndex(0);
			updating = false;
			changed();
		});
		p.add(quick, "wmin 0");

		p.add(ToolStyle.mutedLabel("Percent"));

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
		p.add(lblResizeResult, "span 2, wmin 0");
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
		if (sFilterStrength != null) {
			sFilterStrength.setEnabled(!settings.filters.isEmpty());
		}
	}

	private void refreshResizeResult() {
		if (lblResizeResult == null) {
			return;
		}
		int[] from = effectiveSize();
		if (baseWidth <= 0 || settings.resizeMode == ImageSettings.ResizeMode.NONE) {
			lblResizeResult.setText(baseWidth > 0 ? from[0] + " x " + from[1] + " px" : " ");
			return;
		}
		int[] size = ImageEdit.targetSize(from[0], from[1], settings);
		lblResizeResult.setText(from[0] + " x " + from[1] + "  ->  " + size[0] + " x " + size[1] + " px");
	}

	private JPanel buildAdjustments() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 3, gapy 2, hidemode 3", "[96!][grow,fill][30!,right]", ""));
		p.setOpaque(false);
		sBrightness = slider(p, "Brightness", -100, 100, 0, null, v -> settings.brightness = v);
		sContrast = slider(p, "Contrast", -100, 100, 0, null, v -> settings.contrast = v);
		bind(p, "Exposure", -100, 100, 0, () -> settings.exposure, v -> settings.exposure = v);
		bind(p, "Shadows", -100, 100, 0, () -> settings.shadows, v -> settings.shadows = v);
		bind(p, "Highlights", -100, 100, 0, () -> settings.highlights, v -> settings.highlights = v);
		sSaturation = slider(p, "Saturation", -100, 100, 0, null, v -> settings.saturation = v);
		bind(p, "Vibrance", -100, 100, 0, () -> settings.vibrance, v -> settings.vibrance = v);
		final List<javax.swing.JComponent> nerdOnly = new ArrayList<javax.swing.JComponent>();
		JPanel nerd = new JPanel(new MigLayout("insets 0, wrap 3, gapy 2, hidemode 3", "[96!][grow,fill][30!,right]", ""));
		nerd.setOpaque(false);
		sGamma = slider(nerd, "Gamma", 20, 300, 100, "%.2f", v -> settings.gamma = v / 100.0);
		sTemperature = slider(nerd, "Temperature", -100, 100, 0, null, v -> settings.temperature = v);
		bind(nerd, "Tint", -100, 100, 0, () -> settings.tint, v -> settings.tint = v);
		sHue = slider(nerd, "Hue", -180, 180, 0, null, v -> settings.hue = v);
		p.add(nerd, "span 3, growx");
		nerdOnly.add(nerd);
		JPanel buttons = new JPanel(new MigLayout("insets 0, gapx 4", "[grow,fill][grow,fill]", ""));
		buttons.setOpaque(false);
		JButton auto = ToolStyle.button("Auto enhance", null, "Stretches the tones and adds a little vibrance: a good start for most photos");
		auto.addActionListener(e -> {
			if (!settings.filters.contains(ImageEdit.Filter.AUTO_CONTRAST)) {
				settings.filters.add(ImageEdit.Filter.AUTO_CONTRAST);
			}
			settings.vibrance = Math.max(settings.vibrance, 25);
			syncFromSettings();
			changed();
		});
		JButton reset = ToolStyle.button("Reset", null, "Brings every slider back to zero");
		reset.addActionListener(e -> {
			settings.resetColor();
			syncFromSettings();
			changed();
		});
		buttons.add(auto, "wmin 0");
		buttons.add(reset, "wmin 0");
		p.add(buttons, "span 3, growx, gaptop 4");
		modeRefreshers.add(() -> {
			for (javax.swing.JComponent c : nerdOnly) {
				c.setVisible(StudioMode.current() == StudioMode.NERD);
			}
		});
		return p;
	}

	/** A slider that also follows the settings when they are reset or loaded from a preset */
	private JSlider bind(JPanel p, String label, int min, int max, int value, final java.util.function.IntSupplier getter, IntSetter setter) {
		final JSlider slider = slider(p, label, min, max, value, null, setter);
		syncers.add(() -> slider.setValue(getter.getAsInt()));
		return slider;
	}

	private JButton colorButton(final String title, final java.util.function.Supplier<Color> getter, final java.util.function.Consumer<Color> setter) {
		final JButton button = new JButton("  ");
		button.setPreferredSize(new Dimension(60, 22));
		button.setBackground(getter.get());
		button.setToolTipText(title);
		button.addActionListener(e -> {
			Color picked = JColorChooser.showDialog(this, title, getter.get());
			if (picked != null) {
				setter.accept(picked);
				button.setBackground(picked);
				changed();
			}
		});
		syncers.add(() -> button.setBackground(getter.get()));
		return button;
	}

	private JPanel buildCrop() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 3, gapy 2, hidemode 3", "[96!][grow,fill][30!,right]", ""));
		p.setOpaque(false);
		p.add(ToolStyle.mutedLabel("Ratio"), "wmin 0");
		final JComboBox<ImageSettings.CropRatio> ratio = new JComboBox<ImageSettings.CropRatio>(ImageSettings.CropRatio.values());
		ToolStyle.hand(ratio);
		ratio.setToolTipText("Biggest centered area with this shape, inside the margins below");
		ratio.addActionListener(e -> {
			if (!updating) {
				settings.cropRatio = (ImageSettings.CropRatio) ratio.getSelectedItem();
				changed();
			}
		});
		syncers.add(() -> ratio.setSelectedItem(settings.cropRatio));
		p.add(ratio, "span 2, growx, wmin 0");
		bind(p, "Left (%)", 0, 45, 0, () -> settings.cropLeft, v -> settings.cropLeft = v);
		bind(p, "Top (%)", 0, 45, 0, () -> settings.cropTop, v -> settings.cropTop = v);
		bind(p, "Right (%)", 0, 45, 0, () -> settings.cropRight, v -> settings.cropRight = v);
		bind(p, "Bottom (%)", 0, 45, 0, () -> settings.cropBottom, v -> settings.cropBottom = v);
		lblCropResult = ToolStyle.mutedLabel(" ");
		p.add(lblCropResult, "span 3, wmin 0");
		JButton reset = ToolStyle.button("Reset crop", null, "Keep the whole image");
		reset.addActionListener(e -> {
			settings.resetCrop();
			syncFromSettings();
			changed();
		});
		p.add(reset, "span 3, growx, gaptop 2");
		return p;
	}

	private void refreshCropResult() {
		if (lblCropResult == null) {
			return;
		}
		if (!settings.hasCrop() || baseWidth <= 0) {
			lblCropResult.setText(baseWidth > 0 ? "Cut the sides, or pick a ratio" : " ");
			return;
		}
		int[] r = ImageEdit.cropRect(baseWidth, baseHeight, settings);
		lblCropResult.setText(baseWidth + " x " + baseHeight + "  ->  " + r[2] + " x " + r[3] + " px");
	}

	/** Size after the crop: what the resize starts from */
	private int[] effectiveSize() {
		if (baseWidth > 0 && settings.hasCrop()) {
			int[] r = ImageEdit.cropRect(baseWidth, baseHeight, settings);
			return new int[] {r[2], r[3]};
		}
		return new int[] {baseWidth, baseHeight};
	}

	private JPanel buildEffects() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 3, gapy 2, hidemode 3", "[96!][grow,fill][30!,right]", ""));
		p.setOpaque(false);
		bind(p, "Sharpness", 0, 100, 0, () -> settings.sharpness, v -> settings.sharpness = v);
		bind(p, "Blur (px)", 0, 20, 0, () -> settings.blur, v -> settings.blur = v);
		bind(p, "Vignette", 0, 100, 0, () -> settings.vignette, v -> settings.vignette = v);
		bind(p, "Film grain", 0, 100, 0, () -> settings.grain, v -> settings.grain = v);
		JButton reset = ToolStyle.button("Reset", null, "Turns every effect off");
		reset.addActionListener(e -> {
			settings.resetEffects();
			syncFromSettings();
			changed();
		});
		p.add(reset, "span 3, growx, gaptop 4");
		return p;
	}

	private JPanel buildDecorate() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 3, gapy 3, hidemode 3", "[96!][grow,fill][30!,right]", ""));
		p.setOpaque(false);
		bind(p, "Border (px)", 0, 200, 0, () -> settings.borderWidth, v -> settings.borderWidth = v);
		p.add(ToolStyle.mutedLabel("Border color"), "wmin 0");
		p.add(colorButton("Border color", () -> settings.borderColor, c -> settings.borderColor = c), "span 2");
		bind(p, "Corners (%)", 0, 50, 0, () -> settings.corners, v -> settings.corners = v);
		p.add(new javax.swing.JSeparator(), "span 3, growx, gaptop 4, gapbottom 2");
		p.add(ToolStyle.mutedLabel("Text"), "wmin 0");
		final javax.swing.JTextField text = new javax.swing.JTextField();
		text.setToolTipText("Text written on the image, like a copyright line (empty: nothing)");
		text.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			private void edited() {
				if (!updating) {
					settings.watermark = text.getText();
					changed();
				}
			}

			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				edited();
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				edited();
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				edited();
			}
		});
		syncers.add(() -> {
			String current = settings.watermark == null ? "" : settings.watermark;
			if (!current.equals(text.getText())) {
				text.setText(current);
			}
		});
		p.add(text, "span 2, growx, wmin 0");
		p.add(ToolStyle.mutedLabel("Position"), "wmin 0");
		final JComboBox<ImageSettings.TextPosition> position = new JComboBox<ImageSettings.TextPosition>(ImageSettings.TextPosition.values());
		ToolStyle.hand(position);
		position.addActionListener(e -> {
			if (!updating) {
				settings.watermarkPosition = (ImageSettings.TextPosition) position.getSelectedItem();
				changed();
			}
		});
		syncers.add(() -> position.setSelectedItem(settings.watermarkPosition));
		p.add(position, "span 2, growx, wmin 0");
		bind(p, "Text size (%)", 2, 30, 5, () -> settings.watermarkSize, v -> settings.watermarkSize = v);
		bind(p, "Opacity (%)", 5, 100, 70, () -> settings.watermarkOpacity, v -> settings.watermarkOpacity = v);
		p.add(ToolStyle.mutedLabel("Text color"), "wmin 0");
		p.add(colorButton("Text color", () -> settings.watermarkColor, c -> settings.watermarkColor = c), "span 2");
		return p;
	}

	private JSlider slider(JPanel p, final String label, int min, int max, int value, final String valueFormat, final IntSetter setter) {
		final JLabel caption = ToolStyle.mutedLabel(label);
		final JLabel valueLabel = ToolStyle.mutedLabel(format(value, valueFormat));
		p.add(caption, "wmin 0");
		final JSlider slider = new JSlider(min, max, value);
		final int resetValue = value;
		slider.addChangeListener(e -> {
			valueLabel.setText(format(slider.getValue(), valueFormat));
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
		p.add(valueLabel);
		return slider;
	}

	private static String format(int value, String valueFormat) {
		return valueFormat == null ? String.valueOf(value) : String.format(java.util.Locale.ROOT, valueFormat, value / 100.0);
	}

	private JPanel buildFilters() {
		JPanel p = new JPanel(new MigLayout("insets 0, wrap 1, gapy 6, hidemode 3", "[grow,fill]", ""));
		p.setOpaque(false);

		JPanel chips = new JPanel(new MigLayout("insets 0, gapx 4, gapy 4, hidemode 3, wrap 3", "[grow,fill,sg chip][grow,fill,sg chip][grow,fill,sg chip]", "[]"));
		chips.setOpaque(false);
		ButtonGroup group = new ButtonGroup();
		for (final ImageEdit.Category category : ImageEdit.Category.values()) {
			JToggleButton chip = new JToggleButton(category.label);
			chip.setFont(chip.getFont().deriveFont(Font.PLAIN, chip.getFont().getSize2D() - 1f));
			chip.setMargin(new java.awt.Insets(3, 1, 3, 1));
			chip.putClientProperty("JButton.buttonType", "roundRect");
			chip.setToolTipText("Show the " + category.label.toLowerCase(java.util.Locale.ROOT) + " filters");
			ToolStyle.hand(chip);
			chip.setSelected(category == filterCategory);
			chip.addActionListener(e -> {
				filterCategory = category;
				refreshFilterGrid();
			});
			group.add(chip);
			categoryChips.put(category, chip);
			chips.add(chip, "wmin 0");
		}
		p.add(chips, "growx");

		filterGrid = new JPanel(new MigLayout("insets 0, wrap 3, gap 6", "[grow,fill,sg card][grow,fill,sg card][grow,fill,sg card]", ""));
		filterGrid.setOpaque(false);
		for (ImageEdit.Filter filter : ImageEdit.Filter.values()) {
			filterCards.put(filter, filterCard(filter));
		}
		p.add(filterGrid, "growx, wmin 0");

		JPanel strength = new JPanel(new MigLayout("insets 0, wrap 3", "[96!][grow,fill][30!,right]", ""));
		strength.setOpaque(false);
		sFilterStrength = slider(strength, "Strength", 0, 100, 100, null, v -> settings.filterStrength = v);
		p.add(strength, "growx");
		JButton clear = ToolStyle.button("Clear filters", null, "Turn every filter off");
		clear.addActionListener(e -> {
			settings.filters.clear();
			settings.filterStrength = 100;
			syncFromSettings();
			onChange.run();
		});
		p.add(clear, "gaptop 2");
		modeRefreshers.add(this::refreshFilterGrid);
		refreshFilterGrid();
		return p;
	}

	/** Shows the filters of the chosen group that the current mode offers, and hides the groups left empty */
	private void refreshFilterGrid() {
		if (filterGrid == null) {
			return;
		}
		boolean nerd = StudioMode.current() == StudioMode.NERD;
		List<ImageEdit.Filter> classic = java.util.Arrays.asList(CLASSIC_FILTERS);
		ImageEdit.Category first = null;
		boolean currentShown = false;
		for (ImageEdit.Category category : ImageEdit.Category.values()) {
			boolean any = false;
			for (ImageEdit.Filter filter : ImageEdit.Filter.values()) {
				any |= filter.category == category && (nerd || classic.contains(filter));
			}
			categoryChips.get(category).setVisible(any);
			if (any && first == null) {
				first = category;
			}
			currentShown |= any && category == filterCategory;
		}
		if (!currentShown && first != null) {
			filterCategory = first;
			categoryChips.get(first).setSelected(true);
		}
		filterGrid.removeAll();
		for (ImageEdit.Filter filter : ImageEdit.Filter.values()) {
			if (filter.category == filterCategory && (nerd || classic.contains(filter))) {
				filterGrid.add(filterCards.get(filter));
			}
		}
		filterGrid.revalidate();
		filterGrid.repaint();
		refreshFilterChips();
	}

	/** Group chips show how many filters of the group are on */
	private void refreshFilterChips() {
		for (ImageEdit.Category category : ImageEdit.Category.values()) {
			JToggleButton chip = categoryChips.get(category);
			if (chip == null) {
				continue;
			}
			int on = 0;
			for (ImageEdit.Filter filter : settings.filters) {
				if (filter.category == category) {
					on++;
				}
			}
			chip.setText(on > 0 ? category.label + " " + on : category.label);
		}
	}

	private FilterCard filterCard(final ImageEdit.Filter filter) {
		final FilterCard card = new FilterCard(filter);
		card.setSelected(settings.filters.contains(filter));
		card.addActionListener(e -> {
			settings.filters.remove(filter);
			if (card.isSelected()) {
				settings.filters.add(filter);
			}
			changed();
		});
		return card;
	}

	/**
	 * Small copy of the image the filter cards are drawn from: every filter is applied to it in the background, so each
	 * card shows what the filter does to this very image.
	 */
	public void setThumbnailSource(final java.awt.image.BufferedImage source) {
		if (source == null) {
			return;
		}
		final int generation = ++thumbGeneration;
		new javax.swing.SwingWorker<Map<ImageEdit.Filter, java.awt.image.BufferedImage>, Void>() {
			@Override
			protected Map<ImageEdit.Filter, java.awt.image.BufferedImage> doInBackground() {
				double k = Math.min(1.0, THUMB_SIZE / (double) Math.max(source.getWidth(), source.getHeight()));
				int w = Math.max(1, (int) Math.round(source.getWidth() * k));
				int h = Math.max(1, (int) Math.round(source.getHeight() * k));
				java.awt.image.BufferedImage small = ImageEdit.resize(source, w, h, ImageSettings.Resampling.AREA);
				Map<ImageEdit.Filter, java.awt.image.BufferedImage> result = new EnumMap<ImageEdit.Filter, java.awt.image.BufferedImage>(ImageEdit.Filter.class);
				for (ImageEdit.Filter filter : ImageEdit.Filter.values()) {
					if (generation != thumbGeneration) {
						break;
					}
					result.put(filter, ImageEdit.filter(small, filter));
				}
				return result;
			}

			@Override
			protected void done() {
				if (generation != thumbGeneration) {
					return;
				}
				try {
					for (Map.Entry<ImageEdit.Filter, java.awt.image.BufferedImage> entry : get().entrySet()) {
						filterCards.get(entry.getKey()).setThumbnail(entry.getValue());
					}
				} catch (Exception e) {
					// the cards stay without preview: the filters work anyway
				}
			}
		}.execute();
	}

	/** A filter as a card: preview of the filter on the image, name below, blue ring when it is on */
	private static class FilterCard extends JToggleButton {
		private static final long serialVersionUID = 1L;

		private java.awt.image.BufferedImage thumbnail;

		FilterCard(ImageEdit.Filter filter) {
			super(filter.label);
			setToolTipText("<html><b>" + filter.label + "</b><br>" + filter.description + "</html>");
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setRolloverEnabled(true);
			ToolStyle.hand(this);
			setPreferredSize(new Dimension(80, 70));
			setMinimumSize(new Dimension(40, 60));
		}

		void setThumbnail(java.awt.image.BufferedImage image) {
			thumbnail = image;
			repaint();
		}

		@Override
		protected void paintComponent(java.awt.Graphics g0) {
			java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
			g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			int w = getWidth();
			int h = getHeight();
			java.awt.FontMetrics fm = g.getFontMetrics(getFont().deriveFont(Font.PLAIN, getFont().getSize2D() - 1f));
			int labelHeight = fm.getHeight() + 2;
			int thumbHeight = Math.max(8, h - labelHeight - 3);
			java.awt.geom.RoundRectangle2D.Float shape = new java.awt.geom.RoundRectangle2D.Float(1.5f, 1.5f, w - 3f, thumbHeight - 1f, 9, 9);
			Color panel = javax.swing.UIManager.getColor("Panel.background");
			g.setColor(panel == null ? Color.GRAY : ToolStyle.blend(panel, ToolStyle.isDark() ? Color.WHITE : Color.BLACK, 0.12f));
			g.fill(shape);
			if (thumbnail != null) {
				java.awt.Shape old = g.getClip();
				g.clip(shape);
				double scale = Math.max((w - 3.0) / thumbnail.getWidth(), (thumbHeight - 1.0) / thumbnail.getHeight());
				int sw = (int) Math.ceil(thumbnail.getWidth() * scale);
				int sh = (int) Math.ceil(thumbnail.getHeight() * scale);
				g.drawImage(thumbnail, (w - sw) / 2, 1 + (thumbHeight - sh) / 2, sw, sh, null);
				g.setClip(old);
			}
			if (isSelected()) {
				g.setColor(ToolStyle.BLUE);
				g.setStroke(new java.awt.BasicStroke(2.6f));
				g.draw(shape);
			} else if (getModel().isRollover()) {
				g.setColor(ToolStyle.withAlpha(getForeground(), 120));
				g.setStroke(new java.awt.BasicStroke(1.4f));
				g.draw(shape);
			}
			g.setFont(getFont().deriveFont(isSelected() ? Font.BOLD : Font.PLAIN, getFont().getSize2D() - 1f));
			g.setColor(isSelected() ? ToolStyle.BLUE : getForeground());
			fm = g.getFontMetrics();
			String text = getText();
			while (text.length() > 3 && fm.stringWidth(text) > w - 2) {
				text = text.substring(0, text.length() - 2) + "\u2026";
			}
			g.drawString(text, Math.max(0, (w - fm.stringWidth(text)) / 2), thumbHeight + 2 + fm.getAscent());
			g.dispose();
		}
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
		JPanel level = new JPanel(new MigLayout("insets 0, wrap 3", "[96!][grow,fill][30!,right]", ""));
		level.setOpaque(false);
		bind(level, "Straighten", -45, 45, 0, () -> settings.straighten, v -> settings.straighten = v);
		p.add(level, "span 2, growx");
		p.add(left);
		p.add(right);
		p.add(flipH);
		p.add(flipV);
		return p;
	}


	/** Holder of the accordion: as wide as the viewport (never wider, so nothing slips under the edge) */
	static class ScrollableHolder extends JPanel implements javax.swing.Scrollable {
		private static final long serialVersionUID = 1L;

		ScrollableHolder(java.awt.LayoutManager layout) {
			super(layout);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return orientation == javax.swing.SwingConstants.VERTICAL ? Math.max(16, visibleRect.height - 16) : visibleRect.width;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}


	/** Header badges: what is set in each section, visible with the section closed too */
	private void refreshBadges() {
		accordion.setBadge(SEC_CONVERT, settings.format.toUpperCase(java.util.Locale.ROOT) + ("jpg".equals(settings.format) ? " " + settings.quality : ""));
		String resize = "";
		if (settings.resizeMode != ImageSettings.ResizeMode.NONE) {
			if (baseWidth > 0) {
				int[] from = effectiveSize();
				int[] size = ImageEdit.targetSize(from[0], from[1], settings);
				resize = size[0] + " x " + size[1];
			} else {
				resize = settings.resizeMode.toString();
			}
		}
		accordion.setBadge(SEC_RESIZE, resize);
		int adjustments = (settings.brightness != 0 ? 1 : 0) + (settings.contrast != 0 ? 1 : 0) + (settings.saturation != 0 ? 1 : 0)
				+ (Math.abs(settings.gamma - 1.0) > 0.001 ? 1 : 0) + (settings.temperature != 0 ? 1 : 0) + (settings.hue != 0 ? 1 : 0)
				+ (settings.exposure != 0 ? 1 : 0) + (settings.shadows != 0 ? 1 : 0) + (settings.highlights != 0 ? 1 : 0)
				+ (settings.vibrance != 0 ? 1 : 0) + (settings.tint != 0 ? 1 : 0);
		int effects = (settings.sharpness != 0 ? 1 : 0) + (settings.blur != 0 ? 1 : 0) + (settings.vignette != 0 ? 1 : 0) + (settings.grain != 0 ? 1 : 0);
		accordion.setBadge(SEC_EFFECTS, effects > 0 ? effects + " on" : "");
		accordion.setBadge(SEC_CROP, !settings.hasCrop() ? "" : settings.cropRatio != ImageSettings.CropRatio.FREE ? settings.cropRatio.w + ":" + settings.cropRatio.h : "margins");
		int decoration = (settings.borderWidth > 0 ? 1 : 0) + (settings.corners > 0 ? 1 : 0) + (settings.hasWatermark() ? 1 : 0);
		accordion.setBadge(SEC_DECORATE, decoration > 0 ? decoration + " on" : "");
		accordion.setBadge(SEC_ADJUST, adjustments > 0 ? adjustments + " set" : "");
		accordion.setBadge(SEC_FILTERS, settings.filters.isEmpty() ? "" : settings.filters.size() + " on");
		String transform = (settings.straighten != 0 ? settings.straighten + "\u00b0 fine " : "") + (settings.rotation != 0 ? settings.rotation + "\u00b0 " : "") + (settings.flipH ? "H " : "") + (settings.flipV ? "V" : "");
		accordion.setBadge(SEC_TRANSFORM, transform.trim());
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
