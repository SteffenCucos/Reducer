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
public abstract class RecursiveReducer<T> extends AbstractReducer<T> {

	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		// TODO Auto-generated method stub
		return reduceRecursive(slices, buildCollectionCounts(slices), getWidth(slices), slices.size(), 0);
	}	
	
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
			return (count > other.count) ? -1 : (count == otherCount) ? 0 : 1;
		}
	}
	
	PriorityQueue<CollectionNode> buildCollectionCounts(Set<Slice<T>> slices) {
		PriorityQueue<CollectionNode> queue = new PriorityQueue<>();
		
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
	
	@SuppressWarnings("unchecked")
	private Set<Slice<T>> reduceRecursive(Set<Slice<T>> slices, PriorityQueue<CollectionNode> collectionsQueue, int width, int prevSize, int secondTry) {
		if(slices.isEmpty() || slices.size() == 1 || width == 0) {
			return slices;
		}
		
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
			
			return new HashSet() {{
				add(new Slice<T>(
						new HashMap() {{
							put(category, objects);
						}}
				));
			}};
		}
		 
		CollectionNode mostNode = collectionsQueue.poll();
		
		Collection<?> mostObjects = mostNode.objects;
		Object category = mostNode.category;
		Integer count = mostNode.count;
		
		if(count == 1) { // No merging is possible, all collections are unique
			return slices;
		}
		
		Map<Boolean, List<Slice<T>>> partition = slices.stream().collect(Collectors.partitioningBy(s -> s.getEntry(category).equals(mostObjects)));
		
		Set<Slice<T>> slicesContainingMost = partition.get(true)
				.stream()
				.map(s -> { 
					s.deleteEntry(category); //Remove this section of the slice to reduce width
					return s;
				})
				.collect(Collectors.toSet());
		
		Set<Slice<T>> reduced = reduceRecursive(slicesContainingMost, buildCollectionCounts(slicesContainingMost), width - 1, slicesContainingMost.size(), 0)
				.stream()
				.map(r -> {
					r.addEntry(category, mostObjects); // Get the width back to normal
					return r;
				})
				.collect(Collectors.toSet());
		
		reduced.addAll(partition.get(false));
		
		if(reduced.size() == prevSize && secondTry == 0) {
			reduced = reduceRecursive(reduced, collectionsQueue, width, reduced.size(), 1); // Try again, there may be a better splitting
		}
		
		if(reduced.size() < prevSize || prevSize == 0) {
			return reduceRecursive(reduced, buildCollectionCounts(reduced), width, reduced.size(), 0);
		}
		
		return reduced;
	}
	
	private int getWidth(Set<Slice<T>> slices) {
		if(slices.size() == 0) {
			return 0;
		}
		return slices.iterator().next().getWidth();
	}
}
