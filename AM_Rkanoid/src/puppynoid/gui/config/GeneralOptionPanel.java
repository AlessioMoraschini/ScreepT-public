package puppynoid.gui.config;

import various.common.light.gui.GuiUtils;
import various.common.light.sounds.SoundsManager;

import java.awt.GridBagLayout;
import java.awt.Image;

import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Color;
import java.awt.SystemColor;
import java.io.IOException;

import javax.swing.JSeparator;
import javax.swing.JSlider;

import puppynoid.om.Difficulty;
import puppynoid.utils.config.GameConfigs;
import puppynoid.utils.config.GeneralConfig;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

public class GeneralOptionPanel extends AbstractOptionPanel {
	private static final long serialVersionUID = -8750945005935379183L;
	
	public static volatile int instances = 0;

	private JLabel lblDifficulty;
	private JLabel lblEffectsVolume;
	private JLabel lblMusicVolume;
	private JSlider sliderBaseVolume;
	private JSlider sliderEffectsVolume;
	private JComboBox<Difficulty> comboDifficulty;
	private JCheckBox btnBaseMuted;
	private JCheckBox btnEffectsMuted;

	public GeneralOptionPanel(GeneralConfig configuration) {
		super(configuration);
		
		instances ++;
		
		setForeground(Color.LIGHT_GRAY);
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 120, 0, 0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{40, 0, 0, 30, 30, 30, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 1.0, 1.0, 1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel lblGeneralOptions = new JLabel("General Options");
		lblGeneralOptions.setForeground(SystemColor.windowText);
		lblGeneralOptions.setFont(new Font("Vrinda", Font.BOLD, 20));
		GridBagConstraints gbc_lblGeneralOptions = new GridBagConstraints();
		gbc_lblGeneralOptions.anchor = GridBagConstraints.SOUTH;
		gbc_lblGeneralOptions.gridwidth = 4;
		gbc_lblGeneralOptions.insets = new Insets(0, 0, 5, 5);
		gbc_lblGeneralOptions.gridx = 1;
		gbc_lblGeneralOptions.gridy = 0;
		add(lblGeneralOptions, gbc_lblGeneralOptions);
		
		JSeparator separator = new JSeparator();
		separator.setBackground(SystemColor.activeCaptionBorder);
		separator.setForeground(SystemColor.activeCaptionBorder);
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 4;
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 1;
		gbc_separator.gridy = 1;
		add(separator, gbc_separator);
		
		lblMusicVolume = new JLabel("Music Volume");
		lblMusicVolume.setForeground(SystemColor.controlDkShadow);
		GridBagConstraints gbc_lblMusicVolume = new GridBagConstraints();
		gbc_lblMusicVolume.insets = new Insets(0, 0, 5, 5);
		gbc_lblMusicVolume.gridx = 1;
		gbc_lblMusicVolume.gridy = 3;
		add(lblMusicVolume, gbc_lblMusicVolume);
		
		btnBaseMuted = new JCheckBox("");
		GuiUtils.makeTransparentJToggleButton(btnBaseMuted);
		GridBagConstraints gbc_btnBaseMuted = new GridBagConstraints();
		gbc_btnBaseMuted.fill = GridBagConstraints.BOTH;
		gbc_btnBaseMuted.insets = new Insets(0, 0, 5, 5);
		gbc_btnBaseMuted.gridx = 2;
		gbc_btnBaseMuted.gridy = 3;
		add(btnBaseMuted, gbc_btnBaseMuted);
		
		sliderBaseVolume = new JSlider(1, 100);
		sliderBaseVolume.setBackground(Color.DARK_GRAY);
		GridBagConstraints gbc_sliderBaseVolume = new GridBagConstraints();
		gbc_sliderBaseVolume.gridwidth = 2;
		gbc_sliderBaseVolume.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderBaseVolume.insets = new Insets(0, 0, 5, 5);
		gbc_sliderBaseVolume.gridx = 3;
		gbc_sliderBaseVolume.gridy = 3;
		add(sliderBaseVolume, gbc_sliderBaseVolume);
		
		lblEffectsVolume = new JLabel("Effects Volume");
		lblEffectsVolume.setForeground(SystemColor.controlDkShadow);
		GridBagConstraints gbc_lblEffectsVolume = new GridBagConstraints();
		gbc_lblEffectsVolume.insets = new Insets(0, 0, 5, 5);
		gbc_lblEffectsVolume.gridx = 1;
		gbc_lblEffectsVolume.gridy = 4;
		add(lblEffectsVolume, gbc_lblEffectsVolume);
		
		btnEffectsMuted = new JCheckBox("");
		GuiUtils.makeTransparentJToggleButton(btnEffectsMuted);
		btnEffectsMuted.setPressedIcon(new ImageIcon(GameConfigs.IMG_FOLDER + "muted.png"));
		btnEffectsMuted.setIcon(new ImageIcon(GameConfigs.IMG_FOLDER + "unmuted.png"));
		GridBagConstraints gbc_btnEffectsMuted = new GridBagConstraints();
		gbc_btnEffectsMuted.fill = GridBagConstraints.BOTH;
		gbc_btnEffectsMuted.insets = new Insets(0, 0, 5, 5);
		gbc_btnEffectsMuted.gridx = 2;
		gbc_btnEffectsMuted.gridy = 4;
		add(btnEffectsMuted, gbc_btnEffectsMuted);
		
		try {
			Image muteIcon = puppynoid.utils.GuiUtils.getResizedImage(GameConfigs.IMG_FOLDER + "muted.png", 25, 25);
			Image unmutedIcon = puppynoid.utils.GuiUtils.getResizedImage(GameConfigs.IMG_FOLDER + "unmuted.png", 20, 20);
			ImageIcon unmuted = new ImageIcon();
			unmuted.setImage(unmutedIcon);
			ImageIcon muted = new ImageIcon();
			muted.setImage(muteIcon);
			
			btnBaseMuted.setPressedIcon(muted);
			btnBaseMuted.setSelectedIcon(muted);
			btnBaseMuted.setRolloverIcon(unmuted);
			btnBaseMuted.setIcon(unmuted);
			
			btnEffectsMuted.setPressedIcon(muted);
			btnEffectsMuted.setSelectedIcon(muted);
			btnEffectsMuted.setRolloverIcon(unmuted);
			btnEffectsMuted.setIcon(unmuted);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sliderEffectsVolume = new JSlider();
		sliderEffectsVolume.setBackground(Color.DARK_GRAY);
		GridBagConstraints gbc_sliderEffectsVolume = new GridBagConstraints();
		gbc_sliderEffectsVolume.gridwidth = 2;
		gbc_sliderEffectsVolume.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderEffectsVolume.insets = new Insets(0, 0, 5, 5);
		gbc_sliderEffectsVolume.gridx = 3;
		gbc_sliderEffectsVolume.gridy = 4;
		add(sliderEffectsVolume, gbc_sliderEffectsVolume);
		
		lblDifficulty = new JLabel("Difficulty");
		lblDifficulty.setForeground(SystemColor.controlDkShadow);
		GridBagConstraints gbc_lblDifficulty = new GridBagConstraints();
		gbc_lblDifficulty.insets = new Insets(0, 0, 5, 5);
		gbc_lblDifficulty.gridx = 1;
		gbc_lblDifficulty.gridy = 5;
		add(lblDifficulty, gbc_lblDifficulty);
		
		comboDifficulty = new JComboBox<Difficulty>(Difficulty.values());
		comboDifficulty.setFont(new Font("Tahoma", Font.BOLD, 15));
		comboDifficulty.setBackground(Color.DARK_GRAY);
		GridBagConstraints gbc_comboDifficulty = new GridBagConstraints();
		gbc_comboDifficulty.gridwidth = 3;
		gbc_comboDifficulty.insets = new Insets(0, 0, 5, 5);
		gbc_comboDifficulty.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboDifficulty.gridx = 2;
		gbc_comboDifficulty.gridy = 5;
		add(comboDifficulty, gbc_comboDifficulty);
		
		addListeners();
	}
	
	public void addListeners() {
		sliderBaseVolume.addChangeListener((e)-> {
			if(sliderBaseVolume.getMinimum() == sliderBaseVolume.getValue()) {
				btnBaseMuted.setSelected(true);
			} else {
				btnBaseMuted.setSelected(false);
			}
		});

		sliderEffectsVolume.addChangeListener((e)-> {
			if(sliderEffectsVolume.getMinimum() == sliderEffectsVolume.getValue()) {
				btnEffectsMuted.setSelected(true);
			} else {
				btnEffectsMuted.setSelected(false);
			}
		});
		
		btnEffectsMuted.addActionListener((e)-> {
			if(btnEffectsMuted.isSelected()) {
				sliderEffectsVolume.setValue(sliderEffectsVolume.getMinimum());
			} else {
				sliderEffectsVolume.setValue(floatToIntPercentage(config.loadFloat(GeneralConfig.KEY_EFFECTS_VOLUME), 100));
			}
			
			SoundsManager.MUTED = btnEffectsMuted.isSelected();
		});

		btnBaseMuted.addActionListener((e)-> {
			if(btnBaseMuted.isSelected()) {
				sliderBaseVolume.setValue(sliderBaseVolume.getMinimum());
			} else {
				sliderBaseVolume.setValue(floatToIntPercentage(config.loadFloat(GeneralConfig.KEY_BASE_VOLUME), 100));
			}
		});
	}

	public GeneralConfig getConfiguration() {
		return (GeneralConfig) config;
	}

	public void setConfiguration(GeneralConfig configuration) {
		this.config = configuration;
	}
	
	@Override
	public void reload() {
		super.reload();
		initializeFromConfig();
	}

	@Override
	public boolean save() {
		config.setProperty(GeneralConfig.KEY_BASE_VOLUME, intToFloatPercentage(sliderBaseVolume.getValue(), 100), true);
		config.setProperty(GeneralConfig.KEY_EFFECTS_VOLUME, intToFloatPercentage(sliderEffectsVolume.getValue(), 100), true);
		config.setProperty(GeneralConfig.KEY_DIFFICULTY, ((Difficulty)comboDifficulty.getSelectedItem()).name(), true);
		config.setProperty(GeneralConfig.KEY_BASE_MUTED, btnBaseMuted.isSelected(), true);
		config.setProperty(GeneralConfig.KEY_EFFECTS_MUTED, btnEffectsMuted.isSelected(), true);
		super.showOutcomePopUp(true);
		
		return true;
	}

	@Override
	public void discard() {
		reload();
		super.discard();
		instances --;
	}

	@Override
	public void initializeFromConfig() {
		if(config == null) {
			System.err.println("Configuration is NULL");
			return;
		}
		
		sliderBaseVolume.setValue(floatToIntPercentage(config.loadFloat(GeneralConfig.KEY_BASE_VOLUME), 100));
		sliderEffectsVolume.setValue(floatToIntPercentage(config.loadFloat(GeneralConfig.KEY_EFFECTS_VOLUME), 100));
		comboDifficulty.setSelectedItem(Difficulty.valueOf(config.loadString(GeneralConfig.KEY_DIFFICULTY)));
		btnBaseMuted.setSelected(config.getProperties().getBooleanVarFromProps(GeneralConfig.KEY_BASE_MUTED, false));
		btnEffectsMuted.setSelected(config.getProperties().getBooleanVarFromProps(GeneralConfig.KEY_EFFECTS_MUTED, false));
		config.setProperty(GeneralConfig.KEY_EFFECTS_MUTED, btnEffectsMuted.isSelected(), true);

		if(btnBaseMuted.isSelected()) {
			sliderBaseVolume.setValue(sliderBaseVolume.getMinimum());
		} else {
			sliderBaseVolume.setValue(floatToIntPercentage(config.loadFloat(GeneralConfig.KEY_BASE_VOLUME), 100));
		}
		
		if(btnEffectsMuted.isSelected()) {
			sliderEffectsVolume.setValue(sliderEffectsVolume.getMinimum());
		} else {
			sliderEffectsVolume.setValue(floatToIntPercentage(config.loadFloat(GeneralConfig.KEY_EFFECTS_VOLUME), 100));
		}
	}
	
	public float intToFloatPercentage(int value, int rangeMax) {
		return ((float)value / (float) rangeMax);
	}

	public int floatToIntPercentage(float value, int rangeMax) {
		return (int) (value * rangeMax);
	}

}
