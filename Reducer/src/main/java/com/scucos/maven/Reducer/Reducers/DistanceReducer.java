package com.scucos.maven.Reducer.Reducers;

import java.util.Set;

import com.scucos.maven.Reducer.Slice;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

public abstract class DistanceReducer<T> implements Reducer<T> {

	/**
	 * 
	 */
	@SuppressWarnings("serial")
	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		
		int prevSize = slices.size();
		
		PriorityQueue<Slice<T>> slicesQueue = new PriorityQueue<>();
		slicesQueue.addAll(slices);
		
		Set<Slice<T>> reduced = new HashSet<>();
		
		while (slicesQueue.size() > 0) {
			Set<Slice<T>> leftover = new HashSet<>();
			Slice<T> head = slicesQueue.poll();
			
			for(Slice<T> slice : slicesQueue) {
				if(slice.containedIn(head)) {
					continue;
				}
				if(head.containedIn(slice)) {
					head = slice;
					continue;
				}
				
				List<Object> differentCategories = slice.asymetricDifference(head);
				int distance = differentCategories.size();
				
				if(distance == 1) { // They only differ in one category, we can merge them with no "holes" in the slice
					Object unionCategory = differentCategories.iterator().next();
					head.unionAdd(unionCategory, slice);
				} else {
					leftover.add(slice);
				}
			}
			
			reduced.add(head);
			slicesQueue = new PriorityQueue<Slice<T>>() {{
				addAll(leftover);	
			}};
		}
		
		if(reduced.size() < prevSize) {
			return reduceSlices(reduced);
		}
	
		return reduced;
	}
}