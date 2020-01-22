package com.scucos.maven.Reducer;

import java.util.HashSet;
import java.util.Set;

import com.scucos.maven.Reducer.Slice.ObjectConstructionException;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;

public class Main {

	@SuppressWarnings("unchecked" )
	public static void main(String[] args) {
		
		Set<Region> regions = new HashSet() {{
			add(new Region("USA", "Alaska", "Anchorage"));
			add(new Region("USA", "Alaska", "Juno"));
			add(new Region("USA", "Alaska", "Juno"));
		}};
		
		Reducer<Region> regionReducer = new AbstractReducer<Region>() {
			@Override
			public Region fromSlice(Slice slice) {
				try {
					return (Region) slice.toType(Region.class);
				} catch (ObjectConstructionException e) {
					return null;
				}
			}
		};
		
		Set<Region> reduced = regionReducer.reduce(regions);
		
		return;
	}
	
	public static class Region implements Reduceable<Region> {
		public Set<String> countries;
		public Set<String> states;
		public Set<String> cities;
		
		public Region() {
			
		}
		
		@SuppressWarnings("unchecked" )
		public Region(String country, String state, String city) {
			this.countries = new HashSet() {{ add(country); }};
			this.states = new HashSet() {{ add(state); }};
			this.cities = new HashSet() {{ add(city); }};
		}
		
		public Region(Set<String> countries, Set<String> states, Set<String> cities) {
			this.countries = countries;
			this.states = states;
			this.cities = cities;
		}

		@Override
		@SuppressWarnings("unchecked" )
		public Slice<Region> toSlice() {
			try {
				return new Slice<Region>(this);
			} catch (SliceConstructionException e) {
				return null;
			}
		}
	}
}
