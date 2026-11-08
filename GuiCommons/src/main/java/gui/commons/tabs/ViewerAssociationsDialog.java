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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import gui.commons.frameutils.frame.arch.ToolStyle;
import net.miginfocom.swing.MigLayout;

/**
 * Chooses which viewer opens each file extension (text editor, image studio, LogSearch, HTML viewer...). The list
 * filters while you type, rows changed from the default are in bold, and only the changes are kept in the ini file.
 */
public class ViewerAssociationsDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private final Map<String, String> working = new LinkedHashMap<String, String>();
	private final List<String> allExtensions = new ArrayList<String>();
	private final List<String> shown = new ArrayList<String>();
	private final JTextField filter = new JTextField();
	private final JLabel hint = ToolStyle.mutedLabel("Double click a row to choose its viewer. Changed rows are in bold.");
	private final JTable table;
	private final Model model = new Model();
	private final List<String> viewerIds = new ArrayList<String>();
	private final List<String> viewerNames = new ArrayList<String>();

	/**
	 * @param onSave receives the text to keep in the ini file ("ext=viewer;..." with only the differences from the defaults)
	 */
	public ViewerAssociationsDialog(Component parent, final Consumer<String> onSave) {
		super(parent == null ? null : javax.swing.SwingUtilities.getWindowAncestor(parent), "Viewer associations", ModalityType.APPLICATION_MODAL);

		viewerIds.add(TabImplementors.TEXT_ID);
		viewerNames.add(TabImplementors.TEXT_NAME);
		for (TabImplementor implementor : TabImplementors.all()) {
			viewerIds.add(implementor.getId());
			viewerNames.add(implementor.getDisplayName());
		}
		allExtensions.addAll(TabImplementors.knownExtensions());
		for (String ext : allExtensions) {
			working.put(ext, TabImplementors.effectiveIdFor(ext));
		}

		setLayout(new MigLayout("fill, insets 14, gapy 8", "[grow,fill]", "[][][][grow,fill][][]"));
		add(ToolStyle.header("Viewer associations", "Choose how each type of file opens: in the text editor or in an integrated viewer tab"), "wrap");

		filter.putClientProperty("JTextField.placeholderText", "Filter by extension or viewer (type to narrow the list)");
		filter.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { refilter(); }
			@Override
			public void removeUpdate(DocumentEvent e) { refilter(); }
			@Override
			public void changedUpdate(DocumentEvent e) { refilter(); }
		});
		add(filter, "growx, wrap");
		add(hint, "wrap");

		table = new JTable(model);
		table.setRowHeight(28);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(0).setPreferredWidth(120);
		table.getColumnModel().getColumn(1).setPreferredWidth(240);
		table.getColumnModel().getColumn(2).setPreferredWidth(160);
		JComboBox<String> viewerCombo = new JComboBox<String>(viewerNames.toArray(new String[0]));
		table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(viewerCombo));
		// double click anywhere on a row (or one click on the viewer column) opens the list of viewers
		table.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				int column = table.columnAtPoint(e.getPoint());
				if (row < 0 || !(e.getClickCount() == 2 || column == 1)) {
					return;
				}
				table.setRowSelectionInterval(row, row);
				if (table.editCellAt(row, 1)) {
					javax.swing.SwingUtilities.invokeLater(() -> {
						viewerCombo.requestFocusInWindow();
						viewerCombo.showPopup();
					});
				}
			}
		});
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
				super.getTableCellRendererComponent(t, value, selected, focus, row, column);
				String ext = shown.get(row);
				boolean changed = !working.get(ext).equals(TabImplementors.defaultIdFor(ext));
				setFont(getFont().deriveFont(changed && column < 2 ? Font.BOLD : Font.PLAIN));
				if (column == 2 && !selected) {
					setForeground(ToolStyle.muted());
				}
				return this;
			}
		};
		table.getColumnModel().getColumn(0).setCellRenderer(renderer);
		// the viewer column looks like a drop down, so it is clear that it can be changed
		final JComboBox<String> comboLook = new JComboBox<String>(viewerNames.toArray(new String[0]));
		table.getColumnModel().getColumn(1).setCellRenderer((t, value, selected, focus, row, column) -> {
			comboLook.setSelectedItem(value);
			String ext = shown.get(row);
			boolean changed = !working.get(ext).equals(TabImplementors.defaultIdFor(ext));
			comboLook.setFont(comboLook.getFont().deriveFont(changed ? Font.BOLD : Font.PLAIN));
			return comboLook;
		});
		table.getColumnModel().getColumn(2).setCellRenderer(renderer);
		add(new JScrollPane(table), "grow, wrap");

		final JTextField newExtension = new JTextField();
		newExtension.putClientProperty("JTextField.placeholderText", "e.g. md");
		newExtension.setToolTipText("Type an extension that is not listed (for example md or conf), then press Enter or Add");
		final Runnable addExtension = () -> {
			String ext = newExtension.getText().trim().toLowerCase(Locale.ROOT).replace(".", "").replace("*", "");
			if (ext.isEmpty()) {
				hint.setText("Type an extension first (for example md), then press Add.");
				return;
			}
			if (!working.containsKey(ext)) {
				allExtensions.add(ext);
				java.util.Collections.sort(allExtensions);
				working.put(ext, TabImplementors.defaultIdFor(ext));
				hint.setText(".{ext} added: choose its viewer in the list.".replace("{ext}", ext));
			} else {
				hint.setText(".{ext} is already in the list: choose its viewer.".replace("{ext}", ext));
			}
			newExtension.setText("");
			filter.setText(ext);
			refilter();
			int row = shown.indexOf(ext);
			if (row >= 0) {
				table.setRowSelectionInterval(row, row);
				if (table.editCellAt(row, 1)) {
					javax.swing.SwingUtilities.invokeLater(() -> {
						viewerCombo.requestFocusInWindow();
						viewerCombo.showPopup();
					});
				}
			}
		};
		newExtension.addActionListener(e -> addExtension.run());
		JButton add = ToolStyle.button("Add", null, "Adds an extension that is not in the list, so you can choose its viewer");
		add.addActionListener(e -> addExtension.run());
		JButton reset = ToolStyle.button("Reset to defaults", null, "Every extension goes back to its default viewer");
		reset.addActionListener(e -> {
			for (String ext : allExtensions) {
				working.put(ext, TabImplementors.defaultIdFor(ext));
			}
			model.fireTableDataChanged();
		});
		JButton ok = ToolStyle.accent(ToolStyle.button("Save", null, "Apply the choices and keep them"), ToolStyle.GREEN);
		ok.addActionListener(e -> {
			if (table.isEditing()) {
				table.getCellEditor().stopCellEditing();
			}
			TabImplementors.setOverrides(working);
			onSave.accept(TabImplementors.serializeOverrides());
			dispose();
		});
		JButton cancel = ToolStyle.button("Cancel", null, "Close without changing anything");
		cancel.addActionListener(e -> dispose());

		// extra extensions: a labelled row of its own, so it is clear what the field is for
		javax.swing.JPanel extra = new javax.swing.JPanel(new MigLayout("insets 0, gapx 8", "[][180!][]", "[]"));
		extra.setOpaque(false);
		extra.add(ToolStyle.mutedLabel("Extension missing from the list?"));
		extra.add(newExtension);
		extra.add(add);
		add(extra, "growx, wrap");

		javax.swing.JPanel footer = new javax.swing.JPanel(new MigLayout("insets 0, gapx 8", "[][grow,fill][][]", "[]"));
		footer.setOpaque(false);
		footer.add(reset);
		footer.add(new JLabel(), "growx");
		footer.add(cancel);
		footer.add(ok);
		add(footer, "growx");

		refilter();
		setMinimumSize(new Dimension(560, 460));
		setSize(640, 560);
		setLocationRelativeTo(parent);
	}

	private void refilter() {
		String needle = filter.getText().trim().toLowerCase(Locale.ROOT);
		shown.clear();
		for (String ext : allExtensions) {
			if (needle.isEmpty() || ext.contains(needle) || nameOf(working.get(ext)).toLowerCase(Locale.ROOT).contains(needle)) {
				shown.add(ext);
			}
		}
		model.fireTableDataChanged();
	}

	private String nameOf(String id) {
		int index = viewerIds.indexOf(id);
		return index < 0 ? id : viewerNames.get(index);
	}

	private class Model extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private final String[] columns = {"Extension", "Opens with", "Default"};

		@Override
		public int getRowCount() {
			return shown.size();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public String getColumnName(int column) {
			return columns[column];
		}

		@Override
		public Object getValueAt(int row, int column) {
			String ext = shown.get(row);
			switch (column) {
			case 0:
				return "." + ext;
			case 1:
				return nameOf(working.get(ext));
			default:
				return nameOf(TabImplementors.defaultIdFor(ext));
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 1;
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			int index = viewerNames.indexOf(String.valueOf(value));
			if (column == 1 && index >= 0) {
				working.put(shown.get(row), viewerIds.get(index));
				fireTableRowsUpdated(row, row);
			}
		}
	}

	/** Small helper for callers that only need the label */
	public static JLabel hint() {
		return ToolStyle.mutedLabel("Only the choices that differ from the defaults are kept in the ini file");
	}
}
