package various.common.light.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.log.SafeLogger;

public class ScreepTGuiFactory {

	public static SafeLogger logger = new SafeLogger(ScreepTGuiFactory.class);

	public static final Font DEFAULT_TEXT_AREA_FONT = new Font("Segoe UI", Font.PLAIN, 17);
	public static final Font DEFAULT_BTN_FONT = new Font("Segoe UI", Font.PLAIN, 18);

	public static JRadioButton getRadioButton(String text, String tooltip, boolean opaque) {
		JRadioButton checkBox = new JRadioButton();
		checkBox.setText(text);
		checkBox.setOpaque(opaque);
		checkBox.setCursor(GuiUtils.CURSOR_HAND);
		checkBox.setToolTipText(tooltip);
		checkBox.setFont(DEFAULT_TEXT_AREA_FONT.deriveFont(13f));

		return checkBox;
	}

	public static JCheckBox getCheckBox(String text, String tooltip, boolean opaque) {
		JCheckBox checkBox = new JCheckBox();
		checkBox.setText(text);
		checkBox.setOpaque(opaque);
		checkBox.setCursor(GuiUtils.CURSOR_HAND);
		checkBox.setToolTipText(tooltip);
		checkBox.setFont(DEFAULT_TEXT_AREA_FONT.deriveFont(13f));

		return checkBox;
	}

	public static JScrollPane getScrollPane(boolean verticalScroll, boolean horizontalScroll) {
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		scrollPane.setBackground(Color.black);
		scrollPane.setBackground(Color.DARK_GRAY);
		scrollPane.setForeground(Color.LIGHT_GRAY);

		if (verticalScroll)
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		else
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		if (horizontalScroll)
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		else
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		return scrollPane;
	}

	// ######### COMMON JComponent factory methods ########### //
	public static JButton getButton(String text, Color background, Color foreground, String iconPath, boolean makeTransparent) {
		JButton button = new JButton(text);
		button.setCursor(GuiUtils.CURSOR_HAND);
		button.setFont(DEFAULT_BTN_FONT);
		button.setIcon(new ImageIcon(iconPath));

		if (background != null)
			button.setBackground(background);

		if (foreground != null)
			button.setForeground(foreground);

		if (makeTransparent)
			GuiUtils.makeTransparentJButton(button);

		return button;
	}

	public static JLabel getLabel(String text, Color background, Color foreground, String iconPath, boolean opaque) {
		JLabel label = new JLabel(text);

		label.setFont(DEFAULT_TEXT_AREA_FONT.deriveFont(14f));

		if (iconPath != null)
			label.setIcon(new ImageIcon(iconPath));

		if (background != null)
			label.setBackground(background);

		if (foreground != null)
			label.setForeground(foreground);

		label.setOpaque(opaque);

		return label;
	}

	public static JTextArea getTextArea(JTextArea toEnrich, Color background, Color foreground, Color caretColor, Font font) {
		JTextArea textArea = toEnrich != null ? toEnrich : new JTextArea();
		textArea.setMargin(new Insets(4, 4, 4, 4));

		if (font != null)
			textArea.setFont(font);

		if (foreground != null)
			textArea.setForeground(foreground);

		if (background != null)
			textArea.setBackground(background);

		if (caretColor != null)
			textArea.setCaretColor(caretColor);

		return textArea;
	}

	public static JTextArea getTextArea(Color background, Color foreground, Color caretColor, Font font) {
		return getTextArea(null, background, foreground, caretColor, font);
	}

	public static JTextField getTextField(Color background, Color foreground, Color caretColor, Font font, boolean addAutoSelectMouseListener) {

		JTextField textField = new JTextField();
		textField.setMargin(new Insets(4, 4, 4, 4));

		if (font != null)
			textField.setFont(font);

		if (foreground != null)
			textField.setForeground(foreground);

		if (background != null)
			textField.setBackground(background);

		if (caretColor != null)
			textField.setCaretColor(caretColor);

		if (addAutoSelectMouseListener) {
			textField.addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (textField.getSelectedText() == null || "".equals(textField.getSelectedText())) {
						textField.selectAll();
					}
				}

				@Override
				public void mousePressed(MouseEvent e) {
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseClicked(MouseEvent e) {
				}
			});
		}

		return textField;
	}

	public static JButton getPinToTopButton(JFrame targetFrame, String iconFilePath) {
		JButton pinToTopButton = getButton("", null, null, iconFilePath, true);
		pinToTopButton.setToolTipText("Enable \"always on top\" over other windows (if supported)");

		pinToTopButton.addActionListener((e) -> {
			if (!targetFrame.isAlwaysOnTop()) {

				if (!targetFrame.isAlwaysOnTopSupported()) {
					logger.warn("User tried to set Always on top, but it seems it's not supported in this device!");
					new JOptionHelper(targetFrame).warn("It seems that \"Always on top\" is not supported on this system :(",
							"\"Always on top\" not supported");
				}

				targetFrame.setAlwaysOnTop(true);
				pinToTopButton.setIcon(new ImageIcon(iconFilePath));
				pinToTopButton.setToolTipText("Disable \"always on top\" over other windows");

			} else {
				targetFrame.setAlwaysOnTop(false);
				pinToTopButton.setIcon(new ImageIcon(iconFilePath));
				pinToTopButton.setToolTipText("Enable \"always on top\" over other windows (if supported)");
			}
		});

		return pinToTopButton;
	}

}
