package com.scucos.maven.Reducer;

import java.util.Set;
import com.scucos.maven.Reducer.Slice.SliceConstructionException;
import com.scucos.maven.Reducer.Reducer.timeable;
import com.scucos.maven.Reducer.Slice.ObjectConstructionException;

/**
 * The interface that defines all the necessary methods that a reducer must implement.
 * @author SCucos
 * @param <T> the type of object that will be reduced
 */
public interface Reducer<T> {
	
	interface timeable {
		Object run();
	}
	
	Set<T> reduce(Set<T> ts);
	
	Set<Slice<T>> reduceSlices(Set<Slice<T>> slices);
	
	Slice<T> toSlice(T t) throws SliceConstructionException;
	
	T fromSlice(Slice<T> slice) throws ObjectConstructionException;
	
	@SuppressWarnings("unchecked")
	static <T> T time(String opperation, timeable t) {
		long startTime = System.currentTimeMillis();
		Object result = t.run();
		long endTime = System.currentTimeMillis();
		System.out.println(opperation + " took " + (endTime - startTime) + " milliseconds");
		
		return (T)result;
	}

}
