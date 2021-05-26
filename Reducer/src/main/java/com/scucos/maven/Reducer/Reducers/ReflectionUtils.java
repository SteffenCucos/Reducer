package com.scucos.maven.Reducer.Reducers;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Utility class for handling common reflection tasks
 * @author SCucos
 */
public class ReflectionUtils {

	/**
	 * Exception for when Object construction fails
	 * @author SCucos
	 */
	public static class ObjectConstructionException extends RuntimeException {

		private static final long serialVersionUID = -1850991944114013007L;

		public ObjectConstructionException(String error) {
			super(error);
		}
	}

	/**
	 * Given a Class<T> instance and a map of field names to values, 
	 * creates a new T instance with the desired fields set
	 * @param <T> The type to return
	 * @param tClass An instance of the provided types class
	 * @param fields A map of field names to values
	 * @return A constructed T instance with the fields provided in the map set
	 * @throws ObjectConstructionException
	 */
	public static <T> T createObjectWithFields(Class<T> tClass, Map<String, ? extends Object> fields) throws ObjectConstructionException {
		
		if(tClass == null) {
			throw new ObjectConstructionException("Provided with null tClass argument");
		}
		
		try {
			final T t = (T) tClass.newInstance();
			
			fields.forEach((fieldName, fieldValue) -> {
				if (!setField(t, fieldName, fieldValue)) {
					throw new ObjectConstructionException("Unable to set field '" + fieldName + "' while constructing object");
				}
			});
			
			return t;
		} catch (InstantiationException e) {
			throw new ObjectConstructionException("Class '" + tClass.toString() + "' does not have empty constructor\n" + e.toString());
		} catch (IllegalAccessException e) {
			throw new ObjectConstructionException("Unable to create object of type '" + tClass.getCanonicalName() + "'\n" + e.toString());
		}
	}
	
	/**
	 * If possible, sets the given field to a new value
	 * @param targetObject The object to work on
	 * @param fieldName The name of the field to set
	 * @param fieldValue The value to use
	 * @return true if the field was successfully set, false otherwise
	 */
	public static boolean setField(Object targetObject, String fieldName, Object fieldValue) {
	    Field field;
	    try {
	        field = targetObject.getClass().getDeclaredField(fieldName);
	    } catch (NoSuchFieldException e) {
	        field = null;
	    }
	    Class<?> superClass = targetObject.getClass().getSuperclass();
	    while (field == null && superClass != null) {
	        try {
	            field = superClass.getDeclaredField(fieldName);
	        } catch (NoSuchFieldException e) {
	            superClass = superClass.getSuperclass();
	        }
	    }
	    if (field == null) {
	        return false;
	    }
	    
	    try {
	    	field.setAccessible(true);
	        field.set(targetObject, fieldValue);
	        return true;
	    } catch (IllegalAccessException e) {
	        return false;
	    }
	}
}
