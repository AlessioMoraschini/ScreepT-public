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
package gui.commons.treeutils.om;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeCellRenderer;

import org.apache.log4j.Logger;

import gui.commons.general.MyFileView;
import gui.commons.treeutils.TreeUtils;
import various.common.light.files.om.FileNamed;

public class FileNameCellRenderer implements TreeCellRenderer {

	private static final int FOLDER_TEXT_HEIGHT = 10;
	private static final int FILE_TEXT_HEIGHT = 8;

	public static Font fileFont = new Font("Segoe UI", Font.PLAIN, 14);

	static Logger logger = Logger.getLogger(FileNameCellRenderer.class);

	public JLabel iconContainer;
	public JPanel container;

	public JTree treeRef;

	public int hoveringRow = -1;

	public FileNameCellRenderer() {
		container = new JPanel(new BorderLayout());

		treeRef = new JTree();

		iconContainer = new JLabel();
		iconContainer.setForeground(Color.LIGHT_GRAY);
		container.add(iconContainer);
    }

    @Override
	public Component getTreeCellRendererComponent(final JTree tree, Object value, final boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {

    	treeRef = tree;

        if (iconContainer != null && container != null && value != null) {

			if (value instanceof FileNamed) {

				// custom icon setter
				FileNamed nodeFileObj = (FileNamed) value;
				File selectedFile = nodeFileObj.mFile;
				FileNamed nodeFileObjRoot = (FileNamed) tree.getModel().getRoot();
				File root = nodeFileObjRoot.getFile();

				MyFileView fileView = new MyFileView();
				iconContainer.setIcon(fileView.getIcon(nodeFileObj.mFile));
				try {
					File absoluteFile = selectedFile.getAbsoluteFile();
					if (root.getAbsolutePath().equals(selectedFile.getAbsolutePath())) {
						iconContainer.setText(selectedFile.getName());
					} else {
						iconContainer.setText(absoluteFile.getName());
					}
				} catch (Exception e) {
					logger.error(e);
				}

				iconContainer.setFont(fileFont);
				if (selectedFile.isFile()) {
					iconContainer.setMinimumSize(new Dimension(iconContainer.getWidth() + 100, FILE_TEXT_HEIGHT));
				} else {
					iconContainer.setMinimumSize(new Dimension(iconContainer.getWidth() + 100, FOLDER_TEXT_HEIGHT));
				}

				container.setToolTipText(selectedFile != null ? selectedFile.getAbsolutePath() : "file is null!");

			}

			// the tree paints the row highlight and the level guides: the cell is transparent and its colors follow the tree
			// background, so every theme (light or dark) reads well
			container.setOpaque(false);
			iconContainer.setOpaque(false);
			Color background = tree.getBackground();
			Color strong = TreeUtils.textOn(background);
			boolean folder = value instanceof FileNamed && !((FileNamed) value).mFile.isFile();
			Color text = selected || hoveringRow == row ? strong : TreeUtils.blend(strong, background, folder ? 0.08f : 0.2f);
			iconContainer.setBorder(new EmptyBorder(0, 3, 0, 8));
			iconContainer.setForeground(text);
			container.setForeground(text);
		}
		return container;
    }

    public Object getTreeCellRendererComponent() {
    	return container;
    }

}
