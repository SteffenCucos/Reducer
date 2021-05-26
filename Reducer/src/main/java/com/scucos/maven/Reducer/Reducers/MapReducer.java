package com.scucos.maven.Reducer.Reducers;

import java.util.Collection;
import java.util.Map;

import com.scucos.maven.Reducer.Slice;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;
import com.scucos.maven.Reducer.Reducers.ReflectionUtils.ObjectConstructionException;

public class MapReducer<K, V> extends RecursiveReducer<Map<K, Collection<V>>> {	
	
	@Override
	public Slice<Map<K, Collection<V>>> toSlice(Map<K, Collection<V>> t) throws SliceConstructionException {
		return new Slice<Map<K, Collection<V>>>(t);
	}

	@Override
	public Map<K, Collection<V>> fromSlice(Slice<Map<K, Collection<V>>> slice) throws ObjectConstructionException {
		return Slice.toMap(slice);
	}
}