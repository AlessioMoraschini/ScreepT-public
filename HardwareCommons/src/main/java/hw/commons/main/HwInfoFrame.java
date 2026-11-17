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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.awt.event.MouseEvent;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

import org.apache.log4j.Logger;

import gui.commons.frameutils.frame.arch.ParentFrame;
import gui.commons.frameutils.frame.arch.ToolIcons;
import gui.commons.frameutils.frame.arch.ToolStyle;
import gui.commons.frameutils.utils.GuiUtilsExtended;
import gui.commons.imagestudio.DialogFit;
import hw.commons.om.HDinfo;
import net.miginfocom.swing.MigLayout;
import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.gui.ScreenMemory;
import various.common.light.i18n.I18n;
import various.common.light.runtime.RuntimeUtil;
import various.common.light.utility.manipulation.ConversionUtils;

/**
 * Live view of the computer (CPU, memory, storage, display, battery, network) and of the resources used by ScreepT itself.
 * All the oshi calls run in one background sampler thread, the Swing components are only touched in the event thread.
 * Colors and fonts come from the look and feel (see {@link ToolStyle}), so the window follows the theme and the interface scale.
 */
public class HwInfoFrame extends ParentFrame{
	private static final long serialVersionUID = -2119678215026217031L;

	public static HwInfoFrame singletonInstance = null;

	static Logger logger = Logger.getLogger(HwInfoFrame.class);

	/** Live refresh period */
	private static final int REFRESH_MS = 1000;
	/** Slow or rarely changing values (sensors, volumes, battery, process counters) are read every N samples */
	private static final int SLOW_EVERY = 5;
	/** Samples drawn in the history graphs */
	private static final int HISTORY = 90;
	/** oshi prints the monitor model with one of these prefixes (English, it is not translated by oshi) */
	private static final String[] EDID_NAME_KEYS = {"Monitor Name:", "Unspecified Text:"};

	// system objects (oshi), used by the sampler thread only after the constructor ends
	private SystemInfo sysINFO;
	private HardwareAbstractionLayer hardware;
	private OperatingSystem os;
	private CentralProcessor cpu;
	private Baseboard mobo;
	private Display[] displays = new Display[0];
	private HDinfo[] diskList = new HDinfo[0];
	private List<NetworkIF> networkIfs = new ArrayList<>();

	// sampler state
	private Thread samplerThread;
	private volatile boolean paused;
	private volatile int selectedDisk;
	private volatile boolean diskChanged = true;
	private long prevDiskTs;
	private long prevDiskRead;
	private long prevDiskWrite;
	private long prevNetTs;
	private long prevNetRecv;
	private long prevNetSent;

	// GUI: header
	private JToggleButton btnPause;
	private JToggleButton btnOnTop;
	private JLabel lblUpdated;

	// GUI: meters
	private MeterCard cpuCard;
	private MeterCard ramCard;
	private MeterCard screeptCard;
	private JLabel cpuAvg;
	private JLabel cpuPeak;
	private JLabel ramUsed;
	private JLabel ramFree;
	private JLabel ramTotal;
	private JLabel ramSwap;
	private JLabel scRam;
	private JLabel scHeap;
	private JLabel scThreads;
	private double cpuSum;
	private long cpuCount;
	private double cpuMax;

	// GUI: details
	private JLabel sysUptime;
	private JLabel sysProcesses;
	private JLabel sysThreads;
	private JLabel cpuTemp;
	private JLabel cpuVolt;
	private JLabel cpuFans;
	private JLabel cpuSensorNote;
	private CoreBars coreBars;
	private JLabel powerBattery;
	private JLabel powerTime;
	private JLabel netDown;
	private JLabel netUp;
	private JLabel netIfs;
	private JComboBox<HDinfo> comboDisk;
	private JLabel dkSerial;
	private JLabel dkQueue;
	private JLabel dkReadTotal;
	private JLabel dkWriteTotal;
	private JLabel dkReadSpeed;
	private JLabel dkWriteSpeed;
	private JPanel volumesPanel;
	private final List<VolumeRow> volumeRows = new ArrayList<>();
	private JComboBox<String> comboDisplay;
	private JLabel dpName;
	private JLabel dpManufacturer;
	private JLabel dpSize;
	private JLabel dpResolution;
	private JLabel dpDate;
	private JLabel dpJava;
	private JTextArea dpRaw;
	private JScrollPane dpRawScroll;

	// GUI: GPU
	private JPanel gpuAdapters;
	private JToggleButton gpuLive;
	private final List<Component> gpuLiveComponents = new ArrayList<>();
	private JLabel gpuLoad;
	private JLabel gpuVram;
	private JLabel gpuTemp;
	private JLabel gpuPower;
	private JLabel gpuClock;
	private volatile Process smiProcess;
	private volatile int smiGeneration;
	private String gpuTitle;

	/** Rows of the "copy report" text: section, label and the label showing the value */
	private final List<Row> reportRows = new ArrayList<>();

	private long sampleCount;

////////////////////////// CONSTRUCTOR //////////////////////////

	public HwInfoFrame() {
		super();

		isActive.set(true);

		logger.info(this.getClass().getName() + " Starting...");

		setTitle(I18n.t("hw.hardware_info_frame"));

		// SW window icon
		GuiUtilsExtended.setFrameIcon(IconsPathConfigurator.ICON_HW_INFO, this);

		singletonInstance = this;

		initSystemVars();
		initialize();
		loadStaticInfo();
		// first reading before the window is sized, so the layout already knows the real length of the values
		try {
			apply(collect(0));
		} catch (Throwable e) {
			logger.warn("First hardware sampling failed", e);
		}

		startGpuQuery();

		samplerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				sampleLoop();
			}
		}, "HwInfo-Sampler");
		samplerThread.setDaemon(true);
		samplerThread.start();
	}

////////////////////////// SYSTEM OBJECTS //////////////////////////

	private void initSystemVars() {
		sysINFO = new SystemInfo();
		hardware = sysINFO.getHardware();
		os = sysINFO.getOperatingSystem();
		cpu = hardware.getProcessor();
		try {
			mobo = hardware.getComputerSystem().getBaseboard();
		} catch (Throwable e) {
			logger.warn("Cannot read the motherboard", e);
		}
		try {
			displays = hardware.getDisplays();
		} catch (Throwable e) {
			logger.warn("Cannot read the displays", e);
		}
		try {
			HWDiskStore[] stores = hardware.getDiskStores();
			diskList = new HDinfo[stores.length];
			for (int i = 0; i < stores.length; i++) {
				diskList[i] = new HDinfo(stores[i]);
			}
		} catch (Throwable e) {
			logger.warn("Cannot read the disks", e);
		}
		try {
			for (NetworkIF net : hardware.getNetworkIFs()) {
				networkIfs.add(net);
			}
		} catch (Throwable e) {
			logger.warn("Cannot read the network interfaces", e);
		}
	}

