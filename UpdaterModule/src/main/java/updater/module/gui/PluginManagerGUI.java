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
package updater.module.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.io.FilenameUtils;

import net.miginfocom.swing.MigLayout;
import updater.module.plugins.PluginDTO;
import updater.module.plugins.PluginManager;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.ScreepTGuiFactory;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.gui.dialogs.msg.JOptionHelper.DefaultSysIcons;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.string.StringWorker;

public class PluginManagerGUI extends JFrame implements IPluginManagerGui {
	private static final long serialVersionUID = 8830965172863577977L;

	private static SafeLogger logger = new SafeLogger(PluginManagerGUI.class);

	public static JOptionHelper dialogHelper = new JOptionHelper(null);

	private PluginManagerGUI thisPanel;
	private PluginManager pluginManager;

	private JTable table;
	TableRowSorter<DefaultTableModel> sorter;
	private JLabel lblTitleHeaderMessage;
	private JLabel lblSummary;

	public static final String BUTTON_INSTALL = "Install";
	public static final String BUTTON_UNINSTALL = "Uninstall";
	public static final String BUTTON_UPDATE_UNINSTALL = "Uninstall/Update";

	// String name <-> string description <-> boolean already installed <-> checkBox select <-> jbutton uninstall (if installed)
	private static final String[] columnNames = {"Plugin name", "DeScreepTion", "Status", "Plugin Action", "Last Version"};

	public static boolean active = false;

	public JFrame parentFrame;

	public volatile boolean isInstalling = false;

	public Runnable refreshDependencyAction;
	public Runnable afterExitAction;

	private JTableButtonRenderer renderer;
	private ButtonEditor btnEditor;
	private JPanel panel;
	private JTextField textFieldSearch;
	private JLabel lblFilter;
	private JButton btnRefreshFromRemote;

	// constructor
	/**
	 * If pluaginManager is null, a new one will be created
	 * If parent is null, a new one will be created
	 * @param parent
	 * @param pluginManager
	 */
	public PluginManagerGUI(JFrame parent, PluginManager pluginManager, Runnable refreshDependencyCheck){
		afterExitAction = ()->{};
		init(parent, pluginManager, refreshDependencyCheck);
	}

	public void reset() {
		renderer = new JTableButtonRenderer(thisPanel);
		btnEditor = new ButtonEditor(thisPanel);

		if(table != null) {
			table.setDefaultRenderer(JButton.class, renderer);
		}
	}

