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
package various.common.light.network.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamAdapter;

import various.common.light.files.FileWorker;
import various.common.light.network.progress.DownloadAction;
import various.common.light.network.progress.UploadAction;
import various.common.light.utility.log.SafeLogger;

public class FtpDumper {

	public static SafeLogger logger = new SafeLogger(FtpDumper.class);
	private static final String protocol = "TLS";
	public static int CONNECTION_TIMEOUT = 30000;
	public static int KEEP_ALIVE_DURATION_SECONDS = 25;
	/** Max wait (ms) for any read on control/data channel: a stalled server fails instead of blocking forever. */
	public static int READ_TIMEOUT = 60000;
	public static int MAX_UPLOAD_ATTEMPTS = 3;
	private static final long RETRY_PAUSE_MS = 2000;
	/** A port that is not the standard FTP one is tried this long before falling back to 21 */
	private static final int ALTERNATIVE_PORT_PROBE_MS = 8000;
	
	private UploadAction uploadAction = new UploadAction(){};
	private DownloadAction downloadAction = new DownloadAction(){};
	
	// fields
	public String hostFTP;
	public String usr;
	public String psw;
	public int port;
	public FTPSClient ftps;
	
	private boolean connected = false;
	private String lastError = null;
	/** Port that really answered (-1 until the first connection) */
	private int workingPort = -1;
	/** Folder the server puts the user in after the login: relative folders start from here */
	private String homeDir = "/";
		
	//costruttori
	 
	public FtpDumper(String host, String user, String password, int PORT) {
		this.hostFTP = host;
		this.usr = user;
		this.psw = password;
		this.port = PORT;
		
		try {
			connect();
		} catch (ConnectException e) {
			ftps = null;
		}
	}
	
	//metodi
	
	@Deprecated
	public synchronized static boolean checkFTP(String server, String usr, String inskey, int port) {

		FTPSClient ftps = new FTPSClient(protocol);
		boolean res=false;
		try {
			logger.info("Checking FTPS connection to:  "+ server +" :: USER: " + usr + " :: PORT: " + port);
			ftps.setDefaultPort(port);
			ftps.connect(server);
            res= ftps.login(usr, inskey);   
        }catch(Exception e){
			return false;
		}
		
		try {
            if (ftps.isConnected()) {
                ftps.logout();
                ftps.disconnect();
            }
        } catch (IOException ex) {
        	logger.error("\n**** ERROR: CANNOT DISCONNECT FROM FTPS SERVER ****\n", ex);
        }
		
		return res;
	}
	
	public boolean isConnected() {
		return connected;
	}

	public synchronized boolean checkNewConnection() {
		
		FTPSClient ftps = new FTPSClient(protocol);
		boolean res=false;
		try {
			logger.info("Checking FTPS connection to:  "+ hostFTP +" :: USER: " + usr + " :: PORT: " + port);
			ftps.setDefaultPort(port);
			ftps.connect(hostFTP);
			res= ftps.login(usr, psw);   
		}catch(Exception e){
			return false;
		}
		
		connected = res;
		return res;
	}
	
	public boolean connect() throws ConnectException {
		FTPSClient ftps = new FTPSClient(protocol);
		boolean res = false;
		try {
			logger.info("Connecting to:  "+ hostFTP +" :: USER: " + usr + " :: PORT: " + port);
			// Timeouts must be set BEFORE connect(), otherwise an unreachable server blocks forever
			ftps.setConnectTimeout(CONNECTION_TIMEOUT);
			ftps.setDefaultTimeout(READ_TIMEOUT);
			openControlConnection(ftps);
			ftps.setSoTimeout(READ_TIMEOUT);
			ftps.setDataTimeout(READ_TIMEOUT);
            ftps.setKeepAlive(true);
			ftps.setControlKeepAliveTimeout(KEEP_ALIVE_DURATION_SECONDS);
            res = ftps.login(usr, psw);
        }catch(Exception e){
			logger.error("Cannot connect to server: ", e);
			lastError = "Cannot connect to " + hostFTP + ":" + (workingPort > 0 ? workingPort : port) + " (" + describe(e) + ")";
			try { ftps.disconnect(); } catch (Exception ignored) { }
			ConnectException ce = new ConnectException(lastError);
			ce.initCause(e);
			throw ce;
		}

		if (!res) {
			lastError = "Login refused by " + hostFTP + " (wrong user or password?)";
			try { ftps.disconnect(); } catch (Exception ignored) { }
			this.ftps = ftps;
			connected = false;
			return false;
		}
		lastError = null;
		try {
			String pwd = ftps.printWorkingDirectory();
			homeDir = pwd == null || pwd.isEmpty() ? "/" : pwd;
		} catch (IOException e) {
			homeDir = "/";
		}
		
		logger.info("Connection completed: " + res);
		this.ftps = ftps;
		
		connected = res;
		
		return res;
	}
	
