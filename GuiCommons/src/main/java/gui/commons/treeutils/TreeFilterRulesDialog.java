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
package gui.commons.treeutils;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import gui.commons.imagestudio.DialogFit;
import gui.commons.treeutils.TreeFilterRules.Kind;
import gui.commons.treeutils.TreeFilterRules.Rule;
import various.common.light.i18n.I18n;

/**
 * Edits the {@link TreeFilterRules} of the file tree filter: add, remove, switch on and off, change the name or wildcard
 * (*.java, TEST*.rar...) and the kind of entry (folder, file, symbolic link, any) of every rule.
 */
public final class TreeFilterRulesDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final TreeFilterRules working;
	private boolean confirmed;
	private final JTable table;
	private final RulesModel model;

	private static String kindLabel(Kind kind) {
		switch (kind) {
		case FOLDER:
			return I18n.t("tree.rules_kind_folder");
		case FILE:
			return I18n.t("tree.rules_kind_file");
		case SYMLINK:
			return I18n.t("tree.rules_kind_symlink");
		default:
			return I18n.t("tree.rules_kind_any");
		}
	}

	private static Kind kindOf(Object label) {
		for (Kind k : Kind.values()) {
			if (kindLabel(k).equals(label)) {
				return k;
			}
		}
		return Kind.ANY;
	}

	/** Table of the rules: on/off, name or wildcard, kind */
	private final class RulesModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private final String[] titles = { I18n.t("tree.rules_col_on"), I18n.t("tree.rules_col_pattern"), I18n.t("tree.rules_col_kind") };

		private List<Rule> rules() {
			return working.getRules();
		}

		@Override
		public int getRowCount() {
			return rules().size();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public String getColumnName(int column) {
			return titles[column];
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return column == 0 ? Boolean.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return true;
		}

		@Override
		public Object getValueAt(int row, int column) {
			Rule r = rules().get(row);
			return column == 0 ? (Object) r.enabled : column == 1 ? r.pattern : kindLabel(r.kind);
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			Rule r = rules().get(row);
			if (column == 0) {
				r.enabled = Boolean.TRUE.equals(value);
			} else if (column == 1) {
				r.pattern = value == null ? "" : value.toString().trim();
			} else {
				r.kind = kindOf(value);
			}
			fireTableRowsUpdated(row, row);
		}

		void added() {
			fireTableRowsInserted(rules().size() - 1, rules().size() - 1);
		}

		void changedAll() {
			fireTableDataChanged();
		}
	}

	private TreeFilterRulesDialog(Window owner, TreeFilterRules rules) {
		super(owner, I18n.t("tree.rules_title"), Dialog.ModalityType.APPLICATION_MODAL);
		this.working = rules;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JLabel intro = new JLabel("<html><body style='width:420px'>" + I18n.t("tree.rules_intro") + "</body></html>");
		intro.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));

		model = new RulesModel();
		table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(Math.max(table.getRowHeight(), 24));
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(0).setMaxWidth(50);
		table.getColumnModel().getColumn(2).setPreferredWidth(130);
		table.getColumnModel().getColumn(2).setMaxWidth(190);
		JComboBox<String> kinds = new JComboBox<String>();
		for (Kind k : Kind.values()) {
			kinds.addItem(kindLabel(k));
		}
		table.getColumnModel().getColumn(2).setCellEditor(new javax.swing.DefaultCellEditor(kinds));
		JCheckBox masterBox = new JCheckBox(I18n.t("tree.rules_master"));
		masterBox.setSelected(working.isMasterOn());
		masterBox.addActionListener(e -> working.setMasterOn(masterBox.isSelected()));
		JCheckBox selectAll = new JCheckBox(I18n.t("tree.rules_select_all"));
		selectAll.setSelected(allOn());
		selectAll.addActionListener(e -> {
			stopEditing();
			boolean on = !allOn();
			for (Rule r : working.getRules()) {
				r.enabled = on;
			}
			model.changedAll();
		});
		model.addTableModelListener(e -> selectAll.setSelected(allOn()));
		JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		options.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
		options.add(masterBox);
		options.add(selectAll);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
		scroll.setPreferredSize(new java.awt.Dimension(520, 260));

		JButton add = new JButton(I18n.t("tree.rules_add"));
		add.addActionListener(e -> {
			stopEditing();
			working.getRules().add(new Rule(true, I18n.t("tree.rules_new_pattern"), Kind.FOLDER));
			model.added();
			int row = working.getRules().size() - 1;
			table.getSelectionModel().setSelectionInterval(row, row);
			table.editCellAt(row, 1);
			table.scrollRectToVisible(table.getCellRect(row, 1, true));
			if (table.getEditorComponent() != null) {
				table.getEditorComponent().requestFocusInWindow();
				if (table.getEditorComponent() instanceof javax.swing.text.JTextComponent) {
					((javax.swing.text.JTextComponent) table.getEditorComponent()).selectAll();
				}
			}
		});
		JButton remove = new JButton(I18n.t("tree.rules_remove"));
		remove.addActionListener(e -> {
			stopEditing();
			int row = table.getSelectedRow();
			if (row >= 0) {
				working.getRules().remove(row);
				model.changedAll();
			}
		});
		JButton restore = new JButton(I18n.t("tree.rules_restore"));
		restore.addActionListener(e -> {
			stopEditing();
			working.getRules().clear();
			working.getRules().addAll(TreeFilterRules.defaults().getRules());
			working.setMasterOn(true);
			masterBox.setSelected(true);
			model.changedAll();
		});
		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
		left.add(add);
		left.add(remove);
		left.add(restore);

		JButton save = new JButton(I18n.t("tree.rules_save"));
		save.addActionListener(e -> {
			stopEditing();
			// empty patterns mean nothing: dropped
			working.getRules().removeIf(r -> r.pattern.trim().isEmpty());
			confirmed = true;
			dispose();
		});
		JButton cancel = new JButton(I18n.t("tree.rules_cancel"));
		cancel.addActionListener(e -> dispose()); // cancelled by user
		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
		right.add(save);
		right.add(cancel);

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(left, BorderLayout.WEST);
		bottom.add(right, BorderLayout.EAST);

		JPanel content = new JPanel(new BorderLayout());
		JPanel head = new JPanel(new BorderLayout());
		head.add(intro, BorderLayout.NORTH);
		head.add(options, BorderLayout.SOUTH);
		content.add(head, BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);
		content.add(bottom, BorderLayout.SOUTH);
		setContentPane(content);
		getRootPane().setDefaultButton(save);
		getRootPane().registerKeyboardAction(e -> dispose(), javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
				javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
		DialogFit.apply(this, owner, 560, 400);
	}

	private boolean allOn() {
		List<Rule> all = working.getRules();
		if (all.isEmpty()) {
			return false;
		}
		for (Rule r : all) {
			if (!r.enabled) {
				return false;
			}
		}
		return true;
	}

	private void stopEditing() {
		if (table.isEditing()) {
			table.getCellEditor().stopCellEditing();
		}
	}

	/**
	 * Shows the dialog on a working copy of the rules.
	 *
	 * @return the edited rules, or null when the user cancelled
	 */
	public static TreeFilterRules edit(Window owner, TreeFilterRules workingCopy) {
		TreeFilterRulesDialog dialog = new TreeFilterRulesDialog(owner, workingCopy);
		dialog.setVisible(true);
		return dialog.confirmed ? dialog.working : null;
	}
}
