package puppynoid.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.text.JTextComponent;

import puppynoid.om.Difficulty;
import puppynoid.utils.LevelLoader;
import puppynoid.utils.config.GameConfigs;
import puppynoid.utils.config.GeneralConfig;
import various.common.light.gui.ScreenMemory;

public class StartFrame extends JFrame {
	private static final long serialVersionUID = -8026416994513756565L;

	public static StartFrame instance;

	private GeneralConfig generalConfig;

	GameFrame gamePanel;
	StartFrame thisFrame;

	public static Dimension size = new Dimension(520,230);

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

		addControlsGuide();

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
		ScreenMemory.centerOnLastScreen(this);
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

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				instance = null;
			}

		});

		instance = this;
	}

	/** Keys guide shown on top of the start window */
	private void addControlsGuide() {
		JLabel guide = new JLabel("<html><div style='text-align:center'><b style='color:#FFCD3C'>HOW TO PLAY</b><br>"
				+ "<b>\u2190 / \u2192</b> move paddle &nbsp;&middot;&nbsp; <b>CTRL</b> launch ball &nbsp;&middot;&nbsp; <b>SPACE</b> pause<br>"
				+ "<b>ESC</b> quit &nbsp;&middot;&nbsp; <b>ENTER</b> retry &nbsp;&middot;&nbsp; <b>N</b> next level</div></html>");
		guide.setForeground(Color.WHITE);
		guide.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		guide.setHorizontalAlignment(SwingConstants.CENTER);
		guide.setOpaque(true);
		guide.setBackground(new Color(30, 34, 48));
		guide.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory.createLineBorder(new Color(255, 205, 60), 1),
				javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12)));

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 230;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new java.awt.Insets(8, 10, 12, 10);
		constraints.anchor = GridBagConstraints.NORTH;
		this.add(guide, constraints);
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