	/**
	 * Opens the control connection on the configured port. FTPS runs on 21: a configured port that does not answer (22, for
	 * instance, is SFTP) is tried briefly and then replaced by 21, and the port that worked is remembered.
	 */
	private void openControlConnection(FTPSClient client) throws IOException {
		int[] candidates;
		if (workingPort > 0) {
			candidates = new int[] { workingPort };
		} else if (port <= 0 || port == 21) {
			candidates = new int[] { 21 };
		} else {
			candidates = new int[] { port, 21 };
		}
		IOException last = null;
		for (int i = 0; i < candidates.length; i++) {
			boolean hasAlternative = i < candidates.length - 1;
			try {
				client.setConnectTimeout(hasAlternative ? Math.min(CONNECTION_TIMEOUT, ALTERNATIVE_PORT_PROBE_MS) : CONNECTION_TIMEOUT);
				client.connect(hostFTP, candidates[i]);
				workingPort = candidates[i];
				if (workingPort != port) {
					logger.warn("Port " + port + " does not answer: connected on the standard FTP port " + workingPort + " (fix the PORT of the configuration)");
				}
				return;
			} catch (IOException e) {
				last = e;
				try { client.disconnect(); } catch (Exception ignored) { }
				if (hasAlternative) {
					logger.warn("Port " + candidates[i] + " not reachable (" + describe(e) + "): trying the standard FTP port 21");
				}
			}
		}
		throw last;
	}

	/** Human readable reason of the last failed connect/upload, or null if none. */
	public String getLastError() {
		return lastError;
	}

	private static String describe(Throwable e) {
		String msg = e.getMessage();
		return e.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
	}

