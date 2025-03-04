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
package hw.commons.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;

import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import hw.commons.om.HDinfo;
import net.miginfocom.swing.MigLayout;
import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.GuiUtils;
import various.common.light.om.LimitedConcurrentList;
import various.common.light.runtime.RuntimeUtil;
import various.common.light.utility.manipulation.ConversionUtils;

public class HwInfoFrame extends ParentFrame{
	private static final long serialVersionUID = -2119678215026217031L;

	public static HwInfoFrame singletonInstance = null;

	// KeyGenPanel logger
	static Logger logger = Logger.getLogger(HwInfoFrame.class);

	private static final String errMSG = "### ERROR ###";
	private static final String UNSELECTED = "##############";

	private static final int THREAD_REFRESH_RATE = 1000;

	// GUI elements
	private JScrollPane scrollPane;
	private JTextArea textAreaDisplay;
	private JPanel panel;
	private JTextField dynLblCpuVoltage;
	private JTextField dynLblCpuTemp;
	private JTextField dynLblCpuAvgUsage;
	private JTextField dynLblCpuUsage;
	private JLabel lblUsage;
	private JLabel lblAvg;
	private JLabel lblTemperature;
	private JLabel lblVoltage;
	private JLabel lblHardwareInfoTool;
	private JPanel panelHeaderTitle;
	private JScrollPane ConncetionInfoScrollPanel;
	private JPanel mainContainerPanel;
	private JLabel lblJavaVersion;
	private JLabel lblOsName;
	private JTextField dynLblOsName;
	private JTextField dynLblJavaVersion;
	private JLabel lblOsVersion;
	private JTextField dynLblOsVersion;
	private JLabel lblRam;
	private JLabel lblFreeRam;
	private JLabel lblUsedRam;
	private JTextField dynLblRam;
	private JTextField dynLblFreeRam;
	private JTextField dynLblUsedRam;
	private JSeparator separator;
	private JPanel panelRAM;
	private JLabel lblArchitecture;
	private JTextField dynLblArch;
	private JPanel panelGeneral;
	private JPanel panelCPU;
	private JLabel lblCPU;
	private JTextField dynLblCPU;
	private JLabel lblCpuCores;
	private JTextField dynLblCPUNofCores;
	private JLabel lblVendor;
	private JTextField dynLblCPUVendor;
	private JLabel lblNofSockets;
	private JTextField dynLblCPUNofSockets;
	private JLabel lblCpuFrequency;
	private JLabel lblLogicalCores;
	private JTextField dynLblCpuFrequency;
	private JTextField dynLblLogicalCores;
	private JPanel panelMOBO;
	private JLabel lblMotherboard;
	private JTextField dynLblMoboModel;
	private JLabel lblMoboVendor;
	private JTextField dynLblMoboVendor;
	private JTextField dynLblMoboSerialNumber;
	private JLabel lblSerialNumber;
	private JPanel panelHD;
	private JLabel lblHd;
	private JComboBox<HDinfo> comboSelectedHd;
	private JLabel lblReads;
	private JLabel lblHDName;
	private JTextField dynLblHDReadBytes;
	private JTextField dynLblHDmodel;
	private JPanel panelMonitors;
	private JLabel lblDisplay;
	private JComboBox<Integer> comboSelectedDisplay;
	private JLabel lblName;
	private JTextField dynLblDisplayInfo1;

	// system variables
	private SystemInfo sysINFO;
	private GlobalMemory memoryRam;
	private CentralProcessor cpu;
	private Baseboard MOBO;
	private Display[] displays;
	private Integer[] displayIndexer;
	private LimitedConcurrentList<Double> cpuUsageHystory;
	private HDinfo[] HDList;

	// network related objects
	public HwInfoFrame thisPanel;
	private JLabel lblWrites;
	private JTextField dynLblHDWriteBytes;
	private JLabel lblHDCapacity;
	private JLabel lblHDSerial;
	private JTextField dynLblHDCapacity;
	private JTextField dynLblHDSerial;
	private JTextField dynLblHdBytesWritten;
	private JTextField dynLblHdBytesRead;
	private JLabel lblBytesWritten;
	private JLabel lblBytesRead;
	private JLabel lblVersion;
	private JTextField dynLblMoboVersion;
	private JPanel screeptInfoPanel;
	private JLabel lblScreeptCpuUsage;
	private JTextField dynLblScreeptRamUsage;
	private JTextField dynLblScreeptCpuPerc;
	private JLabel lblScreeptRamUsage;

	long currentPid;

////////////////////////// CONSTRUCTOR //////////////////////////

