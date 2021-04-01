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
package various.common.light.network.utils;

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Arrays;

import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.UpdateProgressPanel;
import various.common.light.network.http.HttpHelper;
import various.common.light.om.exception.ProgressBarInterruptedException;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.string.StringWorker;

public class DownloadUtils {

	public static String tempFolderPath = System.getProperty("user.home")+"/ScreepT_temp_files/COMMON_DATA/Downloads/";

	static {
		File file = new File(tempFolderPath);
		if(!file.exists()) {
			file.mkdirs();
		}
	}

	public static SafeLogger logger = new SafeLogger(DownloadUtils.class);

	public static int CONNECTION_TIMEOUT_MS = 30000;

	public static String downloadFileContent(boolean silent, URL url, UpdateProgressPanel progressUpdater) throws Exception {
		File tempFile = FileVarious.uniqueJavaObjFile(new File(tempFolderPath + "/temp"));
		File downloaded = downloadFile(silent, url, tempFile.getAbsolutePath(), progressUpdater);
		String output = FileWorker.readFileAsString(downloaded);

		downloaded.delete();

		return output;
	}

	public static File downloadFile(boolean silent, URL url, String localPath, UpdateProgressPanel progressUpdater) throws Exception, ProgressBarInterruptedException{
		int fileSize = HttpHelper.getFileSize(url);
		logger.info("Opening url stream...");
		BufferedInputStream in = null;
		FileOutputStream fileOutputStream = null;

		try {
			if(!silent) {
				String title = "Downloading update file: " + new File(url.getFile()).getName();
				progressUpdater = UpdateProgressPanel.fastCheckedInstance(progressUpdater, title, new Dimension(650, 120));
			}

			in = new BufferedInputStream(HttpHelper.urlProxyStream(url));
			fileOutputStream = new FileOutputStream(localPath);
			int bufferSIze = 1024;
			byte dataBuffer[] = new byte[bufferSIze];
			int bytesRead;
			int cycles = 1;

			while ((bytesRead = in.read(dataBuffer, 0, bufferSIze)) != -1) {
			    fileOutputStream.write(dataBuffer, 0, bytesRead);
			    int percDownloaded = getPerc(bufferSIze*cycles, fileSize);
			    percDownloaded = (percDownloaded>99) ? 99 : percDownloaded;
			    ifStoppedByUserThrowException(progressUpdater);
			    String msg = "Downloading " + new File(url.getFile()).getName() + " ("+getPercSizesString(bufferSIze*cycles, fileSize)+")";
				UpdateProgressPanel.updateProgress(msg, percDownloaded, progressUpdater, false);
			    cycles ++;
			}

		} catch (Exception e) {
			if (e instanceof ProgressBarInterruptedException) {
				throw e;
			}
		} finally {

			try {
				logger.info("Closing url stream...");
				in.close();
				fileOutputStream.close();
				UpdateProgressPanel.updateProgress("Download Completed!", 100, progressUpdater, false);
				logger.info("Streams closed!");
			} catch (Exception e) {
				logger.error("An error occurred while closing streams after download" + Arrays.deepToString(e.getStackTrace()));
			}

			if (progressUpdater != null) {
				progressUpdater.forceClose();
			}
		}

		return new File(localPath);
	}

	/**
	 * check if a given version (which can contain letters or underscores) respect
	 * min and max values : if less than max and more(or equal) to min then is OK.
	 * @param version in format n.x.y.z. eccc....
	 * @return
	 */
	public static boolean checkVersionBetweenRange(String version, String max, String min) {

		// remove non dots chars and transform undescores to dots
		String noLettersDotsOnly = StringWorker.keepOnlyDotsAndDigits(version);

		boolean lessThanMax;
		boolean moreThanMin;
		try {
			lessThanMax = StringWorker.compareVersions(noLettersDotsOnly, max) < 0;
			moreThanMin = StringWorker.compareVersions(noLettersDotsOnly, min) >= 0;
		} catch (IllegalArgumentException e) {
			logger.error("Error comparing version: illegal format of ["+version+"]" + Arrays.deepToString(e.getStackTrace()));
			return false;
		}

		return (lessThanMax && moreThanMin);
	}

	public static boolean compareNewVersion(String newVersion, String currentVersion) {

		// remove non dots chars and transform undescores to dots
		String noLettersDotsOnlyNew = StringWorker.keepOnlyDotsAndDigits(newVersion);
		String noLettersDotsOnlyCurr = StringWorker.keepOnlyDotsAndDigits(currentVersion);

		boolean newAvailable = false;
		try {
			newAvailable = StringWorker.compareVersions(noLettersDotsOnlyCurr, noLettersDotsOnlyNew) < 0;
		} catch (IllegalArgumentException e) {
			logger.error("Error comparing version: illegal format of A["+noLettersDotsOnlyCurr+"]"
					+ " or B["+noLettersDotsOnlyNew+"]" + Arrays.deepToString(e.getStackTrace()));
			return false;
		}

		return (newAvailable);
	}

	public static int getPerc(int val, long tot) {
		double perc = ((double)val / (double)tot) * 100;
		return (int)perc;
	}

	public static int getPerc(long val, long tot) {
		float perc = ((float)val / (float)tot) * 100;
		return (int)perc;
	}

	public static String getPercSizesString(int val, long tot) {
		float perc = ((float)val / (float)tot) * 100;

		String totalStr = ConversionUtils.coolFileSize(tot);
		String downloadedStr = ConversionUtils.coolFileSize((long)(tot * perc) / 100);

		return downloadedStr + "/" + totalStr;
	}

	public static void ifStoppedByUserThrowException(UpdateProgressPanel progressMonitor) throws ProgressBarInterruptedException {
		if(progressMonitor != null && progressMonitor.closable.get() && progressMonitor.stopProcess.get()) {
			progressMonitor.forceCloseNExceptionSignal();
		}
	}

}