	public boolean disconnect() {
		try {
			if (ftps != null && ftps.isConnected()) {
				ftps.logout();
				ftps.disconnect();
			}
			logger.info("Disconnected from FTPS server");
			connected = false;
			return true;
		} catch (IOException ex) {
			logger.error("\n**** ERROR: CANNOT DISCONNECT FROM FTPS SERVER ****\n", ex);
			connected = true;
			return false;
		}
	}
	
	
	/**@author Alessio Moraschini
     * 
     *  Questo metodo serve per salvare un file "filename" nella
     *  cartella folderpathname presente nel server ftp, usando i dati specificati
     *  alla creazione dell'oggetto Dumper(host,usr,pssw,port)
     *  Il file viene salvato con il nome specificato in "ftpname"
     * 
     * @return 0 se tutto � andato bene
     * @return 1 se c'� stato un errore
     */  
	public synchronized boolean uploadFile(String localFilePath, String remoteFileName, String ftpDirFromServerRoot) {

		for (int attempt = 1; attempt <= MAX_UPLOAD_ATTEMPTS; attempt++) {
			if (uploadFileOnce(localFilePath, remoteFileName, ftpDirFromServerRoot)) {
				lastError = null;
				return true;
			}
			if (attempt < MAX_UPLOAD_ATTEMPTS) {
				logger.warn("Upload failed (attempt " + attempt + "/" + MAX_UPLOAD_ATTEMPTS + "): " + lastError + " -> reconnecting and retrying...");
				try {
					if (ftps != null && ftps.isConnected()) {
						ftps.disconnect(); // channel may be in an unknown state: drop it, connect() builds a new one
					}
				} catch (Exception ignored) { }
				connected = false;
				try {
					Thread.sleep(RETRY_PAUSE_MS);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		logger.error("Upload definitively failed: " + lastError);
		return false;
	}

	private boolean uploadFileOnce(String localFilePath, String remoteFileName, String ftpDirFromServerRoot) {

		ftpDirFromServerRoot = ftpDirFromServerRoot == null ? "" : ftpDirFromServerRoot;

		InputStream inputStream = null;
        try {
            if(ftps == null || !ftps.isConnected()) {
            	logger.warn("FTPS not connected: trying to reconnect...");
            	if (!connect()) {
            		return false; // lastError already set by connect()
            	}
            }

            ftps.setFileType(FTP.BINARY_FILE_TYPE);
            ftps.enterLocalPassiveMode();

            if (!enterRemoteDirectory(ftpDirFromServerRoot)) {
            	lastError = "Cannot enter or create the folder " + ftpDirFromServerRoot + " on the server: " + ftps.getReplyString();
            	logger.error(lastError);
            	return false;
            }
            
            //carico il file usando inputstream         
            inputStream = new FileInputStream(localFilePath); 
            logger.info("\n### Starting file upload: " + ftpDirFromServerRoot + "/" + remoteFileName + " ###");
            final java.util.concurrent.atomic.AtomicLong lastActivity = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
            final CopyStreamAdapter progress = getUploadListener(new File(localFilePath).length(), localFilePath);
            ftps.setCopyStreamListener(new CopyStreamAdapter() {
            	@Override
            	public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
            		lastActivity.set(System.currentTimeMillis());
            		progress.bytesTransferred(totalBytesTransferred, bytesTransferred, streamSize);
            	}
            });
           
            boolean done = storeWithWatchdog(remoteFileName, inputStream, lastActivity);
            
            if (done) {
            	 logger.info(" -> File uploaded correctly!\n");
            } else {
            	lastError = "Server rejected upload of " + remoteFileName + ": " + ftps.getReplyString();
            	logger.error("Error uploading remote file! " + lastError);
            }
            return done;
        } catch (ConnectException ex) {
            return false; // connect() already stored the reason in lastError
        } catch (IOException ex) {
            logger.error("Error saving remote file: ", ex);
            lastError = "Upload of " + remoteFileName + " interrupted (" + describe(ex) + ")";
            return false;

        } finally {
        	 try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				logger.error("Cannot close input stream", e);
			}
        }
	}	
	
	/**
	 * storeFile with a stall detector. The data timeout only covers reads: when the server stops reading, the write of the
	 * upload blocks for ever. The transfer runs on its own thread; if no byte moves for READ_TIMEOUT ms the connection is
	 * dropped and the call fails, so the caller can retry on a new connection (the stuck thread ends with its socket).
	 */
	private boolean storeWithWatchdog(final String remoteFileName, final InputStream in, final java.util.concurrent.atomic.AtomicLong lastActivity) throws IOException {
		final FTPSClient client = ftps;
		java.util.concurrent.FutureTask<Boolean> task = new java.util.concurrent.FutureTask<Boolean>(new java.util.concurrent.Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return client.storeFile(remoteFileName, in);
			}
		});
		Thread worker = new Thread(task, "FtpDumper-upload");
		worker.setDaemon(true);
		worker.start();
		try {
			while (true) {
				try {
					return task.get(1, java.util.concurrent.TimeUnit.SECONDS);
				} catch (java.util.concurrent.TimeoutException e) {
					if (System.currentTimeMillis() - lastActivity.get() > READ_TIMEOUT) {
						try {
							client.disconnect();
						} catch (Exception ignored) { }
						worker.interrupt();
						throw new java.net.SocketTimeoutException("no data moved for " + (READ_TIMEOUT / 1000) + " s");
					}
				}
			}
		} catch (java.util.concurrent.ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) {
				throw (IOException) cause;
			}
			throw new IOException(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("upload interrupted");
		}
	}

	/**
	 * Makes the folder the working directory of the connection, creating the missing levels one at a time (a plain MKD of
	 * "a/b" fails when "a" does not exist, and the file would silently end up in the wrong place).
	 * @return false when the folder cannot be entered or created
	 */
	private boolean enterRemoteDirectory(String path) throws IOException {
		String clean = path == null ? "" : path.trim().replace('\\', '/');
		if (clean.isEmpty()) {
			return true;
		}
		if (!ftps.changeWorkingDirectory(clean.startsWith("/") ? "/" : homeDir)) {
			return false;
		}
		for (String level : clean.split("/")) {
			if (level.isEmpty() || level.equals(".")) {
				continue;
			}
			if (ftps.changeWorkingDirectory(level)) {
				continue;
			}
			logger.warn("Directory not existing:  " + level + " -> creating it...");
			if (!ftps.makeDirectory(level) || !ftps.changeWorkingDirectory(level)) {
				return false;
			}
		}
		return true;
	}

	/**
     * Determines whether a directory exists or not
     * @param dirPath
     * @return true if exists, false otherwise
     * @throws IOException thrown if any I/O error occurred.
     */
	public boolean checkDirectoryExists(String dirPath) throws IOException {
    	if(!ftps.isConnected()) {
        	logger.warn("FTPS not connected: trying to reconnect...");
        	connect();
        }
        
        ftps.setFileType(FTP.BINARY_FILE_TYPE);
        ftps.enterLocalPassiveMode();
        ftps.changeWorkingDirectory(dirPath);

        logger.info("Checking directory:  "+ dirPath + " ...\n");
        int returnCode = ftps.getReplyCode();
       
        if (returnCode == 550) {
            return false;
        }
        return true;
    }
 
    /**
     * Determines whether a file exists or not
     * @param filePath
     * @return true if exists, false otherwise
     * @throws IOException thrown if any I/O error occurred.
     */
    public boolean checkFileExists(String FTPdir, String RemoteName) throws IOException {
    	
    	if(!checkDirectoryExists(FTPdir)) {
    		throw new IOException("Ftp directory: " + FTPdir + " does not exist!");
    	}
    	
    	if(!ftps.isConnected()) {
        	logger.warn("FTPS not connected: trying to reconnect...");
        	connect();
        }
        
        ftps.setFileType(FTP.BINARY_FILE_TYPE);
        ftps.enterLocalPassiveMode();
        ftps.changeWorkingDirectory(FTPdir);
        
        InputStream inputStream = ftps.retrieveFileStream(RemoteName);
        int returnCode = ftps.getReplyCode();
        if (inputStream == null) {
            return false;
        } else {
        	try {
        		if (inputStream != null) {
					inputStream.close();
				}
        	} catch(Exception e){
        	}
        	return returnCode != 550;
        }
    }
	
	public boolean deleteFile(String FTPdir, String RemoteName) throws IOException {
		
		if(!checkDirectoryExists(FTPdir)) {
    		throw new IOException("Ftp directory: " + FTPdir + " does not exist!");
    	}
		
    	if(!ftps.isConnected()) {
    		logger.warn("FTPS not connected: trying to reconnect...");
        	connect();
        }
        ftps.enterLocalPassiveMode();
        ftps.setFileType(FTP.BINARY_FILE_TYPE);
        ftps.changeWorkingDirectory(FTPdir);
        
        return ftps.deleteFile(RemoteName);
    }
	
	/**
	 * This method saves remote files denoted by name only, that are inside the remote folder specified at FTPdir (folder path from server root)
	 * @param FTPdir the remote folder in which the file is contained
	 * @param RemoteName the name of the remote file to download
	 * @param localOutputPath the local download target path, including the file name (if not existing it will be created)
	 * @return
	 * @throws IOException 
	 */
	public File downloadFile(String FTPdir, String RemoteName, String localOutputPath, boolean overwriteLocal) throws IOException {
		
		if(!checkDirectoryExists(FTPdir)) {
    		throw new IOException("Ftp directory: " + FTPdir + " does not exist!");
    	}
		
		OutputStream outputStream = null;
		File localFile = new File(localOutputPath); 
        
		try {
        	
        	if(!ftps.isConnected()) {
        		logger.warn("FTPS not connected: trying to reconnect...");
            	connect();
            }
            ftps.enterLocalPassiveMode();
            ftps.setFileType(FTP.BINARY_FILE_TYPE);
            ftps.changeWorkingDirectory(FTPdir);

            if(!overwriteLocal) {
            	localFile = FileWorker.renameJavaObjFile(localFile);
            }
            if(!localFile.exists()) {
            	localFile.getParentFile().mkdirs();
            	localFile.createNewFile();
            }
            
            outputStream = new FileOutputStream(localFile);   
            
            String remotePath = FTPdir+"/"+RemoteName;
            FTPFile remoteFile = ftps.mlistFile(remotePath);
            
            logger.info("\n### Downloading file: "+remotePath+" :: remote file size = " + remoteFile.getSize() + "###");
            ftps.setCopyStreamListener(getDownloadListener(remoteFile.getSize(), remotePath));
            
            boolean done = ftps.retrieveFile(RemoteName, outputStream); 
            
            if (done) {
				logger.info("\n### File downloaded! (" + remotePath + ") ###");
			} else {
				logger.error("Error downloading remote file!");
			}
			return localFile;
            
        } catch (IOException ex) {
        	logger.error("Error downloading remote file!", ex);
        	return localFile;
        } finally {
        	
            try {
				outputStream.close();
			} catch (Exception e) {
				logger.error("Cannot close output stream: ", e);
			}
        }		
	}
	
	/**
	 * This method returns an array containing the name-only of files found inside FTP remote directory 
	 * @param FTPdir
	 * @return
	 */
	public String[] getRemoteFilesInFolder(String FTPdir){
		String [] alldumps;
        try {
        	if(!ftps.isConnected()) {
        		logger.warn("FTPS not connected: trying to reconnect...");
            	connect();
            }
            ftps.enterLocalPassiveMode();
            ftps.setFileType(FTP.BINARY_FILE_TYPE);
            ftps.changeWorkingDirectory(FTPdir);
            int returnCodeA = ftps.getReplyCode();
            if (returnCodeA == 550) {//se la directory specificata in ini non esiste
            	return null;            
            }        
            alldumps = ftps.listNames(FTPdir);
            
            return alldumps;
        } catch (IOException ex) {
             return null;
        } 	
	}
	
	private CopyStreamAdapter getUploadListener(long totalLength, String fileName) {
		return new CopyStreamAdapter() {

		    @Override
		    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
		       //this method will be called everytime some bytes are transferred

		       int percent = totalLength <= 0 ? 100 : (int)(totalBytesTransferred*100/totalLength);
		       getUploadAction().doAfterUploadProgress(percent, fileName);
		    }

		 };
	}

	private CopyStreamAdapter getDownloadListener(long totalLength, String fileName) {
		return new CopyStreamAdapter() {
			
			@Override
			public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
				//this method will be called everytime some bytes are transferred
				
				int percent = (int)(totalBytesTransferred*100/totalLength);
				getDownloadAction().doAfterDownloadProgress(percent, fileName);
			}
			
		};
	}

	public static int getCONNECTION_TIMEOUT() {
		return CONNECTION_TIMEOUT;
	}

	public static void setCONNECTION_TIMEOUT(int cONNECTION_TIMEOUT) {
		CONNECTION_TIMEOUT = cONNECTION_TIMEOUT;
	}
	
	public UploadAction getUploadAction() {
		return uploadAction;
	}

	public void setUploadAction(UploadAction uploadAction) {
		this.uploadAction = uploadAction;
	}

	public DownloadAction getDownloadAction() {
		return downloadAction;
	}

	public void setDownloadAction(DownloadAction downloadAction) {
		this.downloadAction = downloadAction;
	}
	
}

