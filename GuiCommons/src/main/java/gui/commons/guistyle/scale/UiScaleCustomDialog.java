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
package gui.commons.guistyle.scale;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.function.IntConsumer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

import initializer.configs.impl.om.GuiOption;
import initializer.configs.impl.om.UiSizePreset;
import net.miginfocom.swing.MigLayout;

/**
 * Shows and edits the interface size values of a GuiOption. Every change switches the preset to CUSTOM
 * and notifies the caller (debounced), which applies it as preview. Saving/discarding is up to the caller.
 */
public class UiScaleCustomDialog extends JDialog {

	private static final long serialVersionUID = -2865410921934126514L;

	private static final String DEFAULT_FAMILY_ITEM = "(default)";
	private static final int MIN_PERCENT = 50;
	private static final int MAX_PERCENT = 200;
	private static final int STEP_PERCENT = 5;
	private static final int PREVIEW_DELAY_MS = 150;

	private final GuiOption guiOpt;
	private final Timer previewTimer;

	public UiScaleCustomDialog(Window owner, GuiOption guiOpt, Runnable onChange) {

		super(owner, "Interface size - custom values", ModalityType.DOCUMENT_MODAL);
		this.guiOpt = guiOpt;

		previewTimer = new Timer(PREVIEW_DELAY_MS, e -> onChange.run());
		previewTimer.setRepeats(false);

		JPanel content = new JPanel(new MigLayout("insets 16, gap 12 10, wrap 2", "[right][200px,grow,fill]"));

		content.add(new JLabel("Font family"));
		content.add(createFamilyCombo());

		addPercentSpinner(content, "General text %", guiOpt.getUiGeneralFontScale(), guiOpt::setUiGeneralFontScale);
		addPercentSpinner(content, "Menu %", guiOpt.getUiMenuFontScale(), guiOpt::setUiMenuFontScale);
		addPercentSpinner(content, "Toolbar %", guiOpt.getUiToolbarFontScale(), guiOpt::setUiToolbarFontScale);
		JSpinner componentSpinner = addPercentSpinner(content, "Graphic elements %", guiOpt.getUiComponentScale(), guiOpt::setUiComponentScale);
		componentSpinner.setToolTipText("Sizes of controls, rows, borders: fully applied with ScreepT Dark style");

		JButton btnClose = new JButton("Close");
		btnClose.addActionListener(e -> dispose());
		content.add(btnClose, "span 2, right, gaptop 8");

		setContentPane(content);
		getRootPane().setDefaultButton(btnClose);
		pack();
		setResizable(false);
		setLocationRelativeTo(owner);
	}

	private JComboBox<String> createFamilyCombo() {

		JComboBox<String> combo = new JComboBox<>();
		combo.addItem(DEFAULT_FAMILY_ITEM);
		for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
			combo.addItem(family);
		}
		combo.setMaximumRowCount(20);
		combo.setSelectedItem(guiOpt.getUiFontFamily().isEmpty() ? DEFAULT_FAMILY_ITEM : guiOpt.getUiFontFamily());

		combo.addActionListener(e -> {
			String selected = (String) combo.getSelectedItem();
			guiOpt.setUiFontFamily(DEFAULT_FAMILY_ITEM.equals(selected) ? GuiOption.DEFAULT_UI_FONT_FAMILY : selected);
			valueChanged();
		});
		return combo;
	}

	private JSpinner addPercentSpinner(JPanel content, String label, int value, IntConsumer setter) {

		int safeValue = Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, value));
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(safeValue, MIN_PERCENT, MAX_PERCENT, STEP_PERCENT));
		spinner.addChangeListener(e -> {
			setter.accept((Integer) spinner.getValue());
			valueChanged();
		});

		content.add(new JLabel(label));
		content.add(spinner);
		return spinner;
	}

	private void valueChanged() {
		guiOpt.setUiSizePreset(UiSizePreset.CUSTOM);
		previewTimer.restart();
	}
}
