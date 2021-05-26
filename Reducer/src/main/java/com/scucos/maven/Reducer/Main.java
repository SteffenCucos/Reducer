package com.scucos.maven.Reducer;

import java.util.HashSet;
import java.util.Set;

import com.scucos.maven.Reducer.Reducers.NewRecursiveReducer;
import com.scucos.maven.Reducer.Reducers.RecursiveReducer;
import com.scucos.maven.Reducer.Reducers.Reducer;

public class Main {

	public static Set<Region> makeLargeRegion(int multiplier, boolean sparse) {
		Set<Region> cube = new HashSet<>();
		if(sparse) {
			for(int i = 0; i < multiplier*150*150; i++) {
				cube.add(new Region(String.valueOf(i), String.valueOf(i)+ "asd", i));
				cube.add(new Region(String.valueOf(i), String.valueOf(i)+ "asd", i+1));
			}
		} else {
			for(int x = 0; x < multiplier; x++) {
				for(int y = 0; y < 150; y++) {
					for(int z = 0; z < 150; z++) {
						cube.add(new Region(String.valueOf(x), "a"+String.valueOf(y), z));
					}
				}
			}
		}
		
		return cube;
	}
	
	public static void main(String[] args) {
		
		Reducer<Region> recursiveReducer = new RecursiveReducer<Region>(){};
		
		Reducer<Region> newRecursiveReducer = new NewRecursiveReducer<Region>() {};
	 	

		System.out.println("Recursive 3 Dimensional Full Cube\n");
		//time(recursiveReducer, false);
		
		System.out.println("\nNew Recursive 3 Dimensional Full Cube\n");
		time(newRecursiveReducer, true);
	}
	
	public static void time(Reducer<Region> reducer, boolean sparse) {
		for(int i = 1; i < 5; i++) {
			Set<Region> large = makeLargeRegion(i, sparse);
			reducer.reduce(large);
		}
	}
	
	public static class Region {
		Set<String> countries;
		Set<String> cities;
		Set<Integer> populations;
		
		public Region() {
			
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes", "serial" } )
		public Region(String country, String city, Integer population) {
			this.countries = new HashSet() {{ add(country); }};
			this.cities = new HashSet() {{ add(city); }};
			this.populations = new HashSet() {{ add(population); }};
		}
		
		public Region(Set<String> countries, Set<String> cities, Set<Integer> populations) {
			this.countries = countries;
			this.cities = cities;
			this.populations = populations;
		}
		
		@Override
		public String toString() {
			return String.format("\nCountires: %s States: %s\nCities: %s Populations: %s", countries, "N/A", cities, populations);
		}
	}
}