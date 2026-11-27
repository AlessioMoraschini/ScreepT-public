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
