package com.scucos.maven.Reducer.Reducers;

import java.util.Set;
import java.util.stream.Collectors;

import com.scucos.maven.Reducer.Slice.SliceConstructionException;
import com.scucos.maven.Reducer.Slice;
import com.scucos.maven.Reducer.Slice.ObjectConstructionException;

/**
 * The interface that defines all the necessary methods that a reducer must implement.
 * @author SCucos
 * @param <T> the type of object that will be reduced
 */
public interface Reducer<T> {
	
	/**
	 * Default reduce method that handles turning Ts into Slice<T>s, 
	 * dispatching to the correct reduceSlices method, and turning the reduced 
	 * Slice<T>s back into Ts
	 * @param ts
	 * @return
	 */
	default Set<T> reduce(Set<T> ts) {
		int oldSize = ts.size();
		
		Set<Slice<T>> slices = ts
				.stream()
				.map(t -> toSlice(t))
				.collect(Collectors.toSet());
		
		Set<Slice<T>> reduced = time(
			new Timeable() {
				int afterSize;
				
				@Override
				public Object run() {
					Set<Slice<T>> reduced = reduceSlices(slices);
					afterSize = reduced.size();
					return reduced; 
				}
	
				@Override
				public String runDescription() {
					return String.format("Reduced %s slices(s) to %s slice(s)", oldSize, afterSize);
				}
			}
		);
		
		return reduced
				.stream()
				.map(s -> fromSlice(s))
				.collect(Collectors.toSet());
	}
	
	/**
	 * All implementations need to define this method to actually reduce the given slices
	 * @param slices
	 * @return
	 */
	Set<Slice<T>> reduceSlices(Set<Slice<T>> slices);
	
	/**
	 * Responsible for taking a T t and constructing a Slice<T> from it.
	 * @param t
	 * @return
	 * @throws SliceConstructionException
	 */
	default Slice<T> toSlice(T t) throws SliceConstructionException {
		return new Slice<T>(t);
	}
	
	/**
	 * Responsible for turning a Slice<T> back into a T
	 * @param slice
	 * @return
	 * @throws ObjectConstructionException
	 */
	default T fromSlice(Slice<T> slice) throws ObjectConstructionException {
		return slice.toType();
	}
	
	/**
	 * Interface for a timeable operation with a description of running the operation
	 */
	interface Timeable {
		Object run();
		String runDescription();
	}
	
	/**
	 * Runs a lambda and logs the execution time in milliseconds, 
	 * while also managing the returned result.
	 * @param opperation
	 * @param t
	 * @return
	 */
	@SuppressWarnings("unchecked")
	static <T> T time(Timeable timeable) {
		long startTime = System.currentTimeMillis();
		Object result = timeable.run();
		long endTime = System.currentTimeMillis();
		System.out.println(timeable.runDescription() + " took " + (endTime - startTime) + " milliseconds");
		
		return (T)result;
	}

}
