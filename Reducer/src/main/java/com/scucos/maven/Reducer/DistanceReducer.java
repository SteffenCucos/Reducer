package com.scucos.maven.Reducer;

import java.util.Set;
import java.util.HashSet;

public abstract class DistanceReducer<T> extends AbstractReducer<T> {

	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		
		int prevSize = slices.size();
		Set<Slice<T>> reduced = new HashSet<>();
		
		while (slices.size() > 0) {
			Set<Slice<T>> leftover = new HashSet<>();
			Slice<T> head = slices.stream().iterator().next();
			slices.remove(head);
			
			for(Slice<T> slice : slices) {
				if(slice.containedIn(head)) {
					continue;
				}
				if(head.containedIn(slice)) {
					head = slice;
					continue;
				}
				
				
				Set<Object> differentCategories = slice.asymetricDifference(head);
				int distance = differentCategories.size();
				
				if(distance == 1) { //They only differ in one category, we can merge them with no "holes" in the slice
					Object unionCategory = differentCategories.iterator().next();
					head.unionAdd(unionCategory, slice);
				} else {
					leftover.add(slice);
				}
			}
			
			reduced.add(head);
			slices = leftover;	
		}
		
		if(reduced.size() < prevSize) {
			return reduceSlices(reduced);
		}
		
//		while(reduced.size() < prevSize) {
//			reduced = new HashSet<>();
//			prevSize = slices.size();
//			
//			main loop goes here
		
//			slices = reduced;
//		}
	
		return reduced;
	}
}