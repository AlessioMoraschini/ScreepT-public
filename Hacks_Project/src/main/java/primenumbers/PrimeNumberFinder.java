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
package primenumbers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrimeNumberFinder {
	
	public ArrayList<Long> primeList;
	private volatile int currentBiggestChecked;
	private volatile boolean searching;
	
	public PrimeNumberFinder() {
		reset();
	}
	
	public void reset() {
		searching = false;
		primeList = new ArrayList<Long>();
		primeList.add(1L);
		currentBiggestChecked = 1;
	}
	
	public void startSearch() {
		if (!searching) {
			searching = true;
			while(searching) {
				boolean divisorFound = false;
				for(long i = 2L; i < (long) Math.sqrt(currentBiggestChecked); i++) {
					if(currentBiggestChecked % i == 0) {
						divisorFound = true;
						break;
					}

				}

				if (!divisorFound) {
					primeList.add(new Long(currentBiggestChecked));
				}

				int nextToCheck = currentBiggestChecked % 2 == 0 ? 3 : 2;
				currentBiggestChecked += nextToCheck;
			}
		}
		
		searching = false;
	}
	
	public void stop() {
		if (searching) {
			searching = false;
		}
	};
	
	public String getCurrentMaxPrimeString() {
		if (primeList.size() > 0) {
			return String.valueOf(primeList.get(primeList.size() - 1));
		} else {
			return "0";
		}
	}

	public Long getHowmanyPrimesFound() {
		return new Long(primeList.size());
	}
	
	public ArrayList<Long> getPrimeList() {
		return primeList;
	}

	public int getCurrentBiggestChecked() {
		return currentBiggestChecked;
	}

	public boolean isSearching() {
		return searching;
	}

	public static void main(String[] args) throws InterruptedException {
		PrimeNumberFinder finder = new PrimeNumberFinder();
		ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "cls").inheritIO();
		
		new Thread(() -> {
			AtomicBoolean listenForPause = new AtomicBoolean();
			
			Scanner scanner = new Scanner(System.in);
			int delay = 200;
			boolean invalidInput = true;
			while(invalidInput){ 
				System.out.print("Specify a refresh interval in milliseconds:\n");
				try {
					delay = Integer.parseInt(scanner.next());
					invalidInput = false;
				} catch (Exception e) {
				}
			}
			
			scanner.close();
			
			listenForPause.set(true);
			
			while(true) {
				try {
					pb.start().waitFor();
				} catch (InterruptedException | IOException e1) {
					e1.printStackTrace();
				}
				System.out.println("Current maxPrime found: " + finder.getCurrentMaxPrimeString());
				System.out.println("Current number of primes found: " + finder.getHowmanyPrimesFound());
				System.out.println("\n\n ## Now Checking: " + finder.getCurrentBiggestChecked());
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		finder.startSearch();
		
	}
	
	public static void clearScreen() {  
	    System.out.print("\033[H\033[2J");  
	    System.out.flush();  
	}  
}