	public HwInfoFrame() {
		super();

		isActive.set(true);

		logger.info(this.getClass().getName() + " Starting...");

		setTitle("Hardware Info Frame");

		// SW window icon
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_HW_INFO, this);

		thisPanel = this;
		singletonInstance = this;

		// Initialize hardware utils objects
		initSystemVars();

		// GUI initializer
		initialize();
		addEventHandlers();

		// Initialize static labels
		updateGenInfo();
		updateMoboInfo();
		updateCpuInfo();
		updateDisplayInfo();

		// START THREAD VALUES UPDATER
		GuiUtilsExtended.launchThreadSafeSwing(new Runnable() {
			@Override
			public void run() {


				while(thisPanel.isActive.get()) {

					updateRamInfo();
					updateHdInfo();
					updateCpuUsageInfo();
					updateScreeptResourcesUsage();
					try {
						Thread.sleep(THREAD_REFRESH_RATE);
					} catch (InterruptedException e) {
						logger.error(Arrays.toString(e.getStackTrace()));
					}

				}
				logger.info(HwInfoFrame.this.getClass().getName() + " Started - OUTPUT_CODE: HWINFOPANEL_STARTED_OK");
			}
		});

		singletonInstance.pack();
		GuiUtils.centerComponent(singletonInstance, getDefaultDimension());

	}

