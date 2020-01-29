package com.scucos.maven.Reducer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.scucos.maven.Reducer.Slice.ObjectConstructionException;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;

public class Main {

	public static Set<Region> makeLargeRegion(int multiplier) {
		Set<Region> cubicRegion = new HashSet<>();
		for(int x = 0; x < multiplier; x++) {
			for(int y = 0; y < 200; y++) {
				for(int z = 0; z < 200; z++) {
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
		
		Reducer<Region> distanceReducerV2 = new DistanceReducerV2<Region>() {
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
	 	

		
		System.out.println("Distance");
		time(distanceReducer);
		
		System.out.println("DistanceV2");
		time(distanceReducerV2);
		
		System.out.println("Recursive");
		time(recursiveReducer);
		
		return;
	}
	
	public static void time(Reducer<Region> reducer) {
		for(int i = 1; i < 6; i++) {
			Set<Region> large = makeLargeRegion(i);
			Set<Region> reduced = reducer.reduce(large);
			int size = reduced.size();
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