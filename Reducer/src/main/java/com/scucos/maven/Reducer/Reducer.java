package com.scucos.maven.Reducer;

import java.util.Set;
import com.scucos.maven.Reducer.Slice.ObjectConstructionException;

public interface Reducer<T extends Reduceable<T>> {
	Set<T> reduce(Set<T> ts);
	
	T fromSlice(Slice<T> slice) throws ObjectConstructionException;
}
