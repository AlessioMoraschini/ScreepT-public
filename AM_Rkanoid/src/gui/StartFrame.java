package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import om.Difficulty;
import utils.LevelLoader;
import utils.config.GameConfigs;
import utils.config.GeneralConfig;

public class StartFrame extends JFrame {
	private static final long serialVersionUID = -8026416994513756565L;
	
	private static StartFrame instance;
	
	private GeneralConfig generalConfig;
	
	GameFrame gamePanel;
	StartFrame thisFrame;

	public static Dimension size = new Dimension(400,150);

	public StartFrame() throws IOException {
		
		thisFrame = this;
		
		generalConfig = new GeneralConfig(GeneralConfig.GAME_CONFIG_FOLDER + "generalConfig.properties");
		
		// Layout Setup 
		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] {30, 60, 30};
		this.setLayout(layout);
		
		// GUI objects Setup 
		JLabel label = new JLabel("Select difficulty");
		label.setForeground(Color.white);
		
		JButton startButton = new JButton("Start");
		
		JComboBox<Difficulty> comboDifficulty = new JComboBox<Difficulty>(Difficulty.values());
		Difficulty difficulty = Difficulty.valueOf(generalConfig.loadString(GeneralConfig.KEY_DIFFICULTY));
		comboDifficulty.setSelectedItem(difficulty);
		comboDifficulty.addActionListener((e)->{
			Difficulty selected = (Difficulty) comboDifficulty.getSelectedItem();
			generalConfig.setProperty(GeneralConfig.KEY_DIFFICULTY, selected.name(), true);
		});
		
		styleComponent(label, 0, 30, 50);
		label.setHorizontalAlignment(SwingConstants.LEFT);
		styleComponent(comboDifficulty, 35, 120, 50);
		comboDifficulty.setAlignmentX(SwingConstants.LEFT);
		styleComponent(startButton, 175, 55, 50);
		startButton.setHorizontalAlignment(SwingConstants.RIGHT);
		
		// GUI adjustment
		this.setMinimumSize(size);
		this.setPreferredSize(size);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.getContentPane().setBackground(Color.BLACK);
		this.setVisible(true);
		this.setLocationRelativeTo(null);
		this.setTitle(GameConfigs.GAME_TITLE);
		this.setIconImage(new ImageIcon(GameConfigs.IMG_FOLDER + "mainIcon.png").getImage());
		
		// Action listeners
		startButton.addActionListener((e) -> {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						gamePanel = new GameFrame(
								generalConfig,
								(Difficulty)comboDifficulty.getSelectedItem(), 
								new Runnable() {
									@Override
									public void run() {
										thisFrame.setVisible(true);
										comboDifficulty.setSelectedItem(
												Difficulty.valueOf(generalConfig.loadString(GeneralConfig.KEY_DIFFICULTY)));
									}
								},
								new LevelLoader());
						
						gamePanel.setLocationRelativeTo(thisFrame);
						thisFrame.setVisible(false);
						gamePanel.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		});
		
		setupMenuBar();
		
		SwingUtilities.invokeLater(()->{
			GUIStyelSettings.setLightNimbus(3);
			GUIStyelSettings.updateAllWindowsLaf();
		});
		
		instance = this;
	}
	
	private void setupMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setVisible(true);
		menuBar.setMinimumSize(new Dimension (200, 30));
		thisFrame.setJMenuBar(menuBar);
		
		menuBar.add(new JMenuItem("Instructions"));
		// TODO
	}
	
	private void styleComponent(JComponent component, int x, int width, int y) {
		if (!(component instanceof JTextComponent) && !(component instanceof JLabel)) {
			component.setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
		component.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		component.setMinimumSize(new Dimension(40, 25));
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = y;
		constraints.gridx = x;
		constraints.gridwidth = width;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.CENTER;
		this.add(component, constraints);
	}
	
	public static StartFrame getInstance(String title) {
		StartFrame instance = null;
		try {
			instance = StartFrame.instance != null ? StartFrame.instance : new StartFrame();
			instance.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			instance.setTitle(title);
			instance.setVisible(true);
			instance.toFront();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return instance;
    }
	
}
