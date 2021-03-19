package utility.time;

public class TimeMeter {

	private long startTime;
	private long endTime;
	
	public TimeMeter() {
		reset();
	}
	
	/**
	 * Set start time as current nanosec time
	 */
	public void updateStart(){
		startTime = System.nanoTime();
	}
	
	/**
	 * Set end time as current nanosec time
	 */
	public void updateEnd(){
		endTime = System.nanoTime();
	}
	
	/**
	 * Get elapsed time in nanoseconds or milliseconds
	 * @param millisec if true return millisec, else return nanosec
	 * @return
	 */
	public long getElapsed(boolean millisec){
		int divisor = (millisec)? 1000000 : 1;
		return (endTime-startTime)/divisor;
	}
	
	public void reset() {
		startTime = 0;
		endTime = 0;
	}
}