////////////////////////// GUI //////////////////////////

	private void initialize() {
		JPanel root = new JPanel(new MigLayout("insets 14, fillx, wrap 3, gapx 12, gapy 12",
				"[grow,fill,sizegroup col][grow,fill,sizegroup col][grow,fill,sizegroup col]"));
		setContentPane(root);

		root.add(buildHeader(), "span 3, growx");

		// live meters
		cpuCard = new MeterCard(I18n.t("hw.cpu"), ToolStyle.BLUE);
		cpuAvg = cpuCard.detail(I18n.t("hw.average"));
		cpuPeak = cpuCard.detail(I18n.t("hw.peak"));
		root.add(cpuCard.panel, "grow");

		ramCard = new MeterCard(I18n.t("hw.memory"), ToolStyle.GREEN);
		ramUsed = ramCard.detail(I18n.t("hw.used"));
		ramFree = ramCard.detail(I18n.t("hw.free"));
		ramTotal = ramCard.detail(I18n.t("hw.total"));
		ramSwap = ramCard.detail(I18n.t("hw.swap"));
		root.add(ramCard.panel, "grow");

		screeptCard = new MeterCard("ScreepT", ToolStyle.ORANGE);
		scRam = screeptCard.detail(I18n.t("hw.process_memory"));
		scHeap = screeptCard.detail(I18n.t("hw.java_heap"));
		scThreads = screeptCard.detail(I18n.t("hw.threads"));
		root.add(screeptCard.panel, "grow");

		// system and processor
		KvCard system = new KvCard(I18n.t("hw.system"), 1);
		JLabel osName = system.row(I18n.t("hw.os_name"));
		JLabel osVersion = system.row(I18n.t("hw.os_version"));
		JLabel arch = system.row(I18n.t("hw.architecture"));
		JLabel javaVersion = system.row(I18n.t("hw.java_version"));
		sysUptime = system.row(I18n.t("hw.uptime"));
		sysProcesses = system.row(I18n.t("hw.processes"));
		sysThreads = system.row(I18n.t("hw.threads"));
		String osNameText = System.getProperty("os.name");
		String osVersionText = System.getProperty("os.version");
		try {
			String build = os.getVersion().getBuildNumber();
			if (build != null && !build.trim().isEmpty()) {
				osVersionText += " (build " + build.trim() + ")";
				// older Java versions report Windows 11 as Windows 10
				if ("Windows 10".equals(osNameText) && Integer.parseInt(build.trim()) >= 22000) {
					osNameText = "Windows 11";
				}
			}
		} catch (Throwable e) {
			logger.debug("os build: " + e);
		}
		osName.setText(osNameText);
		osVersion.setText(osVersionText);
		arch.setText(System.getProperty("os.arch"));
		javaVersion.setText(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
		javaVersion.setToolTipText(System.getProperty("java.vm.name") + " - " + System.getProperty("java.home"));
		root.add(system.panel, "grow");

		KvCard processor = new KvCard(I18n.t("hw.processor"), 2);
		JLabel cpuName = processor.row(I18n.t("hw.name"));
		JLabel cpuVendor = processor.row(I18n.t("hw.vendor"));
		JLabel cpuFreq = processor.row(plain(I18n.t("hw.cpu_frequency")));
		JLabel cpuSockets = processor.row(plain(I18n.t("hw.number_of_sockets")));
		JLabel cpuCores = processor.row(plain(I18n.t("hw.number_of_cores")));
		JLabel cpuLogical = processor.row(plain(I18n.t("hw.logical_cores")));
		cpuTemp = processor.row(I18n.t("hw.temperature"));
		cpuVolt = processor.row(I18n.t("hw.voltage"));
		cpuFans = processor.row(I18n.t("hw.fans"));
		processor.row(null);
		processor.panel.add(ToolStyle.mutedLabel(I18n.t("hw.cores_load")), "span, gaptop 8, wrap");
		coreBars = new CoreBars();
		processor.panel.add(coreBars, "span, growx, h 32!, wrap");
		cpuSensorNote = ToolStyle.mutedLabel("<html><body style='width:560px'>" + I18n.t("hw.sensor_note") + "</body></html>");
		cpuSensorNote.setVisible(false);
		processor.panel.add(cpuSensorNote, "span, gaptop 4");
		root.add(processor.panel, "span 2, grow");

		cpuName.setText(read(() -> cpu.getName()));
		cpuVendor.setText(read(() -> cpu.getVendor()));
		cpuFreq.setText(read(() -> cpu.getVendorFreq() > 0 ? ConversionUtils.coolFrequency(cpu.getVendorFreq()) : null));
		cpuSockets.setText(read(() -> cpu.getPhysicalPackageCount()));
		cpuCores.setText(read(() -> cpu.getPhysicalProcessorCount()));
		cpuLogical.setText(read(() -> cpu.getLogicalProcessorCount()));

		// board, power, network
		KvCard board = new KvCard(I18n.t("hw.motherboard"), 1);
		JLabel mbVendor = board.row(I18n.t("hw.vendor"));
		JLabel mbModel = board.row(I18n.t("hw.model"));
		JLabel mbVersion = board.row(I18n.t("hw.version"));
		JLabel mbSerial = board.row(plain(I18n.t("hw.serial_number")));
		if (mobo != null) {
			mbVendor.setText(read(() -> mobo.getManufacturer()));
			mbModel.setText(read(() -> mobo.getModel()));
			mbVersion.setText(read(() -> mobo.getVersion()));
			mbSerial.setText(read(() -> mobo.getSerialNumber()));
		}

		KvCard power = new KvCard(I18n.t("hw.power") + " / " + I18n.t("hw.network"), 1);
		powerBattery = power.row(I18n.t("hw.battery"));
		powerTime = power.row(I18n.t("hw.time_remaining"));
		netDown = power.row(I18n.t("hw.download"));
		netUp = power.row(I18n.t("hw.upload"));
		netIfs = power.row(I18n.t("hw.interfaces"));

		// storage
		KvCard storage = new KvCard(I18n.t("hw.storage"), 2);
		comboDisk = new JComboBox<HDinfo>(diskList);
		storage.panel.add(comboDisk, "span, growx, gapbottom 4, wrap");
		dkSerial = storage.row(plain(I18n.t("hw.serial_number")));
		dkQueue = storage.row(I18n.t("hw.queue_length"));
		dkReadTotal = storage.row(plain(I18n.t("hw.bytes_read")));
		dkWriteTotal = storage.row(plain(I18n.t("hw.bytes_written")));
		dkReadSpeed = storage.row(I18n.t("hw.read_speed"));
		dkWriteSpeed = storage.row(I18n.t("hw.write_speed"));
		storage.panel.add(ToolStyle.mutedLabel(I18n.t("hw.volumes")), "span, gaptop 8, wrap");
		volumesPanel = new JPanel(new MigLayout("insets 0, fillx, wrap 3, gapx 12, gapy 5", "[200::260,fill][grow,fill][right]"));
		volumesPanel.setOpaque(false);
		storage.panel.add(volumesPanel, "span, growx");
		root.add(storage.panel, "span 2, grow");
		JPanel rightColumn = new JPanel(new MigLayout("insets 0, fill, wrap 1, gapy 12", "[grow,fill]", "[grow,fill][grow,fill]"));
		rightColumn.setOpaque(false);
		rightColumn.add(board.panel, "grow");
		rightColumn.add(power.panel, "grow");
		root.add(rightColumn, "grow");

		// display
		KvCard display = new KvCard(I18n.t("hw.display"), 2);
		comboDisplay = new JComboBox<String>();
		for (int i = 0; i < displays.length; i++) {
			comboDisplay.addItem(I18n.t("hw.display") + " " + (i + 1));
		}
		final JToggleButton details = new JToggleButton(I18n.t("hw.details"));
		details.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		display.panel.add(comboDisplay, "span 3, growx, gapbottom 4");
		display.panel.add(details, "gapbottom 4, alignx right, growx 0, wrap");
		dpName = display.row(I18n.t("hw.model"));
		dpManufacturer = display.row(I18n.t("hw.manufacturer"));
		dpSize = display.row(I18n.t("hw.physical_size"));
		dpResolution = display.row(I18n.t("hw.resolution"));
		dpDate = display.row(I18n.t("hw.manufacture_date"));
		dpJava = display.wideRow(I18n.t("hw.java_screens"));
		dpRaw = new JTextArea(6, 40);
		dpRaw.setEditable(false);
		dpRaw.setFont(ToolStyle.mono(0));
		dpRawScroll = new JScrollPane(dpRaw);
		dpRawScroll.setVisible(false);
		display.panel.add(dpRawScroll, "span, growx");
		details.addActionListener(e -> {
			dpRawScroll.setVisible(details.isSelected());
			revalidate();
			repaint();
		});
		root.add(display.panel, "span 2, grow");

		gpuTitle = I18n.t("hw.gpu");
		KvCard gpu = new KvCard(gpuTitle, 1);
		gpuAdapters = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gapy 6", "[grow,fill]"));
		gpuAdapters.setOpaque(false);
		gpuAdapters.add(ToolStyle.mutedLabel(I18n.t("hw.gpu_loading")));
		gpu.panel.add(gpuAdapters, "span, growx, wrap");
		gpuLive = new JToggleButton(I18n.t("hw.gpu_live"));
		gpuLive.setToolTipText(I18n.t("hw.gpu_live_tip"));
		gpuLive.setEnabled(false);
		gpuLive.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		gpu.panel.add(gpuLive, "span, gaptop 6, wrap");
		gpuLoad = liveRow(gpu, I18n.t("hw.gpu_load"));
		gpuVram = liveRow(gpu, I18n.t("hw.gpu_vram_used"));
		gpuTemp = liveRow(gpu, I18n.t("hw.temperature"));
		gpuPower = liveRow(gpu, I18n.t("hw.gpu_power"));
		gpuClock = liveRow(gpu, I18n.t("hw.gpu_clock"));
		gpuLive.addActionListener(e -> setGpuLive(gpuLive.isSelected()));
		root.add(gpu.panel, "grow");

		comboDisk.addActionListener(e -> {
			selectedDisk = Math.max(0, comboDisk.getSelectedIndex());
			diskChanged = true;
			updateDiskStatic();
		});
		comboDisplay.addActionListener(e -> updateDisplayInfo());

		ToolStyle.bindKey(this, javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "closeHw", new Runnable() {
			@Override
			public void run() {
				dispose();
			}
		});
	}

	private JPanel buildHeader() {
		JPanel header = new JPanel(new MigLayout("insets 0, fillx", "[grow,fill][]"));
		header.setOpaque(false);
		header.add(ToolStyle.header(I18n.t("hw.title"), I18n.t("hw.subtitle")), "growx");

		JPanel buttons = new JPanel(new MigLayout("insets 0, gapx 8", "[][][]"));
		buttons.setOpaque(false);

		JButton btnCopy = ToolStyle.button(I18n.t("hw.copy_report"), ToolIcons.Kind.COPY, I18n.t("hw.copy_report_tip"));
		btnCopy.addActionListener(e -> {
			ToolStyle.copyToClipboard(buildReport());
			lblUpdated.setText(I18n.t("hw.copied"));
		});
		buttons.add(btnCopy);

		btnPause = new JToggleButton(I18n.t("hw.pause"));
		btnPause.setToolTipText(I18n.t("hw.pause_tip"));
		btnPause.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		btnPause.addActionListener(e -> {
			paused = btnPause.isSelected();
			btnPause.setText(I18n.t(paused ? "hw.resume" : "hw.pause"));
		});
		buttons.add(btnPause);

		btnOnTop = new JToggleButton(I18n.t("hw.on_top"), true);
		btnOnTop.setToolTipText(I18n.t("hw.on_top_tip"));
		btnOnTop.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		btnOnTop.addActionListener(e -> setAlwaysOnTop(btnOnTop.isSelected()));
		buttons.add(btnOnTop);

		header.add(buttons, "aligny top, wrap");
		lblUpdated = ToolStyle.mutedLabel(" ");
		header.add(lblUpdated, "span 2");
		return header;
	}

	/** Text of a label id without the trailing colon and spaces some older ids carry */
	private static String plain(String text) {
		return text == null ? "" : text.trim().replaceAll("[:\\s]+$", "");
	}

	/** Value of a getter as text, "n/a" when it is empty or the platform cannot read it */
	private static String read(java.util.concurrent.Callable<?> reader) {
		try {
			Object value = reader.call();
			return safe(value == null ? null : String.valueOf(value));
		} catch (Throwable e) {
			return I18n.t("hw.not_available");
		}
	}

	/** Pseudo and system volumes (tmpfs, snaps, containers, APFS helper volumes...) are not worth a row */
	private static boolean hiddenVolume(OSFileStore store) {
		String type = store.getType() == null ? "" : store.getType().toLowerCase(Locale.ROOT);
		String mount = store.getMount() == null ? "" : store.getMount();
		String[] pseudo = {"tmpfs", "devtmpfs", "squashfs", "overlay", "proc", "sysfs", "cgroup", "cgroup2", "efivarfs", "devfs", "autofs", "ramfs", "fuse.gvfsd-fuse", "fusectl", "debugfs", "tracefs"};
		for (String name : pseudo) {
			if (type.equals(name)) {
				return true;
			}
		}
		if (mount.startsWith("/snap/") || mount.startsWith("/var/lib/docker") || mount.startsWith("/private/var/vm")) {
			return true;
		}
		if (mount.startsWith("/run/") && !mount.startsWith("/run/media/")) {
			return true;
		}
		return mount.startsWith("/System/Volumes/") && !mount.equals("/System/Volumes/Data");
	}

	private static String safe(String text) {
		return text == null || text.trim().isEmpty() ? I18n.t("hw.not_available") : text.trim();
	}

////////////////////////// GPU //////////////////////////

	/** Label and value shown only while the live NVIDIA data is on */
	private JLabel liveRow(KvCard card, String label) {
		JLabel key = ToolStyle.mutedLabel(label);
		JLabel value = new JLabel(" ");
		value.setFont(ToolStyle.font(0, Font.BOLD));
		key.setVisible(false);
		value.setVisible(false);
		card.panel.add(key);
		card.panel.add(value, "growx, wmin 40, wrap");
		gpuLiveComponents.add(key);
		gpuLiveComponents.add(value);
		return value;
	}

	private static String osName() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
	}

	private static boolean isWindows() {
		return osName().startsWith("windows");
	}

	private static boolean isMac() {
		return osName().startsWith("mac");
	}

	private static boolean isLinux() {
		return osName().startsWith("linux");
	}

	/** Looks for an executable in the PATH folders */
	private static boolean inPath(String name) {
		String path = System.getenv("PATH");
		if (path == null) {
			return false;
		}
		for (String folder : path.split(File.pathSeparator)) {
			if (new File(folder, name).isFile() || new File(folder, name + ".exe").isFile()) {
				return true;
			}
		}
		return false;
	}

	/** Reads the graphics adapters once, away from the event thread (Windows: ~1.5 s of PowerShell) */
	private void startGpuQuery() {
		Thread query = new Thread(new Runnable() {
			@Override
			public void run() {
				final List<String[]> adapters = new ArrayList<String[]>();
				final boolean smi = inPath("nvidia-smi");
				final boolean supported = isWindows() || isMac() || isLinux();
				if (supported) {
					try {
						adapters.addAll(queryAdapters());
					} catch (Throwable e) {
						logger.warn("Cannot read the graphics adapters", e);
					}
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (isActive.get()) {
							applyAdapters(adapters, supported, smi);
						}
					}
				});
			}
		}, "HwInfo-Gpu");
		query.setDaemon(true);
		query.start();
	}

	/** Name and description (driver, video memory...) of every graphics adapter, by operating system */
	private static List<String[]> queryAdapters() throws IOException, InterruptedException {
		if (isWindows()) {
			return queryWindowsAdapters();
		}
		if (isMac()) {
			return queryMacAdapters();
		}
		return queryLinuxAdapters();
	}

	/** Runs a command and returns its output lines (errors included); it is killed after the timeout */
	private static List<String> runLines(int timeoutSeconds, String... command) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		final Process proc = pb.start();
		java.util.Timer watchdog = new java.util.Timer("HwInfo-Watchdog", true);
		watchdog.schedule(new java.util.TimerTask() {
			@Override
			public void run() {
				proc.destroy();
			}
		}, timeoutSeconds * 1000L);
		List<String> lines = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			proc.waitFor();
		} finally {
			watchdog.cancel();
			proc.destroy();
		}
		return lines;
	}

	private static String join(String first, String second) {
		if (first.isEmpty() || second.isEmpty()) {
			return first + second;
		}
		return first + "   -   " + second;
	}

	/** WMI for name and driver, registry for the real video memory (the WMI value stops at 4 GB) */
	private static List<String[]> queryWindowsAdapters() throws IOException, InterruptedException {
		String script = "$ProgressPreference='SilentlyContinue'\n"
				+ "[Console]::OutputEncoding=[Text.Encoding]::UTF8\n"
				+ "Get-CimInstance Win32_VideoController | ForEach-Object { 'V|' + $_.Name + '|' + $_.DriverVersion + '|' + $_.AdapterRAM }\n"
				+ "Get-ItemProperty 'HKLM:\\SYSTEM\\CurrentControlSet\\Control\\Video\\*\\0000' -ErrorAction SilentlyContinue | ForEach-Object { $v=$_.'HardwareInformation.qwMemorySize'; "
				+ "if($v -is [byte[]]){$v=[BitConverter]::ToUInt64($v,0)}; if($v){ 'M|' + $_.DriverDesc + '|' + $v } }\n";
		String encoded = java.util.Base64.getEncoder().encodeToString(script.getBytes("UTF-16LE"));
		List<String[]> found = new ArrayList<String[]>();
		Map<String, Long> memory = new LinkedHashMap<String, Long>();
		for (String line : runLines(20, "powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded)) {
			String[] parts = line.trim().split("\\|");
			try {
				if (parts.length >= 4 && parts[0].equals("V")) {
					found.add(new String[] {parts[1], parts[2], parts[3]});
				} else if (parts.length >= 3 && parts[0].equals("M")) {
					memory.put(parts[1].toLowerCase(Locale.ROOT), Long.parseLong(parts[2].trim()));
				}
			} catch (NumberFormatException e) {
				// not a data line
			}
		}
		List<String[]> adapters = new ArrayList<String[]>();
		for (String[] adapter : found) {
			Long real = memory.get(adapter[0].toLowerCase(Locale.ROOT));
			long bytes = real != null ? real.longValue() : parseLong(adapter[2]);
			String detail = join(adapter[1].trim().isEmpty() ? "" : I18n.t("hw.gpu_driver") + " " + adapter[1].trim(),
					bytes > 0 ? I18n.t("hw.gpu_memory") + " " + ToolStyle.size(bytes) : "");
			adapters.add(new String[] {adapter[0], detail});
		}
		return adapters;
	}

	private static long parseLong(String text) {
		try {
			return Long.parseLong(text.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** lspci for the adapters and their kernel driver, sysfs for the memory of AMD cards (not verified on every distribution) */
	private static List<String[]> queryLinuxAdapters() throws IOException, InterruptedException {
		return parseLspci(runLines(10, "lspci", "-vmm", "-k"));
	}

	static List<String[]> parseLspci(List<String> output) {
		List<String> lines = new ArrayList<String>(output);
		lines.add("");
		List<String[]> adapters = new ArrayList<String[]>();
		String slot = "";
		String type = "";
		String vendor = "";
		String device = "";
		String driver = "";
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				if (type.contains("VGA") || type.contains("3D controller") || type.contains("Display controller")) {
					String name = cleanVendor(vendor) + " " + friendlyDevice(device);
					long bytes = 0;
					String id = slot.indexOf(':') == slot.lastIndexOf(':') ? "0000:" + slot : slot;
					File vram = new File("/sys/bus/pci/devices/" + id + "/mem_info_vram_total");
					if (vram.isFile()) {
						try {
							BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(vram), "UTF-8"));
							try {
								bytes = parseLong(reader.readLine());
							} finally {
								reader.close();
							}
						} catch (IOException e) {
							// memory unknown
						}
					}
					String detail = join(driver.isEmpty() ? "" : I18n.t("hw.gpu_driver") + " " + driver,
							bytes > 0 ? I18n.t("hw.gpu_memory") + " " + ToolStyle.size(bytes) : "");
					adapters.add(new String[] {name.trim(), detail});
				}
				slot = "";
				type = "";
				vendor = "";
				device = "";
				driver = "";
				continue;
			}
			int colon = line.indexOf(':');
			if (colon < 0) {
				continue;
			}
			String key = line.substring(0, colon).trim();
			String value = line.substring(colon + 1).trim();
			if (key.equals("Slot")) {
				slot = value;
			} else if (key.equals("Class")) {
				type = value;
			} else if (key.equals("Vendor")) {
				vendor = value;
			} else if (key.equals("Device")) {
				device = value;
			} else if (key.equals("Driver")) {
				driver = value;
			}
		}
		return adapters;
	}

	private static String cleanVendor(String vendor) {
		return vendor.replace("Corporation", "").replace("Inc.", "").replace("[AMD/ATI]", "AMD").replace("Advanced Micro Devices,", "").trim();
	}

	/** lspci writes "GA106M [GeForce RTX 3060 Mobile]": the marketing name is the one in the brackets */
	private static String friendlyDevice(String device) {
		int open = device.indexOf('[');
		int close = device.lastIndexOf(']');
		return open >= 0 && close > open ? device.substring(open + 1, close) : device;
	}

	/** system_profiler: chipset, number of cores (Apple silicon) and video memory (Intel / AMD) */
	private static List<String[]> queryMacAdapters() throws IOException, InterruptedException {
		return parseSystemProfiler(runLines(20, "system_profiler", "SPDisplaysDataType"));
	}

	static List<String[]> parseSystemProfiler(List<String> output) {
		List<String> lines = new ArrayList<String>(output);
		lines.add("Chipset Model: ");
		List<String[]> adapters = new ArrayList<String[]>();
		String name = null;
		String cores = "";
		String vram = "";
		for (String line : lines) {
			String t = line.trim();
			if (t.startsWith("Chipset Model:")) {
				if (name != null && !name.isEmpty()) {
					adapters.add(new String[] {name, join(cores.isEmpty() ? "" : I18n.t("hw.gpu_cores") + " " + cores,
							vram.isEmpty() ? "" : I18n.t("hw.gpu_memory") + " " + vram)});
				}
				name = t.substring("Chipset Model:".length()).trim();
				cores = "";
				vram = "";
			} else if (t.startsWith("Total Number of Cores:")) {
				cores = t.substring(t.indexOf(':') + 1).trim();
			} else if (t.startsWith("VRAM")) {
				vram = t.substring(t.indexOf(':') + 1).trim();
			}
		}
		return adapters;
	}

	private void applyAdapters(List<String[]> adapters, boolean supported, boolean smiFound) {
		gpuAdapters.removeAll();
		if (!supported) {
			gpuAdapters.add(ToolStyle.mutedLabel(I18n.t("hw.gpu_unsupported")));
		} else if (adapters.isEmpty()) {
			gpuAdapters.add(ToolStyle.mutedLabel(I18n.t("hw.not_available")));
		}
		for (String[] adapter : adapters) {
			JLabel name = new JLabel(adapter[0]);
			name.setFont(ToolStyle.font(0, Font.BOLD));
			name.setToolTipText(adapter[0]);
			gpuAdapters.add(name, "wmin 40");
			if (!adapter[1].isEmpty()) {
				gpuAdapters.add(ToolStyle.mutedLabel(adapter[1]), "wmin 40, gapbottom 2");
			}
			reportRows.add(new Row(gpuTitle, adapter[0], new JLabel(adapter[1])));
		}
		gpuLive.setEnabled(smiFound);
		if (!smiFound) {
			gpuLive.setToolTipText(I18n.t("hw.gpu_no_smi"));
		}
		gpuAdapters.revalidate();
		gpuAdapters.repaint();
	}

	/** Starts or stops the stream of nvidia-smi (one line every 2 seconds). Off by default: on laptops reading the GPU can keep it awake. */
	private void setGpuLive(boolean on) {
		for (Component component : gpuLiveComponents) {
			component.setVisible(on);
		}
		stopGpuLive();
		if (!on) {
			return;
		}
		final int generation = ++smiGeneration;
		Thread reader = new Thread(new Runnable() {
			@Override
			public void run() {
				Process proc = null;
				try {
					ProcessBuilder pb = new ProcessBuilder("nvidia-smi", "--query-gpu=temperature.gpu,utilization.gpu,memory.used,memory.total,power.draw,clocks.gr,clocks.max.gr",
							"--format=csv,noheader,nounits", "-l", "2");
					pb.redirectErrorStream(true);
					proc = pb.start();
					smiProcess = proc;
					BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
					String line;
					while ((line = in.readLine()) != null && generation == smiGeneration && isActive.get()) {
						final String[] f = line.split(",");
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								if (generation == smiGeneration) {
									applyGpuLine(f);
								}
							}
						});
					}
				} catch (IOException e) {
					logger.warn("nvidia-smi failed", e);
				} finally {
					if (proc != null) {
						proc.destroy();
					}
				}
			}
		}, "HwInfo-NvidiaSmi");
		reader.setDaemon(true);
		reader.start();
	}

	private void stopGpuLive() {
		smiGeneration++;
		Process proc = smiProcess;
		smiProcess = null;
		if (proc != null) {
			proc.destroy();
		}
	}

	private void applyGpuLine(String[] f) {
		if (f.length < 7) {
			put(gpuLoad, f.length > 0 ? f[0].trim() : I18n.t("hw.not_available"));
			return;
		}
		String na = I18n.t("hw.not_available");
		put(gpuTemp, isNumber(f[0]) ? new DecimalFormat("0.#").format(Double.parseDouble(f[0].trim())) + " \u00B0C" : na);
		put(gpuLoad, isNumber(f[1]) ? f[1].trim() + "%" : na);
		put(gpuVram, isNumber(f[2]) && isNumber(f[3]) ? ToolStyle.size(Long.parseLong(f[2].trim()) * 1048576L) + " / " + ToolStyle.size(Long.parseLong(f[3].trim()) * 1048576L) : na);
		put(gpuPower, isNumber(f[4]) ? f[4].trim() + " W" : na);
		put(gpuClock, isNumber(f[5]) ? f[5].trim() + (isNumber(f[6]) ? " / " + f[6].trim() : "") + " MHz" : na);
	}

	private static boolean isNumber(String text) {
		return text != null && text.trim().matches("[0-9]+(\\.[0-9]+)?");
	}

