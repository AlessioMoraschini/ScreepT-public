/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com/
 */
package antilocker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public class AntilockerGui extends JFrame {
	private static final long serialVersionUID = 261064954643004636L;

	private static final boolean DEBUG_MODE = true;

	private JSpinner spinnerDeltaY;
	private JSpinner spinnerDeltaX;
	private JPanel mainPanel;
	private JLabel lblSeconds;
	private JLabel lblYstep;
	private JButton btnStart;
	private JLabel lblXdelta;
	private JSpinner spinnerSeconds;
	private JButton btnStop;
	private JButton btnPause;

	public AntiLocker antilocker;
	public static AntilockerGui gui;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			gui = new AntilockerGui();
			gui.setBounds(new Rectangle(450, 450, 500, 180));
			gui.setVisible(true);
			gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			gui.setAlwaysOnTop(true);
			gui.setTitle("Anti-screenLocker - By A.M.");
		});
	}

	public AntilockerGui() {

		mainPanel = new JPanel();
		mainPanel.setBackground(Color.DARK_GRAY);
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		GridBagLayout gbl_mainPanel = new GridBagLayout();
		gbl_mainPanel.columnWidths = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		gbl_mainPanel.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		gbl_mainPanel.columnWeights = new double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, Double.MIN_VALUE };
		gbl_mainPanel.rowWeights = new double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, Double.MIN_VALUE };
		mainPanel.setLayout(gbl_mainPanel);

		lblXdelta = new JLabel("Delta X step:");
		lblXdelta.setForeground(Color.WHITE);
		lblXdelta.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		GridBagConstraints gbc_lblXdelta = new GridBagConstraints();
		gbc_lblXdelta.gridwidth = 2;
		gbc_lblXdelta.insets = new Insets(0, 0, 5, 5);
		gbc_lblXdelta.gridx = 2;
		gbc_lblXdelta.gridy = 1;
		mainPanel.add(lblXdelta, gbc_lblXdelta);

		spinnerDeltaX = new JSpinner();
		spinnerDeltaX.setModel(new SpinnerNumberModel(20, 1, 2000, 1));
		GridBagConstraints gbc_spinnerDeltaX = new GridBagConstraints();
		gbc_spinnerDeltaX.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerDeltaX.insets = new Insets(0, 0, 5, 5);
		gbc_spinnerDeltaX.gridx = 4;
		gbc_spinnerDeltaX.gridy = 1;
		mainPanel.add(spinnerDeltaX, gbc_spinnerDeltaX);

		lblYstep = new JLabel("Delta Y step:");
		lblYstep.setForeground(Color.WHITE);
		lblYstep.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		GridBagConstraints gbc_lblYstep = new GridBagConstraints();
		gbc_lblYstep.gridwidth = 2;
		gbc_lblYstep.insets = new Insets(0, 0, 5, 5);
		gbc_lblYstep.gridx = 2;
		gbc_lblYstep.gridy = 2;
		mainPanel.add(lblYstep, gbc_lblYstep);

		spinnerDeltaY = new JSpinner();
		spinnerDeltaY.setModel(new SpinnerNumberModel(20, 1, 2000, 1));
		GridBagConstraints gbc_spinnerDeltaY = new GridBagConstraints();
		gbc_spinnerDeltaY.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerDeltaY.insets = new Insets(0, 0, 5, 5);
		gbc_spinnerDeltaY.gridx = 4;
		gbc_spinnerDeltaY.gridy = 2;
		mainPanel.add(spinnerDeltaY, gbc_spinnerDeltaY);

		lblSeconds = new JLabel("Repeat every N Seconds:");
		lblSeconds.setForeground(Color.WHITE);
		lblSeconds.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		GridBagConstraints gbc_lblSeconds = new GridBagConstraints();
		gbc_lblSeconds.gridwidth = 2;
		gbc_lblSeconds.insets = new Insets(0, 0, 5, 5);
		gbc_lblSeconds.gridx = 2;
		gbc_lblSeconds.gridy = 3;
		mainPanel.add(lblSeconds, gbc_lblSeconds);

		spinnerSeconds = new JSpinner();
		spinnerSeconds.setModel(new SpinnerNumberModel(4, 1, 86400, 1));
		GridBagConstraints gbc_spinnerSeconds = new GridBagConstraints();
		gbc_spinnerSeconds.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerSeconds.insets = new Insets(0, 0, 5, 5);
		gbc_spinnerSeconds.gridx = 4;
		gbc_spinnerSeconds.gridy = 3;
		mainPanel.add(spinnerSeconds, gbc_spinnerSeconds);

		btnStart = new JButton("Start");
		btnStart.setBackground(Color.LIGHT_GRAY);
		GridBagConstraints gbc_btnStart = new GridBagConstraints();
		gbc_btnStart.gridheight = 2;
		gbc_btnStart.insets = new Insets(0, 0, 0, 5);
		gbc_btnStart.gridx = 2;
		gbc_btnStart.gridy = 4;
		mainPanel.add(btnStart, gbc_btnStart);

		btnPause = new JButton("Pause");
		btnPause.setBackground(Color.LIGHT_GRAY);
		GridBagConstraints gbc_btnPause = new GridBagConstraints();
		gbc_btnPause.gridheight = 2;
		gbc_btnPause.insets = new Insets(0, 0, 0, 5);
		gbc_btnPause.gridx = 3;
		gbc_btnPause.gridy = 4;
		mainPanel.add(btnPause, gbc_btnPause);

		btnStop = new JButton("Stop");
		btnStop.setBackground(Color.LIGHT_GRAY);
		GridBagConstraints gbc_btnStop = new GridBagConstraints();
		gbc_btnStop.gridheight = 2;
		gbc_btnStop.insets = new Insets(0, 0, 0, 5);
		gbc_btnStop.gridx = 4;
		gbc_btnStop.gridy = 4;
		mainPanel.add(btnStop, gbc_btnStop);

		antilocker = new AntiLocker(2000, 20, 20);

		addActions();

		initEnabledGuiElements();
	}

	private int getXdelta() {
		return (Integer)spinnerDeltaX.getValue();
	}
	private Integer getYdelta() {
		return (Integer)spinnerDeltaY.getValue();
	}
	private Integer getSeconds() {
		return (Integer)spinnerSeconds.getValue() * 1000;
	}

	private void initEnabledGuiElements() {
		setEnabledEditables(true);
		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
		btnPause.setEnabled(false);
	}

	private void setEnabledEditables(boolean enabled){
		spinnerSeconds.setEnabled(enabled);
		spinnerDeltaX.setEnabled(enabled);
		spinnerDeltaY.setEnabled(enabled);
	}

	private void addActions() {

		new Thread(()-> {
			while(true) {
				if(antilocker != null && !antilocker.isRunning()) {
					initEnabledGuiElements();
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}).start();


		btnStart.addActionListener((e) -> {

			if((antilocker.isPaused())) {
				antilocker.setEnabledDebug(DEBUG_MODE);
				antilocker.resume();

			} else {
				antilocker = new AntiLocker(getSeconds(), getXdelta(), getYdelta());
				antilocker.setEnabledDebug(DEBUG_MODE);
				antilocker.start();
			};

			setEnabledEditables(false);
			btnStart.setEnabled(false);
			btnStop.setEnabled(true);
			btnPause.setEnabled(true);
		});

		btnStop.addActionListener((e) -> {
			if(antilocker.abort()) {
				setEnabledEditables(true);
				btnStart.setEnabled(true);
				btnStop.setEnabled(false);
				btnPause.setEnabled(false);
			}
		});

		btnPause.addActionListener((e) -> {
			if(antilocker.pause()) {
				setEnabledEditables(false);
				btnStart.setEnabled(true);
				btnStop.setEnabled(true);
				btnPause.setEnabled(false);
			}
		});
	}
}
