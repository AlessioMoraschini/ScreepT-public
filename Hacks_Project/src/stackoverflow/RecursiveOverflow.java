/*
 *
 * =========================================================================================
 *  Copyright (C) 2019-2021
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com/
 */
package stackoverflow;

import java.util.ArrayList;
import java.util.List;

public class RecursiveOverflow {
	
	List<Object> list;

	public void heapOverflow() {
		list = new ArrayList<Object>();
		for(int i = 0; i < 50; i++) {
			RecursiveOverflow a = new RecursiveOverflow();
			list.add(a);
			a.heapOverflow();
		}
	}
	
	public static void stackOverflow(int a) {
		stackOverflow(a + 1);
	}
	

	public static void main(String[] args) {
		RecursiveOverflow.stackOverflow(1);
	}
}
