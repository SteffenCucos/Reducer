package com.scucos.maven.Reducer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vena.service.utils.TwoTuple;

import com.scucos.maven.Reducer.Slice.ObjectConstructionException;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;

public class Main {

	public static Set<Region> makeLargeRegion(int multiplier) {
		Set<Region> cubicRegion = new HashSet<>();
		for(int x = 0; x < multiplier; x++) {
			for(int y = 0; y < 300; y++) {
				for(int z = 0; z < 300; z++) {
					cubicRegion.add(new Region(String.valueOf(x), String.valueOf(y), z));
				}
			}
		}
		
		return cubicRegion;
	}
	
	@SuppressWarnings("unchecked" )
	public static void main(String[] args) {
		//If you have your entities as Maps of categories -> objects 
		MapReducer<Integer, Collection<Long>> mapIntegerLongReducer = new MapReducer<Integer, Collection<Long>>();
		
		//If you want to use reflection to merge all fields that are collections
		Reducer<Region> recursiveReducer = new RecursiveReducer<Region>() {
			@Override
			public Slice<Region> toSlice(Region region) throws SliceConstructionException {
				return new Slice<>(region); // Reflection based toSlice
			}
			
			@Override
			public Region fromSlice(Slice<Region> slice) throws ObjectConstructionException {
				return slice.toType(Region.class); // Reflection based fromSlice 
			}
		};
		
		//If you want to use custom logic for reducing your entities
		Reducer<Region> distanceReducer = new DistanceReducer<Region>() {
			@Override
			public Slice<Region> toSlice(Region region) throws SliceConstructionException {
				return new Slice<Region>() {{
					addEntry("countries", region.countries);
					addEntry("cities", region.cities);
					addEntry("populations", region.populations);
				}};
			}
			
			@Override
			public Region fromSlice(Slice<Region> slice) throws ObjectConstructionException {
				Set<String> countries = Slice.getFieldAsType("countries", slice, new HashSet<>());
				//Set<String> states = Slice.getAsType("states", slice, new HashSet<>());
				Set<String> cities = Slice.getFieldAsType("cities", slice, new HashSet<>());
				Set<Integer> populations = Slice.getFieldAsType("populations", slice, new HashSet<>());
				return new Region(countries, cities, populations);
			}
		};
	 	
		System.out.println("Recursive");
		time(recursiveReducer);
		
		System.out.println("Distance");
		time(distanceReducer);
		
		return;
	}
	
	public static void time(Reducer<Region> reducer) {
		for(int i = 1; i < 4; i++) {
			Set<Region> large = makeLargeRegion(i);
			Set<Region> reduced = reducer.reduce(large);
		}
	}
	
	public static class Region {
		public Set<String> countries;
		//public Set<String> states;
		public Set<String> cities;
		public Set<Integer> populations;
		
		public Region() {
			
		}
		
		@SuppressWarnings("unchecked" )
		public Region(String country, String city, Integer population) {
			this.countries = new HashSet() {{ add(country); }};
			//this.states = new HashSet() {{ add(state); }};
			this.cities = new HashSet() {{ add(city); }};
			this.populations = new HashSet() {{ add(population); }};
		}
		
		public Region(Set<String> countries, Set<String> cities, Set<Integer> populations) {
			this.countries = countries;
			//this.states = states;
			this.cities = cities;
			this.populations = populations;
		}
		
		@Override
		public String toString() {
			return String.format("\nCountires: %s States: %s\nCities: %s Populations: %s", countries, "N/A", cities, populations);
		}
	}
}

/**
 * 	public Set<Map<CategoryType, Set<EntityType>>> mergeDistance(Set<Map<CategoryType, Set<EntityType>>> slices ) {
		Set<Map<CategoryType, Set<EntityType>>> reduced = new HashSet<>();
		int startSize = slices.size();
		
		while(slices.size() > 0) {
			Map<CategoryType, Set<EntityType>> head = slices.stream().iterator().next();
			slices.remove(head);
			
			Set<Map<CategoryType, Set<EntityType>>> leftOver = new HashSet<>();
			for(Map<CategoryType, Set<EntityType>> slice : slices) {
				TwoTuple<Integer, Integer> diffTuple = difference(head, slice);
				int headIntoSlice = diffTuple.getO1();
				int sliceIntoHead = diffTuple.getO2();
				
				if(headIntoSlice == 0 && sliceIntoHead == 1) {
					//Slice is completely contained by head
					//We can ignore it
				} else if (headIntoSlice == 1 && sliceIntoHead == 0) {
					//Head is contained completely by slice
					//Replace head with slice?
					head = slice;
				} else if (headIntoSlice == 1 && sliceIntoHead == 1) {
					//They differ in one spot exactly
					head = mergeInto(slice, head);
				} else {
					leftOver.add(slice);
				}
			}
			reduced.add(head);
			slices = leftOver;
		}

		if(reduced.size() < startSize) {
			return mergeDistance(reduced);
		}
		
		return reduced;
	}
	
	public TwoTuple<Integer, Integer> difference(Map<CategoryType, Set<EntityType>> s1, Map<CategoryType, Set<EntityType>> s2) {
		Set<CategoryType> categories = s1.keySet();
		int S1IntoS2 = categories.size();
		int S2IntoS1 = categories.size();
		
		for(CategoryType category : categories) {
			Set<EntityType> e1 = s1.get(category);
			Set<EntityType> e2 = s2.get(category);
			if(e1.containsAll(e2)) {
				S2IntoS1 -= 1;
			}
			if(e2.containsAll(e1)) {
				S1IntoS2 -= 1;
			}
		}
		
		return new TwoTuple<Integer, Integer>(S1IntoS2, S2IntoS1);
	}
	
	private Map<CategoryType, Set<EntityType>> mergeInto(Map<CategoryType, Set<EntityType>> s1, Map<CategoryType, Set<EntityType>> s2) {
		Set<CategoryType> categories = s1.keySet();
		Map<CategoryType, Set<EntityType>> mergedSlice = new HashMap<>();
		for(CategoryType category : categories) {
			Set<EntityType> e1 = s1.get(category);
			Set<EntityType> e2 = s2.get(category);
			@SuppressWarnings("unchecked")
			Set<EntityType> merged = new HashSet() {{
				addAll(e1);
				addAll(e2);
			}};
			mergedSlice.put(category, merged);
		}
		return mergedSlice;
	} */
