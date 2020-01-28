package com.scucos.maven.Reducer;

import java.util.Set;
import java.util.HashSet;

/**
 * Abstract implementation that provides a recursive merging algorithm for turning a Set<Slice<T>> into a fully reduced Set<Slice<T>>.
 * It accomplishes this by describing a N dimensional space (one dimensions for each unique category in a Slice<T>) and trying to form
 * the largest possible sub-regions of the space with no overlap.
 * 
 * The Runtime is ~proportional to the N dimensional volume of the smallest super-slice 
 * that contains all the incoming slices times the density of the contained points ( 0 < density <= 1 )
 * For example if the majority of the contained points are approximately contained in one quarter of the 
 * the smallest containing super-slice, it has a density of ~0.25 and thus runs 4x faster than if the
 * same super-slice had a density of ~1.0.
 * @author SCucos
 *
 * @param <T> The entity type that will be merged
 */
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