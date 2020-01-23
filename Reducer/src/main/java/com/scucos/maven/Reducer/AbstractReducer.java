package com.scucos.maven.Reducer;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import com.scucos.maven.Reducer.Slice.ObjectConstructionException;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;

public abstract class AbstractReducer<T extends Reduceable<T>> implements Reducer<T> {

	@Override
	public Set<T> reduce(Set<T> ts) {
		Set<Slice<T>> slices = ts
				.stream()
				.map(t -> {
					try {
						return t.toSlice();
					} catch (SliceConstructionException e) {
						throw e;
					}
				})
				.collect(Collectors.toSet());
		
		
		Set<Slice<T>> reduced = reduce(slices, buildCollectionCounts(slices), getWidth(slices), slices.size());
		
		return reduced
				.stream()
				.map(s -> {
					try {
						return fromSlice(s);
					} catch (ObjectConstructionException e) {
						throw e;
					}
				})
				.collect(Collectors.toSet());
	}
	
	public class CollectionNode implements Comparable<CollectionNode> {
		public Collection<Object> objects;
		public String category;
		public Integer count;
		
		public CollectionNode(Collection<Object> objects, String category, Integer count) {
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
		
		Map<Collection<Object>, Integer> collectionCountMap = new HashMap<>();
		Map<Collection<Object>, String> collectionCategoryMap = new HashMap<>();
		
		
		Set<String> categories = slices.iterator().next().categories;
		for(Slice<T> slice : slices) {
			for(String category : categories) {
				Collection<Object> objects = slice.getEntry(category);
				collectionCountMap.put(objects, collectionCountMap.getOrDefault(objects, 0) + 1);
				if(!collectionCategoryMap.containsKey(objects)) {
					collectionCategoryMap.put(objects, category);
				}
			}
		}
		
		for(Entry<Collection<Object>, String> partialNode : collectionCategoryMap.entrySet()) {
			Collection<Object> objects = partialNode.getKey();
			String category = partialNode.getValue();
			Integer count = collectionCountMap.get(objects);
			
			queue.add(new CollectionNode(objects, category, count));
		}
		
		return queue;
	}
	
	@SuppressWarnings("unchecked")
	private Set<Slice<T>> reduce(Set<Slice<T>> slices, PriorityQueue<CollectionNode> collectionsQueue, int width, int prevSize) {
		if(slices.isEmpty()) { 
			return slices;
		}
		
		if(slices.size() == 1) {
			return slices;
		}
		
		if(width == 0) {
			return slices;
		}
		
		Set<Slice<T>> reduced = new HashSet<>();
		
		if(width == 1) {
			Set<Object> objects = new HashSet<>();
			String category = slices.iterator().next().categories.iterator().next();
			for(Slice<T> slice : slices) {
				objects.addAll(slice.getEntry(category));
			}
			
			reduced.add(
				new Slice<T>(
					new HashMap() {{
						put(category, objects);
					}}
				));
			
			return reduced;
		}
		 
		CollectionNode mostNode = collectionsQueue.poll();
		
		Collection<Object> mostObjects = mostNode.objects;
		String category = mostNode.category;
		Integer count = mostNode.count;
		
		if(count == 1) {
			//No merging is possible, all collections are unique
			return slices;
		}
		
		Set<Slice<T>> slicesContainingMost = new HashSet<>();
		Set<Slice<T>> slicesWithoutMost = new HashSet<>();
		
		for(Slice<T> slice : slices) {
			if(slice.getEntry(category).equals(mostObjects)) {
				slice.deleteEntry(category);
				slicesContainingMost.add(slice);
			} else {
				slicesWithoutMost.add(slice);
			}
		}
		
		PriorityQueue<CollectionNode> otherCategories = new PriorityQueue<>();
		for(CollectionNode node : collectionsQueue) {
			if(!node.category.equals(category)) {
				otherCategories.add(node);
			}
		}
		
		Set<Slice<T>> reducedSubSlices = reduce(slicesContainingMost, otherCategories, width - 1, slicesContainingMost.size());
		for(Slice<T> reducedSubSlice : reducedSubSlices) {
			//Get the width back to normal
			reducedSubSlice.addEntry(category, mostObjects);
		}
		reduced.addAll(reducedSubSlices);
		reduced.addAll(slicesWithoutMost);
		
		if(reduced.size() < prevSize || prevSize == 0) {
			return reduce(reduced, buildCollectionCounts(reduced), width, reduced.size());
		}
		
		return reduced;
	}
	
	private int getWidth(Set<Slice<T>> slices) {
		if(slices.size() == 0) {
			return 0;
		}
		return slices.iterator().next().width;
	}
	
	
	
	
	
}