////////////////////////// INITIALIZER METHOD //////////////////////////

	private void initialize() {

		getContentPane().setBackground(Color.DARK_GRAY);
		getContentPane().setLayout(new MigLayout("", "[50px,grow][25%,grow][60%,grow][50px,grow]", "[50px][60px][75%,grow]"));

		panelHeaderTitle = new JPanel();
		panelHeaderTitle.setMinimumSize(new Dimension(100, 10));
		panelHeaderTitle.setForeground(SystemColor.menu);
		panelHeaderTitle.setBorder(new LineBorder(Color.LIGHT_GRAY));
		panelHeaderTitle.setBackground(Color.BLACK);
		getContentPane().add(panelHeaderTitle, "cell 0 0 4 1,grow");
		panelHeaderTitle.setLayout(new MigLayout("fill, inset 0", "[50%,grow,center][45%,grow,center]", "[:20px:45px,fill]"));

		lblHardwareInfoTool = new JLabel("  Hardware Info Tool");
		lblHardwareInfoTool.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblHardwareInfoTool.setHorizontalTextPosition(SwingConstants.CENTER);
		lblHardwareInfoTool.setHorizontalAlignment(SwingConstants.CENTER);
		lblHardwareInfoTool.setForeground(new Color(240, 248, 255));
		lblHardwareInfoTool.setFont(new Font("Lithos Pro Regular", Font.BOLD, 16));
		lblHardwareInfoTool.setBackground(SystemColor.activeCaptionBorder);
		panelHeaderTitle.add(lblHardwareInfoTool, "cell 0 0,alignx left,aligny center");

		screeptInfoPanel = new JPanel();
		screeptInfoPanel.setMaximumSize(new Dimension(32767, 30));
		screeptInfoPanel.setPreferredSize(new Dimension(10, 25));
		screeptInfoPanel.setBorder(new LineBorder(new Color(192, 192, 192), 0));
		screeptInfoPanel.setBackground(Color.BLACK);
		panelHeaderTitle.add(screeptInfoPanel, "cell 1 0,growx,aligny center");
		screeptInfoPanel.setLayout(new MigLayout("fill, inset 1", "[90px:n][80,grow][90px:n][100px,grow]", "[::30px,grow,fill]"));

		lblScreeptCpuUsage = new JLabel("ScreepT Cpu");
		lblScreeptCpuUsage.setForeground(Color.LIGHT_GRAY);
		lblScreeptCpuUsage.setFont(new Font("Segoe UI", Font.BOLD, 14));
		screeptInfoPanel.add(lblScreeptCpuUsage, "cell 0 0,alignx center,growy");

		dynLblScreeptCpuPerc = new JTextField(UNSELECTED);
		dynLblScreeptCpuPerc.setPreferredSize(new Dimension(132, 18));
		dynLblScreeptCpuPerc.setMinimumSize(new Dimension(6, 18));
		dynLblScreeptCpuPerc.setMaximumSize(new Dimension(2147483647, 40));
		dynLblScreeptCpuPerc.setForeground(new Color(72, 61, 139));
		dynLblScreeptCpuPerc.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblScreeptCpuPerc.setEditable(false);
		screeptInfoPanel.add(dynLblScreeptCpuPerc, "cell 1 0,growx,aligny center");

		lblScreeptRamUsage = new JLabel("ScreepT Ram");
		lblScreeptRamUsage.setForeground(Color.LIGHT_GRAY);
		lblScreeptRamUsage.setFont(new Font("Segoe UI", Font.BOLD, 14));
		screeptInfoPanel.add(lblScreeptRamUsage, "cell 2 0,alignx center,growy");

		dynLblScreeptRamUsage = new JTextField(UNSELECTED);
		dynLblScreeptRamUsage.setPreferredSize(new Dimension(132, 18));
		dynLblScreeptRamUsage.setMinimumSize(new Dimension(6, 18));
		dynLblScreeptRamUsage.setMaximumSize(new Dimension(2147483647, 40));
		dynLblScreeptRamUsage.setForeground(new Color(72, 61, 139));
		dynLblScreeptRamUsage.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblScreeptRamUsage.setEditable(false);
		screeptInfoPanel.add(dynLblScreeptRamUsage, "cell 3 0,growx,aligny center");

		ConncetionInfoScrollPanel = new JScrollPane();
		ConncetionInfoScrollPanel.setBackground(Color.WHITE);
		ConncetionInfoScrollPanel.setBorder(new LineBorder(Color.DARK_GRAY, 4));
		getContentPane().add(ConncetionInfoScrollPanel, "cell 0 1 4 2,grow");

		mainContainerPanel = new JPanel();
		mainContainerPanel.setBackground(new Color(211, 211, 211));
		ConncetionInfoScrollPanel.setViewportView(mainContainerPanel);
		mainContainerPanel.setLayout(new MigLayout("", "[12.5%,grow][12.5%,grow][12.5%,grow][12.5%,grow]", "[50,grow][5][50,grow][50,grow][50,grow][40,grow][40,grow][40,grow][40,grow][40][80,grow]"));

		panelGeneral = new JPanel();
		panelGeneral.setBackground(new Color(0, 0, 0));
		mainContainerPanel.add(panelGeneral, "cell 0 0 4 1,grow");
		panelGeneral.setLayout(new MigLayout("", "[100][grow][grow][grow][grow][grow][grow][grow]", "[grow]"));

		lblArchitecture = new JLabel("Architecture:");
		lblArchitecture.setForeground(Color.LIGHT_GRAY);
		panelGeneral.add(lblArchitecture, "cell 0 0,alignx center");
		lblArchitecture.setFont(new Font("Segoe UI", Font.BOLD, 16));

		dynLblArch = new JTextField(UNSELECTED);
		panelGeneral.add(dynLblArch, "cell 1 0,growx");
		dynLblArch.setForeground(new Color(72, 61, 139));
		dynLblArch.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblArch.setEditable(false);

		lblOsName = new JLabel("OS Name");
		lblOsName.setForeground(Color.LIGHT_GRAY);
		panelGeneral.add(lblOsName, "cell 2 0,alignx center");
		lblOsName.setFont(new Font("Segoe UI", Font.BOLD, 16));

		dynLblOsName = new JTextField(UNSELECTED);
		panelGeneral.add(dynLblOsName, "cell 3 0,growx");
		dynLblOsName.setEditable(false);
		dynLblOsName.setForeground(new Color(72, 61, 139));
		dynLblOsName.setFont(new Font("Segoe UI", Font.BOLD, 14));

		lblOsVersion = new JLabel("OS Version");
		lblOsVersion.setForeground(Color.LIGHT_GRAY);
		panelGeneral.add(lblOsVersion, "cell 4 0,alignx center");
		lblOsVersion.setFont(new Font("Segoe UI", Font.BOLD, 16));

		dynLblOsVersion = new JTextField(UNSELECTED);
		panelGeneral.add(dynLblOsVersion, "cell 5 0,growx");
		dynLblOsVersion.setEditable(false);
		dynLblOsVersion.setForeground(new Color(72, 61, 139));
		dynLblOsVersion.setFont(new Font("Segoe UI", Font.BOLD, 14));

		lblJavaVersion = new JLabel("Java Version");
		lblJavaVersion.setForeground(Color.LIGHT_GRAY);
		panelGeneral.add(lblJavaVersion, "cell 6 0,alignx center");
		lblJavaVersion.setFont(new Font("Segoe UI", Font.BOLD, 16));

		dynLblJavaVersion = new JTextField(UNSELECTED);
		panelGeneral.add(dynLblJavaVersion, "cell 7 0,growx");
		dynLblJavaVersion.setEditable(false);
		dynLblJavaVersion.setForeground(new Color(72, 61, 139));
		dynLblJavaVersion.setFont(new Font("Segoe UI", Font.BOLD, 14));

		separator = new JSeparator();
		separator.setForeground(Color.BLACK);
		separator.setBackground(Color.DARK_GRAY);
		mainContainerPanel.add(separator, "cell 0 1 4 1,growx");

		panelRAM = new JPanel();
		panelRAM.setBorder(new LineBorder(Color.DARK_GRAY, 2, true));
		panelRAM.setBackground(new Color(169, 169, 169));
		mainContainerPanel.add(panelRAM, "cell 0 2 4 1,grow");
		panelRAM.setLayout(new MigLayout("", "[100][160,grow][80][40,grow][100][40,grow]", "[15px][grow]"));

		lblRam = new JLabel("RAM:");
		panelRAM.add(lblRam, "cell 0 0 1 2,alignx center");
		lblRam.setFont(new Font("Segoe UI", Font.BOLD, 20));

		dynLblRam = new JTextField(UNSELECTED);
		panelRAM.add(dynLblRam, "cell 1 0 1 2,growx");
		dynLblRam.setForeground(new Color(72, 61, 139));
		dynLblRam.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblRam.setEditable(false);

		lblFreeRam = new JLabel("Free:");
		panelRAM.add(lblFreeRam, "cell 2 0 1 2,alignx center");
		lblFreeRam.setFont(new Font("Segoe UI", Font.PLAIN, 16));

		dynLblFreeRam = new JTextField(UNSELECTED);
		panelRAM.add(dynLblFreeRam, "cell 3 0 1 2,growx");
		dynLblFreeRam.setForeground(new Color(72, 61, 139));
		dynLblFreeRam.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblFreeRam.setEditable(false);

		lblUsedRam = new JLabel("Used:");
		panelRAM.add(lblUsedRam, "cell 4 0 1 2,alignx center");
		lblUsedRam.setFont(new Font("Segoe UI", Font.PLAIN, 16));

		dynLblUsedRam = new JTextField(UNSELECTED);
		panelRAM.add(dynLblUsedRam, "cell 5 0 1 2,growx");
		dynLblUsedRam.setForeground(new Color(72, 61, 139));
		dynLblUsedRam.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblUsedRam.setEditable(false);

		panelCPU = new JPanel();
		panelCPU.setBorder(new LineBorder(new Color(64, 64, 64), 2, true));
		panelCPU.setBackground(new Color(169, 169, 169));
		mainContainerPanel.add(panelCPU, "cell 0 3 4 3,grow");
		panelCPU.setLayout(new MigLayout("", "[100][160px,grow][150][60][150][150]", "[grow][grow][grow]"));

		lblCPU = new JLabel("CPU:");
		lblCPU.setFont(new Font("Segoe UI", Font.BOLD, 20));
		panelCPU.add(lblCPU, "cell 0 0,alignx center");

		dynLblCPU = new JTextField(UNSELECTED);
		dynLblCPU.setForeground(new Color(72, 61, 139));
		dynLblCPU.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCPU.setEditable(false);
		panelCPU.add(dynLblCPU, "cell 1 0,growx");

		lblNofSockets = new JLabel("Number of Sockets:");
		lblNofSockets.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelCPU.add(lblNofSockets, "cell 2 0,alignx center");

		dynLblCPUNofSockets = new JTextField(UNSELECTED);
		dynLblCPUNofSockets.setForeground(new Color(72, 61, 139));
		dynLblCPUNofSockets.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCPUNofSockets.setEditable(false);
		panelCPU.add(dynLblCPUNofSockets, "cell 3 0,growx");

		lblCpuFrequency = new JLabel("Cpu Frequency:");
		lblCpuFrequency.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelCPU.add(lblCpuFrequency, "cell 4 0,alignx center");

		dynLblCpuFrequency = new JTextField(UNSELECTED);
		dynLblCpuFrequency.setForeground(new Color(72, 61, 139));
		dynLblCpuFrequency.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCpuFrequency.setEditable(false);
		panelCPU.add(dynLblCpuFrequency, "cell 5 0,growx");

		lblVendor = new JLabel("Vendor:");
		panelCPU.add(lblVendor, "cell 0 1,alignx center");
		lblVendor.setFont(new Font("Segoe UI", Font.PLAIN, 16));

		dynLblCPUVendor = new JTextField(UNSELECTED);
		dynLblCPUVendor.setForeground(new Color(72, 61, 139));
		dynLblCPUVendor.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCPUVendor.setEditable(false);
		panelCPU.add(dynLblCPUVendor, "cell 1 1,growx");

		lblCpuCores = new JLabel("Number of Cores:");
		lblCpuCores.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelCPU.add(lblCpuCores, "cell 2 1,alignx center");

		dynLblCPUNofCores = new JTextField(UNSELECTED);
		dynLblCPUNofCores.setForeground(new Color(72, 61, 139));
		dynLblCPUNofCores.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCPUNofCores.setEditable(false);
		panelCPU.add(dynLblCPUNofCores, "cell 3 1,growx");

		lblLogicalCores = new JLabel("Logical cores:");
		lblLogicalCores.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelCPU.add(lblLogicalCores, "cell 4 1,alignx center");

		dynLblLogicalCores = new JTextField(UNSELECTED);
		dynLblLogicalCores.setForeground(new Color(72, 61, 139));
		dynLblLogicalCores.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblLogicalCores.setEditable(false);
		panelCPU.add(dynLblLogicalCores, "cell 5 1,growx");

		panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		panel.setBackground(Color.DARK_GRAY);
		panelCPU.add(panel, "cell 0 2 6 1,grow");
		panel.setLayout(new MigLayout("", "[60][grow][60][grow][50][grow][60][grow]", "[grow]"));

		lblUsage = new JLabel("Usage:");
		lblUsage.setForeground(Color.WHITE);
		lblUsage.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panel.add(lblUsage, "cell 0 0,alignx center");

		dynLblCpuUsage = new JTextField(UNSELECTED);
		dynLblCpuUsage.setForeground(new Color(72, 61, 139));
		dynLblCpuUsage.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCpuUsage.setEditable(false);
		panel.add(dynLblCpuUsage, "cell 1 0,growx");

		lblAvg = new JLabel("AVG:");
		lblAvg.setForeground(Color.WHITE);
		lblAvg.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panel.add(lblAvg, "cell 2 0,alignx center");

		dynLblCpuAvgUsage = new JTextField(UNSELECTED);
		dynLblCpuAvgUsage.setForeground(new Color(72, 61, 139));
		dynLblCpuAvgUsage.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCpuAvgUsage.setEditable(false);
		panel.add(dynLblCpuAvgUsage, "cell 3 0,growx");

		lblTemperature = new JLabel("Temperature:");
		lblTemperature.setForeground(Color.WHITE);
		lblTemperature.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panel.add(lblTemperature, "cell 4 0,alignx center");

		dynLblCpuTemp = new JTextField(UNSELECTED);
		dynLblCpuTemp.setForeground(new Color(72, 61, 139));
		dynLblCpuTemp.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCpuTemp.setEditable(false);
		panel.add(dynLblCpuTemp, "cell 5 0,growx");

		lblVoltage = new JLabel("Voltage:");
		lblVoltage.setForeground(Color.WHITE);
		lblVoltage.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panel.add(lblVoltage, "cell 6 0,alignx center");

		dynLblCpuVoltage = new JTextField(UNSELECTED);
		dynLblCpuVoltage.setForeground(new Color(72, 61, 139));
		dynLblCpuVoltage.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblCpuVoltage.setEditable(false);
		panel.add(dynLblCpuVoltage, "cell 7 0,growx");

		panelMOBO = new JPanel();
		panelMOBO.setBorder(new LineBorder(new Color(64, 64, 64), 2, true));
		panelMOBO.setBackground(new Color(169, 169, 169));
		mainContainerPanel.add(panelMOBO, "cell 0 6 4 1,grow");
		panelMOBO.setLayout(new MigLayout("", "[100][160][70][grow][70][70][110][grow]", "[grow]"));

		lblMotherboard = new JLabel("MOBO:");
		lblMotherboard.setFont(new Font("Segoe UI", Font.BOLD, 20));
		panelMOBO.add(lblMotherboard, "cell 0 0,alignx center");

		dynLblMoboModel = new JTextField(UNSELECTED);
		dynLblMoboModel.setForeground(new Color(72, 61, 139));
		dynLblMoboModel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblMoboModel.setEditable(false);
		panelMOBO.add(dynLblMoboModel, "cell 1 0,growx");

		lblMoboVendor = new JLabel("Vendor:");
		lblMoboVendor.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelMOBO.add(lblMoboVendor, "cell 2 0,alignx center");

		dynLblMoboVendor = new JTextField(UNSELECTED);
		dynLblMoboVendor.setForeground(new Color(72, 61, 139));
		dynLblMoboVendor.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblMoboVendor.setEditable(false);
		panelMOBO.add(dynLblMoboVendor, "cell 3 0,growx");

		lblVersion = new JLabel("Version:");
		lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelMOBO.add(lblVersion, "cell 4 0,alignx center");

		dynLblMoboVersion = new JTextField(UNSELECTED);
		dynLblMoboVersion.setForeground(new Color(72, 61, 139));
		dynLblMoboVersion.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblMoboVersion.setEditable(false);
		panelMOBO.add(dynLblMoboVersion, "cell 5 0,growx");

		lblSerialNumber = new JLabel("Serial Number:");
		lblSerialNumber.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelMOBO.add(lblSerialNumber, "cell 6 0,alignx center");

		dynLblMoboSerialNumber = new JTextField(UNSELECTED);
		dynLblMoboSerialNumber.setForeground(new Color(72, 61, 139));
		dynLblMoboSerialNumber.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblMoboSerialNumber.setEditable(false);
		panelMOBO.add(dynLblMoboSerialNumber, "cell 7 0,growx");

		panelHD = new JPanel();
		panelHD.setBorder(new LineBorder(new Color(64, 64, 64), 2, true));
		panelHD.setBackground(new Color(169, 169, 169));
		mainContainerPanel.add(panelHD, "cell 0 7 4 2,grow");
		panelHD.setLayout(new MigLayout("", "[100][220][60][100px][100px][90px][110px][100px,grow]", "[grow][grow]"));

		lblHd = new JLabel("HD:");
		lblHd.setFont(new Font("Segoe UI", Font.BOLD, 20));
		panelHD.add(lblHd, "cell 0 0,alignx center");

//		comboSelectedHd = new JComboBox(); // only for editing in windowbuilder
		comboSelectedHd = new JComboBox<HDinfo>(HDList);
		panelHD.add(comboSelectedHd, "cell 1 0,growx");

		lblWrites = new JLabel("Writes:");
		lblWrites.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelHD.add(lblWrites, "cell 2 0,alignx center");

		dynLblHDWriteBytes = new JTextField(UNSELECTED);
		dynLblHDWriteBytes.setForeground(new Color(72, 61, 139));
		dynLblHDWriteBytes.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblHDWriteBytes.setEditable(false);
		panelHD.add(dynLblHDWriteBytes, "cell 3 0,growx");

		lblBytesWritten = new JLabel("Bytes written:");
		lblBytesWritten.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelHD.add(lblBytesWritten, "cell 4 0,alignx center");

		dynLblHdBytesWritten = new JTextField(UNSELECTED);
		dynLblHdBytesWritten.setForeground(new Color(72, 61, 139));
		dynLblHdBytesWritten.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblHdBytesWritten.setEditable(false);
		panelHD.add(dynLblHdBytesWritten, "cell 5 0,growx");

		lblHDCapacity = new JLabel("Capacity:");
		lblHDCapacity.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelHD.add(lblHDCapacity, "cell 6 0,alignx center");

		dynLblHDCapacity = new JTextField(UNSELECTED);
		dynLblHDCapacity.setForeground(new Color(72, 61, 139));
		dynLblHDCapacity.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblHDCapacity.setEditable(false);
		panelHD.add(dynLblHDCapacity, "cell 7 0,growx");

		lblHDName = new JLabel("Model:");
		lblHDName.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelHD.add(lblHDName, "cell 0 1,alignx center");

		dynLblHDmodel = new JTextField(UNSELECTED);
		dynLblHDmodel.setForeground(new Color(72, 61, 139));
		dynLblHDmodel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblHDmodel.setEditable(false);
		panelHD.add(dynLblHDmodel, "cell 1 1,growx");

		lblReads = new JLabel("Reads:");
		lblReads.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelHD.add(lblReads, "cell 2 1,alignx center");

		dynLblHDReadBytes = new JTextField(UNSELECTED);
		dynLblHDReadBytes.setForeground(new Color(72, 61, 139));
		dynLblHDReadBytes.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblHDReadBytes.setEditable(false);
		panelHD.add(dynLblHDReadBytes, "cell 3 1,growx");

		lblBytesRead = new JLabel("Bytes read:");
		lblBytesRead.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelHD.add(lblBytesRead, "cell 4 1,alignx center");

		dynLblHdBytesRead = new JTextField(UNSELECTED);
		dynLblHdBytesRead.setForeground(new Color(72, 61, 139));
		dynLblHdBytesRead.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblHdBytesRead.setEditable(false);
		panelHD.add(dynLblHdBytesRead, "cell 5 1,growx");

		lblHDSerial = new JLabel("Serial Number:");
		lblHDSerial.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelHD.add(lblHDSerial, "cell 6 1,alignx center");

		dynLblHDSerial = new JTextField(UNSELECTED);
		dynLblHDSerial.setForeground(new Color(72, 61, 139));
		dynLblHDSerial.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblHDSerial.setEditable(false);
		panelHD.add(dynLblHDSerial, "cell 7 1,growx");

		panelMonitors = new JPanel();
		panelMonitors.setBorder(new LineBorder(new Color(64, 64, 64), 2, true));
		panelMonitors.setBackground(new Color(169, 169, 169));
		mainContainerPanel.add(panelMonitors, "cell 0 9 4 2,grow");
		panelMonitors.setLayout(new MigLayout("", "[100][160][110][grow]", "[][grow]"));

		lblDisplay = new JLabel("DISPLAY:");
		lblDisplay.setFont(new Font("Segoe UI", Font.BOLD, 20));
		panelMonitors.add(lblDisplay, "cell 0 0,alignx center");

//		comboSelectedDisplay = new JComboBox(); // only for editing in windowbuilder
		comboSelectedDisplay = new JComboBox<Integer>(displayIndexer);
		panelMonitors.add(comboSelectedDisplay, "cell 1 0,growx");

		lblName = new JLabel("Name:");
		lblName.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		panelMonitors.add(lblName, "cell 2 0,alignx center");

		dynLblDisplayInfo1 = new JTextField(UNSELECTED);
		dynLblDisplayInfo1.setForeground(new Color(72, 61, 139));
		dynLblDisplayInfo1.setFont(new Font("Segoe UI", Font.BOLD, 14));
		dynLblDisplayInfo1.setEditable(false);
		panelMonitors.add(dynLblDisplayInfo1, "cell 3 0,growx");

		scrollPane = new JScrollPane();
		panelMonitors.add(scrollPane, "cell 0 1 4 1,grow");

		textAreaDisplay = new JTextArea();
		textAreaDisplay.setBackground(new Color(255, 250, 205));
		scrollPane.setViewportView(textAreaDisplay);

	}

