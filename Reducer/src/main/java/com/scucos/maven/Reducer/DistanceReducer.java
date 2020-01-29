package com.scucos.maven.Reducer;

import java.util.Set;
import java.util.HashSet;

public abstract class DistanceReducer<T> extends AbstractReducer<T> {

	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		
		int prevSize = slices.size();
		Set<Slice<T>> reduced = new HashSet<>();
		
		while(reduced.size() < prevSize) {
			reduced = new HashSet<>();
			prevSize = slices.size();
			
			while (slices.size() > 0) {
				Set<Slice<T>> leftover = new HashSet<>();
				Slice<T> head = slices.stream().iterator().next();
				slices.remove(head);
				
				for(Slice<T> slice : slices) {
					Object mergeCategory = slice.mergeInto(head);
					
					if(mergeCategory != null) {
						slice.merge(mergeCategory, head);
					} else if(!slice.containedIn(head)){
						leftover.add(slice);
					}
				}
				
				reduced.add(head);
				slices = leftover;	
			}
			
			slices = reduced;
		
		}
	
		return reduced;
	}
}