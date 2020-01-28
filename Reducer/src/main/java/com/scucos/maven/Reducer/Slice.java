package com.scucos.maven.Reducer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Slice<T> objects are used to encapsulate a rectangular slice of the N dimensional space formed from a Set<Slice<T>> objects
 * The slice keeps track of a list of categories (dimension identifiers) as well as a Collection<Object> for each category
 * representing positions along that dimension axis.
 * @author SCucos
 *
 * @param <T>
 */
public class Slice<T> {

	private Map<Object, Collection<?>> slice = new HashMap<>();
	
	@SuppressWarnings("serial")
	public static class SliceConstructionException extends RuntimeException {
		public SliceConstructionException(String error) {
			super(error);
		}
	}
	
	@SuppressWarnings("serial")
	public static class ObjectConstructionException extends RuntimeException {
		public ObjectConstructionException(String error) {
			super(error);
		}
	}
	
	public Slice() {

	}
	
	public Slice(Map<Object, Collection<?>> slice) {
		this.slice = slice;
	}
	
	public Object mergeInto(Slice<T> into) {
		
		Object diffCategory = null;
		
		for(Object category : getCategories()) {
			Collection<?> otherObjects = into.getEntry(category);
			Collection<?> thisObjects = this.getEntry(category);
			if(!otherObjects.containsAll(thisObjects) && diffCategory == null) {
				diffCategory = category;
			} else if (!otherObjects.containsAll(thisObjects) && diffCategory != null) {
				return null;
			}
		}
		//completely Contained
		return diffCategory;
	}
	
	public boolean containedIn(Slice<T> into) {
		for(Object category : getCategories()) {
			Collection<?> otherObjects = into.getEntry(category);
			Collection<?> thisObjects = this.getEntry(category);
			if(!otherObjects.containsAll(thisObjects)) {
				return false;
			} 
		}
		//completely Contained
		return true;
	}
	
	public void merge(Object category, Slice<T> into) {
		Collection<?> thisObjects = this.getEntry(category);
		into.addObjects(category,thisObjects);
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
		
		for(Object field : slice.keySet()) {
			Collection<?> objects = slice.get(field);
			String fieldName = (String)field;
			if(!setField(t, fieldName, objects)) {
				throw new ObjectConstructionException("Unable to set field '" + fieldName + "' while constructing object");
			}
		}

		return t;
	}
	
	@SuppressWarnings("unchecked")
	private boolean setField(Object targetObject, String fieldName, Object fieldValue) {
	    Field field;
	    try {
	        field = targetObject.getClass().getDeclaredField(fieldName);
	    } catch (NoSuchFieldException e) {
	        field = null;
	    }
	    Class<? super T> superClass = (Class<? super T>) targetObject.getClass().getSuperclass();
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
	
	public int getWidth() {
		return this.slice.size();
	}
	
	public Set<Object> getCategories() {
		return this.slice.keySet();
	}
	
	public void addEntry(Object category, Collection<?> objects) {
		this.slice.put(category, objects);
	}
	
	public Collection<?> getEntry(Object category) {
		return this.slice.get(category);
	}
	
	public void deleteEntry(Object category) {
		this.slice.remove(category);
	}

	@SuppressWarnings("unchecked")
	public void addObjects(Object category, Collection<?> objects) {
		Collection<Object> thisObjects = (Collection<Object>) this.getEntry(category);
		thisObjects.addAll(objects);
	}
	
	public Map<Object, Collection<?>> getMap() {
		return slice;
	}
	
	@SuppressWarnings("unchecked")
	public static <T, M, C extends Collection> C getFieldAsType(String category, Slice<T> slice, C collection) {
		List<M> list = (List<M>) slice.getEntry(category)
				.stream()
				.map(o -> (M)o)
				.collect(Collectors.<M>toList());
		
		collection.addAll(list);
		
		return collection;
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, Collection<V>> toMap(Slice<Map<K, Collection<V>>> slice) {
		Map<K, Collection<V>> map = new HashMap<>();
		Map<Object, Collection<?>> sliceMap = slice.getMap();
		
		for(Object key : sliceMap.keySet()) {
			map.put((K)key, (Collection<V>) sliceMap.get(key));
		}
		
		return map;
	}
	
	public String toString() {
		return slice.toString();
	}
}