////////////////////////// STATIC INFO //////////////////////////

	private void loadStaticInfo() {
		if (diskList.length > 0) {
			comboDisk.setSelectedIndex(0);
		}
		updateDiskStatic();
		updateDisplayInfo();
		updateJavaScreens();
		sysUptime.setText(I18n.t("hw.not_available"));
		cpuTemp.setText(I18n.t("hw.not_available"));
		cpuVolt.setText(I18n.t("hw.not_available"));
		cpuFans.setText(I18n.t("hw.not_available"));
		powerBattery.setText(I18n.t("hw.not_available"));
		powerTime.setText(I18n.t("hw.not_available"));
	}

	private void updateDiskStatic() {
		HDinfo selected = (HDinfo) comboDisk.getSelectedItem();
		if (selected == null) {
			dkSerial.setText(I18n.t("hw.not_available"));
			return;
		}
		HWDiskStore disk = selected.getDisk();
		put(dkSerial, safe(disk.getSerial()));
		put(dkReadTotal, "...");
		put(dkWriteTotal, "...");
		put(dkReadSpeed, "...");
		put(dkWriteSpeed, "...");
		put(dkQueue, "...");
	}

	private void updateDisplayInfo() {
		int index = comboDisplay.getSelectedIndex();
		if (index < 0 || index >= displays.length) {
			for (JLabel label : new JLabel[] {dpName, dpManufacturer, dpSize, dpResolution, dpDate}) {
				label.setText(I18n.t("hw.not_available"));
			}
			dpRaw.setText("");
			return;
		}
		String text = String.valueOf(displays[index]);
		dpRaw.setText(text.trim());
		dpRaw.setCaretPosition(0);
		String name = null;
		for (String line : text.split("\n")) {
			for (String key : EDID_NAME_KEYS) {
				int at = line.indexOf(key);
				if (name == null && at != -1) {
					name = line.substring(at + key.length()).trim();
				}
			}
		}
		put(dpName, name == null || name.isEmpty() ? I18n.t("hw.not_available") : name);
		put(dpManufacturer, firstGroup(text, "Manuf\\. ID=(\\w+)") + firstGroupSuffix(text, "Product ID=(\\w+)", " / "));
		put(dpSize, firstGroup(text, "(\\d+ x \\d+ cm \\([^)]*\\))"));
		put(dpResolution, firstGroup(text, "Active Pixels (\\d+x\\d+)"));
		put(dpDate, firstGroup(text, "ManufDate=([\\d/]+)"));
	}

	private static String firstGroup(String text, String regex) {
		Matcher m = Pattern.compile(regex).matcher(text);
		return m.find() ? m.group(1) : I18n.t("hw.not_available");
	}

	private static String firstGroupSuffix(String text, String regex, String separator) {
		Matcher m = Pattern.compile(regex).matcher(text);
		return m.find() ? separator + m.group(1) : "";
	}

	/** Monitors as Java sees them: resolution, interface scale and refresh rate */
	private void updateJavaScreens() {
		StringBuilder out = new StringBuilder();
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice primary = ge.getDefaultScreenDevice();
			for (GraphicsDevice device : ge.getScreenDevices()) {
				GraphicsConfiguration gc = device.getDefaultConfiguration();
				int scale = (int) Math.round(gc.getDefaultTransform().getScaleX() * 100);
				int hz = device.getDisplayMode().getRefreshRate();
				if (out.length() > 0) {
					out.append("  |  ");
				}
				out.append(device.getDisplayMode().getWidth()).append('x').append(device.getDisplayMode().getHeight());
				out.append(" @").append(scale).append('%');
				if (hz > 0) {
					out.append(' ').append(hz).append(" Hz");
				}
				if (device == primary) {
					out.append(" (").append(I18n.t("hw.primary")).append(')');
				}
			}
		} catch (Throwable e) {
			logger.warn("Cannot read the screens seen by Java", e);
		}
		put(dpJava, out.length() == 0 ? I18n.t("hw.not_available") : out.toString());
	}

