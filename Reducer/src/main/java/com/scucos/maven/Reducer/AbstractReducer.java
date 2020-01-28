package com.scucos.maven.Reducer;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractReducer<T> implements Reducer<T> {

	@Override
	public Set<T> reduce(Set<T> ts) {
		
		Set<Slice<T>> slices = ts
				.stream()
				.map(t -> toSlice(t))
				.collect(Collectors.toSet());
		
		Set<Slice<T>> reduced = Reducer.time("Reduce", () -> {
			return reduceSlices(slices);
		});
		
		return reduced
				.stream()
				.map(s -> fromSlice(s))
				.collect(Collectors.toSet());
	}
}