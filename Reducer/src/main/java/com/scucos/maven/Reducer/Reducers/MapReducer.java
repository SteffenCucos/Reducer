package com.scucos.maven.Reducer.Reducers;

import java.util.Collection;
import java.util.Map;

import com.scucos.maven.Reducer.Slice;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;
import com.scucos.maven.Reducer.Reducers.ReflectionUtils.ObjectConstructionException;

/**
 * Convenience reducer for Map<K, Collection<V>> values.
 *
 * Runtime complexity:
 * MapReducer delegates the actual reduction to RecursiveReducer after converting
 * each map into a Slice. Let n be the number of maps, d be the number of keys or
 * dimensions represented in each map, and V be the N-dimensional volume of the
 * smallest super-slice containing the input point-slices. The dominant runtime
 * is the inherited RecursiveReducer reduction cost, roughly proportional to V
 * for dense point-slice inputs, plus O(n * d) to convert input maps to slices and
 * O(r * d) to convert the r reduced slices back to maps. Space usage is O(n * d)
 * for the intermediate slice representation plus the reducer's working sets.
 */
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
