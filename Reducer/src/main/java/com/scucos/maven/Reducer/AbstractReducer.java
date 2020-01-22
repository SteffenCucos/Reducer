package com.scucos.maven.Reducer;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public abstract class AbstractReducer<T extends Mergeable<T>> implements Reducer<T> {

	@Override
	public Set<T> reduce(Set<T> ts) {
		Set<Map<Object,Collection<Object>>> slices = ts
				.stream()
				.map(t -> t.toMap())
				.collect(Collectors.toSet());
		
		
		Set<Map<Object,Collection<Object>>> reduced = reduce(slices, buildCollectionCounts(slices), getWidth(slices), slices.size());
		
		return reduced
				.stream()
				.map(s -> fromMap(s))
				.collect(Collectors.toSet());
	}
	
	public class CollectionNode implements Comparable<CollectionNode> {
		public Collection<Object> objects;
		public Object category;
		public Integer count;
		
		public CollectionNode(Collection<Object> objects, Object category, Integer count) {
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
	
	PriorityQueue<CollectionNode> buildCollectionCounts(Set<Map<Object,Collection<Object>>> slices) {
		PriorityQueue<CollectionNode> queue = new PriorityQueue<>();
		
		Map<Collection<Object>, Integer> collectionCountMap = new HashMap<>();
		Map<Collection<Object>, Object> collectionCategoryMap = new HashMap<>();
		
		
		Set<Object> categories = slices.iterator().next().keySet();
		for(Map<Object,Collection<Object>> slice : slices) {
			for(Object category : categories) {
				Collection<Object> objects = slice.get(category);
				collectionCountMap.put(objects, collectionCountMap.getOrDefault(objects, 0) + 1);
				if(!collectionCategoryMap.containsKey(objects)) {
					collectionCategoryMap.put(objects, category);
				}
			}
		}
		
		for(Entry<Collection<Object>, Object> partialNode : collectionCategoryMap.entrySet()) {
			Collection<Object> objects = partialNode.getKey();
			Object category = partialNode.getValue();
			Integer count = collectionCountMap.get(objects);
			
			queue.add(new CollectionNode(objects, category, count));
		}
		
		return queue;
	}
	
	@SuppressWarnings("unchecked")
	private Set<Map<Object,Collection<Object>>> reduce(Set<Map<Object,Collection<Object>>> slices, PriorityQueue<CollectionNode> collectionsQueue, int width, int prevSize) {
		if(slices.isEmpty()) { 
			return slices;
		}
		
		if(slices.size() == 1) {
			return slices;
		}
		
		if(width == 0) {
			return slices;
		}
		
		Set<Map<Object,Collection<Object>>> reduced = new HashSet<>();
		
		if(width == 1) {
			Set<Object> objects = new HashSet<>();
			Object category = slices.iterator().next().keySet().iterator().next();
			for(Map<Object,Collection<Object>> slice : slices) {
				objects.addAll(slice.get(category));
			}
			
			reduced.add(
				new HashMap() {{
					put(category, objects);
				}});
			
			return reduced;
		}
		 
		CollectionNode mostNode = collectionsQueue.poll();
		
		Collection<Object> mostObjects = mostNode.objects;
		Object category = mostNode.category;
		Integer count = mostNode.count;
		
		if(count == 1) {
			//No merging is possible, all collections are unique
			return slices;
		}
		
		Set<Map<Object,Collection<Object>>> slicesContainingMost = new HashSet<>();
		Set<Map<Object,Collection<Object>>> slicesWithoutMost = new HashSet<>();
		
		for(Map<Object,Collection<Object>> slice : slices) {
			if(slice.get(category).equals(mostObjects)) {
				slice.remove(category);
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
		
		Set<Map<Object,Collection<Object>>> reducedSubSlices = reduce(slicesContainingMost, otherCategories, width - 1, slicesContainingMost.size());
		for(Map<Object,Collection<Object>> reducedSubSlice : reducedSubSlices) {
			//Get the width back to normal
			reducedSubSlice.put(category, mostObjects);
		}
		reduced.addAll(reducedSubSlices);
		reduced.addAll(slicesWithoutMost);
		
		if(reduced.size() < prevSize || prevSize == 0) {
			return reduce(reduced, buildCollectionCounts(reduced), width, reduced.size());
		}
		
		return reduced;
	}
	
	private int getWidth(Set<? extends Map<?,?>> slices) {
		if(slices.size() == 0) {
			return 0;
		}
		return slices.iterator().next().size();
	}
	
	
	
	
	
}
