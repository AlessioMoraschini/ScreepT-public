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
package utility.log;

import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import files.FileWorker;
import om.ConsoleColours;
import utility.string.StringWorker.EOL;
/**
 * This class is meant to provide a safe way to log either to system out or using your log4j logger, using reflection.
 * if no logger is found and safeLogger is enabled then logging messages will be logged. (by default logging is enabled)
 * 
 * @author Alessio Moraschini
 *
 */
public class SafeLogger {
	
	private static String loggerClassName = "org.apache.log4j.Logger";
	
	private static final String ERROR = "ERROR: ";
	private static final String WARN = "WARNING: ";
	private static final String INFO = "INFO: ";
	private static final String DEBUG = "DEBUG: ";

	public static final int N_MEGABYTES_MAX_LOG_SIZE = 100;
	public static final long DEFAULT_MAX_LOG_SIZE_BYTES = 1024 * 1024 * N_MEGABYTES_MAX_LOG_SIZE;
	
	private Object logger = null;
	private boolean loggerAvailable = false;
	private boolean flagWriteOnFile = false;
	private EOL logFileEol = null;
	
	private File logFile = null;
	
	private Class<?> instancerClass;
	
	private long maxLogSizeBytes = DEFAULT_MAX_LOG_SIZE_BYTES;
	
	private boolean loggerEnabled;
	
	public static String getLoggerClassName() {
		return loggerClassName;
	}

	public static void setLoggerClassName(String loggerClassName) {
		synchronized (loggerClassName) {
			SafeLogger.loggerClassName = loggerClassName;
		}
	}

	public SafeLogger(Class<?> claSS) {
		this(claSS, true, null, false);
	}

	public SafeLogger(Class<?> claSS, boolean enabled) {
		this(claSS, enabled, null, false);
	}

	public SafeLogger(Class<?> claSS, boolean enabled, File logFile, boolean flagWriteOnFile) {
		reset(claSS);
		this.instancerClass = claSS;
		this.loggerEnabled = enabled;
		this.logFile = logFile;
		this.flagWriteOnFile = flagWriteOnFile;
		logFileEol = null;
		checkAndInitLogFile();
	}
	
	public void reset(Class<?> claSS) {
		try {
			Class<?> loggerClass = Class.forName(loggerClassName);
			Method instancer = loggerClass.getMethod("getLogger", Class.class);
			instancer.setAccessible(true);
			this.logger = instancer.invoke(logger, claSS);
			
			loggerAvailable = true;
		} catch (Throwable throwable) {
			out.println("Cannot initialize logger!");
			loggerAvailable = false;
		}
		
	}
	
	public void invokeLoggerMethod(String method, String msg) {
		try {
			
			Method instancer = logger.getClass().getMethod(method, Object.class);
			instancer.setAccessible(true);
			instancer.invoke(logger, msg);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			loggerAvailable = false;
		}
	}

	public void invokeLoggerMethod(String method, String msg, Throwable throwable) {
		try {
			
			Method instancer = logger.getClass().getMethod(method, Object.class, Throwable.class);
			instancer.setAccessible(true);
			instancer.invoke(logger, msg, throwable);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			loggerAvailable = false;
		}
	}
	
	public void info(String message) {
		if (loggerEnabled) {
			if (loggerAvailable) {
				invokeLoggerMethod("info", message);
			} else {
				String err = classPreamble() + INFO + message;
				out.println(err);
				if(canWriteOnFile(true)) 
					writeOnLogFile(err);
			} 
		}
	}

	public void warn(String message) {
		if (loggerEnabled) {
			if (loggerAvailable) {
				invokeLoggerMethod("warn", message);
			} else {
				String err = classPreamble() + WARN + message;
				out.println(err);
				if(canWriteOnFile(true)) 
					writeOnLogFile(err);
			} 
		}
	}
	
	public void warn(String message, Throwable throwable) {
		if (loggerEnabled) {
			if (loggerAvailable) {
				invokeLoggerMethod("warn", message, throwable);
			} else {
				String err = classPreamble() + ConsoleColours.ANSI_RED + WARN + message + throwable.getClass().getName() + getLinedThrowable(throwable, ConsoleColours.ANSI_YELLOW);
				out.println(err);
				if(canWriteOnFile(true)) 
					writeOnLogFile(err);
			} 
			
		}
	}

	public void debug(Object obj) {
		if (loggerEnabled) {
			if (loggerAvailable) {
				invokeLoggerMethod("debug", obj.toString());
			} else {
				String err = classPreamble() + DEBUG + obj.toString();
				out.println(err);
				if(canWriteOnFile(true)) 
					writeOnLogFile(err);
			} 
		}
	}

	public void debug(String message) {
		if (loggerEnabled) {
			if (loggerAvailable) {
				invokeLoggerMethod("debug", message);
			} else {
				String err = classPreamble() + DEBUG + message;
				out.println(err);
				if(canWriteOnFile(true)) 
					writeOnLogFile(err);
			} 
		}
	}

