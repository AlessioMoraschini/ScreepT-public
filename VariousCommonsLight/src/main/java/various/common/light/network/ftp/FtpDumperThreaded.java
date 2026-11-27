/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
