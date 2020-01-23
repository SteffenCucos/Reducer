package com.scucos.maven.Reducer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.scucos.maven.Reducer.Slice.ObjectConstructionException;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;

public class Main {

	@SuppressWarnings("unchecked" )
	public static void main(String[] args) {
		
		Set<Region> regions = new HashSet() {{
			add(new Region("USA", "Anchorage", 10));
			add(new Region("USA", "Juno", 5));
			add(new Region("CAN", "Juno", 5));
			add(new Region("CAN", "Anchorage", 10));
		}};
		
		Reducer<Region> reflectiveRegionReducer = new AbstractReducer<Region>() {
			@Override
			public Region fromSlice(Slice<Region> slice) throws ObjectConstructionException {
				//Reflection based fromSlice 
				return (Region) slice.toType(Region.class);
			}
			
			@Override
			public Slice<Region> toSlice(Region region) throws SliceConstructionException {
				//Reflection based toSlice
				return new Slice<Region>(region);
			}
		};
		
		Reducer<Region> customRegionReducer = new AbstractReducer<Region>() {
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
				Set<String> countries = Slice.getAsType("countries", slice, new HashSet<>());
				//Set<String> states = Slice.getAsType("states", slice, new HashSet<>());
				Set<String> cities = Slice.getAsType("cities", slice, new HashSet<>());
				List<Integer> populations = Slice.getAsType("populations", slice, new ArrayList<>());
				return new Region(countries, cities, populations);
			}
		};
		
		Set<Region> reflectiveReduced = reflectiveRegionReducer.reduce(regions);
		Set<Region> customReduced = customRegionReducer.reduce(regions);
		return;
	}
	
	public static class Region {
		public Set<String> countries;
		//public Set<String> states;
		public Set<String> cities;
		public List<Integer> populations;
		
		public Region() {
			
		}
		
		@SuppressWarnings("unchecked" )
		public Region(String country, String city, Integer population) {
			this.countries = new HashSet() {{ add(country); }};
			//this.states = new HashSet() {{ add(state); }};
			this.cities = new HashSet() {{ add(city); }};
			this.populations = new ArrayList() {{ add(population); }};
		}
		
		public Region(Set<String> countries, Set<String> cities, List<Integer> populations) {
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