////////////////////////// EVENT HANDLERS ACTION LISTENERS //////////////////////////

	private void addEventHandlers() {

		// default close operation(same as reset)
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {

	        	logger.debug("Closing Frame: " + HwInfoFrame.class.getName());
	        	singletonInstance = null;
	        	isActive.set(false);
	        	dispose();
		    }
		});

		comboSelectedDisplay.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateDisplayInfo();
			}
		});

		comboSelectedHd.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateHdInfo();
			}
		});
	}

////////////////////////// SYSTEM VARIABLES INITIALIZER //////////////////////////

	private void initSystemVars() {
		cpuUsageHystory = new LimitedConcurrentList<Double>(1000);
		sysINFO = new SystemInfo();

		MOBO = sysINFO.getHardware().getComputerSystem().getBaseboard();
		displays = sysINFO.getHardware().getDisplays();
		displayIndexer = initDisplayIndexer();
		cpu = sysINFO.getHardware().getProcessor();
		HWDiskStore[] info = sysINFO.getHardware().getDiskStores();
		HDList = new HDinfo[info.length];
		int i = 0;
		for(HWDiskStore disk : info) {
			HDList[i] = new HDinfo(disk);
			i++;
		}
	}


//////////////////////////AND NOW SOME USEFUL METHOD //////////////////////////

	private void updateScreeptResourcesUsage(){
		String percentRam = RuntimeUtil.getPercMemoryOnTotalSystem(memoryRam.getTotal());
		String usedRam = RuntimeUtil.getUsedMemoryString();
		try {
			dynLblScreeptCpuPerc.setText(RuntimeUtil.getCurrentProcCpuUsageString());
		} catch (IOException e) {
			dynLblScreeptCpuPerc.setText(errMSG);
		}
		dynLblScreeptRamUsage.setText(usedRam + "[" + percentRam + "]");
	}

	private void updateGenInfo() {
		dynLblArch.setText(System.getProperty("os.arch"));
		dynLblOsName.setText(System.getProperty("os.name"));
		dynLblOsVersion.setText(System.getProperty("os.version"));
		dynLblJavaVersion.setText(System.getProperty("java.version"));
	}

	private void updateCpuInfo() {
		dynLblCPU.setText(cpu.getName());
		dynLblCpuFrequency.setText(ConversionUtils.coolFrequency(cpu.getVendorFreq()));
		dynLblCPUNofCores.setText(String.valueOf(cpu.getPhysicalProcessorCount()));
		dynLblCPUNofSockets.setText(String.valueOf(cpu.getPhysicalPackageCount()));
		dynLblCPUVendor.setText(cpu.getVendor());
		dynLblLogicalCores.setText(String.valueOf(cpu.getLogicalProcessorCount()));
	}

	private void updateMoboInfo() {
		String vendor = (!MOBO.getManufacturer().equals(""))? MOBO.getManufacturer(): UNSELECTED;
		String model = (!MOBO.getModel().equals(""))? MOBO.getModel(): UNSELECTED;
		String version = (!MOBO.getVersion().equals(""))? MOBO.getVersion(): UNSELECTED;
		dynLblMoboVersion.setText(version);
		dynLblMoboVendor.setText(vendor);
		dynLblMoboModel.setText(model);
		dynLblMoboSerialNumber.setText(MOBO.getSerialNumber());
	}

	private void updateRamInfo() {
		try {
			memoryRam = sysINFO.getHardware().getMemory();

			long memoryTot = memoryRam.getTotal();
			long memoryFree = memoryRam.getAvailable();
			long memoryUsed = memoryTot - memoryFree;

			String usedPerc = ConversionUtils.getPercentString(memoryUsed, memoryTot);
			String freePerc = ConversionUtils.getPercentString(memoryFree, memoryTot);

			dynLblRam.setText(memoryTot/(1024*1024)+" MB");
			dynLblFreeRam.setText(ConversionUtils.coolFileSize(memoryFree) + " ["+freePerc+"]");
			dynLblUsedRam.setText(ConversionUtils.coolFileSize(memoryUsed) + " ["+usedPerc+"]");
		} catch (Throwable e) {
			dynLblRam.setText(errMSG);
			dynLblFreeRam.setText(errMSG);
			dynLblFreeRam.setText(errMSG);
		}
	}

	private void updateCpuUsageInfo() {
		double currentUsage = cpu.getSystemCpuLoadBetweenTicks();
		cpuUsageHystory.add(currentUsage);
		double avgUsageCurrent = ConversionUtils.getAvgValue(cpuUsageHystory.getList());
		double cpuVolts = sysINFO.getHardware().getSensors().getCpuVoltage();
		double cpuTemp = sysINFO.getHardware().getSensors().getCpuTemperature();
		String usagePerc = new DecimalFormat("#,##0.##").format(currentUsage*100)+ "%";
		String AVGPerc = new DecimalFormat("#,##0.##").format(avgUsageCurrent*100)+ "%";
		String cpuVoltage = (cpuVolts != 0)? (new DecimalFormat("#,##0.####").format(cpuVolts)+ " Volts") : UNSELECTED;
		String cputemperature = (cpuTemp != 0)? (new DecimalFormat("#,##0.####").format(cpuTemp)+ " °C") : UNSELECTED;
		dynLblCpuUsage.setText(usagePerc);
		dynLblCpuAvgUsage.setText(AVGPerc);
		dynLblCpuVoltage.setText(cpuVoltage);
		dynLblCpuTemp.setText(cputemperature);
	}

	private void updateHdInfo() {
		try {
			HDinfo selected = (HDinfo) comboSelectedHd.getSelectedItem();
			HWDiskStore disk = selected.getDisk();
			disk.updateDiskStats();
			dynLblHDReadBytes.setText(String.valueOf(disk.getReadBytes()));
			dynLblHDWriteBytes.setText(String.valueOf(disk.getWrites()));
			dynLblHDmodel.setText(disk.getModel());
			dynLblHDCapacity.setText(ConversionUtils.coolFileSize(disk.getSize()));
			dynLblHDSerial.setText(disk.getSerial());
			dynLblHdBytesRead.setText(ConversionUtils.coolFileSize(disk.getReadBytes()));
			dynLblHdBytesWritten.setText(ConversionUtils.coolFileSize(disk.getWriteBytes()));
		} catch (IndexOutOfBoundsException e) {
			logger.error(e);
		}
	}

	private void updateDisplayInfo() {
		int iDisp = comboSelectedDisplay.getSelectedIndex();
		Display selDisplay = displays[iDisp];
		String displayInfoString = selDisplay.toString();
		textAreaDisplay.setText(displayInfoString);
		String[] splittedLinesString = displayInfoString.split("\n");
		String name = "Monitor Name:";

		for (String splitted : splittedLinesString) {
			if(splitted.indexOf(name)!=-1) {
				String dynName = splitted.substring(splitted.indexOf(name)+name.length());
				dynLblDisplayInfo1.setText(dynName);
				break;
			}else {
				dynLblDisplayInfo1.setText(UNSELECTED);
			}
		}
	}

	private Integer[] initDisplayIndexer() {
		Integer[] indexer = new Integer[displays.length];
		for(int i=0; i<displays.length; i++) {
			indexer[i] = i;
		}
		return indexer;
	}

	/**
	 * Periodically checks if external application is closed or not, exit while cycle when isActive==false (closed) or external is closed
	 */
	public void startExternalSignalCloseFlagCheck() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(isActive.get()){

					if(!GeneralConfig.SCREEPT_ACTIVE_SIGNAL_FILE.exists()) {
						System.exit(0);
					}

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}

				if(!GeneralConfig.SCREEPT_ACTIVE_SIGNAL_FILE.exists()) {
					System.exit(0);
				}
			}
		}).start();
	}

	@Override
	public Dimension getDefaultDimension() {
		return new Dimension(1000, 750);
	}

	public static HwInfoFrame getInstance() {
		return singletonInstance == null ? new HwInfoFrame() : singletonInstance;
	}
}
