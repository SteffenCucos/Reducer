package com.scucos.maven.Reducer.Reducers;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import com.scucos.maven.Reducer.Slice;

public abstract class RowByRowReducer<T> implements Reducer<T> {

	@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		PriorityQueue<Slice<T>> reduced = new PriorityQueue<>();
		
		boolean anyReduction = false;
		
		for(Slice<T> slice : slices) {
			boolean containedIn = false;
			boolean merged = false;
			for(Slice<T> reducedSlice : reduced) {
				if(slice.containedIn(reducedSlice)) {
					containedIn = true;
					anyReduction = true;
					break;
				}
				
				// If they only differ in one category
				List<Object> differentCategories = slice.asymetricDifference(reducedSlice);
				if(differentCategories.size() == 1) {
					// merge them
					Object unionCategory = differentCategories.get(0);
					reducedSlice.unionAdd(unionCategory, slice);
					merged = true;
					anyReduction = true;
					break;
				}
			}
			
			if(containedIn) {
				// The slice was a duplicate and was already contained in another slice
				continue;
			}
			
			if(merged) {
				// The slice was merged into another slice
				continue;
			} else {
				// The slice was not contained in another slice or merged, so add it to reduced
				reduced.add(slice);
			}
		}
		
		if(anyReduction) {
			return reduceSlices(new HashSet() {{addAll(reduced);}});
		}
		return new HashSet<Slice<T>>() {{ addAll(reduced);}};
	}
}
