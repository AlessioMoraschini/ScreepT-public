package various.common.light.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.List;

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
import javax.swing.border.LineBorder;

import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.gui.dnd.FileDrop;
import various.common.light.gui.dnd.FileDropListener;
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

	public static JTextArea getFileTextArea(FileDropListener listener) {
		JTextArea txtFieldSelectedFiles = new JTextArea("\n Nothing selected (drop your files here to add them)");
		txtFieldSelectedFiles.setMargin(new Insets(8, 8, 8, 8));
		txtFieldSelectedFiles.setMinimumSize(new Dimension(320, 70));
		txtFieldSelectedFiles.setPreferredSize(new Dimension(320, 70));
		txtFieldSelectedFiles.setFont(ScreepTGuiFactory.DEFAULT_TEXT_AREA_FONT.deriveFont(17f));
		txtFieldSelectedFiles.setEditable(false);
		txtFieldSelectedFiles.setAutoscrolls(true);
		txtFieldSelectedFiles.setBorder(new LineBorder(Color.DARK_GRAY));
		new FileDrop(txtFieldSelectedFiles, listener);

		return txtFieldSelectedFiles;
	}

	public static void setSelectedFileLbl(List<File> selected, JTextArea txtFieldSelectedFiles) {
		if (selected == null || selected.isEmpty() || !selected.get(0).exists()) {
			txtFieldSelectedFiles.setText("\n Nothing selected (drag your files here)");
			txtFieldSelectedFiles.setBackground(new Color(252, 240, 146));
		} else {
			String fileConcatNames = "";
			for(File iterator : selected) {
				String identifier = iterator.isDirectory() ? "<b>Directory: </b>" : "<b>File: </b>";
				fileConcatNames += identifier + iterator + "<br>";
			}

			txtFieldSelectedFiles.setBackground(new Color(208, 252, 146));
			txtFieldSelectedFiles.setText(getStringSourcesSummary(selected));
			txtFieldSelectedFiles.setToolTipText(GuiUtils.encapsulateInHtml(fileConcatNames));
		}
	}

	protected static String getStringSourcesSummary(List<File> selected) {
		String outcome = "\n Nothing selected (drag your files here)";
		int directories = 0;
		int files = 0;
		if(selected != null) {
			for(File f : selected) {
				if(f != null && f.isDirectory())
					directories ++;
				else if(f != null)
					files++;
			}
		}

		outcome = " Files selected: " + files + "\n";
		outcome += " Directories selected: " + directories + "\n";
		outcome += " (Hover this textArea to see details)";

		return outcome;
	}

	public static String nCellsLayoutStr(String layoutStr, int nTimes) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < nTimes; i++) {
			builder.append(layoutStr);
		}

		return builder.toString();
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
