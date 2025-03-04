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
package various.common.light.utility.reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import various.common.light.utility.string.StringWorker;

public class ReflectionUtils {

	public static List<Field> retrieveAllFields(Class<?> srcClass) {
		List<Field> fields = Arrays.asList(srcClass.getDeclaredFields());
        return fields;
	}
	
	@SafeVarargs
	public static <T,E> List<T> retrieveFieldsByType(Class<T> filterType, E... parentObjInstance) throws IllegalArgumentException, IllegalAccessException {
		List<T> result = new ArrayList<>();
		
		for(E instanz : parentObjInstance) {
			result.addAll(retrieveFieldsByType(instanz, filterType));
		}
		
		return result;
	}
	/**
	 * Search all fields in src class, and filter only the ones that are of generic type T
	 * @param srcClass
	 * @param filterType
	 * @return
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static <T,E> List<T> retrieveFieldsByType(E parentObjInstance, Class<T> filterType) throws IllegalArgumentException, IllegalAccessException {
		List<Field> fields = retrieveAllFields(parentObjInstance.getClass());
		List<T> result = new ArrayList<>();
		
		for(Field i_field : fields) {
			i_field.setAccessible(true);
			if(filterType.isAssignableFrom(i_field.getType())) {
				T positiveMatch = filterType.cast(i_field.get(parentObjInstance));
				result.add(positiveMatch);
			}
		}
        
        return result;
	}
	
	/**
	 * Search all fields in src class, and filter only the ones that are of generic type T
	 * @param srcClass
	 * @param filterType
	 * @param fieldNameFilter use the pattern to match in field name to discard/accept it. You can use a regular expression here
	 * @param discardMatches if true fields with name matching will be discarded, else if false they'll be accepted
	 * @return
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static <T,E> List<T> retrieveFieldsByTypeMatchingRegex(E parentObjInstance, Class<T> filterType, String fieldNameFilter, boolean discardMatches) throws IllegalArgumentException, IllegalAccessException {
		List<Field> fields = retrieveAllFields(parentObjInstance.getClass());
		List<T> result = new ArrayList<>();
		
		for(Field i_field : fields) {
			i_field.setAccessible(true);
			
			if(filterType.isAssignableFrom(i_field.getType())) {
				T positiveMatch = filterType.cast(i_field.get(parentObjInstance));
				
				if (discardMatches) {
					if (!i_field.getName().matches(fieldNameFilter)) {
						// discard when matching string filter
						result.add(positiveMatch);
					}
				}
				
				if (!discardMatches) {
					if (i_field.getName().matches(fieldNameFilter)) {
						// accept when matching string filter
						result.add(positiveMatch);
					}
				}
			}
		}
        
        return result;
	}
	
	/**
	 * Search all fields in src class, and filter only the ones that are of generic type T
	 * @param srcClass
	 * @param filterType
	 * @param fieldNameFilter if contained in field name will be discard/accept it.
	 * @param discardMatches if true fields wich name contains filter will be discarded, else if false only them will be accepted
	 * @param ignoreCase if true it will check even if field name contains filter with different case but same letters
	 * @return
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static <T,E> List<T> retrieveFieldsByTypeContaininingFilter(E parentObjInstance, Class<T> filterType, String fieldNameFilter, boolean discardMatches, boolean ignoreCase) throws IllegalArgumentException, IllegalAccessException {
		Field[] fields = getAllFields(parentObjInstance.getClass());
		
		List<T> result = filterFields(Arrays.asList(fields), parentObjInstance, filterType, fieldNameFilter, discardMatches, ignoreCase);
		
		return result;
	}
	
    /**
     * Gets all fields of the given class and its parents (if any).
     *
     * @param cls
     *            the {@link Class} to query
     * @return an array of Fields (possibly empty).
     * @throws IllegalArgumentException
     *             if the class is {@code null}
     */
    public static List<Field> getAllFieldsList(final Class<?> cls) throws IllegalArgumentException {
        if(cls == null) {
        	throw new IllegalArgumentException("The class must not be null");
        }
        
        final List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            Collections.addAll(allFields, declaredFields);
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }
    
    /**
     * Gets all fields of the given class and its parents (if any).
     *
     * @param cls
     *            the {@link Class} to query
     * @return an array of Fields (possibly empty).
     * @throws IllegalArgumentException
     *             if the class is {@code null}
     */
    public static Field[] getAllFields(final Class<?> cls) {
        final List<Field> allFieldsList = getAllFieldsList(cls);
        return allFieldsList.toArray(new Field[allFieldsList.size()]);
    }

	/**
	 * Search all fields in src class, and filter only the ones that are of generic type T
	 * @param srcClass
	 * @param filterType
	 * @param fieldNameFilter if contained in field name will be discard/accept it.
	 * @param discardMatches if true fields wich name contains filter will be discarded, else if false only them will be accepted
	 * @param ignoreCase if true it will check even if field name contains filter with different case but same letters
	 * @return
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static <T,E> List<T> retrievePubFieldsByTypeContaininingFilter(E parentObjInstance, Class<T> filterType, String fieldNameFilter, boolean discardMatches, boolean ignoreCase) throws IllegalArgumentException, IllegalAccessException {
		List<Field> fields = retrieveAllFields(parentObjInstance.getClass());
		
		List<T> result = filterFields(fields, parentObjInstance, filterType, fieldNameFilter, discardMatches, ignoreCase);
		
		return result;
	}
	
	public static <T,E> List<T> filterFields(List<Field> fields, E parentObjInstance, Class<T> filterType, String fieldNameFilter, boolean discardMatches, boolean ignoreCase) throws IllegalArgumentException, IllegalAccessException {
		List<T> result = new ArrayList<>();
		
		for(Field i_field : fields) {
			i_field.setAccessible(true);
			
			if(filterType.isAssignableFrom(i_field.getType())) {
				T positiveMatch = filterType.cast(i_field.get(parentObjInstance));
				
				if (discardMatches) {
					
					if (ignoreCase && !StringWorker.containsIgnoreCase(i_field.getName(), fieldNameFilter)) {
						result.add(positiveMatch);
					}else if (!ignoreCase && !StringWorker.contains(i_field.getName(), fieldNameFilter)) {
						result.add(positiveMatch);
					}
					
				} else {
					
					if (ignoreCase && StringWorker.containsIgnoreCase(i_field.getName(), fieldNameFilter)) {
						result.add(positiveMatch);
					}else if (!ignoreCase && StringWorker.contains(i_field.getName(), fieldNameFilter)) {
						result.add(positiveMatch);
					}
				}
			}
		}
		
		return result;
	}
}
