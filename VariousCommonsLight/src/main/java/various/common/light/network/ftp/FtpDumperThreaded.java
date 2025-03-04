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

public class FtpDumperThreaded implements Runnable{

	public FtpDumper dumper;
	public String hostFTP;
	public String usr;
	public String psw;
	public int Port;
	public String FILEin; //path file input da copiare
	public String FTPout;  //nome nuovo file output su FTP
	public String folderFTPPath; //path della cartella in cui salvare
	
	public FtpDumperThreaded(String host,String user, String password,int PORT) {
		hostFTP=host;
		usr=user;
		psw=password;
		Port=PORT;
	}
	
	public void startUpload(String Inpath, String outN, String PathFD) {
		FILEin=Inpath;
		FTPout=outN;
		folderFTPPath=PathFD;
	}
	
	@Override
	public synchronized void run() {
		
		dumper = new FtpDumper(hostFTP, usr, psw, Port);
		
		synchronized (dumper) {
			dumper.uploadFile(FILEin, FTPout, folderFTPPath );
			dumper.disconnect();
		}
	}
}
