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
	
	private UploadAction uploadAction = new UploadAction(){};
	private DownloadAction downloadAction = new DownloadAction(){};
	
	// fields
	public String hostFTP;
	public String usr;
	public String psw;
	public int port;
	public FTPSClient ftps;
	
	private boolean connected = false;
		
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
			ftps.connect(hostFTP);
			ftps.setDefaultPort(port);
			ftps.setConnectTimeout(CONNECTION_TIMEOUT);
            ftps.setKeepAlive(true);
			ftps.setControlKeepAliveTimeout(KEEP_ALIVE_DURATION_SECONDS);
            res = ftps.login(usr, psw);   
        }catch(Exception e){
			logger.error("Cannot connect to server: ", e);
			throw new ConnectException();
		}
		
		logger.info("Connection completed: " + res);
		this.ftps = ftps;
		
		connected = res;
		
		return res;
	}
	
	public boolean disconnect() {
		try {
			if (ftps.isConnected()) {
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

		ftpDirFromServerRoot = ftpDirFromServerRoot == null ? "" : ftpDirFromServerRoot;
		
		InputStream inputStream = null;
        try {
            if(!ftps.isConnected()) {
            	logger.warn("FTPS not connected: trying to reconnect...");
            	connect();
            }
            
            ftps.setFileType(FTP.BINARY_FILE_TYPE);
            ftps.enterLocalPassiveMode();
           
            if (!checkDirectoryExists(ftpDirFromServerRoot)) {//if specified directory does not exist
            	logger.warn("Directory not existing:  "+ ftpDirFromServerRoot +" -> creating it...\n");
                ftps.makeDirectory(ftpDirFromServerRoot);
                logger.warn("Directory created:  "+ ftpDirFromServerRoot + " \n");
            }
            ftps.changeWorkingDirectory(ftpDirFromServerRoot);
            
            //carico il file usando inputstream         
            inputStream = new FileInputStream(localFilePath); 
            logger.info("\n### Starting file upload: " + ftpDirFromServerRoot + "/" + remoteFileName + " ###");
            ftps.setCopyStreamListener(getUploadListener(new File(localFilePath).length(), localFilePath));
           
            boolean done = ftps.storeFile(remoteFileName, inputStream);
            
            if (done) {
            	 logger.info(" -> File uploaded correctly!\n");
            } else {
            	logger.error("Error uploading remote file!");
            }
        } catch (IOException ex) {
            logger.error("Error saving remote file: ", ex);
            return false;
        
        } finally {
        	 try {
				inputStream.close();
			} catch (Exception e) {
				logger.error("Cannot close input stream", e);
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

		       int percent = (int)(totalBytesTransferred*100/totalLength);
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

