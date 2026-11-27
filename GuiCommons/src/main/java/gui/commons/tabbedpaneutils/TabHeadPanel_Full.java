/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package gui.commons.tabbedpaneutils;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.i18n.I18n;
import various.common.light.utility.string.StringWorker;
import net.miginfocom.swing.MigLayout;
import java.awt.Component;

public class TabHeadPanel_Full extends JPanel{
	private static final long serialVersionUID = -6387090181225164666L;

	@SuppressWarnings("unused")
	private CurrentTabHeader currentTabRetriever;
	private JPanel thisPanel;
	private JButton closeButton;
	/** Shared instances: setting the same icon again does not trigger a repaint (the tab UI sets it at every paint) */
	private static final Icon PIN_DIM = new PinIcon(false);
	private static final Icon PIN_BRIGHT = new PinIcon(true);
	/** Pinned tab: the close button turns into a pin that unpins the tab (opt-in, see setPinned) */
	private boolean pinned = false;
	private Runnable unpinAction;
	
	public TabHeadPanel_Full(String title, String tooltip, String iconPath, ActionListener tabCloseListener, CurrentTabHeader currentTabRetriever) {
		super();
		setOpaque(false);
		setToolTipText(tooltip);
		
		thisPanel = this;
		this.currentTabRetriever = currentTabRetriever != null ? currentTabRetriever : ()->{return this;};
		// image label
		JLabel imageLbl = new JLabel();
		imageLbl.setIcon(new ImageIcon(iconPath));
		imageLbl.setPreferredSize(new Dimension(16, 20));
		imageLbl.setMaximumSize(new Dimension(25, 20));
		
		
		// tab title
		JLabel lblTitle = new JLabel();
		lblTitle.setHorizontalAlignment(SwingConstants.LEFT);
		lblTitle.setFont(GeneralConfig.BUTTON_TAB_HEAD_FONT.deriveFont(13f));
		lblTitle.setText(StringWorker.fitToMaxLentgh(title, TabUtils.MAX_TAB_LABELENGHT));
		setLayout(new MigLayout("fill, insets 0 6 0 3, gap 4", "[16px][40px,grow][18px]", "[20px:22px:30px]"));
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

		closeButton = btnClose;
		btnClose.addActionListener(e -> {
			if (pinned) {
				if (unpinAction != null) {
					unpinAction.run();
				}
			} else {
				tabCloseListener.actionPerformed(e);
			}
		});
		btnClose.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {
			}
			public void mousePressed(MouseEvent e) {
			}
			public void mouseExited(MouseEvent e) {
				if (pinned) {
					btnClose.setIcon(PIN_DIM);
					btnClose.repaint();
					return;
				}
				if (thisPanel.equals(currentTabRetriever.getCurrent())) {
					btnClose.setIcon(IconsPathConfigurator.X_WHITE_IMG_RED);
				} else {
					btnClose.setIcon(IconsPathConfigurator.X_GRAY_IMG_RED_LIGHT);
				}
				btnClose.repaint();
			}
			public void mouseEntered(MouseEvent e) {
				if (pinned) {
					btnClose.setIcon(PIN_BRIGHT);
					btnClose.repaint();
					return;
				}
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
	
	/**
	 * Pins or unpins this header: a pinned tab shows a pin instead of the close button, and clicking the pin runs unpinAction.
	 */
	public void setPinned(boolean pinned, Runnable unpinAction) {
		this.pinned = pinned;
		this.unpinAction = unpinAction;
		// the pinned tabs are shown in their own row above the tabs, not in the tab strip
		setVisible(!pinned);
		if (pinned) {
			closeButton.setIcon(PIN_DIM);
			closeButton.setToolTipText(I18n.t("tabs.pinned_tip"));
		} else {
			closeButton.setIcon(IconsPathConfigurator.X_WHITE_IMG_RED);
			closeButton.setToolTipText(null);
		}
		closeButton.repaint();
	}

	public boolean isPinned() {
		return pinned;
	}

	/** The pin shown on pinned tabs (also by the strip of the pinned tabs) */
	public static Icon pinIcon(boolean bright) {
		return bright ? PIN_BRIGHT : PIN_DIM;
	}

	/** For a pinned header: bright pin on the selected tab, dimmed on the others (called by the tab UI when painting) */
	public void showPinIcon(boolean bright) {
		if (pinned) {
			closeButton.setIcon(bright ? PIN_BRIGHT : PIN_DIM);
		}
	}

	/** Small drawn push-pin, colors follow the look and feel */
	private static final class PinIcon implements Icon {
		private final boolean highlighted;

		PinIcon(boolean highlighted) {
			this.highlighted = highlighted;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color base = UIManager.getColor("Label.foreground");
				if (base == null) {
					base = new Color(210, 210, 210);
				}
				g2.setColor(highlighted ? base : new Color(base.getRed(), base.getGreen(), base.getBlue(), 170));
				g2.translate(x + 6, y + 6);
				g2.rotate(Math.toRadians(40));
				// head, body and needle of the pin
				g2.fill(new RoundRectangle2D.Double(-3, -6, 6, 3, 1.5, 1.5));
				g2.fill(new RoundRectangle2D.Double(-2, -3.5, 4, 5.5, 1, 1));
				g2.fill(new RoundRectangle2D.Double(-3.5, 1.5, 7, 1.6, 1, 1));
				g2.fill(new Rectangle2D.Double(-0.5, 3, 1, 4));
			} finally {
				g2.dispose();
			}
		}

		@Override
		public int getIconWidth() {
			return 12;
		}

		@Override
		public int getIconHeight() {
			return 12;
		}
	}

	@FunctionalInterface
	public interface CurrentTabHeader {
		public JPanel getCurrent();
	}
}