	public void init(JFrame parent, PluginManager pluginManager, Runnable refreshDependencyCheck) {

		thisPanel = this;

		reset();

		synchronized (dialogHelper) {
			if(dialogHelper == null)
				dialogHelper = new JOptionHelper(parent);
			else
				dialogHelper.setParentComponent(parent);
		}

		active = true;
		parentFrame = parent != null ? parent : new JFrame();
		setLocationRelativeTo(parentFrame);

		thisPanel.setTitle("Plugin Manager Module");

		this.refreshDependencyAction = refreshDependencyCheck;

		this.pluginManager = (pluginManager != null) ? pluginManager : new PluginManager();

		this.setAlwaysOnTop(true);
		getContentPane().setLayout(new MigLayout("insets 0, fill", "[grow]", "[][][grow]"));

		// header: icon, title, subtitle and the counters
		JPanel header = new JPanel(new MigLayout("insets 18 24 8 24, fillx", "[]14[grow][]", "[]0[]"));
		header.add(new JLabel(PluginIcons.of(PluginIcons.Kind.PUZZLE, 36, new Color(0x4FA3FF))), "cell 0 0 1 2");
		JLabel lblTitle = new JLabel("Plugin Manager");
		lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f));
		header.add(lblTitle, "cell 1 0");
		lblTitleHeaderMessage = new JLabel("Choose and install your preferred add-ons");
		lblTitleHeaderMessage.setForeground(UIManager.getColor("Label.disabledForeground"));
		header.add(lblTitleHeaderMessage, "cell 1 1");
		lblSummary = new JLabel(" ");
		lblSummary.setToolTipText("Available plugins, installed ones and updates waiting");
		lblSummary.setForeground(UIManager.getColor("Label.disabledForeground"));
		header.add(lblSummary, "cell 2 0 1 2,aligny center");
		getContentPane().add(header, "cell 0 0,growx");

		// filter and refresh
		panel = new JPanel(new MigLayout("insets 4 24 12 24, fillx", "[]8[grow]12[]", "[]"));
		lblFilter = new JLabel(PluginIcons.of(PluginIcons.Kind.SEARCH, 18));
		lblFilter.setForeground(UIManager.getColor("Label.disabledForeground"));
		panel.add(lblFilter, "cell 0 0");

		String previousFilter = textFieldSearch != null ? textFieldSearch.getText() : "";
		textFieldSearch = new JTextField();
		textFieldSearch.setText(previousFilter);
		textFieldSearch.putClientProperty("JTextField.placeholderText", "Filter plugins by name, description or status...");
		textFieldSearch.setToolTipText("Type to filter the list (the text is matched on name, description and status)");
		panel.add(textFieldSearch, "cell 1 0,growx,h 36!");

		btnRefreshFromRemote = new JButton("Refresh from remote", PluginIcons.of(PluginIcons.Kind.REFRESH, 16));
		btnRefreshFromRemote.setToolTipText("Download the list of the available plugins again");
		btnRefreshFromRemote.setCursor(GuiUtils.CURSOR_HAND);
		panel.add(btnRefreshFromRemote, "cell 2 0,h 36!");
		getContentPane().add(panel, "cell 0 1,growx");

		table = new JTable();
		table.setAutoCreateRowSorter(true);

		JScrollPane tableContainer = new JScrollPane(table);
		tableContainer.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor") != null ? UIManager.getColor("Component.borderColor") : Color.GRAY));
		JPanel body = new JPanel(new java.awt.BorderLayout());
		body.setBorder(BorderFactory.createEmptyBorder(0, 24, 20, 24));
		body.add(tableContainer, java.awt.BorderLayout.CENTER);
		table.setCellSelectionEnabled(false);
		table.setFillsViewportHeight(true);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(false);
		table.setRowHeight(50);
		getContentPane().add(body, "cell 0 2,grow");

		addHandlers();

		try {
			refreshTableOnly(false);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		table.setEnabled(true);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		SwingUtilities.invokeLater(()->{
			GuiUtils.centerComponent(this, 1400, 750);
		});

		setInstalling(false);
	}

	@Override
	public void refreshGuiLibsAfterChange() {
		GuiUtils.launchThreadSafeSwing(refreshDependencyAction);
	}

	private void addHandlers() {

		// Esc closes the window, like the X of the title bar
		getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "close-plugin-manager");
		getRootPane().getActionMap().put("close-plugin-manager", new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				dispatchEvent(new java.awt.event.WindowEvent(thisPanel, java.awt.event.WindowEvent.WINDOW_CLOSING));
			}
		});

		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {

	        	logger.debug("Closing Frame: " + getClass().getName());
	        	afterExitAction.run();
	        	dispose();
		    }
		});

		// default close operation(same as reset)
		thisPanel.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {

		    	if(isInstalling) {
		    		if(!askIfSureToClose()) {
		    			return;
		    		} else {
		    			// TODO kill download/extract process (if install started then user cannot stop it) with boolean signal var
		    		}
		    	}

		    	active = false;

		    	dispose();
		    }
		});

		table.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if(isInstalling) {
					table.setEnabled(false);
					btnRefreshFromRemote.setEnabled(false);
				} else {
					table.setEnabled(true);
					btnRefreshFromRemote.setEnabled(true);
				}
			}
		});

		// listener for row selected: if there is no installer disable button
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(table.getSelectedRow() != -1){
					try{
						// Nothing to do on selection change
					}catch(ArrayIndexOutOfBoundsException ex) {
						logger.debug("Table selection listener triggered, but no rows selected ");
					}
				} // END IF sel != -1
			}
		}); // END table listener

		textFieldSearch.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				try {
					applyFilter();
				} catch (Throwable e1) {
					logger.error("Cannot filter and refresh table!", e1);
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				try {
					applyFilter();
				} catch (Throwable e1) {
					logger.error("Cannot filter and refresh table!", e1);
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				try {
					applyFilter();
				} catch (Throwable e1) {
					logger.error("Cannot filter and refresh table!", e1);
				}
			}
		});

		btnRefreshFromRemote.addActionListener((e)->{
			try {
				restart();
			} catch (Throwable e1) {
				logger.error("Cannot refresh table after user click on button refresh", e1);
			}
		});

		btnRefreshFromRemote.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if(isInstalling)
					btnRefreshFromRemote.setEnabled(false);
				else
					btnRefreshFromRemote.setEnabled(true);
			}
		});
	}

	@Override
	public void restart() throws TimeoutException, Throwable {
		if (!isInstalling) {
			final Image icon = this.getIconImage();
			final Runnable afterActionBackup = afterExitAction;
			afterExitAction = () -> {};
			dispose();
			pluginManager.discoverLatestPlugins();
			GuiUtils.launchThreadSafeSwing(() -> {
				PluginManagerGUI gui = new PluginManagerGUI(parentFrame, pluginManager, refreshDependencyAction);
				gui.setIconImage(icon);
				gui.setVisible(true);
				gui.toFront();
				gui.setAfterExitAction(afterActionBackup);
			});
		}
	}

	private String getRegexFromFilter() {
		String expr = StringWorker.trimToEmpty(textFieldSearch.getText());
//		boolean emptyExpression = StringWorker.isEmpty(expr);
//
//		String regex = emptyExpression
//				? ""
//				: StringWorker.getWildcardRegex(textFieldSearch.getText(), true);

		return "(?i)" + expr;
	}

	private void applyModel() {

	    PluginTableModel model = new PluginTableModel(null, columnNames);
		table.setModel(model);
		sorter = new TableRowSorter<DefaultTableModel>((DefaultTableModel)table.getModel());
		table.setRowSorter(sorter);

	}

	private void applyFilter() {
		RowFilter<DefaultTableModel, Object> rf = null;
		try {
			rf = RowFilter.regexFilter(getRegexFromFilter(), 0, 1, 2);
		} catch (java.util.regex.PatternSyntaxException e) {
			return;
		}
		sorter.setRowFilter(rf);
	}

	public class PluginTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 2183863172814214653L;


		public PluginTableModel(Object[][] data, Object[] columnNames) {
			super(data, columnNames);
		}


		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 3 || column == 1;
		}
	}

	private void setTableModel() {

		applyModel();

		table.setCellSelectionEnabled(false);
		table.setFillsViewportHeight(true);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(true);
		table.setIntercellSpacing(new Dimension(0, 1));
		table.setRowHeight(50);

		table.getTableHeader().setDefaultRenderer(new HeaderRenderer(table));
		table.getTableHeader().setVisible(true);
		table.getTableHeader().setResizingAllowed(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.getColumn(columnNames[0]).setCellRenderer(new PluginNameCellRenderer());
		table.getColumn(columnNames[1]).setCellRenderer(new NormalStringCellRenderer());
		table.getColumn(columnNames[2]).setCellRenderer(new StatusCellRenderer());

		table.getColumn(columnNames[3]).setCellRenderer(renderer);
		table.getColumn(columnNames[3]).setCellEditor(btnEditor);

		table.getColumn(columnNames[4]).setCellRenderer(new WarningsCellRenderer());

		table.getColumnModel().getColumn(0).setPreferredWidth(220);
		table.getColumnModel().getColumn(0).setMinWidth(160);
		table.getColumnModel().getColumn(1).setPreferredWidth(430);
		table.getColumnModel().getColumn(2).setPreferredWidth(230);
		table.getColumnModel().getColumn(3).setPreferredWidth(190);
		table.getColumnModel().getColumn(3).setMinWidth(190);
		table.getColumnModel().getColumn(4).setPreferredWidth(90);
		table.getColumnModel().getColumn(4).setMinWidth(70);
	}

	/** Plugin name with a colored initial tile */
	public class PluginNameCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 4301223018873501241L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			String name = value == null ? "" : value.toString();
			label.setIcon(PluginIcons.avatar(name, 30));
			label.setIconTextGap(12);
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 4));
			return label;
		}
	}

	/** Installed / update available / not installed, with a colored icon */
	public class StatusCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 7213340989164021017L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			PluginDTO plugin = getPluginDTO((String) table.getValueAt(row, 0));
			String muted = "#8b949e";
			if (plugin == null || !plugin.installationCompleted) {
				label.setIcon(PluginIcons.of(PluginIcons.Kind.INFO, 18, PluginActionStyle.MUTED));
				label.setText("<html><b>Not installed</b><br><font color=" + muted + ">Available to install</font></html>");
				label.setToolTipText("Not installed yet");
			} else if (plugin.isUpToDate()) {
				label.setIcon(PluginIcons.of(PluginIcons.Kind.CHECK, 18, PluginActionStyle.GREEN));
				label.setText("<html><b>Installed</b><br><font color=" + muted + ">Version " + plugin.getInstalledVersion() + "</font></html>");
				label.setToolTipText("Installed and up to date");
			} else {
				label.setIcon(PluginIcons.of(PluginIcons.Kind.UPDATE, 18, PluginActionStyle.ORANGE));
				label.setText("<html><b>Update available</b><br><font color=" + muted + ">" + plugin.getInstalledVersion() + " \u2192 " + plugin.getLastVersion() + "</font></html>");
				label.setToolTipText("Installed version " + plugin.getInstalledVersion() + ", latest version " + plugin.getLastVersion());
			}
			label.setIconTextGap(10);
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
			return label;
		}
	}

	public class WarningsCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -6124028505039452207L;

		@Override
		public Component getTableCellRendererComponent(
	                        JTable table, Object value,
	                        boolean isSelected, boolean hasFocus,
	                        int row, int column) {

	    	String pluginName = (String)table.getValueAt(row, 0);
			PluginDTO plugin = getPluginDTO(pluginName);
	        JLabel labelItem = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	        String warningHtml = plugin.getWarnings().isEmpty()
	        		? null
	        		: GuiUtils.encapsulateInHtml("<b>Warnings found:</b> <br><br>" + GuiUtils.getAsHtmlList(plugin.getWarnings())); // Could be value.toString()

//	        warningHtml = GuiUtils.encapsulateInHtml("<b>This is a test:</b> <br>" + plugin.getName()); // TEST
	        labelItem.setOpaque(false);
	        if(warningHtml != null) {
	        	labelItem.setToolTipText(warningHtml);
	        	labelItem.setFont(labelItem.getFont().deriveFont(Font.BOLD));
				labelItem.setIcon(PluginIcons.of(PluginIcons.Kind.WARNING, 16, PluginActionStyle.ORANGE));
	        } else {
	        	labelItem.setToolTipText(null);
	        	labelItem.setFont(labelItem.getFont().deriveFont(Font.PLAIN));
				labelItem.setIcon(null);
	        }
	        labelItem.setHorizontalAlignment(JLabel.CENTER);
	        return labelItem;
	    }
	}

	public class NormalStringCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1453358225111362742L;

		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object value,
				boolean isSelected, boolean hasFocus,
				int row, int column) {

			String description = (String)table.getValueAt(row, 1);
			JLabel labelItem = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			labelItem.setToolTipText(description);
			return labelItem;
		}
	}

	@Override
	public void refreshTable(boolean useCache) throws Throwable {

		logger.debug("Refreshing table...");

		if(isInstalling) {
			return;
		}

		init(parentFrame, pluginManager, refreshDependencyAction);

		logger.debug("Table refreshed!");
	}

	public void refreshTableOnly(boolean useCache) throws Throwable {

		logger.debug("Refreshing table...");

		if(isInstalling) {
			return;
		}

		// first clear the table
		table.removeAll();
		setTableModel();

		ArrayList<PluginDTO> pluginList = (useCache) ?  pluginManager.retrieveAllFromCache() : pluginManager.discoverLatestPlugins();
		Collections.sort(pluginList);

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		int installedCount = 0;
		int toUpdateCount = 0;
		for(PluginDTO current : pluginList) {
			String noExtensionName = FilenameUtils.removeExtension(current.getName());
			String description = current.getDescription();
			boolean installed = current.isInstallationCompleted();
			if (installed) {
				installedCount++;
				if (!current.isUpToDate()) {
					toUpdateCount++;
				}
			}
			String alreadyInstalledStr = "Already installed (" + current.getInstalledVersion() + ")";
			String notUpToDateStr = "Installed - to update (Installed version: " + current.getInstalledVersion() + ")";
			alreadyInstalledStr = current.isUpToDate() ? alreadyInstalledStr : notUpToDateStr;
			String installedText = (installed) ? alreadyInstalledStr : "Not yet Installed";

			JButton installOrRemoveBtn = new JButton(ButtonEditor.getButtonString(current, false));

			installOrRemoveBtn.setEnabled(true);
			model.addRow(new Object[]{noExtensionName, description, installedText, installOrRemoveBtn, current.getLastVersion()});
		}

		lblSummary.setText(pluginList.size() + " plugins   \u2022   " + installedCount + " installed   \u2022   " + toUpdateCount + " to update");
		table.revalidate();
		table.repaint();
		table.setEnabled(true);
		logger.debug("Table refreshed!");
	}

	private boolean askIfSureToClose() {
		String msg = "Are you sure to close Plugin Manager? There is an installation in progress. It will be interrupted";
		if (dialogHelper.showYNOrNullDialogWarn(msg, "Exit plugin manager?")) {
			return true;
		}else {
			return false;
		}
	}

	@Override
	public PluginDTO getPluginDTO(String nameWithoutExtension) {
		PluginDTO dto = pluginManager.retrieveFromCacheByNameNoExtension(nameWithoutExtension);

		if(dto == null)
			pluginManager.refreshCache(nameWithoutExtension);

		return pluginManager.retrieveFromCacheByNameNoExtension(nameWithoutExtension);
	}

	@Override
	public void setInstalling(boolean installing) {
		isInstalling = installing;
	}

	@Override
	public JFrame getParentFrame() {
		return parentFrame;
	}

	@Override
	public JFrame getFrame() {
		return thisPanel;
	}

	@Override
	public PluginManager getPluginManager() {
		return pluginManager != null ? pluginManager : new PluginManager();
	}

	@Override
	public boolean isInstalling() {
		return isInstalling;
	}

	@Override
	public JTable getTable() {
		return table;
	}

	public Runnable getAfterExitAction() {
		return afterExitAction;
	}

	public void setAfterExitAction(Runnable afterExitAction) {
		this.afterExitAction = afterExitAction;
	}

}
