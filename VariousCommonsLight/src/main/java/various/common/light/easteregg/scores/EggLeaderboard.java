/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.easteregg.scores;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import various.common.light.gui.ScreenMemory;

/**
 * The window of the easter egg leaderboard, opened from the "Leaderboard" entry on the top right of every game.
 * One game at a time, the ten best scores of this installation.
 */
public final class EggLeaderboard {

	private static final Color BACKGROUND = new Color(0x101418);
	private static final Color PANEL = new Color(0x171C22);
	private static final Color TEAL = new Color(0x2FB5A8);
	private static final Color TEXT = new Color(0xDDF5F1);
	private static final Color MUTED = new Color(0x6FA39C);
	private static final Color AMBER = new Color(0xF2B84B);

	private static JDialog open;

	private EggLeaderboard() {
	}

	/**
	 * The entry for the menu bar of a game window, pushed to the top right by a glue: add it as the last item of the bar,
	 * after calling {@code menuBar.add(Box.createHorizontalGlue())} (or use {@link #addTo(JMenuBar, Component, String)}).
	 */
	public static JMenuItem menuItem(Supplier<Component> owner, String gameId) {
		JMenuItem item = new JMenuItem("Leaderboard");
		item.setMaximumSize(item.getPreferredSize());
		item.setToolTipText("The best scores of the easter egg games, saved on this computer");
		item.addActionListener(e -> show(owner.get(), gameId));
		return item;
	}

	/** Puts the leaderboard entry on the top right of a menu bar */
	public static void addTo(JMenuBar menuBar, Component owner, String gameId) {
		menuBar.add(javax.swing.Box.createHorizontalGlue());
		menuBar.add(menuItem(() -> owner, gameId));
	}

