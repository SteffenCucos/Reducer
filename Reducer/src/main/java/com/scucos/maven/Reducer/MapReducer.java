package com.scucos.maven.Reducer;

import java.util.Collection;
import java.util.Map;

import com.scucos.maven.Reducer.Slice.ObjectConstructionException;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;

public class MapReducer<K, V> extends RecursiveReducer<Map<K, Collection<V>>> {	
	@Override
	public Slice<Map<K, Collection<V>>> toSlice(Map<K, Collection<V>> t) throws SliceConstructionException {
		return new Slice<>(t);
	}

	@Override
	public Map<K, Collection<V>> fromSlice(Slice<Map<K, Collection<V>>> slice) throws ObjectConstructionException {
		return Slice.toMap(slice);
	}
}