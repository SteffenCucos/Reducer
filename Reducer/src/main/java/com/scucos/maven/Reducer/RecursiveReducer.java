package com.scucos.maven.Reducer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
public abstract class RecursiveReducer<T> implements Reducer<T> {

	private static boolean CONTAINS_MOST = true;
	private static boolean WITHOUT_MOST = false;
	
	static long calls = 0;
	
	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		Set<Slice<T>> reduced = reduceRecursive(slices, getWidth(slices));
		
		System.out.println(String.format("%s calls to buildQueue", calls));
		calls = 0;
		return reduced;
	}	
	
	/**
	 * Container class that's used to track a Collection of objects, 
	 * what category they belong to, and the collections count across all slices
	 * @author SCucos
	 */
	public class CollectionNode implements Comparable<CollectionNode> {
		public Collection<?> objects;
		public Object category;
		public Integer count;
		
		public CollectionNode(Collection<?> objects, Object category, Integer count) {
			this.objects = objects;
			this.category = category;
			this.count = count;
		}

		@Override
		public int compareTo(CollectionNode other) {
			Integer otherCount = other.count;
			return (count > otherCount) ? -1 : (count == otherCount) ? 0 : 1;
		}
	}
	
	/**
	 * Constructs a PriorityQueue<CollectionNode> that is ordered by the nodes count parameter. 
	 * The queue will contain a node for each unique Collection<?> of objects across all of 
	 * the supplied slices categories. O(slice.width * slices.size())
	 * @param slices
	 * @return
	 */
	PriorityQueue<CollectionNode> buildCollectionCounts(Set<Slice<T>> slices) {
		calls += 1;
		PriorityQueue<CollectionNode> queue = new PriorityQueue<>();
		
		if(slices.size() == 0) {
			return queue;
		}
		
		Map<Collection<?>, Integer> collectionCountMap = new HashMap<>();
		Map<Collection<?>, Object> collectionCategoryMap = new HashMap<>();
		
		
		Set<Object> categories = slices.iterator().next().getCategories();
		for(Slice<T> slice : slices) {
			for(Object category : categories) {
				Collection<?> objects = slice.getEntry(category);
				collectionCountMap.put(objects, collectionCountMap.getOrDefault(objects, 0) + 1);
				if(!collectionCategoryMap.containsKey(objects)) {
					collectionCategoryMap.put(objects, category);
				}
			}
		}
		
		for(Entry<Collection<?>, Object> partialNode : collectionCategoryMap.entrySet()) {
			Collection<?> objects = partialNode.getKey();
			Object category = partialNode.getValue();
			Integer count = collectionCountMap.get(objects);
			
			queue.add(new CollectionNode(objects, category, count));
		}
		
		return queue;
	}
	
	/**
	 * The main algorithm used for reducing slices.
	 * Several invariants must be continually satisfied 
	 * for this to produce correct (or coherent) results, 
	 * and many bugs such as NPEs are possible when these invariants fail
	 * Invariants:
	 * 	1) All Slice<T>s provided in the slices argument have the same number of and types of categories
	 *  2) collectionsQueue was computed using the incoming slices (the counts are correct)
	 *  3) width is equal to getWidth(slices)
	 *  
	 *  Calls to the method reduceSlices(Set<Slice<T>> slices) should 
	 *  always run successfully and will maintain the invariants properly.
	 * 
	 * Another important aspect of this algorithm is that it works 
	 * best when the initial slices are all point slices (that is 
	 * they are slices that "unravel" into a single point)
	 * It's this assumption that allows us to get fairly linear runtime, 
	 * whereas a more comprehensive algorithm that could work on slices 
	 * in general may have worse runtime characteristics.
	 * 
	 * @param slices
	 * @param collectionsQueue
	 * @param width
	 * @param secondTry
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Set<Slice<T>> reduceRecursive(Set<Slice<T>> slices, int width) {
		if(slices.isEmpty() || slices.size() == 1 || width == 0) {
			return slices;
		}
		
		PriorityQueue<CollectionNode> collectionsQueue = buildCollectionCounts(slices);
		
		if(width == 1) {
			Object category = collectionsQueue.poll().category;
			Collection<Object> objects = slices
					.stream()
					.map(s -> (Collection<Object>)s.getEntry(category))
					.reduce(null, (accumulator, currentObjects) -> {
						if(accumulator == null) {
							return currentObjects;
						}
						accumulator.addAll(currentObjects);
						return accumulator;
					});
			
			
			Map<Object, Collection<?>> sliceMap = new HashMap<>();
			sliceMap.put(category, objects);
			Slice<T> mergedSlice = new Slice<T>(sliceMap);
			Set<Slice<T>> mergedSliceSet = new HashSet<>();
			mergedSliceSet.add(mergedSlice);
			
			return mergedSliceSet;
		}
		
		int prevSize = slices.size();
		
		CollectionNode mostNode = collectionsQueue.poll();
		Collection<?> mostObjects = mostNode.objects;
		Object mostCategory = mostNode.category;
		Integer mostCount = mostNode.count;
		
		if(mostCount == 1) { // No merging is possible, all collections are unique
			return slices;
		}
		
		Map<Boolean, List<Slice<T>>> partition = slices.stream().collect(Collectors.partitioningBy(s -> s.getEntry(mostCategory).equals(mostObjects)));
		
		Set<Slice<T>> slicesContainingMost = partition.get(CONTAINS_MOST)
				.stream()
				.map(s -> { 
					//Remove this section of the slice to reduce width
					s.deleteEntry(mostCategory);
					return s;
				})
				.collect(Collectors.toSet());
		
		Set<Slice<T>> slicesWithoutMost = partition.get(WITHOUT_MOST)
				.stream()
				.collect(Collectors.toSet());
		
		Set<Slice<T>> reducedMost = reduceRecursive(slicesContainingMost, width - 1)
				.stream()
				.map(r -> {
					// Get the width back to normal
					r.addEntry(mostCategory, mostObjects);
					return r;
				})
				.collect(Collectors.toSet());
		
		Set<Slice<T>> reduceWithoutMost = reduceRecursive(slicesWithoutMost, width);
		
		Set<Slice<T>> reduced = new HashSet<Slice<T>>();
		reduced.addAll(reducedMost);
		reduced.addAll(reduceWithoutMost);
		
		if(reduced.size() < prevSize || prevSize == 0) {
			return reduceRecursive(reduced, width);
		}
		
		return reduced;
	}
	
	/**
	 * Helper method for getting the number of categories in the slices (width)
	 * @param slices
	 * @return
	 */
	private int getWidth(Set<Slice<T>> slices) {
		if(slices.size() == 0) {
			return 0;
		}
		return slices.iterator().next().getWidth();
	}
}
