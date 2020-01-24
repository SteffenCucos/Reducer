package com.scucos.maven.Reducer;

import java.util.Set;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;
import com.scucos.maven.Reducer.Slice.ObjectConstructionException;

/**
 * The interface that defines all the necessary methods that a reducer must implement.
 * @author SCucos
 * @param <T> the type of object that will be reduced
 */
public interface Reducer<T> {
	
	Set<T> reduce(Set<T> ts);
	
	Slice<T> toSlice(T t) throws SliceConstructionException;
	
	T fromSlice(Slice<T> slice) throws ObjectConstructionException;

}