////////////////////////// SAMPLER (BACKGROUND THREAD) //////////////////////////

	/** Everything read in one refresh, handed to the event thread */
	private static final class Sample {
		double cpuLoad = -1;
		double[] cores;
		long memTotal;
		long memAvailable;
		long swapTotal;
		long swapUsed;
		double procCpu;
		long heapUsed;
		long heapMax;
		long rss = -1;
		int jvmThreads;
		long uptime = -1;
		int processes = -1;
		int threads = -1;
		boolean sensorsFresh;
		double temperature;
		double voltage;
		int[] fans;
		boolean diskFresh;
		long diskReadBytes;
		long diskWriteBytes;
		double diskReadRate = -1;
		double diskWriteRate = -1;
		long diskQueue;
		List<Volume> volumes;
		boolean powerFresh;
		String battery;
		String batteryTime;
		boolean netFresh;
		double netDown = -1;
		double netUp = -1;
		String netIfs;
	}

	private static final class Volume {
		String name;
		long total;
		long usable;
	}

	private void sampleLoop() {
		long tick = 1;
		while (isActive.get()) {
			try {
				Thread.sleep(REFRESH_MS);
			} catch (InterruptedException e) {
				return;
			}
			if (!paused) {
				try {
					final Sample sample = collect(tick);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (isActive.get()) {
								apply(sample);
							}
						}
					});
				} catch (Throwable e) {
					logger.warn("Hardware sampling failed", e);
				}
				tick++;
			}
		}
	}

	private Sample collect(long tick) {
		Sample s = new Sample();
		boolean slow = tick % SLOW_EVERY == 0;

		try {
			s.cpuLoad = cpu.getSystemCpuLoadBetweenTicks();
			s.cores = cpu.getProcessorCpuLoadBetweenTicks();
			s.uptime = cpu.getSystemUptime();
		} catch (Throwable e) {
			logger.debug("cpu: " + e);
		}

		try {
			GlobalMemory memory = hardware.getMemory();
			s.memTotal = memory.getTotal();
			s.memAvailable = memory.getAvailable();
			s.swapTotal = memory.getSwapTotal();
			s.swapUsed = memory.getSwapUsed();
		} catch (Throwable e) {
			logger.debug("memory: " + e);
		}

		try {
			Runtime rt = Runtime.getRuntime();
			s.heapUsed = rt.totalMemory() - rt.freeMemory();
			s.heapMax = rt.maxMemory();
			s.jvmThreads = ManagementFactory.getThreadMXBean().getThreadCount();
			s.procCpu = Math.max(0, Math.min(100, RuntimeUtil.getCurrentProcCpuUsage())) / 100d;
			if (slow) {
				OSProcess self = os.getProcess(os.getProcessId());
				if (self != null) {
					s.rss = self.getResidentSetSize();
				}
			}
		} catch (Throwable e) {
			logger.debug("process: " + e);
		}

		if (slow) {
			try {
				s.sensorsFresh = true;
				s.temperature = hardware.getSensors().getCpuTemperature();
				s.voltage = hardware.getSensors().getCpuVoltage();
				s.fans = hardware.getSensors().getFanSpeeds();
			} catch (Throwable e) {
				logger.debug("sensors: " + e);
			}
			try {
				s.processes = os.getProcessCount();
				s.threads = os.getThreadCount();
			} catch (Throwable e) {
				logger.debug("os counters: " + e);
			}
			try {
				List<Volume> volumes = new ArrayList<>();
				for (OSFileStore store : os.getFileSystem().getFileStores()) {
					if (store.getTotalSpace() <= 0 || hiddenVolume(store)) {
						continue;
					}
					Volume v = new Volume();
					String type = store.getType() == null || store.getType().isEmpty() ? "" : "  [" + store.getType() + "]";
					String label = store.getName() == null || store.getName().isEmpty() ? store.getMount() : store.getName();
					v.name = label + type;
					v.total = store.getTotalSpace();
					v.usable = store.getUsableSpace();
					volumes.add(v);
				}
				s.volumes = volumes;
			} catch (Throwable e) {
				logger.debug("volumes: " + e);
			}
			try {
				s.powerFresh = true;
				PowerSource[] sources = hardware.getPowerSources();
				if (sources != null && sources.length > 0) {
					StringBuilder level = new StringBuilder();
					StringBuilder time = new StringBuilder();
					for (PowerSource source : sources) {
						if (level.length() > 0) {
							level.append("  |  ");
							time.append("  |  ");
						}
						level.append(source.getName()).append(": ").append(new DecimalFormat("0").format(source.getRemainingCapacity() * 100)).append('%');
						double left = source.getTimeRemaining();
						if (left == -2) {
							time.append(I18n.t("hw.on_ac"));
						} else if (left < 0) {
							time.append(I18n.t("hw.not_available"));
						} else {
							time.append(duration((long) left));
						}
					}
					s.battery = level.toString();
					s.batteryTime = time.toString();
				}
			} catch (Throwable e) {
				logger.debug("power: " + e);
			}
		}

		collectDisk(s);
		collectNetwork(s);
		return s;
	}

	private void collectDisk(Sample s) {
		int index = selectedDisk;
		if (index < 0 || index >= diskList.length) {
			return;
		}
		try {
			HWDiskStore disk = diskList[index].getDisk();
			disk.updateDiskStats();
			long now = disk.getTimeStamp() > 0 ? disk.getTimeStamp() : System.currentTimeMillis();
			s.diskFresh = true;
			s.diskReadBytes = disk.getReadBytes();
			s.diskWriteBytes = disk.getWriteBytes();
			s.diskQueue = disk.getCurrentQueueLength();
			if (!diskChanged && prevDiskTs > 0 && now > prevDiskTs) {
				double seconds = (now - prevDiskTs) / 1000d;
				s.diskReadRate = Math.max(0, (s.diskReadBytes - prevDiskRead) / seconds);
				s.diskWriteRate = Math.max(0, (s.diskWriteBytes - prevDiskWrite) / seconds);
			}
			diskChanged = false;
			prevDiskTs = now;
			prevDiskRead = s.diskReadBytes;
			prevDiskWrite = s.diskWriteBytes;
		} catch (Throwable e) {
			logger.debug("disk: " + e);
		}
	}

	private void collectNetwork(Sample s) {
		try {
			long recv = 0;
			long sent = 0;
			StringBuilder names = new StringBuilder();
			for (NetworkIF net : networkIfs) {
				String[] ipv4 = net.getIPv4addr();
				if (ipv4 == null || ipv4.length == 0) {
					continue;
				}
				java.net.NetworkInterface ni = net.getNetworkInterface();
				if (ni != null && (ni.isLoopback() || !ni.isUp())) {
					continue;
				}
				net.updateNetworkStats();
				recv += net.getBytesRecv();
				sent += net.getBytesSent();
				if (names.length() > 0) {
					names.append(", ");
				}
				names.append(net.getDisplayName());
			}
			long now = System.currentTimeMillis();
			s.netFresh = true;
			s.netIfs = names.toString();
			if (prevNetTs > 0 && now > prevNetTs && recv >= prevNetRecv && sent >= prevNetSent) {
				double seconds = (now - prevNetTs) / 1000d;
				s.netDown = (recv - prevNetRecv) / seconds;
				s.netUp = (sent - prevNetSent) / seconds;
			}
			prevNetTs = now;
			prevNetRecv = recv;
			prevNetSent = sent;
		} catch (Throwable e) {
			logger.debug("network: " + e);
		}
	}