	/** Opens the leaderboard (a second call brings the open one to the front, showing the asked game) */
	public static void show(Component owner, String gameId) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> show(owner, gameId));
			return;
		}
		if (open != null && open.isDisplayable()) {
			open.toFront();
			return;
		}
		Window window = owner instanceof Window ? (Window) owner : (owner != null ? SwingUtilities.getWindowAncestor(owner) : null);
		JDialog dialog = new JDialog(window, "Easter egg leaderboard", JDialog.ModalityType.APPLICATION_MODAL);
		open = dialog;
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				open = null;
			}
		});
		// ESC is also a game key elsewhere: here it only closes this dialog
		dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		dialog.getRootPane().putClientProperty("screept.escapeToClose.disabled", Boolean.TRUE);
		dialog.setContentPane(buildContent(dialog, gameId));
		dialog.setMinimumSize(new Dimension(560, 380));
		dialog.setPreferredSize(new Dimension(680, 460));
		dialog.pack();
		if (window != null) {
			dialog.setLocationRelativeTo(window);
		} else {
			ScreenMemory.centerOnLastScreen(dialog);
		}
		dialog.setVisible(true);
	}

	private static JPanel buildContent(JDialog dialog, final String preselected) {
		JPanel root = new JPanel(new BorderLayout(0, 0));
		root.setBackground(BACKGROUND);

		JLabel title = new JLabel("  LEADERBOARD  -  the ten best runs of this installation");
		title.setFont(mono(Font.BOLD, 15));
		title.setForeground(TEAL);
		title.setOpaque(true);
		title.setBackground(PANEL);
		title.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
		root.add(title, BorderLayout.NORTH);

		List<EggScores.Game> games = EggScores.games();
		JList<EggScores.Game> list = new JList<>(games.toArray(new EggScores.Game[0]));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setBackground(PANEL);
		list.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
				EggScores.Game game = (EggScores.Game) value;
				int best = EggScores.best(game.id);
				// a game not discovered yet (no score) stays a mystery, except the one that is being played now
				String shown = !isRevealed(game.id, preselected) ? "???" : game.name + (best > 0 ? "   " + best : "");
				JLabel label = (JLabel) super.getListCellRendererComponent(l, shown, index, selected, false);
				label.setFont(mono(Font.PLAIN, 13));
				label.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
				label.setForeground(selected ? BACKGROUND : TEXT);
				label.setBackground(selected ? TEAL : PANEL);
				return label;
			}
		});

		ScoresModel model = new ScoresModel();
		JTable table = new JTable(model);
		table.setBackground(BACKGROUND);
		table.setForeground(TEXT);
		table.setGridColor(new Color(0x1B2229));
		table.setRowHeight(26);
		table.setFont(mono(Font.PLAIN, 13));
		table.setFillsViewportHeight(true);
		table.setRowSelectionAllowed(false);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setBackground(PANEL);
		table.getTableHeader().setForeground(AMBER);
		table.getTableHeader().setFont(mono(Font.BOLD, 12));
		DefaultTableCellRenderer center = new DefaultTableCellRenderer();
		center.setHorizontalAlignment(SwingConstants.CENTER);
		table.getColumnModel().getColumn(0).setCellRenderer(center);
		table.getColumnModel().getColumn(0).setMaxWidth(60);
		DefaultTableCellRenderer right = new DefaultTableCellRenderer();
		right.setHorizontalAlignment(SwingConstants.RIGHT);
		table.getColumnModel().getColumn(1).setCellRenderer(right);
		table.getColumnModel().getColumn(1).setPreferredWidth(110);
		table.getColumnModel().getColumn(2).setPreferredWidth(190);
		table.getColumnModel().getColumn(3).setPreferredWidth(150);
		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.getViewport().setBackground(BACKGROUND);
		tableScroll.setBorder(BorderFactory.createEmptyBorder());

		JScrollPane listScroll = new JScrollPane(list);
		listScroll.setBorder(BorderFactory.createEmptyBorder());
		listScroll.setPreferredSize(new Dimension(190, 100));
		listScroll.setMinimumSize(new Dimension(150, 100));

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, tableScroll);
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setDividerSize(5);
		split.setBackground(BACKGROUND);
		split.setContinuousLayout(true);
		split.setResizeWeight(0);
		split.setDividerLocation(190);
		root.add(split, BorderLayout.CENTER);

		JLabel empty = new JLabel("No score yet: play a round!", SwingConstants.CENTER);
		empty.setForeground(MUTED);
		empty.setFont(mono(Font.ITALIC, 13));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
		buttons.setBackground(PANEL);
		JButton clear = new JButton("Clear this game");
		JButton close = new JButton("Close");
		buttons.add(empty);
		buttons.add(clear);
		buttons.add(close);
		root.add(buttons, BorderLayout.SOUTH);
		close.addActionListener(e -> dialog.dispose());
		dialog.getRootPane().setDefaultButton(close);

		list.addListSelectionListener(e -> {
			if (e.getValueIsAdjusting()) {
				return;
			}
			EggScores.Game game = list.getSelectedValue();
			model.load(game);
			empty.setText(game != null && !isRevealed(game.id, preselected) ? "Still to be discovered..." : "No score yet: play a round!");
			empty.setVisible(model.getRowCount() == 0);
			clear.setEnabled(model.getRowCount() > 0);
		});
		clear.addActionListener(e -> {
			EggScores.Game game = list.getSelectedValue();
			if (game != null && JOptionPane.showConfirmDialog(dialog, "Delete the saved scores of " + game.name + "?", "Clear scores",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				EggScores.clear(game.id);
				list.repaint();
				model.load(game);
				empty.setText(isRevealed(game.id, preselected) ? "No score yet: play a round!" : "Still to be discovered...");
				empty.setVisible(true);
				clear.setEnabled(false);
			}
		});

		int selected = 0;
		for (int i = 0; i < games.size(); i++) {
			if (games.get(i).id.equals(preselected)) {
				selected = i;
			}
		}
		list.setSelectedIndex(selected);
		return root;
	}

	/** A game shows its title when it has at least one score, or when it is the game the leaderboard was opened from */
	private static boolean isRevealed(String gameId, String current) {
		return gameId.equals(current) || EggScores.best(gameId) > 0;
	}

	private static Font mono(int style, int size) {
		Font consolas = new Font("Consolas", style, size);
		return "Consolas".equalsIgnoreCase(consolas.getFamily()) ? consolas : new Font(Font.MONOSPACED, style, size);
	}

	private static final class ScoresModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private static final String[] COLUMNS = { "#", "Score", "Details", "Date" };

		private EggScores.Game game;
		private List<EggScores.Entry> entries = java.util.Collections.emptyList();

		void load(EggScores.Game newGame) {
			game = newGame;
			entries = newGame != null ? EggScores.top(newGame.id) : java.util.Collections.<EggScores.Entry>emptyList();
			fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return entries.size();
		}

		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(int row, int column) {
			EggScores.Entry entry = entries.get(row);
			switch (column) {
			case 0:
				return String.valueOf(row + 1);
			case 1:
				return entry.score + (game != null && game.unit.length() > 0 ? " " + game.unit : "");
			case 2:
				return entry.detail;
			default:
				return entry.when > 0 ? DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(entry.when)) : "-";
			}
		}
	}
}
