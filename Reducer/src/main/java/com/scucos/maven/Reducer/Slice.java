package com.scucos.maven.Reducer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Slice<T> {

	private Map<String, Collection<Object>> slice = new HashMap<>();
	public int width;
	public Set<String> categories;
	
	public static class SliceConstructionException extends RuntimeException {
		public SliceConstructionException(String error) {
			super(error);
		}
	}
	
	public static class ObjectConstructionException extends RuntimeException {
		public ObjectConstructionException(String error) {
			super(error);
		}
	}
	
	public Slice(Map<String, Collection<Object>> slice) {
		this.slice = slice;
		this.categories = slice.keySet();
	}
	
	
	@SuppressWarnings("unchecked")
	public Slice(T t) throws SliceConstructionException {
		//Only work on the top level fields (ignore super fields)
		for(Field field : t.getClass().getDeclaredFields()) {
			
			String name = field.getName();
			Class<?> fieldClass = field.getType();
			Class<?> collectionClass = Collection.class;
			boolean isCollection = collectionClass.isAssignableFrom(fieldClass);
			if(isCollection) {
				field.setAccessible(true);
				
				try {
					slice.put(name, (Collection<Object>) field.get(t));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new SliceConstructionException(e.getMessage());
				}
			}
		}
		this.categories = slice.keySet();
		this.width = slice.size();
	}
	
	public T toType(Class<T> cls) throws ObjectConstructionException {
		T t = null;
		try {
			t = (T) cls.newInstance();
		} catch (InstantiationException e) {
			throw new ObjectConstructionException("Class '" + cls.toString() + "' does not have empty constructor");
		} catch (IllegalAccessException e) {
			throw new ObjectConstructionException("Unable to create object from slice");
		}
		
		for(String fieldName : slice.keySet()) {
			Collection<Object> objects = slice.get(fieldName);
			if(!setField(t, fieldName, objects)) {
				throw new ObjectConstructionException("Unable to set field '" + fieldName + "' while constructing object");
			}
		}

		return t;
	}
	
//	public static Object getFieldValue(Object targetObject, String fieldName) throws SliceConstructionException {
//	    Field field;
//	    try {
//	        field = targetObject.getClass().getDeclaredField(fieldName);
//	    } catch (NoSuchFieldException e) {
//	        field = null;
//	    }
//	    Class<?> superClass = targetObject.getClass().getSuperclass();
//	    while (field == null && superClass != null) {
//	        try {
//	            field = superClass.getDeclaredField(fieldName);
//	        } catch (NoSuchFieldException e) {
//	            superClass = superClass.getSuperclass();
//	        }
//	    }
//	    if (field == null) {
//	        throw new SliceConstructionException("Unable to find field '" + fieldName + "'");
//	    }
//	    field.setAccessible(true);
//	    try {
//	        return field.get(targetObject);
//	    } catch (IllegalAccessException e) {
//	    	throw new SliceConstructionException("Unable to access value from field '" + fieldName + "'");
//	    }
//	}
	
	public static boolean setField(Object targetObject, String fieldName, Object fieldValue) {
	    Field field;
	    try {
	        field = targetObject.getClass().getDeclaredField(fieldName);
	    } catch (NoSuchFieldException e) {
	        field = null;
	    }
	    Class superClass = targetObject.getClass().getSuperclass();
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
	    field.setAccessible(true);
	    try {
	        field.set(targetObject, fieldValue);
	        return true;
	    } catch (IllegalAccessException e) {
	        return false;
	    }
	}
	
	public void addEntry(String category, Collection<Object> objects) {
		this.slice.put(category, objects);
	}
	
	public Collection<Object> getEntry(String category) {
		return this.slice.get(category);
	}
	
	public void deleteEntry(String category) {
		this.slice.remove(category);
	}
}
