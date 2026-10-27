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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Discreet live indicator of this application CPU and RAM usage (JDK only, refreshed every second):
 * ScreepT: CPU = process load on all cores, RAM = JVM heap used / max available;
 * PC: CPU = whole system load, RAM = physical memory used / total. Details in the tooltip.
 */
public class ResourceUsageLabel extends JLabel {

	private static final long serialVersionUID = 2398750982735509831L;

	private static final int REFRESH_MS = 1000;
	private static final float FONT_DELTA = 1f;
	private static final long MB = 1024L * 1024L;
	private static final long GB = MB * 1024L;
	/** Widest expected text (titles bold, rest plain): fixed width, so the menu bar does not move at every refresh */
	private static final String WIDTH_TITLES = "ScreepTPC";
	private static final String WIDTH_VALUES = "  CPU 100% · RAM 99999 / 99999 MB      |      CPU 100% · RAM 999.9 / 999.9 GB";
	/** 0 = invisible (background color), 1 = as visible as the values */
	private static final float SEPARATOR_VISIBILITY = 0.35f;

	private final Runtime runtime = Runtime.getRuntime();
	private final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
	private final Timer timer = new Timer(REFRESH_MS, e -> refresh());

	public ResourceUsageLabel() {
		setHorizontalAlignment(SwingConstants.RIGHT);
		// vertically centered in the whole menu bar height
		setVerticalAlignment(SwingConstants.CENTER);
		setAlignmentY(CENTER_ALIGNMENT);
		setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 14));
		refresh();
		timer.start();
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

	public void stop() {
		timer.stop();
	}

	private void refresh() {
		long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / MB;
		long maxMb = runtime.maxMemory() / MB;
		double processCpu = processCpuLoad();

		// colors follow the current look and feel: titles readable, values muted, separator barely visible
		Color valuesColor = UIManager.getColor("Label.disabledForeground");
		Color titleColor = UIManager.getColor("Label.foreground");
		Color background = getParent() != null ? getParent().getBackground() : UIManager.getColor("MenuBar.background");
		String values = hex(valuesColor);
		String title = hex(titleColor);
		String separator = hex(blend(background, valuesColor, SEPARATOR_VISIBILITY));

		StringBuilder html = new StringBuilder("<html><nobr>")
				.append(titled(title, "ScreepT", values, percent(processCpu) + String.format(" · RAM %d / %d MB", usedMb, maxMb)));

		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			com.sun.management.OperatingSystemMXBean sunOs = (com.sun.management.OperatingSystemMXBean) os;
			long totalPhysical = sunOs.getTotalPhysicalMemorySize();
			long usedPhysical = totalPhysical - sunOs.getFreePhysicalMemorySize();
			html.append("&nbsp;&nbsp;&nbsp;<font color=\"").append(separator).append("\">|</font>&nbsp;&nbsp;&nbsp;")
				.append(titled(title, "PC", values, percent(sunOs.getSystemCpuLoad())
						+ String.format(" · RAM %.1f / %.1f GB", usedPhysical / (double) GB, totalPhysical / (double) GB)));
		}

		setText(html.append("</nobr></html>").toString());
		setToolTipText(details(usedMb, maxMb, processCpu));
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
		return load < 0 ? "CPU n/a" : String.format("CPU %d%%", Math.round(load * 100));
	}

	private double processCpuLoad() {
		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			return ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuLoad();
		}
		return -1;
	}

	private String details(long usedMb, long maxMb, double processCpu) {
		StringBuilder html = new StringBuilder("<html><b>ScreepT resources usage</b><br>");
		html.append("CPU (this application, all cores): ").append(processCpu < 0 ? "n/a" : Math.round(processCpu * 100) + "%").append("<br>");
		html.append("Memory (Java heap): ").append(usedMb).append(" MB used of ").append(maxMb).append(" MB available")
			.append(" (").append(runtime.totalMemory() / MB).append(" MB reserved)<br>");
		html.append("Processors: ").append(os.getAvailableProcessors());
		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			com.sun.management.OperatingSystemMXBean sunOs = (com.sun.management.OperatingSystemMXBean) os;
			double systemCpu = sunOs.getSystemCpuLoad();
			html.append("<br>System CPU: ").append(systemCpu < 0 ? "n/a" : Math.round(systemCpu * 100) + "%");
			html.append("<br>System memory: ").append((sunOs.getTotalPhysicalMemorySize() - sunOs.getFreePhysicalMemorySize()) / MB)
				.append(" MB used of ").append(sunOs.getTotalPhysicalMemorySize() / MB).append(" MB");
		}
		return html.append("</html>").toString();
	}

	@Override
	public Dimension getPreferredSize() {
		FontMetrics metrics = getFontMetrics(getFont());
		FontMetrics boldMetrics = getFontMetrics(getFont().deriveFont(java.awt.Font.BOLD));
		Insets insets = getInsets();
		return new Dimension(boldMetrics.stringWidth(WIDTH_TITLES) + metrics.stringWidth(WIDTH_VALUES) + insets.left + insets.right,
				super.getPreferredSize().height);
	}

	@Override
	public Dimension getMaximumSize() {
		// fixed width, full menu bar height
		return new Dimension(getPreferredSize().width, Short.MAX_VALUE);
	}
}
