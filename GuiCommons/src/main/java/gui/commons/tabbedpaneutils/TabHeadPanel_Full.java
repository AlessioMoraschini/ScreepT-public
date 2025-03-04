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
package gui.commons.tabbedpaneutils;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.utility.string.StringWorker;
import net.miginfocom.swing.MigLayout;
import java.awt.Component;

public class TabHeadPanel_Full extends JPanel{
	private static final long serialVersionUID = -6387090181225164666L;

	@SuppressWarnings("unused")
	private CurrentTabHeader currentTabRetriever;
	private JPanel thisPanel;
	
	public TabHeadPanel_Full(String title, String tooltip, String iconPath, ActionListener tabCloseListener, CurrentTabHeader currentTabRetriever) {
		super();
		setOpaque(false);
		setToolTipText(tooltip);
		
		thisPanel = this;
		this.currentTabRetriever = currentTabRetriever != null ? currentTabRetriever : ()->{return this;};
		// image label
		JLabel imageLbl = new JLabel();
		imageLbl.setIcon(new ImageIcon(iconPath));
		imageLbl.setPreferredSize(new Dimension(15, 20));
		imageLbl.setMaximumSize(new Dimension(25, 20));
		
		
		// tab title
		JLabel lblTitle = new JLabel();
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setFont(GeneralConfig.BUTTON_TAB_HEAD_FONT.deriveFont(14f));
		lblTitle.setText(StringWorker.fitToMaxLentgh(title, TabUtils.MAX_TAB_LABELENGHT));
		setLayout(new MigLayout("fill, insets 0", "[20px][69px,grow][35px]", "[22px:24px:34px]"));
		// button close
		final JButton btnClose = new JButton("");
		btnClose.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnClose.setPreferredSize(new Dimension(20, 20));
		btnClose.setSize(new Dimension(20, 20));
		btnClose.setIgnoreRepaint(true);
		btnClose.setMargin(new Insets(0, 0, 0, 0));
		btnClose.setMaximumSize(new Dimension(18, 18));
		btnClose.setMinimumSize(new Dimension(18, 18));
		btnClose.setVerticalTextPosition(SwingConstants.CENTER);
		btnClose.setHorizontalTextPosition(SwingConstants.CENTER);
		btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
		final Color background = btnClose.getBackground();
		Color foreground = new Color(210,210,210);
		btnClose.setBackground(background);
		btnClose.setForeground(foreground);
		btnClose.setIcon(IconsPathConfigurator.X_WHITE_IMG_RED);

		btnClose.addActionListener(tabCloseListener);
		btnClose.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {
			}
			public void mousePressed(MouseEvent e) {
			}
			public void mouseExited(MouseEvent e) {
				if (thisPanel.equals(currentTabRetriever.getCurrent())) {
					btnClose.setIcon(IconsPathConfigurator.X_WHITE_IMG_RED);
				} else {
					btnClose.setIcon(IconsPathConfigurator.X_GRAY_IMG_RED_LIGHT);
				}
				btnClose.repaint();
			}
			public void mouseEntered(MouseEvent e) {
				if (thisPanel.equals(currentTabRetriever.getCurrent())) {
					btnClose.setIcon(IconsPathConfigurator.X_GRAY_IMG_RED_LIGHT);
				} else {
					btnClose.setIcon(IconsPathConfigurator.X_BLACK_IMG_RED);
				}
				btnClose.repaint();
			}
			public void mouseClicked(MouseEvent e) {
			}
		});


		// add elements to pnlTab : order must be same as "int TAB_XXX_ORDER" defined at start of this class
		add(imageLbl, "cell 0 0,alignx left,aligny center");
		add(lblTitle, "cell 1 0,growx,aligny center"); // 2 - TEXT TITLE
		add(btnClose, "cell 2 0,alignx right,aligny center"); // 3 - CLOSE BUTTON
		
	}
	
	@FunctionalInterface
	public interface CurrentTabHeader {
		public JPanel getCurrent();
	}
}
