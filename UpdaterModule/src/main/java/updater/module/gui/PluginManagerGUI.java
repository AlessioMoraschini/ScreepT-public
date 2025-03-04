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

		UIManager.put("ToolTip.background", new Color(255, 250, 205));
		this.setAlwaysOnTop(true);
		getContentPane().setBackground(Color.DARK_GRAY);
		getContentPane().setLayout(new MigLayout("", "[50%,grow][50%,grow]", "[32px:40px][40px:40px:40px][20px][grow]"));

		lblTitleHeaderMessage = new JLabel("Welcome to Plugin Manager, choose and install your preferred add-ons ;)");
		lblTitleHeaderMessage.setForeground(new Color(211, 211, 211));
		lblTitleHeaderMessage.setBackground(Color.BLACK);
		lblTitleHeaderMessage.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		lblTitleHeaderMessage.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitleHeaderMessage.setHorizontalTextPosition(SwingConstants.CENTER);
		lblTitleHeaderMessage.setOpaque(true);
		lblTitleHeaderMessage.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		getContentPane().add(lblTitleHeaderMessage, "cell 0 0 2 1,grow");

		panel = new JPanel();
		panel.setBorder(new LineBorder(Color.GRAY, 1, true));
		panel.setBackground(new Color(64, 64, 64));
		getContentPane().add(panel, "cell 0 1 2 1,grow");
		panel.setLayout(new MigLayout("", "[::10px][][grow][170px:n][::10px]", "[45px:45px:45px]"));

		lblFilter = new JLabel("Filter:");
		lblFilter.setForeground(Color.LIGHT_GRAY);
		lblFilter.setMinimumSize(new Dimension(60, 45));
		lblFilter.setPreferredSize(new Dimension(60, 45));
		lblFilter.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		lblFilter.setHorizontalTextPosition(SwingConstants.CENTER);
		lblFilter.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(lblFilter, "cell 1 0,alignx right,growy");

		String previousFilter = textFieldSearch != null ? textFieldSearch.getText() : "";
		textFieldSearch = ScreepTGuiFactory.getTextField(Color.WHITE, Color.DARK_GRAY, null, new Font("Segoe UI", Font.PLAIN, 17), false);
		textFieldSearch.setText(previousFilter);
		textFieldSearch.setMinimumSize(new Dimension(10, 45));
		textFieldSearch.setPreferredSize(new Dimension(10, 45));
		textFieldSearch.setColumns(10);
		panel.add(textFieldSearch, "cell 2 0,growx,aligny center");

		btnRefreshFromRemote = new JButton("Refresh from remote");
		btnRefreshFromRemote.setForeground(Color.WHITE);
		btnRefreshFromRemote.setBackground(Color.DARK_GRAY);
		btnRefreshFromRemote.setCursor(GuiUtils.CURSOR_HAND);
		btnRefreshFromRemote.setOpaque(false);
		panel.add(btnRefreshFromRemote, "cell 3 0,alignx right,aligny center");

		table = new JTable();
		table.setAutoCreateRowSorter(true);
//		table.setPreferredScrollableViewportSize(new Dimension(450, 350));

//		renderer = new JTableButtonRenderer(thisPanel);
//		table.setDefaultRenderer(JButton.class, renderer);

		JScrollPane tableContainer = new JScrollPane(table);
		table.setBorder(new LineBorder(new Color(0, 0, 0)));
		table.setCellSelectionEnabled(false);
		table.setFillsViewportHeight(true);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(true);
		table.setRowHeight(30);
		getContentPane().add(tableContainer, "cell 0 3 2 1,grow");

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
		table.setShowVerticalLines(true);
		table.setRowHeight(30);

//		table.setModel(new PluginTableModel(null, columnNames));

		table.setGridColor(Color.DARK_GRAY);
		table.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		table.setBackground(Color.WHITE);
		table.getTableHeader().setDefaultRenderer(new HeaderRenderer(table));
		table.getTableHeader().setFont(new Font("Segoe UI Black", Font.PLAIN, 14));
		table.getTableHeader().setVisible(true);
		table.getTableHeader().setResizingAllowed(true);
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setSelectionBackground(new Color(3, 127, 252, 30));
		table.setSelectionForeground(Color.BLACK);

		table.getColumn(columnNames[1]).setCellRenderer(new NormalStringCellRenderer());

		table.getColumn(columnNames[3]).setCellRenderer(renderer);
		table.getColumn(columnNames[3]).setCellEditor(btnEditor);

		table.getColumn(columnNames[4]).setCellRenderer(new WarningsCellRenderer());

		table.getColumnModel().getColumn(0).setPreferredWidth(110);
		table.getColumnModel().getColumn(0).setMinWidth(110);
		table.getColumnModel().getColumn(1).setPreferredWidth(450);
		table.getColumnModel().getColumn(2).setPreferredWidth(250);
		table.getColumnModel().getColumn(3).setPreferredWidth(200);
		table.getColumnModel().getColumn(3).setMinWidth(200);
		table.getColumnModel().getColumn(4).setPreferredWidth(40);
		table.getColumnModel().getColumn(4).setMinWidth(40);
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
	        	try {
					labelItem.setIcon(GuiUtils.getDefaultSystemIcon(DefaultSysIcons.WARN, 16, 16));
				} catch (IOException e) {
					e.printStackTrace();
				}
	        } else {
	        	labelItem.setToolTipText(null);
	        	labelItem.setFont(labelItem.getFont().deriveFont(Font.PLAIN));
				labelItem.setIcon(new ImageIcon());
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
		for(PluginDTO current : pluginList) {
			String noExtensionName = FilenameUtils.removeExtension(current.getName());
			String description = current.getDescription();
			boolean installed = current.isInstallationCompleted();
			String alreadyInstalledStr = "Already installed (" + current.getInstalledVersion() + ")";
			String notUpToDateStr = "Installed - to update (Installed version: " + current.getInstalledVersion() + ")";
			alreadyInstalledStr = current.isUpToDate() ? alreadyInstalledStr : notUpToDateStr;
			String installedText = (installed) ? alreadyInstalledStr : "Not yet Installed";

			JButton installOrRemoveBtn = new JButton(ButtonEditor.getButtonString(current, false));

			installOrRemoveBtn.setEnabled(true);
			model.addRow(new Object[]{noExtensionName, description, installedText, installOrRemoveBtn, current.getLastVersion()});
		}

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
