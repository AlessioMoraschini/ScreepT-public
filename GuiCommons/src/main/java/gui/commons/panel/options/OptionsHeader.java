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
package gui.commons.panel.options;

import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

/**
 * Header of an options window: title on the left, search field on the right filtering an {@link OptionsPanel}.
 * Ctrl+F focuses the search; ESC in the search clears it first, then closes the window as its X button.
 */
public class OptionsHeader extends JPanel {

	private static final long serialVersionUID = -2286401436357436124L;

	private static final float TITLE_FONT_DELTA = 10f;

	private final JTextField searchField = new JTextField();

	public <W extends Window & RootPaneContainer> OptionsHeader(String title, OptionsPanel optionsPanel, W window) {

		super(new MigLayout("insets 14 18 10 18", "[]push[]"));

		Font base = UIManager.getFont("Label.font");
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + TITLE_FONT_DELTA));

		searchField.setFont(base.deriveFont(base.getSize2D() + OptionSection.OPTION_FONT_DELTA));
		searchField.putClientProperty("JTextField.placeholderText", "Search options (Ctrl+F)");
		searchField.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
		searchField.setToolTipText("Filters the shown options only: saving always saves all of them");
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				optionsPanel.filter(searchField.getText());
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				optionsPanel.filter(searchField.getText());
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				optionsPanel.filter(searchField.getText());
			}
		});

		searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearchOrClose");
		searchField.getActionMap().put("clearSearchOrClose", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (searchField.getText().isEmpty()) {
					window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
				} else {
					searchField.setText("");
				}
			}
		});
		window.getRootPane().registerKeyboardAction(e -> searchField.requestFocusInWindow(),
				KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

		add(titleLabel);
		add(searchField, "w 300!");
	}
}
