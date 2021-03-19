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
package runtime;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;

import javax.management.MBeanServerConnection;

import utility.log.SafeLogger;
import utility.manipulation.ConversionUtils;

public class RuntimeUtil {
	
	private static SafeLogger logger = new SafeLogger(RuntimeUtil.class);

	private RuntimeUtil() {
		// No Init
	}
	
	public static long getMaxMemory() {
		return Runtime.getRuntime().maxMemory();
	}

	public static long getTotalMemory() {
		return Runtime.getRuntime().totalMemory();
	}

	public static long getFreeMemory() {
		return Runtime.getRuntime().freeMemory();
	}

	public static long getUsedMemory() {
		return getMaxMemory() - getFreeMemory();
	}

	public static String getTotalMemoryString() {
		return ConversionUtils.coolFileSize(getTotalMemory());
	}

	public static String getFreeMemoryString() {
		return ConversionUtils.coolFileSize(getFreeMemory());
	}

	public static String getUsedMemoryString() {
		return ConversionUtils.coolFileSize(getUsedMemory());
	}
	
	public static String getPercMemoryOnTotalSystem(long totalSystemMemory) {
		return ConversionUtils.getPercentString(getUsedMemory(), totalSystemMemory);
	}

	public static String getMaxMemoryString() {
		return ConversionUtils.coolFileSize(getMaxMemory());
	}

	public static double getPercentageUsed() {
		return ((double) getUsedMemory() / getMaxMemory()) * 100;
	}

	public static String getPercentageUsedFormatted() {
		String percentage = String.valueOf(getPercentageUsed());
		
		percentage = percentage.length() > 5 ? percentage.substring(0,5) : percentage;
		
		return percentage + "%";
	}

	public static String getHostAdress() {
		try {
			java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
			return addr.getHostAddress();
		} catch (UnknownHostException e) {
			// looks like a strange machine
			System.out.println(e.getMessage());
		}
		return "";
	}
	
	public static String getHostName() {
		try {
			java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
			return addr.getHostName();
		} catch (UnknownHostException e) {
			// looks like a strange machine
			System.out.println(e.getMessage());
		}
		return "";
	}
	
	@SuppressWarnings("restriction")
	public static double getCurrentProcCpuUsage() throws IOException {
		try {
			MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();

			com.sun.management.OperatingSystemMXBean osMBean = ManagementFactory.newPlatformMXBeanProxy(
					mbsc,
					ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
					com.sun.management.OperatingSystemMXBean.class);

			double percent = osMBean.getProcessCpuLoad() * 100;
			
			if (percent <= 0) {
				long nanoBefore = System.nanoTime();
				long cpuBefore = osMBean.getProcessCpuTime();
				long cpuAfter = osMBean.getProcessCpuTime();
				long nanoAfter = System.nanoTime();
				if (nanoAfter > nanoBefore)
					percent = ((cpuAfter - cpuBefore) * 100L) / (nanoAfter - nanoBefore);
				else
					percent = 0;
			}
			
			return percent;
		} catch (Throwable e) {
			logger.error("Cannot retrieve CpuUsage", e);
			return 0d;
		}
	}
	
	public static String getCurrentProcCpuUsageString() throws IOException {
		String percentage = String.valueOf(getCurrentProcCpuUsage());
		
		percentage = percentage.length() > 5 ? percentage.substring(0,5) : percentage;
		
		return percentage + "%";
	}

	public static String getSystemInformation() {
		return String.format("SystemInfo=Current heap:%s; Used:%s; Free:%s; Maximum Heap:%s; Percentage Used:%s", getTotalMemoryString(), getUsedMemoryString(),
				getFreeMemoryString(), getMaxMemoryString(), getPercentageUsedFormatted());
	}
}