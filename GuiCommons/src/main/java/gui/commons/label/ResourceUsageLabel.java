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
package gui.commons.label;

import gui.commons.panel.options.OptionHelp;
import various.common.light.i18n.I18n;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Discreet live indicator of this application CPU and RAM usage (JDK only, refreshed every second):
 * ScreepT: CPU = process load on all cores, RAM = total memory of the process (heap and details in the tooltip);
 * PC: CPU = whole system load, RAM = physical memory used / total. Details in the tooltip.
 * <p>
 * Three parts side by side: the ScreepT values, a small pencil right next to the maximum memory of ScreepT (to change it,
 * see {@link #setMemoryEditAction}) and the PC values, so the pencil is never mistaken as belonging to the PC.
 */
public class ResourceUsageLabel extends JPanel {

	private static final long serialVersionUID = 2398750982735509831L;

	private static final int REFRESH_MS = 1000;
	private static final float FONT_DELTA = 1f;
	private static final long MB = 1024L * 1024L;
	private static final long GB = MB * 1024L;
	/** Widest expected texts (titles bold, rest plain): fixed width, so the menu bar does not move at every refresh */
	private static final String APP_TITLE = "ScreepT";
	private static final String APP_VALUES = "  CPU 100% · RAM 99999 MB";
	private static final String PC_TITLE = "PC";
	private static final String PC_VALUES = "  CPU 100% · RAM 999.9 / 999.9 GB";
	private static final String PC_SEPARATOR = "      |      ";
	/** 0 = invisible (background color), 1 = as visible as the values */
	private static final float SEPARATOR_VISIBILITY = 0.35f;

	/** Label with a fixed width computed from the widest text it can show */
	private static final class FixedLabel extends JLabel {
		private static final long serialVersionUID = 1L;
		private final String widthTitle;
		private final String widthValues;

		FixedLabel(String widthTitle, String widthValues, int alignment) {
			this.widthTitle = widthTitle;
			this.widthValues = widthValues;
			setHorizontalAlignment(alignment);
			setVerticalAlignment(SwingConstants.CENTER);
			setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
			updateUI();
		}

		/** A bit bigger than the default label font, also after a look and feel switch */
		@Override
		public void updateUI() {
			super.updateUI();
			java.awt.Font base = UIManager.getFont("Label.font");
			if (base != null) {
				setFont(new javax.swing.plaf.FontUIResource(base.deriveFont(base.getSize2D() + FONT_DELTA)));
			}
		}

		@Override
		public Dimension getPreferredSize() {
			FontMetrics metrics = getFontMetrics(getFont());
			FontMetrics bold = getFontMetrics(getFont().deriveFont(java.awt.Font.BOLD));
			Insets insets = getInsets();
			return new Dimension(bold.stringWidth(widthTitle) + metrics.stringWidth(widthValues) + insets.left + insets.right, super.getPreferredSize().height);
		}

		@Override
		public Dimension getMaximumSize() {
			return new Dimension(getPreferredSize().width, Short.MAX_VALUE);
		}
	}

	private final Runtime runtime = Runtime.getRuntime();
	/** Obtained off the EDT: the first use of the management classes takes a few hundred ms */
	private volatile OperatingSystemMXBean os;
	private final Timer timer = new Timer(REFRESH_MS, e -> refresh());
	private final FixedLabel appLabel = new FixedLabel(APP_TITLE, APP_VALUES, SwingConstants.RIGHT);
	private final FixedLabel pcLabel = new FixedLabel(PC_TITLE, PC_VALUES + PC_SEPARATOR, SwingConstants.LEFT);
	private final JButton editButton = new JButton(new PencilIcon(14));

	public ResourceUsageLabel() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setOpaque(false);
		setAlignmentY(CENTER_ALIGNMENT);
		setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 14));
		appLabel.setBorder(BorderFactory.createEmptyBorder());

		editButton.putClientProperty("JButton.buttonType", "toolBarButton");
		editButton.setFocusable(false);
		editButton.setMargin(new Insets(2, 3, 2, 3));
		editButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		editButton.setToolTipText(I18n.t("memory.change_the_maximum_memory_of_screept"));
		editButton.setMaximumSize(new Dimension(24, Short.MAX_VALUE));
		editButton.setVisible(false);

		add(appLabel);
		add(editButton);
		add(pcLabel);

		// details of ScreepT in a card that stays open while the mouse is on it (scrolls, takes the focus); nothing over the PC values
		OptionHelp.attachLive(I18n.t("memory.screept_resources_usage").replaceAll("<[^>]+>", "").trim(), this::plainDetails, appLabel);

		// loading the management classes is slow: do it in background so the main window opens sooner
		Thread loader = new Thread(() -> {
			os = ManagementFactory.getOperatingSystemMXBean();
			processCpuLoad();
			SwingUtilities.invokeLater(() -> {
				refresh();
				timer.start();
			});
		}, "ResourceUsageLabel-init");
		loader.setDaemon(true);
		loader.start();
	}

	/** Shows the pencil next to the maximum memory of ScreepT; clicking it runs the action */
	public void setMemoryEditAction(final Runnable action) {
		for (java.awt.event.ActionListener l : editButton.getActionListeners()) {
			editButton.removeActionListener(l);
		}
		editButton.addActionListener(e -> action.run());
		editButton.setVisible(action != null);
		revalidate();
	}

	public void stop() {
		timer.stop();
	}

	private void refresh() {
		long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / MB;
		long maxMb = runtime.maxMemory() / MB;
		double processCpu = processCpuLoad();
		long processBytes = processMemoryBytes();

		// colors follow the current look and feel: titles readable, values muted, separator barely visible
		Color valuesColor = UIManager.getColor("Label.disabledForeground");
		Color titleColor = UIManager.getColor("Label.foreground");
		Color background = getParent() != null ? getParent().getBackground() : UIManager.getColor("MenuBar.background");
		String values = hex(valuesColor);
		String title = hex(titleColor);
		String separator = hex(blend(background, valuesColor, SEPARATOR_VISIBILITY));

		appLabel.setText("<html><nobr>" + titled(title, "ScreepT", values, percent(processCpu) + " · RAM " + memoryText(processBytes)) + "</nobr></html>");

		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			com.sun.management.OperatingSystemMXBean sunOs = (com.sun.management.OperatingSystemMXBean) os;
			long totalPhysical = sunOs.getTotalPhysicalMemorySize();
			long usedPhysical = totalPhysical - sunOs.getFreePhysicalMemorySize();
			pcLabel.setText("<html><nobr>&nbsp;&nbsp;&nbsp;<font color=\"" + separator + "\">|</font>&nbsp;&nbsp;&nbsp;"
					+ titled(title, "PC", values, percent(sunOs.getSystemCpuLoad())
							+ String.format(" · RAM %.1f / %.1f GB", usedPhysical / (double) GB, totalPhysical / (double) GB))
					+ "</nobr></html>");
		}
	}

	private static String titled(String titleColor, String title, String valuesColor, String values) {
		return "<b><font color=\"" + titleColor + "\">" + title + "</font></b>&nbsp; <font color=\"" + valuesColor + "\">" + values + "</font>";
	}

	private static String hex(Color color) {
		return color == null ? "#888888" : String.format("#%06X", color.getRGB() & 0xFFFFFF);
	}

	private static Color blend(Color from, Color to, float amount) {
		if (from == null || to == null) {
			return to != null ? to : from;
		}
		return new Color(
				Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
				Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
				Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
	}

	private static String percent(double load) {
		return load < 0 ? I18n.t("memory.cpu_n_a") : String.format("CPU %d%%", Math.round(load * 100));
	}

	/** Separate class: JNA is touched only on Windows (and failures fall back to the JVM view) */
	private static final class WindowsMemory {

		/** PROCESS_MEMORY_COUNTERS of psapi (jna-platform does not map GetProcessMemoryInfo) */
		public static class Counters extends com.sun.jna.Structure {
			public int cb;
			public int pageFaultCount;
			public com.sun.jna.platform.win32.BaseTSD.SIZE_T peakWorkingSetSize;
			public com.sun.jna.platform.win32.BaseTSD.SIZE_T workingSetSize;
			public com.sun.jna.platform.win32.BaseTSD.SIZE_T quotaPeakPagedPoolUsage;
			public com.sun.jna.platform.win32.BaseTSD.SIZE_T quotaPagedPoolUsage;
			public com.sun.jna.platform.win32.BaseTSD.SIZE_T quotaPeakNonPagedPoolUsage;
			public com.sun.jna.platform.win32.BaseTSD.SIZE_T quotaNonPagedPoolUsage;
			public com.sun.jna.platform.win32.BaseTSD.SIZE_T pagefileUsage;
			public com.sun.jna.platform.win32.BaseTSD.SIZE_T peakPagefileUsage;

			@Override
			protected java.util.List<String> getFieldOrder() {
				return java.util.Arrays.asList("cb", "pageFaultCount", "peakWorkingSetSize", "workingSetSize", "quotaPeakPagedPoolUsage",
						"quotaPagedPoolUsage", "quotaPeakNonPagedPoolUsage", "quotaNonPagedPoolUsage", "pagefileUsage", "peakPagefileUsage");
			}
		}

		public interface Psapi extends com.sun.jna.win32.StdCallLibrary {
			boolean GetProcessMemoryInfo(com.sun.jna.platform.win32.WinNT.HANDLE process, Counters counters, int size);
		}

		private static Psapi psapi;

		static long workingSet() {
			if (psapi == null) {
				psapi = com.sun.jna.Native.load("psapi", Psapi.class);
			}
			Counters counters = new Counters();
			counters.cb = counters.size();
			if (psapi.GetProcessMemoryInfo(com.sun.jna.platform.win32.Kernel32.INSTANCE.GetCurrentProcess(), counters, counters.cb)) {
				return counters.workingSetSize.longValue();
			}
			return -1;
		}
	}

	/** Heap, non-heap (classes, code, JVM), thread stacks and direct buffers: where the memory of the process goes */
	private void appendMemoryDetails(StringBuilder html, long usedMb, long maxMb) {
		html.append(I18n.fmt("memory.heap_detail", usedMb, runtime.totalMemory() / MB, maxMb)).append("<br>");
		try {
			java.lang.management.MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
			html.append(I18n.fmt("memory.non_heap_detail", memory.getNonHeapMemoryUsage().getUsed() / MB, memory.getNonHeapMemoryUsage().getCommitted() / MB)).append("<br>");
			StringBuilder pools = new StringBuilder();
			for (java.lang.management.MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
				if (pool.getType() == java.lang.management.MemoryType.NON_HEAP && pool.getUsage() != null) {
					pools.append(pools.length() == 0 ? "" : " &middot; ").append(pool.getName()).append(": ").append(pool.getUsage().getUsed() / MB).append(" MB");
				}
			}
			if (pools.length() > 0) {
				html.append("&nbsp;&nbsp;<font size=\"-1\">").append(pools).append("</font><br>");
			}
			int threads = ManagementFactory.getThreadMXBean().getThreadCount();
			long stackKb = stackSizeKb();
			html.append(I18n.fmt("memory.thread_stacks", threads, threads * stackKb / 1024, stackKb)).append("<br>");
			for (java.lang.management.BufferPoolMXBean buffers : ManagementFactory.getPlatformMXBeans(java.lang.management.BufferPoolMXBean.class)) {
				if ("direct".equals(buffers.getName())) {
					html.append(I18n.fmt("memory.direct_buffers", buffers.getMemoryUsed() / MB)).append("<br>");
				}
			}
		} catch (Throwable e) {
			// the details are optional: the tooltip still shows the rest
		}
	}

	/** Stack size of a thread in KB: -Xss when given, else the usual default of 64 bit JVMs (1 MB) */
	private static long stackSizeKb() {
		try {
			for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
				java.util.regex.Matcher m = java.util.regex.Pattern.compile("^-Xss(\\d+)([kKmMgG]?)$").matcher(arg);
				if (m.matches()) {
					long value = Long.parseLong(m.group(1));
					String unit = m.group(2).toLowerCase();
					return unit.equals("m") ? value * 1024 : unit.equals("g") ? value * 1024 * 1024 : unit.equals("k") ? value : Math.max(1, value / 1024);
				}
			}
		} catch (Throwable e) {
			// default below
		}
		return 1024;
	}

	/** Text of the details card: the usual details without the HTML */
	private String plainDetails() {
		if (os == null) {
			return "";
		}
		long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / MB;
		String html = details(usedMb, runtime.maxMemory() / MB, processCpuLoad(), processMemoryBytes());
		// the first line is the title of the card
		String text = html.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]+>", "").replace("&nbsp;", " ").replace("&middot;", "\u00B7").trim();
		int firstLine = text.indexOf('\n');
		return firstLine >= 0 ? text.substring(firstLine + 1).trim() : text;
	}

	private static String memoryText(long bytes) {
		return bytes >= GB ? String.format("%.2f GB", bytes / (double) GB) : (bytes / MB) + " MB";
	}

	/**
	 * Total memory of this process (not only the Java heap), JDK only:
	 * Windows = working set (the RAM really used, as in the Task Manager details; JNA, already shipped); Linux = resident set
	 * from /proc; otherwise (and as fallback) heap + non-heap memory reserved by the JVM, which is a lower bound.
	 */
	private long processMemoryBytes() {
		try {
			String osName = System.getProperty("os.name", "").toLowerCase();
			if (osName.contains("win")) {
				long workingSet = WindowsMemory.workingSet();
				if (workingSet > 0) {
					return workingSet;
				}
			} else if (osName.contains("linux")) {
				for (String line : java.nio.file.Files.readAllLines(java.nio.file.Paths.get("/proc/self/status"), java.nio.charset.StandardCharsets.UTF_8)) {
					if (line.startsWith("VmRSS:")) {
						return Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024L;
					}
				}
			}
		} catch (Throwable e) {
			// falls back to the JVM view below
		}
		java.lang.management.MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		return memory.getHeapMemoryUsage().getCommitted() + memory.getNonHeapMemoryUsage().getCommitted();
	}

	private double processCpuLoad() {
		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			return ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuLoad();
		}
		return -1;
	}

	private String details(long usedMb, long maxMb, double processCpu, long processBytes) {
		StringBuilder html = new StringBuilder(I18n.t("memory.screept_resources_usage"));
		html.append(I18n.t("memory.cpu_this_application_all_cores")).append(processCpu < 0 ? "n/a" : Math.round(processCpu * 100) + "%").append("<br>");
		html.append(I18n.t("memory.process_memory_total")).append(memoryText(processBytes)).append("<br>");
		appendMemoryDetails(html, usedMb, maxMb);
		html.append(I18n.t("memory.processors")).append(os.getAvailableProcessors());
		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			com.sun.management.OperatingSystemMXBean sunOs = (com.sun.management.OperatingSystemMXBean) os;
			double systemCpu = sunOs.getSystemCpuLoad();
			html.append(I18n.t("memory.system_cpu")).append(systemCpu < 0 ? "n/a" : Math.round(systemCpu * 100) + "%");
			html.append(I18n.t("memory.system_memory")).append((sunOs.getTotalPhysicalMemorySize() - sunOs.getFreePhysicalMemorySize()) / MB)
				.append(I18n.t("memory.mb_used_of")).append(sunOs.getTotalPhysicalMemorySize() / MB).append(" MB");
		}
		return html.append("</html>").toString();
	}

	@Override
	public Dimension getMaximumSize() {
		// fixed width, full menu bar height
		return new Dimension(getPreferredSize().width, Short.MAX_VALUE);
	}
}