////////////////////////// APPLY (EVENT THREAD) //////////////////////////

	private void apply(Sample s) {
		sampleCount++;
		lblUpdated.setText(I18n.t("hw.updated") + " " + new SimpleDateFormat("HH:mm:ss").format(new Date()));

		// CPU
		if (s.cpuLoad >= 0) {
			cpuCount++;
			cpuSum += s.cpuLoad;
			cpuMax = Math.max(cpuMax, s.cpuLoad);
			cpuCard.set(s.cpuLoad, pct(s.cpuLoad));
			put(cpuAvg, pct(cpuSum / cpuCount));
			put(cpuPeak, pct(cpuMax));
		}
		if (s.cores != null) {
			coreBars.setLoads(s.cores);
		}

		// memory
		if (s.memTotal > 0) {
			long used = s.memTotal - s.memAvailable;
			double fraction = used / (double) s.memTotal;
			ramCard.set(fraction, pct(fraction));
			put(ramUsed, ToolStyle.size(used) + "  (" + pct(fraction) + ")");
			put(ramFree, ToolStyle.size(s.memAvailable) + "  (" + pct(1 - fraction) + ")");
			put(ramTotal, ToolStyle.size(s.memTotal));
			put(ramSwap, s.swapTotal > 0 ? ToolStyle.size(s.swapUsed) + " / " + ToolStyle.size(s.swapTotal) : I18n.t("hw.not_available"));
		}

		// ScreepT
		screeptCard.set(s.procCpu, pct(s.procCpu));
		if (s.rss > 0 && s.memTotal > 0) {
			put(scRam, ToolStyle.size(s.rss) + "  (" + pct(s.rss / (double) s.memTotal) + ")");
		} else if (s.rss <= 0 && scRam.getText().isEmpty()) {
			put(scRam, I18n.t("hw.not_available"));
		}
		put(scHeap, ToolStyle.size(s.heapUsed) + " / " + ToolStyle.size(s.heapMax));
		put(scThreads, String.valueOf(s.jvmThreads));

		// system
		if (s.uptime >= 0) {
			put(sysUptime, duration(s.uptime));
		}
		if (s.processes >= 0) {
			put(sysProcesses, String.valueOf(s.processes));
		}
		if (s.threads >= 0) {
			put(sysThreads, String.valueOf(s.threads));
		}

		// sensors
		if (s.sensorsFresh) {
			boolean hasTemp = s.temperature > 0;
			boolean hasVolt = s.voltage > 0;
			boolean hasFans = s.fans != null && s.fans.length > 0 && anyPositive(s.fans);
			put(cpuTemp, hasTemp ? new DecimalFormat("0.#").format(s.temperature) + " °C" : I18n.t("hw.not_available"));
			put(cpuVolt, hasVolt ? new DecimalFormat("0.###").format(s.voltage) + " V" : I18n.t("hw.not_available"));
			put(cpuFans, hasFans ? fansText(s.fans) : I18n.t("hw.not_available"));
			cpuSensorNote.setVisible(!hasTemp && !hasVolt && !hasFans);
		}

		// power
		if (s.powerFresh) {
			put(powerBattery, s.battery == null ? I18n.t("hw.no_battery") : s.battery);
			put(powerTime, s.batteryTime == null ? I18n.t("hw.not_available") : s.batteryTime);
		}

		// network
		if (s.netFresh) {
			put(netIfs, s.netIfs == null || s.netIfs.isEmpty() ? I18n.t("hw.not_available") : s.netIfs);
			if (s.netDown >= 0) {
				put(netDown, ToolStyle.size((long) s.netDown) + "/s");
				put(netUp, ToolStyle.size((long) s.netUp) + "/s");
			} else {
				put(netDown, "...");
				put(netUp, "...");
			}
		}

		// disk
		if (s.diskFresh) {
			put(dkReadTotal, ToolStyle.size(s.diskReadBytes));
			put(dkWriteTotal, ToolStyle.size(s.diskWriteBytes));
			put(dkReadSpeed, s.diskReadRate >= 0 ? ToolStyle.size((long) s.diskReadRate) + "/s" : "...");
			put(dkWriteSpeed, s.diskWriteRate >= 0 ? ToolStyle.size((long) s.diskWriteRate) + "/s" : "...");
			put(dkQueue, String.valueOf(s.diskQueue));
		}
		if (s.volumes != null) {
			applyVolumes(s.volumes);
		}
	}

	private static boolean anyPositive(int[] values) {
		for (int v : values) {
			if (v > 0) {
				return true;
			}
		}
		return false;
	}

	private static String fansText(int[] fans) {
		StringBuilder out = new StringBuilder();
		for (int fan : fans) {
			if (fan > 0) {
				if (out.length() > 0) {
					out.append(" | ");
				}
				out.append(fan).append(" rpm");
			}
		}
		return out.toString();
	}

	private void applyVolumes(List<Volume> volumes) {
		if (volumes.size() != volumeRows.size()) {
			volumesPanel.removeAll();
			volumeRows.clear();
			for (int i = 0; i < volumes.size(); i++) {
				VolumeRow row = new VolumeRow();
				volumeRows.add(row);
				volumesPanel.add(row.name);
				volumesPanel.add(row.bar, "h 10!");
				volumesPanel.add(row.text, "wmin 150");
			}
			volumesPanel.revalidate();
		}
		for (int i = 0; i < volumes.size(); i++) {
			Volume v = volumes.get(i);
			VolumeRow row = volumeRows.get(i);
			double used = (v.total - v.usable) / (double) v.total;
			put(row.name, v.name);
			row.bar.set(used);
			put(row.text, I18n.fmt("hw.volume_free", ToolStyle.size(v.usable), ToolStyle.size(v.total)));
		}
	}

	/** Sets the text only when it changes (less repaints) and shows it complete in the tooltip when it is cut */
	private static void put(JLabel label, String text) {
		if (text == null) {
			text = "";
		}
		if (!text.equals(label.getText())) {
			label.setText(text);
			label.setToolTipText(text);
		}
	}

	private static String pct(double fraction) {
		return new DecimalFormat("0.#").format(Math.max(0, Math.min(1, fraction)) * 100) + "%";
	}

	private static String duration(long seconds) {
		long days = seconds / 86400;
		long hours = (seconds % 86400) / 3600;
		long minutes = (seconds % 3600) / 60;
		if (days > 0) {
			return days + " d " + hours + " h " + minutes + " min";
		}
		if (hours > 0) {
			return hours + " h " + minutes + " min";
		}
		return minutes + " min " + (seconds % 60) + " s";
	}

