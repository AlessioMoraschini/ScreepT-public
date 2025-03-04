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
package various.common.light.om;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.management.BadAttributeValueExpException;

import various.common.light.files.om.FileNamed;


public class LimitedConcurrentList <T>{

	private Vector<T> list;
	private int maxSize;

	// constructor

	public LimitedConcurrentList(int maxSize){
		this.maxSize = maxSize;
		list = new Vector<T>();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	// methods

	public int getSize() {
		return list.size();
	}

	public boolean isFull() {
		return list.size() == maxSize;
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * Add an element at the end of the list. If max is reached first element will be cleaned out before to insert
	 * @param element
	 * @return
	 */
	public boolean add(T element) {

		synchronized (list) {
			if (isFull() && !list.isEmpty()) {
				list.remove(0);
			}
			list.add(element);
		}
		return isFull();
	}

	/**
	 * Add a collection of elements at the end of the list. If max is reached first element will be cleaned out before to insert
	 * @param element
	 * @return
	 */
	public boolean addAll(Collection<T> elements) {

		synchronized (list) {
			for (T element : elements) {
				if (isFull() && !list.isEmpty()) {
					list.remove(0);
				}
				list.add(element);
			}
		}
		return isFull();
	}

	/**
	 * Add an element at the end of the list (only if not yet present in list). If max is reached first element will be cleaned out before to insert
	 * @param element
	 * @return
	 */
	public boolean addIfNotPresent(Collection<T> elements) {
		boolean added = false;

		synchronized (list) {
			for (T element : elements) {
				if (!list.contains(element)) {
					if (isFull() && !list.isEmpty()) {
						list.remove(0);
					}
					list.add(element);
				}
			}
		}
		return added;
	}

	public void addIfNotPresent(T element) {
		if(!list.contains(element)) {
			addUniqueToTop(element);
		}
	}

	/**
	 * Add elements at the end of the list(if an element is already present it will be cleaned out and then re-added).
	 * If max is reached first element will be cleaned out before to insert
	 * @param element
	 * @return
	 */
	public void addUniquesToTop(Collection<T> elements) {

		if(elements == null)
			return;

		synchronized (list) {
			for (T element : elements) {

				if (isFull() && !list.isEmpty()) {
					list.remove(0);
				}

				if (list.contains(element)) {
					list.remove(element);
					list.add(element);
				} else {
					list.add(element);
				}

			}
		}
	}

	/**
	 * Add an element at the end of the list(if an element is already present it will be cleaned out and then re-added).
	 * If max is reached first element will be cleaned out before to insert
	 * @param element
	 * @return
	 */
	public void addUniqueToTop(T element) {

		if(element == null)
			return;

		synchronized (list) {
			if (isFull() && !list.isEmpty()) {
				list.remove(0);
			}

			if (list.contains(element)) {
				list.remove(element);
				list.add(element);
			} else {
				list.add(element);
			}
		}
	}
	public void addUniqueToTopByEquals(T element) {

		if(element == null)
			return;

		synchronized (list) {
			if (isFull() && !list.isEmpty()) {
				list.remove(0);
			}

			for(T el : list) {
				if(element.equals(el)) {
					list.remove(el);
				}
			}
			list.add(0, element);
		}
	}

	/**
	 * Basically same as add()
	 * @param element
	 * @return
	 */
	public boolean addLast(T element) {

		synchronized (list) {
			if (isFull() && !list.isEmpty()) {
				list.remove(0);
			}
			list.add(element);
		}
		return isFull();
	}

	public boolean removeLast() {
		if(list != null && !list.isEmpty()) {
			int size = list.size();
			list.remove(size-1);
			return true;
		}else return false;
	}

	/** removes all occurrences returning new size after deleting
	 *
	 * @param target the element to remove
	 * @return the size after removal
	 */
	public int removeAllOccurrences(T target) {

		synchronized (list) {
			while (list.contains(target)) {
				if (list != null && !list.isEmpty()) {
					list.remove(target);
				}
			}
		}
		return list.size();
	}

	/** removes all occurrences returning new size after deleting
	 *
	 * @param target the element to remove
	 * @return the size after removal
	 */
	public int removeAllOccurrencesByEquals(T target) {
		Vector<T> toRemove = new Vector<T>();
		synchronized (list) {
			for (T current : list) {
				if (current.equals(target)) {
					toRemove.add(current);
				}
			}

			for(T rmi : toRemove) {
				list.remove(rmi);
			}
		}
		return list.size();
	}

	/** Last index of with search based on equals method to find last match
	 *
	 * @param target the element to remove
	 * @return -1 if not found, last index in the other case
	 */
	public int lastIndexOfByEquals(T target) {
		int lastIndex = -1;
		Iterator<T> iterator = list.iterator();
		int i = 0;
		while(iterator.hasNext()) {
			T currentEl = iterator.next();
			if (currentEl.equals(target)) {
				lastIndex = i;
			}
			i++;
		}

		return lastIndex;
	}

	public T getLast() {
		if (list != null && !list.isEmpty()) {
			int size = list.size();
			return list.get(size-1);

		} else return null;
	}

	/**
	 * Returns list of strings separated by separator. Incase of file list it uses absolutePath
	 * @param separator
	 * @return
	 */
	public String toCharSeparatedString(String separator) {
		if(list == null) {return "";}
		StringBuilder builder = new StringBuilder();
		for(T curr : list) {
			try {
				if (getLast().equals(curr)) {
					separator = "";
					// Avoid last separator after file
				}
				if (curr instanceof File) {
					builder.append(((File) curr).getAbsolutePath()).append(separator);
				} else if (curr instanceof FileNamed) {
					builder.append(((FileNamed) curr).mFile.getAbsolutePath()).append(separator);
				} else {
					builder.append(curr).append(separator);
				}
			} catch (Exception e) {
			}
		}

		return builder.toString();
	}

	/**
	 * Only for elements of type String[] or List<String> or Vector<String>
	 * @param separator
	 * @param innerSeparator
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String toCharSeparatedStringMatrix(String separator, String innerSeparator) {
		if(list == null) {return "";}

		StringBuilder builder = new StringBuilder();
		for(T curr : list) {
			int j = 0;

			try {
				if (curr instanceof String[]) {
					String[] array = (String[]) curr;
					int i = 0;
					for(String s : array) {
						builder.append(s);
						if(i < array.length - 1)
							builder.append(innerSeparator);

						i++;
					}

				} else if (curr instanceof List<?>) {
					List<String> array = (List<String>) curr;
					int i = 0;
					for(String s : array) {
						builder.append(s);
						if(i < array.size() - 1)
							builder.append(innerSeparator);

						i++;
					}

				} else if (curr instanceof Vector){
					Vector<String> array = (Vector<String>) curr;
					int i = 0;
					for(String s : array) {
						builder.append(s);
						if(i < array.size() - 1)
							builder.append(innerSeparator);

						i++;
					}

				} else {
					throw new BadAttributeValueExpException(curr + " -  was expected to be one of [String[] or List<String> or Vector<String>], but it's a " + curr.getClass().getCanonicalName());
				}

			} catch (Exception e) {
			}

			if(j < list.size() - 1)
				builder.append(separator);

			j++;
		}

		return builder.toString();
	}

	public void reset() {
		if(list == null)
			list = new Vector<T>();

		list.clear();
	}

	// getters & setters

	public Vector<T> getList() {
		return list;
	}

	public void setList(Vector<T> list) {
		this.list = list;
	}

	public void setListFromList(List<T> list, boolean uniqueOnly) {
		reset();

		for(T t : list) {
			if(uniqueOnly)
				addUniqueToTopByEquals(t);
			else
				add(t);
		}
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
