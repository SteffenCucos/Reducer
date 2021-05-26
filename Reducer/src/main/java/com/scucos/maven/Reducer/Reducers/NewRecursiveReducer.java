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
 * Implementation that provides a recursive merging algorithm for turning a Set<Slice<T>> into a fully reduced Set<Slice<T>>.
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
public abstract class NewRecursiveReducer<T> implements Reducer<T> {

	private static boolean CONTAINS_MOST = true;
	private static boolean WITHOUT_MOST = false;
	
	static long calls = 0;
	
	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		Set<Slice<T>> reduced = reduceRecursive(slices, buildCollectionCounts(slices).o2, getWidth(slices)).o1;
		
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
		
		@Override
		public String toString() {
			return category.toString() + ":" + objects.toString() + " " + count.toString();
		}
	}
	
	public class Tuple<T1,T2> {
		T1 o1;
		T2 o2;
		
		public Tuple(T1 o1, T2 o2) {
			this.o1 = o1;
			this.o2 = o2;
		}
		
		public void setO1(T1 o1) {
			this.o1 = o1;
		}
		
		public void setO2(T2 o2) {
			this.o2 = o2;
		}
		
		@Override
		public String toString() {
			return "O1: " + o1.toString() + " O2: " + o2.toString();
		}
	}
	
	/**
	 * Constructs a PriorityQueue<CollectionNode> that is ordered by the nodes count parameter. 
	 * The queue will contain a node for each unique Collection<?> of objects across all of 
	 * the supplied slices categories. O(slice.width * slices.size())
	 * @param slices
	 * @return
	 */
	Tuple<PriorityQueue<CollectionNode>, Map<Collection<?>, Tuple<Integer, Object>>> buildCollectionCounts(Set<Slice<T>> slices) {
		calls += 1;
		PriorityQueue<CollectionNode> queue = new PriorityQueue<>();
		
		Map<Collection<?>, Tuple<Integer, Object>> map = new HashMap<>();
		
		if(slices.size() == 0) {
			return new Tuple<>(queue, map);
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
			
			map.put(objects, new Tuple<>(count, category));
			
			queue.add(new CollectionNode(objects, category, count));
		}
		
		return new Tuple<>(queue, map);
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
	private Tuple<Set<Slice<T>>, Map<Collection<?>, Tuple<Integer, Object>>> reduceRecursive(Set<Slice<T>> slices, final Map<Collection<?>, Tuple<Integer, Object>> map, int width) {
		if(slices.isEmpty() || slices.size() == 1 || width == 0) {
			return new Tuple<>(slices, map);
		}
		
		if(width == 1) {
			Object category = null;
			
			for(Slice<T> slice : slices) {
				category = slice.getCategories().stream().findAny().get();
				Tuple<Integer, Object> tuple = map.get(slice.getEntry(category));
				tuple.setO1(tuple.o1 - 1);
			}
			
			final Object finalCategory = category;
			
			final Collection<Object> lost = new HashSet<>();
			
			Slice<T> reduced = slices.stream().reduce(
					null, 
					(accumulator, current) -> {
						if(accumulator == null) {
							map.remove(current.getEntry(finalCategory));
							lost.addAll(current.getEntry(finalCategory));
							return current;
						}
						
						accumulator.unionAdd(finalCategory, current);
						return accumulator;
					}
			);
			map.put(reduced.getEntry(finalCategory), new Tuple<>(1, finalCategory));
			map.put(lost, new Tuple<>(0, finalCategory));
			
			return new Tuple<>(ImmutableSet.of(reduced), map);
		}
		
		Collection<?> mostObjects = null;
		Object mostCategory = null;
		Integer mostCount = 0; 
		
		for(Entry<Collection<?>, Tuple<Integer, Object>> entry : map.entrySet()) {
			int size = entry.getValue().o1;
			if(size > mostCount) {
				mostCount = size;
				mostObjects = entry.getKey();
				mostCategory = entry.getValue().o2;
			}
		}
		
		if(mostCount <= 1) { // No merging is possible, all collections are unique
			return new Tuple<>(slices, map);
		}
		
		final Object finalMostCategory = mostCategory;
		final Collection<?> finalMostObjects = mostObjects;
		
		Map<Boolean, List<Slice<T>>> partition = slices.stream().collect(Collectors.partitioningBy(s -> s.getEntry(finalMostCategory).equals(finalMostObjects)));
		
		Set<Slice<T>> slicesContainingMost = partition.get(CONTAINS_MOST)
				.stream()
				.map(s -> { 
					//Remove this section of the slice to reduce width
					s.deleteEntry(finalMostCategory);
					return s;
				})
				.collect(Collectors.toSet());
		
		Set<Slice<T>> slicesWithoutMost = partition.get(WITHOUT_MOST)
				.stream()
				.collect(Collectors.toSet());
		
		
		Tuple<Integer, Object> addBack = map.remove(mostObjects);
		
		for(Slice<T> slice : slicesWithoutMost) {
			for(Collection<?> collection : slice.getMap().values()) {
				Tuple<Integer, Object> innerTuple = map.get(collection);
				innerTuple.setO1(innerTuple.o1 - 1);
				if(innerTuple.o1 <= 0) {
					
				}
			}
		}

		
		int mostBefore = slicesContainingMost.size();
		
		Tuple<Set<Slice<T>>, Map<Collection<?>, Tuple<Integer, Object>>> reducedMostResult = reduceRecursive(slicesContainingMost, map, width - 1);
		
		Set<Slice<T>> reducedMost = reducedMostResult
				.o1
				.stream()
				.map(r -> {
					// Get the width back to normal
					r.addEntry(finalMostCategory, finalMostObjects);
					return r;
				})
				.collect(Collectors.toSet());
		
		int mostAfter = reducedMost.size();
		
		if(mostBefore == mostAfter) {
			
			for(Slice<T> slice : reducedMost) {
				for(Collection<?> collection : slice.getMap().values()) {
					if(collection != mostObjects) {
						Tuple<Integer, Object> innerTuple = map.get(collection);
						innerTuple.setO1(innerTuple.o1 - 1);
					}

				}
			}
			
			Set<Slice<T>> reducedWithout = reduceRecursive(slicesWithoutMost, map, width).o1;
			
			reducedWithout.addAll(reducedMost);
			
			return new Tuple<>(reducedWithout, map);
			
			
			
		} else {
			addBack.setO1(reducedMost.size());
			map.put(mostObjects, addBack);
			
			for(Slice<T> slice : slicesWithoutMost) {
				for(Collection<?> collection : slice.getMap().values()) {
					Tuple<Integer, Object> innerTuple = map.get(collection);
					innerTuple.setO1(innerTuple.o1 + 1);
				}
			}
			
			reducedMost.addAll(slicesWithoutMost);
			
			return reduceRecursive(reducedMost, map, width);
		}
	}
	
	/**
	 * Helper method for getting the number of categories in the slices (width)
	 * @param slices
	 * @return
	 */
	private int getWidth(Collection<Slice<T>> slices) {
		if(slices.size() == 0) {
			return 0;
		}
		return slices.iterator().next().getWidth();
	}
}