////////////////////////// REPORT //////////////////////////

	private static final class Row {
		final String section;
		final String label;
		final JLabel value;

		Row(String section, String label, JLabel value) {
			this.section = section;
			this.label = label;
			this.value = value;
		}
	}

	/** All the values shown in the window as plain text */
	private String buildReport() {
		Map<String, StringBuilder> sections = new LinkedHashMap<String, StringBuilder>();
		for (Row row : reportRows) {
			StringBuilder out = sections.get(row.section);
			if (out == null) {
				out = new StringBuilder();
				sections.put(row.section, out);
			}
			out.append(row.label.isEmpty() ? "" : row.label + ": ").append(row.value.getText()).append('\n');
		}
		StringBuilder report = new StringBuilder("ScreepT - " + I18n.t("hw.title") + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ")\n");
		for (Map.Entry<String, StringBuilder> entry : sections.entrySet()) {
			report.append("\n[").append(entry.getKey()).append("]\n").append(entry.getValue());
		}
		if (!volumeRows.isEmpty()) {
			report.append("\n[").append(I18n.t("hw.volumes")).append("]\n");
			for (VolumeRow row : volumeRows) {
				report.append(row.name.getText()).append(": ").append(row.text.getText()).append('\n');
			}
		}
		return report.toString();
	}

