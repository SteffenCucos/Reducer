package com.scucos.maven.Reducer;

import java.util.Set;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;
import com.scucos.maven.Reducer.Slice.ObjectConstructionException;

public interface Reducer<T> {
	
	Set<T> reduce(Set<T> ts);
	
	Slice<T> toSlice(T t) throws SliceConstructionException;
	
	T fromSlice(Slice<T> slice) throws ObjectConstructionException;

}
