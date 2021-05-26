package com.scucos.maven.Reducer.Reducers;

import java.util.Set;

import com.scucos.maven.Reducer.Slice;

public abstract class GraphDistanceReducer<T> implements Reducer<T> {

	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		// TODO Auto-generated method stub
		
		//Work on reducing closed cycles in graph of slices where edges are drawn between slices which have distance = 1
		
		
		//the graph can be represented as a matrix like so:
		
		/*
		 * say we have 4 slices like this
		 * 1 = [1,A] 2 = [1,B] 3 = [2,A] 4 = [2,C]
		 * 
		 * which yield this graph, where 1 represents an edge, and x represents no edge
		 * We only need to fill in the top half (minus the diagonal) because the graph is
		 * bidirectional and so the other half would just be the mirror of the first half
		 * 
		 *   1  2  3  4
		 * 1 x  1  1  x
		 * 2 ---x  1  x
		 * 3 ------x  1
		 * 4 ---------x
		 */
		
		
		
		
		
		
		
		
		
		
		
		
		
		return null;
	}	
}
