package com.scucos.maven.Reducer;

import java.util.stream.Collectors;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Slice<T> objects are used to encapsulate a rectangular slice of the N dimensional space formed from a Set<Slice<T>> objects
 * The slice keeps track of a list of categories (dimension identifiers) as well as a Collection<Object> for each category
 * representing positions along that dimension axis.
 * @author SCucos
 *
 * @param <T>
 */
public class Slice<T> implements Comparable<Slice<T>> {

	private Map<Object, Collection<?>> sliceMap = new HashMap<>();
	
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
	
	// Constructors
	
	Class<T> tClass;
	
	public Slice(Class<T> tClass) throws SliceConstructionException {
		this.tClass = tClass;
	}
	
	@SuppressWarnings("unchecked")
	public Slice(T t, Class<? extends Object> tClass) throws SliceConstructionException {
		
		this.tClass = (Class<T>) tClass;
		
		//Only work on the top level fields (ignore super fields)
		for(Field field : t.getClass().getDeclaredFields()) {
			
			String name = field.getName();
			Class<?> fieldClass = field.getType();
			Class<?> collectionClass = Collection.class;
			boolean isCollection = collectionClass.isAssignableFrom(fieldClass);
			if(isCollection) {
				field.setAccessible(true);
				
				try {
					sliceMap.put(name, (Collection<Object>) field.get(t));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new SliceConstructionException(e.getMessage());
				}
			}
		}
	}
	
	// sliceMap operations
	
	public int getWidth() {
		return this.sliceMap.size();
	}
	
	public Set<Object> getCategories() {
		return this.sliceMap.keySet();
	}
	
	public void addEntry(Object category, Collection<?> objects) {
		this.sliceMap.put(category, objects);
	}
	
	public Collection<?> getEntry(Object category) {
		return this.sliceMap.get(category);
	}
	
	public void deleteEntry(Object category) {
		this.sliceMap.remove(category);
	}
	
	@SuppressWarnings("unchecked")
	public void addObjects(Object category, Collection<?> objects) {
		Collection<Object> thisObjects = (Collection<Object>) this.getEntry(category);
		thisObjects.addAll(objects);
	}
	
	public Map<Object, Collection<?>> getMap() {
		return sliceMap;
	}
	
	public int volume() {
		return this.getMap().values().stream().map(c -> c.size()).reduce(1, (accumulator, current) -> accumulator*current);
	}
	
	public String toString() {
		return sliceMap.toString();
	}
	
	// Slice operations
	
	@Override
	public int compareTo(Slice<T> other) {
		int otherVolume = other.volume();
		int volume = volume();
		return (volume > otherVolume) ? -1 : (volume == otherVolume) ? 0 : 1;
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
	
	public List<Object> asymetricDifference(Slice<T> into) {
		List<Object> diffCategories = new ArrayList<>();
		for(Object category : getCategories()) {
			Collection<?> otherObjects = into.getEntry(category);
			Collection<?> thisObjects = this.getEntry(category);
			if(!otherObjects.containsAll(thisObjects) || !thisObjects.containsAll(otherObjects)) {
				diffCategories.add(category);
			}
		}
	
		return diffCategories;
	}
	
	public void unionAdd(Object category, Slice<T> other) {
		Collection<?> otherObjects = other.getEntry(category);
		addObjects(category, otherObjects);
	}
	
	// Reflection utility methods
	
	@SuppressWarnings("unchecked")
	public static <T, M, C extends Collection<M>> C getFieldAsType(String category, Slice<T> slice) {
		return (C) slice.getEntry(category);
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, Collection<V>> toMap(Slice<Map<K, Collection<V>>> slice) {
		Map<K, Collection<V>> map = new HashMap<>();
		
		for(Object key : slice.sliceMap.keySet()) {
			map.put((K)key, (Collection<V>) slice.sliceMap.get(key));
		}
		
		return map;
	}
	
	public T toType() throws ObjectConstructionException {
		
		if (tClass == null) {
			throw new ObjectConstructionException("Slice instance is missing T class object\n");
		}
		
		T t = null;
		try {
			t = (T) tClass.newInstance();
		} catch (InstantiationException e) {
			throw new ObjectConstructionException("Class '" + tClass.toString() + "' does not have empty constructor\n" + e.toString());
		} catch (IllegalAccessException e) {
			throw new ObjectConstructionException("Unable to create object of type '" + tClass.getCanonicalName() + "'\n" + e.toString());
		}
		
		for (Object field : sliceMap.keySet()) {
			Collection<?> objects = sliceMap.get(field);
			String fieldName = (String)field;
			if (!setField(t, fieldName, objects)) {
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
}
