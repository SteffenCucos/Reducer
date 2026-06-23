package com.scucos.maven.Reducer.Reducers;

import java.util.Set;

import com.scucos.maven.Reducer.Slice;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Greedy reducer that repeatedly chooses a head slice, compares it with every
 * remaining slice, and merges slices that differ in exactly one category.
 *
 * Runtime complexity:
 * Let n be the number of input slices, d be the number of categories per slice,
 * and p be the number of outer recursive passes needed until no additional
 * merges are found. One pass compares each chosen head against the remaining
 * queue, which is O(n^2) pairwise comparisons in the worst case. Each comparison
 * calls containment / asymmetric-difference logic over the slice dimensions, so
 * the practical cost is O(d) plus the cost of comparing each category's
 * collection values. Expected worst-case runtime is therefore O(p * n^2 * d)
 * when category collection comparisons are treated as constant-time or bounded.
 * Space usage is O(n) for the priority queue, leftover set, and reduced set.
 *
 * The reducer performs best when many slices merge early, reducing n between
 * passes. If few or no merges are possible, it behaves like a quadratic scan.
 */
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
