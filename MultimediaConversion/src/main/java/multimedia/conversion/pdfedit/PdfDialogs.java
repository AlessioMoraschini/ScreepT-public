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
package multimedia.conversion.pdfedit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;

import gui.commons.dialogutils.NativeFileChooser;
import gui.commons.imagestudio.DialogFit;

/** File choosers, messages and the dialog merging several PDF in one */
public final class PdfDialogs {

	private static final Logger logger = Logger.getLogger(PdfDialogs.class);

	private PdfDialogs() {
	}

	static List<File> choose(Component parent, File folder, boolean several, String description, String... extensions) {
		NativeFileChooser chooser = new NativeFileChooser();
		chooser.setCurrentDirectory(folder);
		chooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
		chooser.setMultiSelectionEnabled(several);
		List<File> files = new ArrayList<>();
		if (chooser.showOpenDialog(parent) == NativeFileChooser.APPROVE_OPTION) {
			if (several) {
				java.util.Collections.addAll(files, chooser.getSelectedFiles());
			}
			if (files.isEmpty() && chooser.getSelectedFile() != null) {
				files.add(chooser.getSelectedFile());
			}
		}
		return files; // empty: cancelled by user
	}

	/** The PDF to write, or null if cancelled */
	static File chooseOutput(Component parent, File proposed) {
		NativeFileChooser chooser = new NativeFileChooser();
		chooser.setCurrentDirectory(proposed.getParentFile());
		chooser.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
		chooser.setSelectedFile(proposed);
		if (chooser.showSaveDialog(parent) != NativeFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
			return null; // cancelled by user
		}
		File chosen = chooser.getSelectedFile();
		if (!chosen.getName().toLowerCase().endsWith(".pdf")) {
			chosen = new File(chosen.getParentFile(), chosen.getName() + ".pdf");
			if (chosen.exists() && !confirm(parent, chosen.getName() + " already exists. Overwrite it?")) {
				return null;
			}
		}
		return chosen;
	}

	static boolean confirm(Component parent, String question) {
		return JOptionPane.showConfirmDialog(parent, question, "PDF", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}

	static void error(Component parent, Throwable problem) {
		Throwable cause = problem instanceof ExecutionException && problem.getCause() != null ? problem.getCause() : problem;
		logger.error("PDF operation failed", cause);
		String message = cause instanceof OutOfMemoryError ? "Not enough memory for this document." : cause.getMessage();
		JOptionPane.showMessageDialog(parent, message != null ? message : cause.toString(), "PDF", JOptionPane.ERROR_MESSAGE);
	}

	/** Runs a file operation in background, then says what was written */
	static void inBackground(final Component parent, final File written, final PdfTools.Writer work) {
		final Cursor before = parent.getCursor();
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				work.writeTo(written);
				return null;
			}

			@Override
			protected void done() {
				parent.setCursor(before);
				try {
					get();
					JOptionPane.showMessageDialog(parent, "Created " + written.getAbsolutePath(), "PDF", JOptionPane.INFORMATION_MESSAGE);
				} catch (InterruptedException | ExecutionException e) {
					error(parent, e);
				}
			}
		}.execute();
	}

	/** Lets the user choose and order several PDF and writes them as one new file; first is proposed in the list (or null) */
	public static void merge(final Component parent, File first) {
		Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
		final JDialog dialog = new JDialog(owner, "Merge PDF files", Dialog.ModalityType.APPLICATION_MODAL);
		final DefaultListModel<File> files = new DefaultListModel<>();
		if (first != null) {
			files.addElement(first);
		}
		final JList<File> list = new JList<>(files);
		list.setVisibleRowCount(12);

		JButton add = new JButton("Add...");
		add.addActionListener(e -> choose(dialog, files.isEmpty() ? null : files.lastElement().getParentFile(), true, "PDF", "pdf")
				.forEach(files::addElement));
		JButton remove = new JButton("Remove");
		remove.addActionListener(e -> list.getSelectedValuesList().forEach(files::removeElement));
		JButton up = new JButton("Up");
		up.addActionListener(e -> shift(list, files, -1));
		JButton down = new JButton("Down");
		down.addActionListener(e -> shift(list, files, 1));
		JButton merge = new JButton("Merge...");
		merge.addActionListener(e -> {
			if (files.size() < 2) {
				JOptionPane.showMessageDialog(dialog, "Add at least two files.", "PDF", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			File output = chooseOutput(dialog, new File(files.firstElement().getParentFile(), "merged.pdf"));
			if (output != null) {
				List<File> sources = java.util.Collections.list(files.elements());
				dialog.dispose();
				inBackground(parent, output, target -> PdfTools.merge(sources, target));
			}
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		for (JButton button : new JButton[] { add, remove, up, down, merge }) {
			buttons.add(button);
		}
		JPanel content = new JPanel(new BorderLayout(0, 6));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		content.add(new JScrollPane(list), BorderLayout.CENTER);
		content.add(buttons, BorderLayout.SOUTH);
		dialog.setContentPane(content);
		DialogFit.apply(dialog, owner, 560, 0);
		dialog.setVisible(true);
	}

	private static void shift(JList<File> list, DefaultListModel<File> files, int direction) {
		int index = list.getSelectedIndex();
		int other = index + direction;
		if (index >= 0 && other >= 0 && other < files.size()) {
			File moved = files.remove(index);
			files.add(other, moved);
			list.setSelectedIndex(other);
		}
	}
}
