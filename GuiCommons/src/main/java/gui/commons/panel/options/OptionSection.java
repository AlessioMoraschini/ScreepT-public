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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

/**
 * Collapsible titled group of options: labeled rows (label + controls), a responsive grid of check boxes
 * (columns follow the available width) and nested subsections. Click on the title to collapse/expand.
 */
public class OptionSection extends JPanel {

	private static final long serialVersionUID = -3410938611958367027L;

	/** Options text is a bit bigger than the default LAF one, for readability */
	static final float OPTION_FONT_DELTA = 2f;
	private static final float TITLE_FONT_DELTA = 8f;
	private static final float SUBTITLE_FONT_DELTA = 4f;
	private static final String EXPANDED_ICON = "▾";
	private static final String COLLAPSED_ICON = "▸";

	private static final int CHECK_MIN_COLUMN_WIDTH = 280;
	private static final int CHECK_MAX_COLUMNS = 4;

	private final String searchableTitle;
	private final int level;
	private final JLabel arrowLabel = new JLabel(EXPANDED_ICON);
	private final JPanel contentPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 0, gapy 12, hidemode 3", "[grow,fill]"));
	private final JPanel rowsPanel = new JPanel(new MigLayout("wrap 2, fillx, insets 0, gap 20 10, hidemode 3", "[right][grow,fill]"));
	private final JPanel checksPanel = new JPanel(new MigLayout(checksLayout(3), checksColumns(3)));
	private final List<Item> items = new ArrayList<>();
	private final List<OptionSection> subsections = new ArrayList<>();
	private int checkColumns = 3;
	private boolean collapsed;

