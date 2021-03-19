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
package om;

import java.util.concurrent.LinkedBlockingDeque;

public class ConcurrentLimitedDeque <T>{
	
	private LinkedBlockingDeque<T> list;
	private int maxSize;
	
	// constructor
	
	public ConcurrentLimitedDeque(int maxSize){
		this.maxSize = maxSize;
		list = new LinkedBlockingDeque<>();
	}

	// methods 
	
	public boolean add(T element) {
		boolean condition = list.size() > maxSize;
		boolean maxReached = (condition) ? true : false;
		
		if(condition && !list.isEmpty()) {
			list.removeFirst();
		}
		
		list.add(element);
		
		return maxReached;
	}
	
	public boolean addLast(T element) {
		boolean condition = list.size() > maxSize;
		boolean maxReached = (condition) ? true : false;
		
		if(condition && !list.isEmpty()) {
			list.removeFirst();
		}
		
		list.addLast(element);
		
		return maxReached;
	}

	// getters & setters
	
	public LinkedBlockingDeque<T> getList() {
		return list;
	}

	public void setList(LinkedBlockingDeque<T> list) {
		this.list = list;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public String toString() {
		return "LimitedVector [list=" + list + ", maxSize=" + maxSize + "]";
	}
	
}
