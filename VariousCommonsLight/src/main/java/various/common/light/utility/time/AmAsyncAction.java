/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : ${project.main.url}
 */
package various.common.light.utility.time;

import java.util.UUID;

public class AmAsyncAction {
	
	public volatile boolean debugEnabled = false;
	
	private String id = "";

	private volatile boolean running;
	private volatile boolean paused;
	private volatile boolean executed;
	private volatile boolean aborted;
	private volatile long msRemaining;
	private volatile long msLength;
	private volatile int sleepInterval;
	private Runnable action;
	
	public AmAsyncAction(Runnable action, long msLength) {
		this.msLength = msLength;
		this.action = action;
		sleepInterval = (int) msLength / 4;
		this.running = false;
		this.executed = false;
		this.msRemaining = msLength;
		this.aborted = false;
		id = getNewId();
		print("AmTIMER: created with id:" + id);
	}
	
	public AmAsyncAction(Runnable action, long msLength, boolean debugEnabled) {
		this(action, msLength);
		this.debugEnabled = debugEnabled;
	}
	
	private String getNewId() {
		return UUID.randomUUID().toString();
	}
	
	public void abort(){
		aborted = true;
		print("AmTIMER: asked to abort, waiting...");
	}
	
	public void executeDelayedAsync() {
		if(aborted || running || action == null) {
			return;
		}
		
		long startMsTime = System.currentTimeMillis();
		
		new Thread(() -> {
			executed = false;
			running = true;
			
			while (!aborted && msRemaining > 0) {
				
				try {
					if(!paused) {
						// remaining = total - (elapsed)
						msRemaining = msLength - (System.currentTimeMillis() - startMsTime);
						print("AmTIMER: Action NOT PAUSED");

					} else {
						print("AmTIMER: Action PAUSED");
					}
					
					Thread.sleep(sleepInterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(!aborted) {
				print("AmTIMER: Executing Action");
				action.run();
				executed = true;
			} else {
				print("AmTIMER: Action ABORTED");
			}
			
			running = false;
			
		}).start();
	}

	private void print(String string) {
		if(debugEnabled) {
			System.out.println(id + " # " + string);
		}
	}

	public boolean isExecuted() {
		return executed;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	
	public boolean isAborted() {
		return aborted;
	}

	public boolean isRunning() {
		return running;
	}

	public long getMsLength() {
		return msLength;
	}

	public void setMsLength(long msLength) {
		this.msLength = msLength;
	}

	public Runnable getAction() {
		return action;
	}

	public void setAction(Runnable action) {
		this.action = action;
	}

	public long getMsRemaining() {
		return msRemaining;
	}
	
}