	OptionSection(String title, String description, int level) {

		super(new MigLayout("wrap 1, fillx, insets 0, gapy 8, hidemode 3", "[grow,fill]"));
		setOpaque(false);
		this.level = level;
		searchableTitle = OptionsPanel.normalize(title + " " + (description == null ? "" : description));

		add(createHeader(title, description), "growx");

		rowsPanel.setOpaque(false);
		checksPanel.setOpaque(false);
		contentPanel.setOpaque(false);
		contentPanel.add(rowsPanel, "growx");
		contentPanel.add(checksPanel, "growx");
		add(contentPanel, "growx, gapleft " + (level == 0 ? 12 : 26));

		// responsive check boxes grid
		checksPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				updateCheckColumns();
			}
		});
	}

	private JPanel createHeader(String title, String description) {

		JPanel header = new JPanel(new MigLayout("wrap 1, fillx, insets 0, gapy 2", "[grow,fill]"));
		header.setOpaque(false);
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setToolTipText("Click to collapse or expand");

		Font base = UIManager.getFont("Label.font");
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + (level == 0 ? TITLE_FONT_DELTA : SUBTITLE_FONT_DELTA)));
		arrowLabel.setFont(titleLabel.getFont());

		header.add(arrowLabel, "split 3, gapright 6");
		header.add(titleLabel, "gapright 12");
		header.add(new JSeparator(), "growx, aligny center");

		if (description != null && !description.isEmpty()) {
			JLabel descriptionLabel = new JLabel(description);
			descriptionLabel.setFont(base.deriveFont(base.getSize2D() + OPTION_FONT_DELTA - 1f));
			descriptionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
			header.add(descriptionLabel, "gapleft 24");
		}

		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				setCollapsed(!collapsed);
			}
		});
		return header;
	}

	/** Nested section, shown indented inside this one (e.g. "Editing" inside "Text editor") */
	public OptionSection addSubsection(String title, String description) {
		OptionSection subsection = new OptionSection(title, description, level + 1);
		subsections.add(subsection);
		contentPanel.add(subsection, "growx, gaptop 6");
		return subsection;
	}

	/** Labeled row: label on the left, controls (combo, spinner, slider, buttons...) on the right */
	public OptionSection addRow(String label, JComponent... controls) {

		JLabel rowLabel = new JLabel(label);
		enlargeFont(rowLabel);
		JPanel controlsPanel = new JPanel(new MigLayout("insets 0, gap 8, hidemode 3"));
		controlsPanel.setOpaque(false);
		for (JComponent control : controls) {
			enlargeFont(control);
			controlsPanel.add(control, constraintsFor(control));
		}
		rowLabel.setLabelFor(controls.length > 0 ? controls[0] : null);

		rowsPanel.add(rowLabel);
		rowsPanel.add(controlsPanel, "growx");
		items.add(new Item(searchText(label, controls), rowLabel, controlsPanel));
		return this;
	}

	public OptionSection addCheckBoxes(AbstractButton... checkBoxes) {
		for (AbstractButton checkBox : checkBoxes) {
			enlargeFont(checkBox);
			checksPanel.add(checkBox, "growx");
			items.add(new Item(searchText(checkBox.getText(), checkBox), checkBox));
		}
		return this;
	}

	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
		arrowLabel.setText(collapsed ? COLLAPSED_ICON : EXPANDED_ICON);
		contentPanel.setVisible(!collapsed);
		revalidate();
		repaint();
	}

	/**
	 * @return true if something is still visible in this section. While searching, sections with matches are shown
	 * expanded; with an empty query the collapsed state chosen by the user is restored.
	 */
	boolean filter(String query) {
		boolean searching = !query.isEmpty();
		boolean showAll = !searching || searchableTitle.contains(query);

		boolean anyVisible = false;
		for (Item item : items) {
			boolean visible = showAll || item.searchText.contains(query);
			item.setVisible(visible);
			anyVisible |= visible;
		}
		for (OptionSection subsection : subsections) {
			anyVisible |= subsection.filter(showAll && searching ? "" : query);
		}

		contentPanel.setVisible(searching ? anyVisible : !collapsed);
		arrowLabel.setText(contentPanel.isVisible() ? EXPANDED_ICON : COLLAPSED_ICON);
		setVisible(anyVisible || showAll);
		return anyVisible || showAll;
	}

	private void updateCheckColumns() {
		int columns = Math.max(1, Math.min(CHECK_MAX_COLUMNS, checksPanel.getWidth() / CHECK_MIN_COLUMN_WIDTH));
		if (columns != checkColumns) {
			checkColumns = columns;
			MigLayout layout = (MigLayout) checksPanel.getLayout();
			layout.setLayoutConstraints(checksLayout(columns));
			layout.setColumnConstraints(checksColumns(columns));
			checksPanel.revalidate();
		}
	}

	static void enlargeFont(JComponent component) {
		Font font = component.getFont() != null ? component.getFont() : UIManager.getFont("Label.font");
		Font bigger = font.deriveFont(font.getSize2D() + OPTION_FONT_DELTA);
		component.setFont(bigger);
		if (component instanceof JSpinner && ((JSpinner) component).getEditor() instanceof JSpinner.DefaultEditor) {
			((JSpinner.DefaultEditor) ((JSpinner) component).getEditor()).getTextField().setFont(bigger);
		}
	}

	private static String checksLayout(int columns) {
		return "wrap " + columns + ", fillx, insets 0, gap 20 8, hidemode 3";
	}

	private static String checksColumns(int columns) {
		StringBuilder constraints = new StringBuilder();
		for (int i = 0; i < columns; i++) {
			constraints.append("[sizegroup checks, grow, fill]");
		}
		return constraints.toString();
	}

	private static String constraintsFor(JComponent control) {
		if (control instanceof JSpinner) {
			return "w 100!";
		}
		if (control instanceof JComboBox || control instanceof JSlider || control instanceof JTextField) {
			return "growx, pushx, wmin 180";
		}
		return "";
	}

	private static String searchText(String label, JComponent... components) {
		StringBuilder text = new StringBuilder(label == null ? "" : label);
		for (JComponent component : components) {
			if (component instanceof AbstractButton) {
				text.append(' ').append(((AbstractButton) component).getText());
			}
			if (component.getToolTipText() != null) {
				text.append(' ').append(component.getToolTipText());
			}
		}
		return OptionsPanel.normalize(text.toString());
	}

	/** One searchable option: the components shown or hidden together */
	private static final class Item {
		private final String searchText;
		private final List<Component> components;

		Item(String searchText, Component... components) {
			this.searchText = searchText;
			this.components = Arrays.asList(components);
		}

		void setVisible(boolean visible) {
			for (Component component : components) {
				component.setVisible(visible);
			}
		}
	}
}
