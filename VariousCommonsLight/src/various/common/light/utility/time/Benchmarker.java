package various.common.light.utility.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import various.common.light.utility.manipulation.ArrayHelper;
import various.common.light.utility.manipulation.EntrophyzerUtils;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.string.StringWorker.EOL;

public class Benchmarker {

	private Long startTime;
	private Long endTime;
	private boolean concurrent;
	
	private Map<Long, TimeRecord> records;
	
	public Benchmarker(boolean concurrent) {
		reset(concurrent);
	}
	
	public void start() {
		reset(concurrent);
	}
	
	/**
	 * Set start time as current nanosec time
	 */
	private void updateStart(){
		if(startTime == null)
			startTime = 0L;
		
		synchronized (startTime) {
			startTime = System.nanoTime();
		}
	}
	
	/**
	 * Set end time as current nanosec time
	 */
	private void updateEnd(){
		if(endTime == null)
			endTime = 0L;
		
		synchronized (endTime) {
			endTime = System.nanoTime();
		}
	}
	
	/**
	 * Insert an event with start/end/elapsed information and the given message
	 * @param message
	 * @return
	 */
	public TimeRecord registerEvent(String message) {
		TimeRecord record = null;
		
		synchronized (endTime) {
			long elapsed = getElapsed(true);
			record = new TimeRecord(startTime, endTime, elapsed, message);
			records.put(endTime, record);
		}
		
		updateStart();
		
		return record;
	}
	
	public void finalize() {
		registerEvent("FINALIZED");
	}
	
	/**
	 * Get elapsed time in nanoseconds or milliseconds
	 * @param millisec if true return millisec, else return nanosec
	 * @return
	 */
	public long getElapsed(boolean millisec){
		updateEnd();
		int divisor = (millisec)? 1000000 : 1;
		return (endTime-startTime)/divisor;
	}
	
	public void reset(boolean concurrent) {
		updateStart();
		updateEnd();
		records = concurrent ? new ConcurrentHashMap<>() : new HashMap<>();
	}
	
	public String getSortedRecordsString() {
		StringBuilder builder = new StringBuilder();
		List<TimeRecord> sortedRecords = getRecords();
		List<Long> elapsedList = new ArrayList<>();
		long totalElapsed = 0;
		for(TimeRecord rec : sortedRecords) {
			elapsedList.add(rec.elapsed);
			totalElapsed += rec.elapsed;
			builder.append("\n >> ")
				.append(rec.elapsed + " ms from last event -> new Event: ")
				.append(rec.message).append(EOL.defaultEol.eol);
		}
		
		builder.append("\n >> TOTAL ELAPSED TIME (MS) : " + totalElapsed + "\n");
		builder.append("\n ################################################################");
		builder.append("\n ## Elapsed time percentage report (from heaviest to lightest) ## \n\n");
		
		Map<Double, TimeRecord> percentSortedMap = new TreeMap<>();
		for(int i = 0; i < elapsedList.size() - 1; i++) {
			percentSortedMap.put(elapsedList.get(i+1)/(double)totalElapsed * 100D, sortedRecords.get(i));
		}
		percentSortedMap = EntrophyzerUtils.sortMapByKeyGeneric(percentSortedMap);
		int i = 1;
		
		List<Double> reverseOrderKeys = ArrayHelper.asList(percentSortedMap.keySet(), true);
		Collections.reverse(reverseOrderKeys);
		for(Double key : reverseOrderKeys) {
			builder.append(i + ": " + StringWorker.getStringDouble(key, 2)).append("% >> ");
			builder.append(percentSortedMap.get(key).message).append("\n");
			i++;
		}
		
		return builder.toString();
	}
	
	public List<TimeRecord> getRecords(){
		Set<Long> keys = records.keySet();
		List<Long> sortedKeys = ArrayHelper.asList(keys, true);
		List<TimeRecord> timeRecords = new ArrayList<>();
		for(Long key : sortedKeys) {
			TimeRecord rec = records.get(key);
			timeRecords.add(rec);
		}
		
		return timeRecords;
	}

	
	public class TimeRecord {
		
		private long startTime;
		private long endTime;
		private long elapsed;
		private String message;
		
		public TimeRecord(long startTime, long endTime, long elapsed, String message) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.elapsed = elapsed;
			this.message = message;
		}

		@Override
		public String toString() {
			return "TimeRecord: ["
					+ " >> message= " + message + "\n"
					+ " >> elapsed since last(ms)= " + elapsed + "\n"
					+ " >> startTime= " + startTime + "\n"
					+ " >> endTime= " + endTime + 
					"\n]\n";
		}
	}
}

