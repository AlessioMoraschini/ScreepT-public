package puppynoid.gui.config;

import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JScrollPane;
import java.awt.Color;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import puppynoid.utils.config.AbstractConfig;
import puppynoid.utils.config.GeneralConfig;
import various.common.light.gui.dialogs.msg.JOptionHelper;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import java.awt.SystemColor;

public class MainOptionsFrame extends JDialog {
	private static final long serialVersionUID = -7587496143040130248L;

	private static final Dimension defaultSize = new Dimension(600, 600);
	
	private JList<OptionsSubPanel> optionsList;
	private JOptionHelper dialogHelper;
	private List<AbstractConfig> configList;
	
	private AbstractOptionPanel currentPanel;
	
	private static final Dimension BTN_DIMENSION = new Dimension (120, 40);
	
	private JPanel mainPanel;
	private JScrollPane leftScrollPane;
	private JScrollPane rightScrollPane;
	private JButton btnDiscard;
	private JButton btnReload;
	private JButton btnSave;
	
	private Frame owner;
	
	public Runnable returnAction;

	public MainOptionsFrame(Frame owner, String title, AbstractConfig... config) {
		super(owner, title);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
		
		this.configList = Arrays.asList(config);
		this.owner = owner;
		
		this.dialogHelper = new JOptionHelper(this);
		
		this.mainPanel = new JPanel();
		this.mainPanel.setBackground(Color.DARK_GRAY);
		getContentPane().add(this.mainPanel);
		GridBagLayout gbl_mainPanel = new GridBagLayout();
		gbl_mainPanel.columnWidths = new int[]{200, 0, 0};
		gbl_mainPanel.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_mainPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_mainPanel.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		this.mainPanel.setLayout(gbl_mainPanel);
		
		leftScrollPane = new JScrollPane();
		leftScrollPane.setPreferredSize(new Dimension(100, 30));
		leftScrollPane.setMaximumSize(new Dimension(120, 32767));
		GridBagConstraints gbc_leftScrollPane = new GridBagConstraints();
		gbc_leftScrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_leftScrollPane.fill = GridBagConstraints.BOTH;
		gbc_leftScrollPane.gridx = 0;
		gbc_leftScrollPane.gridy = 0;
		this.mainPanel.add(leftScrollPane, gbc_leftScrollPane);
		
		rightScrollPane = new JScrollPane();
		GridBagConstraints gbc_rightScrollPane = new GridBagConstraints();
		gbc_rightScrollPane.gridheight = 4;
		gbc_rightScrollPane.fill = GridBagConstraints.BOTH;
		gbc_rightScrollPane.gridx = 1;
		gbc_rightScrollPane.gridy = 0;
		this.mainPanel.add(rightScrollPane, gbc_rightScrollPane);
		
		btnDiscard = new JButton("Discard");
		btnDiscard.setPreferredSize(BTN_DIMENSION);
		btnDiscard.setForeground(new Color(0, 0, 0));
		btnDiscard.setBackground(SystemColor.activeCaptionBorder);
		GridBagConstraints gbc_btnDiscard = new GridBagConstraints();
		gbc_btnDiscard.insets = new Insets(0, 0, 5, 5);
		gbc_btnDiscard.gridx = 0;
		gbc_btnDiscard.gridy = 1;
		mainPanel.add(btnDiscard, gbc_btnDiscard);
		
		btnReload = new JButton("Reload");
		btnReload.setForeground(new Color(0, 0, 0));
		btnReload.setBackground(SystemColor.activeCaptionBorder);
		btnReload.setPreferredSize(BTN_DIMENSION);
		GridBagConstraints gbc_btnReload = new GridBagConstraints();
		gbc_btnReload.insets = new Insets(0, 0, 5, 5);
		gbc_btnReload.gridx = 0;
		gbc_btnReload.gridy = 2;
		mainPanel.add(btnReload, gbc_btnReload);
		
		btnSave = new JButton("Save");
		btnSave.setPreferredSize(BTN_DIMENSION);
		btnSave.setForeground(new Color(0, 0, 0));
		btnSave.setBackground(SystemColor.activeCaptionBorder);
		GridBagConstraints gbc_btnSave = new GridBagConstraints();
		gbc_btnSave.insets = new Insets(0, 0, 0, 5);
		gbc_btnSave.gridx = 0;
		gbc_btnSave.gridy = 3;
		mainPanel.add(btnSave, gbc_btnSave);
		
		initializeOptions();

		setMinimumSize(defaultSize);
		setPreferredSize(defaultSize);
		setVisible(true);
		setLocationRelativeTo(owner);
		
		setModal(true);
		setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
		
		addListeners();
		
		owner.setEnabled(false);
	}
	
	public void addListeners() {
		btnSave.addActionListener((e) -> {
			if(currentPanel != null) {
				currentPanel.save();
			}
		});
		
		btnDiscard.addActionListener((e) -> {
			if(currentPanel != null) {
				currentPanel.discard();
			}
		});

		btnReload.addActionListener((e) -> {
			if(currentPanel != null) {
				currentPanel.reload();
			}
		});
		
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				owner.setEnabled(true);
				if(returnAction != null) {
					returnAction.run();
				}
			}
		});
	}
	
	public void initializeOptions() {
		java.util.List<OptionsSubPanel> options = new ArrayList<OptionsSubPanel>();
		options.add(new OptionsSubPanel("General Options", GeneralOptionPanel.class, GeneralConfig.class));
		
		OptionsSubPanel[] o = new OptionsSubPanel[1];
		
		optionsList = new JList<OptionsSubPanel>(options.toArray(o));
		optionsList.setSelectedIndex(0);
		optionsList.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		loadSelectedCategory();
		optionsList.setMaximumSize(new Dimension(100, 1000));
		optionsList.setBackground(Color.LIGHT_GRAY);
		optionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		leftScrollPane.setViewportView(optionsList);
		
		// Specific option loader
		optionsList.addListSelectionListener((e) -> {
			
		});
	}
	
	private void loadSelectedCategory() {
		OptionsSubPanel selection = optionsList.getSelectedValue();
		if(selection != null) {
			try {
				AbstractConfig correctConfig = getConfigFromList(selection.configClass);
				currentPanel = selection.clazz.getDeclaredConstructor(selection.configClass).newInstance(correctConfig);
				currentPanel.initializeFromConfig();
				rightScrollPane.setViewportView(currentPanel);
			} catch (Exception e1) {
				dialogHelper.info("Cannot load " + selection.name, "Error while loading options");
			}
		}
	}
	
	public AbstractConfig getConfigFromList(Class<? extends AbstractConfig> configClass) {
		for (AbstractConfig config : configList) {
			if(config != null && config.getClass().equals(configClass)) {
				return config;
			}
		}
		
		return null;
	}
}

class OptionsSubPanel {
	
	public String name;
	public Class<? extends AbstractOptionPanel> clazz;
	public Class<? extends AbstractConfig> configClass;
	
	public OptionsSubPanel(String name, Class<? extends AbstractOptionPanel> clazz, Class<? extends AbstractConfig> configClass) {
		super();
		this.name = name;
		this.clazz = clazz;
		this.configClass = configClass;
	}

	@Override
	public String toString() {
		return name;
	}
}
