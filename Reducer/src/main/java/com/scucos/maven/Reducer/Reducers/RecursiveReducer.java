package com.scucos.maven.Reducer.Reducers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.scucos.maven.Reducer.Slice;

/**
 * Abstract implementation that provides a recursive merging algorithm for turning a Set<Slice<T>> into a fully reduced Set<Slice<T>>.
 * It accomplishes this by describing a N dimensional space (one dimensions for each unique category in a Slice<T>) and trying to form
 * the largest possible sub-regions of the space with no overlap.
 * 
 * Runtime complexity:
 * Let n be the number of input slices, d be the number of categories per slice,
 * U_i be the number of unique values/collections in category i, and
 * V = product(U_i) be the N-dimensional volume of the smallest super-slice that
 * contains the input. For point-slice inputs with uniform category structure,
 * the algorithm recursively partitions by the most common category entry and
 * rebuilds collection counts at each recursive level. The expected work is
 * roughly proportional to the explored portion of that containing volume. Dense
 * inputs approach O(V * d), while sparse inputs run closer to O(n * d) when the
 * recursion quickly proves that no larger complete regions can be formed.
 *
 * In the worst case, repeated recounting and repeated recursive passes after a
 * successful merge can add extra scans over the working sets. A conservative
 * upper bound is O(p * n * d * r), where p is the number of merge/retry passes
 * and r is the number of recursive partition calls. For the intended point-slice
 * workload, V and density are more useful predictors than raw n alone.
 *
 * Space usage is O(n * d) for the working slices, partitions, and collection
 * count structures, excluding the storage already held by each category value.
 *
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
		
		//System.out.println(String.format("%s calls to buildQueue", calls));
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
		public String category;
		public Integer count;
		
		public CollectionNode(Collection<?> objects, String category, Integer count) {
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
		Map<Collection<?>, String> collectionCategoryMap = new HashMap<>();
		
		
		Set<String> categories = slices.iterator().next().getCategories();
		for(Slice<T> slice : slices) {
			for(String category : categories) {
				Collection<?> objects = slice.getEntry(category);
				collectionCountMap.put(objects, collectionCountMap.getOrDefault(objects, 0) + 1);
				if(!collectionCategoryMap.containsKey(objects)) {
					collectionCategoryMap.put(objects, category);
				}
			}
		}
		
		for(Entry<Collection<?>, String> partialNode : collectionCategoryMap.entrySet()) {
			Collection<?> objects = partialNode.getKey();
			String category = partialNode.getValue();
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
	private Set<Slice<T>> reduceRecursive(Set<Slice<T>> slices, int width) {
		if(slices.isEmpty() || slices.size() == 1 || width == 0) {
			return slices;
		}
		
		PriorityQueue<CollectionNode> collectionsQueue = buildCollectionCounts(slices);
		
		if(width == 1) {
			Object category = collectionsQueue.poll().category;
			Slice<T> reduced = slices.stream().reduce(
					null, 
					(accumulator, current) -> {
						if(accumulator == null) {
							return current;
						}
						
						accumulator.unionAdd(category, current);
						return accumulator;
					}
			);

			return ImmutableSet.of(reduced);
		}
		
		int prevSize = slices.size();
		
		CollectionNode mostNode = collectionsQueue.poll();
		Collection<?> mostObjects = mostNode.objects;
		String mostCategory = mostNode.category;
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