	public void error(String message, Throwable throwable) {
		if (loggerEnabled) {
			if (loggerAvailable) {
				invokeLoggerMethod("error", message, throwable);
			} else {
				String err = classPreamble() + ConsoleColours.ANSI_RED + ERROR + message + throwable.getClass().getName()
						+ getLinedThrowable(throwable, ConsoleColours.ANSI_YELLOW);
				out.println(err);
				if(canWriteOnFile(true)) 
					writeOnLogFile(err);
			} 
		}
	}

	public void error(String message) {
		if (loggerEnabled) {
			if (loggerAvailable) {
				invokeLoggerMethod("error", message);
			} else {
				String err = classPreamble() + ERROR + message;
				out.println(err);
				if(canWriteOnFile(true)) 
					writeOnLogFile(err);
			} 
		}
	}

	public void error(Throwable throwable) {
		if (loggerEnabled) {
			if (loggerAvailable) {
				invokeLoggerMethod("error", throwable.getClass().getCanonicalName(), throwable);
			} else {
				String err = classPreamble() + ERROR + getLinedThrowable(throwable, ConsoleColours.ANSI_RED);
				out.println(err);
				if(canWriteOnFile(true)) 
					writeOnLogFile(err);
			} 
		}
	}

	public boolean isLoggerEnabled() {
		return loggerEnabled;
	}

	public void setLoggerEnabled(boolean loggerEnabled) {
		this.loggerEnabled = loggerEnabled;
	}

	public Object getLogger() {
		return logger;
	}

	public void setLogger(Object logger) {
		this.logger = logger;
	}

	public boolean isLoggerAvailable() {
		return loggerAvailable;
	}

	public void setLoggerAvailable(boolean loggerAvailable) {
		this.loggerAvailable = loggerAvailable;
	}

	public static String getLoggerclassname() {
		return loggerClassName;
	}
	
	private String getLinedThrowable(Throwable e, String consoleColour) {
		StringBuilder builder = new StringBuilder();
		String colourPrefix = consoleColour != null? consoleColour : "";
		
		builder.append(colourPrefix);
		builder.append("\n:" + e.getMessage() + "\n");
		for(StackTraceElement element : e.getStackTrace()) {
			builder.append("\n   ")
				.append(element.getClassName())
				.append(" : ").append(element.getMethodName())
				.append(" : Line N°").append(element.getLineNumber())
				.append(" : File=").append(element.getFileName());
		}
		builder.append(ConsoleColours.ANSI_RESET);
		
		
		return builder.toString();
	}

	public boolean isFlagWriteOnFile() {
		return flagWriteOnFile;
	}

	public void setFlagWriteOnFile(boolean flagWriteOnFile) {
		this.flagWriteOnFile = flagWriteOnFile;
	}
	
	public boolean canWriteOnFile(boolean considerFlag) {
		
		boolean commonCond = logFile != null &&
				logFile.isFile() &&
				logFile.exists() &&
				logFile.canWrite();
		
		if(considerFlag)
			return flagWriteOnFile && commonCond;
					
		else
			return commonCond;
	}
	
	private void writeOnLogFile(String string) {
		if(canWriteOnFile(true)) {
			if(maxLogSizeReached())
				createAndUseNewLogFile();
			
			FileWorker.appendStringToFile((logFileEol != null ? logFileEol.eol : EOL.LF.eol) + string, logFile, true, logFileEol);
		}
	}
	
	private String getInstancerClassName() {
		return instancerClass != null ? instancerClass.getCanonicalName() : "";
	}
	
	private String wrap(String before, String toWrap, String after) {
		return before + toWrap + after;
	}
	
	private String classPreamble() {
		return wrap(" [", getInstancerClassName(), "] ");
	}
	
	private void checkAndInitLogFile() {
		if(logFile != null && !logFile.exists())
			FileWorker.appendStringToFile("", logFile, true, logFileEol);
	}
	
	private boolean maxLogSizeReached() {
		boolean commonCond = logFile != null &&
				logFile.isFile() &&
				logFile.exists();
		
		if(!commonCond)
			return false;
		
		else
			return logFile.length() >= maxLogSizeBytes;
	}
	
	public long getMaxLogSizeBytes() {
		return maxLogSizeBytes;
	}

	public void setMaxLogSizeBytes(long maxLogSizeBytes) {
		this.maxLogSizeBytes = maxLogSizeBytes;
	}

	private boolean createAndUseNewLogFile() {
		
		synchronized (logFile) {
			String oldLog = logFile.getAbsolutePath();
			File newFile = FileWorker.renameJavaObjFile(logFile);
			try {
				newFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				newFile = new File(oldLog);
				return false;
			}
			logFile = newFile;
		}
		
		return true;
	}

	public File getLogFile() {
		return logFile != null ? new File(logFile.getAbsolutePath()) : null;
	}

	public void setLogFile(File logFile) {
		synchronized (logFile) {
			this.logFile = logFile;
			checkAndInitLogFile();
		}
	}

	public EOL getLogFileEol() {
		return logFileEol;
	}

	public void setLogFileEol(EOL logFileEol) {
		this.logFileEol = logFileEol;
	}
	
}