////////////////////////// GUI COMPONENTS //////////////////////////

	/** Card with label / value rows, in one or two columns of pairs */
	private final class KvCard {
		final JPanel panel;
		final String title;
		final int pairs;
		int count;

		KvCard(String title, int pairs) {
			this.title = title;
			this.pairs = pairs;
			String columns = pairs == 1 ? "[][grow,fill]" : "[][grow,fill]28[][grow,fill]";
			panel = ToolStyle.card(new MigLayout("insets 0, fillx, hidemode 3, gapx 14, gapy 4", columns));
			panel.add(ToolStyle.caption(title), "span, gapbottom 6, wrap");
		}

		/** Row with a long value: it takes a whole line, below the pairs already added */
		JLabel wideRow(String label) {
			if (count % pairs != 0) {
				panel.add(new JLabel(), "span, wrap");
				count = 0;
			}
			panel.add(ToolStyle.mutedLabel(label));
			JLabel value = new JLabel(" ");
			value.setFont(ToolStyle.font(0, Font.BOLD));
			panel.add(value, "span, growx, wmin 40, wrap");
			reportRows.add(new Row(title, label, value));
			return value;
		}

		/** Adds a label with its value label (null label: empty cell, to align the rows) */
		JLabel row(String label) {
			if (label == null) {
				panel.add(new JLabel(), "span 2" + (++count % pairs == 0 ? ", wrap" : ""));
				return new JLabel();
			}
			panel.add(ToolStyle.mutedLabel(label));
			JLabel value = new JLabel(" ");
			value.setFont(ToolStyle.font(0, Font.BOLD));
			boolean last = ++count % pairs == 0;
			panel.add(value, "growx, wmin 40" + (last ? ", wrap" : ""));
			reportRows.add(new Row(title, label, value));
			return value;
		}
	}

	/** Card of a live value: big percentage, bar, history graph and detail rows */
	private final class MeterCard {
		final JPanel panel;
		final JLabel big = new JLabel("-");
		final MeterBar bar = new MeterBar();
		final HistoryGraph graph;
		final String title;
		int details;

		MeterCard(String title, Color accent) {
			this.title = title;
			graph = new HistoryGraph(accent);
			panel = ToolStyle.card(new MigLayout("insets 0, fillx, hidemode 3, gapx 14, gapy 4", "[][grow,fill]"));
			panel.add(ToolStyle.caption(title), "aligny center");
			big.setFont(ToolStyle.font(10f, Font.BOLD));
			panel.add(big, "align right, wrap");
			panel.add(bar, "span, growx, h 10!, wrap");
			panel.add(graph, "span, growx, h 48!, gaptop 4, gapbottom 6, wrap");
			reportRows.add(new Row(title, I18n.t("hw.usage"), big));
		}

		void set(double fraction, String text) {
			bar.set(fraction);
			graph.add(fraction);
			if (!text.equals(big.getText())) {
				big.setText(text);
			}
		}

		JLabel detail(String label) {
			panel.add(ToolStyle.mutedLabel(label));
			JLabel value = new JLabel(" ");
			value.setFont(ToolStyle.font(0, Font.BOLD));
			panel.add(value, "growx, wmin 40, wrap");
			details++;
			reportRows.add(new Row(title, label, value));
			return value;
		}
	}

	private static final class VolumeRow {
		final JLabel name = new JLabel(" ");
		final MeterBar bar = new MeterBar(0.85, 0.95);
		final JLabel text = new JLabel(" ");

		VolumeRow() {
			name.setFont(ToolStyle.font(0, Font.BOLD));
			text.setForeground(ToolStyle.muted());
		}
	}

	private static Color loadColor(double fraction) {
		if (fraction >= 0.85) {
			return ToolStyle.RED;
		}
		if (fraction >= 0.6) {
			return ToolStyle.ORANGE;
		}
		return ToolStyle.GREEN;
	}

	private static Color trackColor() {
		Color background = javax.swing.UIManager.getColor("Panel.background");
		Color foreground = javax.swing.UIManager.getColor("Label.foreground");
		if (background == null || foreground == null) {
			return new Color(128, 128, 128, 60);
		}
		return ToolStyle.blend(background, foreground, ToolStyle.isDark() ? 0.18f : 0.10f);
	}

	/** Horizontal rounded bar, green / orange / red by load (for disks a nearly full one is the problem, so the limits are higher) */
	private static final class MeterBar extends JComponent {
		private static final long serialVersionUID = 1L;
		private double value;
		private final double warn;
		private final double alarm;

		MeterBar() {
			this(0.6, 0.85);
		}

		MeterBar(double warn, double alarm) {
			this.warn = warn;
			this.alarm = alarm;
			setOpaque(false);
			setPreferredSize(new Dimension(100, 10));
		}

		void set(double fraction) {
			double clamped = Math.max(0, Math.min(1, fraction));
			if (Math.abs(clamped - value) > 0.0005) {
				value = clamped;
				repaint();
			}
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int h = getHeight();
				int w = getWidth();
				g.setColor(trackColor());
				g.fillRoundRect(0, 0, w, h, h, h);
				int fill = (int) Math.round(w * value);
				if (fill > 0) {
					g.setColor(value >= alarm ? ToolStyle.RED : value >= warn ? ToolStyle.ORANGE : ToolStyle.GREEN);
					g.fillRoundRect(0, 0, Math.max(fill, h), h, h, h);
				}
			} finally {
				g.dispose();
			}
		}
	}

	/** Last samples of a 0..1 value, drawn as a filled line */
	private static final class HistoryGraph extends JComponent {
		private static final long serialVersionUID = 1L;
		private final double[] ring = new double[HISTORY];
		private int count;
		private final Color accent;

		HistoryGraph(Color accent) {
			this.accent = accent;
			setOpaque(false);
			setPreferredSize(new Dimension(160, 48));
		}

		void add(double value) {
			System.arraycopy(ring, 1, ring, 0, ring.length - 1);
			ring[ring.length - 1] = Math.max(0, Math.min(1, value));
			count = Math.min(count + 1, ring.length);
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int w = getWidth();
				int h = getHeight();
				Color track = trackColor();
				g.setColor(track);
				g.fillRoundRect(0, 0, w, h, 8, 8);
				g.setColor(ToolStyle.withAlpha(ToolStyle.muted(), 70));
				g.drawLine(0, h / 2, w, h / 2);
				if (count < 2) {
					return;
				}
				int n = ring.length;
				int[] xs = new int[n + 2];
				int[] ys = new int[n + 2];
				int points = 0;
				for (int i = n - count; i < n; i++) {
					xs[points] = (int) Math.round((i / (double) (n - 1)) * (w - 1));
					ys[points] = (int) Math.round((h - 2) - ring[i] * (h - 4));
					points++;
				}
				int[] fx = new int[points + 2];
				int[] fy = new int[points + 2];
				System.arraycopy(xs, 0, fx, 0, points);
				System.arraycopy(ys, 0, fy, 0, points);
				fx[points] = xs[points - 1];
				fy[points] = h - 1;
				fx[points + 1] = xs[0];
				fy[points + 1] = h - 1;
				g.setColor(ToolStyle.withAlpha(accent, 60));
				g.fillPolygon(fx, fy, points + 2);
				g.setColor(accent);
				g.setStroke(new java.awt.BasicStroke(1.6f));
				g.drawPolyline(xs, ys, points);
			} finally {
				g.dispose();
			}
		}
	}

	/** One thin vertical bar per logical core, as the CPU view of the task managers */
	private static final class CoreBars extends JComponent {
		private static final long serialVersionUID = 1L;
		private double[] loads = new double[0];

		CoreBars() {
			setOpaque(false);
			setPreferredSize(new Dimension(200, 40));
			ToolTipManager.sharedInstance().registerComponent(this);
		}

		void setLoads(double[] values) {
			loads = values.clone();
			repaint();
		}

		@Override
		public String getToolTipText(MouseEvent e) {
			if (loads.length == 0) {
				return null;
			}
			int index = Math.min(loads.length - 1, Math.max(0, e.getX() * loads.length / Math.max(1, getWidth())));
			return "#" + (index + 1) + ": " + pct(loads[index]);
		}

		@Override
		protected void paintComponent(Graphics g0) {
			if (loads.length == 0) {
				return;
			}
			Graphics2D g = (Graphics2D) g0.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int n = loads.length;
				int gap = 3;
				double barW = Math.max(3, (getWidth() - gap * (n - 1)) / (double) n);
				int h = getHeight();
				Color track = trackColor();
				for (int i = 0; i < n; i++) {
					int x = (int) Math.round(i * (barW + gap));
					int w = (int) Math.round(barW);
					g.setColor(track);
					g.fillRoundRect(x, 0, w, h, 4, 4);
					int fill = (int) Math.round(h * Math.max(0, Math.min(1, loads[i])));
					if (fill > 0) {
						g.setColor(loadColor(loads[i]));
						g.fillRoundRect(x, h - fill, w, fill, 4, 4);
					}
				}
			} finally {
				g.dispose();
			}
		}
	}

////////////////////////// WINDOW //////////////////////////

	/**
	 * Shows the window fitting its content (scroll bars only when the screen is not enough), centered over the owner,
	 * so on the monitor of the main application.
	 */
	public void showOnAppScreen(Window owner) {
		DialogFit.apply(this, owner, 0, 0);
		setAlwaysOnTop(btnOnTop.isSelected());
		setVisible(true);
		ScreenMemory.remember(owner != null ? owner : this);
		// the first samples fill the graphs: ask the layout once more when the real fonts are known
		Timer relayout = new Timer(400, e -> revalidate());
		relayout.setRepeats(false);
		relayout.start();
	}

	@Override
	public void dispose() {
		isActive.set(false);
		if (samplerThread != null) {
			samplerThread.interrupt();
		}
		stopGpuLive();
		if (singletonInstance == this) {
			singletonInstance = null;
		}
		super.dispose();
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
		return new Dimension(1100, 780);
	}

	public static HwInfoFrame getInstance() {
		return singletonInstance == null ? new HwInfoFrame() : singletonInstance;
	}
}
