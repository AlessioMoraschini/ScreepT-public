package various.common.light.utility.manipulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import various.common.light.utility.string.StringWorker;

public class ArrayHelper {

	public static <T> T getLast(T[] source) {
		if(source == null) return null;

		return source[source.length - 1];
	}

	public static <T> List<T> removeNulls(List<T> sourceList){
		if(sourceList != null) {
			int i = 0;
			for(T element : sourceList) {
				if(element == null)
					sourceList.remove(i);
				i++;
			}
		}

		return sourceList;
	}

	@SafeVarargs
	public static <T, E  extends Collection<T>> E populateList(Class<E> collection, T... values) throws InstantiationException, IllegalAccessException{
		E instance = collection.newInstance();
		for(T val : values) {
			instance.add(val);
		}

		return instance;
	}

	public static <T extends Comparable<?>> Set<T> sortSet(Set<T> set){
		return listToSet(setToList(set), true);
	}

	public static <T> List<T> setToList(Set<T> set) {
		List<T> list = new ArrayList<>();

		if(set == null || set.isEmpty())
			return list;

		Iterator<T> it = set.iterator();
		while(it.hasNext()) {
			T element = it.next();
			list.add(element);
		}

		return list;
	}

	public static <T> Set<T> listToSet(List<T> list, boolean sorted) {
		Set<T> set = sorted ? new TreeSet<>() : new HashSet<>();

		if(list == null || list.isEmpty())
			return set;

		for(T element : list) {
			set.add(element);
		}

		return set;
	}

	@SafeVarargs
	public static <T> List<T> removeNulls(T... sourceList){
		List<T> out = new ArrayList<>();
		if(sourceList != null) {
			for(T element : sourceList) {
				if(element != null)
					out.add(element);
			}
		}

		return out;
	}

	public static <T extends Comparable<? super T>> List<T> asList(Collection<T> c, boolean sorted) {
	  List<T> list = new ArrayList<T>(c);
	  if(sorted)
		  java.util.Collections.sort(list);
	  return list;
	}

	public static List<String> removeEmpties(ArrayList<String> sourceList){
		if(sourceList != null) {
			int i = 0;
			for(String element : sourceList) {
				if(element == null || StringWorker.isEmpty(element))
					sourceList.remove(i);
				i++;
			}
		}

		return sourceList;
	}

	public static String[] removeEmpties(String[] sourceList){
		ArrayList<String> list = new ArrayList<>();
		if(sourceList != null) {
			for(String element : sourceList) {
				if(element != null && !StringWorker.isEmpty(element))
					list.add(element);
			}
		}

		String[] result = new String[list.size()];
		int i = 0;
		for(String notEmpty : list) {
			result[i] = notEmpty;
			i++;
		}

		return result;
	}

	public static <T> T getRandomElement(T[] array, Float probabilityPerc) {

		if(array == null || array.length == 0) {
			return null;
		}

		if(probabilityPerc == null) {
			probabilityPerc = 100f;
		}

		if(probabilityPerc > 100f)
			probabilityPerc = 100f;

		if(probabilityPerc < 0f)
			probabilityPerc = 0f;

		int nValues = array.length;
		int progressive = 0;


		Random random = new Random();
		int randomInt = random.nextInt(100);

		for(int i = 0; i < nValues; i++) {
			progressive += probabilityPerc / nValues;
			if(randomInt < progressive) {
				return array[i];
			}
		}

		return null;
	}

	public static <T> T getRandomElement(List<T> list, Float probabilityPerc) {

		if(list == null || list.isEmpty()) {
			return null;
		}

		if(probabilityPerc == null) {
			probabilityPerc = 100f;
		}

		if(probabilityPerc > 100f)
			probabilityPerc = 100f;

		if(probabilityPerc < 0f)
			probabilityPerc = 0f;

		int nValues = list.size();
		int progressive = 0;


		Random random = new Random();
		int randomInt = random.nextInt(100);

		for(int i = 0; i < nValues; i++) {
			progressive += probabilityPerc / nValues;
			if(randomInt < progressive) {
				return list.get(i);
			}
		}

		return null;
	}

}
