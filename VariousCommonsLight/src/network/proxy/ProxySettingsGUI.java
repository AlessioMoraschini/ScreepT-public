package network.proxy;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import gui.GuiUtils;
import network.http.HttpHelper;
import utility.properties.PropertiesManager;

public class ProxySettingsGUI extends JDialog {
	private static final long serialVersionUID = 5160035767546162937L;
	
	public static String defaultResourcesMappingDefPath = "resourcesFilePathsMappings.properties";
	public static PropertiesManager resourcesFilePathsMappings = new PropertiesManager(defaultResourcesMappingDefPath);
	
	private static final String SEPARATOR = ";@;";
	
	public static ImageIcon frameIcon = new ImageIcon();
	public static Dimension DEFAULT_SIZE = new Dimension(630, 190);
	
	public static final char DEFAULT_PSW_CHAR = '\u25CF';
	
	private static JLabel lblHost = new JLabel("Host: ");
	private static JLabel lblPort = new JLabel("Port: ");
	private static JLabel lblUser = new JLabel("User: ");
	private static JLabel lblKey = new JLabel("Key: ");

	private JTextField host = new JTextField();
	private JTextField username = new JTextField();
	
	private JPasswordField key = new JPasswordField();

	private JSpinner port = new JSpinner(new SpinnerNumberModel(8080, 1, 65535, 1));
	
	private JCheckBox chckProxySettings = new JCheckBox("Use proxy");
	private JCheckBox chckAuthSettings = new JCheckBox("Use Authentication");
	private JCheckBox chckRememberKey = new JCheckBox("Save key");
	
	private JButton btnSave = new JButton("Save settings");
	
	private JPanel mainPane;
	
	public ProxySettingsGUI(ModalityType modalityType, final boolean setAndClose, String initString) {
		
		super();
		
		setModalityType(modalityType);
		
		this.setTitle("Proxy Settings");
		this.setAlwaysOnTop(true);
		
		this.setIconImage(frameIcon.getImage());
		
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		mainPane = new JPanel();
		key.setEchoChar(DEFAULT_PSW_CHAR);
		
		GuiUtils.centerComponent(this, DEFAULT_SIZE);
		
		applyDefaultValues(initString);
		
		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] {55, 70, 180, 100, 50, 120, 10};
		layout.rowHeights = new int[] {40, 40, 10, 35};
		mainPane.setLayout(layout);
		
		styleComponent(lblHost, 0, 1, 0);
		lblHost.setHorizontalAlignment(SwingConstants.RIGHT);
		styleComponent(host, 1, 3, 0);
		
		styleComponent(lblPort, 4, 1, 0);
		lblPort.setHorizontalAlignment(SwingConstants.RIGHT);
		styleComponent(port, 5, 1, 0);
		
		styleComponent(lblUser, 0, 1, 1);
		lblUser.setHorizontalAlignment(SwingConstants.RIGHT);
		styleComponent(username, 1, 2, 1);
		styleComponent(chckRememberKey, 3, 1, 1);
		chckRememberKey.setHorizontalAlignment(SwingConstants.RIGHT);
		
		styleComponent(lblKey, 4, 1, 1);
		lblKey.setHorizontalAlignment(SwingConstants.RIGHT);
		styleComponent(key, 5, 1, 1);
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = 2;
		constraints.gridx = 0;
		constraints.gridwidth = 7;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.CENTER;
		mainPane.add(new JSeparator(), constraints);
		
		styleComponent(chckProxySettings, 0, 2, 3);
		styleComponent(chckAuthSettings, 2, 2, 3);
		styleComponent(btnSave, 5, 1, 3);
		
		btnSave.setBackground(Color.LIGHT_GRAY);
		
		addHandlers();
		
		setContentPane(mainPane);
		
		proxyEnabledGUI(chckProxySettings.isSelected());
		proxyAuthEnabledGUI(chckAuthSettings.isSelected());
		
		if (setAndClose) {
			applyProxyData();
			applProxyAuthData();
			dispose();	
		} else {
			setVisible(true);
		}
	}
	
	/**
	 * Keep memory of selected values in a backup file
	 */
	private void saveValues() {
		String proxyConfigString = 
				host.getText() + SEPARATOR +
				port.getValue() + SEPARATOR +
				username.getText() + SEPARATOR +
				/*(chckRememberKey.isSelected() ? key.getText() : "")*/ "" + SEPARATOR +
				chckRememberKey.isSelected() + SEPARATOR +
				chckProxySettings.isSelected() + SEPARATOR +
				chckAuthSettings.isSelected();
		
		resourcesFilePathsMappings.saveCommentedProperty("DEF_PROXY_CONFIG_STRING", proxyConfigString);
	}
	
	private void applyDefaultValues(String initString) {
		String[] backUps = initString.split(SEPARATOR);
		int i = 0;
		host.setText(getObjFromBackup(backUps, i++, ""));
		port.setValue(Integer.valueOf(getObjFromBackup(backUps, i++, "8080")));
		username.setText(getObjFromBackup(backUps, i++, ""));
		key.setText(getObjFromBackup(backUps, i++, ""));
		chckRememberKey.setSelected(Boolean.valueOf(getObjFromBackup(backUps, i++, "false")));
		chckProxySettings.setSelected(Boolean.valueOf(getObjFromBackup(backUps, i++, "false")));
		chckAuthSettings.setSelected(Boolean.valueOf(getObjFromBackup(backUps, i++, "false")));
	}
	
	private String getObjFromBackup(String[] parts, int i, String defaultVal) {
		try {
			if (parts[i] != null) {
				return parts[i];
			} else {
				return defaultVal;
			}
		} catch (Exception e) {
			return defaultVal;
		}
	}
	
	private void addHandlers() {
		addWindowListener(new java.awt.event.WindowAdapter() {
	        @Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
	        	
				new Thread(new Runnable() {
					public void run() {
						dispose();
					}
				}).start();
	        }
        });
		
		btnSave.addActionListener((e) -> {
				
			applyProxyData();
			applProxyAuthData();
			
			dispose();
		});
		
		chckProxySettings.addActionListener((e) -> {
			proxyEnabledGUI(chckProxySettings.isSelected());
		});

		chckAuthSettings.addActionListener((e) -> {
			proxyAuthEnabledGUI(chckAuthSettings.isSelected());
		});
		
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
		mainPane.add(component, constraints);
	}
	
	private void applyProxyData() {
		if (chckProxySettings.isSelected() && isCompiled(host)) {
			HttpHelper.setProxy(host.getText(), (int) port.getValue());
		} else {
			HttpHelper.unsetProxy();
		}
		
		saveValues();
	}
	
	private void applProxyAuthData() {
		if(chckAuthSettings.isSelected() && isCompiled(username)) {
			HttpHelper.setDefaultProxyAutentication(username.getText(), key.getPassword());
		} else {
			HttpHelper.removePreviousProxyAuthentication();
		}
	}
	
	private void proxyEnabledGUI(boolean enabled) {
		host.setEnabled(enabled);
		port.setEnabled(enabled);
		chckAuthSettings.setEnabled(enabled);
		proxyAuthEnabledGUI(chckProxySettings.isSelected() ? chckAuthSettings.isSelected() : false);
	}
	private void proxyAuthEnabledGUI(boolean enabled) {
		username.setEnabled(enabled);
		key.setEnabled(enabled);
		chckRememberKey.setEnabled(enabled);
	}
	
	private boolean isCompiled(JTextComponent textComponent) {
		return textComponent.getText() != null && !"".equals(textComponent.getText().trim());
	}

}
