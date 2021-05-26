package com.scucos.maven.Reducer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.scucos.maven.Reducer.Reducers.ReflectionUtils;
import com.scucos.maven.Reducer.Reducers.ReflectionUtils.ObjectConstructionException;

/**
 * Slice<T> objects are used to encapsulate a rectangular slice of the N dimensional space formed from a Set<Slice<T>> objects
 * The slice keeps track of a list of categories (dimension identifiers) as well as a Collection<Object> for each category
 * representing positions along that dimension axis.
 * @author SCucos
 *
 * @param <T>
 */
public class Slice<T> implements Comparable<Slice<T>> {

	private Map<String, Collection<?>> sliceMap = new HashMap<>();
	
	private Class<T> tClass;
	
	@SuppressWarnings("serial")
	public static class SliceConstructionException extends RuntimeException {
		public SliceConstructionException(String error) {
			super(error);
		}
	}
	
	// Constructor

	@SuppressWarnings("unchecked")
	public Slice(T t) throws SliceConstructionException {
		
		this.tClass = (Class<T>) t.getClass();
		
		//Only work on the top level fields (ignore super fields)
		for(Field field : tClass.getDeclaredFields()) {
			
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
	
	public Set<String> getCategories() {
		return this.sliceMap.keySet();
	}
	
	public void addEntry(String category, Collection<?> objects) {
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
	
	public Map<String, Collection<?>> getMap() {
		return sliceMap;
	}
	
	public long volume() {
		return this.getMap().values()
				.stream()
				.map(c -> (long)c.size())
				.reduce(1l, Math::multiplyExact);
	}
	
	public String toString() {
		return sliceMap.toString();
	}
	
	// Slice operations
	
	@Override
	public int compareTo(Slice<T> other) {
		long otherVolume = other.volume();
		long volume = volume();
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
	
	// Reflection utility methods
	public T toType() throws ObjectConstructionException {
		return ReflectionUtils.createObjectWithFields(tClass, sliceMap);
	}
}
