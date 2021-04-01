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
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.io.FilenameUtils;

import net.miginfocom.swing.MigLayout;
import updater.module.plugins.PluginDTO;
import updater.module.plugins.PluginManager;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.log.SafeLogger;

public class PluginManagerGUI extends JFrame implements IPluginManagerGui {
	private static final long serialVersionUID = 8830965172863577977L;

	private static SafeLogger logger = new SafeLogger(PluginManagerGUI.class);

	public static JOptionHelper dialogHelper = new JOptionHelper(null);

	private PluginManagerGUI thisPanel;
	private PluginManager pluginManager;

	private JTable table;
	private JLabel lblTitleHeaderMessage;

	private static final String BUTTON_INSTALL = "Install";
	private static final String BUTTON_UNINSTALL = "Uninstall";

	// String name <-> string description <-> boolean already installed <-> checkBox select <-> jbutton uninstall (if installed)
	private static final String[] columnNames = {"Plugin name", "DeScreepTion", "Status", "Plugin Action"};

	public static boolean active = false;

	public JFrame parentFrame;

	public volatile boolean isInstalling = false;

	public Runnable refreshDependencyAction;

	private JTableButtonRenderer renderer;
	private ButtonEditor btnEditor;

	// constructor
	/**
	 * If pluaginManager is null, a new one will be created
	 * If parent is null, a new one will be created
	 * @param parent
	 * @param pluginManager
	 */
	public PluginManagerGUI(JFrame parent, PluginManager pluginManager, Runnable refreshDependencyCheck){
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
		thisPanel.setTitle("Plugin Manager Module");

		this.refreshDependencyAction = refreshDependencyCheck;

		this.pluginManager = (pluginManager != null) ? pluginManager : new PluginManager();

		UIManager.put("ToolTip.background", new Color(255, 250, 205));
		this.setAlwaysOnTop(true);
		getContentPane().setBackground(Color.DARK_GRAY);
		getContentPane().setLayout(new MigLayout("", "[50%,grow][50%,grow]", "[50px:80px][grow]"));

		lblTitleHeaderMessage = new JLabel("Welcome to Plugin Manager, choose and install your preferred add-ons ;)");
		lblTitleHeaderMessage.setForeground(new Color(211, 211, 211));
		lblTitleHeaderMessage.setBackground(Color.BLACK);
		lblTitleHeaderMessage.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		lblTitleHeaderMessage.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitleHeaderMessage.setHorizontalTextPosition(SwingConstants.CENTER);
		lblTitleHeaderMessage.setOpaque(true);
		lblTitleHeaderMessage.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		getContentPane().add(lblTitleHeaderMessage, "cell 0 0 2 1,grow");

		table = new JTable();

//		renderer = new JTableButtonRenderer(thisPanel);
//		table.setDefaultRenderer(JButton.class, renderer);

		JScrollPane tableContainer = new JScrollPane(table);
		table.setBorder(new LineBorder(new Color(0, 0, 0)));
		table.setCellSelectionEnabled(false);
		table.setFillsViewportHeight(true);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(true);
		table.setRowHeight(30);
		getContentPane().add(tableContainer, "cell 0 1 2 1,grow");

		addHandlers();

		try {
			refreshTableOnly(false);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		table.setEnabled(true);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		SwingUtilities.invokeLater(()->{
			GuiUtils.centerComponent(this, 1300, 350);
		});

		setInstalling(false);
	}

	@Override
	public void refreshGuiLibsAfterChange() {
		GuiUtils.launchThreadSafeSwing(refreshDependencyAction);
	}

	private void addHandlers() {

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
				if(isInstalling)
					table.setEnabled(false);
				else
					table.setEnabled(true);
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

	}

	private void setTableModel() {
		DefaultTableModel model = new DefaultTableModel(null, columnNames) {
			private static final long serialVersionUID = 2183863172814214653L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return true;
			}
		};

		table.setModel(model);

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

		table.getColumn(columnNames[3]).setCellRenderer(renderer);
		table.getColumn(columnNames[3]).setCellEditor(btnEditor);

		table.getColumnModel().getColumn(0).setPreferredWidth(110);
		table.getColumnModel().getColumn(0).setMinWidth(110);
		table.getColumnModel().getColumn(1).setPreferredWidth(600);
		table.getColumnModel().getColumn(2).setPreferredWidth(110);
		table.getColumnModel().getColumn(3).setPreferredWidth(150);
		table.getColumnModel().getColumn(3).setMinWidth(150);

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

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		for(PluginDTO current : pluginList) {
			String noExtensionName = FilenameUtils.removeExtension(current.getName());
			String description = current.getDescription();
			boolean installed = current.isInstallationCompleted();
			String installedText = (installed) ? "Already installed!" : "Not yet Installed";

			JButton installOrRemoveBtn = (installed) ? new JButton(BUTTON_UNINSTALL) : new JButton(BUTTON_INSTALL);
			installOrRemoveBtn.setEnabled(true);
			model.addRow(new Object[]{noExtensionName, description, installedText, installOrRemoveBtn});
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
}
